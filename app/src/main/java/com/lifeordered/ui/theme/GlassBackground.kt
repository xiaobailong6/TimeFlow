package com.lifeordered.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液体背景：玻璃面板背后的「内容」。
 *
 * 玻璃之所以看起来像玻璃，是因为它**遮住了别的东西**。如果背景是纯色，
 * 半透明层和实色层在视觉上没有任何区别，整套液态玻璃就白做了。
 *
 * ## 浅色版的关键：光斑必须够明显
 *
 * 上一版（深墨蓝底）光斑 alpha 只有 0.14–0.22，深色底上够用，
 * 但在近白底上会被基底直接冲淡成一片灰白，白玻璃压上去等于隐形。
 * 这一版把 alpha 提到 0.22–0.30，并**降低饱和度**（见 [Glass.GlowAmber] 等）：
 * 高饱和色在浅底上会把白玻璃染成脏色块，低饱和才读得出「淡彩水光」。
 *
 * 光斑是静态的：给整屏加无限动画会让低端机在列表滚动时掉帧，
 * 而滚动本身已经在改变「哪个颜色透过哪张卡片」，运动感并不缺。
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Glass.BaseTop, Glass.BaseBottom)
                )
            )
    ) {
        // 左上：暖橙，偏大，压住顶部标题区
        GlowOrb(
            color = Glass.GlowAmber,
            size = 340.dp,
            alpha = 0.30f,
            alignment = Alignment.TopStart,
            offsetX = (-100).dp,
            offsetY = (-130).dp
        )

        // 右侧偏上：玫红，为「重要时刻」区提供暖调
        GlowOrb(
            color = Glass.GlowRose,
            size = 300.dp,
            alpha = 0.24f,
            alignment = Alignment.TopEnd,
            offsetX = 80.dp,
            offsetY = 40.dp
        )

        // 左下：青蓝，中和整体的暖色，避免一片橙红
        GlowOrb(
            color = Glass.GlowCyan,
            size = 320.dp,
            alpha = 0.24f,
            alignment = Alignment.BottomStart,
            offsetX = (-80).dp,
            offsetY = 100.dp
        )

        // 右下：紫罗兰，给底部导航与列表尾段一点深度
        GlowOrb(
            color = Glass.GlowViolet,
            size = 280.dp,
            alpha = 0.22f,
            alignment = Alignment.BottomEnd,
            offsetX = 70.dp,
            offsetY = 70.dp
        )

        content()
    }
}

/**
 * 单个光斑。
 *
 * 用「实心圆 + 径向渐变到透明」而不是模糊：`radialGradient` 的边缘本身就是
 * 平滑衰减的，观感等同于大半径高斯模糊，但不需要 API 31 的 `blur()`。
 *
 * 注意接收者是 [BoxScope]：`align` 是 `BoxScope` 的成员扩展，
 * 不是顶层函数，抽成独立函数时必须把接收者一起搬过来，否则编译报
 * `Unresolved reference 'align'`。
 */
@Composable
private fun BoxScope.GlowOrb(
    color: Color,
    size: Dp,
    alpha: Float,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.45f),
                        Color.Transparent
                    )
                )
            )
    )
}
