package org.matthiaszimmermann.web3j.demo;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.matthiaszimmermann.web3j.util.Alice;
import org.matthiaszimmermann.web3j.util.Bob;
import org.matthiaszimmermann.web3j.util.Web3jConstants;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.request.RawTransaction;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

public class TransferEtherTest extends AbstractEthereumTest {

	/**
	 * Ether transfer tests using methods {@link Transfer#sendFunds()}.
	 * Sending account needs to be unlocked for this to work.   
	 */
	@Test
	public void testSendFunds() throws Exception {
		BigDecimal amountEther = BigDecimal.valueOf(0.123);
		BigInteger amountWei = Convert.toWei(amountEther, Convert.Unit.ETHER).toBigInteger();

		ensureFunds(Alice.ADDRESS, amountWei);

		BigInteger fromBalanceBefore = getBalanceWei(Alice.ADDRESS);
		BigInteger toBalanceBefore = getBalanceWei(Bob.ADDRESS);

		// this is the method to test here
		TransactionReceipt txReceipt = Transfer.sendFunds(
				web3j, Alice.CREDENTIALS, Bob.ADDRESS, amountEther, Convert.Unit.ETHER);

		BigInteger txFees = txReceipt.getGasUsed().multiply(Web3jConstants.GAS_PRICE);

		assertFalse(txReceipt.getBlockHash().isEmpty());
		assertEquals("Unexected balance for 'from' address", fromBalanceBefore.subtract(amountWei.add(txFees)), getBalanceWei(Alice.ADDRESS));
		assertEquals("Unexected balance for 'to' address", toBalanceBefore.add(amountWei), getBalanceWei(Bob.ADDRESS));
	}

	/**
	 * Ether transfer tests using methods {@link Transaction#createEtherTransaction()}, and {@link Web3j#ethSendTransaction()}.
	 * Sending account needs to be unlocked for this to work.   
	 */
	@Test
	public void testCreateAndSendTransaction() throws Exception {

		String from = getCoinbase();
		BigInteger nonce = getNonce(from);
		String to = Alice.ADDRESS;
		BigInteger amountWei = Convert.toWei("0.456", Convert.Unit.ETHER).toBigInteger();

		// this is the method to test here
		Transaction transaction = Transaction
				.createEtherTransaction(
						from, 
						nonce, 
						Web3jConstants.GAS_PRICE, 
						Web3jConstants.GAS_LIMIT_ETHER_TX, 
						to, 
						amountWei);
		

		// record account balances before the transfer
		BigInteger fromBalanceBefore = getBalanceWei(from);
		BigInteger toBalanceBefore = getBalanceWei(to);

		// send the transaction to the ethereum client
		EthSendTransaction ethSendTx = web3j
				.ethSendTransaction(transaction)
				.sendAsync()
				.get();

		String txHash = ethSendTx.getTransactionHash();
		assertFalse(txHash.isEmpty());
		
		TransactionReceipt txReceipt = waitForReceipt(txHash);
		BigInteger txFee = txReceipt.getCumulativeGasUsed().multiply(Web3jConstants.GAS_PRICE);

		// coinbase might have gotten additional funds from mining
		BigInteger fromMinimumBalanceExpected = fromBalanceBefore.subtract(amountWei.add(txFee));
		BigInteger fromBalanceActual = getBalanceWei(from);
		BigInteger fromBalanceDelta = fromBalanceActual.subtract(fromMinimumBalanceExpected);
		
		System.out.println("testCreateAndSendTransaction balance difference=" + fromBalanceDelta + " likely cause block reward (5 Ethers) and tx fees (" + txFee + " Weis)");
		assertTrue("Unexected balance for 'from' address. difference=" + fromBalanceDelta, fromBalanceDelta.signum() >= 0);
		assertEquals("Unexected balance for 'to' address", toBalanceBefore.add(amountWei), getBalanceWei(to));		
	}

