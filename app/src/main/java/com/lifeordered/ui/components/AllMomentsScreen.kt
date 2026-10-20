package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.ui.format.MomentPresentation
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMomentsScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    onEdit: (Moment) -> Unit,
    onDelete: (Moment) -> Unit,
    modifier: Modifier = Modifier
) {
    val moments by viewModel.activeMoments.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "所有重要时刻 🧡",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("all_moments_back_button")
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
            if (moments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂时没有记录的时刻哦",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(moments, key = { it.id }) { moment ->
                        MomentListItem(
                            moment = moment,
                            onEdit = { onEdit(moment) },
                            onDelete = { onDelete(moment) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 全部时刻列表行。
 *
 * 与首页卡片共享 [MomentPresentation]，因此日期标签、天数文案、颜色、
 * 图标槽位与首页完全一致（重构前这里是另一套规则，连图标分支都只有 3 个）。
 */
@Composable
fun MomentListItem(
    moment: Moment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val baseColor = parseHexColor(moment.themeColorHex)
    val days = remember(moment) { MomentSchedule.resolve(moment) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .border(2.dp, baseColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(baseColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                MomentClayIcon(moment)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = moment.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = MomentPresentation.listSubtitle(moment),
                    fontSize = 12.sp,
                    color = GrayTextMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = MomentPresentation.dayCaption(days),
                        fontSize = 11.sp,
                        color = TextDark
                    )
                    Text(
                        text = MomentPresentation.dayValue(days),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MomentPresentation.dayColor(days)
                    )
                    Text(text = " 天", fontSize = 11.sp, color = TextDark)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CreamEditButton(onClick = onEdit, containerSize = 32.dp, iconSize = 16.dp)
                CreamDeleteButton(onClick = onDelete, containerSize = 32.dp, iconSize = 16.dp)
            }
        }
    }
}
