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

val buildDir = layout.buildDirectory.asFile.get()

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
        SampleFileGenerator(sampleFilesDir).generate()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val junitVersion = "5.10.1"

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")

    testImplementation("com.google.jimfs:jimfs:1.3.0")

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
