import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

group = "com.github.kmppy"
version = "0.1.0"

kotlin {
    // All implementation lives in commonMain (pure Kotlin, no expect/actual).
    // These targets only publish the resolved commonMain artifacts.
    android {
        namespace = "com.github.kmppy.tinypinyin"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm()

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    macosArm64()
    macosX64()

    // 本机为 Windows，开启这两个目标会额外下载 Kotlin/Native 工具链；
    // 需要产出 Linux / Windows 原生库时再放开。
    // linuxX64()
    // mingwX64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
