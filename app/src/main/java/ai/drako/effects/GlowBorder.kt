package ai.drako.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Pre-allocated colors
private val GlowCyan = Color.Cyan
private val GlowCyanFaded = Color.Cyan.copy(alpha = 0.3f)
private val GlowCyanSubtle = Color.Cyan.copy(alpha = 0.15f)

/**
 * Animated glow border with a rotating highlight.
 *
 * Creates a "chasing light" effect that rotates around the border.
 * The animation runs at 60fps and completes one rotation every 3 seconds.
 *
 * Performance: Uses drawWithCache to cache Stroke objects. Colors are
 * pre-allocated. Only the sweep gradient is recreated each frame.
 *
 * @param intensity Alpha multiplier (0-1) for fade-out during dismiss
 * @param cornerRadius Corner radius to match the parent container
 * @param rotationDurationMs Time for one full rotation in milliseconds
 */
@Composable
fun AnimatedGlowBorder(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    cornerRadius: Dp = 32.dp,
    rotationDurationMs: Int = 3000
) {
    val transition = rememberInfiniteTransition(label = "glow")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(rotationDurationMs, easing = LinearEasing)
        ),
        label = "rotation"
    )

    GlowBorderCanvas(
        modifier = modifier,
        rotation = rotation,
        intensity = intensity,
        cornerRadius = cornerRadius
    )
}

/**
 * Static glow border without animation.
 *
 * Use this for lower-tier devices that can't afford animation overhead.
 * Still provides visual polish with a subtle cyan outline.
 *
 * @param intensity Alpha multiplier (0-1) for fade-out during dismiss
 * @param cornerRadius Corner radius to match the parent container
 */
@Composable
fun StaticGlowBorder(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    cornerRadius: Dp = 32.dp
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(intensity)
            .drawWithCache {
                val cornerPx = cornerRadius.toPx()
                val cornerRadiusObj = CornerRadius(cornerPx)
                val stroke = Stroke(width = 2.dp.toPx())

                onDrawBehind {
                    drawRoundRect(
                        color = GlowCyan.copy(alpha = 0.4f),
                        style = stroke,
                        cornerRadius = cornerRadiusObj
                    )
                }
            }
    )
}

/**
 * No glow border - just renders nothing.
 *
 * Use for minimal tier to avoid any rendering overhead.
 */
@Composable
fun NoGlowBorder(modifier: Modifier = Modifier) {
    // Intentionally empty
}

@Composable
private fun GlowBorderCanvas(
    modifier: Modifier,
    rotation: Float,
    intensity: Float,
    cornerRadius: Dp
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(intensity)
            .drawWithCache {
                // Cache immutable values
                val cornerPx = cornerRadius.toPx()
                val cornerRadiusObj = CornerRadius(cornerPx)
                val glowStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                val staticStroke = Stroke(width = 2.dp.toPx())

                onDrawBehind {
                    val angle = rotation / 360f

                    // Animated glow: sweep gradient with ~16% visible arc
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            (angle - 0.08f) to Color.Transparent,
                            (angle - 0.02f) to GlowCyanFaded,
                            angle to GlowCyan,
                            (angle + 0.02f) to GlowCyanFaded,
                            (angle + 0.08f) to Color.Transparent,
                            center = center
                        ),
                        style = glowStroke,
                        cornerRadius = cornerRadiusObj
                    )

                    // Static border: always visible subtle outline
                    drawRoundRect(
                        color = GlowCyanSubtle,
                        style = staticStroke,
                        cornerRadius = cornerRadiusObj
                    )
                }
            }
    )
}

/**
 * Adaptive glow border that renders based on feature tier.
 *
 * @param animated Whether to show animated glow (tier HIGH+)
 * @param static Whether to show static glow (tier MEDIUM)
 * @param intensity Alpha multiplier for fade-out
 * @param cornerRadius Corner radius to match container
 */
@Composable
fun AdaptiveGlowBorder(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    static: Boolean = true,
    intensity: Float = 1f,
    cornerRadius: Dp = 32.dp
) {
    when {
        animated -> AnimatedGlowBorder(
            modifier = modifier,
            intensity = intensity,
            cornerRadius = cornerRadius
        )
        static -> StaticGlowBorder(
            modifier = modifier,
            intensity = intensity,
            cornerRadius = cornerRadius
        )
        else -> NoGlowBorder(modifier = modifier)
    }
}

