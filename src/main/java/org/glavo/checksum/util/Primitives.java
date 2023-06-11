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

package org.glavo.checksum.util;

import java.nio.ByteOrder;

public final class Primitives {

    private Primitives() {
    }

    static final boolean NATIVE_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    public static long unsignedInt(int i) {
        return i & 0xFFFFFFFFL;
    }

    public static int unsignedShort(int s) {
        return s & 0xFFFF;
    }

    public static int unsignedByte(int b) {
        return b & 0xFF;
    }
}
