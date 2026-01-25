import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
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
    mainClass.set("dev.simeonya.Main")
}

javafx {
    version = "21.0.4"
    modules = listOf("javafx.controls")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec> {
    jvmArgs("-Dfile.encoding=UTF-8")
}
