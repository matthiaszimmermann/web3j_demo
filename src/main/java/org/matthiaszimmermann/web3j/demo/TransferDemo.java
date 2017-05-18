package org.matthiaszimmermann.web3j.demo;

import java.math.BigInteger;

import org.matthiaszimmermann.web3j.util.Web3jConstants;
import org.matthiaszimmermann.web3j.util.Web3jUtils;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthMining;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

/**
 * Sample application to demonstrate Ether transfer with the web3j library. 
 */
public class TransferDemo extends AbstractDemo {

	public static void main(String [] args) throws Exception  {
		new TransferDemo(args).run();
	}

	public TransferDemo(String [] args) {
		super(args);
	}

	/**
	 * Transfers 0.123 Ethers from the coinbase account to the client's second account.
	 */
	@Override
	public void run() throws Exception {
		super.run();
		
		// get basic info 
		EthMining mining = web3j.ethMining().sendAsync().get();
		EthCoinbase coinbase = web3j.ethCoinbase().sendAsync().get();
		EthAccounts accounts = web3j.ethAccounts().sendAsync().get();

		System.out.println("Client is mining: " + mining.getResult());
		System.out.println("Coinbase address: " + coinbase.getAddress());
		System.out.println("Coinbase balance: " + Web3jUtils.getBalanceEther(web3j, coinbase.getAddress()) + "\n");
		
		// get addresses and amount to transfer
		String fromAddress = coinbase.getAddress();
		String toAddress = accounts.getResult().get(1);
		BigInteger amountWei = Convert.toWei("0.123", Convert.Unit.ETHER).toBigInteger();

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
		System.out.println("Accounts[1] (to address) " + toAddress + "\n" + 
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
						Web3jConstants.GAS_LIMIT_ETHER_TX, 
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

