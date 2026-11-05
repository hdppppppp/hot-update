import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// 热更新独立模块：整包更新 + 远程配置（update 包）+ DEX 热修运行时（hotfix 包）。
//
// 抽成独立模块的原因：这一域的变更节奏与播放、界面完全解耦，独立维护后
// androidApp 只需注入 UpdateHost 契约即可接入。两条不能破的约定：
//
// 1. 包名保持 com.taotao.music.update / com.taotao.music.hotfix 不动 ——
//    插桩白名单（build-logic）、R8 keep（consumer-rules）与补丁方法键
//    （「类名#方法名#签名」）都以这两个包前缀为准，改名即全体系失配。
// 2. 模块不依赖 app 的任何类型（TokenProvider、Room、AppHttp 等）：
//    宿主通过 UpdateHost 接口注入能力，依赖方向永远是 app -> 模块。
plugins {
    id("com.android.library")
    kotlin("android")
    // 热修复插桩。update 包的类在这里插桩：AGP 9 的 InstrumentationScope.PROJECT
    // 只覆盖本模块类，宿主 androidApp 的注册探不到库模块的类（实测
    // printMethodKeys 扫不到库内方法键）。白名单沿用插件默认的 data/player/update
    // 三前缀 —— 本模块没有 data/player 类，实际命中的只有 update 包；
    // hotfix 运行时包由工厂的 isInstrumentable 永久排除，不会自插。
    id("com.taotao.hotfix")
    // JitPack / mavenLocal 发布。依赖方加一行坐标即可接入。
    `maven-publish`
}

group = "com.github.hdppppppp.hot-update"
version = rootProject.file("VERSION").readText().trim()

android {
    namespace = "com.taotao.music.hotupdate"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        // 模块自身的 keep 规则随 AAR 下发（-dontobfuscate / -dontoptimize 是全局构建
        // 行为，仍由宿主 androidApp/proguard-rules.pro 统一声明）。
        // AGP 9 新 DSL 里该属性挂在 defaultConfig/productFlavor 上，不在 android 块顶层。
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    publishing {
        // 只发布 release AAR；同时带上源码包，便于依赖方调试。
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // UpdateManager 用 Compose 状态暴露检查结果；BOM 与 androidApp 保持同一版本。
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.runtime:runtime")
    // UpdateInstaller 经 androidx.core 的 FileProvider 唤起系统安装器。
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                artifactId = "hotupdate"
                pom {
                    name.set("hotupdate")
                    description.set("Android 整包更新、远程配置与方法级 DEX 热修运行时")
                    url.set("https://github.com/hdppppppp/hot-update")
                    licenses {
                        license {
                            name.set("GPL-3.0")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                        }
                    }
                }
            }
        }
    }
}
