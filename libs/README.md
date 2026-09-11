# KmpPinYin (libs)

[![Release](https://jitpack.io/v/jikun2008/KmpPinYin.svg)](https://jitpack.io/#jikun2008/KmpPinYin)

> 本目录代码由独立工程 `D:\workcode\KmpPinYin` 迁入，作为 Gradle 子项目 `:libs:tinypinyin-kt`、
> `:libs:lexicons-cncity`、`:libs:generator` 参与构建；外层工程（androidApp / desktopApp /
> webApp / iosApp / shared）只用于多端功能验证。

Fast, low-memory Chinese-to-Pinyin library for **Kotlin Multiplatform**, inspired by
[TinyPinyin](https://github.com/jikun2008/TinyPinyin).

The entire implementation lives in `commonMain` (pure Kotlin, no `expect/actual`, zero
platform I/O). The same code and the same embedded data are shared by every target
(JVM / Android / iOS / macOS / Linux / Windows / JS / Wasm).

## Features

- Single-character and whole-string conversion, uppercase, toneless output by default
  (drop-in parity with TinyPinyin's `Pinyin.toPinyin`).
- **Tone output**: `TONE_NUMBER` (`ZHONG1`) and `TONE_MARK` (`zhōng`).
- **Traditional Chinese & rare chars**: covers CJK Unified Ideographs (`U+4E00..U+9FFF`)
  plus Extension A (`U+3400..U+4DBF`), ~26.7k characters.
- **Polyphone / custom dictionaries** via longest-match, to fix readings such as
  重庆. A dictionary value may end with a tone digit (`"CHONG2"`), so matched words follow
  the configured `ToneStyle`: `CHONG QING` / `chong2 qing4` / `chóng qìng`. Values without
  a digit stay literal and come out toneless (TinyPinyin-compatible).
- Compact generated data: each character is a 12-bit code packed as two printable ASCII
  characters, decoded lazily into `ShortArray`s (~55 KB resident).

## Modules

| Module                     | Description                                        |
|----------------------------|----------------------------------------------------|
| `tinypinyin-kt`            | Core KMP library (public API + generated table).   |
| `lexicons-cncity`          | Optional Chinese place-name dictionary.            |
| `generator`                | Offline JVM tool that regenerates the Kotlin constants. |

## Usage

```kotlin
import com.github.kmppy.Pinyin
import com.github.kmppy.PinyinCase
import com.github.kmppy.PinyinConfig
import com.github.kmppy.ToneStyle
import com.github.kmppy.dict.PinyinMapDict

// Defaults: no tone, uppercase.
Pinyin.toPinyin('中')            // "ZHONG"
Pinyin.isChinese('中')           // true
Pinyin.toPinyin("中文")          // "ZHONG WEN"
Pinyin.toPinyin("中文", "")      // "ZHONGWEN"

// Tone, via an explicit config (no global mutation):
val tone = PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)
Pinyin.toPinyin("中国", " ", tone)  // "zhōng guó"

// Global config + custom dictionary for polyphones (append a tone digit to keep tones):
Pinyin.config {
    toneStyle(ToneStyle.TONE_NUMBER)
    case(PinyinCase.LOWERCASE)
    with(PinyinMapDict(mapOf("重庆" to arrayOf("chong2", "qing4"))))
}
Pinyin.toPinyin("我去重庆")  // "wo3 qu4 chong2 qing4"
// Same dictionary with ToneStyle.TONE_MARK gives "wǒ qù chóng qìng"; with ToneStyle.NONE
// the digit is consumed instead of printed: "WO QU CHONG QING".
```

## Add to your project

两个库通过 **JitPack** 分发（按需从 GitHub tag 构建，无需本地上传）。

1) 添加仓库：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

2) 添加依赖。KMP 工程在 `commonMain` 写根坐标即可，Gradle 会按 target 自动挑平台产物：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.jikun2008.KmpPinYin:tinypinyin-kt:0.1.0")
            // 可选：中文地名词典（多音字修正）
            implementation("com.github.jikun2008.KmpPinYin:lexicons-cncity:0.1.0")
        }
    }
}
```

纯 JVM / Android 工程也可以直接引用带后缀的模块：
`com.github.jikun2008.KmpPinYin:tinypinyin-kt-jvm:0.1.0`（JAR）、
`com.github.jikun2008.KmpPinYin:tinypinyin-kt-android:0.1.0`（AAR）。

已发布 target：JVM、Android（AAR）、iOS（`iosArm64` / `iosX64` / `iosSimulatorArm64` klib）、
macOS（`macosArm64` / `macosX64` klib）、JS、WasmJs。全部逻辑都在 `commonMain`，各 target 产物等价。

> JitPack 只在 Linux 构建机上执行构建，Apple target 的 klib 靠 Kotlin/Native 交叉编译产出
> （本库无 cinterop、无 framework 二进制，满足交叉编译条件），消费者拿到的仍然是 klib，
> 真正的链接仍在使用方的 macOS/Xcode 侧完成。

本仓库内部的 `shared` 模块保持源码依赖，不走 JitPack：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":libs:tinypinyin-kt"))
            // optional: api(project(":libs:lexicons-cncity"))
        }
    }
}
```

