val kspVersion: String by project
plugins {
    kotlin("jvm")
}

group = "dev.umerov.ksp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.0-RC")

    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.0-1.0.6")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}
