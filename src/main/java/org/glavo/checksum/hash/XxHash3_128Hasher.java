package org.glavo.checksum.hash;

import org.glavo.checksum.util.LittleEndianByteArray;
import org.glavo.checksum.util.Maths;
import org.glavo.checksum.util.Primitives;
import org.glavo.checksum.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

final class XxHash3_128Hasher extends XxHash3Hasher {

    static final XxHash3_128Hasher DEFAULT = new XxHash3_128Hasher(0L);

    XxHash3_128Hasher(long seed) {
        super(16, seed);
    }

    @Override
    protected String hashImpl(ByteChannel channel, ByteBuffer buffer, long firstRead) throws IOException {
        byte[] array = buffer.array();

        if (firstRead <= 16) {
            // XXH3_len_0to16_128b
            if (firstRead > 8) {
                // XXH3_len_9to16_128b
                final long bitflipl = (LittleEndianByteArray.getLong(XXH3_kSecret, 32) ^ LittleEndianByteArray.getLong(XXH3_kSecret, 40)) - seed;
                final long bitfliph = (LittleEndianByteArray.getLong(XXH3_kSecret, 48) ^ LittleEndianByteArray.getLong(XXH3_kSecret, 56)) + seed;
                long input_hi = LittleEndianByteArray.getLong(array, firstRead - 8);
                final long input_lo = LittleEndianByteArray.getLong(array, 0) ^ input_hi ^ bitflipl;
                long m128_lo = input_lo * XXH_PRIME64_1;
                long m128_hi = Maths.unsignedLongMulHigh(input_lo, XXH_PRIME64_1);
                m128_lo += (firstRead - 1) << 54;
                input_hi ^= bitfliph;
                m128_hi += input_hi + Primitives.unsignedInt((int) input_hi) * (XXH_PRIME32_2 - 1);
                m128_lo ^= Long.reverseBytes(m128_hi);

                final long low = XXH3_avalanche(m128_lo * XXH_PRIME64_2);
                final long high = XXH3_avalanche(Maths.unsignedLongMulHigh(m128_lo, XXH_PRIME64_2) + m128_hi * XXH_PRIME64_2);

                return Utils.encodeHex(low, high);
            }
            if (firstRead >= 4) {
                // XXH3_len_4to8_128b
                long s = seed ^ Long.reverseBytes(seed & 0xffffffffL);
                final long input_lo = LittleEndianByteArray.getUnsignedInt(array, 0);
                final long input_hi = (long) LittleEndianByteArray.getInt(array, firstRead - 4); // high int will be shifted

                final long bitflip = (LittleEndianByteArray.getLong(XXH3_kSecret, 16) ^ LittleEndianByteArray.getLong(XXH3_kSecret, 24)) + s;
                final long keyed = (input_lo + (input_hi << 32)) ^ bitflip;
                final long pl = XXH_PRIME64_1 + (firstRead << 2); /* Shift len to the left to ensure it is even, this avoids even multiplies. */
                long m128_lo = keyed * pl;
                long m128_hi = Maths.unsignedLongMulHigh(keyed, pl);
                m128_hi += (m128_lo << 1);
                m128_lo ^= (m128_hi >>> 3);

                m128_lo ^= m128_lo >>> 35;
                m128_lo *= 0x9FB21C651E98DF25L;
                m128_lo ^= m128_lo >>> 28;

                return Utils.encodeHex(m128_lo, XXH3_avalanche(m128_hi));
            }
            if (firstRead != 0) {
                // XXH3_len_1to3_128b
                final int c1 = LittleEndianByteArray.getUnsignedByte(array, 0);
                final int c2 = LittleEndianByteArray.getByte(array, (firstRead >> 1)); // high 3 bytes will be shifted
                final int c3 = LittleEndianByteArray.getUnsignedByte(array, firstRead - 1);
                final int combinedl = (c1 << 16) | (c2 << 24) | c3 | ((int) firstRead << 8);
                final int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), 13);
                final long bitflipl = Primitives.unsignedInt(LittleEndianByteArray.getInt(XXH3_kSecret, 0) ^ LittleEndianByteArray.getInt(XXH3_kSecret, 4)) + seed;
                final long bitfliph = Primitives.unsignedInt(LittleEndianByteArray.getInt(XXH3_kSecret, 8) ^ LittleEndianByteArray.getInt(XXH3_kSecret, 12)) - seed;

                final long low = XXH64_avalanche(Primitives.unsignedInt(combinedl) ^ bitflipl);
                final long high = XXH64_avalanche(Primitives.unsignedInt(combinedh) ^ bitfliph);

                return Utils.encodeHex(low, high);
            }
            final long low = XXH64_avalanche(seed ^ LittleEndianByteArray.getLong(XXH3_kSecret, 64) ^ LittleEndianByteArray.getLong(XXH3_kSecret, 72));
            final long high = XXH64_avalanche(seed ^ LittleEndianByteArray.getLong(XXH3_kSecret, 80) ^ LittleEndianByteArray.getLong(XXH3_kSecret, 88));
            return Utils.encodeHex(low, high);
        }
        if (firstRead <= 128) {
            // XXH3_len_17to128_128b
            long acc0 = firstRead * XXH_PRIME64_1;
            long acc1 = 0;
            if (firstRead > 32) {
                if (firstRead > 64) {
                    if (firstRead > 96) {
                        final long input0 = LittleEndianByteArray.getLong(array, 48);
                        final long input1 = LittleEndianByteArray.getLong(array, 48 + 8);
                        final long input2 = LittleEndianByteArray.getLong(array, firstRead - 64);
                        final long input3 = LittleEndianByteArray.getLong(array, firstRead - 64 + 8);
                        acc0 = XXH128_mix32B_once(seed, 96, acc0, input0, input1, input2, input3);
                        acc1 = XXH128_mix32B_once(seed, 96 + 16, acc1, input2, input3, input0, input1);
                    }
                    final long input0 = LittleEndianByteArray.getLong(array, 32);
                    final long input1 = LittleEndianByteArray.getLong(array, 32 + 8);
                    final long input2 = LittleEndianByteArray.getLong(array, firstRead - 48);
                    final long input3 = LittleEndianByteArray.getLong(array, firstRead - 48 + 8);
                    acc0 = XXH128_mix32B_once(seed, 64, acc0, input0, input1, input2, input3);
                    acc1 = XXH128_mix32B_once(seed, 64 + 16, acc1, input2, input3, input0, input1);
                }
                final long input0 = LittleEndianByteArray.getLong(array, 16);
                final long input1 = LittleEndianByteArray.getLong(array, 16 + 8);
                final long input2 = LittleEndianByteArray.getLong(array, firstRead - 32);
                final long input3 = LittleEndianByteArray.getLong(array, firstRead - 32 + 8);
                acc0 = XXH128_mix32B_once(seed, 32, acc0, input0, input1, input2, input3);
                acc1 = XXH128_mix32B_once(seed, 32 + 16, acc1, input2, input3, input0, input1);
            }
            final long input0 = LittleEndianByteArray.getLong(array, 0);
            final long input1 = LittleEndianByteArray.getLong(array, 0 + 8);
            final long input2 = LittleEndianByteArray.getLong(array, firstRead - 16);
            final long input3 = LittleEndianByteArray.getLong(array, firstRead - 16 + 8);
            acc0 = XXH128_mix32B_once(seed, 0, acc0, input0, input1, input2, input3);
            acc1 = XXH128_mix32B_once(seed, 16, acc1, input2, input3, input0, input1);

            final long low = XXH3_avalanche(acc0 + acc1);
            final long high = -XXH3_avalanche(acc0 * XXH_PRIME64_1 + acc1 * XXH_PRIME64_4 + (firstRead - seed) * XXH_PRIME64_2);
            return Utils.encodeHex(low, high);
        }

        if (firstRead <= 240) {
            // XXH3_len_129to240_128b
            final int nbRounds = (int) firstRead / 32;
            long acc0 = firstRead * XXH_PRIME64_1;
            long acc1 = 0;
            int i = 0;
            for (; i < 4; ++i) {
                final long input0 = LittleEndianByteArray.getLong(array, 32 * i);
                final long input1 = LittleEndianByteArray.getLong(array, 32 * i + 8);
                final long input2 = LittleEndianByteArray.getLong(array, 32 * i + 16);
                final long input3 = LittleEndianByteArray.getLong(array, 32 * i + 24);
                acc0 = XXH128_mix32B_once(seed, 32 * i, acc0, input0, input1, input2, input3);
                acc1 = XXH128_mix32B_once(seed, 32 * i + 16, acc1, input2, input3, input0, input1);
            }
            acc0 = XXH3_avalanche(acc0);
            acc1 = XXH3_avalanche(acc1);

            for (; i < nbRounds; ++i) {
                final long input0 = LittleEndianByteArray.getLong(array, 32 * i);
                final long input1 = LittleEndianByteArray.getLong(array, 32 * i + 8);
                final long input2 = LittleEndianByteArray.getLong(array, 32 * i + 16);
                final long input3 = LittleEndianByteArray.getLong(array, 32 * i + 24);
                acc0 = XXH128_mix32B_once(seed, 3 + 32 * (i - 4), acc0, input0, input1, input2, input3);
                acc1 = XXH128_mix32B_once(seed, 3 + 32 * (i - 4) + 16, acc1, input2, input3, input0, input1);
            }

            /* last bytes */
            final long input0 = LittleEndianByteArray.getLong(array, firstRead - 16);
            final long input1 = LittleEndianByteArray.getLong(array, firstRead - 16 + 8);
            final long input2 = LittleEndianByteArray.getLong(array, firstRead - 32);
            final long input3 = LittleEndianByteArray.getLong(array, firstRead - 32 + 8);
            acc0 = XXH128_mix32B_once(-seed, 136 - 17 - 16, acc0, input0, input1, input2, input3);
            acc1 = XXH128_mix32B_once(-seed, 136 - 17, acc1, input2, input3, input0, input1);

            final long low = XXH3_avalanche(acc0 + acc1);
            final long high = -XXH3_avalanche(acc0 * XXH_PRIME64_1 + acc1 * XXH_PRIME64_4 + (firstRead - seed) * XXH_PRIME64_2);
            return Utils.encodeHex(low, high);
        }


        throw new AssertionError(); // TODO
    }
}
