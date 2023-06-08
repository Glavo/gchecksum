package org.glavo.checksum.hash;

import com.google.common.jimfs.Jimfs;
import org.glavo.checksum.util.IOUtils;
import org.glavo.checksum.util.Utils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.util.xxhash.XXH128Hash;
import org.lwjgl.util.xxhash.XXHash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XxHash3_128HasherTest {
    private static IntStream testArguments() {
        return IntStream.rangeClosed(0, 3);
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void test(int length) throws IOException {
        byte[] data = new byte[length];
        new Random(length).nextBytes(data);

        ByteBuffer nativeBuffer = ByteBuffer.allocateDirect(length);
        nativeBuffer.put(data);
        nativeBuffer.clear();


        String expected;
        try (XXH128Hash result = XXH128Hash.malloc()) {
            XXHash.XXH3_128bits_withSeed(nativeBuffer, 0L, result);
            expected = Utils.encodeHex(result.low64(), result.high64());
        }

        String actual;
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("test.dat");
            Files.write(path, data);

            actual = XxHash3_128Hasher.DEFAULT.hashFile(path);
        }

        assertEquals(expected, actual, () -> Utils.encodeHex(data));
    }
}
