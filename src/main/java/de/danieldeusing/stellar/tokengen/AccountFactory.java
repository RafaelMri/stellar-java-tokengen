package de.danieldeusing.stellar.tokengen;

import de.danieldeusing.stellar.tokengen.impl.AccountImpl;
import de.danieldeusing.stellar.tokengen.interfaces.IAccount;
import de.danieldeusing.stellar.tokengen.types.AccountType;
import de.danieldeusing.stellar.tokengen.types.NetworkType;
import de.danieldeusing.stellar.tokengen.util.StellarUtil;
import org.stellar.sdk.KeyPair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class AccountFactory {

    private static void createTestnetAccount(NetworkType network, String accountId) {
        String friendbotUrl = String.format(
                StellarUtil.getNetworkUrl(network) + "/friendbot?addr=%s",
                accountId);
        InputStream response = null;
        try {
            response = new URL(friendbotUrl).openStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2001);
        }
        String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
    }

    public static IAccount create(NetworkType network, AccountType type) {
        final KeyPair keyPair = KeyPair.random();

        switch (network) {
            case TESTNET: {
                createTestnetAccount(network, keyPair.getAccountId());
                break;
            }
            case MAINNET: {
                break;
            }
            default: {
                System.out.println("No suitable network. Exit.");
                System.exit(2002);
            }
        }

        System.out.println(type.name() + " created with: ");
        System.out.println("\tPublic Key : " + keyPair.getAccountId());
        System.out.println("\tSecret Seed: " + new String(keyPair.getSecretSeed()));

        return new AccountImpl(type, keyPair);
    }
}
