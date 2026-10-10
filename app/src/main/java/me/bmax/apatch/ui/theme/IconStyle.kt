package me.bmax.apatch.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** 应用内图标风格（形状 + 着色），由 APatchTheme 通过 [LocalIconStyle] 提供。 */
@Immutable
data class IconStyleConfig(
    val shape: IconShape = IconShape.ROUND,
    val tint: IconTint = IconTint.THEME,
)

val LocalIconStyle = staticCompositionLocalOf { IconStyleConfig() }

@Composable
fun iconContainerShape(shape: IconShape): Shape = when (shape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.SQUARE -> RoundedCornerShape(6.dp)
    IconShape.ROUND -> RoundedCornerShape(12.dp)
}

/**
 * 支持主题图标风格的 Icon 包装。
 * [container] 为 true 时，在图标外层套一个带形状/底色的容器。
 */
@Composable
fun ThemedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    container: Boolean = false,
) {
    val style = LocalIconStyle.current
    val tint = when (style.tint) {
        IconTint.THEME -> LocalContentColor.current
        IconTint.MONOCHROME -> MaterialTheme.colorScheme.onSurface
        IconTint.ORIGINAL -> Color.Unspecified
    }
    if (container) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(iconContainerShape(style.shape))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector, contentDescription, tint = tint)
        }
    } else {
        Icon(imageVector, contentDescription, modifier = modifier, tint = tint)
    }
}
