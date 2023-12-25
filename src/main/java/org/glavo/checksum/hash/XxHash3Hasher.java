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
import org.glavo.checksum.util.LittleEndianByteArray;
import org.glavo.checksum.util.Maths;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.Set;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

// The implementation references https://github.com/OpenHFT/Zero-Allocation-Hashing
abstract class XxHash3Hasher extends Hasher {

    private static final ThreadLocal<ByteBuffer> threadLocalBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE));

    /*! Pseudorandom secret taken directly from FARSH. */
    protected static final byte[] XXH3_kSecret = {
            (byte) 0xb8, (byte) 0xfe, (byte) 0x6c, (byte) 0x39, (byte) 0x23, (byte) 0xa4, (byte) 0x4b, (byte) 0xbe, (byte) 0x7c, (byte) 0x01, (byte) 0x81, (byte) 0x2c, (byte) 0xf7, (byte) 0x21, (byte) 0xad, (byte) 0x1c,
            (byte) 0xde, (byte) 0xd4, (byte) 0x6d, (byte) 0xe9, (byte) 0x83, (byte) 0x90, (byte) 0x97, (byte) 0xdb, (byte) 0x72, (byte) 0x40, (byte) 0xa4, (byte) 0xa4, (byte) 0xb7, (byte) 0xb3, (byte) 0x67, (byte) 0x1f,
            (byte) 0xcb, (byte) 0x79, (byte) 0xe6, (byte) 0x4e, (byte) 0xcc, (byte) 0xc0, (byte) 0xe5, (byte) 0x78, (byte) 0x82, (byte) 0x5a, (byte) 0xd0, (byte) 0x7d, (byte) 0xcc, (byte) 0xff, (byte) 0x72, (byte) 0x21,
            (byte) 0xb8, (byte) 0x08, (byte) 0x46, (byte) 0x74, (byte) 0xf7, (byte) 0x43, (byte) 0x24, (byte) 0x8e, (byte) 0xe0, (byte) 0x35, (byte) 0x90, (byte) 0xe6, (byte) 0x81, (byte) 0x3a, (byte) 0x26, (byte) 0x4c,
            (byte) 0x3c, (byte) 0x28, (byte) 0x52, (byte) 0xbb, (byte) 0x91, (byte) 0xc3, (byte) 0x00, (byte) 0xcb, (byte) 0x88, (byte) 0xd0, (byte) 0x65, (byte) 0x8b, (byte) 0x1b, (byte) 0x53, (byte) 0x2e, (byte) 0xa3,
            (byte) 0x71, (byte) 0x64, (byte) 0x48, (byte) 0x97, (byte) 0xa2, (byte) 0x0d, (byte) 0xf9, (byte) 0x4e, (byte) 0x38, (byte) 0x19, (byte) 0xef, (byte) 0x46, (byte) 0xa9, (byte) 0xde, (byte) 0xac, (byte) 0xd8,
            (byte) 0xa8, (byte) 0xfa, (byte) 0x76, (byte) 0x3f, (byte) 0xe3, (byte) 0x9c, (byte) 0x34, (byte) 0x3f, (byte) 0xf9, (byte) 0xdc, (byte) 0xbb, (byte) 0xc7, (byte) 0xc7, (byte) 0x0b, (byte) 0x4f, (byte) 0x1d,
            (byte) 0x8a, (byte) 0x51, (byte) 0xe0, (byte) 0x4b, (byte) 0xcd, (byte) 0xb4, (byte) 0x59, (byte) 0x31, (byte) 0xc8, (byte) 0x9f, (byte) 0x7e, (byte) 0xc9, (byte) 0xd9, (byte) 0x78, (byte) 0x73, (byte) 0x64,
            (byte) 0xea, (byte) 0xc5, (byte) 0xac, (byte) 0x83, (byte) 0x34, (byte) 0xd3, (byte) 0xeb, (byte) 0xc3, (byte) 0xc5, (byte) 0x81, (byte) 0xa0, (byte) 0xff, (byte) 0xfa, (byte) 0x13, (byte) 0x63, (byte) 0xeb,
            (byte) 0x17, (byte) 0x0d, (byte) 0xdd, (byte) 0x51, (byte) 0xb7, (byte) 0xf0, (byte) 0xda, (byte) 0x49, (byte) 0xd3, (byte) 0x16, (byte) 0x55, (byte) 0x26, (byte) 0x29, (byte) 0xd4, (byte) 0x68, (byte) 0x9e,
            (byte) 0x2b, (byte) 0x16, (byte) 0xbe, (byte) 0x58, (byte) 0x7d, (byte) 0x47, (byte) 0xa1, (byte) 0xfc, (byte) 0x8f, (byte) 0xf8, (byte) 0xb8, (byte) 0xd1, (byte) 0x7a, (byte) 0xd0, (byte) 0x31, (byte) 0xce,
            (byte) 0x45, (byte) 0xcb, (byte) 0x3a, (byte) 0x8f, (byte) 0x95, (byte) 0x16, (byte) 0x04, (byte) 0x28, (byte) 0xaf, (byte) 0xd7, (byte) 0xfb, (byte) 0xca, (byte) 0xbb, (byte) 0x4b, (byte) 0x40, (byte) 0x7e,
    };

    // Primes
    protected static final long XXH_PRIME32_1 = 0x9E3779B1L;   /*!< 0b10011110001101110111100110110001 */
    protected static final long XXH_PRIME32_2 = 0x85EBCA77L;   /*!< 0b10000101111010111100101001110111 */
    protected static final long XXH_PRIME32_3 = 0xC2B2AE3DL;   /*!< 0b11000010101100101010111000111101 */

    protected static final long XXH_PRIME64_1 = 0x9E3779B185EBCA87L;   /*!< 0b1001111000110111011110011011000110000101111010111100101010000111 */
    protected static final long XXH_PRIME64_2 = 0xC2B2AE3D27D4EB4FL;   /*!< 0b1100001010110010101011100011110100100111110101001110101101001111 */
    protected static final long XXH_PRIME64_3 = 0x165667B19E3779F9L;   /*!< 0b0001011001010110011001111011000110011110001101110111100111111001 */
    protected static final long XXH_PRIME64_4 = 0x85EBCA77C2B2AE63L;   /*!< 0b1000010111101011110010100111011111000010101100101010111001100011 */
    protected static final long XXH_PRIME64_5 = 0x27D4EB2F165667C5L;   /*!< 0b0010011111010100111010110010111100010110010101100110011111000101 */

    // only support fixed size secret
    protected static final long nbStripesPerBlock = (192 - 64) / 8;
    protected static final long block_len = 64 * nbStripesPerBlock;

    protected static long XXH64_avalanche(long h64) {
        h64 ^= h64 >>> 33;
        h64 *= XXH_PRIME64_2;
        h64 ^= h64 >>> 29;
        h64 *= XXH_PRIME64_3;
        return h64 ^ (h64 >>> 32);
    }

    protected static long XXH3_avalanche(long h64) {
        h64 ^= h64 >>> 37;
        h64 *= 0x165667919E3779F9L;
        return h64 ^ (h64 >>> 32);
    }

    protected static long XXH3_rrmxmx(long h64, final long length) {
        h64 ^= Long.rotateLeft(h64, 49) ^ Long.rotateLeft(h64, 24);
        h64 *= 0x9FB21C651E98DF25L;
        h64 ^= (h64 >>> 35) + length;
        h64 *= 0x9FB21C651E98DF25L;
        return h64 ^ (h64 >>> 28);
    }

    protected static long XXH3_mix16B(final long seed, final byte[] input, final long offIn, final long offSec) {
        final long input_lo = LittleEndianByteArray.getLong(input, offIn);
        final long input_hi = LittleEndianByteArray.getLong(input, offIn + 8);
        return Maths.unsignedLongMulXorFold(
                input_lo ^ (LittleEndianByteArray.getLong(XXH3_kSecret, offSec) + seed),
                input_hi ^ (LittleEndianByteArray.getLong(XXH3_kSecret, offSec + 8) - seed)
        );
    }

    /*
     * A bit slower than XXH3_mix16B, but handles multiply by zero better.
     */
    protected static long XXH128_mix32B_once(final long seed, final long offSec, long acc, final long input0, final long input1, final long input2, final long input3) {
        acc += Maths.unsignedLongMulXorFold(
                input0 ^ (LittleEndianByteArray.getLong(XXH3_kSecret, offSec) + seed),
                input1 ^ (LittleEndianByteArray.getLong(XXH3_kSecret, offSec + 8) - seed));
        return acc ^ (input2 + input3);
    }

    protected static long XXH3_mix2Accs(final long acc_lh, final long acc_rh, final byte[] secret, final long offSec) {
        return Maths.unsignedLongMulXorFold(
                acc_lh ^ LittleEndianByteArray.getLong(secret, offSec),
                acc_rh ^ LittleEndianByteArray.getLong(secret, offSec + 8));
    }

    private static void XXH3_initCustomSecret(final byte[] customSecret, final long seed64) {
        final int nbRounds = 192 / 16;
        final ByteBuffer bb = ByteBuffer.wrap(customSecret).order(LITTLE_ENDIAN);
        for (int i = 0; i < nbRounds; i++) {
            final long lo = LittleEndianByteArray.getLong(XXH3_kSecret, 16 * i) + seed64;
            final long hi = LittleEndianByteArray.getLong(XXH3_kSecret, 16 * i + 8) - seed64;
            bb.putLong(16 * i + 0, lo);
            bb.putLong(16 * i + 8, hi);
        }
    }

    private static final Set<OpenOption> OPEN_OPTIONS = Collections.singleton(StandardOpenOption.READ);
    private static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute<?>[0];

    protected final long seed;
    protected final byte[] secret;

    XxHash3Hasher(int digestLength, long seed) {
        super(digestLength);
        this.seed = seed;

        if (seed == 0L) {
            this.secret = XXH3_kSecret;
        } else {
            this.secret = new byte[192];
            XXH3_initCustomSecret(this.secret, seed);
        }
    }

    protected abstract String hashImpl(SeekableByteChannel channel, ByteBuffer buffer, long length) throws IOException;

    @Override
    public final String hash(SeekableByteChannel channel) throws IOException {
        final ByteBuffer buffer = threadLocalBuffer.get();

        long length = channel.size();

        buffer.position(0);
        buffer.limit((int) Math.min(length, IOUtils.DEFAULT_BUFFER_SIZE));

        IOUtils.readFully(channel, buffer);

        return hashImpl(channel, buffer, length);
    }
}
