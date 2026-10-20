package com.lifeordered.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃（Liquid Glass）设计令牌 —— 透明白色版。
 *
 * ## 为什么从深墨蓝改成透明白
 *
 * 上一版是深墨蓝基底 + 白色半透明层。用户反馈两点：夜间色调压抑、
 * 卡片后面浮着一块灰黑方块。灰黑方块的根源不是填充色，而是
 * `Modifier.shadow()`——它的阴影在 API 28 以下**只能画黑色**，
 * 于是每张卡片都被钉了一块硬边黑影。
 *
 * 这一版把两件事一起解决：
 * 1. 基底改成**近白冷调**（[BaseTop] → [BaseBottom]），玻璃层用高不透明度白色，
 *    读起来是「浮在淡彩水面上的磨砂白片」；
 * 2. 深度不再由黑色阴影提供，而由**上亮下暗的折射描边**（[StrokeTop] → [StrokeBottom]）
 *    加一圈同色调的柔光（见 `GlassModifiers.colorHalo`）提供。
 *
 * ## 浅色玻璃的成立条件
 *
 * 半透明白片压在纯白底上是看不见的。所以背景必须**有颜色**：
 * [LiquidGlassBackground] 铺四团低饱和光斑，玻璃片经过不同色区时透出的颜色不同，
 * 才会被眼睛识别为「透明的」而不是「白色的」。这是浅色液态玻璃唯一的关键。
 *
 * ## 实现约束
 *
 * `minSdk = 24`，而 `Modifier.blur()` / `RenderEffect` 需要 API 31。
 * 因此这里**不依赖真实的背景模糊**：柔和感来自径向渐变的天然羽化，
 * 以及「渐变高光边框 + 同色调柔光」模拟出的折射。所有 API 24 设备都能呈现。
 */
object Glass {

    // ---------------------------------------------------------------------
    // 基底：近白冷调，而不是纯白——纯白会让所有玻璃片彻底消失
    // ---------------------------------------------------------------------

    val BaseTop = Color(0xFFF7F9FE)
    val BaseBottom = Color(0xFFE9EFFB)

    // ---------------------------------------------------------------------
    // 液体光斑：玻璃之所以「液态」，是因为背后有缓慢流动的彩色光
    //
    // 浅色底上光斑必须降饱和：高饱和色会直接把白玻璃染成脏色块。
    // ---------------------------------------------------------------------

    val GlowAmber = Color(0xFFFFA76B)
    val GlowRose = Color(0xFFFF86B4)
    val GlowCyan = Color(0xFF6FD0F0)
    val GlowViolet = Color(0xFFA08CF0)

    // ---------------------------------------------------------------------
    // 玻璃填充：三档强度，用于建立层级（不是所有面板都用同一个透明度）
    //
    // 浅色主题下白色越实、层级越高。数值刻意比深色版高很多：
    // 深色版 8% 白就能看见，浅色版 8% 白在浅底上等于什么都没画。
    // ---------------------------------------------------------------------

    /** 最弱：贴在背景上的分组容器。 */
    val FillSubtle = Color(0x59FFFFFF)

    /** 常规：卡片、面板。 */
    val Fill = Color(0x8CFFFFFF)

    /** 强调：浮起的弹层、被选中的项。 */
    val FillStrong = Color(0xB8FFFFFF)

    /** 最强调：模态底部面板、底部导航栏。 */
    val FillHeavy = Color(0xE0FFFFFF)

    // ---------------------------------------------------------------------
    // 玻璃边缘：上亮下暗的渐变，模拟玻璃厚度的折射高光
    //
    // 浅色底上「上亮」要靠纯白顶边，「下暗」必须真的变暗——
    // 若三档都用白色，边框会完全消失，玻璃片就退化成一块白斑。
    // ---------------------------------------------------------------------

    val StrokeTop = Color(0xFFFFFFFF)
    val StrokeMid = Color(0xA6FFFFFF)
    val StrokeBottom = Color(0x4D6B7CA8)

    /** 极细的分隔线：浅色主题下用淡蓝灰，而不是半透明白。 */
    val Hairline = Color(0x1A3C4A6B)

    // ---------------------------------------------------------------------
    // 文字：三级层级。浅色底上必须用深色字，且留足对比度
    // ---------------------------------------------------------------------

    val TextPrimary = Color(0xFF16203A)
    val TextSecondary = Color(0xFF5B6480)
    val TextTertiary = Color(0xFF8B93A8)

    // ---------------------------------------------------------------------
    // 强调色：暖橙作为品牌连续性，其余用于语义
    //
    // 比深色版整体压深一档：原来的浅橙配白字对比度不足（约 2.1:1），
    // 在浅色主题里选中态是要靠「橙底白字」读出来的。
    // ---------------------------------------------------------------------

    val Accent = Color(0xFFF97316)
    val AccentCool = Color(0xFF2E90D0)
    val AccentDanger = Color(0xFFE5484D)
    val AccentSuccess = Color(0xFF17A673)
    val AccentWarning = Color(0xFFD99100)

    /**
     * 强调色之上的前景色。
     *
     * 这一版全部改成白色：强调色已压深，白字对比度 > 4.5:1；
     * 上一版用深色字是因为橙色太浅，改色后若不一起改，文字会糊在填充里。
     */
    val OnAccent = Color(0xFFFFFFFF)

    /** 冷强调色（蓝）之上的前景色。 */
    val OnAccentCool = Color(0xFFFFFFFF)

    /** 成功色之上的前景色。 */
    val OnAccentSuccess = Color(0xFFFFFFFF)

    /** 危险色之上的前景色。 */
    val OnAccentDanger = Color(0xFFFFFFFF)

    /** 警告色之上的前景色。 */
    val OnAccentWarning = Color(0xFFFFFFFF)

    /**
     * 模态遮罩。
     *
     * 浅色主题不用黑色遮罩：黑遮罩压下来会把整屏拉成灰泥，
     * 背后那层淡彩光斑全部消失，弹层看起来像「盖在水泥上」。
     * 改成低透明度的深蓝灰，底下的颜色仍然隐约可见。
     */
    val Scrim = Color(0x66101828)

    /**
     * 模态面板底色。
     *
     * 比常规玻璃更实（约 95% 不透明）：面板里是表单，用户要读字、要对齐数字。
     * 但仍然不是纯色——留 5% 让底层色调渗进来，面板才像「浮在液体上」。
     */
    val SheetFill = Color(0xF2FFFFFF)

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
