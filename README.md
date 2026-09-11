This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).

### Quick Start — Add the library

[![Release](https://jitpack.io/v/jikun2008/KmpPinYin.svg)](https://jitpack.io/#jikun2008/KmpPinYin)

`tinypinyin-kt` 和 `lexicons-cncity` 已通过 JitPack 发布，外部 KMP / JVM / Android 工程可直接依赖：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
// build.gradle.kts（KMP 工程）
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.jikun2008.KmpPinYin:tinypinyin-kt:0.1.0")
            // 可选：中文地名词典（多音字修正）
            implementation("com.github.jikun2008.KmpPinYin:lexicons-cncity:0.1.0")
        }
    }
}

// 或者在纯 JVM / Android 项目中直接引用平台产物：
// implementation("com.github.jikun2008.KmpPinYin:tinypinyin-kt-jvm:0.1.0")   // JAR
// implementation("com.github.jikun2008.KmpPinYin:tinypinyin-kt-android:0.1.0") // AAR
```

### Code example

```kotlin
import com.github.kmppy.Pinyin
import com.github.kmppy.PinyinCase
import com.github.kmppy.PinyinConfig
import com.github.kmppy.ToneStyle
import com.github.kmppy.dict.PinyinMapDict

// 默认：无调，大写
Pinyin.toPinyin('中')           // "ZHONG"
Pinyin.isChinese('中')          // true
Pinyin.toPinyin("中文")          // "ZHONG WEN"
Pinyin.toPinyin("中文", "")     // "ZHONGWEN"

// 显式配置声调（无全局状态）
val tone = PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)
Pinyin.toPinyin("中国", " ", tone)  // "zhōng guó"

// 多音字词典（末尾数字表示声调，激活地名词典后仍带声调输出）
Pinyin.config {
    toneStyle(ToneStyle.TONE_NUMBER)
    case(PinyinCase.LOWERCASE)
    with(PinyinMapDict(mapOf("重庆" to arrayOf("chong2", "qing4"))))
}
Pinyin.toPinyin("我去重庆")  // "wo3 qu4 chong2 qing4"
```

完整文档（发布流程、数据管线、设计说明）见 [libs/README.md](./libs/README.md)。

---

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- Web app:
  - Wasm target (faster, modern browsers): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
  - JS target (slower, supports older browsers): `./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in the IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- 拼音库测试:
  - `./gradlew :libs:tinypinyin-kt:jvmTest`
  - `./gradlew :libs:lexicons-cncity:jvmTest`
- Desktop tests: `./gradlew :shared:jvmTest`
- Web tests:
  - Wasm target: `./gradlew :shared:wasmJsTest`
  - JS target: `./gradlew :shared:jsTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform-get-started.html),
[Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/),
and [Kotlin/Wasm](https://kotl.in/wasm/).

For details on the `tinypinyin-kt` / `lexicons-cncity` libraries, the data pipeline,
and the release process, see [libs/README.md](./libs/README.md).