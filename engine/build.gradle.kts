plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.21"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.yaml:snakeyaml:2.2")

    testImplementation(kotlin("test"))
}

// JVM 17 toolchain (compatible with Android's tooling), auto-provisioned via the
// foojay resolver if not already installed, applied consistently to both Kotlin and
// Java compile tasks to avoid target-compatibility mismatches between them.
kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    // Lets tests read the real rules/adaptation_rules.yaml and index.json from the repo
    // root, so parsing is proven against the actual source-of-truth files, not copies.
    systemProperty("masterbot.repoRoot", rootDir.absolutePath)
}
