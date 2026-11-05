# hot-update

Android 方法级热修复 + 整包更新 / 灰度发布 / 远程配置的完整可复用实现。

本仓库从 [taotaomusic/core](https://github.com/taotaomusic/core) 的热更新模块同步而来，
包含客户端运行时、补丁生成器、Node 服务端 SDK 与自建服务参考实现。

## 目录

| 目录 | 内容 |
| --- | --- |
| `hotupdate/` | Kotlin/Android library：整包更新、远程配置、DEX 热修运行时，宿主经 `UpdateHost` 契约接入 |
| `hotupdate-node/` | 零运行时依赖的 Node SDK：bootstrap 客户端、断点续传下载、灰度分桶、TOTP、发布管理接口，以及 `createHotUpdateServer` 自建服务 |
| `autopatch/` | 补丁自动生成器（独立 JVM 工程）：对比 base→head 构建产物，判定可热修方法并生成补丁 DEX |
| `build-logic/` | Gradle 插桩插件 `com.taotao.hotfix`：编译期在可热修方法入口插入补丁分发判断 |
| `patch/` | 补丁模块示例：把补丁代码编译并打包成可下发的 `patch-<n>.apk` |
| `HOT_UPDATE.md` | 设计文档：为什么选方法级分发、灰度/强制更新规则、安全边界 |

## 快速开始

### Android 客户端

```kotlin
// settings.gradle.kts
includeBuild("build-logic")   // 仅当需要给本模块插桩时
include(":hotupdate")
```

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ① 尽早加载已安装补丁，返回本次生效的补丁版本
        val active = HotUpdate.installPatches(this)
        // ② 创建管理器并检查更新
        hotUpdate = HotUpdate.create(this, MyUpdateHost(this), initialPatchVersion = active)
        hotUpdate.launchCheck()
    }
}
```

完整接入步骤、`UpdateHost` 契约与非 Compose 客户端的 `StateFlow` 用法见
[`hotupdate/README.md`](hotupdate/README.md)。

### Node 侧

```powershell
cd hotupdate-node
npm install
npm run verify   # tsc 构建 + 全部单测
npm start -- --port 4730 --token <管理令牌> --data ./data
```

客户端/SDK/服务端的调用方式见 [`hotupdate-node/README.md`](hotupdate-node/README.md)。

### 构建本仓库

```powershell
# Android library
.\gradlew.bat :hotupdate:assembleRelease

# 示例补丁（先确保 :hotupdate 已编译）
.\gradlew.bat :patch:buildPatch -PpatchVersion=1

# 补丁生成器（独立工程）
.\gradlew.bat -p autopatch test
```

`patch` 通过 `-PhostClasses="dir1;dir2"` 指向宿主 `build/tmp/kotlin-classes/release`；
Android SDK 从 `local.properties` 的 `sdk.dir` 或 `ANDROID_HOME` 读取。

## 不可破坏的约定

1. **包名固定**：`com.taotao.music.update` / `com.taotao.music.hotfix` 是插桩白名单、
   R8 keep 与补丁方法键的共同前缀，改名会让整套体系失配。
2. **补丁与宿主版本强绑定**：补丁只对生成它的那份 `versionCode` 生效。
3. **补丁只能改已有方法**：不能新增类/字段，Compose UI 不可热修，这类改动必须发整包。
4. **自愈优先**：补丁加载失败或加载后进程未能平稳运行，下次启动自动回滚。

## 许可

GPL-3.0，见 [LICENSE](LICENSE)。
