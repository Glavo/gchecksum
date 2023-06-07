package org.glavo.checksum.hash;

import org.glavo.checksum.util.IOUtils;
import org.glavo.checksum.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;

final class MessageDigestHasher extends Hasher {

    static final MessageDigestHasher MD5 = new MessageDigestHasher("MD5", 32);
    static final MessageDigestHasher SHA_1 = new MessageDigestHasher("SHA-1", 40);
    static final MessageDigestHasher SHA_224 = new MessageDigestHasher("SHA-224", 56);
    static final MessageDigestHasher SHA_256 = new MessageDigestHasher("SHA-256", 64);
    static final MessageDigestHasher SHA_384 = new MessageDigestHasher("SHA-384", 96);
    static final MessageDigestHasher SHA_512 = new MessageDigestHasher("SHA-512", 128);

    private final String name;

    MessageDigestHasher(String name, int hashStringLength) {
        super(hashStringLength);
        this.name = name;
    }

    @Override
    public String hashFile(Path file) throws IOException {
        HasherCache cache = HasherCache.getCache();

        ByteBuffer buffer = cache.getBuffer();
        final byte[] array = buffer.array();

        MessageDigest md = cache.getMessageDigest(name);

        int read;
        try (ByteChannel channel = Files.newByteChannel(file, Collections.emptySet(), IOUtils.EMPTY_FILE_ATTRIBUTES)) {
            do {
                buffer.clear();
                read = channel.read(buffer);
                if (read > 0) {
                    md.update(array, 0, read);
                }
            } while (read != -1);
        }
        return Utils.encodeHex(md.digest());
    }
}
