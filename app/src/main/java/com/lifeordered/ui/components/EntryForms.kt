package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassChip
import com.lifeordered.ui.theme.GlassFieldLabel
import com.lifeordered.ui.theme.GlassPrimaryButton
import com.lifeordered.ui.theme.GlassSecondaryButton
import com.lifeordered.ui.theme.GlassTextField
import com.lifeordered.ui.theme.glassSurface
import com.lifeordered.ui.theme.parseHexColor
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.util.Calendar

/**
 * 三个录入表单。
 *
 * 从 `AddEntryBottomSheet` 拆出：宿主管布局与 Tab，这里只管「每种实体需要哪些字段」。
 * 表单本身的视觉已全部换成玻璃控件（[GlassTextField] / [GlassChip]）。
 */

// -----------------------------------------------------------------------------
// 时刻
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentInputForm(
    viewModel: LifeOrderedViewModel,
    onDismiss: () -> Unit,
    editMoment: Moment? = null,
    iconPath: String,
    saveAction: SheetSaveAction? = null
) {
    val notify = rememberNotifier()

    var title by remember { mutableStateOf(editMoment?.title ?: "") }
    var year by remember {
        mutableStateOf(editMoment?.year ?: Calendar.getInstance().get(Calendar.YEAR))
    }
    var month by remember { mutableStateOf(editMoment?.month ?: 1) }
    var day by remember { mutableStateOf(editMoment?.day ?: 1) }
    var isLunar by remember { mutableStateOf(editMoment?.isLunar ?: false) }
    var isLeapMonth by remember { mutableStateOf(editMoment?.isLeapMonth ?: false) }
    var kind by remember { mutableStateOf(editMoment?.type ?: "countdown") }
    var notes by remember { mutableStateOf(editMoment?.notes ?: "") }
    var repeatType by remember { mutableStateOf(editMoment?.repeatType ?: "none") }
    var themeColorHex by remember { mutableStateOf(editMoment?.themeColorHex ?: "#FF7E40") }
    var autoArchive by remember { mutableStateOf(editMoment?.autoArchive ?: true) }
    var cardColorIndex by remember { mutableStateOf(editMoment?.cardColorIndex ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 闰月只在「农历 + 该年该月确有闰月」时才允许勾选
    LaunchedEffect(isLunar, year, month) {
        if (!isLunar || !CalendarHelper.hasLeapMonth(year, month)) {
            isLeapMonth = false
        }
    }

    // 切换公历 / 农历时把已选日期换算过去。
    //
    // 不换算的话，用户先选公历 2024-12-31 再点「农历」，那组数字会被当成
    // 「农历腊月三十」——一个完全不同的日子，而且界面不会提示。
    // lastLunarMode 用来区分「用户切了类型」和「首次进入表单」，
    // 后者绝不能触发换算，否则编辑已有事件时日期会被悄悄改掉。
    var lastLunarMode by remember { mutableStateOf(isLunar) }
    LaunchedEffect(isLunar) {
        if (isLunar == lastLunarMode) return@LaunchedEffect
        lastLunarMode = isLunar
        if (isLunar) {
            CalendarHelper.getLunarDate(year, month, day)?.let { lunar ->
                year = lunar.year
                month = lunar.month
                day = lunar.day
                isLeapMonth = lunar.isLeapMonth
            }
        } else {
            CalendarHelper.l2s(year, month, day, isLeapMonth)?.let { solar ->
                year = solar.get(Calendar.YEAR)
                month = solar.get(Calendar.MONTH) + 1
                day = solar.get(Calendar.DAY_OF_MONTH)
                isLeapMonth = false
            }
        }
        // 兜底：万一换算没成功（历史数据里可能存着公历 31 日这类越界值），
        // 把日号退到该农历月真正存在的最后一天，
        // 否则用户会卡在「日期无效，请重新选择」却不知道哪里错了。
        if (isLunar) {
            var d = day
            while (d > 1 && CalendarHelper.l2s(year, month, d, isLeapMonth) == null) {
                d--
            }
            if (d != day) day = d
        }
    }

    // 两种历法都展示对应日期，保持日期入口的双行高度一致。
    val lunarSolarHint = remember(isLunar, year, month, day, isLeapMonth) {
        if (isLunar) {
            CalendarHelper.l2s(year, month, day, isLeapMonth)?.let { solar ->
                "对应公历 ${solar.get(Calendar.YEAR)} 年 ${solar.get(Calendar.MONTH) + 1} 月 " +
                    "${solar.get(Calendar.DAY_OF_MONTH)} 日"
            }
        } else {
            CalendarHelper.getLunarDate(year, month, day)?.let { lunar ->
                "对应农历 ${lunar.year} 年 " +
                    CalendarHelper.getLunarMonthName(lunar.month, lunar.isLeapMonth) +
                    CalendarHelper.getLunarDayLabel(lunar.day)
            }
        } ?: "日期超出换算范围"
    }

    if (showDatePicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDatePicker = false },
            sheetState = sheetState,
            containerColor = Glass.SheetFill,
            scrimColor = Glass.Scrim,
            shape = Glass.sheetShape
        ) {
            WheelDatePicker(
                initialYear = year,
                initialMonth = month,
                initialDay = day,
                isLunar = isLunar,
                initialLeapMonth = isLeapMonth,
                onDateSelected = { y, m, d, leap ->
                    year = y
                    month = m
                    day = d
                    isLeapMonth = leap
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlassPrimaryButton(
                text = "确定日期",
                onClick = { showDatePicker = false },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))

        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        GlassFieldLabel("事件名称")
        GlassTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "例如：老婆生日、结婚纪念日"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 「倒数 / 正数」与「公历 / 农历」合成一行。
        //
        // 原先这是两组、各占一行（共两行标签 + 两行 Chip）。合并后省掉一行高度，
        // 也让「这条记录是什么类型」集中在一处决策。
        //
        // 在 360dp 宽的屏幕上四个并排会溢出。所以去掉 emoji、只留两字，
        // 并给每个 Chip 等分宽度 + 单行省略，保证任何屏宽都不挤爆。
        // 两个语义组之间用一段弹性空隙分开，避免四颗胶囊看起来像同一组单选。
        // 标签和按钮行使用相同的权重与间距，让日期类型对齐公历按钮。
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassFieldLabel("计日方式", Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.weight(0.35f))
            GlassFieldLabel("日期类型", Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                GlassChip(
                    label = "倒数",
                    selected = kind == "countdown",
                    onClick = { kind = "countdown" },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                GlassChip(
                    label = "正数",
                    selected = kind == "countUp",
                    onClick = { kind = "countUp" },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 组间弹性空隙：比 Chip 之间的 6dp 宽，形成视觉分组
            Spacer(modifier = Modifier.weight(0.35f))

            Box(modifier = Modifier.weight(1f)) {
                GlassChip(
                    label = "公历",
                    selected = !isLunar,
                    onClick = { isLunar = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                GlassChip(
                    label = "农历",
                    selected = isLunar,
                    onClick = { isLunar = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel(if (isLunar) "农历日期" else "公历日期")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
                .clickable { showDatePicker = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLunar) {
                        "${year} 年 ${CalendarHelper.getLunarMonthName(month, isLeapMonth)}" +
                            CalendarHelper.getLunarDayLabel(day)
                    } else {
                        "${year} 年 ${month} 月 ${day} 日"
                    },
                    fontSize = 14.sp,
                    color = Glass.TextPrimary
                )
                if (lunarSolarHint != null) {
                    Text(
                        text = lunarSolarHint,
                        fontSize = 11.sp,
                        color = Glass.TextTertiary
                    )
                }
            }
            Text(text = "📅", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("重复周期")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val repeats = listOf(
                "none" to "不重复",
                "weekly" to "每周",
                "monthly" to "每月",
                "yearly" to "每年"
            )
            repeats.forEach { (key, label) ->
                GlassChip(
                    label = label,
                    selected = repeatType == key,
                    onClick = { repeatType = key }
                )
            }
        }

        if (kind == "countUp") {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "正数日显示「从起始日累计至今」的天数，重复周期只影响它在日历上出现的时间，不会清零。",
                fontSize = 11.sp,
                color = Glass.TextTertiary,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("卡片图标风格")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val styles = listOf(0 to "🎂", 1 to "🎁", 2 to "💗", 3 to "🎉")
            styles.forEach { (index, glyph) ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .then(
                            if (cardColorIndex == index) {
                                Modifier
                                    .clip(Glass.controlShape)
                                    .background(Glass.Accent.copy(alpha = 0.22f))
                                    .border(1.dp, Glass.Accent, Glass.controlShape)
                            } else {
                                Modifier.glassSurface(shape = Glass.controlShape)
                            }
                        )
                        .clickable { cardColorIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = glyph, fontSize = 19.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("纪念色（决定卡片辉光）")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val swatches = listOf(
                "#FF6B6B", "#FF8E53", "#FFA62B", "#FFD54F", "#FFF176",
                "#DCE775", "#AED581", "#81C784", "#4DB6AC", "#4DD0E1",
                "#4FC3F7", "#64B5F6", "#7986CB", "#9575CD", "#BA68C8",
                "#F06292", "#F48FB1", "#FFAB91", "#D7CCC8", "#90A4AE"
            )
            swatches.forEach { hex ->
                val isSelected = themeColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(hex))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            // 未选中用玻璃暗边（蓝灰半透明）：浅色主题下白色描边压在近白卡片上等于没有
                            color = if (isSelected) Glass.TextPrimary else Glass.StrokeBottom,
                            shape = CircleShape
                        )
                        .clickable { themeColorHex = hex }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("备注（不超过 80 字）")
        GlassTextField(
            value = notes,
            onValueChange = { if (it.length <= 80) notes = it },
            placeholder = "想记下的小心情、地址或嘱咐",
            singleLine = false,
            maxLines = 3
        )

        if (kind == "countdown") {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
                    .clickable { autoArchive = !autoArchive }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "到期自动归档",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Glass.TextPrimary
                    )
                    Text(
                        text = "倒数归零后自动移入归档，首页保持清爽",
                        fontSize = 11.sp,
                        color = Glass.TextSecondary
                    )
                }
                Switch(
                    checked = autoArchive,
                    onCheckedChange = { autoArchive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Glass.OnAccent,
                        checkedTrackColor = Glass.Accent,
                        uncheckedThumbColor = Glass.TextTertiary,
                        uncheckedTrackColor = Glass.Fill
                    )
                )
            }
        }

        if (editMoment != null) {
            Spacer(modifier = Modifier.height(14.dp))
            GlassSecondaryButton(
                text = "立即归档这个时刻",
                onClick = {
                    viewModel.updateMomentArchiveStatus(editMoment, true)
                    onDismiss()
                    notify("已归档")
                },
                contentColor = Glass.AccentDanger
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 保存闭包只注册给宿主顶栏，校验和数据写入保持原样。
        val save: () -> Unit = {
            if (title.isBlank()) {
                notify("请先填写事件名称")
            } else if (isLunar && isLeapMonth && !CalendarHelper.hasLeapMonth(year, month)) {
                notify("这一年没有闰${month}月，请重新选择")
            } else {
                val valid = if (isLunar) {
                    CalendarHelper.l2s(year, month, day, isLeapMonth) != null
                } else {
                    try {
                        Calendar.getInstance().apply {
                            clear()
                            isLenient = false
                            set(year, month - 1, day, 0, 0, 0)
                            timeInMillis
                        }
                        true
                    } catch (_: IllegalArgumentException) {
                        false
                    }
                }

                if (!valid) {
                    notify("日期无效，请重新选择")
                } else {
                    if (editMoment != null) {
                        viewModel.updateMoment(
                            editMoment.copy(
                                title = title,
                                year = year,
                                month = month.coerceIn(1, 12),
                                day = day.coerceIn(1, 31),
                                isLunar = isLunar,
                                isLeapMonth = isLunar && isLeapMonth,
                                isYearly = repeatType == "yearly",
                                cardColorIndex = cardColorIndex,
                                type = kind,
                                notes = notes.ifBlank { null },
                                repeatType = repeatType,
                                themeColorHex = themeColorHex,
                                autoArchive = autoArchive,
                                iconPath = iconPath
                            )
                        )
                        notify("已保存修改")
                    } else {
                        viewModel.addMoment(
                            title = title,
                            year = year,
                            month = month.coerceIn(1, 12),
                            day = day.coerceIn(1, 31),
                            isLunar = isLunar,
                            isLeapMonth = isLunar && isLeapMonth,
                            cardColorIndex = cardColorIndex,
                            type = kind,
                            notes = notes.ifBlank { null },
                            repeatType = repeatType,
                            themeColorHex = themeColorHex,
                            autoArchive = autoArchive,
                            iconPath = iconPath
                        )
                        notify("已添加「${title}」")
                    }
                    onDismiss()
                }
            }
        }

        // 每次成功重组都把最新闭包交给宿主顶栏，保证顶栏保存拿到的是当前状态。
        // 用 SideEffect 而非 LaunchedEffect：前者每次重组都执行，后者只在 key 变化时重启。
        SideEffect { saveAction?.action = save }

    }
}

// -----------------------------------------------------------------------------
// 耗材
// -----------------------------------------------------------------------------

@Composable
fun ConsumableInputForm(
    viewModel: LifeOrderedViewModel,
    onDismiss: () -> Unit,
    editConsumable: Consumable? = null,
    iconPath: String,
    saveAction: SheetSaveAction? = null
) {
    val notify = rememberNotifier()
    var name by remember { mutableStateOf(editConsumable?.name ?: "") }
    var totalDays by remember {
        mutableStateOf(editConsumable?.totalDays?.toString() ?: "30")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        GlassFieldLabel("耗材名称")
        GlassTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "例如：净水滤芯、猫砂、牙刷头"
        )

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("可用寿命（天）")
        GlassTextField(
            value = totalDays,
            onValueChange = { input -> totalDays = input.filter { it.isDigit() } },
            placeholder = "例如 30"
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "从今天开始计算，用完后会提醒你更换。",
            fontSize = 11.sp,
            color = Glass.TextTertiary
        )

        Spacer(modifier = Modifier.height(24.dp))

        val save: () -> Unit = {
            if (name.isBlank()) {
                notify("请先填写耗材名称")
            } else {
                val days = totalDays.toIntOrNull() ?: 30
                if (editConsumable != null) {
                    viewModel.updateConsumable(
                        editConsumable.copy(
                            name = name,
                            totalDays = days,
                            iconPath = iconPath
                        )
                    )
                    notify("已保存修改")
                } else {
                    viewModel.addConsumable(name = name, totalDays = days, iconPath = iconPath)
                    notify("已开始追踪「${name}」")
                }
                onDismiss()
            }
        }

        SideEffect { saveAction?.action = save }

    }
}

// -----------------------------------------------------------------------------
// 库存
// -----------------------------------------------------------------------------

@Composable
fun InventoryInputForm(
    viewModel: LifeOrderedViewModel,
    onDismiss: () -> Unit,
    editInventory: Inventory? = null,
    iconPath: String,
    saveAction: SheetSaveAction? = null
) {
    val notify = rememberNotifier()
    val tags by viewModel.allTags.collectAsState(initial = emptyList())

    var name by remember { mutableStateOf(editInventory?.name ?: "") }
    var currentQty by remember {
        mutableStateOf(editInventory?.currentQty?.toString() ?: "1")
    }
    var safeQty by remember { mutableStateOf(editInventory?.safeQty?.toString() ?: "3") }
    var unit by remember { mutableStateOf(editInventory?.unit ?: "袋") }
    var shelfLifeInput by remember {
        mutableStateOf(editInventory?.shelfLifeDays?.toString() ?: "")
    }
    var selectedTag by remember { mutableStateOf(editInventory?.category ?: "居家") }
    var autoConsumePeriod by remember {
        mutableStateOf(editInventory?.autoConsumePeriod ?: "none")
    }
    var autoConsumeQuantity by remember {
        mutableStateOf(
            if (editInventory == null || editInventory.autoConsumeQuantity == 0) {
                ""
            } else {
                editInventory.autoConsumeQuantity.toString()
            }
        )
    }

    LaunchedEffect(tags) {
        if (editInventory == null && tags.isNotEmpty() && selectedTag == "居家") {
            selectedTag = tags[0].name
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        GlassFieldLabel("物品名称")
        GlassTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "例如：猫粮、卷纸、牛奶"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                GlassFieldLabel("当前数量")
                GlassTextField(
                    value = currentQty,
                    onValueChange = { input -> currentQty = input.filter { it.isDigit() } },
                    placeholder = "1"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                GlassFieldLabel("安全线")
                GlassTextField(
                    value = safeQty,
                    onValueChange = { input -> safeQty = input.filter { it.isDigit() } },
                    placeholder = "3"
                )
            }
            Column(modifier = Modifier.weight(0.8f)) {
                GlassFieldLabel("单位")
                GlassTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    placeholder = "袋"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("品类标签")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                GlassChip(
                    label = tag.name,
                    selected = tag.name == selectedTag,
                    onClick = { selectedTag = tag.name }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("自动消耗周期")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val periods = listOf(
                "none" to "关闭",
                "day" to "每天",
                "week" to "每周",
                "month" to "每月"
            )
            periods.forEach { (key, label) ->
                GlassChip(
                    label = label,
                    selected = autoConsumePeriod == key,
                    onClick = { autoConsumePeriod = key }
                )
            }
        }

        if (autoConsumePeriod != "none") {
            Spacer(modifier = Modifier.height(8.dp))
            GlassTextField(
                value = autoConsumeQuantity,
                onValueChange = { input ->
                    autoConsumeQuantity = input.filter { it.isDigit() }
                },
                placeholder = "每个周期自动扣减多少？"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassFieldLabel("保质天数（留空表示不会到期）")
        GlassTextField(
            value = shelfLifeInput,
            onValueChange = { input -> shelfLifeInput = input.filter { it.isDigit() } },
            placeholder = "例如 180"
        )

        Spacer(modifier = Modifier.height(24.dp))

        val save: () -> Unit = {
            if (name.isBlank()) {
                notify("请先填写物品名称")
            } else {
                val cVal = currentQty.toIntOrNull() ?: 1
                val sVal = safeQty.toIntOrNull() ?: 3
                val shelfLife = shelfLifeInput.toIntOrNull()
                val purchaseDate = if (shelfLife != null) System.currentTimeMillis() else null
                val consumeQty = autoConsumeQuantity.toIntOrNull() ?: 1

                if (editInventory != null) {
                    val expiry = shelfLife?.let { days ->
                        CalendarHelper.addLocalDays(
                            editInventory.purchaseDate ?: System.currentTimeMillis(),
                            days
                        )
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
                            autoConsumeQuantity = consumeQty
                        )
                    )
                    notify("已保存修改")
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
                        autoConsumeQuantity = consumeQty
                    )
                    notify("已加入「${name}」")
                }
                onDismiss()
            }
        }

        SideEffect { saveAction?.action = save }

    }
}
