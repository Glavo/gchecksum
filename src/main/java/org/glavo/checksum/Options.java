/*
 * Copyright 2023 Glavo
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
import org.glavo.checksum.util.Lang;
import org.glavo.checksum.util.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class Options {
    protected final Lang lang = Lang.getInstance();

    protected final Iterator<String> iterator;

    public boolean assumeYes;
    public String checksumsFile;
    public String directory;
    public String inputs;
    public Hasher algorithm;
    public Integer numThreads;

    public final Path basePath;

    public Options(Iterator<String> iterator) throws Exit {
        this.iterator = iterator;

        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.startsWith("-")) {
                parseOption(arg);
            } else {
                Logger.error(lang.getInvalidOptionMessage(arg));
                throw Exit.error();
            }
        }

        if (numThreads == null) {
            numThreads = 4;
        }

        basePath = Paths.get(directory == null ? "" : directory).toAbsolutePath();
        if (Files.notExists(basePath)) {
            Logger.error(lang.getPathNotExistMessage(basePath));
            throw Exit.error();
        } else if (!Files.isDirectory(basePath)) {
            Logger.error(lang.getPathIsAFileMessage(basePath));
            throw Exit.error();
        }

        if (checksumsFile == null) {
            checksumsFile = "checksums.txt";
        }
    }

    protected static void reportMissArg(String opt) throws Exit {
        Logger.error(Lang.getInstance().getMissArgMessage(opt));
        throw Exit.error();
    }

    protected static void reportParamRespecified(String opt) throws Exit {
        Logger.error(Lang.getInstance().getOptionRespecifiedMessage(opt));
        throw Exit.error();
    }

    protected void parseOption(String option) throws Exit {
        switch (option) {
            case "-f":
                if (!iterator.hasNext()) {
                    reportMissArg(option);
                }
                if (checksumsFile != null) {
                    reportParamRespecified(option);
                }
                checksumsFile = iterator.next();
                break;
            case "-d":
                if (!iterator.hasNext()) {
                    reportMissArg(option);
                }
                if (directory != null) {
                    reportParamRespecified(option);
                }
                if (inputs != null) {
                    Logger.error(lang.getOptionMixedMessage("-d", "-i"));
                    throw Exit.error();
                }
                directory = iterator.next();
                break;
            case "-i":
                if (!iterator.hasNext()) {
                    reportMissArg(option);
                }
                if (inputs != null) {
                    reportParamRespecified(option);
                }
                if (directory != null) {
                    Logger.error(lang.getOptionMixedMessage("-d", "-i"));
                    throw Exit.error();
                }
                inputs = iterator.next();
                Logger.error("error: -i option is not yet supported");
                throw Exit.error();
            case "-a":
            case "--algorithm":
                if (!iterator.hasNext()) {
                    reportMissArg(option);
                }
                if (algorithm != null) {
                    reportParamRespecified(option);
                }
                String algoName = iterator.next();
                algorithm = Hasher.ofName(algoName);
                if (algorithm == null) {
                    Logger.error(lang.getUnsupportedAlgorithmMessage(algoName));
                    throw Exit.error();
                }
                break;
            case "-n":
            case "--num-threads":
                if (!iterator.hasNext()) {
                    reportMissArg(option);
                }
                if (numThreads != null) {
                    reportParamRespecified(option);
                }
                String nt = iterator.next();
                int n = 0;
                try {
                    n = Integer.parseInt(nt);
                } catch (NumberFormatException ignored) {
                }
                if (n <= 0) {
                    Logger.error(lang.getInvalidOptionValueMessage(option, nt));
                    throw Exit.error();
                }
                numThreads = n;
                break;
            case "-y":
            case "--yes":
            case "--assume-yes":
                if (assumeYes) {
                    reportParamRespecified(option);
                }
                assumeYes = true;
                break;
            default:
                Logger.error(lang.getInvalidOptionMessage(option));
                throw Exit.error();
        }
    }
}
