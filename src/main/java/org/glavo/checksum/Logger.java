package org.glavo.checksum;

public final class Logger {
    public static final String GREEN = "\u001b[32m";
    public static final String RED = "\u001b[31m";
    public static final String RESET = "\u001b[0m";

    private static final boolean colored;

    static {
        colored = "true".equalsIgnoreCase(
                System.getProperty("org.glavo.checksum.logger.colored", System.getenv("CHECKSUM_LOGGER_COLORED")));
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
