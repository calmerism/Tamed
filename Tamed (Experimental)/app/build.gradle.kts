import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val releaseStoreFilePath =
    localProperties.getProperty("RELEASE_STORE_FILE")
        ?: System.getenv("RELEASE_STORE_FILE")
        ?: "keystore/release.keystore"
val releaseStorePassword =
    localProperties.getProperty("STORE_PASSWORD")
        ?: System.getenv("STORE_PASSWORD")
val releaseKeyAlias =
    localProperties.getProperty("KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")
val releaseKeyPassword =
    localProperties.getProperty("KEY_PASSWORD")
        ?: System.getenv("KEY_PASSWORD")
val requestedReleaseStoreFile = rootProject.file(releaseStoreFilePath)
val fallbackDebugStoreFile = File(System.getProperty("user.home"), ".android/debug.keystore")
val effectiveReleaseStoreFile =
    when {
        requestedReleaseStoreFile.exists() -> requestedReleaseStoreFile
        fallbackDebugStoreFile.exists() -> fallbackDebugStoreFile
        else -> requestedReleaseStoreFile
    }
val usingFallbackReleaseKeystore = effectiveReleaseStoreFile == fallbackDebugStoreFile
val effectiveReleaseStorePassword = releaseStorePassword ?: if (usingFallbackReleaseKeystore) "android" else null
val effectiveReleaseKeyAlias = releaseKeyAlias ?: if (usingFallbackReleaseKeystore) "androiddebugkey" else null
val effectiveReleaseKeyPassword = releaseKeyPassword ?: if (usingFallbackReleaseKeystore) "android" else null

