/*
 * Copyright 2023 Glavo
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
