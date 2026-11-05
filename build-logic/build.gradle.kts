plugins {
    `kotlin-dsl`
    `maven-publish`
}

// 与根工程同一版本号；JitPack 以 tag 为版本，二者保持一致。
group = "com.github.hdppppppp.hot-update"
version = rootProject.file("../VERSION").readText().trim()

afterEvaluate {
    publishing {
        publications {
            // kotlin-dsl 自带 java-gradle-plugin，会在评估后生成 pluginMaven 发布项。
            named<MavenPublication>("pluginMaven") {
                artifactId = "hotfix-gradle-plugin"
                pom {
                    name.set("hotfix-gradle-plugin")
                    description.set("com.taotao.hotfix 编译期插桩插件")
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

dependencies {
    // AGP 的 Instrumentation API（AsmClassVisitorFactory）在这个构件里。
    // 版本要与根项目一致，见根 build.gradle.kts。
    compileOnly("com.android.tools.build:gradle:9.4.1")
    // ASM 用来改字节码。AGP 8.7 内部用的是 ASM 9.x，这里对齐大版本避免 ClassReader 版本报错。
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
}

gradlePlugin {
    plugins {
        create("hotfixInstrumentation") {
            id = "com.taotao.hotfix"
            implementationClass = "com.taotao.hotfix.HotfixInstrumentationPlugin"
        }
    }
}
