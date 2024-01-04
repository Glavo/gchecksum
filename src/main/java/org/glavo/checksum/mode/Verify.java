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

import org.glavo.checksum.Exit;
import org.glavo.checksum.Options;
import org.glavo.checksum.hash.Hasher;
import org.glavo.checksum.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

public final class Verify {
    private static boolean verifyFile(Path basePath, String line, Hasher hasher) {
        final HashRecord r = HashRecord.of(line);
        if (r == null) {
            Logger.error(Lang.getInstance().getInvalidHashRecordMessage(line));
            return false;
        }

        final String recordHashValue = r.hash;
        if (!hasher.isAcceptChecksum(recordHashValue)) {
            Logger.error(Lang.getInstance().getInvalidHashRecordMessage(line));
            return false;
        }

        final Path file = basePath.resolve(r.file).toAbsolutePath();
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
            Logger.error(Lang.getInstance().getHashErrorMessage(file), e);
            return false;
        }

        if (!recordHashValue.equalsIgnoreCase(fileHash)) {
            Logger.error(Lang.getInstance().getHashNotMatchMessage(file, fileHash, recordHashValue));
            return false;
        }
        return true;
    }

    private static void verify(BufferedReader reader, Hasher hasher, BiConsumer<String, Hasher> action) throws IOException, Exit {
        String line;
        if (hasher == null) {
            line = reader.readLine();
            while (line != null && line.isBlank()) {
                line = reader.readLine();
            }
            if (line == null) {
                return;
            }
            final int idx = line.indexOf(' ');
            hasher = Hasher.ofHashStringLength(idx);
            if (hasher == null) {
                Logger.error(Lang.getInstance().getNoMatchHasher());
                throw Exit.error();
            }

            action.accept(line, hasher);
        }
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                action.accept(line, hasher);
            }
        }
    }

    public static void verify(Path basePath, BufferedReader reader, Hasher hasher, int numThreads) throws IOException, Exit {
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

            try {
                if (!pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    throw new AssertionError();
                }
            } catch (InterruptedException e) {
                throw new AssertionError(e);
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

        if (failureCount > 0) {
            throw Exit.error();
        }
    }

    public static void verify(Iterator<String> args, boolean argsIsEmpty) throws IOException, Exit {
        Options options = new Options(args);

        BufferedReader reader;
        if ("-".equals(options.checksumsFile)) {
            reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        } else {
            final Path cf = Paths.get(options.checksumsFile).toAbsolutePath();
            if (Files.notExists(cf)) {
                if (argsIsEmpty) {
                    System.out.println(Lang.getInstance().getHelpMessage());
                } else {
                    Logger.error(Lang.getInstance().getFileNotExistMessage(cf));
                }
                throw Exit.error();
            } else if (!Files.isReadable(cf)) {
                Logger.error(Lang.getInstance().getFileCannotBeReadMessage(cf));
                throw Exit.error();
            }
            reader = Files.newBufferedReader(cf);
        }
        try (reader) {
            Verify.verify(options.basePath, reader, options.algorithm, options.numThreads);
        }
    }
}
