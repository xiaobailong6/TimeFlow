package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassChip
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

/**
 * 录入 / 编辑面板的宿主（液态玻璃版）。
 *
 * ## 关于拆分
 *
 * 这个文件原本有 1000 多行，同时包含「宿主 + 三个表单 + 日期轮盘 + 图标选择」。
 * 现在按职责拆成：
 * - 本文件：宿主、图标选择入口、三段 Tab
 * - `EntryForms.kt`：时刻 / 耗材 / 库存三个表单
 * - `WheelDatePicker.kt`：滚轮日期选择（含农历辅助）
 *
 * 拆分不是为了好看，而是因为这三个表单的字段会各自演化——
 * 混在一个文件里改一处就要重读全部上下文。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: LifeOrderedViewModel,
    editMoment: Moment? = null,
    editConsumable: Consumable? = null,
    editInventory: Inventory? = null
) {
    val isEditing = editMoment != null || editConsumable != null || editInventory != null

    var selectedTab by remember {
        mutableStateOf(
            when {
                editMoment != null -> 0
                editConsumable != null -> 1
                editInventory != null -> 2
                else -> 0
            }
        )
    }

    var selectedIcon by remember {
        mutableStateOf(
            editMoment?.iconPath
                ?: editConsumable?.iconPath
                ?: editInventory?.iconPath
                ?: "🎂"
        )
    }
    var showIconPicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Glass.SheetFill,
        scrimColor = Glass.Scrim,
        shape = RoundedCornerShape(topStart = Glass.Radius.panel, topEnd = Glass.Radius.panel),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Glass.FillStrong)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditing) "修改记录" else "添加记录",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 图标选择
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Glass.Fill)
                    .border(1.dp, Glass.StrokeMid, CircleShape)
                    .clickable { showIconPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text(text = selectedIcon, fontSize = 28.sp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Glass.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✏", fontSize = 9.sp, color = Glass.OnAccent)
                }
            }

            if (showIconPicker) {
                IconPicker(
                    onIconSelected = {
                        selectedIcon = it
                        showIconPicker = false
                    },
                    onDismiss = { showIconPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 三段 Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurfaceForTabs()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf("重要时刻", "到期提醒", "库存物品")
                tabs.forEachIndexed { index, title ->
                    val canChange = !isEditing
                    Box(modifier = Modifier.weight(1f)) {
                        GlassChip(
                            label = title,
                            selected = selectedTab == index,
                            onClick = { if (canChange) selectedTab = index },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                0 -> MomentInputForm(
                    viewModel = viewModel,
                    onDismiss = onDismissRequest,
                    editMoment = editMoment,
                    iconPath = selectedIcon
                )
                1 -> ConsumableInputForm(
                    viewModel = viewModel,
                    onDismiss = onDismissRequest,
                    editConsumable = editConsumable,
                    iconPath = selectedIcon
                )
                else -> InventoryInputForm(
                    viewModel = viewModel,
                    onDismiss = onDismissRequest,
                    editInventory = editInventory,
                    iconPath = selectedIcon
                )
            }
        }
    }
}

/**
 * Tab 容器背景。
 *
 * 单独抽出来是因为 `glassSurface` 需要 Shape 参数，而 Tab 行用的是
 * 药丸形容器——直接内联会让上面那段布局读起来太长。
 */
private fun Modifier.glassSurfaceForTabs(): Modifier = this
    .clip(Glass.pillShape)
    .background(Glass.FillSubtle)
    .border(1.dp, Glass.StrokeBottom, Glass.pillShape)
