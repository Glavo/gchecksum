package org.glavo.checksum.mode;

import org.glavo.checksum.Hasher;
import org.glavo.checksum.util.HasherThreadFactory;
import org.glavo.checksum.util.Logger;
import org.glavo.checksum.Resources;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;

public final class CreateOrUpdate {

    public static void update(
            Path basePath,
            PrintWriter writer,
            Path exclude,
            Hasher hasher,
            int numThreads,
            Map<String, String> old) throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads, new HasherThreadFactory());
        final TreeMap<String[], Future<String>> result = new TreeMap<>((x, y) -> {
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

        Files.walkFileTree(basePath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new FileVisitor<Path>() {
            private final String[] pathBuffer = new String[256]; // tmp
            private int count = -1;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (count >= 0) {
                    pathBuffer[count] = dir.getFileName().toString();
                }
                count++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!file.equals(exclude)) {
                    if (attrs.isRegularFile() && Files.isReadable(file)) {
                        final int count = this.count;

                        final String[] p = Arrays.copyOf(pathBuffer, count + 1);
                        p[count] = file.getFileName().toString();
                        result.put(p, pool.submit(() -> hasher.hashFile(file)));
                    } else {
                        Logger.error(Resources.getInstance().getFileCannotBeReadMessage(), file);
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
        });
        pool.shutdown();

        if (old == null) {
            result.forEach((k, v) -> {
                try {
                    writer.print(v.get());
                    writer.print("  ");

                    final int length = k.length;
                    if (length != 0) {
                        writer.print(k[0]);
                        for (int i = 1; i < length; i++) {
                            writer.print('/');
                            writer.print(k[i]);
                        }
                    }
                    writer.println();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        } else {
            result.forEach((k, v) -> {
                try {
                    final String newHash = v.get();
                    final String path;

                    final int length = k.length;
                    if (length == 0) {
                        path = "";
                    } else {
                        final StringBuilder builder = new StringBuilder(80);
                        builder.append(k[0]);
                        for (int i = 1; i < length; i++) {
                            builder.append('/');
                            builder.append(k[i]);
                        }
                        path = builder.toString();
                    }


                    String oldHash = old.remove(path);
                    if (oldHash == null) {
                        Logger.info(Resources.getInstance().getNewFileBeRecordedMessage(), path);
                    } else if (!oldHash.equalsIgnoreCase(newHash)) {
                        Logger.info(Resources.getInstance().getFileHashUpdatedMessage(), path, oldHash, newHash);
                    }

                    writer.print(newHash);
                    writer.print("  ");
                    writer.println(path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }

        Logger.info(Resources.getInstance().getDoneMessage());
    }
}
