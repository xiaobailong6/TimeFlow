# 拾光流年 LifeOrdered — 功能清单与实现逻辑梳理

> 目的：为后续「功能 + UI 重构」提供准确的现状基线。本文只记录**当前代码实际实现的行为**，不描述期望行为。
> 基线版本：`versionName 1.1.0 / versionCode 2`，`applicationId com.aistudio.lifeordered.warmth`，namespace `com.lifeordered`。
> 技术栈：Kotlin + Jetpack Compose (Material3) + Room 5 + AlarmManager 本地通知。无网络层、无登录、无云端同步。

---

## 0. 总体架构现状

### 0.1 分层与数据流

```
MainActivity (ComponentActivity)
  └─ LifeOrderedViewModel (AndroidViewModel, 手写 Factory)
       ├─ LifeOrderedRepository  ← Room DAO (Flow)
       ├─ CalendarHelper (object, 无状态工具)
       └─ NotificationHelper (单例, AlarmManager + NotificationManager)

HomeScreen (唯一 Composable 入口, 以 activeTab 做“伪导航”)
  ├─ tab0 HomeScreenContent
  ├─ tab1 CalendarScreen
  ├─ tab2 ItemsScreen
  ├─ tab3 ProfileScreen
  ├─ detailScreenState != null → AllMomentsScreen / AllUpcomingScreen
  └─ 全局弹层: AddEntryBottomSheet / ActionConfirmBottomSheet / IconPicker
```

关键实现特征（重构需重点关注）：

| 项 | 现状 | 影响 |
|---|---|---|
| 导航 | **没有 Navigation 组件**。`HomeScreen` 用 `activeTabState`(int) + `detailScreenState`(String?) 两个 `mutableStateOf` 手工切换内容 | 无返回栈、无深链、无系统返回整合（仅 `detailScreen` 有一处 `BackHandler`）；Profile 内部的归档/回收站/排序页用三个 `Boolean` 各自持有，返回全靠回调 |
| 状态持有 | ViewModel 内直接使用 `androidx.compose.runtime.mutableStateOf`（如 `activeTabState`、`calendarFilterType`、`itemCategoryFilter`），而非 `StateFlow` | UI 状态与业务状态混在 VM，Compose 之外无法观测；重构需拆分 |
| 副导航 | `viewModel.itemCategoryFilter` 同时被「首页→低库存」跳转与「物品页筛选」复用 | 隐式跨页耦合，容易出现筛选残留 |
| 分区顺序 | 首页三大板块顺序存 SharedPreferences `life_ordered_prefs/home_section_order`，默认 `"moments,alerts,inventory"`；`homeSectionOrder` 是普通 getter，内部读 `mutableStateOf.value` | 读值路径上有 Compose 状态，能触发重组（排在后面的板块**不会**随排序刷新）；但 API 暴露的是普通 `List`，外部无从区分「可观察」与「快照」，重构时应改为 `StateFlow`/`mutableStateOf` 直接暴露 |
| 单 Activity | 只有 `MainActivity`，其余均为 Composable 页面 | 便于后续换成 Compose Navigation |
| 资源 | `res/values/strings.xml` 仅 1 条 `app_name`，其余文案**全部硬编码在 Kotlin 中** | 重构文案/i18n 需先抽取 |
| 无用的依赖 | `build.gradle.kts` 引入了 `retrofit / okhttp / moshi / logging-interceptor`，代码中**无任何使用** | 可直接移除 |

### 0.2 屏幕与组件清单

| 文件 | 行数 | 角色 |
|---|---|---|
| `MainActivity.kt` | 41 | 入口，权限申请，挂载 `HomeScreen` |
| `ui/viewmodel/LifeOrderedViewModel.kt` | 728 | 全部业务逻辑、种子数据、通知调度 |
| `ui/components/HomeScreen.kt` | 406 | 骨架 + 底部栏 + 全局弹层宿主 + 首页内容 |
| `ui/components/HomeScreenComponents.kt` | 829 | 卡片/按钮/底部导航等基础组件 |
| `ui/components/AddEntryBottomSheet.kt` | 1053 | 三大实体统一新增/编辑表单 + 滚轮日期选择器 |
| `ui/components/CalendarScreen.kt` | 498 | 月历 + 当日事件列表 |
| `ui/components/ItemsScreen.kt` | 525 | 库存列表 + 搜索 + 品类标签 + 标签管理 |
| `ui/components/ProfileScreen.kt` | 539 | 统计卡 + 设置项 + 危险区 |
| `ui/components/ArchivedMomentsScreen.kt` | 244 | 归档列表 |
| `ui/components/TrashBinScreen.kt` | 216 | 回收站 |
| `ui/components/AllMomentsScreen.kt` | 198 | 全部重要时刻列表 |
| `ui/components/AllUpcomingScreen.kt` | 217 | 全部到期提醒列表 |
| `ui/components/HomeSectionOrderingScreen.kt` | 174 | 首页板块排序 |
| `ui/components/IconPicker.kt` | 84 | Emoji 图标选择 |
| `ui/components/ActionConfirmBottomSheet.kt` | 110 | 通用二次确认弹层 |
| `ui/components/ClayIcons.kt` | 418 | Canvas 手绘黏土风格图标（8 个） |
| `ui/theme/Color.kt` / `Theme.kt` / `Type.kt` | 36 / 43 / 36 | 颜色、主题、字体 |
| `core/utils/CalendarHelper.kt` | 439 | 公历/农历全部日期算法 |
| `core/utils/NotificationHelper.kt` | 162 | AlarmManager 精确闹钟封装 |
| `core/utils/NotificationReceiver.kt` | 48 | 闹钟到点发通知 |

---

## 1. 数据模型与持久化

### 1.1 Room 实体

**`Moment`（时刻）— 表 `moments`**

| 字段 | 类型 | 语义 |
|---|---|---|
| `id` | Int PK | 自增 |
| `title` | String | 名称 |
| `year/month/day` | Int | 目标日期；(农历时) 表示农历年月日 |
| `isLunar` | Boolean | 农历开关 |
| `isLeapMonth` | Boolean | 闰月标记（v5 新增） |
| `isCompleted` | Boolean | 一次性事件已完成。代码默认 false，**但 UI 无任何入口置 true** |
| `isYearly` | Boolean | 是否每年重复（默认 true） |
| `cardColorIndex` | Int | 0-3，决定首页卡片使用哪个黏土图标（蛋糕/礼物/爱心/派对） |
| `type` | String | `"countdown"` 倒数日 / `"countUp"` 正数日 |
| `notes` | String? | 备注（表单限 80 字） |
| `repeatType` | String | `"none" / "weekly" / "monthly" / "yearly"` |
| `themeColorHex` | String | 卡片主色，默认 `#FF7E40` |
| `autoArchive` | Boolean | 到期后自动归档 |
| `isArchived` | Boolean | 已归档 |
| `iconPath` | String | 实为 **Emoji 字符串**（如 `"🎂"`），非路径 |
| `completionCount` | Int | 打卡次数 |
| `isDeleted/deletedAt` | Int / Long? | 软删除标记（回收站） |

**`Consumable`（耗材寿命）— 表 `consumables`**

`id / name / totalDays（总寿命天数）/ startDateMillis（起始时间，重置时刷新）/ cardColorIndex / iconPath / isDeleted / deletedAt`

派生逻辑（写在实体里）：
- `getRemainingDays(now)`：`now <= start` → 返回 `totalDays`；否则 `totalDays - localDaysBetween(start, now)`，下限 0。
- 采用**本地日历日**差值，避免夏令时导致一天被算成 23/25 小时。

**`Inventory`（库存物品）— 表 `inventories`**

`id / name / currentQty / safeQty / unit（默认“袋”）/ category（默认“居家”）/ iconPath / isArchived / isDeleted / deletedAt / shelfLifeDays? / purchaseDate? / expiryDate? / autoConsumePeriod（none|day|week|month|year）/ autoConsumeQuantity / lastAutoConsumeDate?`

派生逻辑：
- `isLow()` = `currentQty <= safeQty`（注意是 ≤，等于安全线即告警）
- `performAutoConsumption(now)`：从 `lastAutoConsumeDate` 起按周期推进，每跨一个周期扣 `autoConsumeQuantity`，累加至不超过 now；数量下限 0；循环上限 100000 次防脏数据死循环；无跨越周期返回 null。

**`TagModel`（品类标签）— 表 `categories`**

`id / name / colorHex`（`colorHex` 字段实际未被任何界面使用）

### 1.2 DAO 与迁移

- 4 个 DAO（`MomentDao / ConsumableDao / InventoryDao / TagDao`），统一模式：`getAllXxxFlow()`（`WHERE isDeleted = 0 ORDER BY id DESC`）、`getDeletedXxxFlow()`（`isDeleted = 1 ORDER BY deletedAt DESC`）、`insert`(REPLACE)、`update`、`delete`。Tag 无软删除。
- 数据库名 `life_ordered_db`，`version = 5`，`exportSchema = false`。
- 迁移：`1→2`（Moment 加 type/notes/repeatType/themeColorHex/autoArchive/isArchived）、`3→4`（三表加软删除 + Inventory 加保质期三字段）、`4→5`（Moment 加 isLeapMonth）。**缺少 2→3 迁移**，但配置了 `fallbackToDestructiveMigration()`，实际会清库重建。

### 1.3 首次启动种子数据（`seedDatabaseIfEmpty()`）

每个实体表为空时分别注入（因此重置后必然重新生成示例数据）：

- 时刻 3 条（日期为**相对今天动态计算**）：
  - 「老婆生日」= 今天 +3 天，公历、每年、autoArchive=true、主题 `#FF7E40`
  - 「爸爸生日 🎂」= 今天 +12 天，农历、每年、theme `#FFC107`
  - 「恋爱相守守护日」= 今天 −100 天，`countUp`、不重复、theme `#E91E63`
- 耗材 3 条：净水器滤芯（30 天，已用 22）、猫砂（15 天，已用 10）、牙刷头（90 天，已用 87）
- 库存 3 条：猫粮(1/安全2/袋/食品)、卫生纸(1/5/卷/居家)、猫砂(1/3/袋/居家)
- 标签 4 条：食品、洗护、居家、数码

---

## 2. 功能清单（按用户可见能力）

### 2.1 全局框架

| 编号 | 功能 | 实现位置 |
|---|---|---|
| F-01 | 底部导航 4 个 Tab：首页 / 日程 / 物品 / 我的 | `HomeScreen` BottomAppBar + `BottomNavItem`（`HomeScreenComponents.kt:760`） |
| F-02 | 底部栏中央「+」悬浮添加按钮（打开统一录入面板） | `HomeScreen.kt:91-117`，`testTag("main_add_button")` |
| F-03 | 详情页打开时**隐藏**底部导航栏 | `HomeScreen.kt:67`（`detailScreen == null` 才显示） |
| F-04 | 详情页支持系统返回键返回 | `HomeScreen.kt:132` `BackHandler(enabled = detailScreen != null)` |
| F-05 | 启动即申请通知权限（Android 13+） | `MainActivity.kt:26-30` |
| F-06 | 冷启动时执行一次「过期倒数日自动归档」扫描 | `ViewModel.init` → `checkAndArchiveExpiredMoments()` |
| F-07 | 冷启动时执行一次「库存自动消耗」结算 | `allInventories` 的 `map{}` 副作用中调用 `performAutoConsumption` 并写回 |

### 2.2 首页（Tab 0）

| 编号 | 功能 | 实现细节 |
|---|---|---|
| F-10 | 顶部标题 + 动态问候语（按小时 5 段文案） | `TopHeaderSection` + `ViewModel.getGreeting()` |
| F-11 | 右上角头像点击 → Toast「生活的主人：shyx@gmail.com 🌸」（写死） | `HomeScreen.kt:290` |
| F-12 | 渐变 Banner，显示「今天还有 N 件事项待处理」 | `OrganicBannerBlock`；N = 今日时刻数 + 剩余天数≤0 的耗材数 + 低库存件数 |
| F-13 | 「重要时刻」横向卡片列表 + 「全部 >」进度到全部列表页 | `SectionHeader(onAllClick = navigateToDetail("all_moments"))` |
| F-14 | 时刻卡片：标题、倒数/已过天数、日期标签（农历附公历换算）、打卡按钮 | `MomentCardItem` |
| F-15 | 时刻卡「✓ 已达成」打卡（带震动反馈） | `completeMoment()`：重复事件推进到下一周期；一次性事件仅 `completionCount+1` |
| F-16 | 时刻卡点击 → 若有备注弹出备注弹窗 | `combinedClickable(onClick)` |
| F-17 | 时刻卡**长按 → 弹出归档确认** | `combinedClickable(onLongClick)` → `ActionConfirmBottomSheet` |
| F-18 | 「到期提醒」横向耗材卡片 + 剩余天数 + 到期日 + 「重置」 | `ConsumableCardItem`；剩余 ≤3 天转红色样式 |
| F-19 | 「库存物品」区**只显示低库存项**，纵向列表 + 「补货/减一/编辑/删除」 | `HomeScreenContent` inventory 区块；「全部 >」跳转到物品页并置 `itemCategoryFilter = "low"` |
| F-20 | 三大板块顺序可自定义 | `homeSectionOrder` 循环渲染 |
| F-21 | 全空态展示 | `GlobalEmptyStateBlock` |
| F-22 | 首页操作即时 Toast 反馈 | 打卡「🎉 打卡成功！」、重置「🔄 已重置！」 |

> ⚠️ 注意：`HomeScreen.kt:375` 写入筛选值时用的是 `"low"`，而 `ItemsScreen` 判定低库存用的是 `"低库存"`（`ItemsScreen.kt:59`）。**「全部 >」跳转后筛选实际不生效**，是一处现存缺陷。

### 2.3 日程页（Tab 1，`CalendarScreen`）

