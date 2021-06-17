package org.glavo.checksum;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public final class Main {


    private static void reportMissArg(String opt) {
        Logger.error(Resources.getInstance().getMissArgMessage(), opt);
        System.exit(1);
    }

    private static void reportParamRespecified(String opt) {
        Logger.error(Resources.getInstance().getParamRespecifiedMessage(), opt);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        final Resources resources = Resources.getInstance();

        Mode mode = null;
        String checksumsFile = null;
        boolean stdio = false;
        String directory = null;
        String inputs = null;
        Hasher algorithm = null;
        int numThreads = 0;


        boolean skipFirst = true;
        //region Parse Args
        final int argsLength = args.length;
        if (argsLength == 0) {
            mode = Mode.Verify;

        } else {
            switch (args[0]) {
                case "v":
                case "verify":
                    mode = Mode.Verify;
                    break;
                case "c":
                case "create":
                    mode = Mode.Create;
                    break;
                default:
                    skipFirst = false;
            }
        }
        for (int i = skipFirst ? 1 : 0; i < argsLength; i++) {
            final String currentArg = args[i];
            switch (currentArg) {
                case "-h":
                case "--help":
                case "-?":
                    System.out.println(Resources.getInstance().getHelpMessage());
                    return;
                case "-v":
                case "--version":
                    System.out.println(resources.getVersionInformation());
                    return;
                case "-f":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    if (checksumsFile != null) {
                        reportParamRespecified(currentArg);
                    }
                    if (stdio) {
                        Logger.error(resources.getOptionMixedMessage(), "-f", "-x");
                        System.exit(1);
                    }
                    checksumsFile = args[++i];
                    break;
                case "-x":
                    if (stdio) {
                        reportParamRespecified(currentArg);
                    }
                    if (checksumsFile != null) {
                        Logger.error(resources.getOptionMixedMessage(), "-f", "-x");
                        System.exit(1);
                    }
                    stdio = true;
                    break;
                case "-d":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    if (directory != null) {
                        reportParamRespecified(currentArg);
                    }
                    //noinspection ConstantConditions
                    if (inputs != null) {
                        Logger.error(resources.getOptionMixedMessage(), "-d", "-i");
                        System.exit(1);
                    }
                    directory = args[++i];
                    break;
                case "-i":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    //noinspection ConstantConditions
                    if (inputs != null) {
                        reportParamRespecified(currentArg);
                    }
                    if (directory != null) {
                        Logger.error(resources.getOptionMixedMessage(), "-d", "-i");
                        System.exit(1);
                    }
                    inputs = args[++i];
                    Logger.error("error: -i option is not yet supported");
                    System.exit(1);
                    break;
                case "-a":
                case "--algorithm":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    if (algorithm != null) {
                        reportParamRespecified(currentArg);
                    }
                    String algoName = args[++i];
                    algorithm = Hasher.ofName(algoName);
                    if (algorithm == null) {
                        Logger.error(resources.getUnsupportedAlgorithmMessage(), algoName);
                        System.exit(1);
                    }
                    break;
                case "-n":
                case "--num-threads":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    if (numThreads != 0) {
                        reportParamRespecified(currentArg);
                    }
                    String nt = args[++i];
                    int n = 0;
                    try {
                        n = Integer.parseInt(nt);
                    } catch (NumberFormatException ignored) {
                    }
                    if (n <= 0) {
                        Logger.error(resources.getInvalidArgMessage(), nt);
                        System.exit(1);
                    }
                    numThreads = n;
                    break;
                default:
                    Logger.logErrorAndExit(resources.getInvalidOptionMessage(), currentArg);
                    break;
            }
        }
        //endregion

        if (numThreads == 0) {
            numThreads = Integer.max(1, Runtime.getRuntime().availableProcessors() / 2);
        }

        final Path basePath = Paths.get(directory == null ? "" : directory).toAbsolutePath();
        if (Files.notExists(basePath)) {
            Logger.error(resources.getPathNotExistMessage(), basePath);
            System.exit(1);
        } else if (!Files.isDirectory(basePath)) {
            Logger.error(resources.getPathIsAFileMessage(), basePath);
            System.exit(1);
        }

        if (!stdio && checksumsFile == null) {
            checksumsFile = "checksums.txt";
        }

        if (mode == null) {
            mode = Mode.Verify;
        }

        switch (mode) {
            case Verify: {
                BufferedReader reader;
                if (stdio) {
                    reader = new BufferedReader(new InputStreamReader(System.in));
                } else {
                    final Path cf = Paths.get(checksumsFile).toAbsolutePath();
                    if (Files.notExists(cf)) {
                        Logger.error(resources.getFileNotExistMessage(), cf);
                        System.exit(1);
                    } else if (!Files.isReadable(cf)) {
                        Logger.error(resources.getFileCannotBeReadMessage(), cf);
                        System.exit(1);
                    }
                    reader = Files.newBufferedReader(cf);
                }
                try {
                    verify(basePath, reader, algorithm, numThreads);
                } finally {
                    reader.close();
                }
                break;
            }
            case Create: {
                if (algorithm == null) {
                    algorithm = Hasher.getDefault();
                }
                PrintWriter writer;
                Path exclude = null;
                if (stdio) {
                    writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
                } else {
                    final Path cf = Paths.get(checksumsFile).toAbsolutePath();
                    if (Files.isDirectory(cf)) {
                        Logger.logErrorAndExit(resources.getPathIsDirMessage(), cf);
                    }
                    if (Files.exists(cf)) {
                        ; //TODO
                    }
                    exclude = cf;
                    writer = new PrintWriter(Files.newBufferedWriter(cf));
                }
                try {
                    create(basePath, writer, exclude, algorithm, numThreads);
                } finally {
                    writer.close();
                }
                break;
            }
        }

    }

    private static void verify(Path basePath, BufferedReader reader, Hasher hasher, int numThreads)
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
                    //noinspection ConstantConditions
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

    private static boolean verifyFile(Path basePath, String line, Hasher hasher) {
        try {
            final int idx = line.indexOf(' ');
            if (idx != hasher.getHashStringLength()) {
                Logger.error(Resources.getInstance().getInvalidHashRecordMessage(), line);
                return false;
            }
            final String recordHashValue = line.substring(0, idx);
            final String filePath = line.substring(idx + 1);

            final Path file = basePath.resolve(filePath).toAbsolutePath();
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
            // TODO
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

    private static void create(Path basePath, PrintWriter writer, Path exclude, Hasher hasher, int numThreads) throws Exception {
        if (numThreads == 1) {
            FTW<String> ftw = new FTW<String>(exclude) {
                @Override
                protected final String processFile(Path file) throws IOException {
                    return hasher.hashFile(file);
                }
            };
            Files.walkFileTree(basePath, ftw);
            ftw.result().forEach((k, v) -> {
                writer.print(v);
                writer.print(" ");
                writer.println(String.join("/", k));
            });
        } else {
            final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
            try {
                FTW<Future<String>> ftw = new FTW<Future<String>>(exclude) {
                    @Override
                    protected final Future<String> processFile(Path file) throws IOException {
                        return executorService.submit(() -> hasher.hashFile(file));
                    }
                };
                Files.walkFileTree(basePath, ftw);
                ftw.result().forEach((k, v) -> {
                    try {
                        writer.print(v.get());
                        writer.print(" ");
                        writer.println(String.join("/", k));
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
            } finally {
                executorService.shutdown();
            }
        }
    }
}
