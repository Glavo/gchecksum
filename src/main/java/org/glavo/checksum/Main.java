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

import org.glavo.checksum.mode.CreateOrUpdate;
import org.glavo.checksum.mode.Verify;
import org.glavo.checksum.util.Lang;
import org.glavo.checksum.util.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public final class Main {
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
    }

    public static void main(String[] args) {
        final Lang lang = Lang.getInstance();

        if (args.length == 0) {
            System.out.println(Lang.getInstance().getHelpMessage());
        }

        Iterator<String> iterator = Arrays.asList(args).iterator();
        try {
            String firstArg = args[0];
            if (!firstArg.startsWith("-")) {
                iterator.next();
            }

            switch (firstArg) {
                case "v":
                case "verify":
                    Verify.verify(iterator);
                    break;
                case "c":
                case "create":
                    CreateOrUpdate.createOrUpdate(iterator, false);
                    break;
                case "u":
                case "update":
                    CreateOrUpdate.createOrUpdate(iterator, true);
                    break;

                case "-?":
                case "-h":
                case "--help":
                case "help":
                    System.out.println(Lang.getInstance().getHelpMessage());
                    return;
                case "-v":
                case "--version":
                case "version":
                    System.out.println(lang.getVersionInformation());
                    return;
                case "--print-runtime-information":
                    printRuntimeInformation();
                    return;
                default:
                    if (firstArg.startsWith("-")) {
                        Logger.error(Lang.getInstance().getInvalidOptionMessage(firstArg));
                    } else {
                        Logger.error(Lang.getInstance().getUnknownModeMessage(firstArg));
                    }
                    System.exit(1);
            }
        } catch (Exit exit) {
            System.exit(exit.getExitCode());
        } catch (IOException e) {
            Logger.error(Lang.getInstance().getReadWriteErrorMessage(), e);
        }
    }
}
