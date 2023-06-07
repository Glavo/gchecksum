import java.nio.file.*

plugins {
    java
    application
}

group = "org.glavo"
version = "0.12.0" + "-SNAPSHOT"

val mainName = "org.glavo.checksum.Main"

application {
    mainClass.set(mainName)
}

tasks.compileJava {
    options.release.set(8)
    options.encoding = "UTF-8"
}

val generateVersionInfo by tasks.registering {
    val baseDir = file("$buildDir/version")
    baseDir.mkdirs()
    file("$baseDir/Version.txt").writeText("gchecksum $version")
}

val executableJar by tasks.registering {
    group = "build"

    val outputDir = file("$buildDir/libs")
    dependsOn(tasks.jar)

    doLast {
        outputDir.mkdirs()
        val outputFile = file("$outputDir/gchecksum-${project.version}")
        outputFile.outputStream().use { output ->
            file("$rootDir/src/main/shell/header.sh").inputStream().use { input ->
                output.write(input.readAllBytes())
            }
            output.write(tasks.jar.get().archiveFile.get().asFile.readBytes())
        }
        outputFile.setExecutable(true)

    }
}

val nativeImageJar by tasks.registering(org.gradle.jvm.tasks.Jar::class) {
    group = "build"
    archiveClassifier.set("native-image")
    from(sourceSets.main.get().output)
    from("src/main/native-image")
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    dependsOn(generateVersionInfo)
    manifest.attributes(
        mapOf(
            "Main-Class" to mainName,
            "Implementation-URL" to "https://github.com/Glavo/gchecksum",
            "Implementation-Vendor" to "Glavo",
            "Multi-Release" to "true"
        )
    )
    into("org/glavo/checksum") {
        from(file("$buildDir/version/Version.txt"))
    }
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

val buildNativeImage by tasks.registering {
    group = "build"
    dependsOn(nativeImageJar)

    doLast {
        val home = System.getenv("GRAALVM_HOME")
        if (home == null) {
            System.err.println("Missing GRAALVM_HOME")
        } else {
            val binPath = Paths.get(home).resolve("bin")

            val ni = binPath.resolve("native-image.cmd").let {
                if (Files.exists(it)) it else binPath.resolve("native-image")
            }.toAbsolutePath().toString()


            exec {
                workingDir(file("$buildDir/libs"))
                commandLine(
                    ni,
                    "-jar",
                    nativeImageJar.get().archiveFile.get().asFile
                )
            }
        }
    }
}

tasks.build.get().dependsOn(executableJar)
tasks.build.get().dependsOn(nativeImageJar)
