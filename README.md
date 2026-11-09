# 账

账是一款离线 Android 支出账本。支持按月浏览明细、消费日历、分类预算、支出统计、批量管理和 JSON 备份恢复。

当前源码版本为 1.8.11。设置等次级页面支持从左边缘向右拖动返回。

- Android 8.0 及以上，Kotlin、Jetpack Compose、Room
- 使用 Android Studio 打开本项目，安装 Android SDK 36，选择 JDK 17
- Windows 构建调试版运行 `gradlew.bat :app:assembleDebug`。macOS / Linux 运行 `./gradlew :app:assembleDebug`
- 自行构建的 APK 使用本地签名。要覆盖已有安装，需要与原安装相同的签名

本项目在 [SpendWise](https://github.com/wansiong50-pixel/spendwise) 基础上修改，按 [MIT](LICENSE) 许可发布。第三方组件和字体保留各自的许可，见 [第三方声明](THIRD_PARTY_NOTICES.md)。应用中可从「设置 → 关于 → 开源致谢」阅读。

代码仓库仅包含项目源码、构建配置、资源与许可文件。账本数据在设备本地保存。
