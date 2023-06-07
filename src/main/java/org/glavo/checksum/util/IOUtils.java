package org.glavo.checksum.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.attribute.FileAttribute;

public final class IOUtils {
    public static final int DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024; // 1 MiB

    public static final FileAttribute<?>[] EMPTY_FILE_ATTRIBUTES = new FileAttribute[0];

    public static boolean readChoice() throws IOException {
        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
            final String res = stdin.readLine();
            return "y".equalsIgnoreCase(res) || "yes".equalsIgnoreCase(res);
        }
    }
}
