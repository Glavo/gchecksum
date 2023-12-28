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

package org.glavo.checksum.util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;

public final class IOUtils {
    public static final int DEFAULT_BUFFER_SIZE = 320 * 1024; // 320 KiB

    public static final FileAttribute<?>[] EMPTY_FILE_ATTRIBUTES = new FileAttribute[0];

    public static final LinkOption[] EMPTY_LINK_OPTIONS = new LinkOption[0];

    public static boolean readChoice() throws IOException {
        StringBuilder lineBuilder = new StringBuilder();
        int ch;
        while ((ch = System.in.read()) > 0 && ch != '\r' && ch != '\n') {
            lineBuilder.append((char) ch);
        }

        String line = lineBuilder.toString().toLowerCase(Locale.ROOT);
        return "y".equals(line) || "yes".equals(line);
    }

    public static int readAsPossible(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        final int expectedLength = buffer.remaining();
        int read = 0;
        while (read < expectedLength) {
            final int readNow = channel.read(buffer);
            if (readNow <= 0) {
                break;
            }
            read += readNow;
        }
        return read;
    }

    public static void readFully(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        final int expectedLength = buffer.remaining();
        int read = 0;
        while (read < expectedLength) {
            final int readNow = channel.read(buffer);
            if (readNow <= 0) {
                break;
            }
            read += readNow;
        }
        if (read < expectedLength) {
            throw new EOFException();
        }
    }
}
