package com.lifeordered.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃的 Modifier 实现。
 *
 * ## 玻璃是怎么「做」出来的
 *
 * 真实玻璃有三个可被眼睛识别的特征，缺一个就会变成「半透明灰块」：
 *
 * 1. **填充是半透明的**——能透出背后的彩色光斑（[Glass.Fill] 系列）
 * 2. **边缘有折射高光**——上边缘亮、下边缘暗，因为光从上方来
 *    （[Glass.StrokeTop] → [Glass.StrokeBottom] 的垂直渐变）
 * 3. **有厚度**——由外阴影托起，且阴影带一点自身的色调
 *
 * 这三件事分别在 [liquidGlass] 里由 `background` / `border(Brush)` / `shadow` 完成。
 *
 * ## 为什么不 blur
 *
 * `Modifier.blur()` 与 `RenderEffect` 都要 API 31，而本项目 `minSdk = 24`。
 * 用 `if (SDK_INT >= 31) blur(...)` 会导致新老设备观感分裂，比不模糊更糟。
 * 因此统一不做模糊，靠渐变 + 高光获得玻璃感——所有设备一致。
 */

/** 玻璃边缘高光：上亮下暗，模拟光线从上方射入玻璃厚度。 */
val glassStrokeBrush: Brush
    get() = Brush.verticalGradient(
        colors = listOf(Glass.StrokeTop, Glass.StrokeMid, Glass.StrokeBottom)
    )

/**
 * 完整玻璃面板：阴影 + 半透明填充 + 高光边缘。
 *
 * @param glow 自身色调的辉光。传卡片主题色可以让卡片「发光」，
 *        这是液态玻璃里区分不同数据条目的主要手段（而不是换底色）。
 */
fun Modifier.liquidGlass(
    shape: Shape = Glass.cardShape,
    fill: Color = Glass.Fill,
    elevation: Dp = 10.dp,
    strokeWidth: Dp = 1.dp,
    glow: Color = Color.Transparent
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.45f),
        spotColor = if (glow == Color.Transparent) {
            Color.Black.copy(alpha = 0.5f)
        } else {
            glow.copy(alpha = 0.35f)
        }
    )
    .clip(shape)
    .background(fill)
    .border(strokeWidth, glassStrokeBrush, shape)

/**
 * 无阴影玻璃：用于嵌在其它玻璃内部的次级容器。
 *
 * 嵌套时如果也用 [liquidGlass]，两层阴影叠加会让界面发脏，
 * 所以内层只保留填充与描边。
 */
fun Modifier.glassSurface(
    shape: Shape = Glass.fieldShape,
    fill: Color = Glass.FillSubtle,
    strokeWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(fill)
    .border(strokeWidth, glassStrokeBrush, shape)

/** 只有描边的玻璃轮廓，用于分隔线之外的「可见边界」。 */
fun Modifier.glassOutline(
    shape: Shape = Glass.controlShape,
    strokeWidth: Dp = 1.dp
): Modifier = this.border(strokeWidth, glassStrokeBrush, shape)

/**
 * 顶部内高光。
 *
 * 在玻璃面板顶部画一道极细的白色渐变，模拟玻璃表面最亮的那一条反射线。
 * 用 `drawBehind` 实现，不额外增加布局层级。
 */
fun Modifier.glassTopHighlight(shape: Shape): Modifier = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
        endY = 40f
    ),
    shape = shape
)

/** 药丸形状的玻璃，用于 Chip / 标签。 */
fun Modifier.glassPill(
    fill: Color = Glass.FillSubtle,
    strokeWidth: Dp = 1.dp
): Modifier = glassSurface(
    shape = RoundedCornerShape(Glass.Radius.pill),
    fill = fill,
    strokeWidth = strokeWidth
)

/** 强调色辉光：用于当前选中项，让它在玻璃层上「浮起来」。 */
fun Modifier.accentGlow(
    color: Color,
    shape: Shape = Glass.controlShape,
    elevation: Dp = 8.dp
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = color.copy(alpha = 0.5f),
    spotColor = color.copy(alpha = 0.6f)
)
