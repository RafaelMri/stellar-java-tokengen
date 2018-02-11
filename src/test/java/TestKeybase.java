import com.google.gson.Gson;
import de.danieldeusing.stellar.tokengen.util.AppUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Properties;
import java.util.UUID;

public class TestKeybase {

    @Test
    public void testLogin() {
        final Properties properties = AppUtil.getProperties();
        String[] keybaseLogin = new String[] {"keybase", "login", properties.getProperty("KEYBASE_USER")};
        try {
            Process proc = new ProcessBuilder(keybaseLogin).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIPFSUpload() {
        final Properties properties = AppUtil.getProperties();

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
