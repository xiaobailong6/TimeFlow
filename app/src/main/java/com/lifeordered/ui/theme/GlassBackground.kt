package com.lifeordered.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * 液体背景：玻璃面板背后的「内容」，同时也是 Haze 的**采样源**。
 *
 * 玻璃之所以看起来像玻璃，是因为它**折射并模糊了背后的东西**。
 * 所以这一层的职责不只是「好看」：它必须被 [hazeSource] 标记，
 * 玻璃面板才有东西可以采样。
 *
 * ## 光斑为什么可以更饱和
 *
 * 上一版没有真实模糊，光斑只能靠径向渐变自己羽化，alpha 压在 0.22–0.30；
 * 现在 Haze 会做深度模糊与边缘色散，色块被涂抹开，饱和度高一些反而更有
 * 「液体」感。仍然避开纯色块——大块高饱和色被折射后会变成脏色。
 *
 * 光斑是静态的：给整屏加无限动画会让低端机在列表滚动时掉帧，
 * 而滚动本身已经在改变「哪个颜色透过哪张卡片」，运动感并不缺。
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val hazeState = rememberHazeState()

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(modifier = modifier.fillMaxSize()) {

            // 采样源：基底渐变 + 四团光斑全部画在这一层里，
            // 玻璃面板采样到的就是「被光斑染色过的渐变」。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeSource(hazeState)
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
                    alpha = 0.38f,
                    alignment = Alignment.TopStart,
                    offsetX = (-100).dp,
                    offsetY = (-130).dp
                )

                // 右侧偏上：玫红，为「重要时刻」区提供暖调
                GlowOrb(
                    color = Glass.GlowRose,
                    size = 300.dp,
                    alpha = 0.30f,
                    alignment = Alignment.TopEnd,
                    offsetX = 80.dp,
                    offsetY = 40.dp
                )

                // 左下：青蓝，中和整体的暖色，避免一片橙红
                GlowOrb(
                    color = Glass.GlowCyan,
                    size = 320.dp,
                    alpha = 0.30f,
                    alignment = Alignment.BottomStart,
                    offsetX = (-80).dp,
                    offsetY = 100.dp
                )

                // 右下：紫罗兰，给底部导航与列表尾段一点深度
                GlowOrb(
                    color = Glass.GlowViolet,
                    size = 280.dp,
                    alpha = 0.28f,
                    alignment = Alignment.BottomEnd,
                    offsetX = 70.dp,
                    offsetY = 70.dp
                )
            }

            content()
        }
    }
}

/**
 * 单个光斑。
 *
 * 用「径向渐变到透明」而不是实心圆：`radialGradient` 的边缘本身就是
 * 平滑衰减的，被 Haze 采样后不会出现硬边。
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
