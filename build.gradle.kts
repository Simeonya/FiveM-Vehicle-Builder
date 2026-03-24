import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.3"
}

group = "dev.simeonya"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainModule.set("dev.simeonya.fivemvehiclebuilder")
    mainClass.set("dev.simeonya.Main")
}

javafx {
    version = "21.0.4"
    modules = listOf("javafx.controls")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("-Dfile.encoding=UTF-8")
}

jlink {
    imageName.set("fivem-vehicle-builder")

    launcher {
        name = "fivem-vehicle-builder"
    }
}