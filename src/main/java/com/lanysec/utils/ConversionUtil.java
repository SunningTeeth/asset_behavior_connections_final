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
}
