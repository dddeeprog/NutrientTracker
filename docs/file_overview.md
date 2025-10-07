# 文件功能说明

本文按照目录划分，概述 NutrientTracker 项目中主要文件与目录的职责，便于快速了解代码结构。

## 根目录
- `README.md`：项目总览，介绍功能特性、技术栈、目录结构、数据表、快速开始与常见问题，帮助开发者理解应用定位与使用方式。【F:README.md†L1-L200】
- `build.gradle.kts`：定义项目级 Gradle 插件版本（Android、Kotlin、Compose、KSP），供所有模块共享。【F:build.gradle.kts†L1-L7】
- `settings.gradle.kts`：配置插件与依赖仓库、设定根项目名称，并包含 `app` 模块。【F:settings.gradle.kts†L1-L24】
- `gradle.properties`：统一 Gradle/JVM 参数与 AndroidX、Kotlin 风格设置（如 `android.useAndroidX`、`kotlin.code.style`）。【F:gradle.properties†L1-L23】
- `gradlew` / `gradlew.bat`：跨平台的 Gradle Wrapper 启动脚本，用于在 Unix/Windows 上调用项目固定版本的 Gradle。【F:gradlew†L1-L58】【F:gradlew.bat†L1-L62】
- `gradle/wrapper/gradle-wrapper.properties`：指向 Gradle 8.13 分发包，供 Wrapper 下载并缓存指定版本。【F:gradle/wrapper/gradle-wrapper.properties†L1-L7】

## `app` 模块
### 构建与配置
- `app/build.gradle.kts`：模块级构建脚本，启用 Compose、KSP，配置 SDK 版本、构建类型、Java 17 选项，并声明 Compose、Room、Retrofit、Coil 等依赖。【F:app/build.gradle.kts†L1-L105】
- `app/proguard-rules.pro`：混淆规则占位文件，可在发布版构建时添加额外保留/压缩设置。【F:app/proguard-rules.pro†L1-L16】
- `app/src/main/AndroidManifest.xml`：声明应用入口 Activity 与 `INTERNET` 权限，关联主题与图标资源。【F:app/src/main/AndroidManifest.xml†L1-L18】

### 源代码（`app/src/main/java/com/example/nutrienttrackerv1`）
#### `data` 层
- `Entities.kt`：定义 Room 数据实体（食物、记录、目标、自定义条目、AI 会话、API 配置）、一对多关系与营养元数据表常量。【F:app/src/main/java/com/example/nutrienttrackerv1/data/Entities.kt†L1-L122】
- `DaoDatabase.kt`：提供 Food/Entry/Goal/CustomEntry/ApiSetting/AiSession DAO 接口及 `AppDatabase` 抽象类，集中声明数据库访问方法与关联实体。【F:app/src/main/java/com/example/nutrienttrackerv1/data/DaoDatabase.kt†L1-L106】
- `Repository.kt`：封装对各 DAO 的调用，提供添加/删除食物与记录、维护目标、管理 AI 设置与会话等业务操作，供 ViewModel 使用。【F:app/src/main/java/com/example/nutrienttrackerv1/data/Repository.kt†L1-L111】

#### `util` 层
- `TimeAndMath.kt`：提供日期格式化扩展与按克数换算营养值的工具函数。【F:app/src/main/java/com/example/nutrienttrackerv1/util/TimeAndMath.kt†L1-L14】

#### `ai` 层
- `AiModels.kt`：声明多模态对话请求/响应的数据模型、提示词模板及 AI 解析结果结构。【F:app/src/main/java/com/example/nutrienttrackerv1/ai/AiModels.kt†L1-L43】
- `AiService.kt`：定义 Retrofit 接口、构建带日志/超时设置的 Retrofit 客户端，提供图片压缩、Base64 Data URL 构造、聊天请求封装与 AI JSON 解析工具。【F:app/src/main/java/com/example/nutrienttrackerv1/ai/AiService.kt†L1-L107】

