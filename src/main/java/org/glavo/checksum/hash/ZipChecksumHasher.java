package org.glavo.checksum.hash;

import org.glavo.checksum.util.Utils;

import java.util.function.Supplier;
import java.util.zip.Checksum;

final class ZipChecksumHasher extends HasherBase<ZipChecksumHasher.Context> {
    static final ZipChecksumHasher CRC32 = new ZipChecksumHasher(java.util.zip.CRC32::new);
    static final ZipChecksumHasher ADLER32 = new ZipChecksumHasher(java.util.zip.Adler32::new);

    private final Supplier<Checksum> supplier;

    ZipChecksumHasher(Supplier<Checksum> supplier) {
        super(4);
        this.supplier = supplier;
    }

    @Override
    protected Context createContext() {
        return new Context(supplier.get());
    }

    static final class Context extends HasherBase.Context {
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
            return Utils.encodeHex32(checksum.getValue());
        }

        @Override
        protected void reset() {
            super.reset();
            checksum.reset();
        }
    }
}
