package com.lifeordered.ui.components

 import androidx.compose.foundation.background
 import androidx.compose.foundation.layout.Box
 import androidx.compose.foundation.layout.padding
 import androidx.compose.foundation.layout.widthIn
 import androidx.compose.foundation.layout.wrapContentWidth
 import androidx.compose.material3.SnackbarHost
 import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.glassSurface
import kotlinx.coroutines.launch

/**
 * 统一的操作反馈通道。
 *
 * ## 为什么不再用 Toast
 *
 * 重构前全仓约有 20 处 `android.widget.Toast`，问题有三个：
 * 1. **无法与玻璃视觉统一**——Toast 是系统样式，在深色玻璃界面上像补丁；
 * 2. **无法被 Compose 测试捕获**——Toast 是系统窗口，`composeTestRule` 看不到；
 * 3. **连续操作时互相堆叠**——快速打卡多次会排队显示好几条，且不随页面切换消失。
 *
 * Snackbar 解决这三点，并且能通过 [LocalSnackbarHostState] 在任何层级拿到，
 * 不需要把回调一路透传到最深的组件里。
 */

/** 由 `HomeScreen` 提供，其余页面按需读取。 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }

/**
 * 取一个「显示一条提示」的函数。
 *
 * 会先 dismiss 上一条：用户连点两次补货时，不应该看到两条消息排队。
 * 拿不到 Host（例如在预览或独立组件中调用）时退化为空操作，不抛异常。
 */
@Composable
fun rememberNotifier(): (String) -> Unit {
    val hostState = LocalSnackbarHostState.current ?: return { }
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) {
        { message: String ->
            scope.launch {
                hostState.currentSnackbarData?.dismiss()
                hostState.showSnackbar(message)
            }
        }
    }
}

 /** 玻璃风格的气泡通知宿主：胶囊宽度随内容自适应。 */
 @Composable
 fun GlassSnackbarHost(
     hostState: SnackbarHostState,
     modifier: Modifier = Modifier
 ) {
     SnackbarHost(hostState = hostState, modifier = modifier) { data ->
         // 胶囊宽度由内容决定：wrapContentWidth 让短通知不至于撑成一条长条；
         // widthIn(max) 防止超长文案溢出屏幕，超出时靠多行兜底。
         Box(
             modifier = Modifier
                 .padding(horizontal = 20.dp, vertical = 8.dp)
                 .wrapContentWidth()
                 .widthIn(max = 320.dp)
                 .glassSurface(shape = Glass.pillShape, fill = Glass.FillStrong)
                 .background(Glass.SheetFill, Glass.pillShape)
                 .padding(horizontal = 18.dp, vertical = 10.dp)
         ) {
             Text(
                 text = data.visuals.message,
                 fontSize = 13.sp,
                 color = Glass.TextPrimary,
                 maxLines = 2
             )
         }
     }
 }
