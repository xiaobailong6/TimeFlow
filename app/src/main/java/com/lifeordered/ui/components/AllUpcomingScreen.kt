package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Consumable
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUpcomingScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    onEdit: (Consumable) -> Unit,
    onDelete: (Consumable) -> Unit,
    modifier: Modifier = Modifier
) {
    val consumables by viewModel.allConsumables.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "所有到期提醒 🕒",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("all_upcoming_back_button")
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
            if (consumables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🕒", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂时没有记录的耗材哦",
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
                    items(consumables, key = { it.id }) { consumable ->
                        ConsumableListItem(
                            consumable = consumable,
                            onEdit = { onEdit(consumable) },
                            onDelete = { onDelete(consumable) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsumableListItem(
    consumable: Consumable,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingDays = consumable.getRemainingDays()
    val isCritical = remainingDays <= 3
    val baseColor = if (isCritical) Color(0xFFF44336) else Color(0xFF2196F3)
    
    val expiryTime = consumable.startDateMillis + (consumable.totalDays * 24 * 60 * 60 * 1000L)
    val sdf = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
    val formattedExpiry = sdf.format(Date(expiryTime))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .border(2.dp, baseColor.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
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
                    .background(baseColor.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = consumable.iconPath.ifBlank { "📦" }, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = consumable.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$formattedExpiry 到期",
                    fontSize = 12.sp,
                    color = GrayTextMuted
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "剩余 ", fontSize = 11.sp, color = TextDark)
                    Text(
                        text = "$remainingDays",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = baseColor
                    )
                    Text(text = " 天", fontSize = 11.sp, color = TextDark)
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Simple progress bar inline
                    val progress = (remainingDays.toFloat() / consumable.totalDays.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LightCream)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(baseColor)
                        )
                    }
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
