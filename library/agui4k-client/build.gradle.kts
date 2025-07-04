plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

group = "com.contextable.agui4k"
version = "0.2.1"

repositories {
    google()
    mavenCentral()
}

kotlin {
    // Configure K2 compiler options
    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
                    freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
                    freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
                    languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
                    apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
                }
            }
        }
    }
    
    // Android target
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
        publishLibraryVariants("release")
    }

    // JVM target
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core dependencies
                api(project(":agui4k-core"))
                
                // Optional tools integration
                api(project(":agui4k-tools"))
                
                // Kotlinx libraries
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Json Patching
                implementation(libs.kotlin.json.patch)
                
                // HTTP client dependencies - core only (no engine)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                
                // Logging
                implementation(libs.kotlin.logging)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Android-specific HTTP client engine
                implementation(libs.ktor.client.android)
            }
        }
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                // iOS-specific HTTP client engine
                implementation(libs.ktor.client.darwin)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                // JVM-specific HTTP client engine
                implementation(libs.ktor.client.cio)
            }
        }
    }
}

android {
    namespace = "com.contextable.agui4k.client"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    
    testOptions {
        targetSdk = 36
    }
    
    buildToolsVersion = "36.0.0"
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Publishing configuration
publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("agui4k-client")
                description.set("Client SDK for the Agent User Interaction Protocol")
                url.set("https://github.com/contextable/ag-ui-4k")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("contextable")
                        name.set("Contextable Team")
                        email.set("dev@contextable.com")
                    }
                }

                scm {
                    url.set("https://github.com/contextable/ag-ui-4k")
                    connection.set("scm:git:git://github.com/contextable/ag-ui-4k.git")
                    developerConnection.set("scm:git:ssh://github.com:contextable/ag-ui-4k.git")
                }
            }
        }
    }
}

// Signing configuration
signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}