| 编号 | 功能 | 实现细节 |
|---|---|---|
| F-30 | 月份切换（上/下月，跨年处理，切月重置选中日为 1 号） | `testTag("prev_month_btn")` / `next_month_btn` |
| F-31 | 周一为首日的月历网格，周末列着色 | `firstDayOfWeek = (DAY_OF_WEEK + 5) % 7` |
| F-32 | 每个日期格子显示公历日 + **农历日/节日名**（初一显示“X月”，节日显示“春节/元宵/端午/七夕/中秋/重阳/腊八/除夕”） | `CalendarHelper.getLunarDate` + `getLunarFestivalOrDayName` |
| F-33 | 有事件的日期显示小圆点 | `eventsMap.containsKey(dateKey)` |
| F-34 | 筛选 Chip：全部 / 重要时刻 / 耗材到期 | `viewModel.calendarFilterType`（null / "moment" / "consumable"） |
| F-35 | 选中日期下方「这一天的安排」事件列表 | `EventRowItem`，空态「这里目前很安静哦~」 |
| F-36 | 事件行可「完成/重置」（时刻=打卡，耗材=重置）与删除 | 带 Toast |
| F-37 | 事件聚合逻辑 `getEventsMapForMonth(year, month, filter)` | 遍历当月每一天 × 每个时刻（`isMomentOnDate`）+ 每个耗材（到期日落在该日），key 为 `yyyy-MM-dd` |

> `getEventsMapForMonth` 是 **O(天数 × 时刻数 + 天数 × 耗材数)** 的双重循环，且每次重组都全量重算（外层用 `remember(moments, consumables, currentYear, currentMonth, filterType)` 缓存），数据量增大后是性能热点。

### 2.4 物品页（Tab 2，`ItemsScreen`）

| 编号 | 功能 | 实现细节 |
|---|---|---|
| F-40 | 顶部显示「N 款家居物件 🏡」 | `inventories.size` |
| F-41 | 搜索框（按名称模糊匹配，忽略大小写） | `testTag("inventory_search_input")` |
| F-42 | 品类筛选 Chip 行：全部 / 低库存 / 各自定义标签 | `categories = ["全部","低库存"] + tags.map{name}` |
| F-43 | 标签管理入口（+ 圆形按钮）→ 弹窗新增/删除标签 | `ManageTagsDialog`；标签名限 6 字；`colorHex` 未使用 |
| F-44 | 库存卡：图标、名称、库存不足/储备充足徽标、安全线、± 数量调整、储备进度条、编辑、删除 | `InventoryGridCard`；进度 = `currentQty / safeQty`，clamp 到 0-1 |
| F-45 | 空态 + 「清除筛选」按钮 | `ItemsScreen.kt:183-214` |
| F-46 | 切到物品页时自动把筛选重置为「全部」 | `ViewModel.onTabSelected(2)` |

### 2.5 我的页（Tab 3，`ProfileScreen`）

| 编号 | 功能 | 实现细节 |
|---|---|---|
| F-50 | 头部「拾光与生活」+ 邮箱（写死 `shyx@gmail.com 🌸`） | — |
| F-51 | 「我的流年指数」卡片：3 项统计 — 有序运行天数（首次安装至今）、守护的时刻数（未归档）、管辖的物件数 | `StatBox`；`getInstallDays()` 读 `firstInstallTime` |
| F-52 | 「系统提醒推送开关」 | ⚠️ 仅 `remember { mutableStateOf(true) }` **本地 UI 状态 + Toast，未接任何通知系统**，是假开关 |
| F-53 | 「历史时刻归档 📦」→ 归档列表页 | `ArchivedMomentsScreen` |
| F-54 | 「回收站」→ 回收站页 | `TrashBinScreen` |
| F-55 | 「首页板块排序」→ 排序页 | `HomeSectionOrderingScreen` |
| F-56 | 「云备份与数据导出」 | ⚠️ 仅 Toast 提示一个不存在的路径，**未实现** |
| F-57 | 「重置所有应用数据」→ 危险区确认弹窗 → `clearAllTables()` + 重新种子 | `resetAllData()` |
| F-58 | 「关于拾光流年」→ 版本/致谢弹窗 | 文案写死 `v1.1.0` |

> Profile 内部三个子页使用 `var showXxx by remember` + if/else 早返回渲染，**没有统一导航与返回栈**，进入子页后底部栏仍显示。

### 2.6 列表/详情页

| 编号 | 页面 | 功能 | 实现细节 |
|---|---|---|---|
| F-60 | `AllMomentsScreen` | 未归档时刻全量列表（卡片：黏土图标、日期标签、剩余/已过/逾期天数、编辑、删除） | 数据源 `activeMoments` |
| F-61 | `AllUpcomingScreen` | 全部耗材列表（到期日、剩余天数、进度条、编辑、删除） | 数据源 `allConsumables` |
| F-62 | `ArchivedMomentsScreen` | 归档列表 + 顶部提示条 + 恢复 / 移入回收站 | `updateMomentArchiveStatus(m,false)` / `moveToTrash` |
| F-63 | `TrashBinScreen` | 三类软删除项汇总（时刻/耗材/库存）；恢复 / 彻底删除（二次确认） | 空态「回收站空空如也 🍃」 |
| F-64 | `HomeSectionOrderingScreen` | 上/下箭头调整三板块顺序 → 「保存」写 SharedPreferences | `moveItemCursor` 交换相邻项 |

### 2.7 录入与编辑（`AddEntryBottomSheet`）

单一 ModalBottomSheet，顶部**圆形 Emoji 图标选择器**（共享于三个表单）+ 三段式 Tab：重要时刻 / 到期提醒 / 库存物品。编辑态下 Tab 不可切换。

**F-70 重要时刻表单**
- 事件名称（必填）
- 类型：⏳ 倒数日 / 🌱 正数日
- 日期：点击打开二级 `ModalBottomSheet` + `WheelDatePicker`（滚轮：年 2000-2030、月 1-12、日 1-31）
- 日历类型：☀️ 公历 / 🏮 农历；选农历且该年该月存在闰月时，额外显示「闰X月」复选框（`hasLeapMonth` 探测）
- 主题色：20 个预设色板横向滚动
- 重复周期：不重复 / 每周 / 每月 / 每年
- 备注（限 80 字）
- 「自动历史智能归档」开关（仅倒数日可见）
- 编辑态额外提供「📁 立即归档该时刻」按钮
- 保存前校验：名称非空、闰月合法性、日期合法性（公历 `isLenient=false` 试构造，农历走 `l2s`）

**F-71 耗材表单**
- 名称（必填）、可用寿命天数（数字过滤，默认 30）

**F-72 库存表单**
- 名称（必填）、当前数量、安全红线数量、单位（默认“袋”）
- 品类标签 Chip 选择（来自 `TagModel`；新增时默认落到 `tags[0]`）
- 自动消耗周期：关闭 / 每天 / 每周 / 每月 + 「每次扣减数量」
- 保质天数（可空）；填写时会用 `System.currentTimeMillis()` 作为 `purchaseDate` 推算 `expiryDate`

> 已知不一致：`Inventory` 支持 `autoConsumePeriod = "year"`（模型与结算逻辑都支持），但**表单只提供到「每月」**。

---

## 3. 核心业务逻辑

### 3.1 日期算法（`CalendarHelper`）

统一原则：所有比较基于**本地日历日**，用 `startOfDay` + UTC 序数（`dateOrdinal`，以 `setGregorianChange(Long.MIN_VALUE)` 的纯前推格里高利历计算）求天数差，绝不拿毫秒除 86400000。

| 方法 | 作用 |
|---|---|
| `addLocalDays(millis, days)` | 按本地日历加天 |
| `localDaysBetween(a, b)` | 本地日差 |
| `l2s(y,m,d,isLeapMonth)` | 农历→公历，基于 `android.icu.util.ChineseCalendar`（完整天文数据），严格校验回读一致性；农历 30 日不存在时回退 29 日 |
| `getLunarDate(y,m,d)` | 公历→农历（含闰月标记） |
| `hasLeapMonth(year, month)` | 从农历月初起 40 天内探测是否存在闰月 |
| `getLunarDayName / getLunarFestivalOrDayName` | 农历日名（初一/初十/廿X/三十）与 8 个传统节日 |
| `solarDateInGregorianYear(...)` | 指定公历年内求某农历日期的公历日 |
| `getRemainingDays(...)` | 通用剩余天数（**死方法**：全仓无调用，仅参数形式存在。逻辑被 `calculateDays` 取代，但两者口径不同，见 §3.3） |
| `isMomentOnDate(m, y, mth, d)` | **日历页判定某时刻是否落在某天**，覆盖 weekly/monthly/yearly/none × 公历/农历，且要求选中日 ≥ 起始日 |
| `calculateDays(moment, now)` | **卡片天数唯一入口**（首页卡片、全部时刻列表、自动归档扫描都用它）。`countUp` 走单独分支；`countdown` 按 `repeatType` 分支。完整决策树见 §3.3.3 |
| `advanceRecurringMoment(m)` | 打卡后推进到下一周期。`type` **不参与**该函数（见 §3.3.7 缺陷 D-1）。`repeatType == "none"` 原样返回 |

> ⚠️ 两个「剩余天数」入口并存：`getRemainingDays`（按月/日 + isYearly）与 `calculateDays`（按 repeatType）。`isMomentOnDate` 里又同时考虑 `isYearly || repeatType == "yearly"`。**判断口径分散在 3 处**，是重构中最需要收敛的逻辑。

---

### 3.2 通知（AlarmManager）

- 单例 `NotificationHelper`，通道 `life_ordered_reminders`（名称「日程提醒」，IMPORTANCE_HIGH）。
- `scheduleNotification`：小于当前时间直接跳过（**不落库，进程重启后无法重建**）；Android 12+ 优先 `setExactAndAllowWhileIdle`，无精确闹钟权限则退化为 `set`；捕获 `SecurityException` 再退化。
- 通知 ID 命名空间（防冲突）：
  - 时刻：`momentId + 100000`
  - 耗材：`consumableId + 200000`
  - 库存：`inventoryId + 300000`
- 触发点：
  - 时刻告警：新增/恢复/打卡（重复事件）/编辑时，目标日 **09:00**，「重要时刻提醒 ⏰」
  - 耗材告警：新增/重置/编辑时，剩余天数当天 **10:00**，「备忘寿命到期 🕒」
  - 库存告警：**即时**通知 — 新增即低库存、数量减到 ≤ 安全线时；补回安全线以上则取消
- 删除/移入回收站时取消对应闹钟。

> 现存问题：所有闹钟只在「用户操作」时重排。应用被杀死/重启、系统重启后不重启闹钟（无 `BOOT_COMPLETED` 接收器）；「我的」页的推送开关是假的；`SCHEDULE_EXACT_ALARM` 在 Android 13+ 需引导用户授权。
### 3.3 倒数日 / 正数日 专项梳理（`Moment.type`）

这是整个 App 计算量最大、分支最密、且**公历/农历 × 倒数/正数 四象限交叉**的部分。本节按「数据契约 → 决策树 → UI 分叉 → 已知缺陷」四层展开。

#### 3.3.1 触发入口与状态变量

用户在 `AddEntryBottomSheet` 的 `MomentInputForm` 中通过两个互不依赖的开关决定行为：

| 表单控件 | 代码位置 | 写入字段 | 候选值 | 默认 |
|---|---|---|---|---|
| 「时刻事件类型智能分类」二段选择器 | `AddEntryBottomSheet.kt:250` | `type` | `"countdown"` ⏳ 倒数日 / `"countUp"` 🌱 正数日 | `"countdown"` |
| 「选择日期类型（公历 / 农历）」二段选择器 | `AddEntryBottomSheet.kt:339` | `isLunar` | `false` ☀️ 公历 / `true` 🏮 农历 | `false` |
| 「定制生活重复周期」四段选择器 | `AddEntryBottomSheet.kt:446` | `repeatType` | `none` / `weekly` / `monthly` / `yearly` | `"none"` |
| 「闰X月」复选框（条件显示） | `AddEntryBottomSheet.kt:388` | `isLeapMonth` | true / false | `false` |

**关键联动（保存时）**：`AddEntryBottomSheet.kt:595 / 612`
```
isYearly = (repeatType == "yearly")
```
→ `isYearly` **不是独立用户输入**，而是 `repeatType == "yearly"` 的冗余镜像。两个字段同时落库，后续逻辑却各自读其中一部分，这是 §3.3.7 缺陷 D-2 的根源。

**闰月合法性联动**：`AddEntryBottomSheet.kt:371-395`，`LaunchedEffect(isLunar, year, month)` 在非农历或该年该月无闰月时强制 `isLeapMonth = false`；保存前再校验一次（`:561`）。

#### 3.3.2 四象限组合与支持的重复语义

`type` 与 `isLunar` 正交，共 4 种组合。但**并非所有组合都被 `repeatType` 平等支持**：

| 象限 | `type` | `isLunar` | 语义 | 有效 `repeatType` | 备注 |
|---|---|---|---|---|---|
| ① 公历倒数 | countdown | false | 距公历日期还有几天 | none / weekly / monthly / yearly | 主路径，全覆盖 |
| ② 公历正数 | countUp | false | 从公历日期起已过几天 | **仅 none 有意义** | 见 D-4 |
| ③ 农历倒数 | countdown | true | 距农历日期还有几天 | none / weekly / monthly / yearly | `yearly` 走农历年搜索 |
| ④ 农历正数 | countUp | true | 从农历日期起已过几天 | **仅 none 有意义** | **UI 可保存但计算会退化，见 D-4** |

> `repeatType = "none"` 与 `"weekly"/"monthly"/"yearly"` 的语义差异是「一次性 vs 循环」，与 `type`（方向）是两个独立维度，**但 UI 把两者放在同一屏让用户自由组合，未做任何互斥约束**。

