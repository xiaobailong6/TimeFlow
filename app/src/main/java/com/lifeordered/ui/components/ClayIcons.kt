package com.lifeordered.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Lovely claymorphism colors
val ClayPink = Color(0xFFFFB3BA)
val ClayPinkDark = Color(0xFFFF8B94)
val ClayOrange = Color(0xFFFFB07C)
val ClayBlue = Color(0xFF90CCF4)
val ClayBlueDark = Color(0xFF6BA6CE)
val ClayYellow = Color(0xFFFFE082)
val ClayCream = Color(0xFFFDFBF7)
val ClayRed = Color(0xFFFF7B7B)

@Composable
fun ClayCakeIcon(modifier: Modifier = Modifier.size(64.dp)) {
    val path = remember { Path() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        path.reset()
        path.moveTo(w * 0.5f, h * 0.15f)
        path.quadraticTo(w * 0.55f, h * 0.22f, w * 0.5f, h * 0.28f)
        path.quadraticTo(w * 0.45f, h * 0.22f, w * 0.5f, h * 0.15f)
        path.close()

        // Background shadow glowing effect
        drawCircle(
            color = Color(0xFFFFF0F2),
            radius = w * 0.45f,
            center = Offset(w * 0.5f, h * 0.55f)
        )

        // Cake plate
        drawRoundRect(
            color = Color(0xFFE8E5E0),
            topLeft = Offset(w * 0.15f, h * 0.72f),
            size = Size(w * 0.7f, h * 0.1f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Cake lower layer (Pink)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFCCD2), Color(0xFFFFABB6))),
            topLeft = Offset(w * 0.22f, h * 0.45f),
            size = Size(w * 0.56f, h * 0.28f),
            cornerRadius = CornerRadius(20f, 20f)
        )

        // Cake icing cream dripping decoration
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.22f, h * 0.45f),
            size = Size(w * 0.56f, h * 0.08f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Icing drops
        drawCircle(color = Color.White, radius = 8f, center = Offset(w * 0.32f, h * 0.53f))
        drawCircle(color = Color.White, radius = 10f, center = Offset(w * 0.5f, h * 0.55f))
        drawCircle(color = Color.White, radius = 8f, center = Offset(w * 0.68f, h * 0.53f))

        // Candle holder
        drawRect(
            color = ClayYellow,
            topLeft = Offset(w * 0.47f, h * 0.28f),
            size = Size(w * 0.06f, h * 0.17f)
        )

        drawPath(path, color = Color(0xFFFF5252))
        
        // Center cherry
        drawCircle(color = Color(0xFFFF2E56), radius = 12f, center = Offset(w * 0.5f, h * 0.4f))
    }
}

@Composable
fun ClayGiftIcon(modifier: Modifier = Modifier.size(64.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Gift background shadow
        drawCircle(
            color = Color(0xFFEDF4FF),
            radius = w * 0.45f,
            center = Offset(w * 0.5f, h * 0.55f)
        )

        // Main Box (Blue gradient)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF8DC8F3), Color(0xFF5CA4DF))),
            topLeft = Offset(w * 0.23f, h * 0.36f),
            size = Size(w * 0.54f, h * 0.46f),
            cornerRadius = CornerRadius(24f, 24f)
        )

        // Box Lid
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFACD6F6), Color(0xFF75B6E7))),
            topLeft = Offset(w * 0.18f, h * 0.3f),
            size = Size(w * 0.64f, h * 0.11f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // Yellow Ribbon Cross Vertical
        drawRect(
            color = ClayYellow,
            topLeft = Offset(w * 0.46f, h * 0.3f),
            size = Size(w * 0.08f, h * 0.52f)
        )

        // Yellow Ribbon Cross Horizontal
        drawRect(
            color = ClayYellow,
            topLeft = Offset(w * 0.23f, h * 0.52f),
            size = Size(w * 0.54f, h * 0.08f)
        )

        // Cute Ribbon Bow
        drawCircle(
            color = ClayYellow,
            radius = 14f,
            center = Offset(w * 0.4f, h * 0.22f),
            style = Stroke(width = 8f)
        )
        drawCircle(
            color = ClayYellow,
            radius = 14f,
            center = Offset(w * 0.6f, h * 0.22f),
            style = Stroke(width = 8f)
        )
        drawCircle(color = Color(0xFFFFC107), radius = 8f, center = Offset(w * 0.5f, h * 0.24f))
    }
}

