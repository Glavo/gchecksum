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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

final class ByteArrayImpl {
    private ByteArrayImpl() {
    }

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    static int getIntLE(byte[] array, int offset) {
        return (int) INT.get(array, offset);
    }

    static long getLongLE(byte[] array, int offset) {
        return (long) LONG.get(array, offset);
    }


    static void setIntLE(byte[] array, int offset, int value) {
        INT.set(array, offset, value);
    }

    static void setLongLE(byte[] array, int offset, long value) {
        LONG.set(array, offset, value);
    }
}
