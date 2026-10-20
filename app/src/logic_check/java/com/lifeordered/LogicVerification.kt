package com.lifeordered

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.lifeordered.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LogicVerification {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    // 断言值必须与 res/values/strings.xml 保持一致。
    // 重构前这里写的是「生活有序」，而资源实际为「拾光流年」，该测试一直是失败的。
    assertEquals("拾光流年", appName)
  }
}
