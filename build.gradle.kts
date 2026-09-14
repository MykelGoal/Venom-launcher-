// Root build file. All module config lives in app/build.gradle.kts
// AGP 9.0+ ships Kotlin support built in, so there is deliberately no
// `org.jetbrains.kotlin.android` plugin here. Only the compiler plugins are external.
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
}
