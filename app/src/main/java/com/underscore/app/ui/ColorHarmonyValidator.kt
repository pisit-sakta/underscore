package com.underscore.app.ui

import android.graphics.Color as AndroidColor
import kotlin.math.pow

/**
 * Validates and auto-corrects LLM-generated character color pairings.
 *
 * Ensures the two colors for a diagonal split look fabulous together:
 * sufficient contrast, no clashing saturated primaries, no flag/brand lookalikes.
 * Applied after LLM generation, before DB insert. Presets are hand-picked and skip this.
 */
object ColorHarmonyValidator {

    data class ValidatedColors(
        val color1: String,
        val color2: String,
        val wasModified: Boolean
    )

    fun validate(color1: String, color2: String): ValidatedColors {
        var hsv1 = hexToHsv(color1)
        var hsv2 = hexToHsv(color2)
        var modified = false

        repeat(3) {
            val fix1 = checkAndFix(hsv1, hsv2)
            if (fix1 != null) {
                hsv1 = fix1.first
                hsv2 = fix1.second
                modified = true
            } else {
                return ValidatedColors(
                    color1 = hsvToHex(hsv1),
                    color2 = hsvToHex(hsv2),
                    wasModified = modified
                )
            }
        }

        return ValidatedColors(
            color1 = hsvToHex(hsv1),
            color2 = hsvToHex(hsv2),
            wasModified = modified
        )
    }

    private fun checkAndFix(hsv1: FloatArray, hsv2: FloatArray): Pair<FloatArray, FloatArray>? {
        val lum1 = luminanceFromHsv(hsv1)
        val lum2 = luminanceFromHsv(hsv2)
        val h1 = hsv1.copyOf()
        val h2 = hsv2.copyOf()
        var changed = false

        // 1. Known bad pairings at high saturation
        if (isBadPairing(h1, h2)) {
            // Shift the more saturated color's hue by +30 and reduce saturation
            if (h1[1] >= h2[1]) {
                h1[0] = (h1[0] + 30f) % 360f
                h1[1] = (h1[1] - 0.2f).coerceAtLeast(0.2f)
            } else {
                h2[0] = (h2[0] + 30f) % 360f
                h2[1] = (h2[1] - 0.2f).coerceAtLeast(0.2f)
            }
            changed = true
        }

        // 2. Both colors too saturated — desaturate the darker one
        if (h1[1] > 0.8f && h2[1] > 0.8f) {
            val darkIdx = if (luminanceFromHsv(h1) <= luminanceFromHsv(h2)) h1 else h2
            darkIdx[1] *= 0.6f
            changed = true
        }

        // 3. Contrast check — need ratio >= 1.5
        val l1 = luminanceFromHsv(h1)
        val l2 = luminanceFromHsv(h2)
        val ratio = contrastRatio(l1, l2)
        if (ratio < 1.5f) {
            // Darken the darker one further
            val darker = if (l1 <= l2) h1 else h2
            darker[2] = (darker[2] * 0.6f).coerceAtLeast(0.05f)
            changed = true
        }

        // 4. One dark + one accent rule
        val newLum1 = luminanceFromHsv(h1)
        val newLum2 = luminanceFromHsv(h2)
        if (newLum1 >= 0.25f && newLum2 >= 0.25f) {
            // Neither is dark — darken the lower-saturation one
            val anchor = if (h1[1] <= h2[1]) h1 else h2
            anchor[2] = 0.2f
            changed = true
        } else if (newLum1 < 0.08f && newLum2 < 0.08f) {
            // Both too dark — brighten the higher-saturation one
            val accent = if (h1[1] >= h2[1]) h1 else h2
            accent[2] = 0.45f
            changed = true
        }

        return if (changed) Pair(h1, h2) else null
    }

    private fun isBadPairing(hsv1: FloatArray, hsv2: FloatArray): Boolean {
        if (hsv1[1] < 0.5f || hsv2[1] < 0.5f) return false

        val hue1 = hsv1[0]
        val hue2 = hsv2[0]

        fun inRange(h: Float, vararg ranges: Pair<Float, Float>): Boolean =
            ranges.any { (lo, hi) -> h in lo..hi }

        val isRed1 = inRange(hue1, 0f to 30f, 330f to 360f)
        val isRed2 = inRange(hue2, 0f to 30f, 330f to 360f)
        val isGreen1 = inRange(hue1, 90f to 150f)
        val isGreen2 = inRange(hue2, 90f to 150f)
        val isYellow1 = inRange(hue1, 40f to 70f)
        val isYellow2 = inRange(hue2, 40f to 70f)

        // Red + Green (Christmas / accessibility)
        if ((isRed1 && isGreen2) || (isGreen1 && isRed2)) return true
        // Green + Yellow (Brazil flag)
        if ((isGreen1 && isYellow2) || (isYellow1 && isGreen2)) return true
        // Red + Yellow (McDonald's)
        if ((isRed1 && isYellow2) || (isYellow1 && isRed2)) return true

        return false
    }

    private fun contrastRatio(lum1: Float, lum2: Float): Float {
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun luminanceFromHsv(hsv: FloatArray): Float {
        val rgb = IntArray(3)
        val color = AndroidColor.HSVToColor(hsv)
        rgb[0] = (color shr 16) and 0xFF
        rgb[1] = (color shr 8) and 0xFF
        rgb[2] = color and 0xFF
        return 0.2126f * linearize(rgb[0] / 255f) +
               0.7152f * linearize(rgb[1] / 255f) +
               0.0722f * linearize(rgb[2] / 255f)
    }

    private fun linearize(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

    private fun hexToHsv(hex: String): FloatArray {
        val cleaned = hex.removePrefix("#")
        val colorInt = try {
            AndroidColor.parseColor("#$cleaned")
        } catch (e: Exception) {
            AndroidColor.parseColor("#0A0A0A")
        }
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(colorInt, hsv)
        return hsv
    }

    private fun hsvToHex(hsv: FloatArray): String {
        val colorInt = AndroidColor.HSVToColor(hsv)
        val r = (colorInt shr 16) and 0xFF
        val g = (colorInt shr 8) and 0xFF
        val b = colorInt and 0xFF
        return "#%02X%02X%02X".format(r, g, b)
    }
}
