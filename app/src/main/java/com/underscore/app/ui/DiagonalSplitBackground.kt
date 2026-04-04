package com.underscore.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * THE DIAGONAL SPLIT.
 *
 * Renders a sharp diagonal line from top-left to bottom-right,
 * splitting the screen into two triangular color fields.
 * Content is drawn on top.
 *
 * When character mode is active, this IS the front page background.
 * The two colors are the character's signature palette — fans
 * recognize whose colors these are without reading a word.
 */
@Composable
fun DiagonalSplitBackground(
    color1: Color,
    color2: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Color 1: top-left triangle
            val topLeftPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(0f, h)
                close()
            }
            drawPath(topLeftPath, color1)

            // Color 2: bottom-right triangle
            val bottomRightPath = Path().apply {
                moveTo(w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(bottomRightPath, color2)
        }

        // Content on top of the split
        content()
    }
}

/**
 * Compact diagonal split preview for settings lists and blend mode slots.
 * Same two-path rendering as the main split, in a clipped mini canvas.
 */
@Composable
fun MiniDiagonalSplit(
    color1: Color,
    color2: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(2.dp))) {
        val w = size.width
        val h = size.height
        val topLeft = Path().apply {
            moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, h); close()
        }
        val bottomRight = Path().apply {
            moveTo(w, 0f); lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(topLeft, color1)
        drawPath(bottomRight, color2)
    }
}

/** Parse a hex color string like "#1B3D2F" to a Compose Color. */
fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorLong = cleaned.toLong(16)
        Color(
            red = ((colorLong shr 16) and 0xFF) / 255f,
            green = ((colorLong shr 8) and 0xFF) / 255f,
            blue = (colorLong and 0xFF) / 255f
        )
    } catch (e: Exception) {
        Color(0xFF0A0A0A) // Fallback to near-black
    }
}

/**
 * Returns a readable text color (light or dark) for the given background.
 * Uses WCAG relative luminance with a threshold tuned for the app's dark aesthetic.
 */
fun textColorForBackground(bg: Color): Color {
    val luminance = 0.2126f * linearize(bg.red) +
                    0.7152f * linearize(bg.green) +
                    0.0722f * linearize(bg.blue)
    return if (luminance > 0.35f) Color(0xFF121212) else Color(0xFFE0E0E0)
}

private fun linearize(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
