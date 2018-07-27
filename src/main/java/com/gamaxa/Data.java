package com.gamaxa;

import com.wavesplatform.wavesj.Asset;

public class Data {
    private static String GAX_ID = "2m4SEz8Bq9zgjBXF89Z1FQTkrUY6JYnvu9YFhitkBW51";

    public static String getAssetId(boolean test) {
        if (test) {
            return Asset.WAVES;
        }
        return GAX_ID;
    }
}
