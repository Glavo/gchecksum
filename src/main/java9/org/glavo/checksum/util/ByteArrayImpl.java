package org.glavo.checksum.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

final class ByteArrayImpl {
    private ByteArrayImpl() {
    }

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    static int getIntLE(byte[] array, int offset) {
        return (int) INT.get(array, offset);
    }

    public static long getLongLE(byte[] array, int offset) {
        return (long) LONG.get(array, offset);
    }
}
