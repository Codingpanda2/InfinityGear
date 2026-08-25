plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.infinitypickaxes"
version = "2.0.0-SNAPSHOT"

val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.auxilor.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
    compileOnly("com.willfp:EcoEnchants:2026.33")
    compileOnly("com.willfp:eco:2026.33")
    compileOnly("com.willfp:libreforge:2026.33")
    compileOnly("com.willfp:libreforge-loader:2026.33")
    compileOnly("me.clip:placeholderapi:2.12.3")

    implementation("org.xerial:sqlite-jdbc:3.50.3.0")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.20.0")
    mockitoAgent("org.mockito:mockito-core:5.20.0") { isTransitive = false }
    testImplementation("io.papermc.paper:paper-api:26.2.build.112-stable")
    testImplementation("me.clip:placeholderapi:2.12.3")
    testRuntimeOnly("com.willfp:EcoEnchants:2026.33") { isTransitive = false }
    testRuntimeOnly("com.willfp:eco:2026.33") { isTransitive = false }
    testRuntimeOnly("com.willfp:libreforge:2026.33:shadow") { isTransitive = false }
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}

tasks.shadowJar {
    archiveClassifier = ""
    mergeServiceFiles()

    exclude("org/sqlite/native/FreeBSD/**")
    exclude("org/sqlite/native/Linux-Android/**")
    exclude("org/sqlite/native/Mac/**")
    exclude("org/sqlite/native/Windows/**")
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
