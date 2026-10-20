plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.lifeordered"
  compileSdk = 36

  val releaseKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
  val releaseSigningEnabled = releaseKeystorePath != null &&
    providers.environmentVariable("STORE_PASSWORD").orNull != null &&
    providers.environmentVariable("KEY_PASSWORD").orNull != null &&
    file(releaseKeystorePath).exists()

  defaultConfig {
    applicationId = "com.aistudio.lifeordered.warmth"
    minSdk = 31
    targetSdk = 36
    versionCode = 2
    versionName = "1.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (releaseSigningEnabled) {
      create("release") {
        storeFile = file(releaseKeystorePath!!)
        storePassword = providers.environmentVariable("STORE_PASSWORD").orNull
        keyAlias = providers.environmentVariable("KEY_ALIAS").orNull ?: "upload"
        keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (releaseSigningEnabled) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      // Use Android Gradle Plugin's default debug keystore in local and CI builds.
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

  sourceSets {
    getByName("test") {
      java.setSrcDirs(listOf("src/logic_check/java"))
      resources.setSrcDirs(listOf("src/logic_check/resources"))
    }
    getByName("androidTest") {
      java.setSrcDirs(listOf("src/hardware_check/java"))
      resources.setSrcDirs(listOf("src/hardware_check/resources"))
    }
  }
}

// 只保留实际被代码引用的依赖。
// 已移除：retrofit / converter-moshi / okhttp / logging-interceptor / moshi-kotlin
// —— 全仓没有任何网络请求或 JSON 序列化代码（数据导出改用 Android 内置的 org.json）。
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.haze)
  implementation(libs.haze.glass)
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
  kapt(libs.androidx.room.compiler)
}

// Room 的注解处理器改由 kapt 运行。
//
// 原因不是偏好，而是硬约束：KSP 目前没有 Kotlin 2.4.x 的发布版本
// （最新 2.3.12 依赖 kotlin-stdlib 2.3.20），而 Haze 2 的玻璃模块
// 要求 kotlin-stdlib >= 2.4.10，两者无法共存。
// Room 2.7.0 完整支持 kapt，correctErrorTypes 是它推荐的配套设置。
kapt {
  correctErrorTypes = true
}
