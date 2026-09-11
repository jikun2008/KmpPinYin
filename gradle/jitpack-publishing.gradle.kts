// :libs 下需要对外发布的模块共用此脚本（模块自身应用 org.gradle.maven-publish 插件）。
// 用法见 libs/tinypinyin-kt/build.gradle.kts / libs/lexicons-cncity/build.gradle.kts。
//
// 坐标与版本集中在 gradle.properties（GROUP_ID / VERSION_NAME），原因：
// 1) JitPack 用 groupId 反推源码仓库（com.github.<用户>.<仓库> -> github.com/<用户>/<仓库>），
//    自定义成别的 group 会导致消费者拉取时找不到仓库；
// 2) 模块间的 project(...) 依赖会被写成发布坐标，group/version 不统一会让传递依赖解析失败。
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

val repoUrl = "https://github.com/jikun2008/KmpPinYin"

group = providers.gradleProperty("GROUP_ID").get()
version = providers.gradleProperty("VERSION_NAME").get()

extensions.configure<PublishingExtension>("publishing") {
    publications.withType<MavenPublication>().configureEach {
        // KMP 会为每个 target 生成一个 publication（tinypinyin-kt-jvm / -android / -iosarm64 …），
        // 这里统一补齐 POM 元数据；sources jar 由 KMP 默认发布，无需额外配置。
        pom {
            name.set(project.name)
            description.set(project.description ?: "Kotlin Multiplatform library")
            url.set(repoUrl)
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("jikun2008")
                    name.set("jikun")
                }
            }
            scm {
                url.set(repoUrl)
                connection.set("scm:git:git://github.com/jikun2008/KmpPinYin.git")
                developerConnection.set("scm:git:ssh://github.com/jikun2008/KmpPinYin.git")
            }
            issueManagement {
                system.set("GitHub Issues")
                url.set("$repoUrl/issues")
            }
        }
    }
}
