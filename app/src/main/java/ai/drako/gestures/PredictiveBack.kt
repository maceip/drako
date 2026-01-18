package ai.drako.gestures

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * State holder for predictive back gesture.
 *
 * Provides progress (0-1) and swipe edge information for
 * animating the overlay during back gesture.
 */
class PredictiveBackState {
    var isActive by mutableStateOf(false)
        internal set

    var progress by mutableFloatStateOf(0f)
        internal set

    var swipeEdge by mutableStateOf(SwipeEdge.NONE)
        internal set

    // Animated progress for smooth spring-based following
    internal val animatedProgress = Animatable(0f)
}

enum class SwipeEdge {
    NONE,
    LEFT,   // User swiped from left edge
    RIGHT   // User swiped from right edge
}

/**
 * Remember a predictive back state that persists across recompositions.
 */
@Composable
fun rememberPredictiveBackState(): PredictiveBackState {
    return remember { PredictiveBackState() }
}

/**
 * Handle predictive back gesture using the native Android API.
 *
 * This composable:
 * 1. Intercepts the system back gesture
 * 2. Provides real-time progress updates
 * 3. Calls onBack when gesture completes
 * 4. Resets state on cancellation
 *
 * Usage:
 * ```
 * val backState = rememberPredictiveBackState()
 *
 * HandlePredictiveBack(
 *     state = backState,
 *     onBack = { dismiss() }
 * )
 *
 * // Use backState.progress to animate your UI
 * Box(
 *     modifier = Modifier.graphicsLayer {
 *         scaleX = lerp(1f, 0.9f, backState.animatedProgress.value)
 *     }
 * )
 * ```
 *
 * @param state The state holder for back gesture
 * @param enabled Whether to handle back gestures
 * @param onBack Called when back gesture completes
 */
@Composable
fun HandlePredictiveBack(
    state: PredictiveBackState,
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    PredictiveBackHandler(enabled = enabled) { backEvents: Flow<BackEventCompat> ->
        try {
            state.isActive = true

            backEvents.collect { event ->
                // Determine swipe edge from first event
                if (state.swipeEdge == SwipeEdge.NONE) {
                    state.swipeEdge = when (event.swipeEdge) {
                        BackEventCompat.EDGE_LEFT -> SwipeEdge.LEFT
                        BackEventCompat.EDGE_RIGHT -> SwipeEdge.RIGHT
                        else -> SwipeEdge.LEFT
                    }
                }

                // Update progress
                state.progress = event.progress

                // Animate to target with spring for smooth following
                scope.launch {
                    state.animatedProgress.animateTo(
                        targetValue = event.progress,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        )
                    )
                }
            }

            // Gesture completed - animate to 1.0 and dismiss
            state.animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = 300f
                )
            )
            onBack()

        } catch (e: CancellationException) {
            // User cancelled the gesture - animate back to start
            scope.launch {
                state.animatedProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 400f
                    )
                )
            }
        } finally {
            // Reset state
            state.isActive = false
            state.progress = 0f
            state.swipeEdge = SwipeEdge.NONE
        }
    }
}

/**
 * Predictive back progress transformed with decelerate interpolation.
 *
 * Android design guidelines recommend using STANDARD_DECELERATE
 * (PathInterpolator(0f, 0f, 0f, 1f)) for predictive back animations.
 *
 * This provides a more natural feel where initial movement is
 * larger and it settles as the gesture progresses.
 */
fun deceleratedProgress(rawProgress: Float): Float {
    // Decelerate: starts fast, ends slow
    // Approximation of PathInterpolator(0f, 0f, 0f, 1f)
    return 1f - (1f - rawProgress) * (1f - rawProgress)
}

/**
 * Calculate scale based on predictive back progress.
 *
 * Uses the recommended range of 90% to 100% for the preview.
 */
fun backGestureScale(progress: Float): Float {
    val decel = deceleratedProgress(progress)
    return androidx.compose.ui.util.lerp(1f, 0.9f, decel)
}

/**
 * Calculate horizontal translation based on swipe edge and progress.
 *
 * Moves the content toward the edge being swiped from.
 */
fun backGestureTranslationX(progress: Float, edge: SwipeEdge, maxOffset: Float = 100f): Float {
    val decel = deceleratedProgress(progress)
    return when (edge) {
        SwipeEdge.LEFT -> androidx.compose.ui.util.lerp(0f, maxOffset, decel)
        SwipeEdge.RIGHT -> androidx.compose.ui.util.lerp(0f, -maxOffset, decel)
        SwipeEdge.NONE -> 0f
    }
}