#### 3.3.3 计算决策树（`CalendarHelper.calculateDays`）

签名：`calculateDays(moment: Moment, referenceTimeMillis: Long = now): Int`。全部基于本地日历日（`startOfDay` + UTC 序数差），返回值语义随 `type` 翻转。

```
calculateDays(moment, now)
│
├─ 1. 求 original（原始起始日，仅取「年月日」，忽略时分秒）
│      if moment.isLunar → l2s(year, month, day, isLeapMonth)
│                         失败（该农历日不存在 / 闰月不存在）→ return 0      ← 静默返回 0，D-5
│      else               → localDate(year, month, day)
│                         失败（如 2月30日）→ return 0                      ← 静默返回 0，D-5
│
├─ 2. if (type == "countUp")  ← 正数日：唯一分支，完全忽略 repeatType
│      return daysBetween(original, today).coerceAtLeast(0)
│      语义：从起始日到今天已过天数。起始日在未来 → 被 clamp 为 0
│      注意：repeatType 在这里被彻底跳过（D-4）
│
└─ 3. else  ← 倒数日：类型为 countdown，按 repeatType 分四支
       ├─ "weekly"  → daysUntilWeekday(today, original.DAY_OF_WEEK)
       │               = (目标星期 - 今天星期 + 7) % 7
       │               含义：永远返回 0~6，当天即 0
       │               起算日「星期几」取自 original，与 moment.year 无关
       │
       ├─ "monthly" → if (isLunar) daysUntilLunarMonthly(...)
       │              else         daysUntilMonthly(today, moment.day)
       │              公历支：把今天 DAY_OF_MONTH 换成 moment.day；
       │                     若目标日 > 当月最大日则 clamp 到月末（如 31 号在 2 月→28/29）；
       │                     若成果早于今天 → 加一个月再 clamp 一次；
       │                     返回 0~31
       │              农历支：从 max(original, today) 起逐日推进，最多 370 天，
       │                     找第一个「农历日 == moment.day 且闰月标记一致 且 ≥ original」
       │                     的日子；找不到 return 0
       │
       ├─ "yearly"  → daysUntilYearly(today, moment)
       │              公历支：取今年 annualSolarDate(month, day)；
       │                     **2月29日特殊处理**：非闰年回退到 2月28日（annualSolarDate:53-54）；
       │                     若已早于今天 → 换成明年同月日；
       │                     返回可为负（若明年构造失败 → return 0）
       │              农历支：从「今天的农历年」起，在 currentLunarYear..+20 年范围内
       │                     找第一个 l2s(year, month, day, isLeapMonth) 存在且 ≥ today 的日子；
       │                     找不到 return 0
       │                     ⚠️ 闰月场景：若用户指定闰月，只有含该闰月的年份才匹配，
       │                        因此可能一次跳好几年（测试用例：2025 闰6月 → 2028）
       │
       └─ otherwise ("none" 或其他意外值)
                      → daysBetween(today, original)
                        语义：正数=还没到（剩余），负数=已过期（逾期），0=就是今天
                        ⚠️ 不 clamp，是唯一可能返回负数的分支
```

**返回值语义对照表**（重构时最易踩坑处）：

| 分支 | 返回范围 | 0 的含义 | 可能为负 | 陈旧日期表现 |
|---|---|---|---|---|
| countUp | 0 ~ +∞ | 就是起始日当天 | 否 | 数值随天数单调增长 |
| countdown + weekly | 0 ~ 6 | 就是今天 | 否 | 无意义，恒为 0~6 |
| countdown + monthly | 0 ~ 31 | 就是今天 | 否 | 自动滚动到本/下月 |
| countdown + yearly | 0 ~ 366 | 就是今天 | 否（构造失败时 0） | 自动滚动到本/明年 |
| countdown + none | −∞ ~ +∞ | 就是今天 | **是** | 停留在原日期并转为负数 |
| 任意 + 日期非法 | 恒 0 | 计算失败的哨兵值 | — | **错误被伪装成「就是今天」** |

#### 3.3.4 公历 vs 农历的实现差异

两条路径**不是同一算法的两个参数**，而是完全不同的实现：

| 对比项 | 公历路径 | 农历路径 |
|---|---|---|
| 底层 | `java.util.Calendar` + 纯前推格里高利历序数 | `android.icu.util.ChineseCalendar`（ICU 完整天文数据） |
| 换算方式 | 直接构造 | `l2s()` 换算，且**强制回读校验**（`year/month/day/IS_LEAP_MONTH` 四项都要吻合，否则判为非法） |
| 月末容错 | 2月29日 → 非闰年降级为 2月28日 | 农历30日不存在 → 降级为29日（`l2s:133`） |
| 搜索式求解 | 无，直接构造即可 | `yearly`/`monthly` 需**循环搜索**（年范围 +20 / 日范围 370 天） |
| 闰月概念 | 不存在 | `isLeapMonth` 独立布尔位，参与 `l2s` 与 `isMomentOnDate` 的相等判定 |
| 性能 | O(1) | `monthly` 最多 370 次 `ChineseCalendar` 换算；`yearly` 最多 21 次 |
| 春季边界 | 无 | 公历 1 月 1 日仍属**上一农历年**，故 `yearly` 分支必须先取 `lunarYearOf(today)` 再搜索（有测试用例覆盖） |

> ⚠️ 农历 `yearly` 的 20 年搜索窗口是**硬编码上限**。若某闰月布局在 20 年内不重复（理论上罕见但非不可能），会静默 `return 0`。另注意 `l2s` 在 `year < 1` 或 `month !in 1..12` 时直接返回 null，`monthly` 农历支在 370 天窗口内找不到同样 `return 0`。

#### 3.3.5 UI 分叉：`type` 与 `isLunar` 驱动的界面差异

**（a）表单层 —— `AddEntryBottomSheet.MomentInputForm`**

| UI 元素 | 出现条件 | 说明 |
|---|---|---|
| `type` 二段选择器 | 始终 | ⏳ 倒数日 / 🌱 正数日，`AddEntryBottomSheet.kt:240-279` |
| 「自动历史智能归档」开关 | **仅 `type == "countdown"`** | `AddEntryBottomSheet.kt:502`，正数日**完全不提供归档选项** |
| 「闰X月」复选框 | **仅 `isLunar && hasLeapMonth(year, month)`** | `AddEntryBottomSheet.kt:377`，探测该农历年该月是否真有闰月 |
| 日期轮盘 | 始终 | **注意：无论公历还是农历，`WheelDatePicker` 都只提供滑块（年 2000-2030 / 月 1-12 / 日 1-31），没有任何农历日名的可视化辅助**（`AddEntryBottomSheet.kt:1006-1025`），用户需自己知道农历日期对应的数字 |
| 颜色机制 | 始终 | `themeColorHex`（20 色板，决定**卡片底色**）；与 `cardColorIndex`（决定**黏土图标**）并存 |

> ⚠️ **保存时若 `editMoment == null`，`cardColorIndex` 由 `(0..2).random()` 随机生成**（`LifeOrderedViewModel.kt:384`），用户每次新建拿到的图标都是随机的，且**编辑时无法修改**。

**（b）首页卡片 —— `MomentCardItem`（`HomeScreenComponents.kt:332`）**

| 表现位 | countdown（倒数日） | countUp（正数日） |
|---|---|---|
| 副标题文案 | 「正在加速到来」 | 「已安稳度过」 |
| 天数大数字颜色 | `OrangePrimaryText`（暖橙） | `TextDark`（深灰） |
| 主打卡点按钮 | `onComplete(moment)` → **推进周期** / 累加 `completionCount` | 同左（**未做区别处理**，见 D-1） |
| 卡片底色 | `parseHexColor(moment.themeColorHex)` | 同左（无差异） |
| 图标 | 由 `cardColorIndex` 决定：0=ClayCakeIcon / 1=ClayGiftIcon / 2=ClayHeartsIcon / 3=ClayPartyIcon；**4 个以上时回退到 `iconPath` Emoji** | 同左 |
| 日期标签 | 公历 `"公历 M/D"` / 农历 `"农历 [闰]M/D (公历 YYYY年M月D日)"` | 同左（**日期标签不区分 type**） |
| 备注弹窗 | 点击卡片且 `notes` 非空时弹出 | 同左 |
| 长按 | 弹出归档确认 | 同左（**正数日无自动归档选项，但仍可手动归档**） |

> 注意倒数日**没有专门的「负数/逾期」视觉**：`calculateDays` 在 `repeatType=="none"` 时可返回负数，卡片会直接显示 `-3`，而**颜色仍是 `OrangePrimaryText`**（因为 `isCountUp` 为 false），不会变红。

**（c）全部时刻列表 —— `MomentListItem`（`AllMomentsScreen.kt:112`）**

| 表现位 | 逻辑 |
|---|---|
| 类型标签 | `"正数日"` / `"倒数日"`（`:163`） |
| 天数前缀 | countUp→`"已过 "`；countdown 且 `daysRemaining < 0`→`"逾期 "`；否则`"剩余 "`（`:171`） |
| 天数显示 | **统一取 `Math.abs(daysRemaining)`**（`:174`），所以「逾期 -3」会显示成「逾期 3」 |
| 天数颜色 | countdown 且 `daysRemaining < 0` → `RedWarningText`；否则 `baseColor`（即 `themeColorHex`）（`:177`） |
| 图标 | 由 `cardColorIndex` 决定，**只有 0/1/else 三个分支**（无 ClayPartyIcon），与首页卡片不一致（`:142-146`） |

> 与首页卡片的**不一致点**：① 逾期红色只在这里出现；② `ClayPartyIcon`（index 3）在首页可见但在此页会被渲染成 `ClayHeartsIcon`；③ 首图标尺寸/圆角不同（这里 16dp 圆角方框，首页同样 16dp 但底色 alpha 不同）。

**（d）日历页 —— `CalendarScreen`**

| 表现位 | 逻辑 |
|---|---|
| 是否出现在某天 | `CalendarHelper.isMomentOnDate(m, y, m, d)`（`:263`） |
| 事件行图标 | `isLunar ? "🏮" : "📅"`（`CalendarScreen.kt:464`） |
| 事件行副标题 | `"农历 [闰]M月D日"` 或 `"公历 M月D日"`（`:425`，**此处不区分 type**） |
| 操作按钮 | 时刻一律 `Icons.Default.Check`（打卡）；耗材一律 `Icons.Default.Refresh`（`:491`） |
| 卡片颜色 | `cardColorIndex` 映射到 4 个 Pastel 色（`:430-439`），**完全没用 `themeColorHex`** |

> ⚠️ **`isMomentOnDate` 与 `calculateDays` 对 `countUp` 的处理完全不同**：`isMomentOnDate` **完全不区分 `type`**（`:263` 起的分支只读 `isLunar`/`repeatType`/`isYearly`），而 `calculateDays` 把 `countUp` 提到最高优先级。结果是：一个 `countUp` 时刻在首页卡片显示「已过 100 天」，但在日历页**只在自己起始日那天**出现——两者对同一数据给出不同世界观。

**（e）归档列表 —— `ArchivedMomentItemRow`（`ArchivedMomentsScreen.kt:149`）**

| 表现位 | 逻辑 |
|---|---|
| 类型标签 | `"正数日"` / `"倒数日"`（`:199`） |
| 日期标签 | `"农历 [闰]M月D日"` / `"M月D日"`（`:197`，**公历分支无「公历」前缀**，与 `AllMomentsScreen` 不一致） |
| 图标 | **硬编码 `"📦"`**（`:180`），不使用 `themeColorHex` 也不使用 `cardColorIndex` |
| 边框色 | `parseHexColor(moment.themeColorHex)` @ alpha 0.2（`:162`） |

**（f）录入页以外的 `isLunar` 呈现汇总**

| 位置 | 公历表现 | 农历表现 |
|---|---|---|
| 首页卡片日期标签 | `公历 M/D` | `农历 [闰]M/D (公历 YYYY年M月D日)` |
| 全部时刻列表 | `M月D日`（**无「公历」前缀**） | `农历 [闰]M月D日` |
| 归档列表 | `M月D日`（**无前缀**） | `农历 [闰]M月D日` |
| 日历页事件行 | `公历 M月D日` | `农历 [闰]M月D日` |
| 日历页事件图标 | `📅` | `🏮` |
| 首页卡片图标 | 不区分，走 `cardColorIndex` | 同左 |

> 四处日期标签文案规则各不相同，是重构时应收敛为单一格式化函数的直接证据。

#### 3.3.6 正数日专属逻辑缺口

`type == "countUp"` 的实际影响面**只有 5 处**，其余全部与倒数日共用：

1. `CalendarHelper.calculateDays:321` — 计算分支（已过天数）
2. `HomeScreenComponents.kt:397` — 副标题「已安稳度过」
3. `HomeScreenComponents.kt:444` — 天数颜色 `TextDark`
4. `AllMomentsScreen.kt:163/171/177` — 类型标签 / 「已过 」前缀 / 颜色
5. `ArchivedMomentsScreen.kt:199` — 类型标签

**未被 `countUp` 感知、但语义上应当区分的点：**

- `completeMoment()`（`LifeOrderedViewModel.kt:399`）：正数日打卡后若 `repeatType != "none"` 会**推进周期**，导致「已过天数」倒退。一个「已过 100 天」的纪念日在打卡后可能变成「已过 0 天」。
- `checkAndArchiveExpiredMoments()`（`:446`）：已用 `type == "countdown"` 守卫 ✅，正数日不会被自动归档。
- 表单的「自动历史智能归档」开关：已用 `type == "countdown"` 守卫 ✅。
- `CalendarHelper.isMomentOnDate`（`:263`）：**未守卫** ❌，导致日历页行为与首页矛盾（见 3.2.5(d)）。
- 通知调度 `scheduleMomentNotification()`（`:317`）：**未守卫** ❌。正数日也会在起始日 09:00 收到「今天是 [X] 的日子哦，别忘了！」——对「已过 100 天」的纪念日语义错位。
- 卡片「✓ 已达成 (第 N 次)」：正数日显示打卡按钮语义不明。

