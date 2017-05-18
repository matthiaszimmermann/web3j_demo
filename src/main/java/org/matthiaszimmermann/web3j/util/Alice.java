package org.matthiaszimmermann.web3j.util;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

public class Alice {
	static final String PRIVATE_KEY = "0xa392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6";
	static final String PUBLIC_KEY =  "0x506bc1dc099358e5137292f4efdd57e400f29ba5132aa5d12b18dac1c1f6aab" +
					                  "a645c0b7b58158babbfa6c6cd5a48aa7340a8749176b120e8516216787a13dc76";
	static final ECKeyPair KEY_PAIR = new ECKeyPair(Numeric.toBigInt(PRIVATE_KEY), Numeric.toBigInt(PUBLIC_KEY));

	public static final Credentials CREDENTIALS = Credentials.create(KEY_PAIR);
	public static final String ADDRESS = CREDENTIALS.getAddress();
}
