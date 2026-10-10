package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import me.bmax.apatch.APApplication
import java.io.File

/**
 * 主题配置与素材文件的唯一持久化入口。
 * 配置走 SharedPreferences（沿用官方 SP 名 "config"，键加 theme. 前缀隔离）；
 * 二进制素材（壁纸/字体）存私有目录 filesDir/themes/，导入时立即落盘（SAF URI 授权重启会失效）。
 */
object ThemeStore {
    private const val DIR = "themes"

    fun themesDir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    // —— 官方原有键（兼容读写，避免破坏既有配置） ——
    private const val KEY_NIGHT_FOLLOW_SYS = "night_mode_follow_sys"
    private const val KEY_NIGHT_ENABLED = "night_mode_enabled"
    private const val KEY_USE_SYSTEM_COLOR = "use_system_color_theme"
    private const val KEY_CUSTOM_COLOR = "custom_color"

    // —— 新增键 ——
    private const val KEY_COLOR_MODE = "theme.color_mode"
    private const val KEY_SEED = "theme.seed_color"
    private const val KEY_WALL_ENABLED = "theme.wallpaper_enabled"
    private const val KEY_WALL_PATH = "theme.wallpaper_path"
    private const val KEY_WALL_BLUR = "theme.wallpaper_blur"
    private const val KEY_WALL_SCRIM = "theme.wallpaper_scrim"
    private const val KEY_WALL_DARK_SCRIM = "theme.wallpaper_dark_scrim"
    private const val KEY_ICON_SHAPE = "theme.icon_shape"
    private const val KEY_ICON_TINT = "theme.icon_tint"
    private const val KEY_FONT_PATH = "theme.font_path"
    private const val KEY_FONT_SCALE = "theme.font_scale"

    private fun prefs() = APApplication.sharedPreferences

    fun load(): ThemeConfig {
        val p = prefs()
        val followSys = p.getBoolean(KEY_NIGHT_FOLLOW_SYS, true)
        val night = p.getBoolean(KEY_NIGHT_ENABLED, false)
        val darkMode = when {
            followSys -> DarkMode.FOLLOW_SYSTEM
            night -> DarkMode.DARK
            else -> DarkMode.LIGHT
        }
        val colorMode = enumOrNull<ColorMode>(p.getString(KEY_COLOR_MODE, null))
            ?: if (p.getBoolean(KEY_USE_SYSTEM_COLOR, true)) ColorMode.SYSTEM_DYNAMIC else ColorMode.PRESET

        return ThemeConfig(
            colorMode = colorMode,
            presetKey = p.getString(KEY_CUSTOM_COLOR, "blue") ?: "blue",
            customSeedColor = p.getLong(KEY_SEED, 0xFF6750A4),
            wallpaperEnabled = p.getBoolean(KEY_WALL_ENABLED, false),
            wallpaperPath = p.getString(KEY_WALL_PATH, null),
            wallpaperBlur = p.getFloat(KEY_WALL_BLUR, 0f),
            wallpaperScrim = p.getFloat(KEY_WALL_SCRIM, 0.22f),
            wallpaperDarkScrim = p.getFloat(KEY_WALL_DARK_SCRIM, 0.45f),
            iconShape = enumOrNull<IconShape>(p.getString(KEY_ICON_SHAPE, null)) ?: IconShape.ROUND,
            iconTint = enumOrNull<IconTint>(p.getString(KEY_ICON_TINT, null)) ?: IconTint.THEME,
            fontPath = p.getString(KEY_FONT_PATH, null),
            fontScale = p.getFloat(KEY_FONT_SCALE, 1.0f),
            darkMode = darkMode,
        )
    }

    fun save(config: ThemeConfig) {
        prefs().edit {
            putBoolean(KEY_NIGHT_FOLLOW_SYS, config.darkMode == DarkMode.FOLLOW_SYSTEM)
            putBoolean(KEY_NIGHT_ENABLED, config.darkMode == DarkMode.DARK)
            putBoolean(KEY_USE_SYSTEM_COLOR, config.colorMode == ColorMode.SYSTEM_DYNAMIC)
            putString(KEY_CUSTOM_COLOR, config.presetKey)

            putString(KEY_COLOR_MODE, config.colorMode.name)
            putLong(KEY_SEED, config.customSeedColor)
            putBoolean(KEY_WALL_ENABLED, config.wallpaperEnabled)
            putString(KEY_WALL_PATH, config.wallpaperPath)
            putFloat(KEY_WALL_BLUR, config.wallpaperBlur)
            putFloat(KEY_WALL_SCRIM, config.wallpaperScrim)
            putFloat(KEY_WALL_DARK_SCRIM, config.wallpaperDarkScrim)
            putString(KEY_ICON_SHAPE, config.iconShape.name)
            putString(KEY_ICON_TINT, config.iconTint.name)
            putString(KEY_FONT_PATH, config.fontPath)
            putFloat(KEY_FONT_SCALE, config.fontScale)
        }
    }

    /** 导入图片/字体到私有目录，返回落盘绝对路径；失败返回 null。 */
    fun importFile(context: Context, uri: Uri, kind: String): File? {
        return try {
            val ext = inferExt(context, uri, kind)
            val dest = File(themesDir(context), "${kind}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest
        } catch (_: Exception) {
            null
        }
    }

    /** 删除指定素材文件（存在才删）。 */
    fun deleteFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun inferExt(context: Context, uri: Uri, kind: String): String {
        val mime = context.contentResolver.getType(uri) ?: ""
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("otf") -> "otf"
            mime.contains("ttf") || mime.contains("font") -> "ttf"
            else -> if (kind == "font") "ttf" else "jpg"
        }
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? {
        if (name.isNullOrBlank()) return null
        return try {
            enumValueOf<T>(name)
        } catch (_: Exception) {
            null
        }
    }
}
