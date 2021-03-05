package com.lanysec.utils;

/**
 * @author daijb
 * @date 2021/3/5 21:40
 */
public class StringUtil {

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0 || "null".equals(str);
    }

    /**
     * 大小写敏感比较
     */
    public static boolean equals(String str1, String str2) {
        boolean str1e = isEmpty(str1);
        boolean str2e = isEmpty(str2);
        //both empty
        if (str1e && str2e) {
            return true;
        }
        if (str1e != str2e) {
            return false;
        }
        //both not empty
        return str1.equals(str2);
    }

    /**
     * 大小写无关比较
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        boolean str1e = isEmpty(str1);
        boolean str2e = isEmpty(str2);
        //both empty
        if (str1e && str2e) {
            return true;
        }
        if (str1e != str2e) {
            return false;
        }
        return str1.equalsIgnoreCase(str2);
    }
}
