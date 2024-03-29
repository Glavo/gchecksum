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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class MessageDigestHasher extends HasherBase {

    static final MessageDigestHasher MD5 = new MessageDigestHasher("MD5", 16);
    static final MessageDigestHasher SHA_1 = new MessageDigestHasher("SHA-1", 20);
    static final MessageDigestHasher SHA_224 = new MessageDigestHasher("SHA-224", 28);
    static final MessageDigestHasher SHA_256 = new MessageDigestHasher("SHA-256", 32);
    static final MessageDigestHasher SHA_384 = new MessageDigestHasher("SHA-384", 48);
    static final MessageDigestHasher SHA_512 = new MessageDigestHasher("SHA-512", 64);

    private final String algorithm;

    MessageDigestHasher(String algorithm, int digestLength) {
        super(digestLength);
        this.algorithm = algorithm;
    }

    @Override
    protected Context createContext() {
        try {
            return new Context(MessageDigest.getInstance(algorithm));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e.getMessage(), e);
        }
    }

    protected static final class Context extends HasherBase.Context {
        private final MessageDigest md;

        Context(MessageDigest md) {
            this.md = md;
        }

        @Override
        protected void update(byte[] input, int offset, int len) {
            md.update(input, offset, len);
        }

        @Override
        protected String digest() {
            return Utils.encodeHex(md.digest());
        }

        @Override
        protected void reset() {
            md.reset();
        }
    }
}
