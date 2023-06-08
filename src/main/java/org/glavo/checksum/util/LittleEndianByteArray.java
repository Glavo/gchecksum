package org.glavo.checksum.util;

public final class LittleEndianByteArray {
    private LittleEndianByteArray() {
    }

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
        return ByteArrayImpl.getIntLE(array, offset);
    }

    public static int getInt(byte[] array, long offset) {
        return ByteArrayImpl.getIntLE(array, (int) offset);
    }

    public static long getUnsignedInt(byte[] array, int offset) {
        return ((long) getInt(array, offset)) & 0xffffffffL;
    }

    public static long getUnsignedInt(byte[] array, long offset) {
        return ((long) getInt(array, offset)) & 0xffffffffL;
    }

    public static long getLong(byte[] array, int offset) {
        return ByteArrayImpl.getLongLE(array, offset);
    }

    public static long getLong(byte[] array, long offset) {
        return ByteArrayImpl.getLongLE(array, (int) offset);
    }
}
