package org.glavo.checksum.hash;

import org.glavo.checksum.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

abstract class HasherBase<C extends HasherBase.Context> extends Hasher {
    private final ThreadLocal<C> threadLocalContext = ThreadLocal.withInitial(this::createContext);

    HasherBase(int digestLength) {
        super(digestLength);
    }

    protected abstract C createContext();

    @Override
    public final String hashFile(Path file) throws IOException {
        final C context = threadLocalContext.get();

        final ByteBuffer buffer = context.buffer;
        final byte[] array = buffer.array();

        int read;
        try (ByteChannel channel = Files.newByteChannel(file, Collections.emptySet(), IOUtils.EMPTY_FILE_ATTRIBUTES)) {
            do {
                buffer.clear();
                read = channel.read(buffer);
                if (read > 0) {
                    context.update(array, 0, read);
                }
            } while (read != -1);
        }
        String res = context.digest();
        context.reset();
        return res;
    }

    protected static abstract class Context {
        final ByteBuffer buffer = ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE);

        protected abstract void update(byte[] input, int offset, int len);

        protected abstract String digest();

        protected void reset() {
        }
    }
}
