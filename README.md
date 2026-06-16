# KMPRecording - Kotlin Multiplatform Landscape Recorder

[![Kotlin](https://img.shields.io/badge/kotlin-2.0.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.6.11-purple.svg)](https://github.com/JetBrains/compose-multiplatform)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-orange.svg)](#)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

[中文版](#chinese-中文) | [English Version](#english-英文)

---

## Chinese (中文)

KMPRecording 是一个基于 **Kotlin Multiplatform (KMP)** 和 **Compose Multiplatform** 构建的高性能横屏视频录制跨平台应用。本项目针对车载记录仪（Dashcam）、运动相机（Action Camera）等典型横屏高频录制场景进行深度优化，提供流畅、稳定且强反馈的视频采集、分段管理和本地化交互。

### 🌟 核心特性

1. **横屏交互与悬浮控制 (Landscape-Optimized UI/UX)**
   * 100% 沉浸式全屏预览，消除上下黑边。
   * 右侧半透明悬浮控制台（`RightControlDeck`），完美贴合双手横持大拇指操作范围。
   * 正在录像状态加入平滑的呼吸渐变红色 "REC" 指示器。

2. **零断档无缝循环录制 (Seamless Loop Recording)**
   * 独特的后台异步 IO 设计，将“分段保存”与“历史覆盖清理”移出主线程。
   * 分段切换时，最大程度压榨断档漏秒，确保录像文件在物理时空上的绝对连续。

3. **高精时钟与 Rollover (Accurate Timers)**
   * 通过跨平台 `expect`/`actual` 精准物理时钟，免受协程挂起、CPU 调度和前后台切换的飘移干扰。
   * 杜绝多次重复触发分段生成的竞态 Bug。

4. **Android 视频库毫秒级元数据高速缓存 (Metadata Cache)**
   * 针对 Android 频繁读取 `MediaMetadataRetriever` 导致的卡顿与 ANR，引入了基于文件修改时间与大小哈希校验的缓存机制。
   * 视频列表加载性能提升数千倍，50+ 循环视频瞬间秒开。

5. **无需重启的即时中英文切换 (Runtime ZH/EN Language Switch)**
   * 采用纯 Kotlin 多语言抽象字典机制 (`L10n`) 驱动 Compose UI，摆脱双端原生资源 Locale Override 重启限制。
   * 毫秒级应用内即时翻译刷新，并使用 DataStore (Android) 和 NSUserDefaults (iOS) 完成偏好持久化。

6. **双端原生相机硬件捕获 (Dual-Platform Native Engine)**
   * **Android**：基于 Jetpack CameraX 进行配置，确保了广泛的机型兼容性。
   * **iOS**：基于 AVFoundation 进行了深度镜头封装，解决了输入输入流与切换时镜头状态锁死和硬件自消互斥 Bug。

---

### 📂 目录结构与架构

```
.
├── androidApp/             # Android 原生壳工程入口
├── iosApp/                 # iOS Xcode 壳工程入口 (SwiftUI 配置)
└── shared/                 # KMP 共享核心逻辑目录
    └── src
        ├── commonMain/     # 100% 共享逻辑 (UI 页面、L10n 字典、Settings 接口、Recording 表现层)
        ├── androidMain/    # Android 平台具体实现 (CameraX 录制引擎、MediaStore 存储、DataStore 偏好)
        └── iosMain/        # iOS 平台具体实现 (AVFoundation 录制引擎、NSUserDefaults 偏好、系统沙盒管理)
```

---

### 🛠️ 快速上手

#### 运行环境要求
* **JDK**: 17+
* **Xcode**: 15.0+ (iOS 编译需要)
* **Android SDK**: API 26+

#### 编译指令

* **Android 端调试安装**
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```

* **iOS 共享模块编译校验**
  ```bash
  ./gradlew :shared:compileKotlinIosSimulatorArm64
  ```

---

## English (英文)

KMPRecording is a high-performance, cross-platform video recording application built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. Purpose-built for landscape-only scenarios such as dashcams and action cameras, it delivers smooth preview frame rates, stable segmented file writing, responsive UI states, and bilingual runtime translation updates.

### 🌟 Core Features

1. **Landscape UI/UX Layout**
   * Immersive 100% viewport camera preview.
   * Floating, translucent right-side control deck (`RightControlDeck`), optimized for thumb operation when holding devices horizontally.
   * Smooth pulse-fade red "REC" recording indicator badge.

2. **Seamless Loop Recording**
   * Segments database caching and old file clearing are pushed to background coroutine IO threads.
   * Minimized frame gap between loop video segments to guarantee video continuity.

3. **Accurate Timers & Robust Rollover**
   * Clock implementation using native high-precision timers via `expect`/`actual` declarations.
   * Shielded from drift caused by coroutine suspension, backgrounding, and lockscreen events.

4. **Android Millisecond-level Metadata Cache**
   * Resolves list scrolling lags and ANR bugs caused by expensive `MediaMetadataRetriever` disk queries.
   * Compares file attributes (size and modification timestamps) to cached metadata, boosting load performance up to several thousand-fold.

5. **Instant ZH/EN Localization (No Restart Required)**
   * Powered by a type-safe Kotlin localization dictionary (`L10n`) reacting to setting flows.
   * Saves and restores settings locally using Jetpack DataStore (Android) and NSUserDefaults (iOS).

6. **Dual-Platform Native Hardware Engines**
   * **Android**: Leverages Jetpack CameraX to ensure high reliability across thousands of Android device variants.
   * **iOS**: Directly hooks into AVFoundation APIs, resolving common hardware race conditions and camera flipping toggles.

---

### 📂 Directory Structure

```
.
├── androidApp/             # Android native entry project
├── iosApp/                 # iOS Xcode wrapper (SwiftUI entry point)
└── shared/                 # KMP Shared core module
    └── src
        ├── commonMain/     # Common layouts, ViewModels, UI States, L10n translations
        ├── androidMain/    # Android CameraX, MediaStore, and DataStore integrations
        └── iosMain/        # iOS AVFoundation, Sandboxed Files, and NSUserDefaults integrations
```

---

### 🛠️ Getting Started

#### Prerequisites
* **JDK**: 17+
* **Xcode**: 15.0+ (Mandatory for iOS targets)
* **Android SDK**: API 26+

#### Compilation Commands

* **Compile & Assemble Android Debug APK**
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```

* **Compile iOS Framework for Simulator**
  ```bash
  ./gradlew :shared:compileKotlinIosSimulatorArm64
  ```

---

## 📄 License / 开源协议

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

本项目使用 MIT 开源协议 - 详情请参阅 [LICENSE](LICENSE) 文件。