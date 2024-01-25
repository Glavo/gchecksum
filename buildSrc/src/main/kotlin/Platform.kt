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

enum class OS {
    Linux, Windows, MacOS, FreeBSD, Unknown;

    val classifier: String = name.lowercase()
}

enum class Arch {
    X86, X86_64, ARM32, ARM64, RISCV64, Unknown;

    val classifier: String = name.lowercase()
}

val os: OS = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()!!.let {
    when {
        it.isLinux -> OS.Linux
        it.isWindows -> OS.Windows
        it.isMacOsX -> OS.MacOS
        it.isFreeBSD -> OS.FreeBSD
        else -> OS.Unknown
    }
}

val arch = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentArchitecture()!!.let {
    val osArch = System.getProperty("os.arch").lowercase()

    when {
        it.isI386 -> Arch.X86
        it.isAmd64 -> Arch.X86_64
        it.isArm32 -> Arch.ARM32
        it.isArm64 -> Arch.ARM64
        osArch == "riscv64" -> Arch.RISCV64
        else -> Arch.Unknown
    }
}

