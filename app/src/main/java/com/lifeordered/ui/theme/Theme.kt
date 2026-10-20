package com.lifeordered.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 应用主题：液态玻璃（透明白色版）。
 *
 * ## 为什么是亮色，而不是跟随系统
 *
 * 上一版固定深墨蓝，理由是「浅色底上半透明白色层几乎不可见」。
 * 用户明确要求改成透明白色，所以真正要解决的不是「换不换」，
 * 而是「换了之后玻璃还看不看得见」。
 *
 * 答案是：**浅色玻璃的可见性来自背景的彩色光斑，而不是来自基底本身。**
 * [LiquidGlassBackground] 在近白基底上铺四团低饱和光斑，
 * 半透明白片经过不同色区时透出的颜色不同，眼睛才会读到「透明」。
 * 如果背景是纯白，这层玻璃确实会消失——所以基底不能是纯白。
 *
 * ## 为什么不跟随系统深浅色
 *
 * 跟随系统意味着要维护两套完全不同的令牌（深色靠低透明白、浅色靠高透明白 +
 * 深色描边），而玻璃的成立条件相反。本项目只做一套亮色，
 * 系统处于深色模式时也用亮色，保证观感一致。
 */
private val GlassColorScheme = lightColorScheme(
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

    // surface 用接近实心的白：Material 组件（弹层、下拉、BottomSheet）取到它作为
    // **不透明底**，若映射到 Glass.Fill（30% 白）文字会糊在背景上。
    surface = Glass.SheetFill,
    onSurface = Glass.TextPrimary,
    surfaceVariant = Glass.FillStrong,
    onSurfaceVariant = Glass.TextSecondary,

    // 描边令牌必须「在浅底上看得见」：StrokeMid 是近白，做 outline 等于没画，
    // 所以取暗档与极细线。
    outline = Glass.StrokeBottom,
    outlineVariant = Glass.Hairline,

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
     * 液态玻璃只做亮色一套，此参数当前不生效——见文件顶部说明。
     */
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
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
