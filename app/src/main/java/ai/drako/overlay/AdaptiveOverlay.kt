package ai.drako.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import ai.drako.effects.AdaptiveGlowBorder
import ai.drako.effects.hazeGlass
import ai.drako.effects.hazeBlurSource
import ai.drako.system.DrakoMotion
import ai.drako.system.FeatureTier
import ai.drako.system.ResourceMonitor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.abs

// Pre-allocated theme and colors
private val DarkScheme = darkColorScheme()
private val BgTop = Color(0xFF1A1A2E).copy(alpha = 0.95f)
private val BgBottom = Color(0xFF16213E).copy(alpha = 0.98f)
private val SolidBackground = Brush.verticalGradient(listOf(BgTop, BgBottom))

// Pre-allocated shapes
private val SheetShape = RoundedCornerShape(32.dp)
private val SheetShapeSmall = RoundedCornerShape(24.dp)

private enum class GestureEdge { NONE, LEFT, RIGHT, BOTTOM }

/**
 * Adaptive Overlay for mid-tier phones (HIGH/MEDIUM/LIGHT/MINIMAL tiers).
 *
 * Features by tier:
 * - FULL: Haze blur + animated glow + M3 springs (use HighEndOverlay instead for best perf)
 * - HIGH: Haze blur + animated glow + M3 springs
 * - MEDIUM: Haze blur + static glow + M3 springs
 * - LIGHT: No blur, static border + simple easing
 * - MINIMAL: No effects, instant transitions
 *
 * CORE features (all tiers):
 * - LookaheadScope for predictive layout animations
 * - M3 Expressive motion tokens (degraded to tween on LIGHT/MINIMAL)
 *
 * MED/LOW features:
 * - Haze-based blur (RenderScript fallback for older devices)
 * - Standard animated/static glow (not Spring Chain)
 *
 * @param onDismiss Called when overlay should be dismissed
 * @param initialTier Starting feature tier (auto-detected if null)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AdaptiveOverlay(
    onDismiss: () -> Unit,
    initialTier: FeatureTier? = null
) {
    val context = LocalContext.current

    // Resource monitoring
    val resourceMonitor = remember { ResourceMonitor(context) }
    LaunchedEffect(resourceMonitor) {
        resourceMonitor.start()
    }

    // Current tier from monitor
    val monitorTier by resourceMonitor.recommendedTier.collectAsState()
    val currentTier = initialTier ?: monitorTier

    // Animate tier transitions
    val tierLevel by animateFloatAsState(
        targetValue = currentTier.level.toFloat(),
        animationSpec = tween(durationMillis = 500),
        label = "tierLevel"
    )
    val effectiveTier = FeatureTier.fromLevel(tierLevel.toInt())

    // MED/LOW: Haze state for backdrop blur (RenderScript fallback for older devices)
    val hazeState = rememberHazeState()

    MaterialTheme(colorScheme = DarkScheme) {
        // CORE: LookaheadScope for predictive layout animations
        LookaheadScope {
            AdaptiveOverlayContent(
                tier = effectiveTier,
                hazeState = hazeState,
                onDismiss = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LookaheadScope.AdaptiveOverlayContent(
    tier: FeatureTier,
    hazeState: HazeState,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val screenWidth = remember(config) {
        with(density) { config.screenWidthDp.dp.toPx() }
    }
    val edgeZone = remember(density) {
        with(density) { 40.dp.toPx() }
    }

    // Gesture state
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var gestureEdge by remember { mutableStateOf(GestureEdge.NONE) }
    var isDragging by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var committed by remember { mutableStateOf(false) }

    // Entry animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val maxDrag = 300f

    val rawProgress = when (gestureEdge) {
        GestureEdge.LEFT -> (dragOffsetX / maxDrag).coerceIn(0f, 1f)
        GestureEdge.RIGHT -> (-dragOffsetX / maxDrag).coerceIn(0f, 1f)
        GestureEdge.BOTTOM -> (dragOffsetY / maxDrag).coerceIn(0f, 1f)
        GestureEdge.NONE -> 0f
    }

    // CORE: M3 Expressive motion (degraded to tween on lower tiers)
    val entryProgress by animateFloatAsState(
        targetValue = if (isVisible) 0f else 1f,
        animationSpec = if (tier.hasSpringPhysics) {
            DrakoMotion.Spatial.default
        } else {
            tween(durationMillis = 200)
        },
        label = "entry"
    )

    val gestureProgress by animateFloatAsState(
        targetValue = if (isExiting) 1f else rawProgress,
        animationSpec = if (tier.hasSpringPhysics) {
            if (isDragging) DrakoMotion.Spatial.snappy else DrakoMotion.Spatial.default
        } else {
            tween(durationMillis = if (isDragging) 50 else 150)
        },
        finishedListener = { if (isExiting) onDismiss() },
        label = "gesture"
    )

    val progress = maxOf(entryProgress, gestureProgress)

    // MED/LOW: Main container with Haze blur source
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeBlurSource(hazeState)
            .pointerInput(screenWidth) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        committed = false
                        isDragging = true
                        isExiting = false

                        gestureEdge = when {
                            startX < edgeZone -> GestureEdge.LEFT
                            startX > screenWidth - edgeZone -> GestureEdge.RIGHT
                            else -> GestureEdge.BOTTOM
                        }

                        do {
                            val event = awaitPointerEvent()
                            val drag = event.changes.firstOrNull()

                            if (drag != null && drag.pressed) {
                                val deltaX = drag.position.x - drag.previousPosition.x
                                val deltaY = drag.position.y - drag.previousPosition.y
                                dragOffsetX += deltaX
                                dragOffsetY += deltaY

                                if (!committed && gestureEdge != GestureEdge.BOTTOM) {
                                    val hDrag = abs(dragOffsetX)
                                    val vDrag = abs(dragOffsetY)
                                    if (hDrag > 20f) {
                                        if (hDrag > vDrag * 1.5f) {
                                            committed = true
                                            val correctDir = when (gestureEdge) {
                                                GestureEdge.LEFT -> dragOffsetX > 0
                                                GestureEdge.RIGHT -> dragOffsetX < 0
                                                else -> true
                                            }
                                            if (!correctDir) gestureEdge = GestureEdge.NONE
                                        } else if (vDrag > hDrag) {
                                            gestureEdge = GestureEdge.BOTTOM
                                            committed = true
                                        }
                                    }
                                } else if (!committed && gestureEdge == GestureEdge.BOTTOM) {
                                    if (dragOffsetY > 20f) committed = true
                                }

                                when (gestureEdge) {
                                    GestureEdge.LEFT -> dragOffsetX = dragOffsetX.coerceAtLeast(0f)
                                    GestureEdge.RIGHT -> dragOffsetX = dragOffsetX.coerceAtMost(0f)
                                    GestureEdge.BOTTOM -> dragOffsetY = dragOffsetY.coerceAtLeast(0f)
                                    GestureEdge.NONE -> {}
                                }

                                drag.consume()
                            }
                        } while (event.changes.any { it.pressed })

                        isDragging = false

                        if (committed) {
                            val shouldDismiss = when (gestureEdge) {
                                GestureEdge.LEFT -> dragOffsetX > maxDrag * 0.25f
                                GestureEdge.RIGHT -> dragOffsetX < -maxDrag * 0.25f
                                GestureEdge.BOTTOM -> dragOffsetY > maxDrag * 0.25f
                                GestureEdge.NONE -> false
                            }
                            if (shouldDismiss) {
                                isExiting = true
                            } else {
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                gestureEdge = GestureEdge.NONE
                            }
                        } else {
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                            gestureEdge = GestureEdge.NONE
                        }
                    }
                }
            }
    ) {
        AdaptiveBottomSheet(
            tier = tier,
            hazeState = hazeState,
            progress = progress,
            gestureEdge = gestureEdge,
            isExiting = isExiting
        )
    }
}

@Composable
private fun BoxScope.AdaptiveBottomSheet(
    tier: FeatureTier,
    hazeState: HazeState,
    progress: Float,
    gestureEdge: GestureEdge,
    isExiting: Boolean
) {
    val scale = if (tier.hasAnyEffects) {
        lerp(1f, 0.5f, progress)
    } else {
        1f
    }

    val contentAlpha = lerp(1f, 0f, (progress * 2f).coerceIn(0f, 1f))
    val bgAlpha = lerp(1f, 0f, (progress * 1.5f).coerceIn(0f, 1f))
    val glowAlpha = if (isExiting) {
        lerp(1f, 0f, ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f))
    } else 1f

    val cornerRadius = lerp(32f, 24f, progress).dp
    val shape = if (progress < 0.5f) SheetShape else SheetShapeSmall

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(280.dp)
            .then(
                if (tier.hasAnyEffects) {
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        when (gestureEdge) {
                            GestureEdge.LEFT -> {
                                translationX = lerp(0f, 150f, progress)
                                if (tier.hasSpringPhysics) {
                                    rotationY = lerp(0f, -12f, progress)
                                }
                            }
                            GestureEdge.RIGHT -> {
                                translationX = lerp(0f, -150f, progress)
                                if (tier.hasSpringPhysics) {
                                    rotationY = lerp(0f, 12f, progress)
                                }
                            }
                            else -> {
                                translationY = lerp(0f, 300f, progress)
                            }
                        }
                        clip = true
                        this.shape = shape
                    }
                } else {
                    Modifier.clip(SheetShape)
                }
            )
    ) {
        // Layer 1: Background
        // MED/LOW: Haze blur (RenderScript fallback for older devices)
        // LIGHT/MINIMAL: Solid background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha)
                .then(
                    if (tier.hasBlur) {
                        // MED/LOW: Haze with RenderScript fallback
                        Modifier.hazeGlass(
                            hazeState = hazeState,
                            tier = tier,
                            shape = shape
                        )
                    } else {
                        // LIGHT/MINIMAL: Solid fallback
                        Modifier
                            .clip(shape)
                            .background(SolidBackground)
                    }
                )
        )

        // Layer 2: Top highlight (skip on MINIMAL)
        if (tier.hasAnyEffects) {
            TopHighlight(
                modifier = Modifier.align(Alignment.TopCenter),
                alpha = bgAlpha
            )
        }

        // Layer 3: MED/LOW glow (NOT Spring Chain - that's high-end only)
        if (tier.hasStaticGlow) {
            AdaptiveGlowBorder(
                animated = tier.hasAnimatedGlow,
                static = tier.hasStaticGlow && !tier.hasAnimatedGlow,
                intensity = glowAlpha,
                cornerRadius = cornerRadius
            )
        }

        // Layer 4: Content
        OverlayContent(alpha = contentAlpha)
    }
}

/**
 * Adaptive Overlay with manual tier control.
 *
 * @param tier The feature tier to use
 * @param onDismiss Called when overlay should be dismissed
 */
@Composable
fun AdaptiveOverlay(
    tier: FeatureTier,
    onDismiss: () -> Unit
) {
    AdaptiveOverlay(onDismiss = onDismiss, initialTier = tier)
}
