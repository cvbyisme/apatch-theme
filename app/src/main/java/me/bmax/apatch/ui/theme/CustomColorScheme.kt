package me.bmax.apatch.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 由 seed 色运行时生成 Material 3 配色（简化 tonal 派生，零第三方依赖）。
 * 完整 HCT 生成可换 material-color-utilities；这里用 HSL 近似保证开箱即用。
 */
fun colorSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val onPrimary = if (luminance(seed) > 0.5f) Color(0xFF1C1B1F) else Color.White
    val primaryContainer = if (dark) mix(seed, Color.Black, 0.55f) else mix(seed, Color.White, 0.78f)
    val secondary = mix(seed, if (dark) Color.White else Color.Black, if (dark) 0.20f else 0.25f)
    val tertiary = hueShift(seed, 60f)
    val tertiaryContainer = if (dark) mix(tertiary, Color.Black, 0.55f) else mix(tertiary, Color.White, 0.80f)
    val background = if (dark) Color(0xFF141218) else Color(0xFFFFFBFE)
    val onBackground = if (dark) Color(0xFFE6E0E9) else Color(0xFF1C1B1F)

    return if (dark) {
        darkColorScheme(
            primary = seed, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = mix(seed, Color.White, 0.9f),
            secondary = secondary, onSecondary = onPrimary,
            secondaryContainer = tertiaryContainer, onSecondaryContainer = onBackground,
            tertiary = tertiary, onTertiary = onPrimary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onBackground,
            background = background, onBackground = onBackground,
            surface = background, onSurface = onBackground,
        )
    } else {
        lightColorScheme(
            primary = seed, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = Color(0xFF21005D),
            secondary = secondary, onSecondary = onPrimary,
            secondaryContainer = tertiaryContainer, onSecondaryContainer = Color(0xFF1D192B),
            tertiary = tertiary, onTertiary = onPrimary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = Color(0xFF31111D),
            background = background, onBackground = onBackground,
            surface = background, onSurface = onBackground,
        )
    }
}

/** 从位图提取主色（按饱和度加权，避免平均后偏灰）。 */
fun dominantColorFromBitmap(bitmap: Bitmap, sample: Int = 24): Color {
    val step = max(1, min(bitmap.width, bitmap.height) / sample)
    var sumR = 0.0
    var sumG = 0.0
    var sumB = 0.0
    var wsum = 0.0

    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val px = bitmap.getPixel(x, y)
            val r = (px shr 16 and 0xFF)
            val g = (px shr 8 and 0xFF)
            val b = (px and 0xFF)
            val hi = max(r, max(g, b))
            val lo = min(r, min(g, b))
            val sat = (hi - lo) / 255.0
            val weight = 0.1 + sat * sat
            sumR += r * weight
            sumG += g * weight
            sumB += b * weight
            wsum += weight
            x += step
        }
        y += step
    }
    if (wsum <= 0.0) return Color(0xFF6750A4)
    return Color(
        red = (sumR / wsum / 255.0).toFloat().coerceIn(0f, 1f),
        green = (sumG / wsum / 255.0).toFloat().coerceIn(0f, 1f),
        blue = (sumB / wsum / 255.0).toFloat().coerceIn(0f, 1f),
    )
}

// —— 颜色工具 ——

fun mix(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u,
    )
}

fun luminance(c: Color): Float {
    fun lin(v: Float): Float = if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
}

fun hueShift(c: Color, degrees: Float): Color {
    val hsl = rgbToHsl(c)
    return hslToColor((hsl[0] + degrees) % 360f, hsl[1], hsl[2], c.alpha)
}

fun rgbToHsl(c: Color): FloatArray {
    val r = c.red
    val g = c.green
    val b = c.blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val l = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> (g - b) / d + (if (g < b) 6f else 0f)
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } / 6f * 360f
    return floatArrayOf(h, s, l)
}

fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
    if (s <= 0f) return Color(l, l, l, alpha)
    val hue = ((h % 360f) + 360f) % 360f / 360f
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun chan(t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    return Color(chan(hue + 1f / 3f), chan(hue), chan(hue - 1f / 3f), alpha)
}
