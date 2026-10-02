package com.lifeordered.ui.components

import java.util.Calendar
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import com.lifeordered.data.models.Moment
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: LifeOrderedViewModel,
    editMoment: Moment? = null,
    editConsumable: Consumable? = null,
    editInventory: Inventory? = null
) {
    // Initial tab selected depending on editing state
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
    val tabs = listOf("重要时刻", "到期提醒", "库存物品")

    // Standard BottomSheet Scaffold
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    var selectedIcon by remember { mutableStateOf(editMoment?.iconPath ?: editConsumable?.iconPath ?: editInventory?.iconPath ?: "🎂") }
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = WarmCreamBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF8B8076).copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .imePadding()
                .nestedScroll(object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    return Offset.Zero
                }
            })
            .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (editMoment != null || editConsumable != null || editInventory != null) "修改记录 ✏️" else "添加记录 ➕",
                fontSize = 20.sp,
                color = TextDark,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Requirement 2: Icon Selection Row
            var showIconPicker by remember { mutableStateOf(false) }
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, WarmOrange, CircleShape)
                    .clickable { showIconPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text(text = selectedIcon, fontSize = 32.sp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(WarmOrange)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✏️", fontSize = 10.sp)
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

            // Segmented Tab Controls (Clay Style) - disable tab change if editing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val canChangeTab = editMoment == null && editConsumable == null && editInventory == null
                    val backgroundColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) WarmOrange else Color.Transparent,
                        animationSpec = androidx.compose.animation.core.tween(250),
                        label = "tabBackground"
                    )
                    val textColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) Color.White else GrayTextMuted,
                        animationSpec = androidx.compose.animation.core.tween(250),
                        label = "tabText"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(backgroundColor)
                            .clickable(enabled = canChangeTab) { selectedTab = index }
                            .wrapContentHeight(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

            }
            Spacer(modifier = Modifier.height(24.dp))

            // Body inputs based on active tab
            when (selectedTab) {
                0 -> MomentInputForm(viewModel, onDismissRequest, editMoment, selectedIcon)
                1 -> ConsumableInputForm(viewModel, onDismissRequest, editConsumable, selectedIcon)
                2 -> InventoryInputForm(viewModel, onDismissRequest, editInventory, selectedIcon)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentInputForm(viewModel: LifeOrderedViewModel, onDismiss: () -> Unit, editMoment: Moment? = null, iconPath: String) {
    var title by remember { mutableStateOf(editMoment?.title ?: "") }
    var month by remember { mutableStateOf(editMoment?.month?.toString() ?: "1") }
    var day by remember { mutableStateOf(editMoment?.day?.toString() ?: "1") }
    var isLeapMonth by remember { mutableStateOf(editMoment?.isLeapMonth ?: false) }
    var isLunar by remember { mutableStateOf(editMoment?.isLunar ?: false) }
    var type by remember { mutableStateOf(editMoment?.type ?: "countdown") }
    var notes by remember { mutableStateOf(editMoment?.notes ?: "") }
    var repeatType by remember { mutableStateOf(editMoment?.repeatType ?: "none") }
    var themeColorHex by remember { mutableStateOf(editMoment?.themeColorHex ?: "#FF7E40") }
    var autoArchive by remember { mutableStateOf(editMoment?.autoArchive ?: true) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("时刻事件名称", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("例如：我的生日, 纪念日") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = WarmOrange,
                unfocusedBorderColor = Color(0xFFE8E5E0)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("时刻事件类型智能分类", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(LightCream)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val types = listOf("countdown" to "⏳ 倒数日", "countUp" to "🌱 正数日")
            types.forEach { (tKey, tLabel) ->
                val isSelected = type == tKey
                val bgColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) WarmOrange else Color.Transparent,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                )
                val textColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) Color.White else GrayTextMuted,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { type = tKey }
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        var year by remember { mutableStateOf(editMoment?.year?.toString() ?: Calendar.getInstance().get(Calendar.YEAR).toString()) }
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            ModalBottomSheet(onDismissRequest = { showDatePicker = false }) {
                WheelDatePicker(
                    initialYear = year.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR),
                    initialMonth = month.toIntOrNull() ?: 1,
                    initialDay = day.toIntOrNull() ?: 1,
                    onDateSelected = { y, m, d ->
                        year = y.toString()
                        month = m.toString()
                        day = d.toString()
                        showDatePicker = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("事件发生日期 (年-月-日一体化)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE8E5E0), RoundedCornerShape(16.dp))
                .clickable { showDatePicker = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${year}年 ${month}月 ${day}日",
                fontSize = 14.sp,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Text("📅", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Calendar system selection (Gregorian vs Lunar)
        Text("选择日期类型（公历 / 农历）", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(LightCream)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val systems = listOf(false to "☀️ 公历", true to "🏮 农历")
            systems.forEach { (lVal, label) ->
                val isSelected = isLunar == lVal
                val backgroundColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    animationSpec = androidx.compose.animation.core.tween(250),
                    label = "calendarTypeBackground"
                )
                val textColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) WarmOrange else GrayTextMuted,
                    animationSpec = androidx.compose.animation.core.tween(250),
                    label = "calendarTypeText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .clickable { isLunar = lVal }
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
        LaunchedEffect(isLunar, year, month) {
            if (!isLunar || !CalendarHelper.hasLeapMonth(year.toIntOrNull() ?: 0, month.toIntOrNull() ?: 0)) {
                isLeapMonth = false
            }
        }

        if (isLunar && CalendarHelper.hasLeapMonth(year.toIntOrNull() ?: 0, month.toIntOrNull() ?: 0)) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable { isLeapMonth = !isLeapMonth }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isLeapMonth,
                    onCheckedChange = { isLeapMonth = it },
                    colors = CheckboxDefaults.colors(checkedColor = WarmOrange)
                )
                Text("闰${month}月", fontSize = 13.sp, color = TextDark)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High fidelity Theme Swatch pick Row
        Text("选择专属纪念色罐", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val swatches = listOf(
                "#FF6B6B", "#FF8E53", "#FFA62B", "#FFD54F", "#FFF176",
                "#DCE775", "#AED581", "#81C784", "#4DB6AC", "#4DD0E1",
                "#4FC3F7", "#64B5F6", "#7986CB", "#9575CD", "#BA68C8",
                "#F06292", "#F48FB1", "#FFAB91", "#D7CCC8", "#90A4AE"
            )
            swatches.forEach { hex ->
                val isSelected = themeColorHex.lowercase() == hex.lowercase()
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(com.lifeordered.ui.theme.parseHexColor(hex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) TextDark else Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { themeColorHex = hex }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Precise Repeat option selection
        Text("定制生活重复周期", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(LightCream)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val repeats = listOf("none" to "不重复", "weekly" to "每周", "monthly" to "每月", "yearly" to "每年")
            repeats.forEach { (rKey, rLabel) ->
                val isSelected = repeatType == rKey
                val backgroundColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    animationSpec = androidx.compose.animation.core.tween(250),
                    label = "repeatBackground"
                )
                val textColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) WarmOrange else GrayTextMuted,
                    animationSpec = androidx.compose.animation.core.tween(250),
                    label = "repeatText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .clickable { repeatType = rKey }
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Memo note textfield
        Text("添置温馨备注 (不超 80 字)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { if (it.length <= 80) notes = it },
            placeholder = { Text("记录今天美好的小心情、地址或是特定的小嘱咐...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = WarmOrange,
                unfocusedBorderColor = Color(0xFFE8E5E0)
            ),
            shape = RoundedCornerShape(16.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Auto history archiver switch setting
        if (type == "countdown") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable { autoArchive = !autoArchive }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动历史智能归档", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("倒数天数归零至到期日时，将自动移入历史归档，保持首页温暖精简", fontSize = 11.sp, color = GrayTextMuted)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = autoArchive,
                    onCheckedChange = { autoArchive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WarmOrange,
                        uncheckedThumbColor = GrayTextMuted,
                        uncheckedTrackColor = Color(0xFFE8E5E0)
                    )
                )
            }
        }

        if (editMoment != null) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    viewModel.updateMomentArchiveStatus(editMoment, true)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedWarningText),
                border = BorderStroke(1.dp, RedWarningText.copy(alpha = 0.3f))
            ) {
                Text("📁 立即归档该时刻", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button (Clay 3D design)
        Button(
            onClick = {
                if (title.isBlank()) {
                    Toast.makeText(context, "请输入时刻名称哦 ✍️", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val yVal = year.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                val mVal = month.toIntOrNull() ?: 1
                val dVal = day.toIntOrNull() ?: 1
                if (isLunar && isLeapMonth && !CalendarHelper.hasLeapMonth(yVal, mVal)) {
                    Toast.makeText(context, "该年份没有对应闰月，请重新选择", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val validDate = if (isLunar) {
                    CalendarHelper.l2s(yVal, mVal, dVal, isLeapMonth) != null
                } else {
                    try {
                        Calendar.getInstance().apply {
                            clear()
                            isLenient = false
                            set(yVal, mVal - 1, dVal, 0, 0, 0)
                            timeInMillis
                        }
                        true
                    } catch (_: IllegalArgumentException) {
                        false
                    }
                }
                if (!validDate) {
                    Toast.makeText(context, "日期无效，请重新选择", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (title.isNotBlank()) {
                    if (editMoment != null) {
                        viewModel.updateMoment(
                            editMoment.copy(
                                title = title,
                                year = yVal,
                                month = mVal.coerceIn(1, 12),
                                day = dVal.coerceIn(1, 31),
                                isLunar = isLunar,
                                isLeapMonth = isLunar && isLeapMonth,
                                isYearly = (repeatType == "yearly"),
                                type = type,
                                notes = if (notes.isBlank()) null else notes,
                                repeatType = repeatType,
                                themeColorHex = themeColorHex,
                                autoArchive = autoArchive,
                                iconPath = iconPath
                            )
                        )
                    } else {
                        viewModel.addMoment(
                            title = title,
                            year = yVal,
                            month = mVal.coerceIn(1, 12),
                            day = dVal.coerceIn(1, 31),
                            isLunar = isLunar,
                            isLeapMonth = isLunar && isLeapMonth,
                            isYearly = (repeatType == "yearly"),
                            type = type,
                            notes = if (notes.isBlank()) null else notes,
                            repeatType = repeatType,
                            themeColorHex = themeColorHex,
                            autoArchive = autoArchive,
                            iconPath = iconPath
                        )
                    }
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(2.dp, Color(0xFF5D4037), RoundedCornerShape(24.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
        ) {
            Text(if (editMoment != null) "保存修改" else "添加到生活时刻", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ConsumableInputForm(viewModel: LifeOrderedViewModel, onDismiss: () -> Unit, editConsumable: Consumable? = null, iconPath: String) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editConsumable?.name ?: "") }
    var totalDays by remember { mutableStateOf(editConsumable?.totalDays?.toString() ?: "30") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("耗材项目名称", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("例如：净水芯, 猫卷, 面霜") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = WarmOrange,
                unfocusedBorderColor = Color(0xFFE8E5E0)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("可用寿命天数 (例如 30 天)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = totalDays,
            onValueChange = { totalDays = it.filter { char -> char.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = WarmOrange,
                unfocusedBorderColor = Color(0xFFE8E5E0)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Save
        Button(
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "请输入耗材名称哦 ✍️", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val dVal = totalDays.toIntOrNull() ?: 30
                if (name.isNotBlank()) {
                    if (editConsumable != null) {
                        viewModel.updateConsumable(
                            editConsumable.copy(
                                name = name,
                                totalDays = dVal,
                                iconPath = iconPath
                            )
                        )
                    } else {
                        viewModel.addConsumable(name = name, totalDays = dVal, iconPath = iconPath)
                    }
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(2.dp, Color(0xFF5D4037), RoundedCornerShape(24.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
        ) {
            Text(if (editConsumable != null) "保存修改" else "添加到到期追踪", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun InventoryInputForm(viewModel: LifeOrderedViewModel, onDismiss: () -> Unit, editInventory: Inventory? = null, iconPath: String) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editInventory?.name ?: "") }
    var currentQty by remember { mutableStateOf(editInventory?.currentQty?.toString() ?: "1") }
    var safeQty by remember { mutableStateOf(editInventory?.safeQty?.toString() ?: "3") }
    var unit by remember { mutableStateOf(editInventory?.unit ?: "袋") }
    var shelfLifeInput by remember { mutableStateOf(editInventory?.shelfLifeDays?.toString() ?: "") }

    val tags by viewModel.allTags.collectAsState(initial = emptyList())
    var selectedTag by remember {
        mutableStateOf(editInventory?.category ?: "居家")
    }

    // Adjust selectedTag if tags loaded are empty or not
    LaunchedEffect(tags) {
        if (editInventory == null && tags.isNotEmpty() && selectedTag == "居家") {
            selectedTag = tags[0].name
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("库存物品名称", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("例如：猫粮, 卷纸, 牛奶") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = WarmOrange,
                unfocusedBorderColor = Color(0xFFE8E5E0)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("当前数量", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentQty,
                    onValueChange = { currentQty = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = WarmOrange,
                        unfocusedBorderColor = Color(0xFFE8E5E0)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("安全红线数量", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = safeQty,
                    onValueChange = { safeQty = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = WarmOrange,
                        unfocusedBorderColor = Color(0xFFE8E5E0)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
            Column(modifier = Modifier.weight(0.8f)) {
                Text("物品单位", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    placeholder = { Text("袋/卷") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = WarmOrange,
                        unfocusedBorderColor = Color(0xFFE8E5E0)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Choice Category Area (Horizontal list of Custom tags Chips)
        Text("选择品类标签", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val isSelected = tag.name == selectedTag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) WarmOrange.copy(alpha = 0.15f) else Color.White)
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) WarmOrange else Color(0xFFE8E5E0),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedTag = tag.name }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tag.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) WarmOrange else GrayTextMuted
                    )
                }
            }
        }

        var autoConsumePeriod by remember { mutableStateOf(editInventory?.autoConsumePeriod ?: "none") }
        var autoConsumeQuantity by remember { mutableStateOf(if (editInventory?.autoConsumeQuantity == 0) "" else editInventory?.autoConsumeQuantity?.toString() ?: "") }

        Spacer(modifier = Modifier.height(16.dp))

        Text("开启自动消耗周期 (到期自动扣减库存)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(LightCream)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val periods = listOf("none" to "关闭", "day" to "每天", "week" to "每周", "month" to "每月")
            periods.forEach { (pKey, pLabel) ->
                val isSelected = autoConsumePeriod == pKey
                val bgColor by androidx.compose.animation.animateColorAsState(if (isSelected) WarmOrange else Color.Transparent)
                val textColor by androidx.compose.animation.animateColorAsState(if (isSelected) Color.White else GrayTextMuted)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { autoConsumePeriod = pKey }
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = pLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }
        
        if (autoConsumePeriod != "none") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = autoConsumeQuantity,
                onValueChange = { autoConsumeQuantity = it.filter { char -> char.isDigit() } },
                placeholder = { Text("每次周期循环扣除多少数量？") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = WarmOrange,
                    unfocusedBorderColor = Color(0xFFE8E5E0)
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("📅 保质天数", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = shelfLifeInput,
            onValueChange = { shelfLifeInput = it.filter { char -> char.isDigit() } },
            placeholder = { Text("天（为空则不到期）") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = WarmOrange,
                unfocusedBorderColor = Color(0xFFE8E5E0)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Save
        Button(
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "请输入物品名称哦 ✍️", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val cVal = currentQty.toIntOrNull() ?: 1
                val sVal = safeQty.toIntOrNull() ?: 3
                val shelfLife = shelfLifeInput.toIntOrNull()
                val purchaseDate = if (shelfLife != null) System.currentTimeMillis() else null

                if (name.isNotBlank()) {
                    if (editInventory != null) {
                        val expiry = shelfLife?.let { days ->
                            Calendar.getInstance().apply {
                                timeInMillis = editInventory.purchaseDate ?: System.currentTimeMillis()
                                add(Calendar.DAY_OF_YEAR, days)
                            }.timeInMillis
                        }
                        viewModel.updateInventory(
                            editInventory.copy(
                                name = name,
                                currentQty = cVal,
                                safeQty = sVal,
                                unit = unit.ifBlank { "袋" },
                                category = selectedTag,
                                iconPath = iconPath,
                                shelfLifeDays = shelfLife,
                                purchaseDate = purchaseDate ?: editInventory.purchaseDate,
                                expiryDate = expiry,
                                autoConsumePeriod = autoConsumePeriod,
                                autoConsumeQuantity = autoConsumeQuantity.toIntOrNull() ?: 1
                            )
                        )
                    } else {
                        viewModel.addInventory(
                            name = name,
                            currentQty = cVal,
                            safeQty = sVal,
                            unit = unit.ifBlank { "袋" },
                            category = selectedTag,
                            iconPath = iconPath,
                            shelfLifeDays = shelfLife,
                            purchaseDate = purchaseDate,
                            autoConsumePeriod = autoConsumePeriod,
                            autoConsumeQuantity = autoConsumeQuantity.toIntOrNull() ?: 1
                        )
                    }
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(2.dp, Color(0xFF5D4037), RoundedCornerShape(24.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
        ) {
            Text(if (editInventory != null) "保存修改" else "列入库存清单", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun WheelDatePicker(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    var year by remember { mutableStateOf(initialYear) }
    var month by remember { mutableStateOf(initialMonth) }
    var day by remember { mutableStateOf(initialDay) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // Year
        WheelItem(
            modifier = Modifier.weight(1f),
            items = (2000..2030).toList(),
            selectedItem = year,
            onItemSelected = { year = it; onDateSelected(year, month, day) }
        )
        // Month
        WheelItem(
            modifier = Modifier.weight(1f),
            items = (1..12).toList(),
            selectedItem = month,
            onItemSelected = { month = it; onDateSelected(year, month, day) }
        )
        // Day
        WheelItem(
            modifier = Modifier.weight(1f),
            items = (1..31).toList(),
            selectedItem = day,
            onItemSelected = { day = it; onDateSelected(year, month, day) }
        )
    }
}

@Composable
fun WheelItem(items: List<Int>, selectedItem: Int, modifier: Modifier = Modifier, onItemSelected: (Int) -> Unit) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    )
    
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.height(150.dp),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 50.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Text(
                text = "$item",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onItemSelected(item) },
                color = if (item == selectedItem) WarmOrange else GrayTextMuted,
                fontWeight = if (item == selectedItem) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
