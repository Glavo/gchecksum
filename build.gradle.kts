import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.nio.file.*
import java.util.*
import kotlin.io.path.*


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
    options.release.set(8)
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
            "Multi-Release" to "true"
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
    get() = properties["graalvm.home"]?.toString() ?: System.getenv("GRAALVM_HOME")
    ?: throw GradleException("Missing GRAALVM_HOME")

val os = DefaultNativePlatform.getCurrentOperatingSystem()!!
val arch = DefaultNativePlatform.getCurrentArchitecture()!!

val buildNativeImage by tasks.registering {
    group = "build"
    dependsOn(tasks.jar)

    doLast {
        val cmd: MutableList<String> = mutableListOf()

        cmd += Paths.get(graalHome, "bin", if (os.isWindows) "native-image.cmd" else "native-image")
            .absolutePathString()

        if (os.isWindows && Locale.getDefault().language != "en") {
            cmd += "-H:-CheckToolchain"
        }

        if (arch.isAmd64) {
            cmd += "-march=x86-64-v2"
        }

        cmd += "-jar"
        cmd += tasks.jar.get().archiveFile.get().asFile.absolutePath

        exec {
            workingDir(file("$buildDir/libs"))
            commandLine(cmd)
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
    val lwjglPlatform = when {
        os.isWindows && arch.isAmd64 -> "windows"
        os.isWindows && arch.isI386 -> "windows-x86"
        os.isWindows && arch.isArm64 -> "windows-arm64"

        os.isMacOsX && arch.isAmd64 -> "macos"
        os.isMacOsX && arch.isArm64 -> "macos-arm64"

        os.isLinux && arch.isAmd64 -> "linux"
        os.isLinux && arch.isArm64 -> "linux-arm64"
        os.isLinux && arch.isArm32 -> "linux-arm32"

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