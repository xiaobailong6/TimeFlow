package com.lifeordered.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lifeordered.ui.theme.Glass

/**
 * 液态玻璃图标。
 *
 * ## 为什么重写
 *
 * 上一版这里是「黏土拟物」图标（原 ClayIcons.kt，已删除）：浅粉、浅蓝、奶白的
 * 实心形状配大圆角阴影。那套视觉成立的前提是**浅色底**——黏土色本身就很浅，
 * 靠比底色更亮来凸显。
 *
 * 换成深墨蓝液体基底后，同样几个图标变成了刺眼的浅色贴纸：它们不透明，
 * 所以完全遮住背后的彩色光斑，恰好破坏了玻璃最重要的特征（能透出背后）。
 *
 * 现在的画法遵循和面板一致的规则：
 *
 * 1. **填充半透明**——用白色 / 主题色的低透明度渐变，让卡片底色透上来
 * 2. **描边上亮下暗**——和 [glassStrokeBrush] 同一套光向
 * 3. **背后有一圈自身色调的柔光**——这是液态玻璃里「图标在发光」的来源，
 *    替代黏土时代的大圆形底衬
 *
 * 图标尺寸不变（默认 64dp），调用点只换了函数名。
 */

// -----------------------------------------------------------------------------
// 绘制基础：与 GlassModifiers 的面板画法保持同一套光线逻辑
// -----------------------------------------------------------------------------

/**
 * 玻璃填充：白色低透明度垂直渐变。
 *
 * 不用实色——实色会把卡片背面的光斑挡死。上亮下暗是因为光从上方来，
 * 与面板描边的方向一致，否则图标会像是从下方打光的，视觉上「粘不住」。
 *
 * 只保留一档亮度：图标内的层次靠「谁盖在谁上面」建立，
 * 不再额外叠第二档白色，否则 44dp 的小尺寸里会糊成一片。
 */
/** 白色玻璃填充，用于需要「浮起来」的部件（盘子、奶油层）。 */
private val whiteGlassFill: Brush
    get() = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.62f),
            Color.White.copy(alpha = 0.26f)
        )
    )

/**
 * 带色调的玻璃填充。
 *
 * 语义靠色调区分（生日=暖橙、礼物=青蓝、心动=玫红、庆祝=暖黄），
 * 但透明度压到 0.5 以下，保证仍然是「有颜色的玻璃」而不是「有颜色的塑料」。
 */
private fun tintedGlassFill(tint: Color, strong: Boolean = false): Brush =
    Brush.verticalGradient(
        colors = listOf(
            tint.copy(alpha = if (strong) 0.68f else 0.46f),
            tint.copy(alpha = if (strong) 0.34f else 0.18f)
        )
    )

/** 边缘高光：上亮下暗，与面板描边同向。 */
private fun glassEdge(
    topLeft: Offset,
    size: Size
): Brush = Brush.verticalGradient(
    colors = listOf(
        Glass.StrokeTop,
        Glass.StrokeMid,
        Glass.StrokeBottom
    ),
    startY = topLeft.y,
    endY = topLeft.y + size.height
)

