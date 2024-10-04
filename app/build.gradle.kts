import java.io.ByteArrayOutputStream
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleKsp)
}

android {
    val grindrVersion = "24.14.0"

    namespace = "com.grindrplus"
    compileSdk = 34

    defaultConfig {
        val gitCommitHash = getGitCommitHash() ?: "unknown"
        applicationId = "com.grindrplus"
        minSdk = 21
        targetSdk = 34
        versionCode = 14
        versionName = "3.2.3-$grindrVersion ($gitCommitHash)"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "COMMIT_HASH", "\"$gitCommitHash\"")
        buildConfigField("String", "TARGET_GRINDR_VERSION", "\"$grindrVersion\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    android.applicationVariants.configureEach {
        outputs.configureEach {
            val gitCommitHash = getGitCommitHash() ?: "unknown"
            (this as BaseVariantOutputImpl).outputFileName = "GPlus_v${versionName}-${name}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.material)
    implementation(libs.square.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    compileOnly(fileTree("libs") { include("*.jar") })
}

fun getGitCommitHash(): String? {
    return try {
        if (exec { commandLine = "git rev-parse --is-inside-work-tree".split(" ") }.exitValue == 0) {
            val output = ByteArrayOutputStream()
            exec {
                commandLine = "git rev-parse --short HEAD".split(" ")
                standardOutput = output
            }
            output.toString().trim()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

tasks.register("printVersionInfo") {
    doLast {
        val versionName = android.defaultConfig.versionName
        println("VERSION_INFO: GrindrPlus v$versionName")
    }
}