package org.glavo.checksum.hash;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public abstract class Hasher {

    public static Hasher getDefault() {
        return MessageDigestHasher.SHA_256;
    }

    public static Hasher ofHashStringLength(int length) {
        HasherBase<?>[] defaultHashers = {
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
