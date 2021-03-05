package com.lanysec.utils;

/**
 * @author daijb
 * @date 2021/3/5 21:40
 */
public class ConversionUtil {

    public static String toString(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    public static Integer toInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        String str = obj.toString();
        if (StringUtil.isEmpty(str)) {
            return null;
        }
        if (str.contains(".")) {
            return (int) Double.parseDouble(str);
        }
        return Integer.parseInt(str.trim());
    }

    public static boolean toBoolean(Object obj) {
        return toBoolean(obj, false);
    }

    public static boolean toBoolean(Object obj, boolean defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Boolean) {
            return ((Boolean) obj).booleanValue();
        } else if (obj.getClass() == boolean.class) {
            return ((Boolean) obj).booleanValue();
        }
        String str = obj.toString().trim();
        if (StringUtil.equalsIgnoreCase("true", str)
                || StringUtil.equalsIgnoreCase("yes", str)
                || StringUtil.equalsIgnoreCase("1", str)
        ) {
            return true;
        }
        if (StringUtil.equalsIgnoreCase("false", str)
                || StringUtil.equalsIgnoreCase("no", str)
                || StringUtil.equalsIgnoreCase("0", str)
        ) {
            return false;
        }
        return defaultValue;
    }

    public static long str2seconds(String str) {
        if (StringUtil.isEmpty(str)) {
            return 0;
        }
        int begin = 0;
        int cursor = 1;
        long seconds = 0;
        while (cursor < str.length()) {
            if (str.charAt(cursor) >= 'a' && str.charAt(cursor) <= 'z') {
                cursor++;
                String sstr = str.substring(begin, cursor);
                seconds += str2seconds0(sstr);
                begin = cursor;
            }
            cursor++;
        }
        if (begin < str.length()) {
            seconds += str2seconds0(str.substring(begin));
        }
        return seconds;
    }

    /**
     * Convert 1s, 1 to 1 seconds, 1m to 60 seconds, 1h to 3600 seconds, 1d to ***
     */
    private static long str2seconds0(String str) {
        if (StringUtil.isEmpty(str)) {
            return 0;
        }
        long unit = 1;
        if (str.endsWith("s")) {
            str = str.substring(0, str.length() - 1);
        } else if (str.endsWith("m")) {
            unit = 60;
            str = str.substring(0, str.length() - 1);
        } else if (str.endsWith("h")) {
            unit = 60 * 60;
            str = str.substring(0, str.length() - 1);
        } else if (str.endsWith("d")) {
            unit = 60 * 60 * 24;
            str = str.substring(0, str.length() - 1);
        } else if (str.endsWith("M")) {
            unit = 30 * 24 * 60 * 60;
            str = str.substring(0, str.length() - 1);
        } else if (str.endsWith("q")) {
            unit = 4 * 30 * 24 * 60 * 60;
            str = str.substring(0, str.length() - 1);
        }
        return ((long) (Double.parseDouble(str))) * unit;
    }
}
