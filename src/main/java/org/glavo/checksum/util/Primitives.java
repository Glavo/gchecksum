package org.glavo.checksum.util;

import java.nio.ByteOrder;

public final class Primitives {

    private Primitives() {
    }

    static final boolean NATIVE_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    public static long unsignedInt(int i) {
        return i & 0xFFFFFFFFL;
    }

    public static int unsignedShort(int s) {
        return s & 0xFFFF;
    }

    public static int unsignedByte(int b) {
        return b & 0xFF;
    }
}
