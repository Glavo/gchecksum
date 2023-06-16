import org.gradle.internal.impldep.org.apache.http.HttpConnection
import java.io.IOException
import java.net.*

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

for (multiVersion in 9..21) {
    if (!project.file("src/main/java$multiVersion").exists()) {
        continue
    }

    val multiSourceSet = sourceSets.create("java$multiVersion") {
        java.srcDir("src/main/java$multiVersion")
    }

    tasks.named<JavaCompile>("compileJava${multiVersion}Java") {
        sourceCompatibility = "$multiVersion"
        targetCompatibility = "$multiVersion"
    }

    dependencies {
        "java${multiVersion}Implementation"(sourceSets.main.get().output.classesDirs)
    }

    tasks.withType(org.gradle.jvm.tasks.Jar::class) {
        into("META-INF/versions/${multiVersion}") {
            from(multiSourceSet.output)
        }
    }
}

val graalHome: String
    get() = System.getenv("GRAALVM_HOME") ?: throw GradleException("Missing GRAALVM_HOME")

enum class OS {
    Linux, Windows, MacOS, Unknown;

    val classifier: String = name.lowercase()
}

enum class Arch {
    X86, X86_64, ARM32, ARM64, RISCV64, Unknown;

    val classifier: String = name.lowercase()
}

val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()!!.let {
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
        throw IOException("Failed to download from $url to $file ", e)
    }
}

fun nativeImageCommand(
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
            commandLine(nativeImageCommand())
        }
    }
}

val buildNativeImageRISCV by tasks.registering {
    group = "build"
    dependsOn(tasks.jar)

    doLast {
        exec {
            workingDir(file("$buildDir/libs"))
            commandLine(nativeImageCommand(targetArch = Arch.RISCV64))
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

tasks.build.get().dependsOn(executableJar)

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")

    testImplementation("com.google.jimfs:jimfs:1.2")

    val lwjglVersion = "3.3.2"
    val lwjglPlatform = when (os to arch) {
        Pair(OS.Windows, Arch.X86_64) -> "windows"
        Pair(OS.Windows, Arch.X86) -> "windows-x86"
        Pair(OS.Windows, Arch.ARM64) -> "windows-arm64"

        Pair(OS.MacOS, Arch.X86_64) -> "macos"
        Pair(OS.MacOS, Arch.ARM64) -> "macos-arm64"

        Pair(OS.Linux, Arch.X86_64) -> "linux"
        Pair(OS.Linux, Arch.X86) -> "linux-x86"
        Pair(OS.Linux, Arch.ARM64) -> "linux-arm64"
        Pair(OS.Linux, Arch.ARM32) -> "linux-arm32"

        else -> "linux"
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