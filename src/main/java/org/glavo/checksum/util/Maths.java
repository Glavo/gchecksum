package org.glavo.checksum.util;

// https://github.com/OpenHFT/Zero-Allocation-Hashing/blob/ea/src/main/java/net/openhft/hashing/Maths.java
public final class Maths {
    private Maths() {
    }

    public static long unsignedLongMulXorFold(final long lhs, final long rhs) {
        // The Grade School method of multiplication is a hair faster in Java, primarily used here
        // because the implementation is simpler.
        final long lhs_l = lhs & 0xFFFFFFFFL;
        final long lhs_h = lhs >>> 32;
        final long rhs_l = rhs & 0xFFFFFFFFL;
        final long rhs_h = rhs >>> 32;
        final long lo_lo = lhs_l * rhs_l;
        final long hi_lo = lhs_h * rhs_l;
        final long lo_hi = lhs_l * rhs_h;
        final long hi_hi = lhs_h * rhs_h;

        // Add the products together. This will never overflow.
        final long cross = (lo_lo >>> 32) + (hi_lo & 0xFFFFFFFFL) + lo_hi;
        final long upper = (hi_lo >>> 32) + (cross >>> 32) + hi_hi;
        final long lower = (cross << 32) | (lo_lo & 0xFFFFFFFFL);
        return lower ^ upper;
    }

    public static long unsignedLongMulHigh(final long lhs, final long rhs) {
        // The Grade School method of multiplication is a hair faster in Java, primarily used here
        // because the implementation is simpler.
        final long lhs_l = lhs & 0xFFFFFFFFL;
        final long lhs_h = lhs >>> 32;
        final long rhs_l = rhs & 0xFFFFFFFFL;
        final long rhs_h = rhs >>> 32;
        final long lo_lo = lhs_l * rhs_l;
        final long hi_lo = lhs_h * rhs_l;
        final long lo_hi = lhs_l * rhs_h;
        final long hi_hi = lhs_h * rhs_h;

        // Add the products together. This will never overflow.
        final long cross = (lo_lo >>> 32) + (hi_lo & 0xFFFFFFFFL) + lo_hi;
        final long upper = (hi_lo >>> 32) + (cross >>> 32) + hi_hi;
        return upper;
    }
}
