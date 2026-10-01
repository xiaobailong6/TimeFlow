package com.lifeordered.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedMomentsScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val archivedList by viewModel.archivedMoments.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "历史时刻归档 📦",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("archive_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "返回",
                            tint = TextDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCreamBg)
            )
        },
        containerColor = WarmCreamBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (archivedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "📭",
                            fontSize = 64.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "没有找到已归档的历史时刻哦",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "正数日或到期的倒数日可以手动归档~",
                            fontSize = 13.sp,
                            color = GrayTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1EDE6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 提示：归档的时刻可以通过右侧的「恢复」按钮放回首页，继续为您守护并计算天数。",
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.8f),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    items(archivedList, key = { it.id }) { moment ->
                        ArchivedMomentItemRow(
                            moment = moment,
                            onRestore = {
                                viewModel.updateMomentArchiveStatus(moment, false)
                                Toast.makeText(context, "🔄 [${moment.title}] 已成功恢复至首页！", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.moveToTrash(moment)
                                Toast.makeText(context, "🗑️ [${moment.title}] 已移入回收站", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedMomentItemRow(
    moment: Moment,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val baseColor = parseHexColor(moment.themeColorHex)
    val cardBg = Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .background(cardBg, RoundedCornerShape(20.dp))
            .border(2.dp, baseColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon backdrop circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(baseColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📦",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = moment.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                val dateStr = if (moment.isLunar) "农历 ${moment.month}月${moment.day}日" else "${moment.month}月${moment.day}日"
                Text(
                    text = "$dateStr • ${if (moment.type == "countUp") "正数日" else "倒数日"}",
                    fontSize = 11.sp,
                    color = GrayTextMuted
                )

                if (!moment.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "备注: ${moment.notes}",
                        fontSize = 11.sp,
                        color = TextDark.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Restore button
            IconButton(
                onClick = onRestore,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("archive_restore_${moment.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "恢复",
                    tint = baseColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Permanent Delete button
            CreamDeleteButton(
                onClick = onDelete,
                modifier = Modifier.testTag("archive_delete_${moment.id}"),
                containerSize = 32.dp,
                iconSize = 16.dp
            )
        }
    }
}
