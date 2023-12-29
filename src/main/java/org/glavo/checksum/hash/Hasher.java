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

import org.glavo.checksum.util.IOUtils;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                return new ZipChecksumHasher(java.util.zip.CRC32::new);
            case "CRC32C":
                return new ZipChecksumHasher(java.util.zip.CRC32C::new);
            case "ADLER32":
                return new ZipChecksumHasher(java.util.zip.Adler32::new);
            // xxHash
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

    public static List<String> getAlgorithms() {
        ArrayList<String> algorithms = new ArrayList<>(16);

        algorithms.add("Adler32");
        algorithms.add("CRC32");
        algorithms.add("CRC32C");

        algorithms.add("MD2");
        algorithms.add("MD5");
        algorithms.add("SHA-1");

        // SHA-2
        algorithms.add("SHA-224");
        algorithms.add("SHA-256");
        algorithms.add("SHA-384");
        algorithms.add("SHA-512");
        algorithms.add("SHA-512/224");
        algorithms.add("SHA-512/256");

        // SHA-3
        algorithms.add("SHA3-224");
        algorithms.add("SHA3-256");
        algorithms.add("SHA3-384");
        algorithms.add("SHA3-512");

        return algorithms;
    }

    public static List<String> getExperimentalAlgorithms() {
        return List.of("XXH64", "XXH128");
    }

    private final int hashStringLength;

    Hasher(int digestLength) {
        this.hashStringLength = digestLength << 1;
    }

    public final boolean isAcceptChecksum(String checksum) {
        return checksum.length() == hashStringLength;
    }

    public abstract String hash(SeekableByteChannel channel) throws IOException;

    public String hashFile(Path file) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file, Collections.emptySet(), IOUtils.EMPTY_FILE_ATTRIBUTES)) {
            return hash(channel);
        }
    }
}
