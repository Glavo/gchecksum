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

package org.glavo.checksum.mode;

import org.glavo.checksum.hash.Hasher;
import org.glavo.checksum.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

public final class Verify {
    private static boolean verifyFile(Path basePath, String line, Hasher hasher) {
        final Pair<String, String> r = Utils.spiltRecord(line);
        if (r == null) {
            Logger.error(Lang.getInstance().getInvalidHashRecordMessage(line));
            return false;
        }

        final String recordHashValue = r.component1;
        if (!hasher.isAcceptChecksum(recordHashValue)) {
            Logger.error(Lang.getInstance().getInvalidHashRecordMessage(line));
            return false;
        }

        final Path file = basePath.resolve(r.component2).toAbsolutePath();
        if (Files.notExists(file)) {
            Logger.error(Lang.getInstance().getFileNotExistMessage(file));
            return false;
        } else if (Files.isDirectory(file)) {
            Logger.error(Lang.getInstance().getPathIsDirMessage(file));
            return false;
        } else if (!Files.isReadable(file)) {
            Logger.error(Lang.getInstance().getFileCannotBeReadMessage(file));
            return false;
        }

        final String fileHash;
        try {
            fileHash = hasher.hashFile(file);
        } catch (IOException e) {
            synchronized (System.err) {
                Logger.error(Lang.getInstance().getErrorOccurredMessage(file));
                e.printStackTrace();
            }
            return false;
        }

        if (!recordHashValue.equalsIgnoreCase(fileHash)) {
            Logger.error(Lang.getInstance().getHashNotMatchMessage(file, fileHash, recordHashValue));
            return false;
        }
        return true;
    }

    private static void verify(BufferedReader reader, Hasher hasher, BiConsumer<String, Hasher> action) throws IOException {
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
                Logger.error(Lang.getInstance().getNoMatchHasher());
                System.exit(1);
            }

            action.accept(line, hasher);
        }
        while ((line = reader.readLine()) != null) {
            action.accept(line, hasher);
        }
    }

    public static void verify(Path basePath, BufferedReader reader, Hasher hasher, int numThreads)
            throws Exception {

        long successCount;
        long failureCount;

        if (numThreads > 1) {
            final LongAdder successCounter = new LongAdder();
            final LongAdder failureCounter = new LongAdder();
            final ExecutorService pool = Executors.newFixedThreadPool(numThreads, new ChecksumThreadFactory());

            try {
                verify(reader, hasher, (line, actualHasher) -> pool.submit(() -> {
                    if (verifyFile(basePath, line, actualHasher)) {
                        successCounter.increment();
                    } else {
                        failureCounter.increment();
                    }
                }));
            } finally {
                pool.shutdown();
            }

            if (!pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                throw new AssertionError();
            }

            successCount = successCounter.sum();
            failureCount = failureCounter.sum();
        } else {
            long[] counters = new long[2];
            verify(reader, hasher, (line, actualHasher) -> {
                if (verifyFile(basePath, line, actualHasher)) {
                    counters[0]++;
                } else {
                    counters[1]++;
                }
            });
            successCount = counters[0];
            failureCount = counters[1];
        }

        Logger.info(Lang.getInstance().getVerificationCompletedMessage(successCount, failureCount));
    }
}
