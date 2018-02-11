package de.danieldeusing.stellar.tokengen.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public abstract class AppUtil {
    public static Properties getProperties() {
        Properties config = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream("conf/tokengen.properties");
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
