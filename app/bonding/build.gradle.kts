// Pure-Kotlin JVM module — no Android plugin, no heavy deps.
// Versions align with the root build (Kotlin 1.8.22); repositories come from
// the root settings.gradle.kts dependencyResolutionManagement.
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
