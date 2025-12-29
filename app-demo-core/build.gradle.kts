plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    kotlin("kapt")
}

dependencies {
    implementation(project(":utils"))
    kapt(project(":processor"))

    implementation(libs.bundles.kotlinxEcosystem)
    testImplementation(kotlin("test"))
}

