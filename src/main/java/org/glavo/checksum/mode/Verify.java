package org.glavo.checksum.mode;

import org.glavo.checksum.hash.Hasher;
import org.glavo.checksum.util.ChecksumThreadFactory;
import org.glavo.checksum.util.Logger;
import org.glavo.checksum.util.Lang;
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
                Logger.error(Lang.getInstance().getInvalidHashRecordMessage(), line);
                return false;
            }

            final String recordHashValue = r.component1;
            if (!hasher.isAcceptChecksum(recordHashValue)) {
                Logger.error(Lang.getInstance().getInvalidHashRecordMessage(), line);
                return false;
            }

            final Path file = basePath.resolve(r.component2).toAbsolutePath();
            if (Files.notExists(file)) {
                Logger.error(Lang.getInstance().getFileNotExistMessage(), file);
                return false;
            } else if (Files.isDirectory(file)) {
                Logger.error(Lang.getInstance().getPathIsDirMessage(), file);
                return false;
            } else if (!Files.isReadable(file)) {
                Logger.error(Lang.getInstance().getFileCannotBeReadMessage(), file);
                return false;
            }

            final String fileHash = hasher.hashFile(file);
            if (!recordHashValue.equalsIgnoreCase(fileHash)) {
                Logger.error(Lang.getInstance().getHashNotMatchMessage(), file, fileHash, recordHashValue);
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
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads, new ChecksumThreadFactory());
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
                    Logger.logErrorAndExit(Lang.getInstance().getNoMatchHasher());
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
            Logger.info(Lang.getInstance().getVerificationCompletedMessage(), successCount.longValue(), failureCount.longValue());
        }
    }
}
