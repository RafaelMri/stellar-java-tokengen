package de.danieldeusing.stellar.tokengen.util;

import de.danieldeusing.stellar.tokengen.types.NetworkType;

public abstract class StellarUtil {
    public static String getNetworkUrl(NetworkType type) {
        switch (type) {
            case TESTNET: return "https://horizon-testnet.stellar.org";
            case MAINNET: return "";
        }
        // default always is Testnet
        return "https://horizon-testnet.stellar.org";
    }
}
