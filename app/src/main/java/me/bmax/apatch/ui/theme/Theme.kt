package me.bmax.apatch.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import me.bmax.apatch.ui.webui.MonetColorsProvider

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode }, navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}

val refreshTheme = MutableLiveData(false)

@Composable
fun APatchTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var config by remember { mutableStateOf(ThemeStore.load()) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)
    if (refreshThemeObserver == true) {
        config = ThemeStore.load()
        refreshTheme.postValue(false)
    }

    val darkTheme = when (config.darkMode) {
        DarkMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkMode.DARK -> true
        DarkMode.LIGHT -> false
    }

    val targetScheme = resolveColorScheme(config, darkTheme, context)
    val schemeWithWallpaper =
        if (config.hasWallpaper) targetScheme.withWallpaperAlpha() else targetScheme
    // 华丽 MD3 动效：主题切换时关键色平滑过渡
    val colorScheme = animateColorScheme(schemeWithWallpaper)
    val typography = appTypography(config)

    SystemBarStyle(darkMode = darkTheme)

    Box(Modifier.fillMaxSize()) {
        WallpaperLayer(config, darkTheme)
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = {
                MonetColorsProvider.UpdateCss()
                CompositionLocalProvider(
                    LocalIconStyle provides IconStyleConfig(config.iconShape, config.iconTint)
                ) {
                    content()
                }
            }
        )
    }
}

@Composable
private fun resolveColorScheme(config: ThemeConfig, darkTheme: Boolean, context: Context): ColorScheme {
    return when (config.colorMode) {
        ColorMode.PRESET -> presetColorScheme(config.presetKey, darkTheme)

        ColorMode.SYSTEM_DYNAMIC -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            darkTheme -> DarkBlueTheme
            else -> LightBlueTheme
        }

        ColorMode.CUSTOM_SEED -> colorSchemeFromSeed(config.seedColor, darkTheme)

        ColorMode.FROM_WALLPAPER -> {
            val seed = remember(config.wallpaperPath) {
                config.wallpaperPath?.let { wallpaperSeedColor(it) }
            } ?: config.seedColor
            colorSchemeFromSeed(seed, darkTheme)
        }
    }
}

@Composable
private fun presetColorScheme(presetKey: String, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        when (presetKey) {
            "amber" -> DarkAmberTheme
            "blue_grey" -> DarkBlueGreyTheme
            "blue" -> DarkBlueTheme
            "brown" -> DarkBrownTheme
            "cyan" -> DarkCyanTheme
            "deep_orange" -> DarkDeepOrangeTheme
            "deep_purple" -> DarkDeepPurpleTheme
            "green" -> DarkGreenTheme
            "indigo" -> DarkIndigoTheme
            "light_blue" -> DarkLightBlueTheme
            "light_green" -> DarkLightGreenTheme
            "lime" -> DarkLimeTheme
            "orange" -> DarkOrangeTheme
            "pink" -> DarkPinkTheme
            "purple" -> DarkPurpleTheme
            "red" -> DarkRedTheme
            "sakura" -> DarkSakuraTheme
            "teal" -> DarkTealTheme
            "yellow" -> DarkYellowTheme
            else -> DarkBlueTheme
        }
    } else {
        when (presetKey) {
            "amber" -> LightAmberTheme
            "blue_grey" -> LightBlueGreyTheme
            "blue" -> LightBlueTheme
            "brown" -> LightBrownTheme
            "cyan" -> LightCyanTheme
            "deep_orange" -> LightDeepOrangeTheme
            "deep_purple" -> LightDeepPurpleTheme
            "green" -> LightGreenTheme
            "indigo" -> LightIndigoTheme
            "light_blue" -> LightLightBlueTheme
            "light_green" -> LightLightGreenTheme
            "lime" -> LightLimeTheme
            "orange" -> LightOrangeTheme
            "pink" -> LightPinkTheme
            "purple" -> LightPurpleTheme
            "red" -> LightRedTheme
            "sakura" -> LightSakuraTheme
            "teal" -> LightTealTheme
            "yellow" -> LightYellowTheme
            else -> LightBlueTheme
        }
    }
}

/** 启用壁纸时，让背景/表面半透明，使壁纸从下层透出。 */
private fun ColorScheme.withWallpaperAlpha(): ColorScheme {
    return copy(
        background = background.copy(alpha = 0.45f),
        surface = surface.copy(alpha = 0.82f),
        surfaceVariant = surfaceVariant.copy(alpha = 0.78f),
    )
}

/** 主题切换时对关键色做弹簧过渡（MD3 动效）。 */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val primary by animateColorAsState(target.primary, spring(stiffness = Spring.StiffnessLow))
    val onPrimary by animateColorAsState(target.onPrimary)
    val primaryContainer by animateColorAsState(target.primaryContainer)
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer)
    val secondary by animateColorAsState(target.secondary)
    val tertiary by animateColorAsState(target.tertiary)
    val background by animateColorAsState(target.background)
    val onBackground by animateColorAsState(target.onBackground)
    val surface by animateColorAsState(target.surface)
    val onSurface by animateColorAsState(target.onSurface)
    val surfaceVariant by animateColorAsState(target.surfaceVariant)
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant)
    val outline by animateColorAsState(target.outline)

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        tertiary = tertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
    )
}

/** 从壁纸文件解码并提取主色（降采样，轻量）。 */
private fun wallpaperSeedColor(path: String): Color? {
    return try {
        val opts = BitmapFactory.Options().apply { inSampleSize = 16 }
        val bmp = BitmapFactory.decodeFile(path, opts) ?: return null
        dominantColorFromBitmap(bmp)
    } catch (_: Exception) {
        null
    }
}
