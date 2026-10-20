plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.lifeordered"
  // Compose 1.12.1 与 Haze 2.0.1 的 AAR metadata 都要求 minCompileSdk = 37。
  //
  // 注意：**不能只写 `compileSdk = 37`**。SDK 仓库里并没有
  // `platforms;android-37`，只有带小版本号的 `android-37.0` / `37.1` / `37.2`
  // （API 37 起 Google 改成了「主版本.小版本」的目录命名）。
  // 只写 37 时 AGP 会去找 `android-37` 然后报 "Failed to find target"。
  // 用 `compileSdkMinor` 才能定位到 `android-37.0`。
  compileSdk = 37
  compileSdkMinor = 0

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
      // 配了固定签名就用它签 debug 包。
      //
      // 默认的 debug keystore 是每台机器/每次 CI 容器各自生成的，签名不一致，
      // 覆盖安装会失败（INSTALL_FAILED_UPDATE_INCOMPATIBLE），用户必须卸载重装，
      // 本地数据也会丢。CI 产出的 APK 要能「装上并覆盖上一版」，签名就必须固定。
      //
      // 没有配置时（本地开发、fork 仓库）自动退回 AGP 默认 debug keystore，
      // 不会因为缺少密钥而构建失败。
      if (releaseSigningEnabled) {
        signingConfig = signingConfigs.getByName("release")
      }
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
  implementation(libs.haze.core)
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
  // 强制把 kotlin-metadata-jvm 升到与 Kotlin 编译器相同的版本。
  //
  // Room 的注解处理器在 kapt 里要读我们 @Entity 类的 Kotlin 元数据，
  // 但它自带的 kotlin-metadata-jvm 只能读到 2.2.0：
  //   room-compiler-processing 2.7.0 → kotlin-metadata-jvm 2.1.10
  //   room-compiler-processing 2.8.5 → kotlin-metadata-jvm 2.2.0（最新版也一样）
  // 而 Kotlin 2.4.20 产出的元数据版本是 2.4.0，于是 kapt 阶段直接抛：
  //   IllegalArgumentException: Provided Metadata instance has version 2.4.0,
  //   while maximum supported version is 2.2.0.
  // 升级 Room 解决不了（最新 2.8.5 仍是 2.2.0），只能把读元数据的库换新。
  // kapt 配置即处理器类路径，Gradle 取最高版本，故 2.4.20 会胜出。
  kapt(libs.kotlin.metadata.jvm)
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

// Kotlin 的 jvmTarget 必须与 `compileOptions`（VERSION_11）一致。
//
// 关掉 AGP 内置 Kotlin、改用标准 KGP 之后，Kotlin 不再由 AGP 自动对齐 JVM 目标：
// KGP 默认取「运行 Gradle 的那个 JDK」作为 jvmTarget，而 CI 现在用 JDK 21，
// 于是 Kotlin 出 21、Java 出 11，AGP 会直接报
// "Inconsistent JVM-target compatibility detected"。
// 这里显式钉成 11，与 `compileOptions` 对齐。
//
// 注意：jvmTarget 是**产出的字节码版本**，与运行 Gradle 的 JDK 版本是两回事，
// 所以升到 JDK 21 不需要改动这里。
kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
  }
}

// Robolectric 在 Java 17+ 上需要打开若干 JDK 内部包，否则沙箱创建阶段会因
// 反射被模块系统拦截而失败。这些参数在 JDK 21 上同样必需，且对不需要它们的
// 测试任务无害，因此统一加给所有 Test 任务。
//
// 列表取自 Robolectric 官方文档中「Java 17+ 需要追加的 JVM 参数」。
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
  jvmArgs(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/java.text=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.util=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
  )
}
