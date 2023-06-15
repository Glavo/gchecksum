/*
 * Copyright 2021 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glavo.checksum.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

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
        return new String(out, 0, outLength, ISO_8859_1);
    }

    public static String encodeHex32(long data) {
        byte[] out = new byte[8];

        out[0] = DIGITS_LOWER[(int) ((data >>> 28) & 0x0f)];
        out[1] = DIGITS_LOWER[(int) ((data >>> 24) & 0x0f)];
        out[2] = DIGITS_LOWER[(int) ((data >>> 20) & 0x0f)];
        out[3] = DIGITS_LOWER[(int) ((data >>> 16) & 0x0f)];
        out[4] = DIGITS_LOWER[(int) ((data >>> 12) & 0x0f)];
        out[5] = DIGITS_LOWER[(int) ((data >>>  8) & 0x0f)];
        out[6] = DIGITS_LOWER[(int) ((data >>>  4) & 0x0f)];
        out[7] = DIGITS_LOWER[(int) ((data >>>  0) & 0x0f)];

        return new String(out, 0, 8, ISO_8859_1);
    }

    public static String encodeHex(long data) {
        byte[] out = new byte[16];

        out[0]  = DIGITS_LOWER[(int) ((data >>> 60) & 0x0f)];
        out[1]  = DIGITS_LOWER[(int) ((data >>> 56) & 0x0f)];
        out[2]  = DIGITS_LOWER[(int) ((data >>> 52) & 0x0f)];
        out[3]  = DIGITS_LOWER[(int) ((data >>> 48) & 0x0f)];
        out[4]  = DIGITS_LOWER[(int) ((data >>> 44) & 0x0f)];
        out[5]  = DIGITS_LOWER[(int) ((data >>> 40) & 0x0f)];
        out[6]  = DIGITS_LOWER[(int) ((data >>> 36) & 0x0f)];
        out[7]  = DIGITS_LOWER[(int) ((data >>> 32) & 0x0f)];
        out[8]  = DIGITS_LOWER[(int) ((data >>> 28) & 0x0f)];
        out[9]  = DIGITS_LOWER[(int) ((data >>> 24) & 0x0f)];
        out[10] = DIGITS_LOWER[(int) ((data >>> 20) & 0x0f)];
        out[11] = DIGITS_LOWER[(int) ((data >>> 16) & 0x0f)];
        out[12] = DIGITS_LOWER[(int) ((data >>> 12) & 0x0f)];
        out[13] = DIGITS_LOWER[(int) ((data >>>  8) & 0x0f)];
        out[14] = DIGITS_LOWER[(int) ((data >>>  4) & 0x0f)];
        out[15] = DIGITS_LOWER[(int) ((data >>>  0) & 0x0f)];

        return new String(out, 0, 16, ISO_8859_1);
    }

    public static String encodeHex(long lo, long hi) {
        byte[] out = new byte[32];

        out[0]  = DIGITS_LOWER[(int) ((hi >>> 60) & 0x0f)];
        out[1]  = DIGITS_LOWER[(int) ((hi >>> 56) & 0x0f)];
        out[2]  = DIGITS_LOWER[(int) ((hi >>> 52) & 0x0f)];
        out[3]  = DIGITS_LOWER[(int) ((hi >>> 48) & 0x0f)];
        out[4]  = DIGITS_LOWER[(int) ((hi >>> 44) & 0x0f)];
        out[5]  = DIGITS_LOWER[(int) ((hi >>> 40) & 0x0f)];
        out[6]  = DIGITS_LOWER[(int) ((hi >>> 36) & 0x0f)];
        out[7]  = DIGITS_LOWER[(int) ((hi >>> 32) & 0x0f)];
        out[8]  = DIGITS_LOWER[(int) ((hi >>> 28) & 0x0f)];
        out[9]  = DIGITS_LOWER[(int) ((hi >>> 24) & 0x0f)];
        out[10] = DIGITS_LOWER[(int) ((hi >>> 20) & 0x0f)];
        out[11] = DIGITS_LOWER[(int) ((hi >>> 16) & 0x0f)];
        out[12] = DIGITS_LOWER[(int) ((hi >>> 12) & 0x0f)];
        out[13] = DIGITS_LOWER[(int) ((hi >>>  8) & 0x0f)];
        out[14] = DIGITS_LOWER[(int) ((hi >>>  4) & 0x0f)];
        out[15] = DIGITS_LOWER[(int) ((hi >>>  0) & 0x0f)];

        out[16] = DIGITS_LOWER[(int) ((lo >>> 60) & 0x0f)];
        out[17] = DIGITS_LOWER[(int) ((lo >>> 56) & 0x0f)];
        out[18] = DIGITS_LOWER[(int) ((lo >>> 52) & 0x0f)];
        out[19] = DIGITS_LOWER[(int) ((lo >>> 48) & 0x0f)];
        out[20] = DIGITS_LOWER[(int) ((lo >>> 44) & 0x0f)];
        out[21] = DIGITS_LOWER[(int) ((lo >>> 40) & 0x0f)];
        out[22] = DIGITS_LOWER[(int) ((lo >>> 36) & 0x0f)];
        out[23] = DIGITS_LOWER[(int) ((lo >>> 32) & 0x0f)];
        out[24] = DIGITS_LOWER[(int) ((lo >>> 28) & 0x0f)];
        out[25] = DIGITS_LOWER[(int) ((lo >>> 24) & 0x0f)];
        out[26] = DIGITS_LOWER[(int) ((lo >>> 20) & 0x0f)];
        out[27] = DIGITS_LOWER[(int) ((lo >>> 16) & 0x0f)];
        out[28] = DIGITS_LOWER[(int) ((lo >>> 12) & 0x0f)];
        out[29] = DIGITS_LOWER[(int) ((lo >>>  8) & 0x0f)];
        out[30] = DIGITS_LOWER[(int) ((lo >>>  4) & 0x0f)];
        out[31] = DIGITS_LOWER[(int) ((lo >>>  0) & 0x0f)];

        return new String(out, 0, 32, ISO_8859_1);
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
