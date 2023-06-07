package org.glavo.checksum.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public final class LittleEndianByteArray {
    private LittleEndianByteArray() {
    }

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    public static int getInt(byte[] array, int offset) {
        return (int) INT.get(array, offset);
    }

    public static long getLong(byte[] array, int offset) {
        return (long) LONG.get(array, offset);
    }
}
