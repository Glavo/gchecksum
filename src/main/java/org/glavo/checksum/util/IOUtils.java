package org.glavo.checksum.util;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;

public final class IOUtils {
    public static final int DEFAULT_BUFFER_SIZE = 320 * 1024; // 320 KiB

    public static final FileAttribute<?>[] EMPTY_FILE_ATTRIBUTES = new FileAttribute[0];

    public static final LinkOption[] EMPTY_LINK_OPTIONS = new LinkOption[0];

    public static ByteChannel newByteChannel(Path path) throws IOException {
        return Files.newByteChannel(path, Collections.emptySet(), IOUtils.EMPTY_FILE_ATTRIBUTES);
    }

    public static boolean readChoice() throws IOException {
        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
            final String res = stdin.readLine();
            return "y".equalsIgnoreCase(res) || "yes".equalsIgnoreCase(res);
        }
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