#### 3.3.7 倒数日 / 正数日 缺陷清单

> 以下编号 D-1 ~ D-10 为**独立于 §6 的新增专项缺陷**，供重构排期（§3.3.7）。

| 编号 | 级别 | 缺陷 | 位置 | 影响 |
|---|---|---|---|---|
| **D-1** | P0 | `completeMoment()` 未区分 `type`。正数日（`repeatType != "none"`）打卡会推进 `year/month/day`，使「已过天数」归零甚至倒退 | `LifeOrderedViewModel.kt:399-417` | 正数日核心数据被破坏 |
| **D-2** | P0 | `isYearly` 与 `repeatType` 双写冗余：表单只写 `isYearly = (repeatType == "yearly")`，但 `isMomentOnDate` 读 `isYearly \|\| repeatType == "yearly"`，字段默认值又是 `true` | `AddEntryBottomSheet.kt:595/612`、`CalendarHelper.kt:267`、`Moment.kt:16` | 直接构造 `Moment(...)`（如种子数据、恢复归档）时 `isYearly` 可能与 `repeatType` 矛盾 |
| **D-3** | P0 | `isMomentOnDate` 不认 `type` → 日历页与首页对同一 `countUp` 时刻给出矛盾结果 | `CalendarHelper.kt:263` | 用户看到「已过 100 天」却不在日历上 |
| **D-4** | P1 | `calculateDays` 的 `countUp` 分支**完全忽略 `repeatType`**，但表单允许「正数日 + 每周/每月/每年」组合并正常保存 | `CalendarHelper.kt:321` | 用户选了「每年」但计算不生效，静默失效 |
| **D-5** | P1 | 日期非法 / 农历日不存在 / 20 年搜索窗超限时**静默 `return 0`**，与「就是今天」无法区分 | `CalendarHelper.kt:151/155/157/165/316/318/375/383` | 错误状态被伪装成正常值，UI 显示「0 天」而无任何提示 |
| **D-6** | P1 | 倒数日 `repeatType == "none"` 逾期时返回负数，首页卡片**直接显示负数且不变红**（红色只在 `AllMomentsScreen` 出现） | `HomeScreenComponents.kt:441-452` | 首页出现「-3 DAYS」且颜色为正常橙色 |
| **D-7** | P1 | 正数日仍会被调度通知，在起始日 09:00 推送「今天是 [X] 的日子哦」 | `LifeOrderedViewModel.kt:317-336` | 语义错位的打扰 |
| **D-8** | P2 | 四处日期标签格式化规则不一致（公历前缀有时有、有时无） | 见 3.2.5(f) 表 | 同一数据在不同页面显示不同 |
| **D-9** | P2 | 新建时刻时 `cardColorIndex` 随机取值 `(0..2)`，编辑时无法修改；`AllMomentsScreen` 只识别到 index 2，`ClayPartyIcon`（index 3）因随机上界为 2 而**永远不会出现** | `LifeOrderedViewModel.kt:384`、`AllMomentsScreen.kt:142-146` | 首页 4 个图标中第 4 个为死分支；用户无法选择图标风格 |
| **D-10** | P2 | 日期轮盘（`WheelDatePicker`）在农历模式下仍显示数字滑块，无农历日名辅助；年范围硬编码 2000-2030，超出即无法录入 | `AddEntryBottomSheet.kt:1006-1025` | 农历录入易错；2030 年后无解 |

#### 3.3.8 重构时建议的收敛方向

1. **`type` 与 `repeatType` 语义剥离**：明确 `type ∈ {countdown, countUp}` 为**方向**，`repeatType` 为**周期**；UI 层面在选 `countUp` 时禁用/隐藏 `weekly|monthly|yearly`，或在领域层明确 `countUp` 固定为一次性语义（D-4）。
2. **单一计算入口**：删除死方法 `getRemainingDays`（§3.1 已确认全仓无调用）；把 `isMomentOnDate` 改为**调用 `calculateDays` 的同一套分支**，使日历页与卡片共享口径（D-3）。
3. **用 `sealed` 结果替代哨兵 0**：让非法日期返回 `Result`/`null` 或显式 `MomentDays.Invalid`，UI 层可显示「日期无效」而非「0 天」（D-5）。
4. **`isYearly` 降级为派生属性**（`val isYearly get() = repeatType == "yearly"`）或直接删除，迁移时按 `repeatType` 回填（D-2）。
5. **抽出 `MomentDateLabelFormatter`** 与 `MomentDayColor(type, days, isToday)`，统一 4 处文案与颜色规则（D-6、D-8）。
6. **`countUp` 打卡语义单独定义**：明确是「+1 计数」还是「不允许打卡」，并据此决定按钮文案与 `completionCount` 语义（D-1）。
7. **补齐边界测试**：农历 yearly 的 20 年窗口、农历 30 日降级、公历 2/29 降级、`countUp + repeatType != none`、逾期负数在首页的呈现。现有 7 个日期用例可全部复用为回归网（§5）。

---

## 4. UI 与设计系统现状

| 项 | 现状 |
|---|---|
| 色彩 | `ui/theme/Glass.kt`：深墨蓝基底 `BaseTop #0B1020` / `BaseBottom #05070F`；4 个液体光斑 `GlowAmber` / `GlowRose` / `GlowCyan` / `GlowViolet`；玻璃填充 `FillSubtle 0x0AFFFFFF` / `Fill 0x14FFFFFF` / `FillStrong 0x1FFFFFFF` / `FillHeavy 0x29FFFFFF`；描边 `StrokeTop 0x59FFFFFF` / `StrokeMid` / `StrokeBottom`；文字三级 `TextPrimary/Secondary/Tertiary`；强调 `Accent #FFA05C` / `AccentCool #5CC8FF` / `AccentDanger #FF6B6B` / `AccentSuccess #5ED9A0` / `AccentWarning #FFC15C`；强调色前景 `OnAccent` / `OnAccentCool` / `OnAccentSuccess` / `OnAccentDanger`；面板 `SheetFill #E80E1424`；遮罩 `Scrim #CC04060D` |
| 主题 | `LifeOrderedTheme` **固定深色**：`GlassColorScheme = darkColorScheme(...)`、`surfaceTint = Transparent`、`scrim = Glass.Scrim`。`darkTheme` 参数保留但不生效——浅色底上半透明白玻璃不可见 |
| 字体 | `Type.kt` 仅覆盖 `bodyLarge`，其余走 Material 默认。`Theme.kt` 刻意不 import `material3.Typography`，用的是同包 `val Typography` |
| 风格 | 液态玻璃（Liquid Glass）：深墨蓝液体底 + 半透明玻璃薄片 + 上亮下暗渐变描边 + 带色调外阴影。**刻意不使用 `Modifier.blur()`**（需 API 31，`minSdk` 为 24）：柔和感来自径向渐变光斑的天然羽化，保证 API 24 与新机观感一致 |
| 手绘图标 | `GlassIcons.kt`（`Canvas` 绘制）：蛋糕、礼物、心动、庆祝。画法 = 半透明填充 + 上亮下暗描边 + 自身色调柔光。`MomentIcon` 按 `moment.type` 分发，用户自选 emoji 仍原样回落 |
| 自定义组件 | `ui/theme/GlassComponents.kt`：`GlassCard` / `GlassGroup` / `GlassDivider` / `GlassSectionHeader` / `GlassScreenHeader` / `GlassChip` / `GlassPrimaryButton` / `GlassSecondaryButton` / `GlassIconButton` / `GlassTextIconButton` / `GlassTextField` / `GlassFieldLabel` / `GlassEmptyState`；行内小按钮 `GlassEditButton` / `GlassDeleteButton` / `GlassCheckButton`（定义在 `HomeScreenComponents.kt`） |
| 交互反馈 | `ui/components/Feedback.kt`：`LocalSnackbarHostState` + `rememberNotifier()`，先 dismiss 再 show；全仓已无 `android.widget.Toast` |
| 背景 | `LiquidGlassBackground`（`GlassBackground.kt`）：4 个非对称径向光斑，静态不播动画，以保低端机滚动帧率 |
| 图片资源 | 仅 `banner_cozy_clay.png`（首页 Banner）与启动图标；其余视觉全部由 Compose 绘制 |
| 测试标签 | 关键控件带 `testTag`：`main_add_button`、`moment_item_{id}`、`moment_complete_{id}`、`consumable_item_{id}`、`item_inc_{id}`、`item_dec_{id}`、`inventory_search_input`、`category_tab_{name}`、`prev_month_btn`、`next_month_btn`、`notification_toggle_switch`、`history_archive_tile`、`trash_bin_tile`、`reorder_section_tile`、`backup_data_tile`、`reset_data_tile`、`about_app_tile`、`confirm_reset_btn` |

---

## 5. 测试现状

| 测试源集 | 文件 | 覆盖内容 |
|---|---|---|
| `src/logic_check` (unit, `testDebugUnitTest`) | `UnitVerification.kt` | 7 个用例，全部围绕日期：闰年 2/29、农历新年换算、农历→公历定位、闰月与非闰月区分、闰月年重复跳年（2025 闰6月→2028）、春节前农历年归属、农历按月重复、本地日差不受夏令时影响 |
| | `LogicVerification.kt` | 读 `R.string.app_name`，期望值「拾光流年」，与 `strings.xml` 一致 |
| | `UIVerification.kt` | Roborazzi 截图（Pixel 8）2 张：`greeting.png`（液体基底 + 品牌名）、`glass_components.png`（标题栏 / 卡片 / Chip / 主按钮同框，用于锁住玻璃层一致性） |
| `src/hardware_check` (androidTest) | `HardwareVerification.kt` | 断言包名为 `com.aistudio.lifeordered.warmth` |

`build.gradle.kts` 通过 `sourceSets` 把测试目录重定向到 `logic_check` / `hardware_check`。**业务逻辑（ViewModel / Repository / 库存结算 / 通知调度）目前零测试覆盖。**

---

## 6. 重构风险清单（按优先级）

### P0 — 功能缺陷 / 假实现
1. `ItemsScreen` 筛选值 `"low"` vs `"低库存"` 不一致，首页「库存物品 → 全部 >」跳转筛选失效。
2. 「系统提醒推送开关」不控制任何东西；「云备份与数据导出」是纯 Toast。
3. `LogicVerification` 断言 `app_name == "生活有序"`，与 `strings.xml` 的「拾光流年」冲突。
4. 无 `BOOT_COMPLETED` 接收器，重启后所有提醒丢失；提醒也未持久化，进程重启无法重建。

### P1 — 架构问题
5. 无 Navigation 组件，导航靠 `activeTab` + `detailScreenState` + 3 个 Boolean 拼装，无返回栈（系统返回键只在 `detailScreen` 生效）。
6. ViewModel 混入 `androidx.compose.runtime.mutableStateOf` 作为导航/筛选状态，UI 状态未分离。
7. `home_section_order` 以字符串列表存 SharedPreferences，键名散落在 VM，缺少默认值兜底与版本容错。
8. 首页单文件 406 行 + `AddEntryBottomSheet` 1053 行 + `HomeScreenComponents` 829 行，职责过载。
9. `getEventsMapForMonth` 双重循环全量计算，性能随数据增长退化。
10. 日期口径三处并存（`isMomentOnDate` / `calculateDays` / `getRemainingDays`），语义重叠易出偏差。

### P2 — 一致性 / 可维护性
11. `iconPath` 字段名与「Emoji 字符串」语义不符。
12. `cardColorIndex`（决定首页图标）与 `themeColorHex`（决定卡片底色）两套颜色机制并存，语义混乱。
13. `Moment.isCompleted`、`Inventory.isArchived`、`TagModel.colorHex`、`ClayFoodIcon/ClayPaperIcon/OrganicPill` 等均为死代码。
14. 全部文案硬编码在 Kotlin，仅 `app_name` 在资源里；无法做 i18n。
15. 依赖中 `retrofit/okhttp/moshi/logging-interceptor` 未使用。
16. 深色主题色板已定义但 UI 未接入。
17. `fallbackToDestructiveMigration()` + 缺失 2→3 迁移 → 升级即清库，用户数据有丢失风险（当前靠重新种子掩盖）。
18. 大量 `Toast` 反馈，未统一为 Snackbar / 内联提示。

---

## 7. 建议的重构切入点（供讨论）

1. **先补测试基线**：把日期口径收敛成单一入口（建议以 `repeatType` 为准，废弃 `isYearly` 或将其降级为派生字段），并为 `ViewModel` 的三个实体 CRUD + 库存结算写 unit test。现有 `logic_check` 的 7 个日期用例可直接复用为回归网。
2. **引入 Compose Navigation**，把 `activeTab` / `detailScreen` / Profile 三个 Boolean 统一为路由（含 Profile 子页），恢复系统返回栈。
3. **状态分层**：ViewModel 只暴露 `StateFlow`（UI State + Event），导航状态交给 Navigation/`SavedStateHandle`，筛选状态从 VM 里移出或明确归属到各页面。
4. **拆文件**：`AddEntryBottomSheet` 拆成 3 个表单 + 共享的图标/色板/日期子组件；`HomeScreen` 拆出纯骨架 + 各板块。
5. **补齐通知健壮性**：持久化待发提醒、加 `BOOT_COMPLETED` 重建、真正接通推送开关。
6. **修 P0 缺陷**（筛选值、假开关、假备份、测试断言），并同步清理死代码与无用依赖。
7. **占位/命名统一**：`strings.xml` 抽文案，`iconPath` → `icon`，统一颜色来源。

---

## 8. 重构执行记录（第一轮）

