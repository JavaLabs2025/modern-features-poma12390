import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
    id("java")
    id("application")
}

group = "org.lab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("org.lab.Main")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Чтобы компилировать preview-фичи (например pattern matching/switch preview, string templates и т.д.)
    options.compilerArgs.add("--enable-preview")
    // Фиксируем релиз, чтобы компиляция была строго под Java 26
    options.release.set(26)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Чтобы тесты могли запускать код с preview-фичами
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview", "-Dfile.encoding=UTF-8")
    standardInput = System.`in`
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview", "-Dfile.encoding=UTF-8")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    // Чтобы `gradlew run` запускал приложение с preview-фичами
    jvmArgs("--enable-preview")
}
