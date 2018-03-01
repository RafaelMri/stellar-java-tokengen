package de.danieldeusing.stellar.tokengen.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AppUtil {
    public static Properties getProperties(String filename) {
        Properties config = new Properties();
        URL resource = AppUtil.class.getClassLoader().getResource(filename);
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

    public static Map<String, String> getCoinDistributionMap(Properties config, String amountsOfAssetsSpent) {
        Map<String, String> coinDistributionMap = new HashMap<>();

        final Long coinAmount = getCoinAmount(config, amountsOfAssetsSpent);
        config.stringPropertyNames().stream()
                .filter(c -> c.startsWith("COIN_DISTRIBUTION."))
                .forEach(c -> {
                    final Double percentage = Double.valueOf(config.getProperty(c).replace("%", ""));
                    if (percentage > 0)
                        coinDistributionMap.put(c.replace("COIN_DISTRIBUTION.", "").toLowerCase(), String.valueOf(coinAmount * (percentage / 100)));
                });

        // config did not provied any distribution details, all coins will be put to distribution account
        if (coinDistributionMap.size() == 0) {
            coinDistributionMap.put("distribution", String.valueOf(coinAmount));
        }

        final double sum = coinDistributionMap.values().stream().mapToDouble(Double::parseDouble).sum();
        if (sum < coinAmount) {
            double r = coinAmount - sum;
            String distribution = coinDistributionMap.get("distribution");
            coinDistributionMap.put("distribution", (distribution!=null) ? String.valueOf(Double.valueOf(distribution) + r) : String.valueOf(r));
        }

        return coinDistributionMap;
    }

    private static Long getCoinAmount(Properties config, String amountsOfAssetsSpent) {
        Long coinAmount = Long.valueOf(config.getProperty("COIN_AMOUNT"));

        if (amountsOfAssetsSpent != null) {
            coinAmount = coinAmount - Long.valueOf(amountsOfAssetsSpent);
        }
        return coinAmount;
    }
}
