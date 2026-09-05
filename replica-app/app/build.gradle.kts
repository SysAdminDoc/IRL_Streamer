import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val externalSigning = mapOf(
    "storeFile" to providers.environmentVariable("IRL_STREAMER_KEYSTORE_FILE").orNull,
    "storePassword" to providers.environmentVariable("IRL_STREAMER_KEYSTORE_PASSWORD").orNull,
    "keyAlias" to providers.environmentVariable("IRL_STREAMER_KEY_ALIAS").orNull,
    "keyPassword" to providers.environmentVariable("IRL_STREAMER_KEY_PASSWORD").orNull,
)
val externalSigningConfigured = externalSigning.values.all { !it.isNullOrBlank() }

android {
    namespace = "com.irlstreamer.reconstruction"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.irlstreamer.reconstruction"
        minSdk = 28
        targetSdk = 36
        versionCode = 5
        versionName = "0.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (externalSigningConfigured) {
            create("external") {
                storeFile = file(requireNotNull(externalSigning["storeFile"]))
                storePassword = requireNotNull(externalSigning["storePassword"])
                keyAlias = requireNotNull(externalSigning["keyAlias"])
                keyPassword = requireNotNull(externalSigning["keyPassword"])
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfigs.findByName("external")?.let { signingConfig = it }
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_DEBUG_STATE_SELECTOR", "true")
        }
        release {
            signingConfigs.findByName("external")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "ENABLE_DEBUG_STATE_SELECTOR", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // The engine logs through android.util.Log on its failure paths, and the
        // stub android.jar throws on every unmocked call. Without this the JVM
        // tests can only reach the happy paths.
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")


    // Capture, encode and RTMP publish. The endpoint artifact is `streampack-rtmp`
    // since 3.1; `streampack-extension-rtmp` stopped at 3.0.0-RC2. SRT is
    // deliberately not pulled in yet: it would drag srtdroid's bundled libsrt
    // along with it (see ROADMAP IS-52).
    val streamPackVersion = "3.2.0"
    implementation("io.github.thibaultbee.streampack:streampack-core:$streamPackVersion")
    implementation("io.github.thibaultbee.streampack:streampack-rtmp:$streamPackVersion")
    implementation("io.github.thibaultbee.streampack:streampack-compose:$streamPackVersion")

    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
