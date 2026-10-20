package com.lifeordered.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 液态玻璃基础组件库。
 *
 * 所有组件都遵循同一套规则：
 * - 填充用 [Glass.Fill] 三档之一，**不自己发明透明度**
 * - 边缘一律 [glassStrokeBrush]，保证光线方向一致
 * - 圆角从 [Glass.Radius] 取，避免每个调用点各写一个数字
 *
 * 这样即使后续新增界面，视觉也会自动保持一致。
 */

// -----------------------------------------------------------------------------
// 容器
// -----------------------------------------------------------------------------

/**
 * 玻璃卡片。
 *
 * @param glow 卡片自身色调的辉光。液态玻璃里区分条目的主要手段是**辉光颜色**，
 *        而不是换底色——因为底色一旦不透明，玻璃感就消失了。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = Glass.cardShape,
    fill: Color = Glass.Fill,
    elevation: androidx.compose.ui.unit.Dp = 10.dp,
    glow: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = modifier
        .liquidGlass(shape = shape, fill = fill, elevation = elevation, glow = glow)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(contentPadding)

    Box(modifier = base) {
        content()
    }
}

/** 玻璃分组：比卡片更弱，用于把若干条目归到一组。 */
@Composable
fun GlassGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = Glass.cardShape, fill = Glass.FillSubtle)
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

/** 分隔线：极低对比，仅用于切分同一玻璃面板内的行。 */
@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Glass.StrokeMid, Color.Transparent)
                )
            )
    )
}

// -----------------------------------------------------------------------------
// 文本
// -----------------------------------------------------------------------------

/** 区块标题 + 可选的右侧动作。 */
@Composable
fun GlassSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Glass.TextPrimary,
            letterSpacing = (-0.2).sp
        )
        if (trailingText != null && onTrailingClick != null) {
            Text(
                text = trailingText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Glass.Accent,
                modifier = Modifier
                    .clip(Glass.pillShape)
                    .clickable(onClick = onTrailingClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/** 页面标题栏。 */
@Composable
fun GlassScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "返回",
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Glass.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actions != null) {
            Spacer(modifier = Modifier.width(8.dp))
            actions()
        }
    }
}

// -----------------------------------------------------------------------------
// 交互
// -----------------------------------------------------------------------------

/**
 * 玻璃胶囊选择器，用于筛选与多选一。
 *
 * 选中态用强调色**实心填充 + 外辉光**，而不是只改文字颜色——
 * 在深色玻璃上，仅靠文字变色不足以让当前选项跳出来。
 */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Glass.Accent
) {
    val shape = Glass.pillShape
    Box(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier
                        .accentGlow(accent, shape = shape, elevation = 6.dp)
                        .clip(shape)
                        .background(accent)
                } else {
                    Modifier.glassPill(fill = Glass.FillSubtle)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Glass.OnAccent else Glass.TextSecondary,
            maxLines = 1
        )
    }
}

/** 主操作按钮：强调色渐变填充。 */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Glass.Accent
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .accentGlow(if (enabled) accent else Color.Transparent, shape = Glass.fieldShape)
            .clip(Glass.fieldShape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        listOf(accent, accent.copy(alpha = 0.82f))
                    )
                } else {
                    Brush.horizontalGradient(listOf(Glass.Fill, Glass.Fill))
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Glass.OnAccent else Glass.TextTertiary
        )
    }
}

/** 次级按钮：玻璃描边。 */
@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Glass.TextPrimary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

/** 圆形玻璃图标按钮。 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Glass.TextPrimary,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .glassSurface(
                shape = androidx.compose.foundation.shape.CircleShape,
                fill = Glass.FillSubtle
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 文字型圆形按钮（用于 Emoji 或单字）。 */
@Composable
fun GlassTextIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    Box(
        modifier = modifier
            .size(size)
            .glassSurface(
                shape = androidx.compose.foundation.shape.CircleShape,
                fill = Glass.FillSubtle
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = fontSize, color = Glass.TextPrimary)
    }
}

// -----------------------------------------------------------------------------
// 输入
// -----------------------------------------------------------------------------

/**
 * 玻璃输入框。
 *
 * 没有用 Material 的 `OutlinedTextField`：它的默认描边、标签动画、
 * 内部 padding 与玻璃体系冲突，逐个覆盖比重写还麻烦。
 * 这里用 `BasicTextField` + 自绘装饰盒，视觉完全可控。
 *
 * 注意 `textStyle` 必须显式指定颜色——深色背景上默认的黑色文字会直接消失。
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leading: @Composable (() -> Unit)? = null
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = TextStyle(
            color = Glass.TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        cursorBrush = SolidColor(Glass.Accent),
        keyboardOptions = keyboardOptions,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    leading()
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 14.sp,
                            color = Glass.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

/** 表单字段标签。 */
@Composable
fun GlassFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Glass.TextSecondary
    )
}

// -----------------------------------------------------------------------------
// 空状态
// -----------------------------------------------------------------------------

/**
 * 空状态。
 *
 * 空屏是「邀请行动」的地方，不是「报告心情」的地方——
 * 所以文案写清楚下一步做什么，而不是只放一个叹号图标。
 */
@Composable
fun GlassEmptyState(
    glyph: String,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentPadding = PaddingValues(vertical = 26.dp, horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = glyph, fontSize = 30.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Glass.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                fontSize = 12.sp,
                color = Glass.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .glassPill(fill = Glass.Fill)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Glass.Accent
                    )
                }
            }
        }
    }
}
