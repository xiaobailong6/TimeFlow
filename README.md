# 拾光流年（LifeOrdered）

一款奶油泥塑风的暖系生活记录助手，提供纪念日倒计时、耗材寿命维护和安全库存提醒。

## GitHub 项目结构

```text
.
├── app/                         # Android 应用模块
│   └── src/main/                # 应用源码与资源
├── gradle/libs.versions.toml    # 依赖与插件版本目录
├── .github/workflows/android.yml # GitHub Actions 构建流程
├── gradlew                      # Linux/macOS/CI 入口
└── gradlew.bat                  # Windows 入口
```

工程已从 Google AI Studio 导出结构整理为标准 Gradle Android 仓库：Gradle Wrapper、依赖版本目录、源码模块和 GitHub Actions 均位于仓库内，克隆后不需要提交本机 `local.properties` 或构建产物。

## 本地开发

### 环境要求

- Android Studio（建议使用最新稳定版）
- JDK 17
- Android SDK 36

使用 Android Studio 打开仓库根目录，等待 Gradle 同步完成后即可运行。也可以在命令行执行：

```bash
# macOS/Linux
./gradlew testDebugUnitTest
./gradlew assembleDebug

# Windows
./gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```

Debug APK 输出在 `app/build/outputs/apk/debug/`。

## GitHub Actions

工作流文件位于 `.github/workflows/android.yml`，会在推送到 `main`/`master`、Pull Request 或手动触发时：

1. 使用 JDK 17 和 Gradle Wrapper；
2. 安装 Android SDK 36 与对应 Build Tools；
3. 执行 Debug 单元测试；
4. 编译 Debug APK；
5. 上传测试报告和 `lifeordered-debug-apk` 构建产物。

GitHub Actions 不依赖本机 Android SDK，也不需要签名密钥即可生成 Debug APK。

## 签名与敏感信息

- `.env`、`debug.keystore`、`local.properties` 和构建产物不会提交到 Git。
- Release 签名通过环境变量配置：`KEYSTORE_PATH`、`STORE_PASSWORD`、`KEY_PASSWORD`，可选 `KEY_ALIAS`（默认 `upload`）。
- 正式发布时应通过 GitHub Actions Secrets 或受保护的密钥文件注入签名信息，不要将 keystore 或密码提交到仓库。

## 许可

当前仓库未声明开源许可证。如需公开分发，请补充合适的 `LICENSE` 文件。
