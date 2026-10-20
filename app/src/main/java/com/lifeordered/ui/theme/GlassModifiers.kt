package com.lifeordered.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.StrokeJoin
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
 * 3. **有厚度**——由柔光托起，且柔光带自身的色调
 *
 * ## 为什么彻底删掉了 `Modifier.shadow()`
 *
 * 上一版第 3 点用 `shadow(ambientColor = Black, spotColor = Black)` 实现，
 * 结果每张卡片后面都挂着一块**硬边灰黑方块**。原因有两条：
 *
 * - `ambientColor` / `spotColor` 只在 **API 28+** 生效；`minSdk = 24`，
 *   28 以下的设备一律回落成纯黑阴影，且是矩形轮廓（因为 Compose 的
 *   阴影层不跟随 `RoundedCornerShape` 的圆角在低版本上重绘）；
 * - 阴影的模糊半径随 elevation 线性增长，卡片越多越脏，在浅色底上尤其明显。
 *
 * 现在改用 [colorHalo]：沿形状轮廓画几圈逐渐变淡的同色调描边。
 * 它**没有矩形轮廓**（描边跟随 shape），**没有黑色**（颜色来自卡片主题色），
 * 并且所有 API 24 设备表现一致。
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
 * 完整玻璃面板：柔光 + 半透明填充 + 高光边缘。
 *
 * @param elevation 柔光扩散半径。参数名保留是为了不改动既有调用点，
 *        但它的语义已经从「阴影高度」变成「外发光半径」——不再画任何黑色阴影。
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
    .colorHalo(color = glow, shape = shape, spread = elevation)
    .clip(shape)
    .background(fill)
    .border(strokeWidth, glassStrokeBrush, shape)

/**
 * 同色调柔光：沿形状轮廓画几圈逐渐变淡的描边，替代黑色阴影。
 *
 * 描边是**居中**画在轮廓线上的，所以最外圈会向外溢出 [spread] 的一半，
 * 形成一圈没有硬边的光晕。颜色透明时直接跳过，不产生任何绘制开销。
 */
fun Modifier.colorHalo(
    color: Color,
    shape: Shape = Glass.controlShape,
    spread: Dp = 8.dp,
    layers: Int = 4
): Modifier {
    if (color.alpha <= 0.02f) return this
    return this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val base = spread.toPx()
        for (i in 1..layers) {
            // 越往外越宽、越淡：靠宽度递增制造「衰减」，而不是靠模糊
            val width = base * i / layers
            val alpha = color.alpha * 0.18f * (1f - (i - 1).toFloat() / layers)
            drawOutline(
                outline = outline,
                color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                style = Stroke(width = width, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * 无阴影玻璃：用于嵌在其它玻璃内部的次级容器。
 *
 * 嵌套时如果也用 [liquidGlass]，两层描边叠加会让边框变粗、界面发脏，
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
 * 浅色主题下这条线要更亮才看得见（白色叠白色），所以透明度提到 0.55。
 */
fun Modifier.glassTopHighlight(shape: Shape): Modifier = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
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

/**
 * 强调色辉光：用于当前选中项，让它在玻璃层上「浮起来」。
 *
 * 与 [liquidGlass] 的 `glow` 共用同一套 [colorHalo] 实现——
 * 上一版这里也是 `shadow(...)`，同样会在 API 28 以下变成灰黑方块。
 */
fun Modifier.accentGlow(
    color: Color,
    shape: Shape = Glass.controlShape,
    elevation: Dp = 8.dp
): Modifier = colorHalo(color = color, shape = shape, spread = elevation)
