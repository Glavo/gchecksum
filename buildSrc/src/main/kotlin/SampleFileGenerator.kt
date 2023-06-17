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

import java.io.File
import java.io.RandomAccessFile
import kotlin.random.Random

class SampleFileGenerator(private val sampleFilesDir: File) {
    fun generate() {
        sampleFilesDir.deleteRecursively()

        sampleFilesDir.resolve("zero").also { dir ->
            dir.mkdirs()
            for (size in 0..2048) {
                RandomAccessFile(dir.resolve("size-%04d.bin".format(size)), "rw").use { it.setLength(size.toLong()) }
            }
        }

        for (seed in 0..4) {
            val dir = sampleFilesDir.resolve("small-$seed")
            dir.mkdirs()

            for (size in 1..4096) {
                dir.resolve("size-%04d.bin".format(size)).writeBytes(Random(seed).nextBytes(size))
            }
        }

        sampleFilesDir.resolve("large").also { dir ->
            dir.mkdirs()

            fun sizesOf(vararg baseSizes: Int): IntArray {
                val n = 9
                val arr = IntArray(baseSizes.size * n)
                for ((index, baseSize) in baseSizes.withIndex()) {
                    arr[index * n + 0] = baseSize - 64 - 1
                    arr[index * n + 1] = baseSize - 64
                    arr[index * n + 2] = baseSize - 64 + 1

                    arr[index * n + 3] = baseSize - 1
                    arr[index * n + 4] = baseSize
                    arr[index * n + 5] = baseSize + 1

                    arr[index * n + 6] = baseSize + 64 - 1
                    arr[index * n + 7] = baseSize + 64
                    arr[index * n + 8] = baseSize + 64 + 1
                }
                return arr
            }

            val bufferSize = 320 * 1024

            val sizes = sizesOf(
                bufferSize / 4 * 1,
                bufferSize / 4 * 2,
                bufferSize / 4 * 3,
                bufferSize / 4 * 4,
                bufferSize / 4 * 5,
                bufferSize / 4 * 6,
                bufferSize / 4 * 7,
                bufferSize / 4 * 8,
            )

            for (size in sizes) {
                dir.resolve("size-$size.bin").writeBytes(Random(0).nextBytes(size))
            }
        }
    }
}
