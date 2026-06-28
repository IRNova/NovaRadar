import java.net.URL
import java.net.HttpURLConnection

val appVersionName = "1.6.0"

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.novaradar.app"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.novaradar.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 5
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/nova-radar-key.jks"
      storeFile = file(keystorePath)
      storePassword = "NovaRadar2026"
      keyAlias = "nova-radar"
      keyPassword = "NovaRadar2026"
      enableV1Signing = false
      enableV2Signing = true
    }
    create("tempRelease") {
      storeFile = file("${rootDir}/temp-release-key.jks")
      storePassword = "password123"
      keyAlias = "nova-radar"
      keyPassword = "password123"
      enableV1Signing = false
      enableV2Signing = true
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = false
      isShrinkResources = false
      // Use official release key for signing
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  // APK output renaming is now handled via the androidComponents API in AGP 9.0+

  lint {
    checkReleaseBuilds = false
    abortOnError = false
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
      isUniversalApk = true
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType.name == "ABI" }?.identifier ?: "universal"
            output.outputFileName.set("NovaRadar-v${appVersionName}-${abi}-release.apk")
        }
    }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Custom task to automatically download Vazirmatn Persian fonts during build
tasks.register("downloadVazirFonts") {
    notCompatibleWithConfigurationCache("Downloads fonts dynamically")
    doLast {
        val fontDir = file("src/main/res/font")
        if (!fontDir.exists()) {
            fontDir.mkdirs()
            println("Created font resources directory at ${fontDir.absolutePath}")
        }
        val fonts = mapOf(
            "vazirmatn_regular.ttf" to "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/Vazirmatn-Regular.ttf",
            "vazirmatn_bold.ttf" to "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/Vazirmatn-Bold.ttf",
            "vazirmatn_medium.ttf" to "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/Vazirmatn-Medium.ttf"
        )
        fonts.forEach { (name, urlString) ->
            val destFile = file("src/main/res/font/$name")
            if (!destFile.exists()) {
                println("Downloading Persian font $name...")
                try {
                    val url = URL(urlString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    if (conn.responseCode == 200) {
                        conn.inputStream.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        println("Downloaded $name successfully!")
                    } else {
                        println("Server returned status ${conn.responseCode} for $name, creating fallback placeholder")
                        destFile.createNewFile()
                    }
                } catch (e: Exception) {
                    println("Failed to download font $name: ${e.message}, creating placeholder")
                    // Secure placeholder to avoid build crash
                    destFile.createNewFile()
                }
            } else {
                println("Font file $name already exists, skipping download")
            }
        }
    }
}

// Make sure fonts are downloaded before code is compiled
tasks.matching { it.name == "preBuild" }.all {
    dependsOn("downloadVazirFonts")
}


