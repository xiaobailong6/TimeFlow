package com.lifeordered.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 应用主题：液态玻璃。
 *
 * ## 为什么固定深色
 *
 * 液态玻璃的成立前提是「背后有东西可以被折射」。浅色底上的半透明白色层
 * 几乎不可见，玻璃会退化成一块灰蒙蒙的矩形。因此这里**不跟随系统深浅色**，
 * 始终使用深墨蓝液体基底——这是设计约束，不是偷懒。
 *
 * 若将来要做浅色版，需要换一整套「浅色液体 + 深色描边」的令牌，
 * 而不是简单反转颜色。
 */

private val GlassColorScheme = darkColorScheme(
    primary = Glass.Accent,
    onPrimary = Glass.OnAccent,
    primaryContainer = Glass.FillStrong,
    onPrimaryContainer = Glass.TextPrimary,

    secondary = Glass.AccentCool,
    onSecondary = Glass.OnAccentCool,

    tertiary = Glass.AccentSuccess,
    onTertiary = Glass.OnAccentSuccess,

    background = Glass.BaseTop,
    onBackground = Glass.TextPrimary,

    surface = Glass.Fill,
    onSurface = Glass.TextPrimary,
    surfaceVariant = Glass.FillSubtle,
    onSurfaceVariant = Glass.TextSecondary,

    outline = Glass.StrokeMid,
    outlineVariant = Glass.StrokeBottom,

    error = Glass.AccentDanger,
    onError = Glass.OnAccentDanger,

    // 玻璃层内部不使用 Material 的抬升色调，全部由自绘高光负责
    surfaceTint = Color.Transparent,
    scrim = Glass.Scrim
)

@Composable
fun LifeOrderedTheme(
    /**
     * 保留参数以兼容既有调用点。
     *
     * 液态玻璃固定深色，此参数当前不生效——见文件顶部说明。
     */
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Typography 来自同包的 Type.kt（`val Typography`）。
    // 不要在这里 import androidx.compose.material3.Typography——
    // 显式导入会盖掉同包声明，`typography = Typography` 就变成了类引用而编译失败。
    MaterialTheme(
        colorScheme = GlassColorScheme,
        typography = Typography,
        content = content
    )
}
