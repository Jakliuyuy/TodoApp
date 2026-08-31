# TodoApp · 极简待办清单

一个用 **Kotlin + Jetpack Compose** 写的 Android 效率工具，Material 3 设计，数据本地持久化。
本仓库已内置 GitHub Actions 流水线，**推送到 GitHub 即可自动编译出可安装的 APK**。

## 功能

- 添加 / 完成 / 删除任务，点击任务卡片可编辑文字
- 星标重要任务（重要项会高亮并自动排在前面）
- 筛选：全部 / 进行中 / 已完成
- 顶部进度条显示「已完成 X / Y」
- 一键清除已完成
- 数据存于 Jetpack DataStore，杀进程、重启手机都不丢
- 跟随系统深色模式

## 最快出 APK 的方式（不需要本地装 Android Studio）

1. 在 GitHub 新建一个空仓库（比如叫 `TodoApp`），**不要**勾选初始化 README
2. 在本目录执行：

```bash
git init
git add .
git commit -m "init: TodoApp"
git branch -M main
git remote add origin https://github.com/<你的用户名>/TodoApp.git
git push -u origin main
```

3. 打开仓库的 **Actions** 页，等 3～8 分钟（首次编译较慢，之后有缓存）
4. 运行结束后，点进绿色的那次任务，在 **Artifacts** 里下载 `TodoApp-APK`
5. 解压得到 `TodoApp-debug.apk`（手机可直接安装）和 `TodoApp-release.apk`（已签名）

> debug 包的 applicationId 带 `.debug` 后缀，可以和 release 包同时装在一台手机上。

想正式发版时打个 tag 即可自动创建 Release：

```bash
git tag v1.0
git push origin v1.0
```

## 本地构建（可选）

需要 **JDK 17** 和 Android SDK（命令行工具即可，不必装 Android Studio）：

```bash
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

本机已有 JDK 17 时，也可以直接跑封装好的脚本：

```bash
./build.sh debug
```

> 说明：仓库不含 `gradle-wrapper.jar`（二进制文件不便由文本生成），流水线会自动生成。
> 本地首次使用时执行 `gradle wrapper --gradle-version 8.7 --distribution-type bin` 即可补齐。

## 技术栈

| 项 | 版本 |
| --- | --- |
| Kotlin | 1.9.24 |
| AGP | 8.5.0 |
| Gradle | 8.7 |
| Compose BOM | 2024.02.00 |
| minSdk / targetSdk | 26 / 34 |

## 目录结构

```
app/src/main/java/com/yuanbao/todo/
├── MainActivity.kt           # 入口
├── model/Task.kt             # 数据模型 + 筛选枚举
├── data/TaskRepository.kt    # DataStore 持久化
├── viewmodel/TodoViewModel.kt# 业务逻辑与状态
└── ui/
    ├── TodoScreen.kt         # 全部界面（列表、输入、筛选、编辑弹窗）
    └── theme/                # 主题、配色、字体
```

## 二次开发小贴士

- 改应用名：`app/src/main/res/values/strings.xml` 里的 `app_name`
- 改包名：`app/build.gradle.kts` 的 `applicationId` + 代码中 `com.yuanbao.todo` 目录
- 换图标：改 `tools/make_icon.py` 后执行 `python3 tools/make_icon.py`
- 改完代码想快速自查（字符串/资源引用/括号配对）：`python3 tools/check_project.py`
- 加字段：改 `Task` 数据类即可，DataStore 反序列化用 `ignoreUnknownKeys`，老数据不会崩
