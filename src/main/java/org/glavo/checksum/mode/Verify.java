package org.glavo.checksum.mode;

import org.glavo.checksum.Hasher;
import org.glavo.checksum.util.HasherThreadFactory;
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

    private static Runnable verifyFileTask(
            LongAdder successCount, LongAdder failureCount, Path basePath, String line, Hasher hasher) {
        return () -> {
            if (verifyFile(basePath, line, hasher)) {
                successCount.increment();
            } else {
                failureCount.increment();
            }
        };
    }

    public static void verify(Path basePath, BufferedReader reader, Hasher hasher, int numThreads)
            throws Exception {

        final LongAdder successCount = new LongAdder();
        final LongAdder failureCount = new LongAdder();
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads, new HasherThreadFactory());
        try {
            String line;
            if (hasher == null) {
                line = reader.readLine();
                while (line != null && line.isEmpty()) {
                    line = reader.readLine();
                }
                if (line == null) {
                    return;
                }
                final int idx = line.indexOf(' ');
                hasher = Hasher.ofHashStringLength(idx);
                if (hasher == null) {
                    Logger.logErrorAndExit(Resources.getInstance().getInvalidHashRecordMessage(), line);
                }
                pool.submit(verifyFileTask(successCount, failureCount, basePath, line, hasher));
            }
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    pool.submit(verifyFileTask(successCount, failureCount, basePath, line, hasher));
                }
            }
        } finally {
            pool.shutdown();
            //noinspection ResultOfMethodCallIgnored
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            Logger.info(Resources.getInstance().getVerificationCompletedMessage(), successCount.longValue(), failureCount.longValue());
        }
    }
}
