package ai.drako.overlay

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import ai.drako.effects.SpringChainGlowBorder
import ai.drako.effects.adaptiveFrostedGlass
import ai.drako.gestures.HandlePredictiveBack
import ai.drako.gestures.SwipeEdge
import ai.drako.gestures.backGestureScale
import ai.drako.gestures.backGestureTranslationX
import ai.drako.gestures.rememberPredictiveBackState
import ai.drako.system.DrakoMotion

// Pre-allocated theme
private val DarkScheme = darkColorScheme()

// Pre-allocated shapes
private val SheetShape = RoundedCornerShape(32.dp)
private val SheetShapeSmall = RoundedCornerShape(24.dp)

/**
 * High-End Overlay for flagship phones (FULL tier).
 *
 * Premium features:
 * - Native RenderEffect blur (Android 12+ only, fastest path)
 * - Spring Chain Glow (cascading multi-layer glow animation)
 * - LookaheadScope for predictive layout animations (CORE)
 * - M3 Expressive spring physics (CORE)
 * - Native PredictiveBackHandler integration
 *
 * Requirements:
 * - Android 12+ for blur
 * - High-end GPU for 60fps Spring Chain Glow
 * - 8GB+ RAM recommended
 *
 * @param onDismiss Called when overlay should be dismissed
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HighEndOverlay(onDismiss: () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme) {
        // CORE: LookaheadScope for predictive layout animations
        LookaheadScope {
            val backState = rememberPredictiveBackState()

            HandlePredictiveBack(
                state = backState,
                onBack = onDismiss
            )

            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isVisible = true
            }

            // CORE: M3 Expressive spatial spring (can bounce)
            val entryProgress by animateFloatAsState(
                targetValue = if (isVisible) 0f else 1f,
                animationSpec = DrakoMotion.Spatial.default,
                label = "entry"
            )

            val backProgress = backState.animatedProgress.value
            val combinedProgress = maxOf(entryProgress, backProgress)

            Box(modifier = Modifier.fillMaxSize()) {
                HighEndBottomSheet(
                    progress = combinedProgress,
                    swipeEdge = backState.swipeEdge,
                    isBackGesture = backState.isActive
                )
            }
        }
    }
}

@Composable
private fun BoxScope.HighEndBottomSheet(
    progress: Float,
    swipeEdge: SwipeEdge,
    isBackGesture: Boolean
) {
    val scale = backGestureScale(progress)
    val translationX = backGestureTranslationX(progress, swipeEdge, maxOffset = 80f)
    val translationY = if (!isBackGesture || swipeEdge == SwipeEdge.NONE) {
        lerp(0f, 400f, progress)
    } else 0f

    // Layered alpha: content fades first, glow persists
    val contentAlpha = lerp(1f, 0f, (progress * 2f).coerceIn(0f, 1f))
    val bgAlpha = lerp(1f, 0f, (progress * 1.5f).coerceIn(0f, 1f))
    val glowIntensity = lerp(1f, 0f, ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f))

    val cornerRadius = lerp(32f, 24f, progress).dp
    val shape = if (progress < 0.5f) SheetShape else SheetShapeSmall

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = translationX
                this.translationY = translationY

                if (isBackGesture) {
                    rotationY = when (swipeEdge) {
                        SwipeEdge.LEFT -> lerp(0f, -8f, progress)
                        SwipeEdge.RIGHT -> lerp(0f, 8f, progress)
                        SwipeEdge.NONE -> 0f
                    }
                }

                clip = true
                this.shape = shape
            }
    ) {
        // Layer 1: Native RenderEffect blur (fastest path for Android 12+)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha)
                .adaptiveFrostedGlass(
                    enabled = true,
                    blurRadius = 25f,
                    shape = shape
                )
        )

        // Layer 2: Top highlight
        TopHighlight(
            modifier = Modifier.align(Alignment.TopCenter),
            alpha = bgAlpha
        )

        // Layer 3: HIGH-END ONLY - Spring Chain Glow (cascading animation)
        SpringChainGlowBorder(
            leaderIntensity = glowIntensity,
            layerCount = 3,
            cornerRadius = cornerRadius,
            dampingRatio = 0.6f,
            baseStiffness = 200f
        )

        // Layer 4: Content
        OverlayContent(alpha = contentAlpha)
    }
}

/**
 * High-End Overlay with custom content.
 *
 * @param onDismiss Called when overlay should be dismissed
 * @param content Content to display inside the overlay
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HighEndOverlay(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    MaterialTheme(colorScheme = DarkScheme) {
        // CORE: LookaheadScope
        LookaheadScope {
            val backState = rememberPredictiveBackState()

            HandlePredictiveBack(
                state = backState,
                onBack = onDismiss
            )

            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isVisible = true
            }

            // CORE: M3 Expressive motion
            val entryProgress by animateFloatAsState(
                targetValue = if (isVisible) 0f else 1f,
                animationSpec = DrakoMotion.Spatial.default,
                label = "entry"
            )

            val combinedProgress = maxOf(entryProgress, backState.animatedProgress.value)
            val scale = backGestureScale(combinedProgress)
            val translationX = backGestureTranslationX(combinedProgress, backState.swipeEdge, 80f)
            val bgAlpha = lerp(1f, 0f, (combinedProgress * 1.5f).coerceIn(0f, 1f))
            val glowIntensity = lerp(1f, 0f, ((combinedProgress - 0.5f) / 0.5f).coerceIn(0f, 1f))

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .height(280.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.translationX = translationX
                            clip = true
                            shape = SheetShape
                        }
                ) {
                    // Native RenderEffect blur
                    Box(
                        Modifier
                            .fillMaxSize()
                            .alpha(bgAlpha)
                            .adaptiveFrostedGlass(enabled = true, shape = SheetShape)
                    )
                    TopHighlight(Modifier.align(Alignment.TopCenter), bgAlpha)
                    // HIGH-END: Spring Chain Glow
                    SpringChainGlowBorder(leaderIntensity = glowIntensity)
                    content()
                }
            }
        }
    }
}