> 本节记录**已经落地**的改动，与前面 §0–§7 的「重构前基线」对照阅读。
> 施工约束：开发机只有 JDK 1.8 且未安装 Android SDK，**本轮无法执行 `gradlew` 编译与测试**，
> 因此所有结论来自逐文件静态核查 + 一次独立静态审查，未经真机/CI 验证。

### 8.1 新增文件

| 文件 | 作用 |
|---|---|
| `core/date/MomentKind.kt` | `MomentKind`（倒数/正数）、`MomentRepeat`（周期）枚举，带 `raw` 双向映射；数据库仍存原始字符串，**无需迁移** |
| `core/date/MomentDays.kt` | `sealed interface MomentDays`：`Countdown(days, isOverdue)` / `CountUp(elapsedDays)` / `Invalid` |
| `core/date/MomentSchedule.kt` | 日期计算**唯一权威实现**：`resolve()` / `occursOn()` / `advance()` / `startDateMillisOf()` |
| `core/utils/ReminderRescheduler.kt` | 提醒调度**唯一入口**，含 `rescheduleAll()` / `cancelAll()` / 推送开关读写 |
| `core/utils/BootReminderReceiver.kt` | 开机 / 应用升级后重建全部闹钟（`goAsync` + IO 协程） |
| `core/utils/DataExporter.kt` | 真实 JSON 导出（`org.json`）+ `FileProvider` 分享 |
| `ui/format/MomentPresentation.kt` | 时刻的**唯一呈现规则**：日期标签 / 天数文案 / 天数颜色 / 图标槽位 / 打卡文案 |
| `ui/format/InventoryFilter.kt` | 物品筛选常量，修掉 `"low"` vs `"低库存"` 的拼写分叉 |
| `ui/navigation/AppDestination.kt` | `AppDestination` 密封目的地 + `NavigationState` 返回栈 |
| `res/xml/file_paths.xml` | `FileProvider` 路径声明 |

### 8.2 缺陷修复对照

| 编号 | 状态 | 处理方式 |
|---|---|---|
| D-1 正数日打卡推进日期 | ✅ 已修 | `MomentSchedule.advance()`：`COUNT_UP` 只累加 `completionCount`，永不推进日期 |
| D-2 `isYearly` 双写冗余 | ✅ 已修 | 新增 `Moment.repeat`（派生自 `repeatType`），**全部判定逻辑不再读 `isYearly`**；列保留仅为免迁移，已在注释中标明 Legacy |
| D-3 日历与首页口径矛盾 | ✅ 已修 | `occursOn()` 与 `resolve()` 共用同一套周期判定；`countUp` 也尊重 `repeat` 的落点规则 |
| D-4 `countUp` 忽略 `repeatType` | ✅ 已澄清 | 语义定为「天数是累计值，周期只决定日历落点」，表单在 `countUp` 时显示说明文案 |
| D-5 非法日期静默返回 0 | ✅ 已修 | 返回 `MomentDays.Invalid`，UI 显示「—」/「日期无效」/ 红色，不再伪装成「就是今天」 |
| D-6 逾期负数不变红 | ✅ 已修 | `MomentPresentation.dayColor()` 对 `isOverdue` 返回红色，首页卡片与列表统一 |
| D-7 正数日仍发提醒 | ✅ 已修 | `ReminderRescheduler.rescheduleMoment()` 对 `COUNT_UP` 直接返回 |
| D-8 日期标签四处不一致 | ✅ 已修 | 统一走 `MomentPresentation.dateLabel()`；首页卡片额外带公历换算 |
| D-9 图标随机且第 4 种不可达 | ✅ 已修 | 移除 `(0..2).random()`，表单新增「卡片图标风格」四选一，`iconSlot()` clamp 到 0..3，列表页也支持全部 4 种 |
| D-10 农历录入无辅助 / 年份上限 2030 | 🟡 部分 | 年份范围放宽到 `1900..2100`；**农历日名辅助尚未实现**（见 8.4） |

| 原编号 | 状态 | 处理方式 |
|---|---|---|
| P0-1 筛选值 `"low"` / `"低库存"` | ✅ 已修 | 统一为 `InventoryFilter` 常量，并经导航参数 `Items(initialFilter)` 传入 |
| P0-2 假推送开关 | ✅ 已修 | 读写 SharedPreferences；切换时真正 `rescheduleAll()` / `cancelAll()`；调度侧也读同一开关 |
| P0-3 假数据备份 | ✅ 已修 | 真实写出 JSON 到 `getExternalFilesDir(null)/exports/`（无外部存储回退 `cacheDir`），并调起系统分享 |
| P0-4 `LogicVerification` 断言错值 | ✅ 已修 | 断言改为与 `strings.xml` 一致的「拾光流年」 |
| P0-5 重启后提醒丢失 | ✅ 已修 | 新增 `BootReminderReceiver`（`BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` / `QUICKBOOT_POWERON`）+ `RECEIVE_BOOT_COMPLETED` 权限；数据源为数据库，重建天然幂等 |
| P1-5 无导航 / 无返回栈 | ✅ 已修 | `AppDestination` + `NavigationState`；`BackHandler` 由 `canGoBack` 驱动；Profile 子页进入后自动隐藏底部栏 |
| P1-6 UI 状态混入 ViewModel | ✅ 已修 | 删除 `activeTabState` / `detailScreenState` / `navigateToDetail` / `onTabSelected` / `itemCategoryFilter` / `calendarFilterType` |
| P1-9 日历聚合双重循环 | ✅ 已优化 | 改为「先算每个条目命中的日期再入 map」；一次性事件走 O(1) 分支，不再逐日扫描 |
| P1-10 日期口径三处并存 | ✅ 已修 | 删除死方法 `CalendarHelper.getRemainingDays`；`calculateDays` / `isMomentOnDate` / `advanceRecurringMoment` 降级为**兼容门面**，全部委托 `MomentSchedule` |
| P2-13 死代码 | ✅ 部分 | 删除 `getRemainingDays`、`replenishInventory`、`consumeInventory`；`OrganicPill` / `ClayFoodIcon` / `ClayPaperIcon` 仍在（未引用，未删以免影响后续视觉重构） |
| P2-15 未使用依赖 | ✅ 已修 | 移除 `retrofit` / `converter-moshi` / `okhttp` / `logging-interceptor` / `moshi-kotlin` 与 `ksp(moshi-codegen)` |
| P2-18 库存自动消耗写在 Flow 里 | ✅ 已修 | 移出 `map{}`，改为初始化时显式 `applyInventoryAutoConsumption()` 结算一次 |

### 8.3 关键语义（重构后确定下来的口径）

```
MomentKind  = 方向：COUNTDOWN 显示「还有几天」 / COUNT_UP 显示「已过几天」
MomentRepeat = 周期：NONE / WEEKLY / MONTHLY / YEARLY，只决定「何时出现」

resolve(moment) → MomentDays
  COUNT_UP            → CountUp(从起始日累计至今，≥0)     // 打卡不重置
  COUNTDOWN + NONE    → Countdown(|天差|, isOverdue=天差<0) // 唯一可能逾期
  COUNTDOWN + WEEKLY  → Countdown(0..6)
  COUNTDOWN + MONTHLY → Countdown(0..31)，月末自动 clamp
  COUNTDOWN + YEARLY  → Countdown(0..366)，农历按闰月搜索
  日期非法            → Invalid

occursOn(moment, y, m, d) → Boolean   // 日历落点，对两种 kind 一视同仁
advance(moment)           → 打卡后状态；COUNT_UP 与 NONE 只 +1 计数
```

### 8.4 本轮未做 / 已知遗留

| 项 | 原因 |
|---|---|
| **未编译、未跑测试** | 开发机无 JDK 17 与 Android SDK。请在可用环境执行 `gradlew testDebugUnitTest` 与 `assembleDebug` |
| 文案 i18n | `strings.xml` 仍只有 `app_name`，其余文案仍硬编码在 Kotlin；全量抽取属于独立任务，本轮未做 |
| 深色主题 | `Theme.kt` 已有 dark scheme，但界面仍直接引用硬编码色常量；接入属于视觉重构范畴 |
| `AddEntryBottomSheet` 拆文件 | 该文件已增至约 1100 行，拆分（3 个表单 + 共享子组件）未做 |
| 农历日期录入辅助 | 轮盘仍只显示数字，未提供农历日名/节日提示 |
| Toast → Snackbar | 约 20 处 Toast 未统一 |
| 数据库迁移 | 未新增迁移；`fallbackToDestructiveMigration()` 与缺失的 2→3 迁移仍在。因 `Moment` 只新增了 getter-only 计算属性，schema 未变，本轮不引入迁移风险 |
| 真实云备份 | 当前为本地 JSON 导出 + 系统分享，未接任何云服务 |
| 农历 yearly 20 年窗口 | 仍为硬编码上限，超窗返回 `Invalid`（不再静默返回 0） |

### 8.5 测试用例变更

- `LogicVerification`：断言由「生活有序」改为「拾光流年」，与 `strings.xml` 一致。
- `UnitVerification` 的 `leap lunar annual recurrence…`：原先写死 `assertEquals(2028, next.year)`。
  该常数是否成立取决于 ICU 农历数据（2028 是否为闰六月），且与测试名表达的意图无关。
  已改为**契约断言**：必须向后推进、月份不变、仍为闰月、目标年确实存在该闰月、
  且中间年份都不存在该闰月。这样既验证「跳过」行为，也不依赖某个硬编码年份。
- 其余 6 条日期用例的签名与语义均保持不变（`calculateDays` 仍返回 `Int`，
  `advanceRecurringMoment` 仍返回 `Moment`）。

### 8.6 首次 CI 编译失败与修复

第一轮推送后 GitHub Actions 在 `:app:compileDebugKotlin` 失败，日志共报 2 个错误（`kspDebugKotlin` 与 manifest 处理均已通过，说明 Room 实体与清单无误）：

| 位置 | 错误 | 根因 | 修复 |
|---|---|---|---|
| `MomentSchedule.kt:261` | `Unresolved reference 'getActualMaximum'` | `daysUntilSolarMonthly` 第二次调用落在 `apply {}` **作用域之外**，`getActualMaximum(...)` 没有隐式接收者 | 改为显式 `candidate.getActualMaximum(...)` |
| `MomentPresentation.kt:63` | `Unresolved reference 'year年'` | 字符串模板写了 `$year年`。**中文是合法的 Kotlin 标识符字符**，编译器把 `year年` 当成一个简单名，而不是 `year` + 字面量「年」 | 改为 `${year}年` |

第二类问题是中文项目里的系统性陷阱，已全仓扫描 `\$name` 紧跟中文字符的模式，除本次修复点外无其他命中（仅剩说明性注释）。
`$lunarLabel（` 之所以没报错，是因为全角括号 `（` 属于标点而非字母，能正确终止标识符。

---

### 8.7 第二轮：整体 UI 改为液态玻璃

> 范围：`ui/theme` 设计系统整体替换 + 全部界面文件 + 4 个底部面板/表单。
> 这一轮换的不是配色，而是**视觉成立的前提**：黏土拟物靠「浅色底 + 实色块 + 大圆角阴影」，
> 液态玻璃靠「深色底 + 半透明 + 透出背后光斑」。两者的组件无法共存，
> 所以只能整套换掉，不存在「先改一半」的中间态。

#### 8.7.1 新增文件

| 文件 | 行数 | 作用 |
|---|---|---|
| `ui/theme/Glass.kt` | 157 | 唯一色板与几何令牌。`object Glass` 共 38 个成员：基底 2、光斑 4、玻璃填充 4、描边 3、文字 3、强调 5、强调色前景 4、`SheetFill`、`Scrim`、嵌套 `object Radius`（5 档圆角）、6 个预构造 `Shape`。文件尾保留 `parseHexColor` |
| `ui/theme/GlassModifiers.kt` | 126 | 6 个 `Modifier` 扩展：`liquidGlass`（完整玻璃薄片）/ `glassSurface` / `glassOutline` / `glassTopHighlight` / `glassPill` / `accentGlow` |
| `ui/theme/GlassBackground.kt` | 121 | `LiquidGlassBackground`：垂直基底渐变 + 4 个非对称径向光斑 |
| `ui/theme/GlassComponents.kt` | 503 | 13 个玻璃组件：`GlassCard` / `GlassGroup` / `GlassDivider` / `GlassSectionHeader` / `GlassScreenHeader` / `GlassChip` / `GlassPrimaryButton` / `GlassSecondaryButton` / `GlassIconButton` / `GlassTextIconButton` / `GlassTextField` / `GlassFieldLabel` / `GlassEmptyState` |
| `ui/components/GlassIcons.kt` | 398 | 4 个手绘玻璃图标（蛋糕 / 礼物 / 心动 / 庆祝）+ 6 个私有绘制辅助（`whiteGlassFill`、`tintedGlassFill`、`glassEdge`、`iconGlow`、`glassRoundRect`、`drawGlassHeart`） |
| `ui/components/Feedback.kt` | 80 | `LocalSnackbarHostState` + `rememberNotifier()`，先 dismiss 再 show |
| `ui/components/EntryForms.kt` | 709 | 玻璃化过程中从 `AddEntryBottomSheet` 拆出的 3 个表单与共享子组件 |
| `ui/components/WheelDatePicker.kt` | 222 | 玻璃化滚轮日期选择器 |

#### 8.7.2 删除文件

| 文件 | 行数 | 原因 |
|---|---|---|
| `ui/theme/Color.kt` | 旧色板 | 17 个黏土色名（`WarmCreamBg` / `PastelPeach` / `OrangePrimaryText` …）全部由 `Glass.*` 取代。保留一层别名只会让「全仓液态玻璃」这个说法站不住 |
| `ui/components/ClayIcons.kt` | 418 | 9 个黏土图标 + 8 个 Clay 配色常量。黏土图标是**不透明实色**，在深墨蓝底上会变成刺眼的浅色贴纸，并且把背后的彩色光斑整块遮死——恰好破坏了玻璃最重要的特征 |

