package com.lifeordered.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/**
 * 液态玻璃的 Modifier 实现。
 *
 * ## 分工：Haze 画玻璃，本文件画「装饰」
 *
 * 真正让玻璃成立的三件事——**背景模糊、边缘折射、镜面高光**——现在由
 * Haze 的 `haze-glass` 负责（`Modifier.hazeGlass`）。本文件只保留两类东西：
 *
 * 1. [liquidGlass]：把 Haze 玻璃 + 品牌色柔光组合成一个大面板；
 * 2. [glassSurface] 一族：**不走 Haze** 的轻量容器，给嵌在玻璃内部的
 *    小控件（Chip、步进器、图标按钮）用。
 *
 * ## 为什么内层控件不也用 Haze
 *
 * 全仓有 41 处 `glassSurface`。Haze 的玻璃是逐节点采样的着色器，
 * 每个小控件都开一层会带来成倍的绘制开销，而且**视觉上是错的**：
 * 玻璃里的玻璃会把折射叠加两次，边框变粗、界面发脏。
 * 所以只有「大面积、真正浮在背景上」的面板才用 [liquidGlass]。
 *
 * ## 光晕为什么保留
 *
 * [colorHalo] 用几圈递减透明度的同色调描边模拟外发光，替代了
 * `Modifier.shadow()`（后者在 API 28 以下只能画纯黑矩形，就是用户看到的
 * 「灰黑方块」）。Haze 的 `edgeShadow` 只作用于玻璃自身的边缘，
 * 不负责「卡片把颜色洒到背景上」这件事，两者不重复。
 */

/**
 * 当前作用域的 Haze 状态。
 *
 * 由 [LiquidGlassBackground] 提供；为 `null` 时说明该子树没有可采样的背景，
 * 此时 [liquidGlass] 退化成「半透明填充 + 渐变描边」，仍然能正常渲染
 * （Roborazzi 截图测试、预览都依赖这个兜底）。
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** 玻璃边缘高光：上亮下暗，模拟光线从上方射入玻璃厚度。 */
val glassStrokeBrush: Brush
    get() = Brush.verticalGradient(
        colors = listOf(Glass.StrokeTop, Glass.StrokeMid, Glass.StrokeBottom)
    )

/**
 * 完整玻璃面板：真折射玻璃 + 品牌色柔光。
 *
 * @param elevation 柔光扩散半径。参数名保留是为了不改动既有调用点，
 *        但它的语义是「外发光半径」，不画任何黑色阴影。
 * @param glow 自身色调的辉光。传卡片主题色可以让卡片「发光」，
 *        这是液态玻璃里区分不同数据条目的主要手段（而不是换底色）。
 */
@Composable
fun Modifier.liquidGlass(
    shape: RoundedCornerShape = Glass.cardShape,
    fill: Color = Glass.Fill,
    elevation: Dp = 10.dp,
    strokeWidth: Dp = 1.dp,
    glow: Color = Color.Transparent
): Modifier {
    val hazeState = LocalHazeState.current
    val halo = colorHalo(color = glow, shape = shape, spread = elevation)

    // 没有可采样的背景时退回「手搓玻璃」。
    // 这不是降级妥协，而是必要的：Haze 采样不到源时会画出空材质。
    if (hazeState == null) {
        return halo
            .clip(shape)
            .background(fill)
            .border(strokeWidth, glassStrokeBrush, shape)
    }

    // Style 是不可变的、可复用的配置，构造一次即可；
    // 参数变化时重建，而不是原地改（Haze 的 Style 捕获值后不会跟随变更）。
    val style = remember(shape, fill, glow) {
        GlassStyle.regular.then {
            tint(fill)
            shape(shape)
            edgeSoftness(1.dp)
            // 有品牌色时把边缘暗影染成同色调，让卡片「发光」而不是「压黑」
            if (glow.alpha > 0.02f) {
                edgeShadow(glow.copy(alpha = glow.alpha * 0.45f))
            }
        }
    }

    return halo.hazeGlass(
        input = HazeInput.Backdrop(hazeState),
        style = style
    )
}

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
        val base = spread.toPx()

        // 光晕沿「形状的圆角」扩散。
        //
        // 这里刻意不调用 `shape.createOutline(...)` 再去读 `Outline.Rounded` 的成员：
        // `roundRect.topLeft` / `roundRect.size` 在不同 Compose 版本里的可见性不稳定，
        // 已经让 CI 连续失败两次。改用 DrawScope 自己的 `drawRoundRect`——
        // 它的 `topLeft` / `size` 都有默认值（Offset.Zero 与当前节点尺寸），
        // 只要给 `cornerRadius` 就足以描述轮廓，不需要碰 Outline。
        //
        // 本项目的 Glass 形状全部是 RoundedCornerShape 或 CircleShape
        // （后者本身就是 RoundedCornerShape(50)），所以下面这个 cast 覆盖全部调用点。
        val cornerPx = (shape as? RoundedCornerShape)?.topStart?.toPx(size, this) ?: 0f

        for (i in 1..layers) {
            // 越往外越宽、越淡：靠宽度递增制造「衰减」，而不是靠模糊
            val width = base * i / layers
            val alpha = (color.alpha * 0.18f * (1f - (i - 1).toFloat() / layers)).coerceIn(0f, 1f)
            drawRoundRect(
                color = color.copy(alpha = alpha),
                // 描边是居中画在轮廓线上的，半径要加半个线宽，外圈才不会露出方角
                cornerRadius = CornerRadius(cornerPx + width / 2f),
                style = Stroke(width = width, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * 轻量玻璃：用于嵌在其它玻璃内部的次级容器。
 *
 * 刻意**不接 Haze**：嵌套折射会让边框变粗、界面发脏，而且每个小控件
 * 各开一层着色器会成倍放大绘制开销。这里只保留填充与渐变描边。
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
