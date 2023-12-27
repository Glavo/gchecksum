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

public final class Exit extends Exception {

    public static Exit success() {
        return new Exit(0);
    }

    public static Exit error() {
        return new Exit(1);
    }

    private final int exitCode;

    private Exit(int exitCode) {
        super(Integer.toString(exitCode));
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
