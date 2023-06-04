val kspVersion: String by project
plugins {
    kotlin("jvm")
}

group = "dev.umerov.ksp"
version = "1.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")

    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-Beta-1.0.11")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}
