package org.glavo.checksum.util;

public final class Logger {
    public static final String GREEN = "\u001b[32m";
    public static final String RED = "\u001b[31m";
    public static final String RESET = "\u001b[0m";

    private static final boolean colored;

    static {
        String p = System.getProperty("org.glavo.checksum.logger.colored", System.getenv("GCHECKSUM_LOGGER_COLORED"));
        if (p == null) {
            colored = !System.getProperty("os.name").toLowerCase().startsWith("windows");
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

    public static void info(String format, Object... args) {
        if (colored) {
            System.out.println(GREEN + String.format(format, args) + RESET);
        } else {
            //noinspection RedundantStringFormatCall
            System.out.println(String.format(format, args));
        }
    }

    public static void error(String message) {
        if (colored) {
            System.err.println(RED + message + RESET);
        } else {
            System.err.println(message);
        }
    }

    public static void error(String format, Object... args) {
        if (colored) {
            System.err.println(RED + String.format(format, args) + RESET);
        } else {
            //noinspection RedundantStringFormatCall
            System.err.println(String.format(format, args));
        }
    }

    public static void logErrorAndExit(String message) {
        if (colored) {
            System.err.println(RED + message + RESET);
        } else {
            System.err.println(message);
        }
        System.exit(1);
    }

    public static void logErrorAndExit(String format, Object... args) {
        if (colored) {
            System.err.println(RED + String.format(format, args) + RESET);
        } else {
            //noinspection RedundantStringFormatCall
            System.err.println(String.format(format, args));
        }
        System.exit(1);
    }

}
