package com.gamaxa;

import com.wavesplatform.wavesj.Asset;

public class Data {
    private static String GAX_ID = "2m4SEz8Bq9zgjBXF89Z1FQTkrUY6JYnvu9YFhitkBW51";
    private static String NODE_URL = "https://nodes.wavesplatform.com";

    public static String getUrl(String amount, String recipient) {
        return "https://client.wavesplatform.com/#send/" + getAssetId(false) + "?recipient=" + recipient + "&amount=" + amount;
    }

    public static String getAssetId(boolean test) {
        if (test) {
            return Asset.WAVES;
        }
        return GAX_ID;
    }

    public static String getNodeUrl(boolean test) {
        return NODE_URL;
    }
}
