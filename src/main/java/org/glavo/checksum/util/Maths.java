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

// https://github.com/OpenHFT/Zero-Allocation-Hashing/blob/ea/src/main/java/net/openhft/hashing/Maths.java
public final class Maths {
    private Maths() {
    }

    public static long unsignedLongMulXorFold(final long lhs, final long rhs) {
        final long upper = Math.multiplyHigh(lhs, rhs) + ((lhs >> 63) & rhs) + ((rhs >> 63) & lhs);
        final long lower = lhs * rhs;
        return lower ^ upper;
    }

    public static long unsignedLongMulHigh(final long lhs, final long rhs) {
        return Math.multiplyHigh(lhs, rhs) + ((lhs >> 63) & rhs) + ((rhs >> 63) & lhs);
    }
}
