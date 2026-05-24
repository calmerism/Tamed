import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @Suppress("DEPRECATION")
    androidLibrary {
        namespace = "com.tamed.music.shared"
        compileSdk = 36
        minSdk = 26
        
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            transitiveExport = true
            export(project(":innertube"))
            export(project(":lrclib"))
            export(project(":lastfm"))
            export(project(":kugou"))
            export(project(":simpmusic"))
            export(project(":betterlyrics"))
            export(project(":canvas"))
            export(project(":shazamkit"))
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            api(project(":innertube"))
            api(project(":lrclib"))
            api(project(":lastfm"))
            api(project(":kugou"))
            api(project(":simpmusic"))
            api(project(":betterlyrics"))
            api(project(":canvas"))
            api(project(":shazamkit"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
