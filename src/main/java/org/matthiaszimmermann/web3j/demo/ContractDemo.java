package org.matthiaszimmermann.web3j.demo;

import java.math.BigInteger;

import org.matthiaszimmermann.web3j.demo.contract.Greeter;
import org.matthiaszimmermann.web3j.util.Alice;
import org.matthiaszimmermann.web3j.util.Web3jConstants;
import org.matthiaszimmermann.web3j.util.Web3jUtils;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Sample application to demonstrate smart contract deployment and usage with the web3j library. 
 */
public class ContractDemo extends AbstractDemo {

	public static String MESSAGE = "hello from ContractDemo";

	public static void main(String [] args) throws Exception  {
		new ContractDemo(args).run();
	}

	public ContractDemo(String [] args) {
		super(args);
	}

	@Override
	public void run() throws Exception {
		super.run();
		
		// make sure Alice has sufficient funds to run this demo
		BigInteger aliceBalance = Web3jUtils.getBalanceWei(web3j, Alice.ADDRESS);
		BigInteger initialBalance = BigInteger.valueOf(25_000_000_000_000_000L);
		
		if(aliceBalance.subtract(initialBalance).signum() < 0) {
			Web3jUtils.transferFromCoinbaseAndWait(web3j, Alice.ADDRESS, initialBalance.subtract(aliceBalance));			
		}

		balanceAlice("before deploy");
		
		// deploy the greeter contract
		Greeter contract = Greeter
				.deploy(
						web3j, 
						Alice.CREDENTIALS, 
						Web3jConstants.GAS_PRICE, 
						Web3jConstants.GAS_LIMIT_GREETER_TX, 
						BigInteger.ZERO, 
						new Utf8String(MESSAGE))
				.get();
		
		// get deploy info
		String contractAddress = contract.getContractAddress(); 
		TransactionReceipt txReceipt = contract.getTransactionReceipt().get();
		String deployHash = txReceipt.getTransactionHash();
		BigInteger deployFees = txReceipt.getCumulativeGasUsed().multiply(Web3jConstants.GAS_PRICE);
		
		System.out.println("Deploy hash: " + deployHash);
		System.out.println("Deploy fees: " + Web3jUtils.weiToEther(deployFees));
		balanceAlice("after deploy");
		System.out.println();
		
		// get contract balance
		Uint256 deposits = contract
				.deposits()
				.get();
		
		System.out.println("Contract address: " + contractAddress);
		System.out.println("Contract address balance (initial): " + Web3jUtils.getBalanceWei(web3j, contractAddress));
		System.out.println("Contract.deposits(): " + deposits.getValue());
	
		// transfer funds to contract, and call deposits() function
		BigInteger contractFunding = BigInteger.valueOf(123_456_789L);
		Web3jUtils.transferFromCoinbaseAndWait(web3j, contractAddress, contractFunding);			
		
		deposits = contract
				.deposits()
				.get();
		
		System.out.println("Contract address balance (after funding): " + Web3jUtils.weiToEther(Web3jUtils.getBalanceWei(web3j, contractAddress)));
		System.out.println("Contract.deposits(): " + deposits.getValue() + "\n");
		
		// call greet() function
		Utf8String message = contract
				.greet()
				.get();
		
		System.out.println("Message returned by Contract.greet(): " + message.toString() + "\n");
		balanceAlice("after greet");
		
		// kill contract
		txReceipt = contract
				.kill()
				.get();
		
		BigInteger killFees = txReceipt.getCumulativeGasUsed().multiply(Web3jConstants.GAS_PRICE);
		System.out.println("Contract.kill() fee: " + Web3jUtils.weiToEther(killFees));
		balanceAlice("after kill");
	}
	
	private void balanceAlice(String info) throws Exception {
		System.out.println("Alice's account balance (" + info + "): " + Web3jUtils.weiToEther(Web3jUtils.getBalanceWei(web3j, Alice.ADDRESS)));
	}
}