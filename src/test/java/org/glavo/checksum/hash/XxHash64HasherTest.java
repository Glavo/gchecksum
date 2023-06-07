package org.glavo.checksum.hash;

import com.google.common.jimfs.Jimfs;
import org.glavo.checksum.util.IOUtils;
import org.glavo.checksum.util.Utils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XxHash64HasherTest {

    private static IntStream testArguments() {
        return IntStream.concat(
                IntStream.rangeClosed(0, 32),
                IntStream.iterate(33, it -> it < 512, it -> it + 7)
        ).flatMap(it -> IntStream.of(it, it + IOUtils.DEFAULT_BUFFER_SIZE));
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void test(int length) throws IOException {
        byte[] data = new byte[length];
        new Random(length).nextBytes(data);

        String expected = Utils.encodeHex64(net.openhft.hashing.LongHashFunction.xx().hashBytes(data));

        String actual;
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("test.dat");
            Files.write(path, data);

            actual = XxHash64Hasher.DEFAULT.hashFile(path);
        }

        assertEquals(expected, actual, () -> Utils.encodeHex(data));
    }
}
