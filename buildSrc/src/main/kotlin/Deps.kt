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

object Deps {
    val lwjglVersion = "3.3.3"
    val lwjglPlatform = when (os) {
        OS.Windows -> "windows"
        OS.MacOS -> "macos"
        else -> "linux" // OS.Linux -> "linux"
    } + when (arch) {
        Arch.X86_64 -> ""
        Arch.X86 -> "-x86"
        Arch.ARM64 -> "-arm64"
        Arch.ARM32 -> "-arm32"
        Arch.RISCV64 -> "-riscv64"
        else -> ""
    }

    fun lwjgl(name: String) = "org.lwjgl:$name:$lwjglVersion"
    fun lwjglNatives(name: String) = "org.lwjgl:$name:$lwjglVersion:natives-$lwjglPlatform"

    val junitVersion = "5.10.1"
    fun junit(name: String) = "org.junit.jupiter:$name:$junitVersion"
}