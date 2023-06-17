import java.io.RandomAccessFile
import java.net.*
import kotlin.random.Random

plugins {
    java
    application
}

group = "org.glavo"
version = "0.14.0" + "-SNAPSHOT"

val mainName = "org.glavo.checksum.Main"

application {
    mainClass.set(mainName)
}

tasks.compileJava {
    options.release.set(11)
    options.encoding = "UTF-8"
}

val versionFile = buildDir.resolve("version.txt")
val createBuildProperties by tasks.registering {
    group = "build"
    outputs.file(versionFile)

    doLast {
        versionFile.writeText("$version")
    }
}

tasks.processResources {
    dependsOn(createBuildProperties)

    into("org/glavo/checksum") {
        from(versionFile)
    }
}

val executableJar by tasks.registering {
    group = "build"

    val outputFile = file("$buildDir/libs/gchecksum-${project.version}.sh")

    dependsOn(tasks.jar)
    outputs.file(outputFile)

    doLast {
        outputFile.outputStream().use { output ->
            file("$rootDir/src/main/shell/header.sh").inputStream().use { input ->
                output.write(input.readAllBytes())
            }
            output.write(tasks.jar.get().archiveFile.get().asFile.readBytes())
        }
        outputFile.setExecutable(true)
    }
}

tasks.build { dependsOn(executableJar) }

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    manifest.attributes(
        mapOf(
            "Main-Class" to mainName,
            "Implementation-URL" to "https://github.com/Glavo/gchecksum",
            "Implementation-Vendor" to "Glavo",
            // "Multi-Release" to "true"
        )
    )
}

val sampleFilesDir = file("$buildDir/sample")

val generateSampleFiles by tasks.registering {
    outputs.dir(sampleFilesDir)

    doLast {
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

enum class OS {
    Linux, Windows, MacOS, Unknown;

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

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")

    testImplementation("com.google.jimfs:jimfs:1.2")

    val lwjglVersion = "3.3.2"
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

    testImplementation("org.lwjgl:lwjgl:$lwjglVersion")
    testImplementation("org.lwjgl:lwjgl:$lwjglVersion:natives-$lwjglPlatform")
    testImplementation("org.lwjgl:lwjgl-xxhash:$lwjglVersion")
    testImplementation("org.lwjgl:lwjgl-xxhash:$lwjglVersion:natives-$lwjglPlatform")
    testImplementation("net.openhft:zero-allocation-hashing:0.16")
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}


//region native-image

val graalHome: String
    get() = System.getenv("GRAALVM_HOME") ?: throw GradleException("Missing GRAALVM_HOME")

fun downloadFile(url: String, file: File): File {
    try {
        file.parentFile.mkdirs()

        val connection = URL(url).openConnection()
        connection.connect()

        val length = connection.contentLengthLong

        if (connection is HttpURLConnection && file.exists()
            && length == file.length()
            && connection.lastModified == file.lastModified()
        ) {
            logger.info("$url has not changed")
            connection.disconnect()
            return file
        }

        logger.info("Download $url to $file")

        file.outputStream().use { output ->
            connection.getInputStream().use { input ->
                input.transferTo(output)
            }
        }

        if (connection is HttpURLConnection && connection.lastModified > 0) {
            file.setLastModified(connection.lastModified)
        }

        return file
    } catch (e: Throwable) {
        throw GradleException("Failed to download from $url to $file ", e)
    }
}

fun nativeImageCommand(
    pgoInstrument: Boolean = false,
    pgo: List<File>? = null,
    targetArch: Arch = arch
): List<String> {
    val cmd: MutableList<String> = mutableListOf()

    cmd += file("$graalHome/bin/" + if (os == OS.Windows) "native-image.cmd" else "native-image").absolutePath

    if (os == OS.Windows && System.getProperty("user.language") != "en") {
        cmd += "-H:-CheckToolchain"
    }

    if (targetArch == Arch.X86_64) {
        cmd += "-march=x86-64-v2"
    }

    // not working yet
    if (targetArch != arch && targetArch == Arch.RISCV64) {
        val dir = file("$buildDir/graal-external-deps").absoluteFile

        val capcacheDir = dir.resolve("riscv-capcache-1.0")
        val staticLibraryDir = dir.resolve("riscv-static-libraries-1.0")

        val capcache = downloadFile(
            "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/riscv-capcache-1.0.tar.gz",
            dir.resolve("riscv-capcache-1.0.tar.gz")
        )
        val staticLibrary = downloadFile(
            "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/riscv-static-libraries-1.0.tar.gz",
            dir.resolve("riscv-static-libraries-1.0.tar.gz")
        )

        if (capcacheDir.exists()) {
            capcacheDir.deleteRecursively()
        }
        if (staticLibrary.exists()) {
            staticLibraryDir.deleteRecursively()
        }

        capcacheDir.mkdirs()
        staticLibraryDir.mkdirs()

        exec { commandLine("tar", "-xf", capcache, "-C", capcacheDir) }.assertNormalExitValue()
        exec { commandLine("tar", "-xf", staticLibrary, "-C", staticLibraryDir) }.assertNormalExitValue()


        // ---

        cmd += listOf(
            "-H:CompilerBackend=llvm",
            "-Dsvm.targetPlatformArch=riscv64",
            "-H:CAPCacheDir=${capcacheDir.resolve("capcache")}",
            "-H:CCompilerPath=/usr/bin/riscv64-linux-gnu-gcc",
            "-H:CustomLD=/usr/bin/riscv64-linux-gnu-ld",
            "-H:CLibraryPath=${staticLibraryDir.resolve("riscv-static-libraries")}",
            "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.riscv64=org.graalvm.nativeimage.builder"
        )
    }

    if (pgoInstrument) {
        cmd += "--pgo-instrument"
    }

    if (pgo != null) {
        cmd += "--pbo=" + pgo.joinToString(",") { it.absolutePath }
    }

    val targetFileNameBase = "${project.name}-${project.version}-${os.classifier}-${targetArch.classifier}"
    val targetFile = file("$buildDir/libs/$targetFileNameBase" + if (os == OS.Windows) ".exe" else "").absolutePath

    cmd += "-o"
    cmd += targetFile

    cmd += "-jar"
    cmd += tasks.jar.get().archiveFile.get().asFile.absolutePath


    return cmd
}

val buildNativeImage by tasks.registering {
    group = "build"
    dependsOn(tasks.jar)

    doLast {
        exec {
            workingDir(file("$buildDir/libs"))
            commandLine(nativeImageCommand(
                pgoInstrument = project.properties["graal.native.pgoInstrument"] == "true",
                pgo = project.properties["graal.native.pgo"]?.let { paths ->
                    paths.toString().split(File.pathSeparator).map { file(it) }
                }
            ))
        }
    }
}

val trackNativeImageConfiguration by tasks.registering {
    dependsOn(tasks.jar)
    doLast {
        val cmd = listOf(
            file("$graalHome/bin/java").absolutePath,
            "-agentlib:native-image-agent=config-output-dir=${buildDir.resolve("native-image-config")}",
            "-jar",
            tasks.jar.get().archiveFile.get().asFile.absolutePath
        )

        logger.quiet(cmd.joinToString(" ") { if (it.contains(' ')) "'$it'" else it })

//        exec {
//            workingDir(buildDir)
//            commandLine(cmd)
//        }
    }
}

//endregion
