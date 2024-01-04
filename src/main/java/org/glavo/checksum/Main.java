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
    public static void main(String[] args) {
        final Lang resources = Lang.getInstance();

        Iterator<String> iterator = Arrays.asList(args).iterator();

        try {
            if (args.length == 0 || args[0].startsWith("-")) {
                Verify.verify(iterator, args.length == 0);
            } else {
                String mode = iterator.next();
                switch (mode) {
                    case "v":
                    case "verify":
                        Verify.verify(iterator, false);
                        break;
                    case "c":
                    case "create":
                        CreateOrUpdate.createOrUpdate(iterator, false);
                        break;
                    case "u":
                    case "update":
                        CreateOrUpdate.createOrUpdate(iterator, true);
                        break;
                    default:
                        Logger.error(Lang.getInstance().getUnknownModeMessage(mode));
                        System.exit(1);
                }
            }
        } catch (Exit exit) {
            System.exit(exit.getExitCode());
        } catch (IOException e) {
            Logger.error(Lang.getInstance().getReadWriteErrorMessage(), e);
        }
    }
}
