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

    static final class VerifyFileTask implements Runnable {
        private final LongAdder successCount;
        private final LongAdder failureCount;

        private final Path basePath;
        private final String line;
        private final Hasher hasher;

        VerifyFileTask(LongAdder successCount, LongAdder failureCount, Path basePath, String line, Hasher hasher) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.basePath = basePath;
            this.line = line;
            this.hasher = hasher;
        }

        @Override
        public final void run() {
            if (verifyFile(basePath, line, hasher)) {
                successCount.increment();
            } else {
                failureCount.increment();
            }
        }
    }

    public static void verify(Path basePath, BufferedReader reader, Hasher hasher, int numThreads)
            throws Exception {

        final LongAdder successCount = new LongAdder();
        final LongAdder failureCount = new LongAdder();
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
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
                pool.submit(new VerifyFileTask(successCount, failureCount, basePath, line, hasher));
            }
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    pool.submit(new VerifyFileTask(successCount, failureCount, basePath, line, hasher));
                }
            }
        } finally {
            pool.shutdown();
            //noinspection ResultOfMethodCallIgnored
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            Logger.info(Resources.getInstance().getVerificationCompletedMessage(), successCount.longValue(), failureCount.longValue());
        }
    }
}