android {
    namespace = "com.tamed.music"
    compileSdk = 36

    defaultConfig {
    applicationId = "com.tamed.music.lossless"
        minSdk = 26
        targetSdk = 36
        versionCode = 136
        versionName = "0.0.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val lastfmApiKey =
            localProperties.getProperty("LASTFM_API_KEY")
                ?: System.getenv("LASTFM_API_KEY")
                ?: ""
        val lastfmSecret =
            localProperties.getProperty("LASTFM_SECRET")
                ?: System.getenv("LASTFM_SECRET")
                ?: ""
        buildConfigField("String", "LASTFM_API_KEY", "\"$lastfmApiKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastfmSecret\"")

        val togetherBearerToken =
            localProperties.getProperty("TOGETHER_BEARER_TOKEN")
                ?: System.getenv("TOGETHER_BEARER_TOKEN")
                ?: ""
        buildConfigField("String", "TOGETHER_BEARER_TOKEN", "\"$togetherBearerToken\"")

        val canvasBearerToken =
            localProperties.getProperty("CANVAS_BEARER_TOKEN")
                ?: System.getenv("CANVAS_BEARER_TOKEN")
                ?: ""
        val canvasBaseUrl =
            localProperties.getProperty("CANVAS_BASE_URL")
                ?: System.getenv("CANVAS_BASE_URL")
                ?: ""
        buildConfigField("String", "CANVAS_BASE_URL", "\"$canvasBaseUrl\"")
        buildConfigField("String", "CANVAS_BEARER_TOKEN", "\"$canvasBearerToken\"")
    }

    flavorDimensions += "abi"
    productFlavors {
        create("universal") {
            dimension = "abi"
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
            buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = effectiveReleaseStoreFile
            storePassword = effectiveReleaseStorePassword
            keyAlias = effectiveReleaseKeyAlias
            keyPassword = effectiveReleaseKeyPassword
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = false
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so"
            )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val spotiFlacBackendDir = rootProject.file("spotiflac/go_backend")
val spotiFlacBackendAar = project.file("libs/gobackend.aar")
val spotiFlacBackendIosXcframework = project.file("libs/gobackend.xcframework")
val androidSdkDir = localProperties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
val gomobileBinary =
    sequenceOf(
        File(System.getProperty("user.home"), "go/bin/gomobile"),
        File("/opt/homebrew/bin/gomobile"),
        System.getenv("GOMOBILE")?.let(::File),
    ).filterNotNull().firstOrNull { it.exists() }

tasks.register<Exec>("buildSpotiFlacBackend") {
    group = "spotiflac"
    description = "Builds the embedded SpotiFLAC Go backend into app/libs/gobackend.aar using gomobile."
    workingDir = spotiFlacBackendDir
    spotiFlacBackendAar.parentFile.mkdirs()
    isIgnoreExitValue = false
    inputs.files(
        fileTree(spotiFlacBackendDir) {
            include("**/*.go")
            include("go.mod")
            include("go.sum")
        },
    )
    outputs.file(spotiFlacBackendAar)
    environment("CGO_ENABLED", "1")
    androidSdkDir?.let {
        environment("ANDROID_HOME", it)
        environment("ANDROID_SDK_ROOT", it)
    }
    environment("PATH", "${File(System.getProperty("user.home"), "go/bin").absolutePath}:/opt/homebrew/bin:${System.getenv("PATH")}")
    commandLine(
        gomobileBinary?.absolutePath ?: "gomobile",
        "bind",
        "-target=android",
        "-androidapi",
        "26",
        "-ldflags=-linkmode=external -extldflags=-Wl,-z,max-page-size=16384",
        "-o",
        spotiFlacBackendAar.absolutePath,
        ".",
    )
}

tasks.register<Exec>("buildSpotiFlacBackendIos") {
    group = "spotiflac"
    description = "Builds the embedded SpotiFLAC Go backend into app/libs/gobackend.xcframework using gomobile."
    workingDir = spotiFlacBackendDir
    spotiFlacBackendIosXcframework.parentFile.mkdirs()
    isIgnoreExitValue = false
    inputs.files(
        fileTree(spotiFlacBackendDir) {
            include("**/*.go")
            include("go.mod")
            include("go.sum")
        },
    )
    outputs.dir(spotiFlacBackendIosXcframework)
    environment("CGO_ENABLED", "1")
    environment("PATH", "${File(System.getProperty("user.home"), "go/bin").absolutePath}:/opt/homebrew/bin:${System.getenv("PATH")}")
    commandLine(
        gomobileBinary?.absolutePath ?: "gomobile",
        "bind",
        "-target=ios",
        "-o",
        spotiFlacBackendIosXcframework.absolutePath,
        ".",
    )
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("buildSpotiFlacBackend")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)
    implementation(libs.work.runtime)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    compileOnly("androidx.compose.ui:ui-tooling-preview:${libs.versions.compose.get()}")
    debugImplementation("androidx.compose.ui:ui-tooling-preview:${libs.versions.compose.get()}")
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(libs.multiplatform.markdown)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation("androidx.media3:media3-exoplayer-hls:${libs.versions.media3.get()}")
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation("androidx.media3:media3-ui:${libs.versions.media3.get()}")
    implementation(libs.squigglyslider)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.anyascii)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    implementation(libs.jsoup)
    implementation(libs.re2j)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":lastfm"))
    implementation(project(":betterlyrics"))
    implementation(project(":kizzy"))
    implementation(project(":simpmusic"))
    implementation(project(":canvas"))
    implementation(project(":shazamkit"))
    implementation("com.github.Kyant0:m3color:2025.4")
    implementation(libs.compose.cloudy)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    // Ensure ProcessLifecycleOwner is available for the presence manager and CI unit tests
    implementation("com.github.therealbush:translator:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("androidx.compose.material3.adaptive:adaptive:1.2.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-parameters"
        )
        // Suppress warnings
        suppressWarnings.set(true)
    }
}

configurations.configureEach {
    resolutionStrategy.force(
        "androidx.compose.runtime:runtime:${libs.versions.compose.get()}",
        "androidx.compose.foundation:foundation:${libs.versions.compose.get()}",
        "androidx.compose.ui:ui:${libs.versions.compose.get()}",
        "androidx.compose.ui:ui-util:${libs.versions.compose.get()}",
        "androidx.compose.ui:ui-tooling:${libs.versions.compose.get()}",
        "androidx.compose.animation:animation-graphics:${libs.versions.compose.get()}",
    )
}
