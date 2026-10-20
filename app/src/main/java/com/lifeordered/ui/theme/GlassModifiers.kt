package com.lifeordered.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
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
import dev.chrisbanes.haze.glass.SurfaceProfile
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

val glassStrokeBrush: Brush
    get() = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.08f)
        )
    )

/**
 * 完整玻璃面板：真折射玻璃 + 品牌色柔光。
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
    // 卡片和按钮统一取消外发光；玻璃边缘由 Haze 的折射与细描边表达，避免形成一圈阴影。
    if (hazeState == null) {
        return this.clip(shape).background(fill).border(strokeWidth, glassStrokeBrush, shape)
    }

    // 不再叠加 colorHalo，防止卡片外侧出现用户可见的暗色/彩色环。
    return this.clip(shape).hazeGlass(
        input = HazeInput.Backdrop(hazeState),
        style = rememberGlassStyle(shape = shape, fill = fill, glow = glow)
    )
}

/**
 * 玻璃的光学配置。
 *
 * ## 为什么用 [GlassStyle.clear] 而不是 `regular`
 *
 * Haze 的两个内置风格差别很关键（依据官方文档）：
 *
 * - `regular` 的 `refractionDetailIntensity` 是 **0**，只有主折射；
 * - `clear` 是 **0.76**，边缘有一圈二次折射细节，也就是 iOS 液态玻璃
 *   最有辨识度的「边缘光弧 / 环状折光」。
 *
 * 另外 `clear` 保留更多背景可见度，更像「玻璃」；`regular` 偏磨砂。
 * 本项目的背景是彩色光斑，需要透出来才好看，所以选 `clear`。
 *
 * ## 为什么要显式调这些参数
 *
 * 只写 `tint` / `shape` 时，其余参数全是默认值，其中三项直接决定「像不像
 * iOS 液态玻璃」：
 *
 * | 参数 | 默认值 | 本项目 | 作用 |
 * |---|---|---|---|
 * | `chromaticAberrationStrength` | **0** | 0.20 | 边缘棱镜色散，iOS 的标志性细节 |
 * | `specularIntensity` | 0.48 | 0.75 | 镜面高光强度 |
 * | `ambientResponse` | 0.15 | 0.50 | 菲涅尔边缘提亮 |
 *
 * 默认的 `chromaticAberrationStrength = 0` 意味着**完全没有色散**，
 * 玻璃会退化成一块磨砂白片——这是上一版「不像 iOS」的主要原因之一。
 *
 * `surfaceProfile(Squircle)` 让边缘截面接近超椭圆，与 iOS 的圆角观感一致
 * （默认是 `Circle`，在小圆角上会显得偏「圆管」）。
 */
 @Composable
 private fun rememberGlassStyle(
     shape: RoundedCornerShape,
     fill: Color,
     glow: Color
 ): GlassStyle = remember(shape, fill, glow) {
     GlassStyle.clear.then {
         tint(fill)
         shape(shape)
 
         // 边缘更「锐」，玻璃片才不会糊成一团雾
         edgeSoftness(0.5.dp)
 
         // 决定「像不像 iOS」的三项
         specularIntensity(0.75f)
         chromaticAberrationStrength(0.20f)
         ambientResponse(0.50f)
 
         surfaceProfile(SurfaceProfile.Squircle)
 
         // 光从上方来，高光落在顶边
         lightPosition(Alignment.TopCenter)
         // 显式覆盖库默认暗边，避免透明玻璃内部出现阴影圈。
         edgeShadow(Color.Transparent)
     }
 }
 
 /**
  * 浅色光晕：沿形状轮廓画几圈逐渐变淡的描边，替代黑色阴影。
 *
 * ## 为什么是「浅色」而不是「强调色」
 *
 * 最初直接用卡片主题色（高饱和的橙/粉/蓝）画光晕，结果每张卡片外面都套着
 * 一圈浓色光圈，整屏被强调色压住，主次不分——用户反馈「太强调色了」。
 *
 * 现在先把颜色**向白色混合 65%**、透明度再压到 55%，得到同一色相的
 * 极浅版本。色相还在（能区分不同条目），但不再是「发光」，只是
 * 「卡片把一点颜色洇到背景上」。
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

    // 向白色混合，得到同色相的浅色版本。
    // 0.35 的保留比例是「还能看出是橙/粉/蓝」与「不抢主体」之间的折中。
    val soft = Color(
        red = color.red * 0.35f + 0.65f,
        green = color.green * 0.35f + 0.65f,
        blue = color.blue * 0.35f + 0.65f,
        alpha = color.alpha * 0.55f
    )
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
            // 越往外越宽、越淡：靠宽度递增制造「衰减」，而不是靠模糊。
            // 0.10 是「能看见有一层洇色」的下限；再低就等于没画。
            val width = base * i / layers
            val alpha = (soft.alpha * 0.10f * (1f - (i - 1).toFloat() / layers)).coerceIn(0f, 1f)
            drawRoundRect(
                color = soft.copy(alpha = alpha),
                cornerRadius = CornerRadius(cornerPx + width / 2f),
                style = Stroke(width = width, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * 玻璃容器。
 *
 * ## 两种模式
 *
 * - `haze = false`（默认）：只画「半透明填充 + 渐变描边」，不采样背景。
 *   用于**嵌在其它玻璃内部**的次级容器、分隔线、输入框这类「结构件」。
 * - `haze = true`：走 [rememberGlassStyle] 的真折射玻璃。
 *   用于**按钮、Chip、图标按钮**这类独立控件，让它们也有玻璃质感。
 *
 * ## 为什么默认不开 Haze
 *
 * 一是开销：Haze 的玻璃是逐节点采样的着色器，34 处容器全开会让绘制成本成倍上升。
 * 二是分层：iOS 的液态玻璃也是分层的——大面板折射背景，面板里的小控件
 * 只需要薄薄一层材质，不必再折射一次。
 *
 * ## 关于嵌套
 *
 * Haze 2 的更新日志明确修过「nested Haze states」的采样，
 * 所以「玻璃里的玻璃」在库层面是受支持的，不会画错。
 * 但小控件嵌在大面板里时仍默认走 `haze = false`，
 * 避免两次折射把边框叠厚。
 *
 * @param haze 是否采样背景做真折射玻璃。只在 `LocalHazeState` 存在时生效。
 */
 @Composable
 fun Modifier.glassSurface(
     shape: Shape = Glass.fieldShape,
     fill: Color = Glass.FillSubtle,
     strokeWidth: Dp = 1.dp,
     haze: Boolean = false
 ): Modifier {
     // ## 为什么小控件一律不走向色器（haze 参数已停用）
     //
     // 上一版允许调用方传 haze = true 让 Chip / 图标按钮 / 底栏胶囊采样背景，
     // 结果有两个副作用：
     // 1. **性能**：着色器是逐节点采样的，全屏几十个小控件全开，
     //    列表滚动时 GPU 开销成倍增长，页面明显卡顿；
     // 2. **阴影圈**：着色器的镜面高光 / 边缘暗影落在 30dp 级别的胶囊上，
     //    会挤压成一圈肉眼可见的暗环，怎么调参数都去不干净。
     //
     // 所以现在统一退回「半透明填充 + 细描边」：零着色器开销，
     // 且描边是纯白渐变，天然不存在暗色阴影。haze 参数保留只为兼容旧调用点。
     return this
         .clip(shape)
         .background(fill)
         .border(strokeWidth, glassStrokeBrush, shape)
 }

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

/**
 * 药丸形状的玻璃，用于 Chip / 标签。
 *
 * @param haze 透传给 [glassSurface]；Chip 这类独立控件应传 `true` 以获得真折射。
 */
@Composable
fun Modifier.glassPill(
    fill: Color = Glass.FillSubtle,
    strokeWidth: Dp = 1.dp,
    haze: Boolean = false
): Modifier = glassSurface(
    shape = RoundedCornerShape(Glass.Radius.pill),
    fill = fill,
    strokeWidth = strokeWidth,
    haze = haze
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
