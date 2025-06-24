plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // ag-ui-4k library - new multi-module structure
                implementation(libs.agui4k.core)
                implementation(libs.agui4k.transport)
                implementation(libs.agui4k.client)
                implementation(libs.agui4k.tools)
                implementation(libs.agui4k.tools.builtin)

                // Navigation
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenmodel)
                implementation(libs.voyager.transitions)

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Serialization
                implementation(libs.kotlinx.serialization.json)

                // Preferences
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.coroutines)

                // Logging
                implementation(libs.kotlin.logging)

                // DateTime
                implementation(libs.kotlinx.datetime)

                // Base64 encoding/decoding
                implementation(libs.okio)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)  // Add this line
            }
        }

        val androidMain by getting {
            dependencies {
                api(libs.activity.compose)
                api(libs.appcompat)
                api(libs.core.ktx)
                implementation(libs.logback.android)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.runner)
                implementation(libs.ext.junit)
                implementation(libs.core)

                // Fixed Compose testing dependencies with explicit versions
                implementation(libs.ui.test.junit4)
                implementation("androidx.compose.ui:ui-test-manifest:1.8.3")
                implementation(libs.activity.compose)
                implementation(libs.androidx.ui.tooling)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.slf4j:slf4j-simple:2.0.9")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.contextable.agui4k.example.client"
    compileSdk = 36

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
        
        managedDevices {
            allDevices {
                // Create a pixel 8 API 34 device for testing
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel8Api34") {
                    device = "Pixel 8"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
                
                // Create a pixel 6 API 33 device for broader compatibility testing
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api33") {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "aosp"
                }
                
                // Create a tablet device for testing different form factors
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel7TabletApi34") {
                    device = "Pixel Tablet"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
            }
            
            groups {
                create("phone") {
                    targetDevices.add(allDevices["pixel8Api34"])
                    targetDevices.add(allDevices["pixel6Api33"])
                }
                
                create("all") {
                    targetDevices.add(allDevices["pixel8Api34"])
                    targetDevices.add(allDevices["pixel6Api33"])
                    targetDevices.add(allDevices["pixel7TabletApi34"])
                }
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "agui4kclient.shared.generated.resources"
    generateResClass = auto
}