package com.lifeordered.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃（Liquid Glass）设计令牌 —— 真折射玻璃版。
 *
 * ## 这一版换了实现，也换了配色
 *
 * 前几版是「手搓玻璃」：半透明白填充 + 上亮下暗的渐变描边 + 同色调柔光，
 * 靠 [LiquidGlassBackground] 的彩色光斑透出来制造透明感。结果是整屏发白、
 * 卡片没有边界——实测 `Fill` 与基底 `BaseTop` 的对比度只有 **1.03:1**，
 * 人眼需要约 1.2:1 才能分辨「有东西浮在上面」。所以「难看」的根因是
 * **对比度**，不是缺效果。
 *
 * 现在玻璃交给 Haze 的 `haze-glass`（折射 + 深度模糊 + 菲涅尔 + 镜面高光），
 * 本文件只负责**颜色**。因此：
 *
 * 1. 基底从近白压深到 [BaseTop] → [BaseBottom]，玻璃才有东西可折射；
 * 2. 填充透明度大幅下调（Haze 自己会加材质响应，再叠高不透明度会糊成白板）；
 * 3. 所有强调色按 **WCAG AA（白字 ≥ 4.5:1）** 重新取值，并逐一实测：
 *
 * | 令牌 | 值 | 白字对比度 |
 * |---|---|---|
 * | [Accent] | `#C2410C` | 5.18 |
 * | [AccentCool] | `#0B72B8` | 5.11 |
 * | [AccentDanger] | `#C42B30` | 5.62 |
 * | [AccentSuccess] | `#0F7A54` | 5.34 |
 * | [AccentWarning] | `#A16207` | 4.92 |
 *
 * 上一版是 `#F97316`（2.80）、`#D99100`（2.62）等，全部不达标，
 * 白字压上去会「发飘」——这也是观感廉价的原因之一。
 *
 * ## 为什么现在可以真模糊
 *
 * `minSdk` 已提到 **31**，`RenderEffect` 全量可用，Haze 在 API 31+ 走真实渲染路径。
 * 之前为了 API 24 兼容而完全不做模糊的取舍已经不存在了。
 */
object Glass {

    // ---------------------------------------------------------------------
    // 基底：必须比玻璃「有色」，否则白玻璃在白底上等于没画
    // ---------------------------------------------------------------------

    val BaseTop = Color(0xFFF0F5FD)
    val BaseBottom = Color(0xFFDCE7F7)

    // ---------------------------------------------------------------------
    // 液体光斑：玻璃之所以「液态」，是因为背后有缓慢流动的彩色光
    //
    // Haze 会把它们模糊、折射、并在边缘做色散，所以这里可以比手搓版更饱和。
    // ---------------------------------------------------------------------

    val GlowAmber = Color(0xFFFFA76B)
    val GlowRose = Color(0xFFFF86B4)
    val GlowCyan = Color(0xFF6FD0F0)
    val GlowViolet = Color(0xFFA08CF0)

    // ---------------------------------------------------------------------
    // 玻璃填充（作为 Haze 的 tint）
    //
    // 数值刻意比手搓版低很多：Haze 的材质本身已经带底色与亮度响应，
    // 再叠 55% 白会直接盖住折射效果，玻璃退化成白板。
    // ---------------------------------------------------------------------

     // 玻璃容器不叠白色底板；形状依靠折射和细描边表达。
     val FillSubtle = Color.Transparent
     val Fill = Color.Transparent
     val FillStrong = Color.Transparent
     val FillHeavy = Color.Transparent
 
     /**
      * 小控件（Chip / 图标按钮 / 底栏胶囊）的实体填充。
      *
      * 这些控件**不走 Haze 着色器**（性能 + 阴影圈考虑），所以不能用透明填充，
      * 否则点按区域完全隐形。这里给一层中等透明度的白：既有玻璃的「材质感」，
      * 又不会像 SheetFill 那样盖死背景光斑。
      */
     val FillControl = Color(0x73FFFFFF)
 
     /**
      * 大面板（卡片 / 底栏 / 横幅）的实体填充。
      *
      * 面板也**不走 Haze 着色器**——`clear` 风格的折射渲染必然在边缘产生
      * 亮度突变圈（用户看到的「阴影背景色」）。改用一层低透明度白 + 顶部高光
      * + 白色渐变描边，全部白色系，物理上不可能有暗圈。
      */
     val FillPanel = Color(0x4DFFFFFF)
 
     // ---------------------------------------------------------------------
     // 玻璃边缘
     //
     // 顶边保持高亮，底边改为近透明：玻璃的「暗边圈」主要来自底部暗描边。
     // ---------------------------------------------------------------------
     val StrokeTop = Color(0x52FFFFFF)
     val StrokeMid = Color(0x2EFFFFFF)
     /** 透明玻璃只留极淡边缘，避免内部暗线形成阴影圈。 */
     val StrokeBottom = Color(0x14FFFFFF)
     val Hairline = Color(0x1A3C4A6B)
    // ---------------------------------------------------------------------
    // 文字：三级层级，全部实测过对比度（基底 #F0F5FD）
    // ---------------------------------------------------------------------

    /** 15.3:1 */
    val TextPrimary = Color(0xFF16203A)

    /** 5.57:1 */
    val TextSecondary = Color(0xFF5B6480)

    /** 5.00:1。上一版 #8B93A8 只有 2.92:1，属于「看着脏」的来源之一。 */
    val TextTertiary = Color(0xFF5F6878)

    // ---------------------------------------------------------------------
    // 强调色：暖橙作为品牌连续性，其余用于语义
    // ---------------------------------------------------------------------

    val Accent = Color(0xFFC2410C)
    val AccentCool = Color(0xFF0B72B8)
    val AccentDanger = Color(0xFFC42B30)
    val AccentSuccess = Color(0xFF0F7A54)
    val AccentWarning = Color(0xFFA16207)

    /** 强调色之上的前景色。上面每个强调色都已实测 ≥ 4.5:1，因此统一用白。 */
    val OnAccent = Color(0xFFFFFFFF)

    val OnAccentCool = Color(0xFFFFFFFF)
    val OnAccentSuccess = Color(0xFFFFFFFF)
    val OnAccentDanger = Color(0xFFFFFFFF)
    val OnAccentWarning = Color(0xFFFFFFFF)

    /**
     * 模态遮罩。
     *
     * 不用纯黑：黑遮罩会把背后的彩色光斑压成灰泥，玻璃的折射就没有可采样的内容了。
     */
    val Scrim = Color(0x66101828)

    /**
     * 模态面板底色。
     *
     * 表单里要读字、要对齐数字，所以面板比普通玻璃更实；
     * 但仍留一点透明度，让底层色调渗进来。
     */
    val SheetFill = Color(0xE6FFFFFF)

    // ---------------------------------------------------------------------
    // 圆角分级：面板 > 卡片 > 控件 > 胶囊
    //
    // 全部是 RoundedCornerShape —— Haze 的 `shape(...)` 只接受这个类型，
    // 这里统一口径可以避免调用点做类型转换。
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
