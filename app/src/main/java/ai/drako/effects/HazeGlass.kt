package ai.drako.effects

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.drako.system.FeatureTier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Haze-based glass effect system for Drako overlays.
 *
 * Haze provides true backdrop blur across more Android versions by using
 * different rendering strategies per platform:
 * - Android 12+: RenderEffect
 * - Android <12: RenderScript fallback
 *
 * Key concepts:
 * - hazeSource: Applied to content that should be blurred behind the glass
 * - hazeEffect: Applied to the glass surface that shows the blur
 */

// Pre-allocated colors for glass tinting
private val GlassTintColor = Color(0xFF1A1A2E)
private val FallbackTop = Color(0xFF1A1A2E).copy(alpha = 0.95f)
private val FallbackBottom = Color(0xFF16213E).copy(alpha = 0.98f)
internal val HazeFallbackBrush = Brush.verticalGradient(listOf(FallbackTop, FallbackBottom))

/**
 * Drako's standard glass style using Haze.
 *
 * Creates a frosted glass appearance with:
 * - 25dp blur radius
 * - Dark tint overlay
 * - Subtle noise for texture
 */
val DrakoGlassStyle = HazeStyle(
    blurRadius = 25.dp,
    tint = HazeTint(GlassTintColor.copy(alpha = 0.7f)),
    noiseFactor = 0.1f
)

/**
 * Lighter glass style with less blur.
 * Use for secondary surfaces or when performance is a concern.
 */
val DrakoGlassStyleLight = HazeStyle(
    blurRadius = 15.dp,
    tint = HazeTint(GlassTintColor.copy(alpha = 0.6f)),
    noiseFactor = 0.05f
)

/**
 * Heavy glass style with more blur.
 * Use for primary overlays on high-end devices.
 */
val DrakoGlassStyleHeavy = HazeStyle(
    blurRadius = 35.dp,
    tint = HazeTint(GlassTintColor.copy(alpha = 0.8f)),
    noiseFactor = 0.15f
)

/**
 * Apply Haze glass effect to a composable.
 *
 * This modifier creates a frosted glass appearance by blurring content
 * marked with `hazeSource` that appears behind this composable.
 *
 * @param hazeState Shared state from `rememberHazeState()` that connects source and effect
 * @param tier Current feature tier - falls back to solid on MINIMAL/LIGHT
 * @param style Glass style to apply (blur, tint, noise)
 * @param shape Shape to clip the glass effect
 */
@Composable
fun Modifier.hazeGlass(
    hazeState: HazeState,
    tier: FeatureTier,
    style: HazeStyle = DrakoGlassStyle,
    shape: Shape
): Modifier {
    return when {
        // FULL tier: Real Haze blur
        tier.hasBlur -> this
            .hazeEffect(
                state = hazeState,
                style = style
            )
            .clip(shape)

        // Lower tiers: Solid fallback
        else -> this
            .clip(shape)
            .background(HazeFallbackBrush)
    }
}

/**
 * Apply Haze glass effect with custom parameters.
 *
 * Use this when you need fine-grained control over the blur appearance.
 *
 * @param hazeState Shared state connecting source and effect
 * @param enabled Whether to apply blur (false = solid fallback)
 * @param blurRadius Blur radius in dp
 * @param tintColor Color overlay on the blur
 * @param tintAlpha Alpha of the tint overlay
 * @param noiseFactor Amount of noise texture (0-1)
 * @param shape Shape to clip the effect
 */
@Composable
fun Modifier.hazeGlassCustom(
    hazeState: HazeState,
    enabled: Boolean = true,
    blurRadius: Dp = 25.dp,
    tintColor: Color = GlassTintColor,
    tintAlpha: Float = 0.7f,
    noiseFactor: Float = 0.1f,
    shape: Shape
): Modifier {
    return if (enabled) {
        this
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    blurRadius = blurRadius,
                    tint = HazeTint(tintColor.copy(alpha = tintAlpha)),
                    noiseFactor = noiseFactor
                )
            )
            .clip(shape)
    } else {
        this
            .clip(shape)
            .background(HazeFallbackBrush)
    }
}

/**
 * Mark content as a blur source for Haze.
 *
 * Apply this to the content that should appear blurred behind glass surfaces.
 * Typically applied to the background/main content container.
 *
 * @param hazeState Shared state that connects this source to hazeEffect consumers
 */
@Composable
fun Modifier.hazeBlurSource(hazeState: HazeState): Modifier {
    return this.hazeSource(state = hazeState)
}

/**
 * Material-style glass using Haze Materials library.
 *
 * Provides pre-configured material styles matching iOS/macOS vibrancy.
 *
 * @param hazeState Shared state connecting source and effect
 * @param tier Feature tier for fallback handling
 * @param material Material style to apply
 * @param shape Shape to clip the effect
 */
@Composable
fun Modifier.hazeMaterialGlass(
    hazeState: HazeState,
    tier: FeatureTier,
    material: HazeMaterial = HazeMaterial.REGULAR,
    shape: Shape
): Modifier {
    return when {
        tier.hasBlur -> {
            val style = when (material) {
                HazeMaterial.ULTRA_THIN -> HazeMaterials.ultraThin()
                HazeMaterial.THIN -> HazeMaterials.thin()
                HazeMaterial.REGULAR -> HazeMaterials.regular()
                HazeMaterial.THICK -> HazeMaterials.thick()
                HazeMaterial.ULTRA_THICK -> HazeMaterials.ultraThick()
            }
            this
                .hazeEffect(state = hazeState, style = style)
                .clip(shape)
        }
        else -> this
            .clip(shape)
            .background(HazeFallbackBrush)
    }
}

/**
 * Material styles matching system vibrancy levels.
 */
enum class HazeMaterial {
    ULTRA_THIN,
    THIN,
    REGULAR,
    THICK,
    ULTRA_THICK
}
