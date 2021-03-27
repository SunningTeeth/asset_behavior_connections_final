package com.lanysec.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author daijb
 * @date 2021/3/5 21:41
 */
public class SystemUtil {

    /**
     * 数据库用户名
     */
    public static String getMysqlUser() {
        return "root";
    }

    /**
     * 数据库密码
     */
    public static String getMysqlPassword() {
        return "Admin@123";
    }

    private static final String JDBC_URL = getJdbcUrl0();

    public static String getJdbcUrl() {
        return JDBC_URL;
    }

    private static String getJdbcUrl0() {
        return "jdbc:mysql://" + getHostIp0() + ":3306/csp?useEncoding=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
    }

    /**
     * 获取本地服务器IP地址
     */
    public static String getHostIp() {
        return HOST_IP;
    }

    private static final String HOST_IP = getHostIp0();

    private static String getHostIp0() {
        String hostIp = System.getProperty("mysql.servers");
        if (!StringUtil.isEmpty(hostIp)) {
            return hostIp;
        }
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return "192.168.3.101";
        }

        String result = null;
        //String hostname = getHostName();
        String hostname = null;
        if (!StringUtil.isEmpty(hostname)) {
            try {
                InetAddress addr = InetAddress.getByName(hostname);
                if (!addr.isLoopbackAddress() && !addr.isMulticastAddress()) {
                    result = addr.getHostAddress();
                }
            } catch (Throwable t) {
            }
        }

        if (result == null) {
            // 获取第一个不是127.0.0.1的地址
            List<String> ipv4Addrs = new ArrayList<>();
            List<String> ipv6Addrs = new ArrayList<>();
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface nic = en.nextElement();
                    for (Enumeration<InetAddress> en2 = nic.getInetAddresses(); en2.hasMoreElements(); ) {
                        InetAddress addr = en2.nextElement();
                        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isMulticastAddress()) {
                            continue;
                        }
                        if (addr instanceof Inet4Address) {
                            ipv4Addrs.add(addr.getHostAddress());
                        } else if (addr instanceof Inet6Address) {
                            ipv6Addrs.add(addr.getHostAddress());
                        }
                    }
                }
            } catch (Throwable t) {
            }
            if (ipv4Addrs.size() > 0) {
                result = ipv4Addrs.get(0);
            } else if (ipv6Addrs.size() > 0) {
                result = ipv6Addrs.get(0);
            }
        }
        return result;
    }

    public static void findMask() {
        String mask = "255.255.255.0";
        String ip = "192.168.126.2";
        String[] ipArray = ip.split("[.]");
        String[] maskArray = mask.split("[.]");

        String minIp = "";
        String maxIp = "";
        int subMask = 0;
        for (int i = 0; i < maskArray.length; i++) {
            int maskTmp = Integer.valueOf(maskArray[i]);
            int ipTmp = Integer.valueOf(ipArray[i]);
            int mi = maskTmp & ipTmp;

            if (ipTmp == mi && maskTmp == 255) {
                minIp = minIp + ipTmp + ".";
                maxIp = maxIp + ipTmp + ".";
                subMask = subMask + 8;
            } else {
                minIp = minIp + mi + ".";
                maxIp = maxIp + maxNetSeg(Integer.toBinaryString(maskTmp), mi) + ".";
                subMask = subMask + subMaskNum(Integer.toBinaryString(maskTmp));
            }
        }

        minIp = minIp.substring(0, minIp.length() - 1);
        maxIp = maxIp.substring(0, maxIp.length() - 1);
        System.out.println("minip " + minIp);
        System.out.println("maxip " + maxIp);
        System.out.println("mask " + subMask);
    }

    private static int calculate(int n) {
        if (n == 0)
            return 1;
        return 2 * calculate(n - 1);
    }

    private static Integer maxNetSeg(String s1, int mi) {
        s1 = new StringBuffer(s1).reverse().toString();

        if (s1.indexOf("1") != -1) {
            int i = s1.indexOf("1");
            return mi + calculate(i);
        } else {
            return 255;
        }

    }

    private static Integer subMaskNum(String s1) {
        if (s1.lastIndexOf("1") != -1) {
            int i = s1.lastIndexOf("1") + 1;
            return i;
        }
        return 0;

    }
}
