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
import org.glavo.checksum.path.ArrayPathComparator;
import org.glavo.checksum.util.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

public final class CreateOrUpdate {
    private static abstract class Visitor<T> implements FileVisitor<Path> {
        private final String[] pathBuffer = new String[256]; // tmp
        private int count = -1;

        final TreeMap<String[], T> result = new TreeMap<>(ArrayPathComparator.INSTANCE);

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
                    Logger.error(Lang.getInstance().getFileCannotBeReadMessage(file));
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            Logger.error(Lang.getInstance().getErrorOccurredMessage(file), e);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            count--;
            return FileVisitResult.CONTINUE;
        }
    }

    private static void doCreate(String[] pathArray, String v, Writer writer) throws IOException {
        writer.write(v);
        writer.write("  ");

        final int length = pathArray.length;
        if (length != 0) {
            writer.write(pathArray[0]);
            for (int i = 1; i < length; i++) {
                writer.write('/');
                writer.write(pathArray[i]);
            }
        }
        writer.write('\n');
    }

    private static void doUpdate(String[] pathArray, String newHash, Writer writer, Map<String, String> old) throws IOException {
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
            Logger.info(Lang.getInstance().getNewFileBeRecordedMessage(path));
        } else if (!oldHash.equalsIgnoreCase(newHash)) {
            Logger.info(Lang.getInstance().getFileHashUpdatedMessage(path, newHash, oldHash));
        }

        writer.write(newHash);
        writer.write("  ");
        writer.write(path);
        writer.write('\n');
    }

    public static void createOrUpdate(
            Path basePath,
            Writer writer,
            Path exclude,
            Hasher hasher,
            int numThreads,
            Map<String, String> old) throws IOException {

        final Set<FileVisitOption> fileVisitOptions = Collections.singleton(FileVisitOption.FOLLOW_LINKS);

        if (numThreads > 1) {
            final ExecutorService pool = Executors.newFixedThreadPool(numThreads, new ChecksumThreadFactory());
            final Visitor<Future<String>> visitor = new Visitor<>(exclude) {
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
            final Visitor<String> visitor = new Visitor<>(exclude) {
                @Override
                protected String submit(Path file) throws IOException {
                    return hasher.hashFile(file);
                }
            };
            Files.walkFileTree(basePath, fileVisitOptions, Integer.MAX_VALUE, visitor);

            if (old != null) {
                visitor.result.forEach((pathArray, hash) -> {
                    try {
                        doUpdate(pathArray, hash, writer, old);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                visitor.result.forEach((pathArray, hash) -> {
                    try {
                        doCreate(pathArray, hash, writer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        Logger.info(Lang.getInstance().getDoneMessage());
    }

    public static void createOrUpdate(Options options, boolean update) throws IOException, Exit {
        if (options.algorithm == null) {
            options.algorithm = Hasher.getDefault();
        }
        Map<String, String> old = null;
        Writer writer;
        Path exclude = null;
        if ("-".equals(options.checksumsFile)) {
            if (update) {
                Logger.error(Lang.getInstance().getInvalidOptionValueMessage("-f", "-"));
                throw Exit.success();
            }

            String stdoutEncoding = System.getProperty("stdout.encoding", System.getProperty("sun.stdout.encoding", System.getProperty("native.encoding")));
            Charset charset;
            if (stdoutEncoding != null) {
                charset = Charset.forName(stdoutEncoding);
            } else {
                charset = Charset.defaultCharset();
            }
            writer = new BufferedWriter(new OutputStreamWriter(System.out, charset));
        } else {
            final Path cf = Paths.get(options.checksumsFile).toAbsolutePath();
            if (Files.isDirectory(cf)) {
                Logger.error(Lang.getInstance().getPathIsDirMessage(cf));
                throw Exit.error();
            }
            if (Files.exists(cf)) {
                if (update) {
                    old = new HashMap<>();
                    boolean hasError = false;
                    try (BufferedReader r = Files.newBufferedReader(cf)) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (!line.isEmpty()) {
                                final Pair<String, String> p = Utils.spiltRecord(line);
                                if (p == null || !options.algorithm.isAcceptChecksum(p.component1)) {
                                    Logger.error(Lang.getInstance().getInvalidHashRecordMessage(line));
                                    hasError = true;
                                } else {
                                    String oldHash = old.put(p.component2, p.component1);
                                    if (oldHash != null) {
                                        Logger.error(Lang.getInstance().getDuplicateHashRecordMessage(p.component2));
                                        hasError = true;
                                    }
                                }
                            }
                        }
                    }

                    if (hasError && !options.assumeYes) {
                        Logger.error(Lang.getInstance().getHasErrorMessage(cf));
                        if (!IOUtils.readChoice()) {
                            return;
                        }
                    }
                } else if (!options.assumeYes) {
                    Logger.error(Lang.getInstance().getOverwriteFileMessage(cf));
                    if (!IOUtils.readChoice()) {
                        return;
                    }
                }
            } else if (update && !options.assumeYes) {
                Logger.error(Lang.getInstance().getCreateFileMessage(cf));
                if (!IOUtils.readChoice()) {
                    return;
                }
            }
            exclude = cf;
            writer = Files.newBufferedWriter(cf);
        }

        try {
            CreateOrUpdate.createOrUpdate(options.basePath, writer, exclude, options.algorithm, options.numThreads, old);
        } finally {
            writer.close();
        }
    }
}
