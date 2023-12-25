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

package org.glavo.checksum.hash;

import org.glavo.checksum.util.IOUtils;
import org.glavo.checksum.util.Maths;
import org.glavo.checksum.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import static org.glavo.checksum.util.LittleEndianByteArray.*;

final class XxHash3_128Hasher extends XxHash3Hasher {

    static final XxHash3_128Hasher DEFAULT = new XxHash3_128Hasher(0L);

    XxHash3_128Hasher(long seed) {
        super(16, seed);
    }

    @Override
    protected String hashImpl(SeekableByteChannel channel, ByteBuffer buffer, long length) throws IOException {
        final byte[] array = buffer.array();

        if (length <= 16) {
            // XXH3_len_0to16_128b
            if (length > 8) {
                // XXH3_len_9to16_128b
                final long bitflipl = (getLong(XXH3_kSecret, 32) ^ getLong(XXH3_kSecret, 40)) - seed;
                final long bitfliph = (getLong(XXH3_kSecret, 48) ^ getLong(XXH3_kSecret, 56)) + seed;
                long input_hi = getLong(array, length - 8);
                final long input_lo = getLong(array, 0) ^ input_hi ^ bitflipl;
                long m128_lo = input_lo * XXH_PRIME64_1;
                long m128_hi = Maths.unsignedLongMulHigh(input_lo, XXH_PRIME64_1);
                m128_lo += (length - 1) << 54;
                input_hi ^= bitfliph;
                m128_hi += input_hi + Integer.toUnsignedLong((int) input_hi) * (XXH_PRIME32_2 - 1);
                m128_lo ^= Long.reverseBytes(m128_hi);

                final long low = XXH3_avalanche(m128_lo * XXH_PRIME64_2);
                final long high = XXH3_avalanche(Maths.unsignedLongMulHigh(m128_lo, XXH_PRIME64_2) + m128_hi * XXH_PRIME64_2);

                return Utils.encodeHex(low, high);
            }
            if (length >= 4) {
                // XXH3_len_4to8_128b
                long s = seed ^ Long.reverseBytes(seed & 0xffffffffL);
                final long input_lo = getUnsignedInt(array, 0);
                final long input_hi = getInt(array, length - 4); // high int will be shifted

                final long bitflip = (getLong(XXH3_kSecret, 16) ^ getLong(XXH3_kSecret, 24)) + s;
                final long keyed = (input_lo + (input_hi << 32)) ^ bitflip;
                final long pl = XXH_PRIME64_1 + (length << 2); /* Shift len to the left to ensure it is even, this avoids even multiplies. */
                long m128_lo = keyed * pl;
                long m128_hi = Maths.unsignedLongMulHigh(keyed, pl);
                m128_hi += (m128_lo << 1);
                m128_lo ^= (m128_hi >>> 3);

                m128_lo ^= m128_lo >>> 35;
                m128_lo *= 0x9FB21C651E98DF25L;
                m128_lo ^= m128_lo >>> 28;

                return Utils.encodeHex(m128_lo, XXH3_avalanche(m128_hi));
            }
            if (length != 0) {
                // XXH3_len_1to3_128b
                final int c1 = getUnsignedByte(array, 0);
                final int c2 = getByte(array, length >> 1); // high 3 bytes will be shifted
                final int c3 = getUnsignedByte(array, length - 1);
                final int combinedl = (c1 << 16) | (c2 << 24) | c3 | ((int) length << 8);
                final int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), 13);
                int i1 = getInt(XXH3_kSecret, 0) ^ getInt(XXH3_kSecret, 4);
                final long bitflipl = Integer.toUnsignedLong(i1) + seed;
                int i = getInt(XXH3_kSecret, 8) ^ getInt(XXH3_kSecret, 12);
                final long bitfliph = Integer.toUnsignedLong(i) - seed;

                final long low = XXH64_avalanche(Integer.toUnsignedLong(combinedl) ^ bitflipl);
                final long high = XXH64_avalanche(Integer.toUnsignedLong(combinedh) ^ bitfliph);

                return Utils.encodeHex(low, high);
            }
            final long low = XXH64_avalanche(seed ^ getLong(XXH3_kSecret, 64) ^ getLong(XXH3_kSecret, 72));
            final long high = XXH64_avalanche(seed ^ getLong(XXH3_kSecret, 80) ^ getLong(XXH3_kSecret, 88));
            return Utils.encodeHex(low, high);
        }

        if (length <= 128) {
            // XXH3_len_17to128_128b
            long acc0 = length * XXH_PRIME64_1;
            long acc1 = 0;
            if (length > 32) {
                if (length > 64) {
                    if (length > 96) {
                        final long input0 = getLong(array, 48);
                        final long input1 = getLong(array, 48 + 8);
                        final long input2 = getLong(array, length - 64);
                        final long input3 = getLong(array, length - 64 + 8);
                        acc0 = XXH128_mix32B_once(seed, 96, acc0, input0, input1, input2, input3);
                        acc1 = XXH128_mix32B_once(seed, 96 + 16, acc1, input2, input3, input0, input1);
                    }
                    final long input0 = getLong(array, 32);
                    final long input1 = getLong(array, 32 + 8);
                    final long input2 = getLong(array, length - 48);
                    final long input3 = getLong(array, length - 48 + 8);
                    acc0 = XXH128_mix32B_once(seed, 64, acc0, input0, input1, input2, input3);
                    acc1 = XXH128_mix32B_once(seed, 64 + 16, acc1, input2, input3, input0, input1);
                }
                final long input0 = getLong(array, 16);
                final long input1 = getLong(array, 16 + 8);
                final long input2 = getLong(array, length - 32);
                final long input3 = getLong(array, length - 32 + 8);
                acc0 = XXH128_mix32B_once(seed, 32, acc0, input0, input1, input2, input3);
                acc1 = XXH128_mix32B_once(seed, 32 + 16, acc1, input2, input3, input0, input1);
            }
            final long input0 = getLong(array, 0);
            final long input1 = getLong(array, 8);
            final long input2 = getLong(array, length - 16);
            final long input3 = getLong(array, length - 16 + 8);
            acc0 = XXH128_mix32B_once(seed, 0, acc0, input0, input1, input2, input3);
            acc1 = XXH128_mix32B_once(seed, 16, acc1, input2, input3, input0, input1);

            final long low = XXH3_avalanche(acc0 + acc1);
            final long high = -XXH3_avalanche(acc0 * XXH_PRIME64_1 + acc1 * XXH_PRIME64_4 + (length - seed) * XXH_PRIME64_2);
            return Utils.encodeHex(low, high);
        }

        if (length <= 240) {
            // XXH3_len_129to240_128b
            final int nbRounds = (int) length / 32;
            long acc0 = length * XXH_PRIME64_1;
            long acc1 = 0;
            int i = 0;
            for (; i < 4; ++i) {
                final long input0 = getLong(array, 32 * i);
                final long input1 = getLong(array, 32 * i + 8);
                final long input2 = getLong(array, 32 * i + 16);
                final long input3 = getLong(array, 32 * i + 24);
                acc0 = XXH128_mix32B_once(seed, 32 * i, acc0, input0, input1, input2, input3);
                acc1 = XXH128_mix32B_once(seed, 32 * i + 16, acc1, input2, input3, input0, input1);
            }
            acc0 = XXH3_avalanche(acc0);
            acc1 = XXH3_avalanche(acc1);

            for (; i < nbRounds; ++i) {
                final long input0 = getLong(array, 32 * i);
                final long input1 = getLong(array, 32 * i + 8);
                final long input2 = getLong(array, 32 * i + 16);
                final long input3 = getLong(array, 32 * i + 24);
                acc0 = XXH128_mix32B_once(seed, 3 + 32 * (i - 4), acc0, input0, input1, input2, input3);
                acc1 = XXH128_mix32B_once(seed, 3 + 32 * (i - 4) + 16, acc1, input2, input3, input0, input1);
            }

            /* last bytes */
            final long input0 = getLong(array, length - 16);
            final long input1 = getLong(array, length - 16 + 8);
            final long input2 = getLong(array, length - 32);
            final long input3 = getLong(array, length - 32 + 8);
            acc0 = XXH128_mix32B_once(-seed, 136 - 17 - 16, acc0, input0, input1, input2, input3);
            acc1 = XXH128_mix32B_once(-seed, 136 - 17, acc1, input2, input3, input0, input1);

            final long low = XXH3_avalanche(acc0 + acc1);
            final long high = -XXH3_avalanche(acc0 * XXH_PRIME64_1 + acc1 * XXH_PRIME64_4 + (length - seed) * XXH_PRIME64_2);
            return Utils.encodeHex(low, high);
        }

        return hashLong128b(channel, buffer, length);
    }

    private String hashLong128b(SeekableByteChannel channel, ByteBuffer buffer, long length) throws IOException {
        final byte[] array = buffer.array();
        int offBlock = 0;

        // XXH3_hashLong_128b_internal
        long acc_0 = XXH_PRIME32_3;
        long acc_1 = XXH_PRIME64_1;
        long acc_2 = XXH_PRIME64_2;
        long acc_3 = XXH_PRIME64_3;
        long acc_4 = XXH_PRIME64_4;
        long acc_5 = XXH_PRIME32_2;
        long acc_6 = XXH_PRIME64_5;
        long acc_7 = XXH_PRIME32_1;

        // XXH3_hashLong_internal_loop
        final long nb_blocks = (length - 1) / block_len;
        for (long n = 0; n < nb_blocks; n++) {
            if (offBlock == IOUtils.DEFAULT_BUFFER_SIZE) {
                offBlock = 0;
                buffer.position(0);
                buffer.limit((int) Math.min(length - (n * block_len), IOUtils.DEFAULT_BUFFER_SIZE));
                IOUtils.readFully(channel, buffer);
            }

            // XXH3_accumulate
            // final long offBlock = off + n * block_len;
            for (long s = 0; s < nbStripesPerBlock; s++) {
                // XXH3_accumulate_512
                final long offStripe = offBlock + s * 64;
                final long offSec = s * 8;
                {
                    final long data_val_0 = getLong(array, offStripe + 8 * 0);
                    final long data_val_1 = getLong(array, offStripe + 8 * 1);
                    final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 0);
                    final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 1);
                    /* swap adjacent lanes */
                    acc_0 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                    acc_1 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
                }
                {
                    final long data_val_0 = getLong(array, offStripe + 8 * 2);
                    final long data_val_1 = getLong(array, offStripe + 8 * 3);
                    final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 2);
                    final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 3);
                    /* swap adjacent lanes */
                    acc_2 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                    acc_3 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
                }
                {
                    final long data_val_0 = getLong(array, offStripe + 8 * 4);
                    final long data_val_1 = getLong(array, offStripe + 8 * 5);
                    final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 4);
                    final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 5);
                    /* swap adjacent lanes */
                    acc_4 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                    acc_5 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
                }
                {
                    final long data_val_0 = getLong(array, offStripe + 8 * 6);
                    final long data_val_1 = getLong(array, offStripe + 8 * 7);
                    final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 6);
                    final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 7);
                    /* swap adjacent lanes */
                    acc_6 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                    acc_7 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
                }
            }

            // XXH3_scrambleAcc_scalar
            final long offSec = 192 - 64;
            acc_0 = (acc_0 ^ (acc_0 >>> 47) ^ getLong(secret, offSec + 8 * 0)) * XXH_PRIME32_1;
            acc_1 = (acc_1 ^ (acc_1 >>> 47) ^ getLong(secret, offSec + 8 * 1)) * XXH_PRIME32_1;
            acc_2 = (acc_2 ^ (acc_2 >>> 47) ^ getLong(secret, offSec + 8 * 2)) * XXH_PRIME32_1;
            acc_3 = (acc_3 ^ (acc_3 >>> 47) ^ getLong(secret, offSec + 8 * 3)) * XXH_PRIME32_1;
            acc_4 = (acc_4 ^ (acc_4 >>> 47) ^ getLong(secret, offSec + 8 * 4)) * XXH_PRIME32_1;
            acc_5 = (acc_5 ^ (acc_5 >>> 47) ^ getLong(secret, offSec + 8 * 5)) * XXH_PRIME32_1;
            acc_6 = (acc_6 ^ (acc_6 >>> 47) ^ getLong(secret, offSec + 8 * 6)) * XXH_PRIME32_1;
            acc_7 = (acc_7 ^ (acc_7 >>> 47) ^ getLong(secret, offSec + 8 * 7)) * XXH_PRIME32_1;

            offBlock += block_len;
        }

        if (offBlock == IOUtils.DEFAULT_BUFFER_SIZE) {
            offBlock = 0;
            buffer.position(0);
            buffer.limit((int) (length - (nb_blocks * block_len)));
            IOUtils.readFully(channel, buffer);
        }

        /* last partial block */
        final long nbStripes = ((length - 1) - (block_len * nb_blocks)) / 64;
        // final long offBlock = off + block_len * nb_blocks;
        for (long s = 0; s < nbStripes; s++) {
            // XXH3_accumulate_512
            final long offStripe = offBlock + s * 64;
            final long offSec = s * 8;
            {
                final long data_val_0 = getLong(array, offStripe + 8 * 0);
                final long data_val_1 = getLong(array, offStripe + 8 * 1);
                final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 0);
                final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 1);
                /* swap adjacent lanes */
                acc_0 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                acc_1 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
            }
            {
                final long data_val_0 = getLong(array, offStripe + 8 * 2);
                final long data_val_1 = getLong(array, offStripe + 8 * 3);
                final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 2);
                final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 3);
                /* swap adjacent lanes */
                acc_2 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                acc_3 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
            }
            {
                final long data_val_0 = getLong(array, offStripe + 8 * 4);
                final long data_val_1 = getLong(array, offStripe + 8 * 5);
                final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 4);
                final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 5);
                /* swap adjacent lanes */
                acc_4 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                acc_5 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
            }
            {
                final long data_val_0 = getLong(array, offStripe + 8 * 6);
                final long data_val_1 = getLong(array, offStripe + 8 * 7);
                final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 6);
                final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 7);
                /* swap adjacent lanes */
                acc_6 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
                acc_7 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
            }
        }

        /* last stripe */
        // XXH3_accumulate_512
        long offStripe = (length - 64) % IOUtils.DEFAULT_BUFFER_SIZE;
        if (IOUtils.DEFAULT_BUFFER_SIZE - offStripe < 64) {
            channel.position(length - 64);

            offStripe = 0;

            buffer.position(0);
            buffer.limit(64);

            IOUtils.readFully(channel, buffer);
        }
        final long offSec = 192 - 64 - 7;
        {
            final long data_val_0 = getLong(array, offStripe + 8 * 0);
            final long data_val_1 = getLong(array, offStripe + 8 * 1);
            final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 0);
            final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 1);
            /* swap adjacent lanes */
            acc_0 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
            acc_1 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
        }
        {
            final long data_val_0 = getLong(array, offStripe + 8 * 2);
            final long data_val_1 = getLong(array, offStripe + 8 * 3);
            final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 2);
            final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 3);
            /* swap adjacent lanes */
            acc_2 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
            acc_3 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
        }
        {
            final long data_val_0 = getLong(array, offStripe + 8 * 4);
            final long data_val_1 = getLong(array, offStripe + 8 * 5);
            final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 4);
            final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 5);
            /* swap adjacent lanes */
            acc_4 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
            acc_5 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
        }
        {
            final long data_val_0 = getLong(array, offStripe + 8 * 6);
            final long data_val_1 = getLong(array, offStripe + 8 * 7);
            final long data_key_0 = data_val_0 ^ getLong(secret, offSec + 8 * 6);
            final long data_key_1 = data_val_1 ^ getLong(secret, offSec + 8 * 7);
            /* swap adjacent lanes */
            acc_6 += data_val_1 + (0xFFFFFFFFL & data_key_0) * (data_key_0 >>> 32);
            acc_7 += data_val_0 + (0xFFFFFFFFL & data_key_1) * (data_key_1 >>> 32);
        }

        // XXH3_mergeAccs
        final long low = XXH3_avalanche(length * XXH_PRIME64_1
                + XXH3_mix2Accs(acc_0, acc_1, secret, 11)
                + XXH3_mix2Accs(acc_2, acc_3, secret, 11 + 16)
                + XXH3_mix2Accs(acc_4, acc_5, secret, 11 + 16 * 2)
                + XXH3_mix2Accs(acc_6, acc_7, secret, 11 + 16 * 3));
        final long high = XXH3_avalanche(~(length * XXH_PRIME64_2)
                + XXH3_mix2Accs(acc_0, acc_1, secret, 192 - 64 - 11)
                + XXH3_mix2Accs(acc_2, acc_3, secret, 192 - 64 - 11 + 16)
                + XXH3_mix2Accs(acc_4, acc_5, secret, 192 - 64 - 11 + 16 * 2)
                + XXH3_mix2Accs(acc_6, acc_7, secret, 192 - 64 - 11 + 16 * 3));

        return Utils.encodeHex(low, high);
    }
}
