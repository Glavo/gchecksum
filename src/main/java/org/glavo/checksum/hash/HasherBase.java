package org.glavo.checksum.hash;

import org.glavo.checksum.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Path;

abstract class HasherBase extends Hasher {
    private final ThreadLocal<Context> threadLocalContext = ThreadLocal.withInitial(this::createContext);

    HasherBase(int digestLength) {
        super(digestLength);
    }

    protected abstract Context createContext();

    @Override
    public final String hashFile(Path file) throws IOException {
        final Context context = threadLocalContext.get();

        final ByteBuffer buffer = context.buffer;
        final byte[] array = buffer.array();

        int read;
        try (ByteChannel channel = IOUtils.newByteChannel(file)) {
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

        protected abstract void reset();
    }
}
