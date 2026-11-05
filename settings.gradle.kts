pluginManagement {
    // 插桩插件放在 build-logic 里，用 includeBuild 引进来，不发布到任何仓库。
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories { google(); mavenCentral() }
}

rootProject.name = "hot-update"
include(":hotupdate", ":patch")
