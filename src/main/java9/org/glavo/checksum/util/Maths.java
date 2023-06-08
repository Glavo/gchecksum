package org.glavo.checksum.util;

// https://github.com/OpenHFT/Zero-Allocation-Hashing/blob/ea/src/main/java/net/openhft/hashing/Maths.java
public final class Maths {
    private Maths() {
    }

    // Math.multiplyHigh() is intrinsified from JDK 10. But JDK 9 is out of life, we always prefer
    // this version to the scalar one.
    public static long unsignedLongMulXorFold(final long lhs, final long rhs) {
        final long upper = Math.multiplyHigh(lhs, rhs) + ((lhs >> 63) & rhs) + ((rhs >> 63) & lhs);
        final long lower = lhs * rhs;
        return lower ^ upper;
    }

    public static long unsignedLongMulHigh(final long lhs, final long rhs) {
        return Math.multiplyHigh(lhs, rhs) + ((lhs >> 63) & rhs) + ((rhs >> 63) & lhs);
    }
}
