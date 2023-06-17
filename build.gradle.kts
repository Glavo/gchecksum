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
        SampleFileGenerator(sampleFilesDir).generate()
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
