plugins {
    // 版本与上游桃桃音乐主仓保持一致：compileSdk 37 / Kotlin 2.4 的配套链路。
    id("com.android.library") version "9.4.1" apply false
    kotlin("android") version "2.4.21" apply false
    // 热修复补丁模块是纯 JVM 模块：它只产出一个 dex，不需要资源、清单和签名。
    kotlin("jvm") version "2.4.21" apply false
}
