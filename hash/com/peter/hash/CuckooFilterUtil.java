package com.peter.hash;

/**
 * Created by ylkang on 2/25/16.
 */
public class CuckooFilterUtil {

    public static boolean isSame(byte[] a, byte[] b) {
        if (a == b) {
            return true;
        }
        if ((a == null && b != null) || (a != null && b == null)) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}