	/**
	 * Ether transfer tests using methods {@link RawTransaction#createEtherTransaction()}, {@link TransactionEncoder#signMessage()} and {@link Web3j#ethSendRawTransaction()}.
	 * Most complex transfer mechanism, but offers the highest flexibility.   
	 */
	@Test
	public void testCreateSignAndSendTransaction() throws Exception {

		String from = Alice.ADDRESS;
		Credentials credentials = Alice.CREDENTIALS;
		BigInteger nonce = getNonce(from);
		String to = Bob.ADDRESS; 
		BigInteger amountWei = Convert.toWei("0.789", Convert.Unit.ETHER).toBigInteger();

		// create raw transaction
		RawTransaction txRaw = RawTransaction
				.createEtherTransaction(
						nonce, 
						Web3jConstants.GAS_PRICE, 
						Web3jConstants.GAS_LIMIT_ETHER_TX, 
						to, 
						amountWei);

		// sign raw transaction using the sender's credentials
		byte[] txSignedBytes = TransactionEncoder.signMessage(txRaw, credentials);
		String txSigned = Numeric.toHexString(txSignedBytes);

		BigInteger txFeeEstimate = Web3jConstants.GAS_LIMIT_ETHER_TX.multiply(Web3jConstants.GAS_PRICE);

		// make sure sender has sufficient funds
		ensureFunds(Alice.ADDRESS, amountWei.add(txFeeEstimate));

		// record balanances before the ether transfer
		BigInteger fromBalanceBefore = getBalanceWei(Alice.ADDRESS);
		BigInteger toBalanceBefore = getBalanceWei(Bob.ADDRESS);

		// send the signed transaction to the ethereum client
		EthSendTransaction ethSendTx = web3j
				.ethSendRawTransaction(txSigned)
				.sendAsync()
				.get();

		Error error = ethSendTx.getError();
		assertTrue(error == null);
		
		String txHash = ethSendTx.getTransactionHash();
		assertFalse(txHash.isEmpty());
		
		TransactionReceipt txReceipt = waitForReceipt(txHash);
		BigInteger txFee = txReceipt.getCumulativeGasUsed().multiply(Web3jConstants.GAS_PRICE);

		assertEquals("Unexected balance for 'from' address", fromBalanceBefore.subtract(amountWei.add(txFee)), getBalanceWei(from));
		assertEquals("Unexected balance for 'to' address", toBalanceBefore.add(amountWei), getBalanceWei(to));
	}

	/**
	 * Test accessing transactions, blocks and their attributes using methods {@link Web3j#ethGetTransactionByHash()},  {@link Web3j#ethGetBlockByHash()}  {@link Web3j#ethGetBlockByNumber()}.
	 */
	@Test
	public void testTransactionAndBlockAttributes() throws Exception {
		String account0 = getCoinbase();
		String account1 = getAccount(1);
		BigInteger transferAmount = new BigInteger("31415926");

		String txHash = transferWei(account0, account1, transferAmount);
		waitForReceipt(txHash);

		// query for tx via tx hash value
		EthTransaction ethTx = web3j
				.ethGetTransactionByHash(txHash)
				.sendAsync()
				.get();

		org.web3j.protocol.core.methods.response.Transaction tx = ethTx
				.getTransaction()
				.get();

		String blockHash = tx.getBlockHash();
		BigInteger blockNumber = tx.getBlockNumber();
		String from = tx.getFrom();
		String to = tx.getTo();
		BigInteger amount = tx.getValue();

		// check tx attributes
		assertTrue("Tx hash does not match input hash", txHash.equals(tx.getHash()));
		assertTrue("Tx block index invalid", blockNumber == null || blockNumber.compareTo(new BigInteger("0")) >= 0);
		assertTrue("Tx from account does not match input account", account0.equals(from));
		assertTrue("Tx to account does not match input account", account1.equals(to));
		assertTrue("Tx transfer amount does not match input amount", transferAmount.equals(amount));

		// query for block by hash
		EthBlock ethBlock = web3j
				.ethGetBlockByHash(blockHash, true)
				.sendAsync()
				.get();

		Block blockByHash = ethBlock.getBlock(); 
		assertNotNull(String.format("Failed to get block for hash %s", blockHash), blockByHash);
		System.out.println("Got block for hash " + blockHash);


		// query for block by number
		DefaultBlockParameter blockParameter = DefaultBlockParameter
				.valueOf(blockNumber);

		ethBlock = web3j
				.ethGetBlockByNumber(blockParameter, true)
				.sendAsync()
				.get();
		
		Block blockByNumber = ethBlock.getBlock(); 
		assertNotNull(String.format("Failed to get block for number %d", blockNumber), blockByNumber);
		System.out.println("Got block for number " + blockNumber);

		assertTrue("Bad tx hash for block by number", blockByNumber.getHash().equals(blockHash));
		assertTrue("Bad tx number for block by hash", blockByHash.getNumber().equals(blockNumber));
		assertTrue("Query block by hash and number have different parent hashes", blockByHash.getParentHash().equals(blockByNumber.getParentHash()));
		assertTrue("Query block by hash and number results in different blocks", blockByHash.equals(blockByNumber));

		// find original tx in block
		boolean found = false;
		for(TransactionResult<?> txResult: blockByHash.getTransactions()) {
			TransactionObject txObject = (TransactionObject) txResult;

			// verify tx attributes returned by block query
			if(txObject.getHash().equals(txHash)) {
				assertTrue("Tx from block has bad from", txObject.getFrom().equals(account0));
				assertTrue("Tx from block has bad to", txObject.getTo().equals(account1));
				assertTrue("Tx from block has bad amount", txObject.getValue().equals(transferAmount));
				found = true;
				break;
			}
		}

		assertTrue("Tx not found in blocks transaction list", found);
	}
}
