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
import com.lifeordered.core.utils.CalendarHelper
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

@Composable
fun MomentListItem(
    moment: Moment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val baseColor = parseHexColor(moment.themeColorHex)
    val daysRemaining = CalendarHelper.calculateDays(moment)

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
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(baseColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                when (moment.cardColorIndex) {
                    0 -> ClayCakeIcon()
                    1 -> ClayGiftIcon()
                    else -> ClayHeartsIcon()
                }
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
                
                val dateLabel = if (moment.isLunar) "农历 ${moment.month}月${moment.day}日" else "${moment.month}月${moment.day}日"
                Text(
                    text = "$dateLabel • ${if (moment.type == "countUp") "正数日" else "倒数日"}",
                    fontSize = 12.sp,
                    color = GrayTextMuted
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val prefix = if (moment.type == "countUp") "已过 " else if (daysRemaining < 0) "逾期 " else "剩余 "
                    Text(text = prefix, fontSize = 11.sp, color = TextDark)
                    Text(
                        text = "${Math.abs(daysRemaining)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (daysRemaining < 0 && moment.type == "countdown") RedWarningText else baseColor
                    )
                    Text(text = " 天", fontSize = 11.sp, color = TextDark)
                }
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CreamEditButton(
                    onClick = onEdit,
                    containerSize = 32.dp,
                    iconSize = 16.dp
                )
                CreamDeleteButton(
                    onClick = onDelete,
                    containerSize = 32.dp,
                    iconSize = 16.dp
                )
            }
        }
    }
}
