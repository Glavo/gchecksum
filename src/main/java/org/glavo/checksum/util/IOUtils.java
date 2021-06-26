package org.glavo.checksum.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class IOUtils {
    public static final int DEFAULT_BUFFER_SIZE = 327680; // 320 kb

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean readChoice() throws IOException {
        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
            final String res = stdin.readLine();
            return "y".equalsIgnoreCase(res) || "yes".equalsIgnoreCase(res);
        }
    }
}
