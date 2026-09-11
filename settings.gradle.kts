rootProject.name = "KmpPinYin"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 拼音库源码，由 D:\workcode\KmpPinYin 迁入，统一隔离在 libs/ 下与本测试工程区分
include(":libs:tinypinyin-kt")
include(":libs:lexicons-cncity")
include(":libs:generator")

// 示例 App 与验证台只用于本地多端自测；JitPack 构建传 -PskipExampleApps 时不纳入，
// 让远端只配置 / 构建 :libs 下的库模块（见根目录 jitpack.yml）。
val skipExampleApps = gradle.startParameter.projectProperties.containsKey("skipExampleApps")
if (!skipExampleApps) {
    include(":androidApp")
    include(":desktopApp")
    include(":shared")
    include(":webApp")
}