@Composable
fun ClayHeartsIcon(modifier: Modifier = Modifier.size(64.dp)) {
    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Shadow circle
        drawCircle(
            color = Color(0xFFFFF0F2),
            radius = w * 0.45f,
            center = Offset(w * 0.5f, h * 0.55f)
        )

        // Main big heart in center
        drawHeartInCanvas(path1, w * 0.5f, h * 0.5f, w * 0.38f, ClayPinkDark)

        // Small overlapping heart
        drawHeartInCanvas(path2, w * 0.32f, h * 0.36f, w * 0.18f, ClayRed)
        drawHeartInCanvas(path3, w * 0.68f, h * 0.68f, w * 0.18f, ClayRed)
    }
}

private fun DrawScope.drawHeartInCanvas(path: Path, cx: Float, cy: Float, size: Float, color: Color) {
    path.reset()
    // Simple cubic bezier curve approximation for a beautiful 3D heart
    path.moveTo(cx, cy - size * 0.25f)
    path.cubicTo(
        cx - size * 0.5f, cy - size * 0.65f,
        cx - size, cy - size * 0.1f,
        cx, cy + size * 0.55f
    )
    path.cubicTo(
        cx + size, cy - size * 0.1f,
        cx + size * 0.5f, cy - size * 0.65f,
        cx, cy - size * 0.25f
    )
    path.close()
    
    drawPath(path = path, color = color)
}

@Composable
fun ClayFilterIcon(modifier: Modifier = Modifier.size(54.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer container shadow glow
        drawCircle(color = Color(0xFFE3F2FD), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f))

        // Cylinder Body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFE2F1FF), Color(0xFF82B1FF))),
            topLeft = Offset(w * 0.25f, h * 0.3f),
            size = Size(w * 0.5f, h * 0.5f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Filter top rim
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.2f, h * 0.22f),
            size = Size(w * 0.6f, h * 0.1f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Core filter lines inside
        drawRoundRect(
            color = Color(0x44FFFFFF),
            topLeft = Offset(w * 0.35f, h * 0.38f),
            size = Size(w * 0.3f, h * 0.34f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Little water droplet
        drawCircle(color = Color(0xFF2979FF), radius = 7f, center = Offset(w * 0.44f, h * 0.65f))
        
        // Splash ripple circle
        drawCircle(
            color = Color(0xFF64B5F6),
            radius = 12f,
            center = Offset(w * 0.56f, h * 0.65f),
            style = Stroke(width = 3f)
        )
    }
}

@Composable
fun ClayLitterIcon(modifier: Modifier = Modifier.size(54.dp)) {
    val path = remember { Path() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Shadow circle
        drawCircle(color = Color(0xFFF3F0EC), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.52f))

        // Litter Sack Bag
        path.reset()
        path.moveTo(w * 0.25f, h * 0.8f)
        path.lineTo(w * 0.2f, h * 0.35f)
        path.quadraticTo(w * 0.5f, h * 0.25f, w * 0.8f, h * 0.35f)
        path.lineTo(w * 0.75f, h * 0.8f)
        path.quadraticTo(w * 0.5f, h * 0.86f, w * 0.25f, h * 0.8f)
        path.close()
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF039BE5)))
        )

        // Bag tie top ruffle
        drawRoundRect(
            color = Color(0xFF0288D1),
            topLeft = Offset(w * 0.3f, h * 0.28f),
            size = Size(w * 0.4f, h * 0.08f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Draw a minimalist cat face silhouette
        drawCircle(color = Color.White, radius = 10f, center = Offset(w * 0.5f, h * 0.54f))
        drawCircle(color = Color.White, radius = 5f, center = Offset(w * 0.44f, h * 0.62f))
        drawCircle(color = Color.White, radius = 5f, center = Offset(w * 0.56f, h * 0.62f))
    }
}