#### `ui` 层
- `MainActivity.kt`：应用入口 Activity 及所有 Compose 界面。包含底部导航标签与“今日、记录、食物库、目标、导出、趋势、AI”等页面的 UI 与交互逻辑（添加/删除食物与记录、导出 CSV、触发 AI 分析等）。【F:app/src/main/java/com/example/nutrienttrackerv1/ui/MainActivity.kt†L1-L280】
- `MainViewModel.kt`：继承 `AndroidViewModel`，初始化 Room 数据库与 `Repository`，维护 StateFlow 状态、处理增删改查、统计营养总量、最近 7 天趋势及 AI API 配置管理，向 UI 提供数据。【F:app/src/main/java/com/example/nutrienttrackerv1/ui/MainViewModel.kt†L1-L286】
- `ui/theme/Color.kt`：定义 Compose 主题使用的基础色值常量。【F:app/src/main/java/com/example/nutrienttrackerv1/ui/theme/Color.kt†L1-L11】
- `ui/theme/Theme.kt`：配置深浅色配色方案与 `NutrientTrackerv1Theme` 组合，支持 Android 12+ 动态色彩。【F:app/src/main/java/com/example/nutrienttrackerv1/ui/theme/Theme.kt†L1-L58】
- `ui/theme/Type.kt`：定制 Material3 字体排印设置的入口，当前覆盖 `bodyLarge`。【F:app/src/main/java/com/example/nutrienttrackerv1/ui/theme/Type.kt†L1-L34】

### 资源（`app/src/main/res`）
- `values/strings.xml`：保存应用名称字符串。【F:app/src/main/res/values/strings.xml†L1-L3】
- `values/colors.xml`：保留模板色值（紫/青/黑/白），可供主题或组件引用。【F:app/src/main/res/values/colors.xml†L1-L10】
- `values/themes.xml`：定义 `Theme.NutrientTrackerv1`，供 Manifest 关联应用主题。【F:app/src/main/res/values/themes.xml†L1-L5】
- `drawable/ic_launcher_background.xml` 与 `drawable/ic_launcher_foreground.xml`：自适应图标的背景/前景矢量资源，供启动图标组合使用。【F:app/src/main/res/drawable/ic_launcher_background.xml†L1-L60】【F:app/src/main/res/drawable/ic_launcher_foreground.xml†L1-L25】
- `mipmap-anydpi/ic_launcher.xml` 与 `ic_launcher_round.xml`：自适应图标定义，引用上述前景与背景资源；其它 `mipmap-*` 目录下的 WebP 位图提供不同分辨率的启动图标位图。【F:app/src/main/res/mipmap-anydpi/ic_launcher.xml†L1-L6】【F:app/src/main/res/mipmap-anydpi/ic_launcher_round.xml†L1-L6】【10dc5f†L1-L20】
- `xml/data_extraction_rules.xml` 与 `xml/backup_rules.xml`：Android 12+ 的数据备份/迁移规则模板，可按需开启备份或设备迁移。【F:app/src/main/res/xml/data_extraction_rules.xml†L1-L19】【F:app/src/main/res/xml/backup_rules.xml†L1-L13】

### 测试代码
- `app/src/androidTest/java/com/example/nutrienttrackerv1/ExampleInstrumentedTest.kt`：运行于设备的示例仪器化测试，校验应用包名。【F:app/src/androidTest/java/com/example/nutrienttrackerv1/ExampleInstrumentedTest.kt†L1-L24】
- `app/src/test/java/com/example/nutrienttrackerv1/ExampleUnitTest.kt`：本地单元测试样例，验证简单的加法断言。【F:app/src/test/java/com/example/nutrienttrackerv1/ExampleUnitTest.kt†L1-L17】

### 其它
- `app/src/main/res/mipmap-*/ic_launcher*.webp`：各密度的启动图标位图资源，由模板生成，与适配图标 XML 配合使用。【10dc5f†L1-L20】