## Publishing（发新版本到 JitPack）

坐标与版本集中在 `gradle.properties`：`GROUP_ID=com.github.jikun2008.KmpPinYin`、
`VERSION_NAME=<版本号>`。`group` 不能随意改：JitPack 会按 `com.github.<用户>.<仓库>` 反推源码仓库；
`VERSION_NAME` 必须与 GitHub **tag 名完全一致**，否则消费者按 tag 取版本时会 404。

1. 改 `gradle.properties` 的 `VERSION_NAME`（如 `0.1.0` → `0.2.0`）并提交。
2. 打上同名 tag（不带 `v` 前缀）并推送：

   ```bash
   git tag 0.2.0
   git push origin main --tags
   ```

3. 打开 <https://jitpack.io/#jikun2008/KmpPinYin> → `Look up` → 对应版本点 `Get it`；
   首次请求触发远端构建，绿色 log 表示成功，红色 log 点进去看 `build.log` 排错。
4. 远端构建命令由根目录 [jitpack.yml](../jitpack.yml) 指定：只跑
   `:libs:tinypinyin-kt:publishToMavenLocal :libs:lexicons-cncity:publishToMavenLocal`，
   并用 `-PskipExampleApps` 把 `shared` 与示例 App 排除在 settings 之外（见
   [settings.gradle.kts](../settings.gradle.kts)）。

本地预检（不依赖网络，产物落在 `~/.m2`，注意 `settings.xml` 可能把本地仓库改到别处）：

```bash
./gradlew :libs:tinypinyin-kt:publishToMavenLocal -PskipExampleApps
```

JitPack 上发布满 7 天的版本不可覆盖（只读），需要修正请递增版本号重新发布。

## Building

在外层工程根目录（`D:\code\KmpPinYin`）执行，Kotlin / AGP / Gradle 版本跟随外层工程
（当前 Kotlin `2.4.10` + Gradle `9.1.0`）。

```bash
gradle build                              # compile all targets that run on the current host
gradle :libs:tinypinyin-kt:jvmTest
gradle :libs:tinypinyin-kt:allTests
```

## Data pipeline

The generated source `tinypinyin-kt/.../internal/PinyinTable.kt` is committed, so
building the library needs **no** external data or code generation.

To regenerate from scratch:

1. `generator/src/main/resources/pinyin-dict.tsv` is the raw source of truth, one row
   per character: `codepoint<TAB>tone-less-syllable<TAB>tone(1..5)`. It was bootstrapped
   from the `pypinyin` dataset with `generator/tools/bootstrap_raw_data.py`
   (a provenance helper; not needed afterward).
2. Run the generator to rewrite `PinyinTable.kt`（不带参数时默认输出到
   `libs/tinypinyin-kt/src/commonMain/kotlin`）:

   ```bash
   gradle :libs:generator:run
   # 或显式指定目录：
   gradle :libs:generator:run --args="<abs-path>/libs/tinypinyin-kt/src/commonMain/kotlin"
   ```

Because the initial sandbox had no `kotlinc`/Gradle, an equivalent pure-Python mirror
(`generator/tools/gen_table.py`) implements the exact same encoding and was used to emit
the committed artifact. It stays byte-compatible with `PinyinTableGenerator.kt`; keep the
two in sync if the format ever changes.

## Design notes vs TinyPinyin

- TinyPinyin stores a 9-bit pinyin index per char; here each char stores a 12-bit
  `(syllableIndex, tone)` code so tone output is possible without a second table.
- Tone diacritics are computed at runtime from `(syllable, tone)` following standard
  vowel-placement rules, rather than pre-storing every accented syllable.
- Dictionary handling follows the same "user dictionary wins, longest match, per-char
  separator" behavior. The one extension: a trailing `1..5` in a dictionary value is read
  as the tone (see `Pinyin.formatDictValue`) instead of being emitted verbatim, so lexicon
  hits never lose the tone the rest of the sentence keeps.
