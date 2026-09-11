plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

group = "com.github.kmppy"
version = "0.1.0"

application {
    mainClass.set("com.github.kmppy.tools.PinyinTableGeneratorKt")
}

// Run manually to regenerate the committed Kotlin constants:
//   ./gradlew :libs:generator:run --args="<abs-path-to-commonMain-dir>"