#### 8.7.3 关键设计决策

| 决策 | 理由 |
|---|---|
| **不做真实模糊** | `Modifier.blur()` / `RenderEffect` 需 API 31，而 `minSdk = 24`。写 `if (SDK_INT >= 31)` 分支会让新老设备观感分裂——一半毛玻璃一半平板色，比完全不模糊更糟。柔和感改由径向渐变光斑的天然羽化提供，API 24 与新机一致 |
| **主题固定深色** | 半透明白玻璃只在深色底上成立；浅色底上它退化成一块灰。`LifeOrderedTheme` 的 `darkTheme` 参数保留以兼容调用点，但不参与决策 |
| **图标重写而非调色** | 黏土图标的观感来自「比底色更亮」，在深底上这个关系反了。新画法 = 半透明填充 + 上亮下暗描边 + 自身色调柔光，光向与面板描边保持一致，否则图标像是从下方打光、视觉上「粘不住」 |
| **`SheetFill` 用 91% 不透明** | 面板里是表单，用户要读字、要对齐数字。但仍是 9% 透明——让底层色调渗进来，面板才像「浮在液体上」而不是「贴在上面」 |
| **`Scrim` 用 `0xCC04060D` 而非纯黑** | 纯黑压在深墨蓝基底上会让整屏掉进死黑，光斑全部消失。主题与三个底部面板共用同一个值，避免遮罩深浅不一 |
| **兼容旧名别名层全部删除** | 调用点已全部迁移完毕，保留别名只是延迟清理成本 |
| **`Cream*Button` → `Glass*Button` 重命名** | 6 个文件 18 处。`GlassEditButton` / `GlassDeleteButton` / `GlassCheckButton` 已是 `GlassIconButton` 的薄包装，旧名是黏土时代的残留，会误导后续读者 |
| **`UIVerification` 改为渲染设计系统** | 原来只渲染一行文字，且文案还是旧名「生活有序」，与 `strings.xml` 的「拾光流年」对不上。玻璃靠半透明成立，任何组件退回不透明实色都会在截图里立刻暴露 |

#### 8.7.4 重复硬编码色的收敛

| 原写法 | 现写法 | 收敛范围 |
|---|---|---|
| `Color(0xFF1A1008)` 散落在强调色按钮前景 | `Glass.OnAccent` | 并补齐同族 `OnAccentCool` / `OnAccentSuccess` / `OnAccentDanger`，让 `Theme.kt` 的 `onPrimary` / `onSecondary` / `onTertiary` / `onError` 全部令牌化 |
| `scrimColor = Color(0xCC04060D)` | `Glass.Scrim` | `ActionConfirmBottomSheet` / `AddEntryBottomSheet` / `EntryForms` 三处 + `Theme.kt` 的 `scrim` |

核查结果：`0xFF1A1008` 与 `0xCC04060D` 现在**只出现在 `Glass.kt`**（含一处说明性注释），无外泄。

#### 8.7.5 与 §8.4 遗留表的对应关系

§8.4 是第一轮的记录，其中三行已被本轮闭环：

| §8.4 行 | 现状 |
|---|---|
| 深色主题 | ✅ 已闭环。主题固定深色，界面不再引用任何硬编码色常量 |
| `AddEntryBottomSheet` 拆文件 | ✅ 已闭环。拆为 `EntryForms.kt`（709 行，3 个表单 + 共享子组件）与 `AddEntryBottomSheet.kt`（218 行，仅容器与状态） |
| Toast → Snackbar | ✅ 已闭环。统一走 `Feedback.kt` 的 `rememberNotifier()`；全仓 `android.widget.Toast` 只剩 3 处说明性注释，无代码调用 |

其余 6 项（i18n、农历录入辅助、数据库迁移、真实云备份、农历 20 年窗口、未编译）仍未做。

---

### 8.8 静态校验（无本地编译环境下的替代手段）

本轮改动面很大（新增 8 个文件、删除 2 个、修改 15 个），而开发机依然没有 Android SDK，
`gradlew` 跑不起来。编译器能挡住的错误只能靠机械核查补位，共做了 6 类：

| 校验项 | 手段 | 结果 |
|---|---|---|
| 括号平衡 | 逐文件统计 `{}` 与 `()` 的净数差 | 47 个 `.kt` 文件全部为 0 差异 |
| `Glass.*` 引用可解析 | 提取 `Glass.kt` 全部 `val` / `fun` 与嵌套 `object Radius`，与全仓 `Glass.X` 引用求差集 | 34 个引用全部有定义（`Glass.Radius` 为嵌套 object，非缺失） |
| 旧标识符残留 | 正则扫 17 个旧色名 + `ClayIcon` + `Cream\w*Button` | 仅 1 处，位于 `GlassIcons.kt:24` 的说明性注释（解释黏土图标为何被删） |
| 硬编码强调色外泄 | 扫 `0xFF1A1008` / `0xCC04060D` 在 `Glass.kt` 之外的出现 | 0 处 |
| `android.widget.Toast` 残留 | 全仓扫 `Toast.makeText` / `Toast.LENGTH` | 3 处命中全在注释中，无代码调用 |
| `Theme.kt` 的 `Typography` 导入 | 精确匹配 `import androidx.compose.material3.Typography` | 无该 import；3 处命中均为第 62–64 行的警示注释 |

这些脚本是一次性的，放在会话临时目录，不入库。

#### 8.8.1 一处真实编译隐患的复盘

重构版 `Theme.kt` 初稿加了一行 `import androidx.compose.material3.Typography`。

`Type.kt` 在**同一个包**里声明了 `val Typography = Typography(...)`，而它自己**必须**
`import androidx.compose.material3.Typography`（模板写法，靠同名类构造实例）。

Kotlin 的名字解析顺序里，**显式导入优先于同包声明**。于是 `Theme.kt` 里的
`typography = Typography` 被解析成 Material3 的**类**，而不是那个 `val` 实例，
类型不匹配 → 编译失败。已删除该 import，并在 `MaterialTheme(` 上方留 3 行注释把约束钉住。

这类错误的共性是**只报一处、且报错位置离真正原因很远**：`Type.kt` 自身完全合法，
错误会出现在 `Theme.kt` 的 `typography =` 参数上，而报错信息只说类型不符，
不会提示「你遮蔽了同包声明」。同类风险还有 `Color`——本轮把旧色板文件 `Color.kt` 整个删掉，
正是为了消掉 `Color` 这个最容易与 `androidx.compose.ui.graphics.Color` 撞名的同包声明。

#### 8.8.2 静态校验管不到的部分

以下只能等 CI 或真机：

| 项 | 说明 |
|---|---|
| `:app:compileDebugKotlin` | 类型推断、重载决议、作用域接收者（第一轮就栽在 `getActualMaximum` 的接收者上） |
| `:app:compileDebugUnitTestKotlin` | `UIVerification` 新增调用 `LiquidGlassBackground` / `GlassCard` / `GlassChip` / `GlassPrimaryButton`，`GlassScreenHeader` 的 `title` / `subtitle` 参数名也需实测 |
| Roborazzi 截图 | `greeting.png` / `glass_components.png` 的实际像素，静态核查完全无法预判 |
| 滚动帧率 | `LiquidGlassBackground` 静态不播动画是有意为之，但每张卡片都有 `accentGlow` 外阴影，低端机滚动表现需真机确认 |

#### 8.8.3 顺带清掉的未使用 import

第一轮遗留的告警清理，本轮一并做完。判定方式：逐行取出 `import` 的末段简单名，
在**同一文件内大小写敏感**地统计该名出现次数；次数为 1（只有 import 行本身）即判定未使用。
共 14 处：

| 文件 | 删除的 import |
|---|---|
| `HomeScreen.kt` | `foundation.shape.RoundedCornerShape`、`material3.Text`、`unit.sp` |
| `ProfileScreen.kt` | `theme.GlassChip`、`theme.GlassIconButton`、`theme.liquidGlass` |
| `IconPicker.kt` | `foundation.background`、`foundation.border`、`ui.draw.clip`、`foundation.shape.RoundedCornerShape` |
| `HomeScreenComponents.kt` | `layout.PaddingValues`、`foundation.shape.RoundedCornerShape` |
| `CalendarScreen.kt` | `material3.Icon` |
| `ItemsScreen.kt` | `ui.graphics.Color` |

两个易踩的假阳性，值得记下来：

1. **大小写不敏感扫描会漏判**。`ItemsScreen.kt` 的 `Color` 用不敏感匹配能命中 14 次，
   但全是 `color = ...` 参数名；`CalendarScreen.kt` 的 `Icon` 同理，命中 3 次全是 `icon = ...`。
   必须用大小写敏感匹配才能得到真实答案。
2. **`getValue` / `setValue` 永远不能删**。它们只出现在 `by mutableStateOf(...)` 的委托语法里，
   正文中一次都不会出现，任何「未被引用」的判据都会把它们标红。扫描时直接排除。

清理后全仓显式 import 的未使用数为 **0**。剩余 16 条「命中」全是 `import xxx.*` 通配导入，
逐符号扫描对它们无意义，属工具能力边界而非真实告警。

---

### 8.9 第二次 CI 编译失败与修复（液态玻璃轮）

推送后 `:app:compileDebugKotlin` 失败，日志共 2 个错误，**都是同一类根因**：

| 位置 | 错误 | 根因 | 修复 |
|---|---|---|---|
| `CalendarScreen.kt:330` | `Unresolved reference 'weight'` | `DayCell` 被声明成普通 `@Composable fun`，但正文里用 `Modifier.weight(1f)`。`weight` 不是顶层函数，而是 `RowScope` 的成员扩展 | 声明改为 `private fun RowScope.DayCell(...)`，并补 `import androidx.compose.foundation.layout.RowScope` |
| `GlassBackground.kt:107` | `Unresolved reference 'align'` | 同上：`GlowOrb` 是普通 `@Composable fun`，正文里用 `Modifier.align(alignment)`，而 `align` 是 `BoxScope` 的成员扩展 | 声明改为 `private fun BoxScope.GlowOrb(...)`；`BoxScope` 原本已因 `content: @Composable BoxScope.() -> Unit` 导入 |

#### 为什么静态核查抓不到

§8.8 的 6 类核查里没有一类能发现它：括号是配平的、`Glass.*` 全都解析得到、
标识符没有残留。**唯一能判定的信息是「这个函数声明有没有作用域接收者」**，
而这需要理解 `weight` / `align` 不是普通函数而是 scope 成员——属于语义层，不是文本层。

这类错误的通用形态是：**把一段本来写在 `Row { }` / `Box { }` 里的内联代码抽成独立函数时，
忘了把接收者也一起搬过去**。抽取本身是对的（`DayCell` 有 60 行，`GlowOrb` 有 25 行，
留在主 composable 里会把日历月视图和背景组件撑成一片），错的只是签名。

两种修法里选了「加接收者」而不是「加 `modifier: Modifier` 参数」：
后者会让调用点必须写 `modifier = Modifier.weight(1f)`，而 `weight` 在调用点同样需要
`RowScope`——只是把同一个问题从定义处挪到调用处，并没有解决。

#### 已确认的排查范围

全仓 `.weight(` 共 28 处、`.align(` 共 7 处，逐处确认了所在作用域：

- 修复后这 2 处是仅有的「独立函数内使用 scope 成员」
- 其余 33 处全部位于 `Row { }` / `Column { }` / `Box { }` 的 inline lambda 内，接收者天然存在
- `matchParentSize` / `alignBy` / `alignByBaseline` 全仓 0 处使用，无同类风险

CI 的 2 个错误即为全集：Kotlin 前端会一次性报告整个模块的解析错误，不会因前一个错误而停止。

#### 本轮仍未验证到的部分

编译在 `compileDebugKotlin` 就中断，所以下面这些**依旧没有跑过**，需下一次 CI 确认：

| 任务 | 待验证点 |
|---|---|
| `compileDebugUnitTestKotlin` | `UIVerification` 的调用。已提前核对签名：`GlassCard(modifier, shape, fill, elevation, glow, contentPadding, onClick, content)`、`GlassScreenHeader(title, subtitle, modifier, onBack, actions)`、`GlassChip(label, selected, onClick, modifier, accent)`、`GlassPrimaryButton(text, onClick, modifier, enabled, accent)`、`LiquidGlassBackground(modifier, content)` —— 测试里的具名参数与尾随 lambda 均匹配 |
| `testDebugUnitTest` | 7 个日期用例 + 1 个 `app_name` 断言 + 2 张 Roborazzi 截图 |
| `assembleDebug` | 打包 |

---

### 8.10 第三轮：透明白色玻璃 / 动画 / 轮盘吸附 / 图标 / 农历重做

本轮由 6 条用户反馈驱动，全部落在源码里，不含「计划」或「待办」。

#### 8.10.1 六项需求的落点

| # | 需求 | 实现方式 | 主要文件 |
|---|---|---|---|
| ① | 去掉玻璃卡片后的灰黑方块 | 删除全部 `Modifier.shadow()`，改用同色 `colorHalo()` 柔光 | `GlassModifiers.kt` |
| ② | 整体改为透明白色 | 重写全套颜色令牌 + `lightColorScheme` | `Glass.kt`、`Theme.kt`、`GlassBackground.kt` |
| ③ | 增加 UI 动画 | 新增 `Motion.kt`：页面淡入 + 按压回弹 | `Motion.kt` + 16 个调用点 |
| ④ | 轮盘吸附、不能选到空白行 | 中线判据 + `animateScrollToItem` 吸附；列项按真实数量生成 | `WheelDatePicker.kt` |
| ⑤ | 重设计图标（日历样式） | 矢量自适应图标 + 5 档密度 PNG 兜底 | `res/drawable/ic_launcher_*`、`res/mipmap-*` |
| ⑥ | 农历日期误报 + 直接选农历 | 农历查表 + 轮盘三列农历 + 类型切换双向换算 | `LunarYear.kt`、`CalendarHelper.kt`、`WheelDatePicker.kt`、`EntryForms.kt` |

