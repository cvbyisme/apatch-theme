package me.bmax.apatch.ui.theme

import androidx.compose.ui.graphics.Color

/** 颜色来源 */
enum class ColorMode { PRESET, SYSTEM_DYNAMIC, CUSTOM_SEED, FROM_WALLPAPER }

/** 明暗模式（映射到官方 night_mode_* 键，向后兼容） */
enum class DarkMode { FOLLOW_SYSTEM, LIGHT, DARK }

/** 应用内图标容器形状 */
enum class IconShape { ROUND, SQUARE, CIRCLE }

/** 应用内图标着色 */
enum class IconTint { THEME, MONOCHROME, ORIGINAL }

/**
 * 统一主题配置。所有主题状态集中在这一处，由 [ThemeStore] 持久化，
 * [APatchTheme] 单点消费。
 */
data class ThemeConfig(
    // 颜色
    val colorMode: ColorMode = ColorMode.PRESET,
    val presetKey: String = "blue",
    val customSeedColor: Long = 0xFF6750A4,
    // 壁纸
    val wallpaperEnabled: Boolean = false,
    val wallpaperPath: String? = null,
    val wallpaperBlur: Float = 0f,
    val wallpaperScrim: Float = 0.22f,
    val wallpaperDarkScrim: Float = 0.45f,
    // 图标
    val iconShape: IconShape = IconShape.ROUND,
    val iconTint: IconTint = IconTint.THEME,
    // 字体
    val fontPath: String? = null,
    val fontScale: Float = 1.0f,
    // 明暗
    val darkMode: DarkMode = DarkMode.FOLLOW_SYSTEM,
) {
    /** 壁纸是否实际可显示 */
    val hasWallpaper: Boolean get() = wallpaperEnabled && !wallpaperPath.isNullOrBlank()

    val seedColor: Color get() = Color(customSeedColor)
}
