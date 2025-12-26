plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
    kotlin("kapt")
}

dependencies {
    implementation(project(":utils"))
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)
    testImplementation(kotlin("test"))

    implementation(libs.squareup.kotlin.poet)
    implementation(libs.squareup.kotlin.poet.ksp)
    implementation(libs.squareup.kotlin.poet.metadata)
//    implementation(libs.squareup.java.poet)
    implementation(libs.auto.service.annotations)
    kapt(libs.auto.service)

}