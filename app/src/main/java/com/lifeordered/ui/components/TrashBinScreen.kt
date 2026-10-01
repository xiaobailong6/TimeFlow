package com.lifeordered.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBinScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deletedMoments by viewModel.deletedMoments.collectAsState()
    val deletedConsumables by viewModel.deletedConsumables.collectAsState()
    val deletedInventories by viewModel.deletedInventories.collectAsState()

    val hasItems = deletedMoments.isNotEmpty() || deletedConsumables.isNotEmpty() || deletedInventories.isNotEmpty()
    var itemToPermanentDelete by remember { mutableStateOf<Any?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🗑️ 回收站",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            if (!hasItems) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍃", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "回收站空空如也",
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
                    items(deletedMoments, key = { "m_${it.id}" }) { moment ->
                        TrashItemRow(
                            title = moment.title,
                            type = "重要时刻",
                            icon = "🧡",
                            onRestore = {
                                viewModel.restoreFromTrash(moment)
                                Toast.makeText(context, "✅ 已恢复", Toast.LENGTH_SHORT).show()
                            },
                            onPermanentDelete = { itemToPermanentDelete = moment }
                        )
                    }

                    items(deletedConsumables, key = { "c_${it.id}" }) { consumable ->
                        TrashItemRow(
                            title = consumable.name,
                            type = "到期提醒",
                            icon = "🕒",
                            onRestore = {
                                viewModel.restoreFromTrash(consumable)
                                Toast.makeText(context, "✅ 已恢复", Toast.LENGTH_SHORT).show()
                            },
                            onPermanentDelete = { itemToPermanentDelete = consumable }
                        )
                    }

                    items(deletedInventories, key = { "i_${it.id}" }) { inventory ->
                        TrashItemRow(
                            title = inventory.name,
                            type = "库存物品",
                            icon = "🛍️",
                            onRestore = {
                                viewModel.restoreFromTrash(inventory)
                                Toast.makeText(context, "✅ 已恢复", Toast.LENGTH_SHORT).show()
                            },
                            onPermanentDelete = { itemToPermanentDelete = inventory }
                        )
                    }
                }
            }

            // Permanent Delete Confimration Dialog
            // Permanent Delete Confirm Dialog
            itemToPermanentDelete?.let { item ->
                ActionConfirmBottomSheet(
                    title = "要彻底销毁它吗？",
                    content = "彻底删除后将无法恢复哦～",
                    confirmText = "彻底删除",
                    isWarning = true,
                    onConfirm = {
                        when (item) {
                            is Moment -> viewModel.permanentDeleteMoment(item)
                            is Consumable -> viewModel.permanentDeleteConsumable(item)
                            is Inventory -> viewModel.permanentDeleteInventory(item)
                        }
                        Toast.makeText(context, "🗑️ 已彻底删除", Toast.LENGTH_SHORT).show()
                        itemToPermanentDelete = null
                    },
                    onCancel = { itemToPermanentDelete = null },
                    onDismissRequest = { itemToPermanentDelete = null }
                )
            }
        }
    }
}

@Composable
fun TrashItemRow(
    title: String,
    type: String,
    icon: String,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE8E5E0), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = type, fontSize = 12.sp, color = GrayTextMuted)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("恢复", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEECEB)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    elevation = null
                ) {
                    Text("彻底删除", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RedWarningText)
                }
            }
        }
    }
}
