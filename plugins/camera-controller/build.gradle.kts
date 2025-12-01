plugins {
    `java-library`
}

group = "com.egg.plugins"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Dependencia al launcher para acceder a la API de plugins
    // En un escenario real, esto sería una API separada
    compileOnly(files("../../launcher/build/libs/launcher.jar"))

    // Dependencia a slf4j para logging
    compileOnly("org.slf4j:slf4j-api:2.0.7")
}

tasks.jar {
    archiveBaseName.set("camera-controller")
    archiveVersion.set("")
    archiveClassifier.set("")
}
