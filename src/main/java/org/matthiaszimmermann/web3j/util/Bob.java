package org.matthiaszimmermann.web3j.util;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

public class Bob {
	private static final String PRIVATE_KEY = "0xc85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
	private static final String PUBLIC_KEY = "0x947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad7"+
			                                 "5aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b";
	static final ECKeyPair KEY_PAIR = new ECKeyPair(Numeric.toBigInt(PRIVATE_KEY), Numeric.toBigInt(PUBLIC_KEY));

	public static final Credentials CREDENTIALS = Credentials.create(KEY_PAIR);
	public static final String ADDRESS = CREDENTIALS.getAddress();
}