/** 图标背后的柔光，替代黏土时代的实心圆形底衬。 */
private fun DrawScope.iconGlow(tint: Color, radius: Float) {
    val center = Offset(size.width * 0.5f, size.height * 0.52f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(tint.copy(alpha = 0.26f), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/** 一块玻璃圆角矩形：半透明填充 + 渐变描边。 */
private fun DrawScope.glassRoundRect(
    topLeft: Offset,
    size: Size,
    corner: Float,
    fill: Brush,
    strokeWidth: Float
) {
    val radius = CornerRadius(corner, corner)
    drawRoundRect(brush = fill, topLeft = topLeft, size = size, cornerRadius = radius)
    drawRoundRect(
        brush = glassEdge(topLeft, size),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
        style = Stroke(width = strokeWidth)
    )
}

// -----------------------------------------------------------------------------
// 图标
// -----------------------------------------------------------------------------

/**
 * 蛋糕：生日 / 纪念日。
 *
 * 层序自下而上：盘子 → 蛋糕体 → 奶油 → 蜡烛 → 火苗。
 * 火苗是全图唯一的高饱和发光点，用来在列表里一眼抓住注意力。
 */
@Composable
fun GlassCakeIcon(modifier: Modifier = Modifier.size(64.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.035f

        iconGlow(Glass.Accent, w * 0.52f)

        // 盘子
        glassRoundRect(
            topLeft = Offset(w * 0.12f, h * 0.76f),
            size = Size(w * 0.76f, h * 0.10f),
            corner = h * 0.05f,
            fill = whiteGlassFill,
            strokeWidth = stroke
        )

        // 蛋糕体：暖橙玻璃
        glassRoundRect(
            topLeft = Offset(w * 0.22f, h * 0.50f),
            size = Size(w * 0.56f, h * 0.27f),
            corner = w * 0.07f,
            fill = tintedGlassFill(Glass.Accent),
            strokeWidth = stroke
        )

        // 奶油层：比蛋糕体更亮，压在顶部
        glassRoundRect(
            topLeft = Offset(w * 0.22f, h * 0.44f),
            size = Size(w * 0.56f, h * 0.11f),
            corner = w * 0.05f,
            fill = whiteGlassFill,
            strokeWidth = stroke
        )

        // 蜡烛
        glassRoundRect(
            topLeft = Offset(w * 0.472f, h * 0.27f),
            size = Size(w * 0.056f, h * 0.18f),
            corner = w * 0.025f,
            fill = tintedGlassFill(Glass.AccentCool, strong = true),
            strokeWidth = stroke * 0.8f
        )

        // 火苗：径向发光
        val flameCenter = Offset(w * 0.5f, h * 0.22f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Glass.AccentWarning.copy(alpha = 0.9f)),
                center = flameCenter,
                radius = w * 0.055f
            ),
            radius = w * 0.055f,
            center = flameCenter
        )
    }
}

/**
 * 礼物：收礼 / 送礼。
 *
 * 丝带用暖橙与盒身的青蓝形成对比，十字交叉是最省笔画又能立刻读懂的形状。
 */
@Composable
fun GlassGiftIcon(modifier: Modifier = Modifier.size(64.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.035f

        iconGlow(Glass.AccentCool, w * 0.52f)

        // 盒身
        glassRoundRect(
            topLeft = Offset(w * 0.24f, h * 0.42f),
            size = Size(w * 0.52f, h * 0.42f),
            corner = w * 0.06f,
            fill = tintedGlassFill(Glass.AccentCool),
            strokeWidth = stroke
        )

        // 盖子：略宽，更亮
        glassRoundRect(
            topLeft = Offset(w * 0.18f, h * 0.33f),
            size = Size(w * 0.64f, h * 0.12f),
            corner = w * 0.04f,
            fill = tintedGlassFill(Glass.AccentCool, strong = true),
            strokeWidth = stroke
        )

        // 竖丝带
        glassRoundRect(
            topLeft = Offset(w * 0.465f, h * 0.33f),
            size = Size(w * 0.07f, h * 0.51f),
            corner = w * 0.02f,
            fill = tintedGlassFill(Glass.Accent, strong = true),
            strokeWidth = stroke * 0.7f
        )

        // 横丝带
        glassRoundRect(
            topLeft = Offset(w * 0.24f, h * 0.56f),
            size = Size(w * 0.52f, h * 0.07f),
            corner = w * 0.02f,
            fill = tintedGlassFill(Glass.Accent, strong = true),
            strokeWidth = stroke * 0.7f
        )

        // 蝴蝶结：两个圆环，留空比实心更像玻璃
        drawCircle(
            color = Glass.Accent.copy(alpha = 0.85f),
            radius = w * 0.075f,
            center = Offset(w * 0.40f, h * 0.26f),
            style = Stroke(width = w * 0.035f)
        )
        drawCircle(
            color = Glass.Accent.copy(alpha = 0.85f),
            radius = w * 0.075f,
            center = Offset(w * 0.60f, h * 0.26f),
            style = Stroke(width = w * 0.035f)
        )
    }
}

/**
 * 爱心：恋爱 / 心动 / 陪伴。
 *
 * 一大两小，避开居中的完美对称——三个心错开摆放，视觉上更松弛。
 */
@Composable
fun GlassHeartsIcon(modifier: Modifier = Modifier.size(64.dp)) {
    val bigPath = remember { Path() }
    val smallLeft = remember { Path() }
    val smallRight = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.032f

        iconGlow(Glass.GlowRose, w * 0.52f)

        drawGlassHeart(
            path = bigPath,
            cx = w * 0.5f,
            cy = h * 0.54f,
            extent = w * 0.40f,
            fill = tintedGlassFill(Glass.GlowRose),
            strokeWidth = stroke
        )

        drawGlassHeart(
            path = smallLeft,
            cx = w * 0.30f,
            cy = h * 0.34f,
            extent = w * 0.19f,
            fill = tintedGlassFill(Glass.AccentDanger, strong = true),
            strokeWidth = stroke * 0.8f
        )

        drawGlassHeart(
            path = smallRight,
            cx = w * 0.70f,
            cy = h * 0.70f,
            extent = w * 0.19f,
            fill = tintedGlassFill(Glass.AccentDanger, strong = true),
            strokeWidth = stroke * 0.8f
        )
    }
}

