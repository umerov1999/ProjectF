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
    implementation("androidx.annotation:annotation:1.7.0-beta01")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0-RC")

    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.13")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}
