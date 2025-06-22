// Root build script for AG-UI-4K multiplatform library
// All modules are configured individually - see each module's build.gradle.kts

plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    id("com.android.library") version "8.10.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Configure all subprojects with common settings
subprojects {
    group = "com.contextable.agui4k"
    version = "0.1.6"
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}