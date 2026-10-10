package me.bmax.apatch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.io.File

/** 加载导入的字体文件为 FontFamily；不存在或解析失败则回退系统默认。 */
fun fontFamilyFromPath(path: String?): FontFamily {
    if (path.isNullOrBlank()) return FontFamily.Default
    return try {
        val file = File(path)
        if (file.exists() && file.length() > 0) FontFamily(Font(file)) else FontFamily.Default
    } catch (_: Exception) {
        FontFamily.Default
    }
}

/** 根据配置生成动态 Typography：支持导入字体 + 全局字号缩放。 */
fun appTypography(config: ThemeConfig): Typography {
    val family = fontFamilyFromPath(config.fontPath)
    val s = config.fontScale.coerceIn(0.8f, 1.6f)

    fun t(size: Float, weight: FontWeight = FontWeight.Normal, lh: Float = size * 1.5f) =
        TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = size.sp * s,
            lineHeight = lh.sp * s,
        )

    return Typography(
        bodyLarge = t(16f),
        bodyMedium = t(14f),
        bodySmall = t(12f),
        titleLarge = t(22f),
        titleMedium = t(16f, FontWeight.Medium),
        titleSmall = t(14f, FontWeight.Medium),
        headlineSmall = t(24f),
        headlineMedium = t(28f),
        labelLarge = t(14f, FontWeight.Medium),
        labelMedium = t(12f, FontWeight.Medium),
        labelSmall = t(11f, FontWeight.Medium),
    )
}
