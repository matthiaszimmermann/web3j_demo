package org.matthiaszimmermann.web3j.demo;

import java.math.BigInteger;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

/**
 * Sample application to demonstrate Ether transfer with the web3j library. 
 */
public class TransferDemo {
	
	static Web3j web3j = null;

	public static void main(String [] args) throws Exception  {
		String ip = Web3jConstants.CLIENT_IP;
		String port = Web3jConstants.CLIENT_PORT;

		if(args.length >= 1) { ip = args[0]; }
		if(args.length >= 2) { port = args[1]; }

		TransferDemo demo = new TransferDemo(ip, port);
		demo.run();
	}

	public TransferDemo(String ip, String port) {
		String url = String.format("http://%s:%s", ip, port);
		web3j = Web3j.build(new HttpService(url));
	}

	/**
	 * Transfers 0.123 Ethers from the coinbase account to the client's second account.
	 */
	public void run()
			throws Exception
	{
		// show Ethereum client details
		Web3ClientVersion client = web3j
				.web3ClientVersion()
				.sendAsync()
				.get();
		
		System.out.println("Connected to " + client.getWeb3ClientVersion() + "\n");

		// get addresses and amount to transfer
		EthCoinbase coinbase = web3j.ethCoinbase().sendAsync().get();
		EthAccounts accounts = web3j.ethAccounts().sendAsync().get();

		String fromAddress = coinbase.getAddress();
		String toAddress = accounts.getResult().get(1);
		BigInteger amountWei = Convert
				.toWei("0.123", Convert.Unit.ETHER)
				.toBigInteger();

		// do the transfer
		demoTransfer(fromAddress, toAddress, amountWei);
	}

	/**
	 * Implementation of the Ethers transfer.
	 * <ol>
	 *   <li>Get nonce for for the sending account</li>
	 *   <li>Create the transaction object</li>
	 *   <li>Send the transaction to the network</li>
	 *   <li>Wait for the confirmation</li>
	 * </ol>
	 */
	void demoTransfer(String fromAddress, String toAddress, BigInteger amountWei)
			throws Exception
	{
		System.out.println("Account (to address) " + toAddress + "\n" + 
				"Balance before Tx: " + Web3jUtils.getBalanceEther(web3j, toAddress) + "\n");

		System.out.println("Transfer " + Web3jUtils.weiToEther(amountWei) + " Ether to account");

		// step 1: get the nonce (tx count for sending address)
		EthGetTransactionCount transactionCount = web3j
				.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
				.sendAsync()
				.get();

		BigInteger nonce = transactionCount.getTransactionCount();
		System.out.println("Nonce for sending address (coinbase): " + nonce);

		// step 2: create the transaction object
		Transaction transaction = Transaction
				.createEtherTransaction(
						fromAddress, 
						nonce, 
						Web3jConstants.GAS_PRICE, 
						Web3jConstants.GAS_LIMIT, 
						toAddress, 
						amountWei);

		// step 3: send the tx to the network
		EthSendTransaction response = web3j
				.ethSendTransaction(transaction)
				.sendAsync()
				.get();

		String txHash = response.getTransactionHash();		
		System.out.println("Tx hash: " + txHash);

		// step 4: wait for the confirmation of the network
		TransactionReceipt receipt = Web3jUtils.waitForReceipt(web3j, txHash);
		
		BigInteger gasUsed = receipt.getCumulativeGasUsed();
		System.out.println("Tx cost: " + gasUsed + " Gas (" + 
				Web3jUtils.weiToEther(gasUsed.multiply(Web3jConstants.GAS_PRICE)) +" Ether)\n");

		System.out.println("Balance after Tx: " + Web3jUtils.getBalanceEther(web3j, toAddress));
	}
}

