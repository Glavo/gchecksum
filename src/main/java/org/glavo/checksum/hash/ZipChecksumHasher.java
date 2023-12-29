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

package org.glavo.checksum.hash;

import org.glavo.checksum.util.Utils;

import java.util.function.Supplier;
import java.util.zip.Checksum;

final class ZipChecksumHasher extends HasherBase {
    private final Supplier<Checksum> supplier;

    public ZipChecksumHasher(Supplier<Checksum> supplier) {
        super(4);
        this.supplier = supplier;
    }

    @Override
    protected HasherBase.Context createContext() {
        return new Context(supplier.get());
    }

    private static final class Context extends HasherBase.Context {
        private final Checksum checksum;

        Context(Checksum checksum) {
            this.checksum = checksum;
        }

        @Override
        protected void update(byte[] input, int offset, int len) {
            checksum.update(input, offset, len);
        }

        @Override
        protected String digest() {
            return Utils.encodeHex((int) checksum.getValue());
        }

        @Override
        protected void reset() {
            checksum.reset();
        }
    }
}
