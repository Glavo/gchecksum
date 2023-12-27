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

import java.util.Locale;

public final class Logger {
    public static final String GREEN = "\u001b[32m";
    public static final String RED = "\u001b[31m";

    public static final String RESET = "\u001b[0m";

    private static final boolean colored;

    static {
        String p = System.getProperty("org.glavo.checksum.logger.colored", System.getenv("GCHECKSUM_LOGGER_COLORED"));
        if (p == null) {
            colored = !System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        } else {
            colored = "true".equalsIgnoreCase(p);
        }
    }

    public static void info(String message) {
        if (colored) {
            System.out.println(GREEN + message + RESET);
        } else {
            System.out.println(message);
        }
    }

    public static void error(String message) {
        if (colored) {
            System.err.println(RED + message + RESET);
        } else {
            System.err.println(message);
        }
    }
}
