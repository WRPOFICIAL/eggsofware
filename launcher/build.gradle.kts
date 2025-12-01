plugins {
    `java-library`
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.egg"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // OSHI for system monitoring
    implementation("com.github.oshi:oshi-core:6.4.10")
    // Logging with Logback
    implementation("ch.qos.logback:logback-classic:1.4.14")
    // SnakeYAML for plugin configuration
    implementation("org.yaml:snakeyaml:2.2")
}

application {
    mainClass.set("com.egg.launcher.Main")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.egg.launcher.Main"
        )
    }
}

tasks.withType<JavaCompile> {
    // Force UTF-8 encoding for all Java compilation
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveBaseName.set("launcher")
    archiveClassifier.set("")
    archiveVersion.set("")
}
