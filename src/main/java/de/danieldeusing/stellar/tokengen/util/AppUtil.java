package de.danieldeusing.stellar.tokengen.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public abstract class AppUtil {
    public static Properties getProperties() {
        Properties config = new Properties();
        URL resource = AppUtil.class.getClassLoader().getResource("ico.properties");
        InputStream in = null;
        try {
            in = resource.openStream();
            config.load(in);
            in.close();
        } catch (IOException e) {
            AppUtil.fatalError(e, 0);
        }
        return config;
    }

    // We got an error which does not let us continue with the process
    public static void fatalError(Exception e, Integer code) {
        e.printStackTrace();
        System.exit(code);
    }

    // We got an error which does not let us continue with the process
    public static void fatalError(String error, Integer code) {
        System.out.println(error);
        System.exit(code);
    }
}
