package org.glavo.checksum.mode;

import org.glavo.checksum.Hasher;
import org.glavo.checksum.util.Logger;
import org.glavo.checksum.Resources;
import org.glavo.checksum.util.Pair;
import org.glavo.checksum.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public final class Verify {
    private static boolean verifyFile(Path basePath, String line, Hasher hasher) {
        try {
            final int lineLength = line.length();
            final Pair<String, String> r = Utils.spiltRecord(line);
            if (r == null) {
                Logger.error(Resources.getInstance().getInvalidHashRecordMessage(), line);
                return false;
            }

            final String recordHashValue = r.component1;
            if (recordHashValue.length() != hasher.getHashStringLength()) {
                Logger.error(Resources.getInstance().getInvalidHashRecordMessage(), line);
                return false;
            }

            final Path file = basePath.resolve(r.component2).toAbsolutePath();
            if (Files.notExists(file)) {
                Logger.error(Resources.getInstance().getFileNotExistMessage(), file);
                return false;
            } else if (Files.isDirectory(file)) {
                Logger.error(Resources.getInstance().getPathIsDirMessage(), file);
                return false;
            } else if (!Files.isReadable(file)) {
                Logger.error(Resources.getInstance().getFileCannotBeReadMessage(), file);
                return false;
            }

            final String fileHash = hasher.hashFile(file);
            if (!recordHashValue.equalsIgnoreCase(fileHash)) {
                Logger.error(Resources.getInstance().getHashNotMatchMessage(), file, fileHash, recordHashValue);
                return false;
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void verifyInSignalThread(Path basePath, BufferedReader reader, Hasher hasher) throws IOException {
        long successCount = 0;
        long failureCount = 0;

        try {
            if (hasher == null) {
                String firstLine = reader.readLine();
                while ("".equals(firstLine)) {
                    firstLine = reader.readLine();
                }
                if (firstLine == null) {
                    return;
                }
                final int idx = firstLine.indexOf(' ');
                hasher = Hasher.ofHashStringLength(idx);
                if (hasher == null) {
                    Logger.logErrorAndExit(Resources.getInstance().getInvalidHashRecordMessage(), firstLine);
                }
                if (verifyFile(basePath, firstLine, hasher)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    if (verifyFile(basePath, line, hasher)) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }
            }
        } finally {
            Logger.info(Resources.getInstance().getVerificationCompletedMessage(), successCount, failureCount);
        }
    }

    public static void verify(Path basePath, BufferedReader reader, Hasher hasher, int numThreads)
            throws Exception {

        final LongAdder successCount = new LongAdder();
        final LongAdder failureCount = new LongAdder();
        final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        try {
            if (hasher == null) {
                String firstLine = reader.readLine();
                while (firstLine != null && firstLine.isEmpty()) {
                    firstLine = reader.readLine();
                }
                if (firstLine == null) {
                    return;
                }
                final int idx = firstLine.indexOf(' ');
                hasher = Hasher.ofHashStringLength(idx);
                if (hasher == null) {
                    Logger.logErrorAndExit(Resources.getInstance().getInvalidHashRecordMessage(), firstLine);
                }
                final String l = firstLine;
                final Hasher h = hasher;
                executorService.submit(() -> {
                    if (verifyFile(basePath, l, h)) {
                        successCount.increment();
                    } else {
                        failureCount.increment();
                    }
                });
            }
            String line;
            final Hasher theHasher = hasher;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    final String theLine = line;
                    executorService.submit(() -> {
                        if (verifyFile(basePath, theLine, theHasher)) {
                            successCount.increment();
                        } else {
                            failureCount.increment();
                        }
                    });
                }
            }
        } finally {
            executorService.shutdown();
            //noinspection ResultOfMethodCallIgnored
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            Logger.info(Resources.getInstance().getVerificationCompletedMessage(), successCount.longValue(), failureCount.longValue());
        }
    }
}
