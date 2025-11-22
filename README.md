# NutrientTracker (Android · Kotlin · Jetpack Compose)

一个**离线优先**的每日营养摄入记录 App，支持本地食物库、目标管理、趋势图；并内置 **AI 图片营养分析**（支持 Kimi、通义千问 Qwen、火山方舟 Doubao）。

> ⚠️ 仅供个人学习与记录，不作为医疗/营养处方依据。

---

## ✨ 功能特性

* **食物库（本地 Room）**

  * 新增自定义食物（以“每 100g 成分”建模）
  * 一键按“克数 / 份数”记录摄入
* **每日目标**

  * 手动设置，或根据身高体重等自动估算
  * 进度条 + 当日汇总 + 最近 7 天趋势
* **AI 图片营养分析（Beta）**

  * 选图 → AI 识别食物/克数 → 输出卡路里及宏微量营养素
  * 支持 **Kimi (Moonshot)**、**Qwen (DashScope)**、**Doubao (Ark)**（OpenAI 已移除）
  * 支持**保存多个 API 配置**，下拉选择使用哪个
  * 可将 AI 结果“**营养直加**”到当日记录
* **导出**

  * 导出当日或全部历史 CSV
* **本地数据库**

  * 所有数据保存在 `nutrition.db`（Room）

---

## 🧱 技术栈

* **Kotlin 2.x** + **Jetpack Compose**（Material 3, Icons）
* **Room**（Food / Entry / Goal / CustomEntry / ApiSetting / AiSession）
* **Retrofit + OkHttp + Gson**（AI 网络层，OpenAI 兼容协议）
* **Coil**（Compose 图片加载）
* **Activity Result API**（系统相册选图）

---

## 📁 目录结构（简化）

```
app/
 └─ src/main/java/com/example/nutrienttrackerv1/
    ├─ ui/                 // Compose UI（MainActivity、各 Screen）
    ├─ data/               // Entity / Dao / Room Database / Repository
    └─ ai/                 // AiService.kt, AiModels.kt（协议与工具）
```

---

## 🗃 数据表（Room）

* `foods`：食物库（每 100g 成分）
* `entries`：按食物库记录的当天摄入
* `custom_entries`：AI 直加或手动直加的营养条目（已是“总量”）
* `goals`：每日营养目标（唯一一行 `id=1`）
* `api_settings`：AI 提供商配置（provider, api_key, model, endpoint）
* `ai_sessions`：AI 调用结果留存（可选）

---

## 🚀 快速开始

### 1) 环境需求

* Android Studio (Koala+)
* Android Gradle Plugin 与 Kotlin 2.x
* minSdk ≥ 24，targetSdk 按项目 `build.gradle.kts`

### 2) 打开项目并同步依赖

* 用 Android Studio 打开项目根目录
* 等待 Gradle Sync 完成
* 若提示 Compose Compiler 插件，请确保项目已在 `settings.gradle`/`build.gradle` 配置 compose-compiler 版本（本项目已配好）

### 3) 权限

`app/src/main/AndroidManifest.xml`：

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

### 4) 运行

* 选择模块 **app**，设备选模拟器或真机，点击 Run ▶️
* 首次启动会自动创建/初始化 `nutrition.db`

---

## 🤖 配置 AI

1. 进入 **AI** 页签
2. 顶部下拉选择已有配置，或点击“＋ 新建自定义配置…”
3. 填写并保存：

   * `provider`：自定义名称（例如 “Kimi (Moonshot)” / “Qwen (DashScope)” / “Doubao (Ark)”）
   * `api_key`：各平台的 API Key
   * `model`（推荐）：

     * **Kimi**：`moonshot-v1-8k-vision`
     * **Qwen**：`qwen2.5-vl`
     * **Doubao**：`doubao-vision-lite`
   * `endpoint`（兼容模式）：

     * Kimi `https://api.moonshot.cn/v1/chat/completions`
     * Qwen `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
     * Doubao `https://ark.cn-beijing.volces.com/api/v3/chat/completions`
4. 选择图片 → “开始分析” → 勾选条目 → “✅ 添加选中项（AI 直加）”

> App 会将 API 配置保存在本地数据库，可随时切换/删除。

---

## 🔧 构建与发布

### Debug 构建

Android Studio 直接 Run 即可（`app-debug.apk` 位于 `app/build/outputs/apk/debug/`）。

### Release APK

1. **Build > Generate Signed App Bundle / APK…**
2. 选择 **APK** → **Create new…** 生成 keystore（或选择已有 keystore）
3. Build Type 选择 **release**（勾选 V1/V2/V3 签名）
4. 输出：`app/build/outputs/apk/release/app-release.apk`

> **Passwords do not match**：确保 keystore 密码与确认一致；若为新建可勾选“Use same password for key”。

---

## 🧩 关键依赖（节选）

```kotlin
// Compose
implementation("androidx.activity:activity-compose:1.9.3")
implementation("androidx.compose.material3:material3:1.3.0")
implementation("androidx.compose.material:material-icons-extended:1.7.4")

// Room
implementation("androidx.room:room-runtime:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// 网络 & 图片
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("io.coil-kt:coil-compose:2.7.0")

// 协程
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
```

---

## 🧪 常见问题（FAQ / Troubleshooting）

* **看不到 App 或总是“Hello Android!”**

  * Manifest 的 LAUNCHER Activity 是否指向你的 `MainActivity` 完整类名
  * 右上角 Run 配置选择模块 `app`
  * 清理重装：卸载旧应用 → Clean/Rebuild

* **`setContent` / Composable 报错**

  * 确认有 `implementation("androidx.activity:activity-compose:1.9.3")`
  * 用 `@Composable` 标注组合函数；`setContent { … }` 只能在 `ComponentActivity` 内调用

* **Material3 实验 API 警告**

  * 在文件顶部添加：
    `@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)`

* **图标 `Icons.Default.*` 未解析**

  * 添加依赖 `material-icons-extended` 并导入：
    `import androidx.compose.material.icons.Icons`
    `import androidx.compose.material.icons.filled.*`

* **图片选择器参数不匹配**

  * 使用新版 API：

    ```kotlin
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> … }
    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    ```

* **`withContext` / `Dispatchers` 未解析**

  * 加依赖 `kotlinx-coroutines-android` 并导入：
    `import kotlinx.coroutines.Dispatchers`、`import kotlinx.coroutines.withContext`

* **AI 请求超时**

  * 已在网络层将超时提升到 120s
  * 模型必须为“视觉版”；图片太大时会自动压缩（长边 ~1280，JPEG ~85）
  * 网络抖动可重试或换更小图片

* **导出 Release 提示密码不匹配**

  * Keystore/Key 密码与确认必须一致；可勾选“Use same password for key”

---

## 🔒 隐私与安全

* 数据默认存储在本地 `nutrition.db`；AI 分析会将图片（压缩后 base64）随请求发往你选择的第三方模型服务，请自行评估风险。
* API Key 保存在本地数据库，仅用于本机调用。

---

## 📜 许可证

MIT（可按你需要替换）

---

## 🙌 鸣谢

* Jetpack Compose / Room / Coil
* Moonshot（Kimi）、Alibaba Cloud DashScope（Qwen）、Volcengine Ark（Doubao）

---
