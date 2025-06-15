import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.contextable.agui4k.client.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "agui4k-client"
            packageVersion = "1.0.0"
            
            windows {
                menuGroup = "AG-UI"
                upgradeUuid = "18159995-d967-4e32-82f1-5c9c9e1fe56e"
            }
            
            macOS {
                bundleID = "com.contextable.agui4k.client"
            }
        }
    }
}
