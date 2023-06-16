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

package org.glavo.checksum.path;

import java.util.Comparator;

public final class ArrayPathComparator implements Comparator<String[]> {

    public static final ArrayPathComparator INSTANCE = new ArrayPathComparator();

    @Override
    public int compare(String[] x, String[] y) {
        final int xLength = x.length;
        final int yLength = y.length;

        int length = Math.min(xLength, yLength);
        assert length > 0;
        for (int i = 0; i < length - 1; i++) {
            int v = x[i].compareTo(y[i]);
            if (v != 0) {
                return v;
            }
        }

        if (xLength == yLength)
            return x[length - 1].compareTo(y[length - 1]);
        else
            return Integer.compare(xLength, yLength);
    }
}
