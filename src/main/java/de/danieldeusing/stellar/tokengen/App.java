package de.danieldeusing.stellar.tokengen;

import de.danieldeusing.stellar.tokengen.impl.AccountImpl;
import de.danieldeusing.stellar.tokengen.impl.TokenDescImpl;
import de.danieldeusing.stellar.tokengen.interfaces.IAccount;
import de.danieldeusing.stellar.tokengen.types.AccountType;
import de.danieldeusing.stellar.tokengen.types.NetworkType;
import de.danieldeusing.stellar.tokengen.util.AppUtil;
import de.danieldeusing.stellar.tokengen.util.StellarUtil;
import org.apache.commons.lang3.SystemUtils;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class App {

    public static void main(String[] args) {
        if (args.length < 4 || (args.length % 2 != 0)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters not valid. Usage:\n");
            sb.append(" -p ico.properties\n");
            sb.append(" -t presale (available types are 'presale', 'aftersale', 'directico')\n");
            sb.append("    if presale is selected, there will only be an issuer and an asset created with unlimited supply.\n");
            sb.append("    if aftersale is selected, the already sold tokens are calculated and the issuing account is locked to its COIN_AMOUNT supply.\n");
            sb.append("    if directico is selected, an issuer and all distribtion accounts are created.\n");
            sb.append(" -s optional if not aftersale: secret seed of issuer\n");
            sb.append(" -a optional if not aftersale: amount of assets already spent\n");
            AppUtil.fatalError(sb.toString(), 0);
        }

        Map<String, String> paramsMap = new HashMap<>();
        for (int i=0; i<args.length;i+=2) {
            switch (args[i]) {
                case "-p": paramsMap.put("properties", args[i+1]); break;
                case "-t": paramsMap.put("type", args[i+1]); break;
                case "-a": paramsMap.put("assets", args[i+1]); break;
                case "-s": paramsMap.put("secret", args[i+1]); break;
            }
        }

        if (paramsMap.get("type").equals("aftersale")
                && (paramsMap.get("secret") == null || paramsMap.get("assets") == null)) {
            AppUtil.fatalError("Type aftersale selected, but not ipub, iseed or assets given. Exit.", 0);
        }

        // check if we are on linux/mac else quit
        if ( !(SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) ) {
            AppUtil.fatalError("Operating system not supported by this App. Exit.", -1);
        }

        // init some global stuff
        Properties config = AppUtil.getProperties(paramsMap.get("properties"));
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

        System.out.println("Token and Account creation started for " + network.name() + " and token " + config.getProperty("COIN_NAME"));

        // Step 1 – Create issuing account.
        final IAccount issuer = getIssuer(paramsMap, network);

        // Create Asset
        final Asset appAsset = Asset.createNonNativeAsset(config.getProperty("COIN_CODE"), issuer.getKeyPair());
        final Asset btcAsset = Asset.createNonNativeAsset("BTC", issuer.getKeyPair());
        final Asset ethAsset = Asset.createNonNativeAsset("ETH", issuer.getKeyPair());

        //if (paramsMap.get("type").equals("afterpresale") || paramsMap.get("type").equals("directico")) {
            // Step 2 – Create distribution account.
        List<IAccount> distributionAccounts = new ArrayList<>();

        AppUtil.getCoinDistributionMap(config, paramsMap.get("assets")).forEach((k,v) -> {
            final IAccount account = AccountFactory.create(k, network, AccountType.DISTRIBUTOR);

            // Step 3 – Trust the issuing account
            // Step 4 – Create tokens
            createAccAndToken(server, account, appAsset, config, issuer, v, network);

            if (account.getName().equals("presale")) {
                // also trust BTC and ETH
                trust(server, account, btcAsset, config);
                trust(server, account, ethAsset, config);
            }

            distributionAccounts.add(account);
        });

        //} else {
        //    final IAccount initAccount = AccountFactory.create("initUser", network, AccountType.USER);
        //    createAccAndToken(server, initAccount, appAsset, config, issuer, "41", network);
        //}

        // Step 5 – Publish information about your token
        TokenDescImpl appTokenDesc = new TokenDescImpl(
                config.getProperty("COIN_CODE"),
                config.getProperty("COIN_NAME"),
                config.getProperty("COIN_DESCRIPTION"),
                config.getProperty("COIN_CONDITIONS"),
                config.getProperty("COIN_IMAGEURL"),
                issuer.getKeyPair().getAccountId(),
                config.getProperty("COIN_SHOWDECIMALS"));
        if (Boolean.valueOf(config.getProperty("APP_IPFS"))) {
            String keybase = config.getProperty("KEYBASE_USER");
            StellarUtil.ipfsMeta(appTokenDesc, server, issuer, keybase, 5);
        }
        if (Boolean.valueOf(config.getProperty("APP_TOML"))) {
            StellarUtil.tomlMeta(appTokenDesc, server, issuer, config.getProperty("APP_TOML_URL"), 5);
        }

        if (paramsMap.get("type").equals("afterpresale") || paramsMap.get("type").equals("directico")) {
            // Step 6 – Limit the supply
            Transaction limitSupply = new Transaction.Builder(StellarUtil.getAccountResponse(server, issuer, 6))
                    .addOperation(
                            new SetOptionsOperation.Builder().setMasterKeyWeight(0).build())
                    .build();   // set master weight of issuer to lock him to max COIN_AMOUNT
            StellarUtil.performTx(server, issuer.getKeyPair(), limitSupply, 6);
            System.out.println("Account locked. Take a look at " + StellarUtil.getNetworkUrl(network) + "/accounts/" + issuer.getKeyPair().getAccountId());
        }

        if (config.getProperty("COIN.DIRECT_DISTRIBUTION") != null && Boolean.valueOf(config.getProperty("COIN.DIRECT_DISTRIBUTION"))) {
            // Step 7 – Distribute your Token
            final Optional<IAccount> presale = distributionAccounts.stream().filter(d -> d.getName().equalsIgnoreCase("presale")).findFirst();
            if (!presale.isPresent()) {
                AppUtil.fatalError("No presale Account found. Exit", 7);
            }

            String balance = "0";

            try {
                final Optional<AccountResponse.Balance> coin_code = Arrays.stream(server.accounts().account(presale.get().getKeyPair()).getBalances()).filter(b -> b.getAssetCode().equals(config.getProperty("COIN_CODE"))).findFirst();
                if (coin_code.isPresent()) {
                    balance = coin_code.get().getBalance();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            DecimalFormat formatter = new DecimalFormat("#00000000000.0000000");
            Transaction dXlm = new Transaction.Builder(StellarUtil.getAccountResponse(server, presale.get(), 6))
                    .addOperation(
                            new ManageOfferOperation.Builder(appAsset, new AssetTypeNative(), String.valueOf(formatter.format(Double.valueOf(balance)/3)), config.getProperty("COIN_INIT_XLM_PRICE")).build())
                    .build();
            StellarUtil.performTx(server, presale.get().getKeyPair(), dXlm, 7);

            Transaction dBtc = new Transaction.Builder(StellarUtil.getAccountResponse(server, presale.get(), 6))
                    .addOperation(
                            new ManageOfferOperation.Builder(appAsset, btcAsset, String.valueOf(formatter.format(Double.valueOf(balance)/3)), config.getProperty("COIN_INIT_BTC_PRICE")).build())
                    .build();
            StellarUtil.performTx(server, presale.get().getKeyPair(), dBtc, 7);

            Transaction dEth = new Transaction.Builder(StellarUtil.getAccountResponse(server, presale.get(), 6))
                    .addOperation(
                            new ManageOfferOperation.Builder(appAsset, ethAsset, String.valueOf(formatter.format(Double.valueOf(balance)/3)), config.getProperty("COIN_INIT_ETH_PRICE")).build())
                    .build();
            StellarUtil.performTx(server, presale.get().getKeyPair(), dEth, 7);



            System.out.println("---------------------------------");
            System.out.println("Token created.");
            if (network.equals(NetworkType.TESTNET)) {
                System.out.println("First open https://stellarterm.com/#testnet and open then in the same tab the following URL:");
            }
            System.out.println("You can now trade it on https://stellarterm.com/#exchange/" + config.getProperty("COIN_CODE") + "-" + issuer.getKeyPair().getAccountId() + "/XLM-native");

            // Step 8 - Create test Account which buys Tokens
            if (network.equals(NetworkType.TESTNET) && Boolean.valueOf(config.getProperty("USER_ENABLED"))) {
                final IAccount tester = AccountFactory.create("TestUser", network, AccountType.USER);

                Transaction trustUser = new Transaction.Builder(StellarUtil.getAccountResponse(server, tester, 3))
                        .addOperation(
                                new ChangeTrustOperation.Builder(appAsset, config.getProperty("COIN_AMOUNT")).build())
                        .build();   // the receiving account must trust the asset with maximum amount
                StellarUtil.performTx(server, tester.getKeyPair(), trustUser, 8);

                Transaction offerBuy = new Transaction.Builder(StellarUtil.getAccountResponse(server, tester, 8))
                        .addOperation(
                                new ManageOfferOperation.Builder(new AssetTypeNative(), appAsset, config.getProperty("USER_OFFER_SELL_AMOUNT"), config.getProperty("USER_OFFER_PRICE_SELL")).build())
                        .build();   // set master weight of issuer to lock him to max COIN_AMOUNT
                StellarUtil.performTx(server, tester.getKeyPair(), offerBuy, 8);

                Transaction offerBid = new Transaction.Builder(StellarUtil.getAccountResponse(server, tester, 8))
                        .addOperation(
                                new ManageOfferOperation.Builder(new AssetTypeNative(), appAsset, config.getProperty("USER_OFFER_SELL_AMOUNT"), config.getProperty("USER_OFFER_PRICE_SOLD")).build())
                        .build();   // set master weight of issuer to lock him to max COIN_AMOUNT
                StellarUtil.performTx(server, tester.getKeyPair(), offerBid, 8);
                System.out.println("Checkout my balance: " + StellarUtil.getNetworkUrl(network) + "/accounts/" + tester.getKeyPair().getAccountId());
            }
        }
    }

    private static IAccount getIssuer(Map<String, String> paramsMap, NetworkType network) {
        IAccount issuer = null;
        if (paramsMap.get("type").equals("presale") || paramsMap.get("type").equals("directico")) {
            issuer = AccountFactory.create(AccountType.ISSUER.name().toLowerCase(), network, AccountType.ISSUER);
        } else {
            issuer = new AccountImpl(AccountType.ISSUER, KeyPair.fromSecretSeed(paramsMap.get("secret")));
        }
        return issuer;
    }

    private static void createAccAndToken(Server server, IAccount account, Asset appAsset, Properties config, IAccount issuer, String amount, NetworkType network) {
        // Step 3 – Trust the issuing account
        trust(server, account, appAsset, config);

        // Step 4 – Create tokens
        Transaction send = new Transaction.Builder(StellarUtil.getAccountResponse(server, issuer, 4))
                .addOperation(
                        new PaymentOperation.Builder(account.getKeyPair(), appAsset, amount).build())
                .build();   // there has to be a payment to create the token. We send all from issuer to distributor
        StellarUtil.performTx(server, issuer.getKeyPair(), send, 4);

        System.out.println("\tINFO: Sent " + amount + " from issuer to " + account.getName() + ".");
        System.out.println("\tINFO: Checkout my balance: " + StellarUtil.getNetworkUrl(network) + "/accounts/" + account.getKeyPair().getAccountId());
    }

    private static void trust(Server server, IAccount account, Asset asset, Properties config) {
        Transaction trust = new Transaction.Builder(StellarUtil.getAccountResponse(server, account, 3))
                .addOperation(
                        new ChangeTrustOperation.Builder(asset, config.getProperty("COIN_AMOUNT")).build())
                .build();   // the receiving account must trust the asset with maximum amount
        StellarUtil.performTx(server, account.getKeyPair(), trust, 3);
    }
}
