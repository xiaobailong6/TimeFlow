package com.lifeordered.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.DataExporter
import com.lifeordered.ui.navigation.AppDestination
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassCard
import com.lifeordered.ui.theme.GlassDivider
import com.lifeordered.ui.theme.GlassGroup
import com.lifeordered.ui.theme.GlassScreenHeader
import com.lifeordered.ui.theme.accentGlow
import com.lifeordered.ui.theme.glassSurface
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.io.File

/**
 * 「我的」页（液态玻璃版）。
 *
 * 这里承担两个真实功能，而不是装饰：
 * - **提醒开关**：读写 SharedPreferences，切换时真正重建/取消全部闹钟
 * - **数据导出**：真的写出 JSON 并通过 FileProvider 交给系统分享
 */
@Composable
fun ProfileScreen(
    viewModel: LifeOrderedViewModel,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val notify = rememberNotifier()
    val context = androidx.compose.ui.platform.LocalContext.current

    val moments by viewModel.allMoments.collectAsState()
    val inventories by viewModel.allInventories.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled

    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    fun shareExport(file: File) {
        runCatching {
            val uri = DataExporter.shareUriFor(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "导出生活数据"))
        }.onFailure {
            notify("分享失败：${it.message}")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        GlassScreenHeader(title = "拾光与生活", subtitle = "shyx@gmail.com")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 统计卡
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = Glass.panelShape,
                    fill = Glass.Fill,
                    elevation = 14.dp,
                    glow = Glass.GlowRose,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .accentGlow(Glass.Accent, shape = CircleShape, elevation = 10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Glass.GlowAmber, Glass.GlowRose)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "☀️", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "我的流年指数",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Glass.TextPrimary,
                            letterSpacing = (-0.2).sp
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "让每一件小事都有迹可循",
                            fontSize = 12.sp,
                            color = Glass.TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox(
                                value = viewModel.getInstallDays().toString(),
                                label = "运行天数"
                            )
                            StatBox(
                                value = moments.count { !it.isArchived }.toString(),
                                label = "守护时刻"
                            )
                            StatBox(
                                value = inventories.size.toString(),
                                label = "在管物件"
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "设置",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Glass.TextTertiary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }

            item {
                GlassGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        iconTint = Glass.Accent,
                        title = "提醒推送",
                        description = if (notificationsEnabled) {
                            "已开启，纪念日与耗材到期会提醒你"
                        } else {
                            "已关闭，不会收到任何本地提醒"
                        },
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.setNotificationsEnabled(checked)
                                    notify(if (checked) "已重新排定全部提醒" else "已取消全部提醒")
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Glass.OnAccent,
                                    checkedTrackColor = Glass.Accent,
                                    uncheckedThumbColor = Glass.TextTertiary,
                                    uncheckedTrackColor = Glass.Fill
                                ),
                                modifier = Modifier.testTag("notification_toggle_switch")
                            )
                        }
                    )

                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        iconTint = Glass.AccentCool,
                        title = "历史时刻归档",
                        description = "查看或恢复已归档的到期事件",
                        onClick = { onNavigate(AppDestination.ArchivedMoments) },
                        modifier = Modifier.testTag("history_archive_tile")
                    )

                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.Delete,
                        iconTint = Glass.TextSecondary,
                        title = "回收站",
                        description = "找回已删除的记录，或彻底销毁",
                        onClick = { onNavigate(AppDestination.TrashBin) },
                        modifier = Modifier.testTag("trash_bin_tile")
                    )

                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        iconTint = Glass.TextSecondary,
                        title = "首页板块排序",
                        description = "调整三大模块在首页的先后顺序",
                        onClick = { onNavigate(AppDestination.SectionOrdering) },
                        modifier = Modifier.testTag("reorder_section_tile")
                    )

                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.Share,
                        iconTint = Glass.AccentSuccess,
                        title = if (isExporting) "正在导出…" else "数据导出",
                        description = "把时刻、耗材、库存与标签导出成 JSON 文件",
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                viewModel.exportData { result ->
                                    isExporting = false
                                    result.onSuccess { file ->
                                        notify("已导出 ${file.name}")
                                        shareExport(file)
                                    }.onFailure { error ->
                                        notify("导出失败：${error.message}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("backup_data_tile")
                    )

                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        iconTint = Glass.AccentDanger,
                        title = "重置所有数据",
                        description = "清空全部记录并重新注入演示数据",
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_data_tile")
                    )

                    GlassDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconTint = Glass.TextSecondary,
                        title = "关于拾光流年",
                        description = "版本信息与致谢",
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("about_app_tile")
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("重置所有数据？", fontWeight = FontWeight.Bold, color = Glass.TextPrimary)
            },
            text = {
                Text(
                    text = "会清空全部时刻、耗材、库存与标签，并重新注入演示数据。此操作无法撤销。",
                    color = Glass.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                        notify("数据已重置")
                    },
                    modifier = Modifier.testTag("confirm_reset_btn")
                ) {
                    Text("确认重置", color = Glass.AccentDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消", color = Glass.TextSecondary)
                }
            },
            shape = Glass.cardShape,
            containerColor = Glass.SheetFill
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("关于拾光流年", fontWeight = FontWeight.Bold, color = Glass.TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "记录至亲的生日、耗材的使用周期、家里的储备余量。所有数据都留在本机，不上传任何服务器。",
                        color = Glass.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "版本 1.1.0",
                        color = Glass.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("知道了", color = Glass.Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            shape = Glass.cardShape,
            containerColor = Glass.SheetFill
        )
    }
}

/** 统计数字单元。 */
@Composable
fun StatBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Glass.Accent,
            letterSpacing = (-0.8).sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Glass.TextTertiary
        )
    }
}

/** 设置行。 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .glassSurface(shape = CircleShape, fill = Glass.FillSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Glass.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = Glass.TextSecondary,
                lineHeight = 16.sp
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Glass.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
