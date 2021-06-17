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