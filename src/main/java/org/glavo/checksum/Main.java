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
import org.glavo.checksum.mode.Mode;
import org.glavo.checksum.mode.Verify;
import org.glavo.checksum.util.Lang;
import org.glavo.checksum.util.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public final class Main {
    public static void main(String[] args)  {
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
                        Logger.error(Lang.getInstance().getUnknownModeMessage(firstArg));
                        System.exit(1);
                        return;
                    }
            }
        }

        try {
            Options options = new Options(iterator);
            switch (mode) {
                case Verify: {
                    Verify.verify(options, args.length == 0);
                    break;
                }
                case Update: {
                    CreateOrUpdate.createOrUpdate(options, true);
                    break;
                }
                case Create: {
                    CreateOrUpdate.createOrUpdate(options, false);
                    break;
                }
            }
        } catch (Exit exit) {
            System.exit(exit.getExitCode());
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
    }
}
