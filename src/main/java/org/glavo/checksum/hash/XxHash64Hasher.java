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
import org.glavo.checksum.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

// The implementation references https://github.com/OpenHFT/Zero-Allocation-Hashing
final class XxHash64Hasher extends Hasher {
    // Primes if treated as unsigned
    private static final long P1 = -7046029288634856825L;
    private static final long P2 = -4417276706812531889L;
    private static final long P3 = 1609587929392839161L;
    private static final long P4 = -8796714831421723037L;
    private static final long P5 = 2870177450012600261L;

    private static final ThreadLocal<ByteBuffer> threadLocalBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE));

    static final XxHash64Hasher DEFAULT = new XxHash64Hasher(0L);

    private final long seed;

    XxHash64Hasher(long seed) {
        super(8);
        this.seed = seed;
    }

    @Override
    public String hashFile(Path file) throws IOException {
        final ByteBuffer buffer = threadLocalBuffer.get();
        final byte[] array = buffer.array();

        long v1 = seed + P1 + P2;
        long v2 = seed + P2;
        long v3 = seed;
        long v4 = seed - P1;

        long count = 0L;
        long remaining = 0L;
        int offset = 0;

        int read;
        try (ByteChannel channel = IOUtils.newByteChannel(file)) {
            do {
                buffer.clear();
                read = IOUtils.readAsPossible(channel, buffer);

                if (read <= 0) {
                    break;
                }

                count += read;
                remaining += read;

                offset = 0;
                while (remaining >= 32) {
                    v1 += LittleEndianByteArray.getLong(array, offset) * P2;
                    v1 = Long.rotateLeft(v1, 31);
                    v1 *= P1;

                    v2 += LittleEndianByteArray.getLong(array, offset + 8) * P2;
                    v2 = Long.rotateLeft(v2, 31);
                    v2 *= P1;

                    v3 += LittleEndianByteArray.getLong(array, offset + 16) * P2;
                    v3 = Long.rotateLeft(v3, 31);
                    v3 *= P1;

                    v4 += LittleEndianByteArray.getLong(array, offset + 24) * P2;
                    v4 = Long.rotateLeft(v4, 31);
                    v4 *= P1;

                    offset += 32;
                    remaining -= 32;
                }
            } while (read == IOUtils.DEFAULT_BUFFER_SIZE);
        }

        long hash;

        if (remaining < count) {
            hash = Long.rotateLeft(v1, 1)
                    + Long.rotateLeft(v2, 7)
                    + Long.rotateLeft(v3, 12)
                    + Long.rotateLeft(v4, 18);

            v1 *= P2;
            v1 = Long.rotateLeft(v1, 31);
            v1 *= P1;
            hash ^= v1;
            hash = hash * P1 + P4;

            v2 *= P2;
            v2 = Long.rotateLeft(v2, 31);
            v2 *= P1;
            hash ^= v2;
            hash = hash * P1 + P4;

            v3 *= P2;
            v3 = Long.rotateLeft(v3, 31);
            v3 *= P1;
            hash ^= v3;
            hash = hash * P1 + P4;

            v4 *= P2;
            v4 = Long.rotateLeft(v4, 31);
            v4 *= P1;
            hash ^= v4;
            hash = hash * P1 + P4;
        } else {
            hash = seed + P5;
        }

        hash += count;

        while (remaining >= 8) {
            long k1 = LittleEndianByteArray.getLong(array, offset);
            k1 *= P2;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= P1;
            hash ^= k1;
            hash = Long.rotateLeft(hash, 27) * P1 + P4;
            offset += 8;
            remaining -= 8;
        }

        if (remaining >= 4) {
            hash ^= LittleEndianByteArray.getUnsignedInt(array, offset) * P1;
            hash = Long.rotateLeft(hash, 23) * P2 + P3;
            offset += 4;
            remaining -= 4;
        }

        while (remaining != 0) {
            hash ^= (array[offset] & 0xff) * P5;
            hash = Long.rotateLeft(hash, 11) * P1;
            --remaining;
            ++offset;
        }

        return Utils.encodeHex(finalize(hash));
    }

    private static long finalize(long hash) {
        hash ^= hash >>> 33;
        hash *= P2;
        hash ^= hash >>> 29;
        hash *= P3;
        hash ^= hash >>> 32;
        return hash;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Need input file");
            System.exit(1);
        }

        System.out.println(DEFAULT.hashFile(Paths.get(args[0])) + "  " + args[0]);
    }
}