#### 8.10.2 灰黑方块的真实成因

不是「阴影颜色没调好」，而是**平台降级**：

`Modifier.shadow(elevation, shape, ambientColor, spotColor)` 里的 `ambientColor` / `spotColor`
只在 **API 28+** 生效。本项目 `minSdk = 24`，在 API 24–27 的设备上这两个参数被**直接忽略**，
阴影一律退化为**纯黑矩形**，糊在卡片后面。于是「柔和投影」在低版本机器上
变成了用户看到的灰黑方块。

替代方案 `colorHalo(color, shape, spread, layers = 4)`：用 4 层**同心、递减透明度**的
同色调圆角矩形叠加模拟外发光。它只依赖 `drawBehind` + `Brush`，API 24 全量可用，
且颜色可控（取卡片自身色调），不会再出现黑色。

#### 8.10.3 为什么是「透明白」而不是「白卡片」

关键约束：**白玻璃放在白底上等于没画**。所以这次不是把深色值简单反转，而是同时换了基底：

| 令牌 | 值 | 作用 |
|---|---|---|
| `BaseTop` / `BaseBottom` | `0xFFF7F9FE` / `0xFFE9EFFB` | 近白但带蓝调，让白色填充有边界可依 |
| `GlowAmber` / `GlowRose` / `GlowCyan` / `GlowViolet` | `0xFFFFA76B` / `0xFFFF86B4` / `0xFF6FD0F0` / `0xFFA08CF0` | 背景光斑，降饱和以适配浅色 |
| `FillSubtle` → `FillHeavy` | `0x59` → `0xE0` 白 | 四档填充，越靠前景越不透明 |
| `StrokeTop` / `StrokeMid` / `StrokeBottom` | `0xFFFFFFFF` / `0xA6FFFFFF` / `0x4D6B7CA8` | 上亮下暗描边，玻璃厚度感的唯一来源 |
| `TextPrimary` / `TextSecondary` / `TextTertiary` | `0xFF16203A` / `0xFF5B6480` / `0xFF8B93A8` | 深墨蓝文字，保证浅底对比度 |
| `Accent` 系 | `0xFFF97316` / `0xFF2E90D0` / `0xFFE5484D` / `0xFF17A673` / `0xFFD99100` | 比深色主题时**压深**一档，否则白底上发飘 |

配套的语义陷阱：原深色主题里用**白色**当高亮的图标、描边、条纹，在浅色主题下
「白叠白」等于没画。这些位置全部改成**比卡片更深或更饱和**的颜色，而不是保留白色。

#### 8.10.4 动画：为什么没有 `AnimatedContent`

`gradle/libs.versions.toml` 里没有 `androidx-compose-animation` 的别名，而本轮约定
**不新增 Gradle 依赖**。因此所有动效只用 `androidx.compose.animation.core`
（`animateFloatAsState` / `spring` / `tween` / `FastOutSlowInEasing`）配合 `graphicsLayer`：

| API | 行为 |
|---|---|
| `ScreenFadeIn(key, ...)` | 换页时透明度 0→1、同时上浮 12dp；`key` 相同不重播 |
| `pressScale(...)` | 按压缩放到 `pressedScale`，松手用 `DampingRatioMediumBouncy` 回弹 |
| `pressableClickable(...)` | 上面两者的封装，调用点只需换一个词 |

`graphicsLayer` 只影响绘制、不触发重新测量，长列表里不会掉帧。

#### 8.10.5 一个静态检查抓不到的视觉缺陷：`graphicsLayer` 的位置

`pressScale` 最初写成 `return this.graphicsLayer { ... }`（**追加**在链条末尾）。
这不会编译报错，但效果是错的：

修饰符链的绘制顺序是「外层先画、内层后画」。把 `graphicsLayer` 追加在
`.background(...)` 之后，它只包住**后续内容**——于是按压时缩小的是卡片里的
文字和图标，卡片自己的填充与描边纹丝不动，看起来像「内容往下陷」，
而不是「整块玻璃被按下去」。

改成 `return Modifier.graphicsLayer { ... }.then(this)`（**前置**到外层）后，
调用点 `modifier.背景().pressableClickable()` 就能整块缩放，且 14 个调用点无需改动。
唯一需要调整的是 `GlassCard`：它用 `.then(Modifier.pressableClickable(...))` 传入
**空** `Modifier` 作接收者，前置的层会落在 `liquidGlass` 内层，所以把点击挂到
`modifier` 上再串玻璃外观。

#### 8.10.6 轮盘吸附的判据

视口 160dp、项高 34dp、`contentPadding` 上下各 63dp。第 `i` 项顶边在偏移 0 时位于
`63 + 34i`，中心 `80 + 34i`，而视口中心恒为 80 —— 所以 `animateScrollToItem(i)`
**恰好**把第 `i` 项摆到正中，不需要额外算偏移。

吸附目标：拖动中 `firstVisibleItemScrollOffset = o`，当 `o > itemHeight / 2` 时中线
更靠近下一项，取 `first + 1`，否则取 `first`。同一判据也用于滚动中实时高亮
（`derivedStateOf`），因此视觉与落点永远一致。

**「不能选到空白行」不是靠吸附实现的，而是列项根本不存在空白**：日列是
`(1..maxDay)`、农历月列是该年真实月份（含闰月）、年列是 `LunarTable.MIN_YEAR..MAX_YEAR`，
没有任何补齐到 31 的占位项。

`snapshotFlow { isScrollInProgress }` 的首帧就是 `false`，若无条件
`animateScrollToItem`，它会把自己翻成 `true→false` 再触发同一个收集器，
形成**自激循环**（空转 + 每次回调一次 `onItemSelected`）。所以只有
`target != first || offset != 0` 时才滚；值没变也不回调。

#### 8.10.7 农历误报的根因

旧实现直接对 `ChineseCalendar` 写 `set(YEAR, 2024)`。但 ICU 的
`ChineseCalendar.YEAR` **不是公历年**（基准是 `CHINESE_EPOCH_YEAR = -2636`），
写入公历年会落到公元前，回读校验必然失败 → 一律返回 `null` → 界面永远提示
「该日期在农历中不存在」。所以这不是「个别日期不存在」，而是**任何日期都不存在**。

现在的做法：

1. `LunarYear.kt` 新增 `LunarTable`：用 ICU 的 `ChineseCalendar` **只读**
   `MONTH` / `DAY_OF_MONTH` / `IS_LEAP_MONTH` 三个可靠字段，从每个农历年的正月初一
   起逐月扫描月界，折叠成 `LunarYear(year, newYearMillis, months)` /
   `LunarMonth(month, isLeapMonth, days, startMillis)` 的预计算表。
   公历年与农历年的对应靠 `scanChineseNewYear` 定位正月初一。
2. `CalendarHelper.l2s` / `getLunarDate` 全部改为查表，文件内**不再写任何
   `ChineseCalendar` 字段**。
3. 轮盘在农历模式下三列分别是「农历年 / 农历月（含闰月）/ 农历日」，
   选出来的就是农历日期本身，**不再走「选公历再换算农历」**。
4. 表单里切换公历 ↔ 农历时用 `getLunarDate` / `l2s` **双向换算**已选日期。
   不换算的话，用户先选公历 2024-12-31 再点「农历」，那组数字会被当成
   「农历腊月三十」——一个完全不同的日子，且界面不会提示。
5. 兜底：若换算没成功（历史数据里可能存着公历 31 日这类越界值），把日号退到
   该农历月真正存在的最后一天，避免用户卡在「日期无效」却不知道哪里错了。

保存前仍保留 `l2s(...) != null` 校验，但轮盘已只能产出合法农历日期，
这条校验从「必失败」变成了「兜底」。

#### 8.10.8 图标

- `ic_launcher_background.xml` / `ic_launcher_foreground.xml`：纯矢量日历造型
  （圆角板身 + 装订环 + 高亮日期格）。
- `mipmap-anydpi-v26/ic_launcher.xml`、`ic_launcher_round.xml`：自适应图标，
  引用上面两个矢量层。
- API 24–25 不支持自适应图标，因此另用 GDI+（`System.Drawing`）生成
  `mdpi` / `hdpi` / `xhdpi` / `xxhdpi` / `xxxhdpi` 五档 PNG（方形 + 圆形各一套）。
  **必须先删同名 `.webp`**，否则同目录下两种格式冲突，构建报重复资源。

#### 8.10.9 本轮静态核查结果

本机无 Android SDK，不能编译，只能用脚本做文本层核查：

| 检查项 | 结果 |
|---|---|
| 括号 / 大括号平衡（全仓 `.kt`） | 通过 |
| `Glass.*` 令牌：引用 vs 定义（43 个成员） | 全部有定义 |
| `Motion.kt` 导出符号 | `Motion` / `ScreenFadeIn` / `pressScale` / `pressableClickable` 均存在 |
| 农历 API 符号 | `l2s` / `LunarTable` / `LunarMonth` / `LunarYear` 均存在 |
| `shadow(` 残留 | 仅 2 处注释说明，无实际调用 |
| 未使用 import | 0（含本轮删掉的 4 个 `clickable`） |
| `Color.White` 残留（浅色主题高风险） | 仅 `GlassModifiers.kt:134` 一处，用于高光渐变，属正确用法 |
| 旧命名残留（`ClayIcons` / `Cream*` / `Clay*`） | 仅 `GlassIcons.kt:24` 一处注释 |
| 按压反馈覆盖 | 16 个实际调用点（卡片、主按钮、图标按钮、Chip、底部导航、悬浮按钮、日历格、加减步进器、设置行） |

#### 8.10.10 本轮仍未验证到的部分

同前两轮，编译与测试都必须在 CI 上跑。以下是本轮改动里**只能靠人工核对**的点：

| 任务 | 待验证点 |
|---|---|
| `compileDebugKotlin` | `Motion.kt` 的 `graphicsLayer` 前置写法、`GlassCard` 的 `clickable` 局部变量、`EntryForms` 新增的两个 `LaunchedEffect`、`WheelDatePicker` 的 `rememberUpdatedState` 三处用法 |
| `compileDebugUnitTestKotlin` | `UIVerification` 对 `GlassCard` / `GlassScreenHeader` / `GlassChip` / `GlassPrimaryButton` / `LiquidGlassBackground` 的具名参数调用 |
| `testDebugUnitTest` | 农历用例：`l2s(2024,1,1)` 对应 2024-02-10、2023 闰二月与正月不同日、2025 闰六月在中间年份必须为 `null`、`getLunarDate(2024,2,10).year == 2024` |
| 真机 / 模拟器 | 浅色玻璃的实际对比度、按压回弹手感、轮盘吸附是否有回弹抖动、自适应图标在 API 26+ 与 PNG 兜底在 API 24–25 的呈现 |

其中「浅色玻璃的实际对比度」是最可能需要二次调参的一项：`Fill` 系列透明度是按
深色主题的经验值换算的，没有真机目视校对，数值可能需要回调。

---

### 8.11 第三次 CI 编译失败与修复（透明白玻璃轮）

CI 日志 `logs/0_Test and build APK.txt`。`kspDebugKotlin` 与全部资源任务
（`generateDebugResources` / `mergeDebugResources` / `processDebugResources` /
`packageDebugResources`）均通过，失败点是 `:app:compileDebugKotlin`，共 3 个错误，
**全部集中在同一个文件**：

| 位置 | 错误 |
|---|---|
| `GlassModifiers.kt:13:47` | `Unresolved reference 'StrokeJoin'` |
| `GlassModifiers.kt:96:13` | `Unresolved reference 'drawOutline'` |
| `GlassModifiers.kt:99:54` | `Unresolved reference 'StrokeJoin'` |

#### 根因：import 包路径写错

`StrokeJoin` 属于 **`androidx.compose.ui.graphics`**，而不是
`androidx.compose.ui.graphics.drawscope`。同一段 import 里相邻的
`Stroke` 确实在 `...graphics.drawscope`，于是顺手把 `StrokeJoin` 也归到了那个包——
这是唯一的原因。

第 2 个错误 `drawOutline` 是**连锁反应**：它的 `style` 实参里引用了未解析的
`StrokeJoin.Round`，参数类型退化为错误类型后，重载解析失败，
诊断便落到了函数名上。修掉 `StrokeJoin` 后它本会一起消失。

#### 处理方式：顺手拆掉这个隐患，而不只是改对包名

`drawOutline` 的可见性在 Compose 各版本间变动过（成员函数 / 扩展函数），
而本项目**没有本地 Android SDK，无法编译验证**，只能靠 CI 试错。
与其赌一次「包名改对就好了」，不如换掉这个调用：

`colorHalo` 现在按 `Outline` 的三种子类型分别绘制，只用
`drawRoundRect` / `drawRect` / `drawPath`——这三个是 `DrawScope` 的**接口成员**，
任何 Compose 版本上都可直接调用，不需要 import：

| Outline 子类型 | 绘制方式 |
|---|---|
| `Outline.Rounded` | `drawRoundRect(color, topLeft, size, cornerRadius, style)` |
| `Outline.Rectangle` | `drawRect(color, topLeft, size, style)` |
| `Outline.Generic` | `drawPath(path, color, style)` |

代价是多 6 行 `when` 分支和 1 个 `Outline` import，换来的是不再依赖版本相关的可见性。

#### 顺带核对的两件事

1. **资源层没有问题**。`processDebugResources` 已在失败前成功执行，
   说明「删 `.webp` + 加 PNG + 矢量自适应图标」这套改动通过了 AAPT2 校验，
   §8.10.8 里担心的重复资源冲突并不存在。
