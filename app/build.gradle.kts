import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleKsp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.grindrplus"
    compileSdk = 35

    defaultConfig {
        val grindrVersionName = listOf("26.0.1", "25.20.0")
        val grindrVersionCode = listOf(149389, 147239)
        val gitCommitHash = getGitCommitHash() ?: "unknown"

        applicationId = "com.grindrplus"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "4.8.0-${grindrVersionName.let { it.joinToString("_") }}_$gitCommitHash"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String[]",
            "TARGET_GRINDR_VERSION_NAMES",
            grindrVersionName.let { it.joinToString(prefix = "{", separator = ", ", postfix = "}") { version -> "\"$version\"" } }
        )

        buildConfigField(
            "int[]",
            "TARGET_GRINDR_VERSION_CODES",
            grindrVersionCode.let { it.joinToString(prefix = "{", separator = ", ", postfix = "}") { code -> "$code" } }
        )
    }

    buildFeatures {
        buildConfig = true
        aidl = true
        compose = true
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

    // Room compiler emits the schema JSON for each version into this
    // directory; checking it into VCS lets reviewers see schema diffs and
    // gives any future migration tests something to compare against.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
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

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.material3)

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
    implementation(libs.fetch2) {
        exclude(group = "io.requery", module = "requery-android-database-sqlite")
    }
    implementation(libs.fetch2okhttp) {
        exclude(group = "io.requery", module = "requery-android-database-sqlite")
    }
    implementation(libs.zip.android) {
        artifact {
            type = "aar"
        }
    }
    implementation(libs.zipalign.java)
    implementation(libs.coil.gif)
    implementation(libs.arsclib)
    compileOnly(libs.bcprov.jdk18on)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

// Pinned LSPatch release. Update both values together when upgrading.
// To re-download: ./gradlew setupLSPatch -PlspatchForce=true
// To upgrade: set LSPATCH_RELEASE_TAG to the new tag, run with -PlspatchForce=true,
//             then update LSPATCH_JAR_SHA256 to the value printed by the task.
val LSPATCH_RELEASE_TAG = "v0.7"
val LSPATCH_JAR_SHA256  = "668c5e55f392139737cd1a531392d29d1941f9d1fb63c89d526c8e8a30ca1bbc"

tasks.register("setupLSPatch") {
    description = "Downloads the pinned LSPatch release JAR into libs/ and verifies SHA-256. " +
        "Pass -PlspatchForce=true to force re-download."
    doLast {
        val existingJar = File("${project.projectDir}/libs/lspatch.jar")
        val forceUpdate = project.findProperty("lspatchForce")?.toString()?.toBoolean() ?: false

        fun sha256(file: File): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            return bytes.fold("") { acc, b -> acc + "%02x".format(b) }
        }

        if (existingJar.exists() && !forceUpdate) {
            val actual = sha256(existingJar)
            if (actual == LSPATCH_JAR_SHA256) {
                println("setupLSPatch: lspatch.jar present and SHA-256 verified — skipping download.")
                return@doLast
            }
            println("setupLSPatch: SHA-256 mismatch (got $actual) — re-downloading pinned release $LSPATCH_RELEASE_TAG...")
        } else {
            println("setupLSPatch: downloading pinned LSPatch release $LSPATCH_RELEASE_TAG...")
        }

        val releaseJson = URI(
            "https://api.github.com/repos/JingMatrix/LSPatch/releases/tags/$LSPATCH_RELEASE_TAG"
        ).toURL().readText()

        val zipUrl = Regex(""""browser_download_url":\s*"([^"]+lspatch[^"]+\.zip)"""")
            .find(releaseJson)?.groupValues?.get(1)
            ?: error("setupLSPatch: could not locate lspatch ZIP in GitHub Releases API response for $LSPATCH_RELEASE_TAG")

        val tmpDir = "${System.getProperty("java.io.tmpdir")}/lspatch"

        providers.exec {
            commandLine("mkdir", "-p", tmpDir)
        }.result.get()

        providers.exec {
            commandLine("wget", "-q", zipUrl, "-O", "$tmpDir/lspatch.zip")
        }.result.get()

        providers.exec {
            commandLine("unzip", "-o", "$tmpDir/lspatch.zip", "-d", tmpDir)
        }.result.get()

        val jarPath = File(tmpDir).listFiles()?.find { it.name.contains("jar-") }?.absolutePath
            ?: error("setupLSPatch: jar not found inside downloaded zip — zip structure may have changed")

        providers.exec {
            commandLine("unzip", "-o", jarPath, "assets/lspatch/so*", "-d", "${project.projectDir}/src/main/")
        }.result.get()

        providers.exec {
            commandLine("mv", jarPath, "${project.projectDir}/libs/lspatch.jar")
        }.result.get()

        providers.exec {
            commandLine("zip", "-d", "${project.projectDir}/libs/lspatch.jar", "com/google/common/util/concurrent/ListenableFuture.class")
        }.result.get()

        providers.exec {
            commandLine("zip", "-d", "${project.projectDir}/libs/lspatch.jar", "com/google/errorprone/annotations/*")
        }.result.get()

        val downloadedSha256 = sha256(File("${project.projectDir}/libs/lspatch.jar"))
        if (downloadedSha256 != LSPATCH_JAR_SHA256) {
            println("setupLSPatch: WARNING — downloaded JAR SHA-256 ($downloadedSha256) does not match" +
                " pinned value ($LSPATCH_JAR_SHA256).")
            println("  If this is an intentional upgrade, update LSPATCH_JAR_SHA256 in app/build.gradle.kts.")
        } else {
            println("setupLSPatch: SHA-256 verified.")
        }

        println("setupLSPatch: done — lspatch.jar updated in libs/")
    }
}

fun getGitCommitHash(): String? {
    return try {
        val isGitRepo = providers.exec {
            commandLine("git", "rev-parse", "--is-inside-work-tree")
            isIgnoreExitValue = true
        }.result.get().exitValue == 0

        if (isGitRepo) {
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim()
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

tasks.register("printVersionInfo") {
    description = "Prints the current GrindrPlus version name to stdout."
    doLast {
        val versionName = android.defaultConfig.versionName
        println("VERSION_INFO: GrindrPlus v$versionName")
    }
}
