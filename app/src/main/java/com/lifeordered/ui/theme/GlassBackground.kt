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
 * 因此这里铺了四个大尺寸的径向渐变光斑，分别用暖橙、玫红、青蓝、紫罗兰，
 * 位置刻意不对齐（避开对称构图），让每个屏幕区域透出的颜色都不同——
 * 滚动时卡片会经过不同色区，产生真实的「液体流动」错觉。
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
            size = 320.dp,
            alpha = 0.22f,
            alignment = Alignment.TopStart,
            offsetX = (-90).dp,
            offsetY = (-120).dp
        )

        // 右侧偏上：玫红，为「重要时刻」区提供暖调
        GlowOrb(
            color = Glass.GlowRose,
            size = 280.dp,
            alpha = 0.16f,
            alignment = Alignment.TopEnd,
            offsetX = 70.dp,
            offsetY = 40.dp
        )

        // 左下：青蓝，中和整体的暖色，避免一片橙红
        GlowOrb(
            color = Glass.GlowCyan,
            size = 300.dp,
            alpha = 0.15f,
            alignment = Alignment.BottomStart,
            offsetX = (-70).dp,
            offsetY = 90.dp
        )

        // 右下：紫罗兰，给底部导航与列表尾段一点深度
        GlowOrb(
            color = Glass.GlowViolet,
            size = 260.dp,
            alpha = 0.14f,
            alignment = Alignment.BottomEnd,
            offsetX = 60.dp,
            offsetY = 60.dp
        )

        content()
    }
}

/**
 * 单个光斑。
 *
 * 用「实心圆 + 径向渐变到透明」而不是模糊：`radialGradient` 的边缘本身就是
 * 平滑衰减的，观感等同于大半径高斯模糊，但不需要 API 31 的 `blur()`。
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
