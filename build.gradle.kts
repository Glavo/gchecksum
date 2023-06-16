import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString


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
    Linux, Windows, MacOS, Unknown
}

enum class Arch {
    X86, X86_64, ARM32, ARM64, RISCV64, Unknown
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

fun nativeImageCommand(
    targetArch: Arch = arch
): List<String> {
    val cmd: MutableList<String> = mutableListOf()

    cmd += Paths.get(graalHome, "bin", if (os == OS.Windows) "native-image.cmd" else "native-image")
        .absolutePathString()

    if (os == OS.Windows && Locale.getDefault().language != "en") {
        cmd += "-H:-CheckToolchain"
    }

    if (arch == Arch.X86_64) {
        cmd += "-march=x86-64-v2"
    }


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

val trackNativeImageConfiguration by tasks.registering {
    dependsOn(tasks.jar)
    doLast {
        val cmd = listOf(
            Paths.get(graalHome, "bin", "java").absolutePathString(),
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