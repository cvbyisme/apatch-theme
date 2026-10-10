package me.bmax.apatch.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

/**
 * 全局壁纸层：铺底 + 模糊 + 遮罩（保证文字可读）。放在 APatchTheme 最外层。
 */
@Composable
fun WallpaperLayer(config: ThemeConfig, darkTheme: Boolean, modifier: Modifier = Modifier) {
    if (!config.hasWallpaper) return
    val file = remember(config.wallpaperPath) { File(config.wallpaperPath) }

    Box(modifier.fillMaxSize()) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (config.wallpaperBlur > 0f) Modifier.blur(config.wallpaperBlur.dp) else Modifier),
        )
        val scrim = if (darkTheme) config.wallpaperDarkScrim else config.wallpaperScrim
        if (scrim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrim.coerceIn(0f, 1f))),
            )
        }
    }
}
