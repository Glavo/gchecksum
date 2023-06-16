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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public final class LittleEndianByteArray {
    private LittleEndianByteArray() {
    }

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, LITTLE_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN);

    public static byte getByte(byte[] array, int offset) {
        return array[offset];
    }

    public static byte getByte(byte[] array, long offset) {
        return array[(int) offset];
    }

    public static int getUnsignedByte(byte[] array, int offset) {
        return array[offset] & 0xff;
    }

    public static int getUnsignedByte(byte[] array, long offset) {
        return array[(int) offset] & 0xff;
    }

    public static int getInt(byte[] array, int offset) {
        return (int) INT.get(array, offset);
    }

    public static int getInt(byte[] array, long offset) {
        return getInt(array, (int) offset);
    }

    public static long getUnsignedInt(byte[] array, int offset) {
        return ((long) getInt(array, offset)) & 0xffffffffL;
    }

    public static long getUnsignedInt(byte[] array, long offset) {
        return ((long) getInt(array, offset)) & 0xffffffffL;
    }

    public static long getLong(byte[] array, int offset) {
        return (long) LONG.get(array, offset);
    }

    public static long getLong(byte[] array, long offset) {
        return getLong(array, (int) offset);
    }
}
