package ai.drako.effects

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import ai.drako.system.FeatureTier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

// Pre-allocated colors for frosted glass effect
private val FrostedTop = Color(0xFF1A1A2E).copy(alpha = 0.85f)
private val FrostedBottom = Color(0xFF16213E).copy(alpha = 0.92f)
private val FallbackTop = Color(0xFF1A1A2E).copy(alpha = 0.95f)
private val FallbackBottom = Color(0xFF16213E).copy(alpha = 0.98f)

// Pre-allocated brushes
private val FrostedBrush = Brush.verticalGradient(listOf(FrostedTop, FrostedBottom))
internal val FallbackBrush = Brush.verticalGradient(listOf(FallbackTop, FallbackBottom))

/**
 * Frosted glass effect using RenderEffect blur.
 *
 * This effect blurs the content *behind* the composable, creating
 * a translucent frosted glass appearance. Only available on Android 12+.
 *
 * @param blurRadius Blur radius in pixels (higher = more blur)
 * @param shape Shape to clip the effect to
 * @param fallbackToSolid If true, use solid background on unsupported devices
 */
@Composable
fun Modifier.frostedGlass(
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape,
    fallbackToSolid: Boolean = true
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+: Real blur effect
        this
            .clip(shape)
            .graphicsLayer {
                renderEffect = RenderEffect
                    .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
            .background(brush = FrostedBrush)
    } else if (fallbackToSolid) {
        // Older Android: Solid background (more opaque to compensate for no blur)
        this
            .clip(shape)
            .background(brush = FallbackBrush)
    } else {
        this
    }
}

/**
 * Check if the device supports hardware blur effects.
 *
 * Blur requires Android 12 (API 31) or higher.
 */
fun supportsBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Adaptive frosted glass that respects feature tiers.
 *
 * @param enabled Whether blur should be applied (false on lower tiers)
 * @param blurRadius Blur radius when enabled
 * @param shape Shape to clip the effect to
 */
@Composable
fun Modifier.adaptiveFrostedGlass(
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape
): Modifier {
    return if (enabled && supportsBlur()) {
        frostedGlass(blurRadius = blurRadius, shape = shape)
    } else {
        // Fallback: just use the gradient background
        this
            .clip(shape)
            .background(brush = FallbackBrush)
    }
}

/**
 * Animated blur that can transition smoothly.
 *
 * Use this for tier transitions where blur fades in/out.
 *
 * @param blurAmount Current blur amount (0 = no blur, 1 = full blur)
 * @param maxBlurRadius Maximum blur radius at blurAmount = 1
 * @param shape Shape to clip the effect to
 */
@Composable
fun Modifier.animatedFrostedGlass(
    blurAmount: Float = 1f,
    maxBlurRadius: Float = 25f,
    shape: Shape = RectangleShape
): Modifier {
    val actualRadius = maxBlurRadius * blurAmount.coerceIn(0f, 1f)

    return if (actualRadius > 0.5f && supportsBlur()) {
        frostedGlass(blurRadius = actualRadius, shape = shape)
    } else {
        // No blur or blur too small to matter
        this
            .clip(shape)
            .background(brush = FallbackBrush)
    }
}

/**
 * Background options for different tiers.
 */
object FrostedGlassBackgrounds {
    /**
     * Full frosted glass with blur (Android 12+ only)
     */
    val frosted = FrostedBrush

    /**
     * Fallback solid background for older devices or lower tiers
     */
    val solid = FallbackBrush

    /**
     * More transparent background for when blur is enabled
     */
    val transparent = Brush.verticalGradient(
        listOf(
            Color(0xFF1A1A2E).copy(alpha = 0.6f),
            Color(0xFF16213E).copy(alpha = 0.75f)
        )
    )
}

/**
 * Hybrid frosted glass that uses Haze when available.
 *
 * This function provides the best blur experience by using:
 * - Haze library: Better cross-platform support with RenderScript fallback
 * - RenderEffect: Native Android 12+ blur (used within Haze)
 * - Solid fallback: For devices/tiers where blur is disabled
 *
 * @param hazeState Optional Haze state for backdrop blur
 * @param tier Current feature tier
 * @param blurRadius Blur radius for RenderEffect fallback
 * @param shape Shape to clip the effect to
 */
@Composable
fun Modifier.hybridFrostedGlass(
    hazeState: HazeState? = null,
    tier: FeatureTier = FeatureTier.FULL,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape
): Modifier {
    return when {
        // If Haze state provided and blur enabled, use Haze
        hazeState != null && tier.hasBlur -> this
            .hazeEffect(state = hazeState, style = DrakoGlassStyle)
            .clip(shape)

        // Fallback to RenderEffect if no Haze state but blur enabled
        tier.hasBlur && supportsBlur() -> frostedGlass(
            blurRadius = blurRadius,
            shape = shape
        )

        // Solid fallback for lower tiers or older devices
        else -> this
            .clip(shape)
            .background(brush = FallbackBrush)
    }
}
