pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    // Maven Central 的镜像冗余。
    //
    // repo.maven.apache.org 与 repo1.maven.org 是同一份内容的不同主机名。
    // CI 曾出现「kotlin-gradle-plugins-bom:2.4.20 在 mavenCentral 与
    // gradlePluginPortal 都找不到」而产物实际存在的情况（事后复测两个 URL
    // 都是 200 且内容一致），属于单主机/CDN 边缘异常。
    // 多挂一个同内容主机名，让一次边缘抖动不至于让整轮构建失败。
    maven("https://repo1.maven.org/maven2")
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://repo1.maven.org/maven2")
  }
}

rootProject.name = "LifeOrdered"

include(":app")
