import java.io.ByteArrayOutputStream
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.net.URL

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleKsp)
    alias(libs.plugins.compose.compiler)
}

android {
    val grindrVersionName = listOf("25.12.1")
    val grindrVersionCode = listOf(140963)

    namespace = "com.grindrplus"
    compileSdk = 35

    defaultConfig {
        val gitCommitHash = getGitCommitHash() ?: "unknown"
        applicationId = "com.grindrplus"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "4.5.0-${grindrVersionName.joinToString("_")}_$gitCommitHash"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String[]",
            "TARGET_GRINDR_VERSION_NAMES",
            grindrVersionName.joinToString(prefix = "{", separator = ", ", postfix = "}") { "\"$it\"" }
        )

        buildConfigField(
            "int[]",
            "TARGET_GRINDR_VERSION_CODES",
            grindrVersionCode.joinToString(prefix = "{", separator = ", ", postfix = "}") { "$it" }
        )

        buildFeatures {
            compose = true
        }
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

    applicationVariants.configureEach {
        outputs.configureEach {
            val sanitizedVersionName = versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
            (this as BaseVariantOutputImpl).outputFileName =
                "GPlus_v${sanitizedVersionName}-${name}.apk"
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
    implementation(libs.androidx.runtime.android)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    compileOnly(fileTree("libs") { include("*.jar") })
    implementation(fileTree("libs") { include("lspatch.jar") })

    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)

    // Material Design 3
    implementation(libs.androidx.material3)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.markdown)
    implementation(libs.plausible.android.sdk)
    implementation(libs.timber)
    implementation(libs.fetch2)
    implementation(libs.fetch2okhttp)
    implementation(libs.rootbeer.lib)
    implementation(libs.zip.android) {
        artifact {
            type = "aar"
        }
    }
    implementation(libs.zipalign.java)
    implementation(libs.coil.gif)
    implementation(libs.arsclib)
    compileOnly(libs.bcprov.jdk18on)
}

tasks.register("setupLSPatch") {
    doLast {
        val jarUrl =
            Regex("https:\\/\\/nightly\\.link\\/JingMatrix\\/LSPatch\\/workflows\\/main\\/master\\/lspatch-debug-[^.]+\\.zip").find(
                URL("https://nightly.link/JingMatrix/LSPatch/workflows/main/master?preview").readText()
            )!!.value

        exec {
            commandLine = listOf("mkdir", "-p", "/tmp/lspatch")
        }

        exec {
            commandLine = listOf("wget", jarUrl, "-O", "/tmp/lspatch/lspatch.zip")
        }

        exec {
            commandLine = listOf("unzip", "-o", "/tmp/lspatch/lspatch.zip", "-d", "/tmp/lspatch")
        }

        val jarPath =
            File("/tmp/lspatch").listFiles()?.find { it.name.contains("jar-") }?.absolutePath

        exec {
            commandLine = listOf("unzip", "-o", jarPath, "assets/lspatch/so*", "-d", "src/main/")
        }

        exec {
            commandLine = listOf("mv", jarPath, "./libs/lspatch.jar")
        }

        exec {
            commandLine = listOf("zip", "-d", "./libs/lspatch.jar", "com/google/common/util/concurrent/ListenableFuture.class")
        }

        exec {
            commandLine = listOf("zip", "-d", "./libs/lspatch.jar", "com/google/errorprone/annotations/*")
        }
    }
}

fun getGitCommitHash(): String? {
    return try {
        if (exec {
                commandLine = "git rev-parse --is-inside-work-tree".split(" ")
            }.exitValue == 0) {
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