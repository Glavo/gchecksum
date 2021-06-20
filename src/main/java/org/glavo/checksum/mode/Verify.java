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
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
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

    public static void verify(Path basePath, BufferedReader reader, Hasher hasher, int numThreads)
            throws Exception {

        final LongAdder successCount = new LongAdder();
        final LongAdder failureCount = new LongAdder();

        try {
            if (numThreads == 1) {
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
                        successCount.increment();
                    } else {
                        failureCount.increment();
                    }
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        if (verifyFile(basePath, line, hasher)) {
                            successCount.increment();
                        } else {
                            failureCount.increment();
                        }
                    }
                }
            } else {
                final ForkJoinPool pool = new ForkJoinPool(numThreads);
                ForkJoinTask<?> t0 = null;
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
                    t0 = pool.submit(() -> {
                        if (verifyFile(basePath, l, h)) {
                            successCount.increment();
                        } else {
                            failureCount.increment();
                        }
                    });
                }
                final Hasher finalHasher = hasher;
                final ArrayList<String> lines = new ArrayList<>(1024);
                {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isEmpty()) {
                            lines.add(line);
                        }
                    }
                }

                pool.submit(() -> lines.stream().parallel()
                        .forEach(line -> {
                            if (verifyFile(basePath, line, finalHasher)) {
                                successCount.add(1);
                            } else {
                                failureCount.add(1);
                            }
                        })).get();

                if (t0 != null) {
                    t0.get();
                }
            }
        } finally {
            Logger.info(Resources.getInstance().getVerificationCompletedMessage(), successCount.longValue(), failureCount.longValue());
        }
    }
}
