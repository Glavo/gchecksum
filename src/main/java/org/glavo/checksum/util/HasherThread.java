package org.glavo.checksum.util;

import org.glavo.checksum.Hasher;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public final class HasherThread extends Thread {
    private final ByteBuffer buffer = ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE);
    private MessageDigest digest;

    public HasherThread(Runnable target) {
        super(target);
    }

    public final ByteBuffer getBuffer() {
        return buffer;
    }

    public final MessageDigest getMessageDigest(Hasher hasher) {
        if (digest == null) {
            return digest = hasher.newMessageDigest();
        } else {
            return digest;
        }
    }
}
