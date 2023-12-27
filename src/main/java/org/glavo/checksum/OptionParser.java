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

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class OptionParser {
    protected final Lang lang = Lang.getInstance();

    protected final Iterator<String> iterator;

    public boolean assumeYes;
    public String checksumsFile;
    public String directory;
    public String inputs;
    public Hasher algorithm;
    public Integer numThreads;

    public OptionParser(Iterator<String> iterator) {
        this.iterator = iterator;
    }

    protected static void reportMissArg(String opt) throws Exit {
        Logger.error(Lang.getInstance().getMissArgMessage(), opt);
        throw Exit.error();
    }

    protected static void reportParamRespecified(String opt) throws Exit {
        Logger.error(Lang.getInstance().getParamRespecifiedMessage(), opt);
        throw Exit.error();
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

    public void parse() throws Exit {
        while (iterator.hasNext()) {
            String arg = iterator.next();
            switch (arg) {
                case "-?":
                case "-h":
                case "--help":
                    System.out.println(Lang.getInstance().getHelpMessage());
                    throw Exit.success();
                case "-v":
                case "--version":
                    System.out.println(lang.getVersionInformation());
                    throw Exit.success();
                case "--print-runtime-information":
                    printRuntimeInformation();
                    throw Exit.success();
                case "-f":
                    if (!iterator.hasNext()) {
                        reportMissArg(arg);
                    }
                    if (checksumsFile != null) {
                        reportParamRespecified(arg);
                    }
                    checksumsFile = iterator.next();
                    break;
                case "-d":
                    if (!iterator.hasNext()) {
                        reportMissArg(arg);
                    }
                    if (directory != null) {
                        reportParamRespecified(arg);
                    }
                    if (inputs != null) {
                        Logger.error(lang.getOptionMixedMessage(), "-d", "-i");
                        throw Exit.error();
                    }
                    directory = iterator.next();
                    break;
                case "-i":
                    if (!iterator.hasNext()) {
                        reportMissArg(arg);
                    }
                    if (inputs != null) {
                        reportParamRespecified(arg);
                    }
                    if (directory != null) {
                        Logger.error(lang.getOptionMixedMessage(), "-d", "-i");
                        throw Exit.error();
                    }
                    inputs = iterator.next();
                    Logger.error("error: -i option is not yet supported");
                    throw Exit.error();
                case "-a":
                case "--algorithm":
                    if (!iterator.hasNext()) {
                        reportMissArg(arg);
                    }
                    if (algorithm != null) {
                        reportParamRespecified(arg);
                    }
                    String algoName = iterator.next();
                    algorithm = Hasher.ofName(algoName);
                    if (algorithm == null) {
                        Logger.error(lang.getUnsupportedAlgorithmMessage(), algoName);
                        throw Exit.error();
                    }
                    break;
                case "-n":
                case "--num-threads":
                    if (!iterator.hasNext()) {
                        reportMissArg(arg);
                    }
                    if (numThreads != 0) {
                        reportParamRespecified(arg);
                    }
                    String nt = iterator.next();
                    int n = 0;
                    try {
                        n = Integer.parseInt(nt);
                    } catch (NumberFormatException ignored) {
                    }
                    if (n <= 0) {
                        Logger.error(lang.getInvalidOptionValueMessage(), nt);
                        throw Exit.error();
                    }
                    numThreads = n;
                    break;
                case "-y":
                case "--yes":
                case "--assume-yes":
                    if (assumeYes) {
                        reportParamRespecified(arg);
                    }
                    assumeYes = true;
                    break;
            }
        }
    }

    protected void parseOption(String option) throws Exit {
        Logger.error(lang.getInvalidOptionMessage(), option);
        throw Exit.error();
    }
}
