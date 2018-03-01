package de.danieldeusing.stellar.tokengen.util;

import com.google.gson.Gson;
import de.danieldeusing.stellar.tokengen.impl.TokenDescImpl;
import de.danieldeusing.stellar.tokengen.interfaces.IAccount;
import de.danieldeusing.stellar.tokengen.types.NetworkType;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.*;
import java.nio.charset.Charset;

public abstract class StellarUtil {
    public static String getNetworkUrl(NetworkType type) {
        switch (type) {
            case TESTNET: return "https://horizon-testnet.stellar.org";
            case MAINNET: return "";
        }
        // default always is Testnet
        return "https://horizon-testnet.stellar.org";
    }

    // export to method because of IOException
    public static AccountResponse getAccountResponse(Server server, IAccount account, Integer actualStep) {
        try {
            return server.accounts().account(account.getKeyPair());
        } catch (IOException e) {
            AppUtil.fatalError("Account not found int the stellar network.", actualStep);
        }
        // we never can get here because fatalError does exit, but compiler strangely complains.
        return null;
    }

    public static void performTx(Server server, KeyPair sKeyPair, Transaction t, Integer actualStep) {
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

    public static void ipfsMeta(TokenDescImpl appTokenDesc, Server server, IAccount issuer, String keybase, Integer actualStep) {
        Gson gson = new Gson();

        try {
            String filePath = "/tmp/appToken.json";
            FileWriter fileWriter = new FileWriter(filePath);
            gson.toJson(appTokenDesc, fileWriter);
            fileWriter.close();

            try {
                String[] keybaseLogin = new String[]{"keybase", "login", keybase};
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
                String[] ipfsUp = new String[]{"ipfs", "add", filePath};
                Process proc = new ProcessBuilder(ipfsUp).start();
                InputStream is = proc.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                final String[] split = reader.readLine().split(" ");
                if (split.length != 3) {
                    AppUtil.fatalError("IPFS upload failed. Exit.", actualStep * 10 + 1);
                }
                String ipfsHash = split[1];
                System.out.println("IPFS location: https://ipfs.io/ipfs/" + ipfsHash);

                Transaction t = new Transaction.Builder(getAccountResponse(server, issuer, 5))
                        .addOperation(
                                new ManageDataOperation.Builder("issue", ipfsHash.getBytes(Charset.forName("UTF-8"))).build())
                        .build();   // there has to be a payment to create the token. We send all from issuer to distributor
                performTx(server, issuer.getKeyPair(), t, actualStep);
            } catch (IOException e) {
                AppUtil.fatalError("IPFS not installed. Exit.", actualStep * 10 + 2);
            }
        } catch (IOException e) {
            // we could provide a solution without file creation ...
            // final String appTokenDescJson = gson.toJson(appTokenDesc);
            AppUtil.fatalError("Json file could not be created. Exit.", actualStep * 10 + 3);
        }
    }

    public static void tomlMeta(TokenDescImpl appTokenDesc, Server server, IAccount issuer, String tomlUrl, Integer actualStep) {
        if (tomlUrl == null) {
            AppUtil.fatalError("No tomlUrl given although toml was set to true. Exit.", actualStep * 10 + 1);
        }

        // this has to be done by the user itself, so we only print an output which the user can copy&paste
        StringBuilder sb = new StringBuilder();
        sb.append("--------- TOML File Content ------------\n");
        sb.append("[[CURRENCIES]]\n");
        sb.append("code=\"");
        sb.append(appTokenDesc.getCode());
        sb.append("\"\n");
        sb.append("issuer=\"");
        sb.append(appTokenDesc.getIssuerPub());
        sb.append("\"\n");
        sb.append("display_decimals=");
        sb.append(appTokenDesc.getDecimals());
        sb.append("\n");
        sb.append("name=\"");
        sb.append(appTokenDesc.getName());
        sb.append("\"\n");
        sb.append("desc=\"");
        sb.append(appTokenDesc.getDescription());
        sb.append("\"\n");
        sb.append("conditions=\"");
        sb.append(appTokenDesc.getConditions());
        sb.append("\"\n");
        sb.append("image=\"");
        sb.append(appTokenDesc.getImgURL());
        sb.append("\"\n");
        sb.append("---------------------------------------\n");
        sb.append("For more information about toml Files: https://www.stellar.org/developers/guides/concepts/stellar-toml.html");

        System.out.println(sb.toString());

        final AccountResponse accountResponse = getAccountResponse(server, issuer, actualStep);
        Transaction setHomeDomain = new Transaction.Builder(accountResponse)
                .addOperation(new SetOptionsOperation.Builder()
                    .setHomeDomain(tomlUrl).build())
                .build();
        performTx(server, issuer.getKeyPair(), setHomeDomain, actualStep);
    }
}
