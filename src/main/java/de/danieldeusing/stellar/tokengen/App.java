package de.danieldeusing.stellar.tokengen;

import com.google.gson.Gson;
import de.danieldeusing.stellar.tokengen.impl.TokenDescImpl;
import de.danieldeusing.stellar.tokengen.interfaces.IAccount;
import de.danieldeusing.stellar.tokengen.types.AccountType;
import de.danieldeusing.stellar.tokengen.types.NetworkType;
import de.danieldeusing.stellar.tokengen.util.AppUtil;
import de.danieldeusing.stellar.tokengen.util.StellarUtil;
import org.apache.commons.lang3.SystemUtils;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;

public class App {

    public static void main(String[] args) {
        // check if we are on linux/mac else quit
        if ( !(SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) ) {
            AppUtil.fatalError("Operating system not supported by this App. Exit.", -1);
        }

        // init some global stuff
        Properties config = AppUtil.getProperties();
        NetworkType network = NetworkType.valueOf(config.getProperty("NETWORK").toUpperCase());
        Server server = new Server(StellarUtil.getNetworkUrl(network));

        switch (network) {
            case TESTNET: Network.useTestNetwork(); break;
            case MAINNET: Network.usePublicNetwork(); break;
        }

        /*
         * This little App takes the "Tokens on Stellar" Tutorial written by Jed McCaleb
         * URL: https://www.stellar.org/blog/tokens-on-stellar/
         */

        System.out.println("Token and Account creation started for " + network.name());

        // Step 1 – Create issuing account.
        final IAccount issuer = AccountFactory.create(network, AccountType.ISSUER);

        // Step 2 – Create distribution account.
        final IAccount distributor = AccountFactory.create(network, AccountType.DISTRIBUTOR);

        // Create Assed
        final Asset appAsset = Asset.createNonNativeAsset(config.getProperty("COIN_CODE"), issuer.getKeyPair());

        // Step 3 – Trust the issuing account
        Transaction trust = new Transaction.Builder(getAccountResponse(server, distributor, 3))
                .addOperation(
                        new ChangeTrustOperation.Builder(appAsset, config.getProperty("COIN_AMOUNT")).build())
                .build();   // the receiving account must trust the asset with maximum amount
        performTx(server, distributor.getKeyPair(), trust, 3);

        // Step 4 – Create tokens
        Transaction send = new Transaction.Builder(getAccountResponse(server, issuer, 4))
                .addOperation(
                        new PaymentOperation.Builder(distributor.getKeyPair(), appAsset, config.getProperty("COIN_AMOUNT")).build())
                .build();   // there has to be a payment to create the token. We send all from issuer to distributor
        performTx(server, issuer.getKeyPair(), send, 4);

        // Step 5 – Publish information about your token
        Gson gson = new Gson();
        TokenDescImpl appTokenDesc = new TokenDescImpl(
                config.getProperty("COIN_CODE"),
                config.getProperty("COIN_NAME"),
                config.getProperty("COIN_DESCRIPTION"),
                config.getProperty("COIN_CONDITIONS"));
        try {
            String filePath = "/tmp/appToken.json";
            FileWriter fileWriter = new FileWriter(filePath);
            gson.toJson(appTokenDesc, fileWriter);
            fileWriter.close();

            try {
                String[] keybaseLogin = new String[]{"keybase", "login", config.getProperty("KEYBASE_USER")};
                Process proc = new ProcessBuilder(keybaseLogin).start();
            } catch (IOException e) {
                System.out.println("Keybase not installed. We will continue for now with an unsigned document.");
            }

            try {
                String[] keybaseSign = new String[]{"keybase", "pgp", "sign", "-i", filePath};
                Process proc = new ProcessBuilder(keybaseSign).start();
            } catch (IOException e) {
                System.out.println("Sign failed. We dont care right now and continue to upload unsigned file.");
            }

            try {
                String[] keybaseSign = new String[]{"ipfs", "add", filePath};
                Process proc = new ProcessBuilder(keybaseSign).start();
                InputStream is = proc.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                final String[] split = reader.readLine().split(" ");
                if (split.length != 3) {
                    AppUtil.fatalError("IPFS upload failed. Exit.", 51);
                }
                String ipfsHash = split[1];
                System.out.println("IPFS location: https://ipfs.io/ipfs/" + ipfsHash);

                Transaction t = new Transaction.Builder(getAccountResponse(server, issuer, 5))
                        .addOperation(
                                new ManageDataOperation.Builder("issue", ipfsHash.getBytes(Charset.forName("UTF-8"))).build())
                        .build();   // there has to be a payment to create the token. We send all from issuer to distributor
                performTx(server, issuer.getKeyPair(), t, 5);
            } catch (IOException e) {
                AppUtil.fatalError("IPFS not installed. Exit.", 52);
            }
        } catch (IOException e) {
            // we could provide a solution without file creation ...
            // final String appTokenDescJson = gson.toJson(appTokenDesc);
            AppUtil.fatalError("Json file could not be created. Exit.", 53);
        }

        // Step 6 – Limit the supply
        Transaction limitSupply = new Transaction.Builder(getAccountResponse(server, issuer, 6))
                .addOperation(
                        new SetOptionsOperation.Builder().setMasterKeyWeight(0).build())
                .build();   // set master weight of issuer to lock him to max COIN_AMOUNT
        performTx(server, issuer.getKeyPair(), limitSupply, 6);
        System.out.println("Account locked. Take a look at " + StellarUtil.getNetworkUrl(network)+ "/accounts/" + issuer.getKeyPair().getAccountId());


        // Step 7 – Distribute your Token
        Transaction distribute = new Transaction.Builder(getAccountResponse(server, distributor, 6))
                .addOperation(
                        new ManageOfferOperation.Builder(appAsset, new AssetTypeNative(), config.getProperty("COIN_AMOUNT"), config.getProperty("COIN_INIT_SELL_PRICE")).build())
                .build();   // set master weight of issuer to lock him to max COIN_AMOUNT
        performTx(server, distributor.getKeyPair(), distribute, 7);
        System.out.println("---------------------------------");
        System.out.println("Token created.");
        if (network.equals(NetworkType.TESTNET)) {
            System.out.println("First open https://stellarterm.com/#testnet and open then in the same tab the following URL:");
        }
        System.out.println("You can now trade it on https://stellarterm.com/#exchange/" + config.getProperty("COIN_CODE") +"-" + issuer.getKeyPair().getAccountId() + "/XLM-native");

        // Step 8 - Create test Account which buys Tokens
        if (network.equals(NetworkType.TESTNET) && Boolean.valueOf(config.getProperty("USER_ENABLED"))) {
            final IAccount tester = AccountFactory.create(network, AccountType.USER);

            Transaction trustUser = new Transaction.Builder(getAccountResponse(server, tester, 3))
                    .addOperation(
                            new ChangeTrustOperation.Builder(appAsset, config.getProperty("COIN_AMOUNT")).build())
                    .build();   // the receiving account must trust the asset with maximum amount
            performTx(server, tester.getKeyPair(), trustUser, 8);

            Transaction offerBuy = new Transaction.Builder(getAccountResponse(server, tester, 8))
            .addOperation(
                    new ManageOfferOperation.Builder(new AssetTypeNative(), appAsset, config.getProperty("USER_OFFER_SELL_AMOUNT"), config.getProperty("USER_OFFER_PRICE_SELL")).build())
            .build();   // set master weight of issuer to lock him to max COIN_AMOUNT
            performTx(server, tester.getKeyPair(), offerBuy, 8);

            Transaction offerBid = new Transaction.Builder(getAccountResponse(server, tester, 8))
                    .addOperation(
                            new ManageOfferOperation.Builder(new AssetTypeNative(), appAsset, config.getProperty("USER_OFFER_SELL_AMOUNT"), config.getProperty("USER_OFFER_PRICE_SOLD")).build())
                    .build();   // set master weight of issuer to lock him to max COIN_AMOUNT
            performTx(server, tester.getKeyPair(), offerBid, 8);
            System.out.println("Checkout my balance: " + StellarUtil.getNetworkUrl(network)+ "/accounts/" + tester.getKeyPair().getAccountId());
        }
    }

    // export to method because of IOException
    private static AccountResponse getAccountResponse(Server server, IAccount account, Integer actualStep) {
        try {
            return server.accounts().account(account.getKeyPair());
        } catch (IOException e) {
            AppUtil.fatalError("Account not found int the stellar network.", actualStep);
        }
        // we never can get here because fatalError does exit, but compiler strangely complains.
        return null;
    }

    private static void performTx(Server server, KeyPair sKeyPair, Transaction t, Integer actualStep) {
        t.sign(sKeyPair);

        final SubmitTransactionResponse submitTransactionResponse;
        try {
            submitTransactionResponse = server.submitTransaction(t);
            if (!submitTransactionResponse.isSuccess()) {
                AppUtil.fatalError("Transaction was not successful.", actualStep);
            }
        } catch (IOException | NullPointerException e) {
            AppUtil.fatalError(e, actualStep);
        }

    }
}
