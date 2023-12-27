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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Main {
    public static void main(String[] args) throws Exception {
        final Lang resources = Lang.getInstance();

        Mode mode = Mode.Verify;

        Iterator<String> iterator = Arrays.asList(args).iterator();

        if (args.length != 0) {
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
                        iterator.next();
                    } else {
                        Logger.logErrorAndExit(Lang.getInstance().getUnknownModeMessage(), firstArg);
                        return;
                    }
            }
        }

        OptionParser options = new OptionParser(iterator);
        options.parse();

        if (options.numThreads == 0) {
            options.numThreads = 4;
        }

        final Path basePath = Paths.get(options.directory == null ? "" : options.directory).toAbsolutePath();
        if (Files.notExists(basePath)) {
            Logger.logErrorAndExit(resources.getPathNotExistMessage(), basePath);
        } else if (!Files.isDirectory(basePath)) {
            Logger.logErrorAndExit(resources.getPathIsAFileMessage(), basePath);
        }

        if (options.checksumsFile == null) {
            options.checksumsFile = "checksums.txt";
        }

        switch (mode) {
            case Verify: {
                BufferedReader reader;
                if ("-".equals(options.checksumsFile)) {
                    reader = new BufferedReader(new InputStreamReader(System.in));
                } else {
                    final Path cf = Paths.get(options.checksumsFile).toAbsolutePath();
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
                    Verify.verify(basePath, reader, options.algorithm, options.numThreads);
                } finally {
                    reader.close();
                }
                break;
            }
            case Update:
            case Create: {
                if (options.algorithm == null) {
                    options.algorithm = Hasher.getDefault();
                }
                Map<String, String> old = null;
                PrintWriter writer;
                Path exclude = null;
                if ("-".equals(options.checksumsFile)) {
                    if (mode == Mode.Update) {
                        Logger.logErrorAndExit(resources.getInvalidOptionValueMessage(), "-f");
                    }
                    writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
                } else {
                    final Path cf = Paths.get(options.checksumsFile).toAbsolutePath();
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
                                        if (p == null || !options.algorithm.isAcceptChecksum(p.component1)) {
                                            Logger.error(resources.getInvalidHashRecordMessage(), line);
                                        } else {
                                            old.put(p.component2, p.component1);// TODO
                                        }
                                    }
                                }
                            }
                        } else if (!options.assumeYes) {
                            Logger.error(resources.getOverwriteFileMessage(), cf);
                            if (!IOUtils.readChoice()) {
                                return;
                            }
                        }
                    } else if (mode == Mode.Update && !options.assumeYes) {
                        Logger.error(resources.getCreateFileMessage(), cf);
                        if (!IOUtils.readChoice()) {
                            return;
                        }
                    }
                    exclude = cf;
                    writer = new PrintWriter(Files.newBufferedWriter(cf));
                }

                try {
                    CreateOrUpdate.update(basePath, writer, exclude, options.algorithm, options.numThreads, old);
                } finally {
                    writer.close();
                }
                break;
            }
        }

    }
}
