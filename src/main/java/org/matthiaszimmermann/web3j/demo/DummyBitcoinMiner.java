package org.matthiaszimmermann.web3j.demo;

import java.security.MessageDigest;
import java.util.Random;

// TODO remove this class (somewhat unrelated demo class)
public class DummyBitcoinMiner {

	public static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static final String LEADING_ZEROS = "0000000000";

	private static Random rnd = new Random();
	private static int hashes = 0;
	private static String difficulty;
	private static MessageDigest md5;

	public static void main(String[] args) throws Exception {

		md5 = MessageDigest.getInstance("MD5");
		difficulty = args.length == 0 ? 
				LEADING_ZEROS.substring(0, 2) : 
				LEADING_ZEROS.substring(0, new Integer(args[0])); 

		String hashOfPreviousBlock = "B37060F28617A5DFA3DB9A3D547663B3";
		String blockPayload = "I am Satoshi Nakamoto";
		String nonce = nextNonceValue(4);
		String blockData = buildBlock(hashOfPreviousBlock, blockPayload, nonce);
		String blockHash = calculateHash(blockData);

		while(!blockHash.startsWith(difficulty)) {
			nonce = nextNonceValue(4);
			blockData = buildBlock(hashOfPreviousBlock, blockPayload, nonce);
			blockHash = calculateHash(blockData);

			System.out.println(blockData + " -> " + blockHash);
			hashes++;
		}

		System.out.println("Success with nonce '" + nonce + 
				"' for difficulty '" + difficulty + "'. Hashes calculated: " + hashes);
	}

	private static String buildBlock(String hashOfPreviousBlock, String blockPayload, String nonce) {
		return String.format("%s:%s:%s", hashOfPreviousBlock, blockPayload, nonce);
	}
	
	private static String nextNonceValue(int size) {
		StringBuffer buf = new StringBuffer();

		for(int i = 0; i < size; i++) {
			buf.append((char)(rnd.nextInt(25)+97));
		}

		return buf.toString();
	}
	
	private static String calculateHash(String blockData) {
		md5.update(blockData.getBytes());
		return bytesToHex(md5.digest());
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}	
}