/** 用两条三次贝塞尔画出心形，再叠一层描边。 */
private fun DrawScope.drawGlassHeart(
    path: Path,
    cx: Float,
    cy: Float,
    extent: Float,
    fill: Brush,
    strokeWidth: Float
) {
    path.reset()
    path.moveTo(cx, cy - extent * 0.25f)
    path.cubicTo(
        cx - extent * 0.5f, cy - extent * 0.65f,
        cx - extent, cy - extent * 0.1f,
        cx, cy + extent * 0.55f
    )
    path.cubicTo(
        cx + extent, cy - extent * 0.1f,
        cx + extent * 0.5f, cy - extent * 0.65f,
        cx, cy - extent * 0.25f
    )
    path.close()

    drawPath(path = path, brush = fill)
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(Glass.StrokeTop, Glass.StrokeBottom),
            startY = cy - extent,
            endY = cy + extent
        ),
        style = Stroke(width = strokeWidth)
    )
}

/**
 * 派对帽：庆祝 / 节日 / 聚会。
 *
 * 帽身是唯一的暖黄大色块，顶球用玫红做点缀——两者对比足够强，
 * 缩到 32dp 的归档列表里也能分辨。
 */
@Composable
fun GlassPartyIcon(modifier: Modifier = Modifier.size(64.dp)) {
    val hat = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.035f

        iconGlow(Glass.AccentWarning, w * 0.52f)

        hat.reset()
        hat.moveTo(w * 0.5f, h * 0.22f)
        hat.lineTo(w * 0.20f, h * 0.80f)
        hat.lineTo(w * 0.80f, h * 0.80f)
        hat.close()

        drawPath(path = hat, brush = tintedGlassFill(Glass.AccentWarning))
        drawPath(
            path = hat,
            brush = Brush.verticalGradient(
                colors = listOf(Glass.StrokeTop, Glass.StrokeBottom),
                startY = h * 0.22f,
                endY = h * 0.80f
            ),
            style = Stroke(width = stroke)
        )

        // 帽身条纹：半透明白，顺着三角形的宽度递增
        drawRect(
            color = Color.White.copy(alpha = 0.24f),
            topLeft = Offset(w * 0.35f, h * 0.50f),
            size = Size(w * 0.30f, h * 0.05f)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.24f),
            topLeft = Offset(w * 0.29f, h * 0.66f),
            size = Size(w * 0.42f, h * 0.05f)
        )

        // 顶球
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Glass.GlowRose),
                center = Offset(w * 0.5f, h * 0.20f),
                radius = w * 0.06f
            ),
            radius = w * 0.06f,
            center = Offset(w * 0.5f, h * 0.20f)
        )
    }
}
