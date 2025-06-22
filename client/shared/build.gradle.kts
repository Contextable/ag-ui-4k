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
                implementation("com.contextable.agui4k:agui4k-core:0.1.6")
                implementation("com.contextable.agui4k:agui4k-transport:0.1.6")
                implementation("com.contextable.agui4k:agui4k-client:0.1.6")
                implementation("com.contextable.agui4k:agui4k-tools:0.1.6")
                implementation("com.contextable.agui4k:agui4k-tools-builtin:0.1.6")

                // Navigation
                implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
                implementation("cafe.adriel.voyager:voyager-screenmodel:1.0.0")
                implementation("cafe.adriel.voyager:voyager-transitions:1.0.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

                // Preferences
                implementation("com.russhwolf:multiplatform-settings:1.2.0")
                implementation("com.russhwolf:multiplatform-settings-coroutines:1.2.0")

                // Logging
                implementation("io.github.microutils:kotlin-logging:3.0.5")

                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                implementation("io.ktor:ktor-client-mock:3.1.3")  // Add this line
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.10.1")
                api("androidx.appcompat:appcompat:1.7.1")
                api("androidx.core:core-ktx:1.16.0")
                implementation("com.github.tony19:logback-android:3.0.0")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("junit:junit:4.13.2")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("androidx.test:runner:1.6.2")
                implementation("androidx.test.ext:junit:1.2.1")
                implementation("androidx.test:core:1.6.1")

                // Fixed Compose testing dependencies with explicit versions
                implementation("androidx.compose.ui:ui-test-junit4:1.8.3")
                implementation("androidx.compose.ui:ui-test-manifest:1.8.3")
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("androidx.compose.ui:ui-tooling:1.8.3")
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
    namespace = "com.contextable.agui4k.sample.client"
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