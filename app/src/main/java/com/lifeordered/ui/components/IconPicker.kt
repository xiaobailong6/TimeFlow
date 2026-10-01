package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.ui.theme.*

@Composable
fun IconPicker(
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val localIcons = listOf(
        "🎂", "💍", "🐱", "🐶", "🐰", "🪥", "🎀", "🎁", "🎉", "📅", "🍎", "💊",
        "🏠", "🚗", "💻", "💡", "⚽", "🎵", "📷", "🍔", "☕", "🧴", "🧼", "🧻",
        "👕", "👗", "👟", "👜", "💄", "🧺", "🛋️", "🪴", "🌷", "🌻", "🌲", "🌟",
        "📚", "✏️", "✂️", "📌", "🧸", "🎈", "🔑", "💳", "💰", "🍉", "🍓", "🥩",
        "🥪", "🍣", "🍦", "🍺", "🍷", "🎮", "🎸", "💊", "🩹", "🧯", "🧹", "🗑️"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmCreamBg, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择图标",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            IconButton(onClick = onDismiss) {
                Text("✕", color = GrayTextMuted)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.heightIn(max = 300.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(localIcons) { icon ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF5EDE6), RoundedCornerShape(16.dp))
                        .clickable { onIconSelected(icon) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 28.sp)
                }
            }
        }
    }
}
