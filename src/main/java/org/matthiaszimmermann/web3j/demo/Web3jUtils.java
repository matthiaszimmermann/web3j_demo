package org.matthiaszimmermann.web3j.demo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

public class Web3jUtils {

	public static Web3j buildHttpClient(String ip, String port) {
		String url = String.format("http://%s:%s", ip, port);
		return Web3j.build(new HttpService(url));
	}

	public static String getClientVersion(Web3j web3j) throws InterruptedException, ExecutionException {
		Web3ClientVersion client = web3j
				.web3ClientVersion()
				.sendAsync()
				.get();

		return client.getWeb3ClientVersion();
	}

	public static EthCoinbase getCoinbase(Web3j web3j) throws InterruptedException, ExecutionException {
		return web3j
				.ethCoinbase()
				.sendAsync()
				.get();
	}

	/**
	 * Returns the list of addresses owned by this client.
	 */
	public static EthAccounts getAccounts(Web3j web3j) throws InterruptedException, ExecutionException {
		return web3j
				.ethAccounts()
				.sendAsync()
				.get();
	}
	
	/**
	 * Returns the balance (in Ether) of the specified account address. 
	 */
	public static BigDecimal getBalanceEther(Web3j web3j, String address) throws InterruptedException, ExecutionException {
		return weiToEther(getBalanceWei(web3j, address));
	}
	
	/**
	 * Returns the balance (in Wei) of the specified account address. 
	 */
	public static BigInteger getBalanceWei(Web3j web3j, String address) throws InterruptedException, ExecutionException {
		EthGetBalance balance = web3j
				.ethGetBalance(address, DefaultBlockParameterName.LATEST)
				.sendAsync()
				.get();

		return balance.getBalance();
	}

	/**
	 * Return the nonce (tx count) for the specified address.
	 */
	public static BigInteger getNonce(Web3j web3j, String address) throws InterruptedException, ExecutionException {
		EthGetTransactionCount ethGetTransactionCount = 
				web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();

		return ethGetTransactionCount.getTransactionCount();
	}
	
	/**
	 * Converts the provided Wei amount (smallest value Unit) to Ethers. 
	 */
	public static BigDecimal weiToEther(BigInteger wei) {
		return Convert.fromWei(wei.toString(), Convert.Unit.ETHER);
	}
	

	/**
	 * Waits for the receipt for the transaction specified by the provided tx hash.
	 * Makes 40 attempts (waiting 1 sec. inbetween attempts) to get the receipt object.
	 * In the happy case the tx receipt object is returned.
	 * Otherwise, a runtime exception is thrown. 
	 */
	public static TransactionReceipt waitForReceipt(Web3j web3j, String transactionHash) 
			throws Exception 
	{

		int attempts = Web3jConstants.CONFIRMATION_ATTEMPTS;
		int sleep_millis = Web3jConstants.SLEEP_DURATION;
		
		Optional<TransactionReceipt> receipt = getReceipt(web3j, transactionHash);

		while(attempts-- > 0 && !receipt.isPresent()) {
			Thread.sleep(sleep_millis);
			receipt = getReceipt(web3j, transactionHash);
		}

		if (attempts <= 0) {
			throw new RuntimeException("No Tx receipt received");
		}

		return receipt.get();
	}

	/**
	 * Returns the TransactionRecipt for the specified tx hash as an optional.
	 */
	public static Optional<TransactionReceipt> getReceipt(Web3j web3j, String transactionHash) 
			throws Exception 
	{
		EthGetTransactionReceipt receipt = web3j
				.ethGetTransactionReceipt(transactionHash)
				.sendAsync()
				.get();

		return receipt.getTransactionReceipt();
	}

}
