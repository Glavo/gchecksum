package org.glavo.checksum.hash;

import org.glavo.checksum.RandomUtils;
import org.glavo.checksum.util.ByteBufferChannel;
import org.glavo.checksum.util.Utils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.util.xxhash.XXH128Hash;
import org.lwjgl.util.xxhash.XXHash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.glavo.checksum.util.IOUtils.DEFAULT_BUFFER_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class XxHash3_128HasherTest {
    private static final int block_len = 1024;
    private static final int stripe_len = 64;

    private static final int i = DEFAULT_BUFFER_SIZE / 2 + 2 * block_len + 1;

    private static List<Arguments> testArguments() {

        int[] lengths = Stream.of(
                IntStream.rangeClosed(0, 2048),
                IntStream.rangeClosed(DEFAULT_BUFFER_SIZE / 2 - 2 * block_len - 1, DEFAULT_BUFFER_SIZE / 2 + 2 * block_len + 1),
                IntStream.rangeClosed(DEFAULT_BUFFER_SIZE - 2 * block_len - 1, DEFAULT_BUFFER_SIZE + 2 * block_len + 1),
                IntStream.rangeClosed(DEFAULT_BUFFER_SIZE * 2 - 2 * block_len - 1, DEFAULT_BUFFER_SIZE * 2 + 2 * block_len + 1),
                IntStream.rangeClosed(DEFAULT_BUFFER_SIZE * 3 / 2 - 2 * block_len - 1, DEFAULT_BUFFER_SIZE * 3 / 2+ 2 * block_len + 1)
        ).flatMapToInt(Function.identity()).toArray();

        int[] seeds = {0, 1};

        ArrayList<Arguments> res = new ArrayList<>();
        for (int length : lengths) {
            for (int seed : seeds) {
                res.add(Arguments.of(seed, length));
            }
        }
        return res;
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void test(int seed, int length) throws IOException {
        byte[] data = RandomUtils.getBytes(seed, length);

        ByteBuffer nativeBuffer = ByteBuffer.allocateDirect(length);
        nativeBuffer.put(data);
        nativeBuffer.clear();

        String expected;
        try (XXH128Hash result = XXH128Hash.malloc()) {
            XXHash.XXH3_128bits(nativeBuffer, result);
            expected = Utils.encodeHex(result.low64(), result.high64());
        }
        String actual = XxHash3_128Hasher.DEFAULT.hash(new ByteBufferChannel(data));;

        assertEquals(expected, actual, () -> Utils.encodeHex(data));
    }
}
