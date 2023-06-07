package org.glavo.checksum.hash;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public abstract class Hasher {

    public static MessageDigestHasher getDefault() {
        return MessageDigestHasher.SHA_256;
    }

    public static MessageDigestHasher ofHashStringLength(int length) {
        switch (length) {
            case 32:
                return MessageDigestHasher.MD5;
            case 40:
                return MessageDigestHasher.SHA_1;
            case 56:
                return MessageDigestHasher.SHA_224;
            case 64:
                return MessageDigestHasher.SHA_256;
            case 96:
                return MessageDigestHasher.SHA_384;
            case 128:
                return MessageDigestHasher.SHA_512;
            default:
                return null;
        }
    }

    public static MessageDigestHasher ofName(String name) {
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
            default:
                try {
                    // Check if the algorithm is available
                    MessageDigest md = MessageDigest.getInstance(name);
                    return new MessageDigestHasher(name, md.getDigestLength() << 1);
                } catch (NoSuchAlgorithmException ignored) {
                    return null;
                }
        }
    }

    private final int hashStringLength;

    protected Hasher(int hashStringLength) {
        this.hashStringLength = hashStringLength;
    }

    public int getHashStringLength() {
        return hashStringLength;
    }

    public abstract String hashFile(Path file) throws IOException;
}
