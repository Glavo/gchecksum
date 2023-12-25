package org.glavo.checksum.hash;

import org.glavo.checksum.util.ByteBufferChannel;
import org.glavo.checksum.util.IOUtils;
import org.glavo.checksum.util.Utils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.ByteBuffer;
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

        ByteBuffer nativeBuffer = ByteBuffer.allocateDirect(length);
        nativeBuffer.put(data);
        nativeBuffer.clear();

        String expected = Utils.encodeHex(org.lwjgl.util.xxhash.XXHash.XXH64(nativeBuffer, 0L));
        String actual = XxHash64Hasher.DEFAULT.hash(new ByteBufferChannel(data));

        assertEquals(expected, actual, () -> Utils.encodeHex(data));
    }
}
