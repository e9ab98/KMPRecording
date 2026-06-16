# Contributing to KMPRecording / 贡献指南

## English

Thank you for your interest in contributing to KMPRecording! We welcome contributions to improve performance, fix bugs, localise further, or add new KMP camera features.

### How to Contribute

1. **Fork the Repository**: Create a personal fork of the project on GitHub.
2. **Clone the Project**: Clone your fork locally.
3. **Create a Feature Branch**: Use a descriptive branch name (e.g., `feature/dynamic-filters` or `bugfix/ios-aspect-ratio`).
4. **Make Your Changes**: Write clean, self-documenting Kotlin and follow KMP modularisation principles.
5. **Bilingual Support**: If you add any user-facing strings, make sure to add them to `L10n.kt` under both `ZhL10n` and `EnL10n` classes.
6. **Verify Builds**:
   - Verify Android build: `./gradlew :androidApp:assembleDebug`
   - Verify iOS build: `./gradlew :shared:compileKotlinIosSimulatorArm64`
7. **Submit a Pull Request**: Provide a detailed description of your changes and why they are necessary.

---

## 中文

感谢您对 KMPRecording 项目的关注与贡献！我们非常欢迎您提交代码来提升性能、修复 Bug、丰富本地化文案或增加更多 KMP 相机功能。

### 如何参与贡献

1. **Fork 仓库**：在 GitHub 上将本项目 fork 到您个人的账户。
2. **克隆项目**：将您的 fork 仓库克隆到本地。
3. **新建分支**：使用描述性好的分支名称（例如：`feature/dynamic-filters` 或 `bugfix/ios-aspect-ratio`）。
4. **编写代码**：保持代码干净整洁，遵循 KMP 跨平台模块化开发规范。
5. **双语支持**：如果新增了任何用户可见的文案，请务必在 `L10n.kt` 中的 `ZhL10n` 和 `EnL10n` 类里同步添加翻译。
6. **运行编译校验**：
   - 验证 Android 端编译：`./gradlew :androidApp:assembleDebug`
   - 验证 iOS 端编译：`./gradlew :shared:compileKotlinIosSimulatorArm64`
7. **提交 Pull Request**：详细描述您的修改点、设计考量以及所解决的具体问题。
