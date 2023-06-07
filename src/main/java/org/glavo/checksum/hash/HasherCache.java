package org.glavo.checksum.hash;

import org.glavo.checksum.util.IOUtils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class HasherCache {
    private static final ThreadLocal<HasherCache> cache = ThreadLocal.withInitial(HasherCache::new);

    static HasherCache getCache() {
        return cache.get();
    }

    private final ByteBuffer buffer = ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE);
    private MessageDigest messageDigest;

    ByteBuffer getBuffer() {
        return buffer;
    }

    MessageDigest getMessageDigest(String name) {
        MessageDigest md = this.messageDigest;
        if (md != null) {
            md.reset();
        } else {
            try {
                md = messageDigest = MessageDigest.getInstance(name);
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e.getMessage(), e);
            }
        }
        return md;
    }
}
