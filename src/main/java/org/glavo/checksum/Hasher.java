package org.glavo.checksum;

import org.glavo.checksum.util.IOUtils;
import org.glavo.checksum.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

public final class Hasher {
    private final String name;
    private final int hashStringLength;

    private static final FileAttribute<?>[] EMPTY_ATTRIBUTES = new FileAttribute[0];

    private static final ThreadLocal<ByteBuffer> localBuffer = new ThreadLocal<ByteBuffer>() {
        @Override
        protected final ByteBuffer initialValue() {
            return ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE);
        }
    };

    private final ThreadLocal<MessageDigest> localMessageDigest = new ThreadLocal<MessageDigest>() {
        @Override
        protected final MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance(name);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                System.exit(-1);
                return null;
            }
        }
    };

    public static final Hasher MD5 = new Hasher("MD5", 32);
    public static final Hasher SHA_1 = new Hasher("SHA-1", 40);
    public static final Hasher SHA_224 = new Hasher("SHA-224", 56);
    public static final Hasher SHA_256 = new Hasher("SHA-256", 64);
    public static final Hasher SHA_384 = new Hasher("SHA-384", 96);
    public static final Hasher SHA_512 = new Hasher("SHA-512", 128);

    public static final Hasher[] ALL = {
            MD5, SHA_1, SHA_224, SHA_256, SHA_384, SHA_512
    };

    public static Hasher getDefault() {
        return SHA_256;
    }

    public static Hasher ofHashStringLength(int length) {
        switch (length) {
            case 32:
                return MD5;
            case 40:
                return SHA_1;
            case 56:
                return SHA_224;
            case 64:
                return SHA_256;
            case 96:
                return SHA_384;
            case 128:
                return SHA_512;
            default:
                return null;
        }
    }

    public static Hasher ofName(String name) {
        switch (name.toUpperCase()) {
            case "MD5":
                return MD5;
            case "SHA1":
            case "SHA-1":
                return SHA_1;
            case "SHA224":
            case "SHA-224":
                return SHA_224;
            case "SHA256":
            case "SHA-256":
                return SHA_256;
            case "SHA384":
            case "SHA-384":
                return SHA_384;
            case "SHA512":
            case "SHA-512":
                return SHA_512;
            default:
                return null;
        }
    }

    Hasher(String name, int hashStringLength) {
        this.name = name;
        this.hashStringLength = hashStringLength;
    }

    public int getHashStringLength() {
        return hashStringLength;
    }

    public final String hashFile(Path file) throws IOException {
        final String[] byte2str = Utils.byte2str;

        ByteBuffer buffer = localBuffer.get();
        final byte[] array = buffer.array();

        MessageDigest md = localMessageDigest.get();
        md.reset();

        int read;
        try (ByteChannel channel = Files.newByteChannel(file, Collections.emptySet(), EMPTY_ATTRIBUTES)) {
            do {
                buffer.clear();
                read = channel.read(buffer);
                if (read > 0) {
                    md.update(array, 0, read);
                }
            } while (read != -1);
        }
        byte[] digest = md.digest();
        if (digest == null) {
            throw new AssertionError(file);
        }

        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(byte2str[b & 0xFF]);
        }
        return builder.toString();
    }
}