// ============================================================================
// Spring Chain Glow - Cascading follower animations for trailing glow effects
// ============================================================================

/**
 * Spring Chain Glow Border with cascading intensity.
 *
 * Creates a trailing glow effect where multiple glow layers follow
 * a leader intensity value with cascading spring animations.
 * Each layer trails behind the previous one, creating a smooth
 * "wake" effect during interactions.
 *
 * Based on AOSP SpringChainDemo pattern - each follower element
 * animates to follow the previous element's position.
 *
 * @param leaderIntensity The primary intensity value (0-1) that layers follow
 * @param layerCount Number of glow layers (default 3)
 * @param cornerRadius Corner radius to match container
 * @param dampingRatio Spring damping - lower = more bouncy trailing
 * @param baseStiffness Base spring stiffness - layers get progressively stiffer
 */
@Composable
fun SpringChainGlowBorder(
    modifier: Modifier = Modifier,
    leaderIntensity: Float = 1f,
    layerCount: Int = 3,
    cornerRadius: Dp = 32.dp,
    dampingRatio: Float = 0.6f,
    baseStiffness: Float = 200f
) {
    // Create spring chain - each layer follows the previous
    val layerIntensities = remember(layerCount) {
        Array(layerCount) { mutableStateOf(0f) }
    }

    // Animate each layer to follow the previous one
    for (i in 0 until layerCount) {
        val targetValue = if (i == 0) leaderIntensity else layerIntensities[i - 1].value
        val animatedValue by animateFloatAsState(
            targetValue = targetValue,
            animationSpec = spring(
                dampingRatio = dampingRatio,
                stiffness = baseStiffness + (i * 50f)  // Progressively stiffer
            ),
            label = "chainLayer$i"
        )
        layerIntensities[i].value = animatedValue
    }

    // Draw layered glows with cascading intensities
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val cornerPx = cornerRadius.toPx()
                val cornerRadiusObj = CornerRadius(cornerPx)

                // Pre-calculate strokes for each layer
                val strokes = Array(layerCount) { i ->
                    Stroke(
                        width = (6.dp.toPx() - (i * 1.5f)).coerceAtLeast(2f),
                        cap = StrokeCap.Round
                    )
                }

                onDrawBehind {
                    // Draw layers from back to front (outer to inner)
                    for (i in (layerCount - 1) downTo 0) {
                        val layerIntensity = layerIntensities[i].value
                        val layerAlpha = layerIntensity * (1f - i * 0.2f)

                        if (layerAlpha > 0.01f) {
                            // Each layer is slightly offset outward
                            val offset = i * 2f

                            drawRoundRect(
                                color = GlowCyan.copy(alpha = layerAlpha * 0.5f),
                                topLeft = androidx.compose.ui.geometry.Offset(-offset, -offset),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width + offset * 2,
                                    size.height + offset * 2
                                ),
                                style = strokes[i],
                                cornerRadius = CornerRadius(cornerPx + offset)
                            )
                        }
                    }

                    // Inner static border
                    drawRoundRect(
                        color = GlowCyanSubtle,
                        style = Stroke(width = 2.dp.toPx()),
                        cornerRadius = cornerRadiusObj
                    )
                }
            }
    )
}

/**
 * Reactive Spring Chain Glow that responds to touch pressure.
 *
 * Enhanced version that can react to gesture intensity,
 * creating a dynamic glow that pulses with interactions.
 *
 * @param intensity Current intensity (0-1), typically from gesture progress
 * @param isPressing Whether the user is actively pressing
 * @param cornerRadius Corner radius to match container
 */
@Composable
fun ReactiveSpringChainGlow(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    isPressing: Boolean = false,
    cornerRadius: Dp = 32.dp
) {
    // Boost intensity when pressing for tactile feedback
    val boostMultiplier by animateFloatAsState(
        targetValue = if (isPressing) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "pressBoost"
    )

    val effectiveIntensity = (intensity * boostMultiplier).coerceIn(0f, 1f)

    SpringChainGlowBorder(
        modifier = modifier,
        leaderIntensity = effectiveIntensity,
        layerCount = if (isPressing) 4 else 3,  // More layers when pressing
        cornerRadius = cornerRadius,
        dampingRatio = if (isPressing) 0.5f else 0.6f,  // Bouncier when pressing
        baseStiffness = if (isPressing) 300f else 200f
    )
}
