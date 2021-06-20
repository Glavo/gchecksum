package org.glavo.checksum.mode;

import org.glavo.checksum.Hasher;
import org.glavo.checksum.util.Logger;
import org.glavo.checksum.Resources;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;

public final class CreateOrUpdate {

    private static abstract class FTW<T> implements FileVisitor<Path> {
        String[] pathBuffer = new String[256]; // tmp
        int count = -1;

        private final TreeMap<String[], T> result = new TreeMap<>((x, y) -> {
            final int xLength = x.length;
            final int yLength = y.length;

            int length = Math.min(xLength, yLength);
            for (int i = 0; i < length; i++) {
                int v = x[i].compareTo(y[i]);
                if (v != 0) {
                    return v;
                }
            }

            return xLength - yLength;
        });

        private final Path exclude;

        protected FTW(Path exclude) {
            this.exclude = exclude;
        }

        protected abstract T processFile(Path file) throws IOException;

        @Override
        public final FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (count >= 0) {
                pathBuffer[count] = dir.getFileName().toString();
            }
            count++;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public final FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!file.equals(exclude)) {
                if (Files.isReadable(file)) {
                    final String[] p = Arrays.copyOf(pathBuffer, count + 1);
                    p[count] = file.getFileName().toString();
                    result.put(p, processFile(file));
                } else {
                    Logger.error(Resources.getInstance().getFileCannotBeReadMessage(), file);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public final FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            exc.printStackTrace(); // TODO
            return FileVisitResult.CONTINUE;
        }

        @Override
        public final FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            count--;
            return FileVisitResult.CONTINUE;
        }

        public final TreeMap<String[], T> result() {
            return result;
        }
    }

    private static void printPath(PrintWriter writer, String[] path) {
        final int length = path.length;
        if (length != 0) {
            writer.print(path[0]);
            for (int i = 1; i < length; i++) {
                writer.print('/');
                writer.print(path[i]);
            }
        }
        writer.println();
    }

    private static String joinPath(String[] sa) {
        final int length = sa.length;
        if (length == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(80);
        builder.append(sa[0]);
        for (int i = 1; i < length; i++) {
            builder.append('/');
            builder.append(sa[i]);
        }
        return builder.toString();
    }

    public static void updateInSingleThread(
            Path basePath,
            PrintWriter writer,
            Path exclude,
            Hasher hasher,
            Map<String, String> old) throws IOException {
        FTW<String> ftw = new FTW<String>(exclude) {
            @Override
            protected final String processFile(Path file) throws IOException {
                return hasher.hashFile(file);
            }
        };
        Files.walkFileTree(basePath, ftw);
        if (old == null) {
            ftw.result().forEach((k, v) -> {
                writer.print(v);
                writer.print("  ");
                printPath(writer, k);
            });
        } else {
            ftw.result().forEach((k, v) -> {
                final String path = joinPath(k);
                String oldHash = old.remove(path);
                if (oldHash == null) {
                    Logger.info(Resources.getInstance().getNewFileBeRecordedMessage(), path);
                } else if (!oldHash.equalsIgnoreCase(v)) {
                    Logger.info(Resources.getInstance().getFileHashUpdatedMessage(), path, oldHash, v);
                }

                writer.print(v);
                writer.print("  ");
                writer.println(path);
            });
            old.forEach((path, oldHash) -> Logger.info(Resources.getInstance().getFileRecordBeRemoved(), path));
        }
        Logger.info(Resources.getInstance().getDoneMessage());
    }

    public static void update(
            Path basePath,
            PrintWriter writer,
            Path exclude,
            Hasher hasher,
            int numThreads,
            Map<String, String> old) throws Exception {
        final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        try {
            FTW<Future<String>> ftw = new FTW<Future<String>>(exclude) {
                @Override
                protected final Future<String> processFile(Path file) {
                    return executorService.submit(() -> hasher.hashFile(file));
                }
            };
            Files.walkFileTree(basePath, ftw);
            if(old == null) {
                ftw.result().forEach((k, v) -> {
                    try {
                        writer.print(v.get());
                        writer.print("  ");
                        printPath(writer, k);
                    } catch (InterruptedException | CancellationException e) {
                        throw new AssertionError(e);
                    } catch (ExecutionException e) {
                        e.printStackTrace(); // TODO
                    }
                });
            } else {
                ftw.result().forEach((k, v) -> {
                    try {
                        final String newHash = v.get();
                        final String path = joinPath(k);

                        String oldHash = old.remove(path);
                        if (oldHash == null) {
                            Logger.info(Resources.getInstance().getNewFileBeRecordedMessage(), path);
                        } else if (!oldHash.equalsIgnoreCase(newHash)) {
                            Logger.info(Resources.getInstance().getFileHashUpdatedMessage(), path, oldHash, newHash);
                        }

                        writer.print(newHash);
                        writer.print("  ");
                        writer.println(path);
                    } catch (InterruptedException | CancellationException e) {
                        throw new AssertionError(e);
                    } catch (ExecutionException e) {
                        e.printStackTrace(); // TODO
                    }
                });
            }
            Logger.info(Resources.getInstance().getDoneMessage());
        } finally {
            executorService.shutdown();
        }
    }
}
