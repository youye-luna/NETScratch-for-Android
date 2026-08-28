// 顶层构建文件：声明全局插件版本（由 gradle/libs.versions.toml 统一管理）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
