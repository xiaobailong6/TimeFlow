package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassPrimaryButton
import com.lifeordered.ui.theme.GlassSecondaryButton

/**
 * 通用二次确认面板（液态玻璃版）。
 *
 * 视觉要点：面板本身是一块较厚的玻璃（[Glass.FillHeavy] 叠加在深色底上），
 * 危险操作由按钮的辉光色区分，而不是把整个面板染红——整块变红会让文字
 * 在深色底上失去对比度。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfirmBottomSheet(
    title: String,
    content: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit,
    isWarning: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = if (isWarning) Glass.AccentDanger else Glass.Accent

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Glass.SheetFill,
        scrimColor = Glass.Scrim,
        shape = RoundedCornerShape(topStart = Glass.Radius.panel, topEnd = Glass.Radius.panel),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.34f)
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 拖拽指示条
            Box(
                modifier = Modifier
                    .padding(bottom = 18.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Glass.FillStrong)
            )

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Text(
                text = content,
                fontSize = 13.sp,
                color = Glass.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 22.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassSecondaryButton(
                    text = "取消",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                GlassPrimaryButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    accent = accent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
