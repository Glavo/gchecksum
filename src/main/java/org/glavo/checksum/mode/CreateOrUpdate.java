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
import org.glavo.checksum.util.ChecksumThreadFactory;
import org.glavo.checksum.util.Logger;
import org.glavo.checksum.util.Lang;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

public final class CreateOrUpdate {
    private static final Comparator<String[]> PATH_COMPARATOR = (x, y) -> {
        final int xLength = x.length;
        final int yLength = y.length;

        int length = Math.min(xLength, yLength);
        assert length > 0;
        for (int i = 0; i < length - 1; i++) {
            int v = x[i].compareTo(y[i]);
            if (v != 0) {
                return v;
            }
        }

        if (xLength == yLength)
            return x[length - 1].compareTo(y[length - 1]);
        else
            return Integer.compare(xLength, yLength);
    };

    private static abstract class Visitor<T> implements FileVisitor<Path> {
        private final String[] pathBuffer = new String[256]; // tmp
        private int count = -1;

        final TreeMap<String[], T> result = new TreeMap<>(PATH_COMPARATOR);

        private final Path exclude;

        Visitor(Path exclude) {
            this.exclude = exclude;
        }

        protected abstract T submit(Path file) throws IOException;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (count >= 0) {
                pathBuffer[count] = dir.getFileName().toString();
            }
            count++;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!file.equals(exclude)) {
                if (attrs.isRegularFile() && Files.isReadable(file)) {
                    final int count = this.count;

                    final String[] p = Arrays.copyOf(pathBuffer, count + 1);
                    p[count] = file.getFileName().toString();
                    result.put(p, submit(file));
                } else {
                    Logger.error(Lang.getInstance().getFileCannotBeReadMessage(), file);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            exc.printStackTrace();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            count--;
            return FileVisitResult.CONTINUE;
        }
    }

    private static void doCreate(String[] pathArray, String v, PrintWriter writer) {
        try {
            writer.print(v);
            writer.print("  ");

            final int length = pathArray.length;
            if (length != 0) {
                writer.print(pathArray[0]);
                for (int i = 1; i < length; i++) {
                    writer.print('/');
                    writer.print(pathArray[i]);
                }
            }
            writer.println();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void doUpdate(String[] pathArray, String newHash, PrintWriter writer, Map<String, String> old) {
        try {
            final String path;

            final int length = pathArray.length;
            if (length == 0) {
                path = "";
            } else {
                final StringBuilder builder = new StringBuilder(80);
                builder.append(pathArray[0]);
                for (int i = 1; i < length; i++) {
                    builder.append('/');
                    builder.append(pathArray[i]);
                }
                path = builder.toString();
            }


            String oldHash = old.remove(path);
            if (oldHash == null) {
                Logger.info(Lang.getInstance().getNewFileBeRecordedMessage(), path);
            } else if (!oldHash.equalsIgnoreCase(newHash)) {
                Logger.info(Lang.getInstance().getFileHashUpdatedMessage(), path, oldHash, newHash);
            }

            writer.print(newHash);
            writer.print("  ");
            writer.println(path);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void update(
            Path basePath,
            PrintWriter writer,
            Path exclude,
            Hasher hasher,
            int numThreads,
            Map<String, String> old) throws Exception {

        final Set<FileVisitOption> fileVisitOptions = Collections.singleton(FileVisitOption.FOLLOW_LINKS);

        if (numThreads > 1) {
            final ExecutorService pool = Executors.newFixedThreadPool(numThreads, new ChecksumThreadFactory());
            final Visitor<Future<String>> visitor = new Visitor<Future<String>>(exclude) {
                @Override
                protected Future<String> submit(Path file) {
                    return pool.submit(() -> hasher.hashFile(file));
                }
            };

            try {
                Files.walkFileTree(basePath, fileVisitOptions, Integer.MAX_VALUE, visitor);
            } finally {
                pool.shutdown();
            }

            if (old != null) {
                visitor.result.forEach((pathArray, future) -> {
                    try {
                        doUpdate(pathArray, future.get(), writer, old);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
            } else {
                visitor.result.forEach((pathArray, future) -> {
                    try {
                        doCreate(pathArray, future.get(), writer);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            final Visitor<String> visitor = new Visitor<String>(exclude) {
                @Override
                protected String submit(Path file) throws IOException {
                    return hasher.hashFile(file);
                }
            };
            Files.walkFileTree(basePath, fileVisitOptions, Integer.MAX_VALUE, visitor);

            if (old != null) {
                visitor.result.forEach((pathArray, hash) -> doUpdate(pathArray, hash, writer, old));
            } else {
                visitor.result.forEach((pathArray, hash) -> doCreate(pathArray, hash, writer));
            }
        }

        Logger.info(Lang.getInstance().getDoneMessage());
    }
}
