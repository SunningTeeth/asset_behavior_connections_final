package com.lanysec.utils;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

import java.util.UUID;

/**
 * @author daijb
 * @date 2021/3/5 21:41
 */
public class UUIDUtil {

    private static final TimeBasedGenerator UUID_GENERATOR = Generators.timeBasedGenerator(EthernetAddress.fromInterface());

    /**
     * 创建UUID, BASE58 编码, 22 bytes length
     */
    public static String genId()
    {
        UUID uuid = UUID_GENERATOR.generate();
        return compressedUUID(uuid);
    }

    /**
     * @param uuid UUID
     * @return
     */
    public static String compressedUUID(UUID uuid) {
        byte[] byUuid = new byte[16];
        long least = uuid.getLeastSignificantBits();
        long most = uuid.getMostSignificantBits();
        long2bytes(most, byUuid, 0);
        long2bytes(least, byUuid, 8);
        String compressUUID = encode(byUuid);
        return compressUUID;
    }

    /**
     * @param input
     * @return
     */
    public static String encode(byte[] input) {
        if (input.length == 0) {
            // paying with the same coin
            return "";
        }
        // Make a copy of the input since we are going to modify it.
        input = copyOfRange(input, 0, input.length);
        // Count leading zeroes
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0) {++zeroCount;}
        // The actual encoding
        byte[] temp = new byte[input.length * 2];
        int j = temp.length;
        int startAt = zeroCount;
        while (startAt < input.length) {
            byte mod = divmod58(input, startAt);
            if (input[startAt] == 0) {++startAt;}
            temp[--j] = (byte) ALPHABET[mod];
        }
        // Strip extra '1' if any
        while (j < temp.length && temp[j] == ALPHABET[0]) {++j;}
        // Add as many leading '1' as there were leading zeros.
        while (--zeroCount >= 0) {temp[--j] = (byte) ALPHABET[0];}
        byte[] output = copyOfRange(temp, j, temp.length);
        return new String(output);
    }
    private static final int BASE_256 = 256;
    private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int BASE_58 = ALPHABET.length;

    private static byte divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            int digit256 = number[i] & 0xFF;
            int temp = remainder * BASE_256 + digit256;
            number[i] = (byte) (temp / BASE_58);
            remainder = temp % BASE_58;
        }
        return (byte) remainder;
    }

    protected static void long2bytes(long value, byte[] bytes, int offset) {
        for (int i = 7; i > -1; i--) {
            bytes[offset++] = (byte) ((value >> 8 * i) & 0xFF);
        }
    }

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);
        return range;
    }
}
