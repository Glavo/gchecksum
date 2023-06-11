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

package org.glavo.checksum;

import org.glavo.checksum.hash.Hasher;
import org.glavo.checksum.mode.CreateOrUpdate;
import org.glavo.checksum.mode.Mode;
import org.glavo.checksum.mode.Verify;
import org.glavo.checksum.util.*;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class Main {

    private static void reportMissArg(String opt) {
        Logger.logErrorAndExit(Lang.getInstance().getMissArgMessage(), opt);
    }

    private static void reportParamRespecified(String opt) {
        Logger.logErrorAndExit(Lang.getInstance().getParamRespecifiedMessage(), opt);
    }

    private static void printRuntimeInformation() {
        final String[] properties = {
                "java.home",
                "java.runtime.name",
                "java.runtime.version",
                "java.vm.name",
                "java.vm.info",
                "java.vm.vendor",
                "java.vm.version",
                "os.name",
                "os.arch",
                "os.version",
                "path.separator",
                "sun.boot.library.path",
                "user.dir",
                "user.language",
                "file.encoding",
                "file.separator",
                "native.encoding"
        };

        System.out.println(Lang.getInstance().getVersionInformation());
        System.out.println();

        int maxLength = 0;
        for (String property : properties) {
            maxLength = Integer.max(property.length(), maxLength);
        }

        System.out.println("Property settings:");
        for (String key : properties) {
            System.out.printf("    %-" + maxLength + "s = %s%n", key, System.getProperty(key));
        }

        System.out.println("Crypto settings:");
        try {
            System.out.println("    provider = " + Cipher.getInstance("AES/GCM/NoPadding").getProvider());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        final Lang resources = Lang.getInstance();

        Mode mode = Mode.Verify;
        boolean assumeYes = false;
        String checksumsFile = null;
        String directory = null;
        String inputs = null;
        Hasher algorithm = null;
        int numThreads = 0;


        //region Parse Args
        boolean skipFirst = true;
        final int argsLength = args.length;
        if (argsLength != 0) {
            String firstArg = args[0];
            switch (firstArg) {
                case "v":
                case "verify":
                    mode = Mode.Verify;
                    break;
                case "c":
                case "create":
                    mode = Mode.Create;
                    break;
                case "u":
                case "update":
                    mode = Mode.Update;
                    break;
                default:
                    if (firstArg.startsWith("-")) {
                        skipFirst = false;
                    } else {
                        Logger.logErrorAndExit(Lang.getInstance().getUnknownModeMessage(), firstArg);
                    }
            }
        }
        for (int i = skipFirst ? 1 : 0; i < argsLength; i++) {
            final String currentArg = args[i];
            switch (currentArg) {
                case "-?":
                case "-h":
                case "--help":
                    System.out.println(Lang.getInstance().getHelpMessage());
                    return;
                case "-v":
                case "--version":
                    System.out.println(resources.getVersionInformation());
                    return;
                case "--print-runtime-information":
                    printRuntimeInformation();
                    return;
                case "-f":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    if (checksumsFile != null) {
                        reportParamRespecified(currentArg);
                    }
                    checksumsFile = args[++i];
                    break;
                case "-d":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    if (directory != null) {
                        reportParamRespecified(currentArg);
                    }
                    if (inputs != null) {
                        Logger.logErrorAndExit(resources.getOptionMixedMessage(), "-d", "-i");
                    }
                    directory = args[++i];
                    break;
                case "-i":
                    if (i == argsLength - 1) {
                        reportMissArg(currentArg);
                    }
                    if (inputs != null) {
                        reportParamRespecified(currentArg);
                    }
                    if (directory != null) {
                        Logger.logErrorAndExit(resources.getOptionMixedMessage(), "-d", "-i");
                    }
                    inputs = args[++i];
                    Logger.logErrorAndExit("error: -i option is not yet supported");
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
                        Logger.logErrorAndExit(resources.getUnsupportedAlgorithmMessage(), algoName);
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
                        Logger.logErrorAndExit(resources.getInvalidOptionValueMessage(), nt);
                    }
                    numThreads = n;
                    break;
                case "-y":
                case "--yes":
                case "--assume-yes":
                    if (assumeYes) {
                        reportParamRespecified(currentArg);
                    }
                    assumeYes = true;
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
            Logger.logErrorAndExit(resources.getPathNotExistMessage(), basePath);
        } else if (!Files.isDirectory(basePath)) {
            Logger.logErrorAndExit(resources.getPathIsAFileMessage(), basePath);
        }

        if (checksumsFile == null) {
            checksumsFile = "checksums.txt";
        }

        switch (mode) {
            case Verify: {
                BufferedReader reader;
                if ("-".equals(checksumsFile)) {
                    reader = new BufferedReader(new InputStreamReader(System.in));
                } else {
                    final Path cf = Paths.get(checksumsFile).toAbsolutePath();
                    if (Files.notExists(cf)) {
                        if (args.length == 0) {
                            System.out.println(Lang.getInstance().getHelpMessage());
                            System.exit(1);
                        } else {
                            Logger.logErrorAndExit(resources.getFileNotExistMessage(), cf);
                        }
                    } else if (!Files.isReadable(cf)) {
                        Logger.logErrorAndExit(resources.getFileCannotBeReadMessage(), cf);
                    }
                    reader = Files.newBufferedReader(cf);
                }
                try {
                    Verify.verify(basePath, reader, algorithm, numThreads);
                } finally {
                    reader.close();
                }
                break;
            }
            case Update:
            case Create: {
                if (algorithm == null) {
                    algorithm = Hasher.getDefault();
                }
                Map<String, String> old = null;
                PrintWriter writer;
                Path exclude = null;
                if ("-".equals(checksumsFile)) {
                    if (mode == Mode.Update) {
                        Logger.logErrorAndExit(resources.getInvalidOptionValueMessage(), "-f");
                    }
                    writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
                } else {
                    final Path cf = Paths.get(checksumsFile).toAbsolutePath();
                    if (Files.isDirectory(cf)) {
                        Logger.logErrorAndExit(resources.getPathIsDirMessage(), cf);
                    }
                    if (Files.exists(cf)) {
                        if (mode == Mode.Update) {
                            old = new HashMap<>();
                            try (BufferedReader r = Files.newBufferedReader(cf)) {
                                String line;
                                while ((line = r.readLine()) != null) {
                                    if (!line.isEmpty()) {
                                        final Pair<String, String> p = Utils.spiltRecord(line);
                                        if (p == null || !algorithm.isAcceptChecksum(p.component1)) {
                                            Logger.error(resources.getInvalidHashRecordMessage(), line);
                                        } else {
                                            old.put(p.component2, p.component1);// TODO
                                        }
                                    }
                                }
                            }
                        } else if (!assumeYes) {
                            Logger.error(resources.getOverwriteFileMessage(), cf);
                            if (!IOUtils.readChoice()) {
                                return;
                            }
                        }
                    } else if (mode == Mode.Update && !assumeYes) {
                        Logger.error(resources.getCreateFileMessage(), cf);
                        if (!IOUtils.readChoice()) {
                            return;
                        }
                    }
                    exclude = cf;
                    writer = new PrintWriter(Files.newBufferedWriter(cf));
                }

                try {
                    CreateOrUpdate.update(basePath, writer, exclude, algorithm, numThreads, old);
                } finally {
                    writer.close();
                }
                break;
            }
        }

    }

}
