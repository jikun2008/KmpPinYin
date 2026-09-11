This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/libs](./libs) 是从独立工程 `D:\workcode\KmpPinYin` 迁入的拼音库源码，与本工程隔离开：
  - [tinypinyin-kt](./libs/tinypinyin-kt/src) 核心库（公共 API + 生成的拼音表），Gradle 模块 `:libs:tinypinyin-kt`。
  - [lexicons-cncity](./libs/lexicons-cncity/src) 地名词典（多音字修正），模块 `:libs:lexicons-cncity`。
  - [generator](./libs/generator) 离线 JVM 生成器，重新生成 `PinyinTable.kt`，模块 `:libs:generator`。
  详见 [libs/README.md](./libs/README.md)。

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  它已依赖上述两个库模块，[App.kt](./shared/src/commonMain/kotlin/com/yisingle/kmppinyin/App.kt)
  是一个拼音转换测试台（输入框 + 声调/大小写/分隔符/词典选项 + 逐字对照）。**打开界面即自动跑一轮校验**，
  不需要手动输入中文，校验数据全部内置在 [PinyinVerifier](./shared/src/commonMain/kotlin/com/yisingle/kmppinyin/PinyinVerifier.kt)：
  - 27 条内置中文样本（常用词/成语/古诗/人名/地名/多音字/ü 韵母/轻声/儿化/繁体/生僻字/Ext-A/中英数混排），
    每条比对无调大写、数字调小写、符号调小写三种配置 = 81 条期望断言，叠加 25 条 API 用例和 8 项结构不变量，共 114 条。
  - 报告**逐条列出被测中文与转换结果**（`中文：…` / `拼音：…`）便于人工复核，失败项额外打印期望值；
    顶部勾上「只看失败」才收敛为只展示失败条目。
  - 勾选「启用地名词典」后命中的词**仍然带声调**，并跟随所选声调风格（`我去重庆` →
    `WO QU CHONG QING` / `wo3 qu4 chong2 qing4` / `wǒ qù chóng qìng`）：词典值尾部写声调数字（如 `CHONG2`），
    没有数字的老式词典值仍按字面量输出，与上游 TinyPinyin 行为一致。
  - 期望值不是手写的：由 [shared/tools/gen_pinyin_samples.py](./shared/tools/gen_pinyin_samples.py) 直接从数据源
    `pinyin-dict.tsv` 独立算出（输出快照见 [samples-out.txt](./shared/tools/samples-out.txt)），与 Kotlin 实现互为对照。
  - 三个按钮：「全量校验 + 长文本计时」、「逐样本校验 + 计时」（每条样本 100 次转换的 ms / µs每字 / 字每秒）、
    「校验并计时当前输入」。
  同一套校验也写成了可回归用例：[PinyinVerifierTest](./shared/src/commonTest/kotlin/com/yisingle/kmppinyin/PinyinVerifierTest.kt)。
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

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

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- 拼音库测试（随迁移代码一起带过来的用例）:
  - `./gradlew :libs:tinypinyin-kt:jvmTest`
  - `./gradlew :libs:lexicons-cncity:jvmTest`
- Desktop tests: `./gradlew :shared:jvmTest`
  （其中 `PinyinVerifierTest` 即“libs 拼音库是否正确 + 转换耗时”的自动化版本，控制台会打印一份完整报告）
- Web tests:
  - Wasm target: `./gradlew :shared:wasmJsTest`
  - JS target: `./gradlew :shared:jsTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com.cn/en-us/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).