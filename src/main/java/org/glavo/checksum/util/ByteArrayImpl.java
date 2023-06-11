/*
 * Copyright 2023 Glavo
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

final class ByteArrayImpl {
    private ByteArrayImpl() {
    }

    static short getShortLE(byte[] array, int offset) {
        int b0 = array[offset + 0] & 0xff;
        int b1 = array[offset + 1] & 0xff;

        return (short) ((b0 << 0) | (b1 << 8));
    }

    static int getIntLE(byte[] array, int offset) {
        int b0 = array[offset + 0] & 0xff;
        int b1 = array[offset + 1] & 0xff;
        int b2 = array[offset + 2] & 0xff;
        int b3 = array[offset + 3] & 0xff;

        return (b0 << 0) | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    static long getLongLE(byte[] array, int offset) {
        long b0 = array[offset + 0] & 0xffL;
        long b1 = array[offset + 1] & 0xffL;
        long b2 = array[offset + 2] & 0xffL;
        long b3 = array[offset + 3] & 0xffL;
        long b4 = array[offset + 4] & 0xffL;
        long b5 = array[offset + 5] & 0xffL;
        long b6 = array[offset + 6] & 0xffL;
        long b7 = array[offset + 7] & 0xffL;

        return (b0 << 0) | (b1 << 8) | (b2 << 16) | (b3 << 24) | (b4 << 32) | (b5 << 40) | (b6 << 48) | (b7 << 56);
    }
}
