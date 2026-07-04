# mkdn

> 一款为长时间阅读 Markdown 文档设计的 Android 应用  
> 参照 One Markdown / Bear / 微信读书的阅读质感

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-green.svg)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/targetSdk-34-blueviolet.svg)](https://developer.android.com)

## ✨ 特性

- 🌓 **三套主题**——浅色 / 护眼米黄 / 深色，一键切换，自动持久化
- 🔤 **七档字号**——13sp ~ 30sp，覆盖小屏到大屏
- 📖 **Markdown 完整渲染**——标题、列表、引用、代码块、表格、删除线、任务列表、链接
- 💻 **代码块语法高亮**——50+ 语言（Prism4j 完整集）
- 🔍 **文件内搜索**——大小写不敏感，↑/↓ 跳转 + 系统级蓝把高亮
- 📑 **目录大纲**——H1-H6 自动解析，**关键词定位精准跳转**（v1.7.6 起 100% 准）
- ✏️ **简单编辑**——快速改个错字，未保存自动确认，崩了 `.bak` 备份恢复
- 📂 **最近文件**——最多 50 个，文件丢失自动清理
- 🈶 **GBK 编码自动识别**——Windows 中文 .md 不乱码
- 🔐 **完全本地**——无网络请求，无数据上传

## 📸 截图

> 暂未提供（v1.8+ 计划添加）

## 🏗️ 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **渲染引擎**：[Markwon 4.6.2](https://github.com/noties/Markwon)（Android 业界标准 Markdown 渲染）
- **代码高亮**：Prism4j 2.0.0
- **导航**：Navigation Compose 2.7.7
- **构建**：Gradle 8.7 + Android Gradle Plugin

## 📦 项目结构

```
mkdn/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/luody/mkdn/
│       │   ├── MainActivity.kt          # 主题持久化层
│       │   ├── data/                    # PrefsStore / RepoStore
│       │   ├── render/                  # MarkdownRenderer / 高亮 plugin
│       │   ├── ui/
│       │   │   ├── AppContext.kt
│       │   │   ├── MkdnApp.kt           # NavHost
│       │   │   ├── theme/               # Theme.kt (LIGHT/SEPIA/DARK)
│       │   │   └── screens/             # Home / Reader / Editor / Highlights
│       │   └── util/FileReader.kt       # 编码自动检测 + 10MB 阈值
│       └── res/                         # 图标 / 主题 / 字符串资源
├── docs/
│   ├── 用户使用指南.md
│   └── sample.md
├── gradle/
│   └── wrapper/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── .gitignore
├── LICENSE
└── README.md
```

## 🚀 构建

### 环境要求
- JDK 21（Android Studio 自带 jbr）
- Android SDK 34+
- Gradle 8.7+（用项目自带的 `gradlew` 即可）

### 构建命令

#### 方式 A：Debug 版（不需要签名）
```bash
# Windows PowerShell
$env:JAVA_HOME = '<path-to-your-jdk-17+>'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
cd <项目根目录>
.\gradlew.bat assembleDebug
```

#### 方式 B：Release 版（需要签名密钥）
签名密钥**不**包含在仓库中（参见 [签名配置](#-签名配置)）。

```powershell
# 通过命令行参数传密码（推荐）
.\build-release.ps1 -StorePassword 'your-password' -KeyPassword 'your-password'

# 或通过环境变量
$env:MKDN_RELEASE_STORE_PASSWORD = 'your-password'
$env:MKDN_RELEASE_KEY_PASSWORD = 'your-password'
.\build-release.ps1
```

### 输出
APK 在 `app/build/outputs/apk/release/app-release.apk`

### 🔐 签名配置

**签名密钥（`*.jks`）和密码都必须在仓库之外管理**，否则开源后会泄露。

#### 首次使用
1. **生成自己的签名密钥**（或用现有的）：
   ```bash
   keytool -genkey -v -keystore my-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
   ```
2. **复制配置模板**：
   ```bash
   cp gradle.properties.example gradle.properties
   ```
3. **填入真实值**（在 `gradle.properties` 中）：
   ```properties
   RELEASE_STORE_FILE=my-release.jks
   RELEASE_STORE_PASSWORD=your-store-password
   RELEASE_KEY_ALIAS=my-key-alias
   RELEASE_KEY_PASSWORD=your-key-password
   ```
4. **把 jks 文件放到项目根**（或在 `RELEASE_STORE_FILE` 写绝对路径）

`gradle.properties` 已在 `.gitignore` 中排除，**不会被提交到仓库**。

#### CI 环境
推荐用环境变量（避免密码出现在任何文件）：
```bash
export MKDN_RELEASE_STORE_PASSWORD=xxx
export MKDN_RELEASE_KEY_PASSWORD=xxx
./gradlew assembleRelease
```

## 📝 文档

- [用户使用指南](docs/用户使用指南.md) — 完整操作说明（中文）

## 🤝 贡献

欢迎提交 Issue 和 PR！

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源。

## 👤 作者

**luody2005**

📧 联系方式：见 GitHub profile（https://github.com/luody2005）

## 🔗 相关项目

- [Markwon](https://github.com/noties/Markwon) — 本项目使用的 Markdown 渲染引擎
- [Prism4j](https://github.com/noties/Prism4j) — 代码语法高亮

---

**享受 Markdown 阅读 ✨**
