package com.lifeordered.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassTextIconButton
import com.lifeordered.ui.theme.glassSurface

/**
 * Emoji 图标选择器（液态玻璃版）。
 *
 * 图标以网格呈现，每个格子是独立的小玻璃片——这样即使在深色底上
 * 也能一眼看出「这是一个可点的方块」，而不是一片浮空的 emoji。
 */
@Composable
fun IconPicker(
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val icons = listOf(
        "🎂", "💍", "🐱", "🐶", "🐰", "🪥", "🎀", "🎁", "🎉", "📅", "🍎", "💊",
        "🏠", "🚗", "💻", "💡", "⚽", "🎵", "📷", "🍔", "☕", "🧴", "🧼", "🧻",
        "👕", "👗", "👟", "👜", "💄", "🧺", "🛋️", "🪴", "🌷", "🌻", "🌲", "🌟",
        "📚", "✏️", "✂️", "📌", "🧸", "🎈", "🔑", "💳", "💰", "🍉", "🍓", "🥩",
        "🥪", "🍣", "🍦", "🍺", "🍷", "🎮", "🎸", "🩹", "🧯", "🧹", "🗑️"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择图标",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary
            )
            GlassTextIconButton(
                text = "✕",
                onClick = onDismiss,
                size = 30.dp,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(icons) { icon ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .glassSurface(shape = Glass.controlShape, fill = Glass.FillSubtle)
                        .clickable { onIconSelected(icon) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 24.sp)
                }
            }
        }
    }
}
