plugins {
    java
    application
}

group = "org.glavo"
version = "0.1.0-SNAPSHOT"

val mainName = "org.glavo.checksum.Main"

application {
    mainClass.set(mainName)
}

tasks.compileJava {
    options.release.set(8)
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest.attributes(
        mapOf(
            "Main-Class" to mainName
        )
    )
}

val executableJar by tasks.registering {
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

tasks.build.get().dependsOn(executableJar)
