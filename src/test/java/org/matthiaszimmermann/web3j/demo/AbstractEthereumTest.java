package org.matthiaszimmermann.web3j.demo;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.matthiaszimmermann.web3j.util.Web3jConstants;
import org.matthiaszimmermann.web3j.util.Web3jUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class AbstractEthereumTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	public static Web3j web3j = null;
	public static boolean setupFailed = false;

	@Before
	public void setUp() throws Exception {
		 web3j = Web3jUtils.buildHttpClient("192.168.99.100", Web3jConstants.CLIENT_PORT);
	}

	void ensureFunds(String address, BigInteger amountWei) throws Exception {
		BigInteger balance = getBalanceWei(address);
		
		if(balance.compareTo(amountWei) >= 0) {
			return;
		}
		
		BigInteger missingAmount = amountWei.subtract(balance);
		Web3jUtils.transferFromCoinbaseAndWait(web3j, address, missingAmount);
	}
//	
//	void ensureFundsForTransaction(String address, BigInteger amount, BigInteger txFeeEstimate) throws Exception {
//		BigInteger balance = getBalanceWei(address);
//		BigInteger totalAmount = amount.add(txFeeEstimate);
//		BigInteger missingAmount = totalAmount.subtract(balance);
//		
//		if(balance.compareTo(totalAmount) >= 0) {
//			return;
//		}
//		
//		System.out.println(String.format("insufficient funds. transfer %d to %s from coinbase", missingAmount, address));
//		
//		transferFunds(address, missingAmount);
//	}
	
//	String transferFunds(String address, BigInteger amount) throws Exception {
//		String txHash = transferWei(getCoinbase(), address, amount); 
//		waitForReceipt(txHash);
//		return txHash;
//	}

	TransactionReceipt waitForReceipt(String transactionHash) throws Exception {
		return Web3jUtils.waitForReceipt(web3j, transactionHash);
	}

	BigInteger getBalanceWei(String address) throws Exception {
		return Web3jUtils.getBalanceWei(web3j, address); 
	}

	BigDecimal toEther(BigInteger weiAmount) {
		return Web3jUtils.weiToEther(weiAmount);
	}

	String transferWei(String from, String to, BigInteger amountWei) throws Exception {
		BigInteger nonce = getNonce(from);
		Transaction transaction = Transaction.createEtherTransaction(
				from, nonce, Web3jConstants.GAS_PRICE, Web3jConstants.GAS_LIMIT_ETHER_TX, to, amountWei);

		EthSendTransaction ethSendTransaction = web3j.ethSendTransaction(transaction).sendAsync().get();
		System.out.println("transferEther. nonce: " + nonce + " amount: " + amountWei + " to: " + to);

		String txHash = ethSendTransaction.getTransactionHash(); 
		waitForReceipt(txHash);
		
		return txHash;
	}

	BigInteger getNonce(String address) throws Exception {
		return Web3jUtils.getNonce(web3j, address);
	}

	String getCoinbase() {
		return getAccount(0);
	}

	String getAccount(int i) {
		try {
			EthAccounts accountsResponse = web3j.ethAccounts().sendAsync().get();
			List<String> accounts = accountsResponse.getAccounts();

			return accounts.get(i);
		} 
		catch (Exception e) {
			System.out.println(e.getMessage());
			return "<no address>";
		}
	}

	static String load(String filePath) throws URISyntaxException, IOException {
		URL url = AbstractEthereumTest.class.getClass().getResource(filePath);
		byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));
		return new String(bytes);
	}
}
