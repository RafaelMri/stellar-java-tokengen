import com.google.gson.Gson;
import de.danieldeusing.stellar.tokengen.impl.AccountImpl;
import de.danieldeusing.stellar.tokengen.interfaces.IAccount;
import de.danieldeusing.stellar.tokengen.types.AccountType;
import de.danieldeusing.stellar.tokengen.util.AppUtil;
import de.danieldeusing.stellar.tokengen.util.StellarUtil;
import org.junit.Assert;
import org.junit.Test;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.UUID;

public class TestKeybase {

    @Test
    public void testLogin() {
        final Properties properties = AppUtil.getProperties("ico.properties");
        String[] keybaseLogin = new String[] {"keybase", "login", properties.getProperty("KEYBASE_USER")};
        try {
            Process proc = new ProcessBuilder(keybaseLogin).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIPFSUpload() {
        final Properties properties = AppUtil.getProperties("ico.properties");

        final String s = UUID.randomUUID().toString();
        final String filePath = "/tmp/" + s + ".json";
        Gson gson = new Gson();
        try {
            String s1 = gson.toJson(new X(s));
            final FileWriter fileWriter = new FileWriter(filePath);
            gson.toJson(new X(s), fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] keybaseLogin = new String[] {"ipfs", "adds", filePath};
        try {
            Process proc = new ProcessBuilder(keybaseLogin).start();
            InputStream is = proc.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String s1 = reader.readLine();
            Assert.assertNotNull(s1);
            Assert.assertTrue(Integer.valueOf(s1).equals(3));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testKeyPairs() {
        KeyPair k = KeyPair.fromSecretSeed("SAX2XT4HMW7O6WAQQFEQSAQROJ7M5HRNT2LJ5OVRMX5JZE7S6TM7NARM");
        System.out.println(k.getAccountId());
        System.out.println(k.getSecretSeed());
    }

    @Test
    public void createTestAccount() throws MalformedURLException, IOException {
        Network.useTestNetwork();

        KeyPair pair = KeyPair.random();
        System.out.println(new String(pair.getSecretSeed()));
        System.out.println(pair.getAccountId());

        KeyPair feeder = KeyPair.fromSecretSeed("SA3W53XXG64ITFFIYQSBIJDG26LMXYRIMEVMNQMFAQJOYCZACCYBA34L");

        Server server = new Server("https://horizon-testnet.stellar.org");
        checkBalance(feeder, server);

        IAccount feed = new AccountImpl(AccountType.USER, feeder);
        Transaction trustUser = new Transaction.Builder(StellarUtil.getAccountResponse(server, feed, 3))
                .addOperation(
                        new CreateAccountOperation.Builder(pair, "1000").build())
                .build();   // the receiving account must trust the asset with maximum amount
        StellarUtil.performTx(server, feeder, trustUser, 8);

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        checkBalance(pair, server);

        // friendbot down
        /*String friendbotUrl = String.format(
                "https://horizon-testnet.stellar.org/friendbot?addr=%s",
                pair.getAccountId());
        InputStream response = new URL(friendbotUrl).openStream();
        String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();

        System.out.println("SUCCESS! You have a new account :)\n" + body);*/
    }

    private static void checkBalance(KeyPair pair, Server server) {
        AccountResponse account = null;
        try {
            account = server.accounts().account(pair);
            System.out.println("Balances for account " + pair.getAccountId());
            for (AccountResponse.Balance balance : account.getBalances()) {
                System.out.println(String.format(
                        "Type: %s, Code: %s, Balance: %s",
                        balance.getAssetType(),
                        balance.getAssetCode(),
                        balance.getBalance()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class X {
        private String test;

        public X(String test) {
            this.test = test;
        }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }
    }
}
