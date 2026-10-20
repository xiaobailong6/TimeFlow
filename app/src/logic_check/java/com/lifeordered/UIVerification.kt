package com.lifeordered

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassCard
import com.lifeordered.ui.theme.GlassChip
import com.lifeordered.ui.theme.GlassPrimaryButton
import com.lifeordered.ui.theme.GlassScreenHeader
import com.lifeordered.ui.theme.LifeOrderedTheme
import com.lifeordered.ui.theme.LiquidGlassBackground
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 液态玻璃设计系统的截图校验。
 *
 * ## 为什么不是一句 Text
 *
 * 重构前这里只渲染了一行文字，且文案还是旧名「生活有序 (LifeOrdered)」——
 * 既没覆盖到设计系统，也和 `strings.xml` 里的「拾光流年」对不上。
 * 现在两张图分别锁住两件事：
 *
 * 1. `greeting_screenshot`：基底 + 品牌名，验证深墨蓝液体底没有被意外改回浅色
 * 2. `glass_components_screenshot`：标题栏 / 卡片 / Chip / 主按钮同框，
 *    验证玻璃层的「半透明 + 上亮下暗描边」在整套组件上是一致的
 *
 * 玻璃靠半透明成立，任何组件退回不透明实色都会在这张图里立刻暴露。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class UIVerification {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      LifeOrderedTheme {
        LiquidGlassBackground {
          Text(
            text = "拾光流年",
            fontSize = 28.sp,
            color = Glass.TextPrimary,
            modifier = Modifier.padding(24.dp)
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/logic_check/screenshots/greeting.png")
  }

  @Test
  fun glass_components_screenshot() {
    composeTestRule.setContent {
      LifeOrderedTheme {
        LiquidGlassBackground {
          Column(modifier = Modifier.fillMaxSize()) {
            GlassScreenHeader(title = "拾光流年", subtitle = "记录正在流逝的时间")

            Column(
              modifier = Modifier.padding(horizontal = 20.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              GlassCard(glow = Glass.GlowAmber) {
                Column {
                  Text(text = "相识纪念日", fontSize = 16.sp, color = Glass.TextPrimary)
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(text = "剩余 128 天", fontSize = 22.sp, color = Glass.Accent)
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassChip(label = "全部", selected = true, onClick = {})
                GlassChip(label = "倒数日", selected = false, onClick = {})
                GlassChip(label = "正数日", selected = false, onClick = {})
              }

              GlassPrimaryButton(text = "新建记录", onClick = {})
            }
          }
        }
      }
    }

    composeTestRule.onRoot()
      .captureRoboImage(filePath = "src/logic_check/screenshots/glass_components.png")
  }
}
