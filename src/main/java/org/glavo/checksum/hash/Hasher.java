/*
 * Copyright 2021 Glavo
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

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public abstract class Hasher {

    public static Hasher getDefault() {
        return MessageDigestHasher.SHA_256;
    }

    public static Hasher ofHashStringLength(int length) {
        Hasher[] defaultHashers = {
                MessageDigestHasher.MD5,
                MessageDigestHasher.SHA_1,
                MessageDigestHasher.SHA_224,
                MessageDigestHasher.SHA_256,
                MessageDigestHasher.SHA_384,
                MessageDigestHasher.SHA_512
        };

        for (Hasher hasher : defaultHashers) {
            if (length == hasher.hashStringLength) {
                return hasher;
            }
        }

        return null;
    }

    public static Hasher ofName(String name) {
        switch (name.toUpperCase(Locale.ROOT)) {
            case "MD5":
                return MessageDigestHasher.MD5;
            case "SHA1":
            case "SHA-1":
                return MessageDigestHasher.SHA_1;
            case "SHA224":
            case "SHA-224":
                return MessageDigestHasher.SHA_224;
            case "SHA256":
            case "SHA-256":
                return MessageDigestHasher.SHA_256;
            case "SHA384":
            case "SHA-384":
                return MessageDigestHasher.SHA_384;
            case "SHA512":
            case "SHA-512":
                return MessageDigestHasher.SHA_512;
            // java.util.zip.Checksum
            case "CRC32":
                return ZipChecksumHasher.CRC32;
            case "CRC32C":
                ZipChecksumHasher crc32c = null;

                try {
                    Class<?> clazz = Class.forName("java.util.zip.CRC32C");
                    MethodHandle constructor = MethodHandles.publicLookup()
                            .findConstructor(clazz, MethodType.methodType(void.class));

                    crc32c = new ZipChecksumHasher(() -> {
                        try {
                            return (java.util.zip.Checksum) constructor.invoke();
                        } catch (Throwable e) {
                            throw new InternalError(e);
                        }
                    });
                } catch (Throwable ignored) {
                }

                return crc32c;
            case "ADLER32":
                return ZipChecksumHasher.ADLER32;
            // xxHash
            case "XX":
            case "XX64":
            case "XXH64":
            case "XXHASH64":
                return XxHash64Hasher.DEFAULT;
            case "XX128":
            case "XXH128":
            case "XXHASH128":
            case "XXH3_128":
            case "XXH3-128":
                return XxHash3_128Hasher.DEFAULT;
            default:
                try {
                    // Check if the algorithm is available
                    MessageDigest md = MessageDigest.getInstance(name);
                    return new MessageDigestHasher(name, md.getDigestLength());
                } catch (NoSuchAlgorithmException ignored) {
                    return null;
                }
        }
    }

    private final int hashStringLength;

    Hasher(int digestLength) {
        this.hashStringLength = digestLength << 1;
    }

    public final boolean isAcceptChecksum(String checksum) {
        return checksum.length() == hashStringLength;
    }

    public abstract String hashFile(Path file) throws IOException;
}
