// Root build script. Individual modules (engine, app) configure their own plugins.
plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("com.android.application") version "8.5.2" apply false
    kotlin("android") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    kotlin("plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
