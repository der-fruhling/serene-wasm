@file:OptIn(ExperimentalWasmDsl::class, ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "net.derfruhling.serenity"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    jvm()

    android {
        namespace = "net.derfruhling.serene.wasm"
        minSdk = 23
        compileSdk = 36

        withHostTest {}
        withDeviceTest {}
    }

    js {
        browser {
            testTask {
                useKarma {
                    useChromiumHeadless()
                    useFirefoxHeadless()
                }
            }
        }

        nodejs()
    }

    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromiumHeadless()
                    useFirefoxHeadless()
                }
            }
        }

        nodejs()
        d8()
    }

    wasmWasi {
        nodejs()

        binaries.executable()
    }

    fun KotlinNativeTarget.configureFramework() {
        binaries.framework()
    }

    @Suppress("DEPRECATION")
    val executableTargets = listOf(
        mingwX64(),
        macosX64 { configureFramework() },
        macosArm64 { configureFramework() },
        linuxArm64(),
        linuxX64()
    )

    for (tgt in executableTargets) {
        tgt.binaries.executable {
            entryPoint = "net.derfruhling.serene.wasm.main"
        }
    }

    iosArm64 { configureFramework() }
    iosSimulatorArm64 { configureFramework() }
    iosX64 { configureFramework() }
    watchosArm32 { configureFramework() }
    watchosArm64 { configureFramework() }
    watchosDeviceArm64 { configureFramework() }
    watchosSimulatorArm64 { configureFramework() }
    @Suppress("DEPRECATION")
    watchosX64 { configureFramework() }
    tvosArm64 { configureFramework() }
    tvosSimulatorArm64 { configureFramework() }
    @Suppress("DEPRECATION")
    tvosX64 { configureFramework() }

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()

    targets.withType(KotlinNativeTarget::class) {
        binaries.sharedLib()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyHierarchyTemplate {
        common {
            group("executable") {
                group("desktop") {
                    group("linux") {
                        withLinuxX64()
                        withLinuxArm64()
                    }

                    group("macos") {
                        withMacosX64()
                        withMacosArm64()
                    }

                    group("mingw") {
                        withMingwX64()
                    }
                }

                withJvm()
                withWasmWasi()
            }

            group("native") {
                group("desktop")

                group("mobile") {
                    group("ios") {
                        withIosArm64()
                        withIosSimulatorArm64()
                        withIosX64()
                    }

                    group("androidNative") {
                        withAndroidNativeArm32()
                        withAndroidNativeArm64()
                        withAndroidNativeX64()
                    }
                }

                group("apple") {
                    group("macos")
                    group("ios")

                    group("watchos") {
                        withWatchosArm32()
                        withWatchosArm64()
                        withWatchosDeviceArm64()
                        withWatchosSimulatorArm64()
                        withWatchosX64()
                    }

                    group("tvos") {
                        withTvosArm64()
                        withTvosSimulatorArm64()
                        withTvosX64()
                    }
                }
            }

            group("web") {
                withJs()
                withWasmJs()
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                api(libs.kotlinx.io)
                api(libs.kotlinx.serialization.core)
                implementation(libs.oshai.kotlinLogging)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        named("executableMain") {
            dependencies {
                implementation(libs.clikt)
            }
        }

        jvmTest {
            dependencies {
                compileOnly(libs.junit.api)
                runtimeOnly(libs.junit.engine)
                runtimeOnly(libs.logback.classic)
            }
        }
    }

    abiValidation {
        enabled = true
        klib {
            enabled = true
            keepUnsupportedTargets = true
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    enableAssertions = true
}

val testTasks = mutableListOf(
    tasks.named("jvmTest"),
    tasks.named("jsBrowserTest"),
    tasks.named("jsNodeTest"),
    tasks.named("wasmJsBrowserTest"),
    tasks.named("wasmJsNodeTest"),
    tasks.named("wasmJsD8Test"),
    tasks.named("wasmWasiNodeTest"),
)

val os = System.getProperty("os.name").lowercase()
val arch = System.getProperty("os.arch").lowercase()

when {
    "linux" in os -> when {
        "x86_64" in arch -> testTasks += tasks.named("linuxX64Test")
        "aarch64" in arch -> testTasks += tasks.named("linuxArm64Test")
    }

    "macos" in os -> {
        when {
            "x86_64" in arch -> testTasks += listOf(
                tasks.named("macosX64Test"),
                tasks.named("iosX64Test")
            )

            "aarch64" in arch -> testTasks += listOf(
                tasks.named("macosX64Test"),
                tasks.named("macosArm64Test"),
                tasks.named("iosSimulatorArm64Test")
            )
        }
    }

    "mingw" in os -> testTasks += tasks.named("mingwX64Test")
}

@DisableCachingByDefault
abstract class AggregateTestTask : DefaultTask(), VerificationTask

tasks.register<AggregateTestTask>("test") {
    description = "Runs tests available on your current platform"
    group = "verification"

    dependsOn(testTasks)
}
