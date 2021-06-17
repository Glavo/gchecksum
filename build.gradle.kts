plugins {
    java
    application
}

group = "org.glavo"
version = "0.3.0" + "-SNAPSHOT"

val mainName = "org.glavo.checksum.Main"

application {
    mainClass.set(mainName)
}

tasks.compileJava {
    options.release.set(8)
    options.encoding = "UTF-8"
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

val nativeImageJar by tasks.registering(org.gradle.jvm.tasks.Jar::class) {
    archiveClassifier.set("native-image")
    from(sourceSets.main.get().output)
    from("src/main/native-image")
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    manifest.attributes(
        mapOf(
            "Main-Class" to mainName
        )
    )
}

tasks.build.get().dependsOn(executableJar)
tasks.build.get().dependsOn(nativeImageJar)
