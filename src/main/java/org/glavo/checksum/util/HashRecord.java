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

package org.glavo.checksum.util;

import java.util.Objects;

public final class HashRecord {

    public static HashRecord of(String line) {
        final int rLength = line.length();

        final int idx = line.indexOf(' ');
        if (idx == -1 || idx == rLength - 1) {
            return null;
        }

        for (int i = idx + 1; i < rLength; i++) {
            if (line.charAt(i) != ' ') {
                return new HashRecord(line.substring(0, idx), line.substring(i));
            }
        }
        return null;
    }

    public final String hash;
    public final String file;

    public HashRecord(String hash, String file) {
        this.hash = hash;
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HashRecord)) {
            return false;
        }
        HashRecord hashRecord = (HashRecord) o;
        return Objects.equals(hash, hashRecord.hash) && Objects.equals(file, hashRecord.file);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hash) ^ Objects.hashCode(file);
    }

    @Override
    public String toString() {
        return hash + "  " + file;
    }

}
