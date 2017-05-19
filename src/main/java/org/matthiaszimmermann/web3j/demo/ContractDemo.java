package org.matthiaszimmermann.web3j.demo;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.matthiaszimmermann.web3j.demo.contract.Greeter;
import org.matthiaszimmermann.web3j.util.Alice;
import org.matthiaszimmermann.web3j.util.Web3jConstants;
import org.matthiaszimmermann.web3j.util.Web3jUtils;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Sample application to demonstrate smart contract deployment
 * and usage with the web3j library. 
 */
public class ContractDemo extends AbstractDemo {

	@Override
	public void run() throws Exception {
		super.run();

		fundAlice();                        // make alice rich
		Greeter greeter = deployContract(); // deploy the greeter contract
		sendFunds(greeter);                 // send money to contract
		callGreet(greeter);                 // call greet()
		killContract(greeter);              // kill contract
	}
	
	public static void main(String [] args) throws Exception  {
		new ContractDemo(args).run();
	}

	public ContractDemo(String [] args) {
		super(args);
	}


	private Greeter deployContract() throws Exception {
		System.out.println("// Deploy contract Greeter");

		Greeter contract = Greeter
				.deploy(
						web3j, 
						Alice.CREDENTIALS, 
						Web3jConstants.GAS_PRICE, 
						Web3jConstants.GAS_LIMIT_GREETER_TX, 
						BigInteger.ZERO, 
						new Utf8String("hello world"))
				.get();

		// get tx receipt
		TransactionReceipt txReceipt = contract
				.getTransactionReceipt()
				.get();

		// get tx hash and tx fees
		String deployHash = txReceipt.getTransactionHash();
		BigInteger deployFees = txReceipt
				.getCumulativeGasUsed()
				.multiply(Web3jConstants.GAS_PRICE);

		System.out.println("Deploy hash: " + deployHash);
		System.out.println("Deploy fees: " + Web3jUtils.weiToEther(deployFees));

		// get initial contract balance
		Uint256 deposits = contract
				.deposits()
				.get();

		String contractAddress = contract.getContractAddress();
		System.out.println("Contract address: " + contractAddress);
		System.out.println("Contract address balance (initial): " + Web3jUtils.getBalanceWei(web3j, contractAddress));
		System.out.println("Contract.deposits(): " + deposits.getValue());
		printBalanceAlice("after deploy");
		System.out.println();

		return contract;
	}

	private void sendFunds(Greeter contract) throws Exception {
		System.out.println("// Send 0.05 Ethers to contract");
		
		// trasfer ether to contract account
		String contractAddress = contract.getContractAddress();
		BigDecimal amountEther = BigDecimal.valueOf(0.05);
		BigInteger amountWei = Web3jUtils.etherToWei(amountEther);
		Web3jUtils.transferFromCoinbaseAndWait(web3j, contractAddress, amountWei);			

		// check current # of deposits and balance
		Uint256 deposits = contract
				.deposits()
				.get();

		System.out.println("Contract address balance (after funding): " + Web3jUtils.weiToEther(Web3jUtils.getBalanceWei(web3j, contractAddress)));
		System.out.println("Contract.deposits(): " + deposits.getValue() + "\n");
	}
	
	private void callGreet(Greeter contract) throws Exception {
		System.out.println("// Call greet()");
		
		Utf8String message = contract
				.greet()
				.get();
		
		System.out.println("Message returned by Contract.greet(): " + message.toString());
		printBalanceAlice("after greet");
		System.out.println();
	}

	private void killContract(Greeter contract) throws Exception {
		System.out.println("// Kill contract");
		
		TransactionReceipt txReceipt = contract
				.kill()
				.get();

		BigInteger killFees = txReceipt
				.getCumulativeGasUsed()
				.multiply(Web3jConstants.GAS_PRICE);

		System.out.println("Contract.kill() fee: " + Web3jUtils.weiToEther(killFees));
		printBalanceAlice("after kill");
	}

	private void fundAlice() throws Exception {
		System.out.println("// Fund Alice");
		
		// make sure Alice has sufficient funds to run this demo
		BigInteger aliceBalance = Web3jUtils.getBalanceWei(web3j, Alice.ADDRESS);
		BigInteger initialBalance = BigInteger.valueOf(25_000_000_000_000_000L);

		if(aliceBalance.subtract(initialBalance).signum() < 0) {
			Web3jUtils.transferFromCoinbaseAndWait(web3j, Alice.ADDRESS, initialBalance.subtract(aliceBalance));			
		}

		printBalanceAlice("before deploy");
		System.out.println();
	}

	private void printBalanceAlice(String info) throws Exception {
		System.out.println("Alice's account balance (" + info + "): " + Web3jUtils.weiToEther(Web3jUtils.getBalanceWei(web3j, Alice.ADDRESS)));
	}
}