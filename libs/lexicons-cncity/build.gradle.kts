import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    id("maven-publish")
}

description = "Optional Chinese place-name lexicon for tinypinyin-kt (longest-match polyphone fixes)."
apply(from = rootProject.file("gradle/jitpack-publishing.gradle.kts"))

kotlin {
    android {
        namespace = "com.github.kmppy.lexicons.cncity"
        // 与 :libs:tinypinyin-kt 保持一致，固定 36，不依赖 SDK Platform 37。
        compileSdk = 36
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

    sourceSets {
        commonMain {
            dependencies {
                api(project(":libs:tinypinyin-kt"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
