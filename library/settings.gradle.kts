rootProject.name = "ag-ui-4k"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include all modules
include(":agui4k-core")
include(":agui4k-transport")
include(":agui4k-client")
include(":agui4k-tools")
include(":agui4k-tools-builtin")