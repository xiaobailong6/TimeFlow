package com.lifeordered.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃（Liquid Glass）设计令牌。
 *
 * ## 设计取向
 *
 * 这是一个记录「时间流逝」的私密工具：纪念日、耗材寿命、库存余量。
 * 视觉上刻意避开常见的「暖奶油底 + 陶土橙卡片」配色——那套组合读起来
 * 像任何一个生成式模板。这里改用**深墨蓝的液体基底 + 悬浮的半透明玻璃层**：
 * 时间被比作流动的暗夜，而记录本身是浮在上面的透明薄片。
 *
 * ## 实现约束
 *
 * `minSdk = 24`，而 `Modifier.blur()` / `RenderEffect` 需要 API 31。
 * 因此这里**不依赖真实的背景模糊**：玻璃的柔和感来自径向渐变的天然羽化，
 * 以及「渐变高光边框 + 内阴影」模拟出的折射。所有 API 24 设备都能呈现。
 */
object Glass {

    // ---------------------------------------------------------------------
    // 基底：深墨蓝，而非纯黑——保留一点蓝调，避免 OLED 上的死黑断层
    // ---------------------------------------------------------------------

    val BaseTop = Color(0xFF0B1020)
    val BaseBottom = Color(0xFF05070F)

    // ---------------------------------------------------------------------
    // 液体光斑：玻璃之所以「液态」，是因为背后有缓慢流动的彩色光
    // ---------------------------------------------------------------------

    val GlowAmber = Color(0xFFFF8A5C)
    val GlowRose = Color(0xFFFF5E9C)
    val GlowCyan = Color(0xFF4EC9E8)
    val GlowViolet = Color(0xFF8B6FE8)

    // ---------------------------------------------------------------------
    // 玻璃填充：三档强度，用于建立层级（不是所有面板都用同一个透明度）
    // ---------------------------------------------------------------------

    /** 最弱：贴在背景上的分组容器。 */
    val FillSubtle = Color(0x0AFFFFFF)

    /** 常规：卡片、面板。 */
    val Fill = Color(0x14FFFFFF)

    /** 强调：浮起的弹层、被选中的项。 */
    val FillStrong = Color(0x1FFFFFFF)

    /** 最强调：模态底部面板。 */
    val FillHeavy = Color(0x29FFFFFF)

    // ---------------------------------------------------------------------
    // 玻璃边缘：上亮下暗的渐变，模拟玻璃厚度的折射高光
    // ---------------------------------------------------------------------

    val StrokeTop = Color(0x59FFFFFF)
    val StrokeMid = Color(0x1AFFFFFF)
    val StrokeBottom = Color(0x0DFFFFFF)

    // ---------------------------------------------------------------------
    // 文字：三级层级，足够对比度但不刺眼
    // ---------------------------------------------------------------------

    val TextPrimary = Color(0xFFF2F4FA)
    val TextSecondary = Color(0xFF9AA3B8)
    val TextTertiary = Color(0xFF6B7488)

    // ---------------------------------------------------------------------
    // 强调色：暖橙作为品牌连续性，其余用于语义
    // ---------------------------------------------------------------------

    val Accent = Color(0xFFFFA05C)
    val AccentCool = Color(0xFF5CC8FF)
    val AccentDanger = Color(0xFFFF6B6B)
    val AccentSuccess = Color(0xFF5ED9A0)
    val AccentWarning = Color(0xFFFFC15C)

    /**
     * 强调色之上的前景色。
     *
     * 亮橙 / 亮青这类高饱和填充上必须配深色文字才有对比度，
     * 之前这个值被散落硬编码成 `Color(0xFF1A1008)` 十余次，
     * 一旦调整品牌色就会漏改某几处，于是收敛成一个令牌。
     */
    val OnAccent = Color(0xFF1A1008)

    /** 冷强调色（青）之上的前景色，与 [OnAccent] 同一族。 */
    val OnAccentCool = Color(0xFF04141C)

    /** 成功色之上的前景色。 */
    val OnAccentSuccess = Color(0xFF04170F)

    /** 危险色之上的前景色。 */
    val OnAccentDanger = Color(0xFF1C0505)

    /**
     * 模态遮罩。
     *
     * 不用纯黑：纯黑压在深墨蓝基底上会让整屏掉进死黑，底层的液体光斑完全消失，
     * 玻璃的「浮起来」感随之垮掉。带一点点蓝，遮罩下的光斑仍然隐约可见。
     * 主题色板与三个底部面板共用同一个值，避免遮罩深浅不一。
     */
    val Scrim = Color(0xCC04060D)

    /**
     * 模态面板底色。
     *
     * 比常规玻璃更实（约 91% 不透明）：面板里是表单，用户要读字、要对齐数字，
     * 背后光斑透上来只会让输入框边界变模糊。但仍然不是纯色——
     * 留 9% 让底层的色调渗进来，面板才像是「浮在液体上」而不是「贴在上面」。
     */
    val SheetFill = Color(0xE80E1424)
    // ---------------------------------------------------------------------
    // 圆角分级：不再一刀切。面板 > 卡片 > 控件 > 胶囊
    // ---------------------------------------------------------------------

    object Radius {
        /** 全屏面板 / 底部面板顶部圆角。 */
        val panel = 28.dp

        /** 常规卡片。 */
        val card = 20.dp

        /** 输入框、内联容器。 */
        val field = 16.dp

        /** Chip、小按钮。 */
        val control = 14.dp

        /** 胶囊。 */
        val pill = 100.dp
    }

    /** 常用形状，避免每个调用点重复构造。 */
    val panelShape = RoundedCornerShape(Radius.panel)
    val cardShape = RoundedCornerShape(Radius.card)
    val fieldShape = RoundedCornerShape(Radius.field)
    val controlShape = RoundedCornerShape(Radius.control)
    val pillShape = RoundedCornerShape(Radius.pill)

    /** 底部面板：只圆上两个角。 */
    val sheetShape = RoundedCornerShape(topStart = Radius.panel, topEnd = Radius.panel)
}

/** 把数据里存的主题色转成 Compose 颜色；失败时回落到品牌强调色。 */
fun parseHexColor(hexString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexString))
    } catch (_: Exception) {
        Glass.Accent
    }
}
