package org.glavo.checksum.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IOUtils {
    public static final int DEFAULT_BUFFER_SIZE = 327680; // 320 kb

    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return new BufferedReader(
                new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8.newDecoder()),
                DEFAULT_BUFFER_SIZE
        );
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean readChoice() throws IOException {
        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
            final String res = stdin.readLine();
            return res != null
                    && (res.equalsIgnoreCase("y") || res.equalsIgnoreCase("yes"));
        }
    }
}
