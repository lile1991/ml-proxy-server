package io.ml.proxy.utils.lang;

public class StringUtils {
    public static boolean isNotBlack(String str) {
        return !isBlack(str);
    }
    public static boolean isBlack(String str) {
        return str == null || str.isEmpty() || str.trim().isEmpty();
    }
}