2. **单元测试源码集仍然没有执行到**。`compileDebugUnitTestKotlin` 排在
   `compileDebugKotlin` 之后，本次未运行。已逐个核对 `UIVerification` 的调用：
   `Glass.TextPrimary` / `Glass.GlowAmber` / `Glass.Accent` 三个令牌均存在，
   `GlassCard(glow = ...)` / `GlassChip(label, selected, onClick)` /
   `GlassPrimaryButton(text, onClick)` / `GlassScreenHeader(title, subtitle)` /
   `LiquidGlassBackground { }` 签名全部匹配。
   另外 `app/src/logic_check/screenshots/` 目录并不存在，
   而 `captureRoboImage(filePath = ...)` 只写出文件、不做基线比对，
   所以主题由深转浅不会造成截图断言失败。

#### 本轮仍未验证到的部分

| 任务 | 待验证点 |
|---|---|
| `compileDebugKotlin` | `GlassModifiers.kt` 的 `Outline` 分支写法、`Motion.kt` 的 `graphicsLayer` 前置、`EntryForms.kt` 的两个 `LaunchedEffect` |
| `compileDebugUnitTestKotlin` | `UIVerification` / `UnitVerification` / `LogicVerification` / `HardwareVerification` |
| `testDebugUnitTest` | 日期用例与 2 张 Roborazzi 截图 |
| `assembleDebug` | 打包 |

`allWarningsAsErrors` 未启用，因此告警不会导致构建失败。

---

### 8.12 第四次 CI 编译失败与修复（同文件第二次）

CI 日志 `logs/0_Test and build APK.txt`，2 个错误，仍然都在 `GlassModifiers.kt`：

| 位置 | 错误 |
|---|---|
| `GlassModifiers.kt:107:49` | `Unresolved reference 'topLeft'` |
| `GlassModifiers.kt:108:46` | `Unresolved reference 'size'` |

这两行是 §8.11 里新写的 `Outline.Rounded` 分支：

```kotlin
is Outline.Rounded -> drawRoundRect(
    topLeft = outline.roundRect.topLeft,   // 报错
    size = outline.roundRect.size,         // 报错
    cornerRadius = outline.roundRect.topLeftCornerRadius,  // 没报错
    ...
)
```

同一个 `when` 里 `Outline.Rectangle` 分支的 `outline.rect.topLeft` / `outline.rect.size`
都没报错，`Outline.Generic` 的 `drawPath` 也没报错。也就是说 `Rect` 的成员可用，
而 `RoundRect` 的 `topLeft` / `size` 在本项目的 Compose 版本上解析不到
（`topLeftCornerRadius` 却能解析）。这个差异没有可靠解释，
也说明**依赖 `Outline` 子类的成员本身就是不可控的**。

#### 处理方式：彻底不再碰 `Outline`

连续两轮栽在 `Outline` 上，继续逐行修补只是在下一次换个成员名再失败一次。
所以这次把 `colorHalo` 改成完全不读取 `Outline`：

光晕只需要「轮廓的圆角半径」，而轮廓就是节点自身的矩形（`topLeft = Zero`、
`size = 节点尺寸`）。`DrawScope.drawRoundRect` 的 `topLeft` / `size` **都有默认值**，
于是只需从形状里取出圆角：

```kotlin
val cornerPx = (shape as? RoundedCornerShape)?.topStart?.toPx(size, this) ?: 0f
drawRoundRect(
    color = color.copy(alpha = alpha),
    cornerRadius = CornerRadius(cornerPx + width / 2f),
    style = Stroke(width = width, join = StrokeJoin.Round)
)
```

这样用到的每一个 API 都是确定的：

| API | 为何确定 |
|---|---|
| `drawRoundRect(color, cornerRadius, style)` | `DrawScope` 接口成员；同仓 `GlassIcons.kt:121` 早已在用具名参数版本并编译通过 |
| `RoundedCornerShape.topStart` | `CornerBasedShape` 的构造参数，`androidx.compose.foundation.shape` 包，`RoundedCornerShape` 本就已 import |
| `CornerSize.toPx(Size, Density)` | `CornerSize` 接口成员，`drawBehind` 的 `this` 是 `DrawScope`，本身就是 `Density` |
| `CornerRadius` | `androidx.compose.ui.geometry`，稳定 |

同时删掉了 `Outline` 的 import，`RoundedCornerShape` 则本来就在 import 里。

#### 形状覆盖是否退化

`colorHalo` 的形状参数现在只接受 `RoundedCornerShape` 一族。逐个核对了全部调用点
（`liquidGlass` / `accentGlow` / 直接调用 `colorHalo`）实际传入的形状：
`Glass.cardShape` / `panelShape` / `fieldShape` / `controlShape` / `pillShape` /
`sheetShape` 全部是 `RoundedCornerShape`；另外几处传 `CircleShape`，
而 `CircleShape` 本身就是 `RoundedCornerShape(50)`。
**因此 100% 的调用点都被 cast 覆盖，没有形状退化。**
`CircleShape` 的 `topStart` 是 50% 百分比 `CornerSize`，`toPx` 得到短边的 50%，
正好是圆的半径。

若将来传入别的形状，`cornerPx` 退化为 0，光晕变成直角矩形——外观变差但不会崩，
这个兜底可以接受。

#### 本轮仍未验证到的部分

同 §8.11：`compileDebugUnitTestKotlin` / `testDebugUnitTest` / `assembleDebug`
本轮依旧没有执行到，需要在 CI 上确认。

---

### 8.13 第五轮：换用 Haze 真折射玻璃 + 工具链升级

用户反馈「UI 总觉得有点难看」，要求换成成熟的液态玻璃框架。
结论先行：**难看的主因是对比度，不是缺库**；换库同时把对比度一起修了。

#### 8.13.1 为什么「难看」——实测数据

用 WCAG 相对亮度公式逐项计算旧令牌（脚本实测，非估算）：

| 组合 | 对比度 | 结论 |
|---|---|---|
| `FillSubtle`(35% 白) 压 `#F7F9FE` 基底 | **1.04 : 1** | 卡片边界等于不存在 |
| `Fill`(55% 白) 压基底 | **1.03 : 1** | 同上 |
| `StrokeTop` 纯白描边 on 基底 | **1.05 : 1** | 描边看不见 |
| 白字 on `Accent` `#F97316` | **2.80 : 1** | 低于 AA 的 4.5 |
| 白字 on `AccentWarning` `#D99100` | **2.62 : 1** | 最差 |
| `TextTertiary` `#8B93A8` on 基底 | **2.92 : 1** | 三级文字发灰 |

人眼分辨「有东西浮在背景上」约需 **1.2:1**，而卡片只有 1.03:1 —— 整屏糊成一片淡白，
没有层次。`Glass.kt` 旧注释里写的「白字对比度 > 4.5:1」是错的，实测 2.80。

#### 8.13.2 选型：为什么是 Haze，以及它卡在哪

联网核实（Maven Central 的 Gradle module 元数据 + 官方 Dokka API，不是凭记忆）：

| 事实 | 数据 |
|---|---|
| `haze-glass` 起始版本 | **2.0.0-alpha04**（alpha03 没有 glass 模块） |
| 稳定版 `haze-glass:2.0.1` 要求 | `kotlin-stdlib` **2.4.20**、`compose.ui` **1.12.1**、`activity` 1.13.0 |
| 对应 Compose BOM | **2026.09.00** → `androidx.compose.ui:ui 1.12.1` |
| KSP 最新版 | **2.3.12**，依赖 `kotlin-stdlib 2.3.20` —— **没有 Kotlin 2.4.x 版本** |

最后一条是硬阻塞：Room 靠 KSP 生成代码（`"ksp"(room.compiler)`），
而 Haze 的玻璃模块要求 Kotlin ≥ 2.4.10。两者无法共存，
所以 **Room 从 KSP 迁到 kapt**（Room 2.7.0 完整支持，`correctErrorTypes = true`）。

#### 8.13.3 依赖变更

| 项 | 旧 | 新 |
|---|---|---|
| `kotlin` | 2.2.10 | **2.4.20** |
| `composeBom` | 2024.09.00（Compose 1.7.1） | **2026.09.00**（Compose 1.12.1） |
| `activityCompose` | 1.10.1 | **1.13.0**（Haze 要求） |
| `lifecycle*` 三项 | 2.8.7 | **2.9.4**（对齐，避免与 activity 1.13 混版） |
| 注解处理 | KSP 2.3.5 | **kapt**（插件 `org.jetbrains.kotlin.kapt`） |
| `minSdk` | 24 | **31** |
| 新增 | — | `dev.chrisbanes.haze:haze`、`haze-glass`（均 2.0.1） |

`material-icons-core` 在 BOM 2026.09.00 里仍被管理（冻结在 1.7.8），
`material3` 变为 1.4.0 —— 都已确认存在，不会出现「无版本号」错误。

#### 8.13.4 玻璃层的职责重新划分

`liquidGlass` 现在是真的 Haze 玻璃，`glassSurface` 刻意**不接** Haze：

| API | 实现 | 用在哪 |
|---|---|---|
| `liquidGlass` | `Modifier.hazeGlass(input = HazeInput.Backdrop(state), style = GlassStyle.regular.then { tint/shape/edgeShadow })` | 卡片、面板、底栏（16 处） |
| `glassSurface` | 半透明填充 + 渐变描边 | 玻璃内部的小控件（41 处） |
| `colorHalo` | 同色调多层描边 | 品牌色柔光 |

内层不接 Haze 的理由有两条：逐节点采样着色器会成倍放大绘制开销；
而且**视觉上是错的**——玻璃里的玻璃会把折射叠两次，边框变粗、界面发脏。

`LocalHazeState` 是 `staticCompositionLocalOf<HazeState?> { null }`。
为 `null` 时 `liquidGlass` 退回「填充 + 描边」的手搓实现，
这样 Roborazzi 截图测试与 `@Preview` 在没有 `LiquidGlassBackground` 时仍能渲染。

#### 8.13.5 配色修正

| 令牌 | 旧 | 新 | 白字对比度 |
|---|---|---|---|
| `Accent` | `#F97316` | `#C2410C` | 2.80 → **5.18** |
| `AccentCool` | `#2E90D0` | `#0B72B8` | 3.49 → **5.11** |
| `AccentDanger` | `#E5484D` | `#C42B30` | 3.91 → **5.62** |
| `AccentSuccess` | `#17A673` | `#0F7A54` | 3.12 → **5.34** |
| `AccentWarning` | `#D99100` | `#A16207` | 2.62 → **4.92** |
| `TextTertiary` | `#8B93A8` | `#5F6878` | 2.92 → **5.00** |
| `BaseTop` / `BaseBottom` | `#F7F9FE` / `#E9EFFB` | `#F0F5FD` / `#DCE7F7` | 基底压深，玻璃才有可折射的内容 |
| `Fill` 系列 | `0x59`→`0xE0` 白 | `0x33`→`0x99` 白 | 下调：Haze 自带材质响应，再叠高不透明度会糊成白板 |

`Theme.kt` 同时修了两处会被新透明度放大的问题：

- `surface` 原本映射到 `Glass.Fill`（现 30% 白），Material 弹层会几乎全透明 → 改映射到 `SheetFill`（90% 白）；
- `outline` 原本映射到 `StrokeMid`（近白），在浅底上等于没画 → 改映射到 `StrokeBottom` 与 `Hairline`。

#### 8.13.6 本轮静态核查结果

| 检查项 | 结果 |
|---|---|
| 括号平衡 | 通过 |
| 未使用 import | 0 |
| `shadow(` 实际调用 | 0（仅 1 处注释说明） |
| `liquidGlass` 调用点 | 16 处，全部位于 `@Composable` 上下文（已逐个确认，含 `HomeScreenComponents.kt:692`、`TrashBinScreen.kt:157`） |
| Haze API 签名 | 逐一比对官方 Dokka：`rememberHazeState()`、`Modifier.hazeSource(state, zIndex, key)`、`Modifier.hazeGlass(input, style, ...)`、`HazeInput.Backdrop(state)`、`GlassStyle.regular/clear/then`、`GlassStyleScope.{tint,shape,edgeShadow,edgeSoftness}` |
| `@ExperimentalHazeApi` | `Backdrop` / `Sources` / `hazeGlass` 均**未**标注，无需 opt-in |
| BOM 2026.09.00 内容 | `material-icons-core 1.7.8`、`material3 1.4.0`、`ui-test-* 1.12.1` 均存在 |

#### 8.13.7 本轮风险（必须由 CI / 真机确认）

这一轮改动的面比前几轮大得多，且**本机无法编译**，以下都是未知：

| 风险 | 说明 |
|---|---|
| Compose 1.7 → 1.12 | 跨 5 个小版本，Material3 1.3 → 1.4。全仓 Compose API 都可能遇到弃用或签名变更，这是最可能出错的一项 |
| Kotlin 2.2.10 → 2.4.20 | 语言层变化 + 需要确认 AGP 9.1.1 与 KGP 2.4.20 的兼容性 |
| kapt + K2 | kapt 处于维护状态，Room 2.7.0 的注解处理在 Kotlin 2.4 下未验证过 |
| Roborazzi 1.59.0 | 是否兼容 Compose 1.12 未知；截图测试可能失败 |
| 真机观感 | Haze 的 `GlassStyle.regular` 是按 iOS 27 校准的材质，压在这套浅色底上实际效果没有目视校对，可能需要调 `tint` / `optics` |
| `minSdk 24 → 31` | 放弃 Android 7–11 设备；`mipmap-*` 里的低版本 PNG 兜底图标已无用（保留不影响构建） |

建议 CI 失败时按「先 Compose API 迁移、再 kapt、最后 Haze 调参」的顺序处理。
