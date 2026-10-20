package com.lifeordered.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.DataExporter
import com.lifeordered.ui.navigation.AppDestination
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.io.File

/**
 * 「我的」页。
 *
 * ## 重构要点
 *
 * 1. **子页面交给导航栈**：重构前这里用三个 `remember { mutableStateOf(false) }`
 *    自行切换归档 / 回收站 / 排序页，导致进入子页后底部导航栏仍然显示、
 *    系统返回键也无法返回。现在统一通过 [onNavigate] 推入目的地栈。
 * 2. **提醒开关变成真的**：原来只是 `remember { mutableStateOf(true) }`，
 *    既不持久化也不影响任何调度。现在读写 SharedPreferences，
 *    并在切换时真正重建 / 取消全部闹钟。
 * 3. **数据导出变成真的**：原来只弹一个指向不存在路径的 Toast。
 *    现在会真的写出 JSON 并通过 `FileProvider` 交给系统分享面板。
 */
@Composable
fun ProfileScreen(
    viewModel: LifeOrderedViewModel,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            Toast.makeText(context, "分享失败：${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBg)
    ) {
        // 1. 头部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "拾光与生活",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            Text(
                text = "shyx@gmail.com 🌸",
                fontSize = 12.sp,
                color = GrayTextMuted,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 2. 流年指数
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = WarmOrange.copy(alpha = 0.25f)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.White, PastelPeach.copy(alpha = 0.15f))
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors = listOf(WarmOrange, DarkOrange))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "☀️", fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "我的流年指数",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "让每一件小事都有迹可循，让爱与秩序常伴左右 🧡",
                            fontSize = 12.sp,
                            color = GrayTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox(value = viewModel.getInstallDays().toString(), label = "有序运行天数")
                            StatBox(
                                value = moments.count { !it.isArchived }.toString(),
                                label = "守护的时刻"
                            )
                            StatBox(value = inventories.size.toString(), label = "管辖的物件")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "系统功能与设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimaryText,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            // 3. 设置项
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    SettingsListTile(
                        icon = {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = WarmOrange)
                        },
                        title = "系统提醒推送开关",
                        description = if (notificationsEnabled) {
                            "已开启：纪念日与耗材到期会在本地提醒你"
                        } else {
                            "已关闭：不会收到任何本地提醒"
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.setNotificationsEnabled(checked)
                                    Toast.makeText(
                                        context,
                                        if (checked) "🔔 已重新排定全部提醒" else "🔕 已取消全部提醒",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = WarmOrange,
                                    uncheckedThumbColor = GrayTextMuted,
                                    uncheckedTrackColor = LightCream
                                ),
                                modifier = Modifier.testTag("notification_toggle_switch")
                            )
                        }
                    )

                    Divider(color = WarmCreamBg, thickness = 1.dp)

                    SettingsListTile(
                        icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = WarmOrange) },
                        title = "历史时刻归档 📦",
                        description = "查看或恢复已归档的历史和到期事件",
                        onClick = { onNavigate(AppDestination.ArchivedMoments) },
                        modifier = Modifier.testTag("history_archive_tile")
                    )

                    Divider(color = WarmCreamBg, thickness = 1.dp)

                    SettingsListTile(
                        icon = { Text("🗑️", fontSize = 20.sp) },
                        title = "回收站",
                        description = "找回被删除的记录或将其彻底销毁",
                        onClick = { onNavigate(AppDestination.TrashBin) },
                        modifier = Modifier.testTag("trash_bin_tile")
                    )

                    Divider(color = WarmCreamBg, thickness = 1.dp)

                    SettingsListTile(
                        icon = { Text("⚙️", fontSize = 20.sp) },
                        title = "首页板块排序",
                        description = "自定义调整首页三大模块的展示顺序",
                        onClick = { onNavigate(AppDestination.SectionOrdering) },
                        modifier = Modifier.testTag("reorder_section_tile")
                    )

                    Divider(color = WarmCreamBg, thickness = 1.dp)

                    SettingsListTile(
                        icon = { Icon(Icons.Default.Share, contentDescription = null, tint = WarmOrange) },
                        title = if (isExporting) "正在导出…" else "云备份与数据导出",
                        description = "把时刻、耗材、库存与标签导出为 JSON 文件",
                        onClick = {
                            if (isExporting) return@SettingsListTile
                            isExporting = true
                            viewModel.exportData { result ->
                                isExporting = false
                                result.onSuccess { file ->
                                    Toast.makeText(
                                        context,
                                        "💾 已导出 ${file.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    shareExport(file)
                                }.onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        "导出失败：${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("backup_data_tile")
                    )

                    Divider(color = WarmCreamBg, thickness = 1.dp)

                    SettingsListTile(
                        icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = RedWarningText) },
                        title = "重置所有应用数据",
                        description = "清空所有时刻及耗材物资，重新注入演示包",
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_data_tile")
                    )

                    Divider(color = WarmCreamBg, thickness = 1.dp)

                    SettingsListTile(
                        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = BlueAccentText) },
                        title = "关于拾光流年",
                        description = "查看应用版本及温暖的致谢信息",
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("about_app_tile")
                    )
                }
            }
        }
    }

    // --- 危险区确认 ---
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "🚨 危险区警告",
                    fontWeight = FontWeight.Bold,
                    color = RedWarningText
                )
            },
            text = {
                Text(
                    text = "您正在操作高级重置命令。该选项会清空现有的所有本地 SQLite 数据库表，并注入系统预设演示项。\n\n您确定要抹除并重建整个生活秩序吗？这无法撤销哦！",
                    color = TextDark,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                        Toast.makeText(
                            context,
                            "🗑️ 数据库清理完成！生活秩序已恢复初始预设种子 🌱",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedWarningText),
                    modifier = Modifier.testTag("confirm_reset_btn")
                ) {
                    Text("核心数据一键抹除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消安全返回", color = GrayTextMuted)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // --- 关于 ---
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "🏡 关于拾光流年",
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "《拾光流年》App 为您记录生活的点点滴滴。从至亲的生日纪念、到净水滤芯的使用周期、再到猫砂纸巾等物件的精细储备管理，都在它的安全框架守护之中。",
                        color = TextDark,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Divider(color = LightCream, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "应用版本: v1.1.0\n开发专员: LifeOrdered Team\n致谢守护员: shyx@gmail.com\n\n致谢: 3D Cream Styling layout & Kotlin Compose Engine framework.",
                        color = GrayTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmOrange)
                ) {
                    Text("收到，谢谢致谢 🧡", color = Color.White)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

/** 统计数字单元。 */
@Composable
fun StatBox(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkOrange
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = GrayTextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 设置项列表行。 */
@Composable
fun SettingsListTile(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WarmCreamBg)
                .wrapContentSize(Alignment.Center)
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Text(
                text = description,
                fontSize = 12.sp,
                color = GrayTextMuted
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = GrayTextMuted.copy(alpha = 0.6f)
            )
        }
    }
}