@Composable
fun ClayBrushIcon(modifier: Modifier = Modifier.size(54.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Glow background
        drawCircle(color = Color(0xFFF1F8E9), radius = w * 0.43f, center = Offset(w * 0.5f, h * 0.5f))

        // Brush Handle
        drawRoundRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(w * 0.44f, h * 0.45f),
            size = Size(w * 0.12f, h * 0.45f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Brush Silver connector
        drawRoundRect(
            color = Color(0xFF9E9E9E),
            topLeft = Offset(w * 0.42f, h * 0.35f),
            size = Size(w * 0.16f, h * 0.12f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Toothbrush Bristles head (Clay blue/mint)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFE0F7FA), Color(0xFF26A69A))),
            topLeft = Offset(w * 0.34f, h * 0.16f),
            size = Size(w * 0.32f, h * 0.2f),
            cornerRadius = CornerRadius(12f, 12f)
        )
    }
}

@Composable
fun ClayFoodIcon(modifier: Modifier = Modifier.size(44.dp)) {
    val path = remember { Path() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sack structure
        path.reset()
        path.moveTo(w * 0.22f, h * 0.85f)
        path.lineTo(w * 0.16f, h * 0.35f)
        path.lineTo(w * 0.84f, h * 0.35f)
        path.lineTo(w * 0.78f, h * 0.85f)
        path.close()
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(listOf(Color(0xFFFFCC80), Color(0xFFE65100)))
        )

        // Top tied knot
        drawRoundRect(
            color = Color(0xFFE65100),
            topLeft = Offset(w * 0.3f, h * 0.25f),
            size = Size(w * 0.4f, h * 0.12f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Little yellow label card in center
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.32f, h * 0.48f),
            size = Size(w * 0.36f, h * 0.22f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Heart on the tag label
        drawCircle(color = Color(0xFFFF3D00), radius = 5f, center = Offset(w * 0.5f, h * 0.58f))
    }
}

@Composable
fun ClayPartyIcon(modifier: Modifier = Modifier.size(64.dp)) {
    val path = remember { Path() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Glow
        drawCircle(color = Color(0xFFFFFDE7), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f))

        // Party Hat (Triangle)
        path.reset()
        path.moveTo(w * 0.5f, h * 0.2f)
        path.lineTo(w * 0.2f, h * 0.8f)
        path.lineTo(w * 0.8f, h * 0.8f)
        path.close()
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(listOf(Color(0xFFFFF176), Color(0xFFFBC02D)))
        )

        // Pom pom on top
        drawCircle(color = Color(0xFFFF4081), radius = 10f, center = Offset(w * 0.5f, h * 0.18f))

        // Stripes on hat
        drawRect(color = Color.White.copy(alpha = 0.3f), topLeft = Offset(w * 0.35f, h * 0.5f), size = Size(w * 0.3f, h * 0.05f))
        drawRect(color = Color.White.copy(alpha = 0.3f), topLeft = Offset(w * 0.3f, h * 0.65f), size = Size(w * 0.4f, h * 0.05f))
    }
}

@Composable
fun ClayPaperIcon(modifier: Modifier = Modifier.size(44.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Roll background shadow
        drawCircle(color = Color(0xFFECEFF1), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.52f))

        // Main cylindrical paper roll (White/grey)
        drawOval(
            brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFCFD8DC))),
            topLeft = Offset(w * 0.2f, h * 0.16f),
            size = Size(w * 0.6f, h * 0.46f)
        )

        // Hollow tube hole in center of the oval
        drawOval(
            color = Color(0xFF8D6E63),
            topLeft = Offset(w * 0.42f, h * 0.28f),
            size = Size(w * 0.16f, h * 0.12f)
        )

        // Hanging segment block representing dispenser paper sheet
        drawRoundRect(
            color = Color(0xFFECEFF1),
            topLeft = Offset(w * 0.2f, h * 0.42f),
            size = Size(w * 0.36f, h * 0.42f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}
