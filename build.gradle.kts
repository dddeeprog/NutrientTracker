// build.gradle.kts (Project level)
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false   // ← 必需
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
}
