package org.glavo.checksum.util;

import java.nio.charset.StandardCharsets;

public final class Utils {
    private static final byte[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String encodeHex(byte[] data) {
        final int dataLength = data.length;
        final int outLength = dataLength << 1;

        final byte[] out = new byte[outLength];
        for (int i = 0, j = 0; i < dataLength; i++) {
            out[(j++)] = DIGITS_LOWER[((0xf0 & data[i]) >>> 4)];
            out[(j++)] = DIGITS_LOWER[(0x0f & data[i])];
        }
        return new String(out, 0, outLength, StandardCharsets.US_ASCII);
    }

    public static String encodeHex32(long data) {
        byte[] out = new byte[8];

        out[0] = DIGITS_LOWER[(int) ((data >>> 28) & 0x0f)];
        out[1] = DIGITS_LOWER[(int) ((data >>> 24) & 0x0f)];
        out[2] = DIGITS_LOWER[(int) ((data >>> 20) & 0x0f)];
        out[3] = DIGITS_LOWER[(int) ((data >>> 16) & 0x0f)];
        out[4] = DIGITS_LOWER[(int) ((data >>> 12) & 0x0f)];
        out[5] = DIGITS_LOWER[(int) ((data >>> 8) & 0x0f)];
        out[6] = DIGITS_LOWER[(int) ((data >>> 4) & 0x0f)];
        out[7] = DIGITS_LOWER[(int) ((data >>> 0) & 0x0f)];

        return new String(out, 0, 8, StandardCharsets.US_ASCII);
    }

    public static Pair<String, String> spiltRecord(String r) {
        final int rLength = r.length();

        final int idx = r.indexOf(' ');
        if (idx == -1 || idx == rLength - 1) {
            return null;
        }

        for (int i = idx + 1; i < rLength; i++) {
            if (r.charAt(i) != ' ') {
                return new Pair<>(r.substring(0, idx), r.substring(i));
            }
        }
        return null;
    }

}
