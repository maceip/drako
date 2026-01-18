package ai.drako.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Pre-allocated colors
private val TextSecondary = Color.White.copy(alpha = 0.7f)
private val TextTertiary = Color.White.copy(alpha = 0.5f)
private val StatusGreen = Color(0xFF00FF88)
private val HandleColor = Color.White

/**
 * The main content of the overlay bottom sheet.
 *
 * This is shared between high-end and adaptive overlays.
 * Only the container and effects change based on device capabilities.
 *
 * @param modifier Modifier for the content container
 * @param alpha Alpha for fade-out animation during dismiss
 */
@Composable
fun OverlayContent(
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .alpha(alpha)
    ) {
        // Drag handle indicator
        DragHandle(Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = "Drako Spatial AI",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(Modifier.height(12.dp))

        // Description
        Text(
            text = "Swipe from edges or down to dismiss.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        // Status indicator
        StatusIndicator()
    }
}

/**
 * Drag handle at the top of the sheet.
 */
@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 4.dp)
            .alpha(0.5f)
            .background(HandleColor, CircleShape)
    )
}

/**
 * Status indicator showing overlay is active.
 */
@Composable
private fun StatusIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Green dot
        Box(
            Modifier
                .size(8.dp)
                .background(StatusGreen, CircleShape)
        )

        // Status text
        Text(
            text = "Overlay Active",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

/**
 * Top highlight line for glass effect depth.
 */
@Composable
fun TopHighlight(
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .alpha(alpha)
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
    )
}
