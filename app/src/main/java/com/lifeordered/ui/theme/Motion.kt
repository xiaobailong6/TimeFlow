package com.lifeordered.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 全局动效参数。
 *
 * ## 为什么只用 `animation-core`，不用 `AnimatedContent` / `Crossfade`
 *
 * 项目的 `gradle/libs.versions.toml` 里没有 `androidx-compose-animation` 的别名，
 * 只有 `compose-ui` / `compose-material3` 等。`animation-core` 是它们的传递依赖，
 * 因此 `animateFloatAsState` / `spring` / `tween` 可以直接用；
 * 而 `AnimatedContent` 属于 `androidx.compose.animation` 包，不在传递依赖里，
 * 贸然使用会编译失败。这里统一用「动画驱动一个 Float，再喂给 graphicsLayer」的写法。
 *
 * ## 为什么不用 `Modifier.animateContentSize()`
 *
 * 它同样来自 `androidx.compose.animation`。列表卡片高度本来就不常变，
 * 为它引一个新依赖不划算。
 */
object Motion {
    /** 轻量反馈：按压回弹、图标状态切换。 */
    const val Fast = 160

    /** 页面/区块入场。 */
    const val Normal = 260

    /** 需要被感知的较大变化（弹层、整屏切换）。 */
    const val Slow = 420
}

/**
 * 按压缩放反馈。
 *
 * 用法：先建一个 [MutableInteractionSource]，把它同时交给 `clickable` 和本修饰符，
 * 否则本修饰符收不到按压事件。
 *
 * 缩放值走 `graphicsLayer`，只影响绘制，不触发重新测量/布局，
 * 因此在长列表里也不会掉帧。
 *
 * @param pressedScale 按下时的缩放比例。默认 0.96：小于 0.94 会显得廉价，
 *        大于 0.98 用户感知不到。
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        // 阻尼偏低、刚度偏中：松手时有一次轻微回弹，像真的按在软材质上
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )

    // graphicsLayer 必须**前置**（放在已有修饰符的外层），不能 append。
    //
    // 修饰符链的绘制是「外层先画、内层后画」：若把 graphicsLayer 追加在
    // `.background(...)` 之后，它只包住后续内容，于是按压时缩小的是卡片里的
    // 文字和图标，卡片自己的填充与描边纹丝不动——看起来像「内容往下陷」，
    // 而不是「整块玻璃被按下去」。
    // 前置之后，调用点写成 `modifier.背景().pressableClickable()` 就能整块缩放。
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.then(this)
}

/**
 * 页面淡入 + 轻微上浮。
 *
 * @param key 切换时重新播放动画的标识（通常是当前导航目的地）。
 *        相同的 key 不会重播，避免每次重组都闪一下。
 */
@Composable
fun ScreenFadeIn(
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 首帧用 0 起手，下一帧才置 true，动画才有起点可插值
    var entered by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { entered = true }

    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = Motion.Normal, easing = FastOutSlowInEasing),
        label = "screenFadeIn"
    )

    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            // 12dp 的位移足够让「换了一屏」被感知，又不会让内容像在飞
            translationY = (1f - progress) * 12.dp.toPx()
        }
    ) {
        content()
    }
}

/**
 * 带按压回弹的 [clickable]。
 *
 * 单独封装是为了让组件库只改一个词：把 `.clickable(onClick = ...)` 换成
 * `.pressableClickable(onClick = ...)`，全站按钮就有了统一的按压手感，
 * 不必每个调用点自己管理 `MutableInteractionSource`。
 *
 * 显式传 `interactionSource` 时**必须同时传 `indication`**，
 * 否则 Compose 会直接抛异常（默认涟漪会失效）。
 */
@Composable
fun Modifier.pressableClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
        .pressScale(interactionSource, pressedScale)
}
