package ai.drako

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.abs

/**
 * Drako Overlay Service
 *
 * A foreground service that displays a floating bottom sheet overlay with:
 * - Animated cyan glow border
 * - Predictive back gesture support (swipe from edges or swipe down)
 * - Smooth shrink-to-dismiss animation
 */
class DrakoOverlayService : Service(),
    androidx.lifecycle.LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            showOverlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Drako Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Drako overlay is active"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Drako AI Active")
            .setContentText("Tap to reopen")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DrakoOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DrakoOverlayService)
            setViewTreeViewModelStoreOwner(this@DrakoOverlayService)

            setContent {
                DrakoOverlay(onDismiss = {
                    sendBroadcast(Intent(ACTION_DISMISS))
                    stopSelf()
                })
            }
        }

        windowManager.addView(overlayView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    companion object {
        private const val CHANNEL_ID = "drako_overlay"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "ai.drako.DISMISS_OVERLAY"
    }
}

// ============================================================================
// Composables
// ============================================================================

private enum class GestureEdge { NONE, LEFT, RIGHT, BOTTOM }

@Composable
private fun DrakoOverlay(onDismiss: () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        val screenWidth = with(density) { config.screenWidthDp.dp.toPx() }
        val edgeZone = with(density) { 40.dp.toPx() }

        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        var gestureEdge by remember { mutableStateOf(GestureEdge.NONE) }
        var isDragging by remember { mutableStateOf(false) }
        var isExiting by remember { mutableStateOf(false) }
        var committed by remember { mutableStateOf(false) }

        val maxDrag = 300f

        val rawProgress = when (gestureEdge) {
            GestureEdge.LEFT -> (dragOffset.x / maxDrag).coerceIn(0f, 1f)
            GestureEdge.RIGHT -> (-dragOffset.x / maxDrag).coerceIn(0f, 1f)
            GestureEdge.BOTTOM -> (dragOffset.y / maxDrag).coerceIn(0f, 1f)
            GestureEdge.NONE -> 0f
        }

        val progress by animateFloatAsState(
            targetValue = if (isExiting) 1f else rawProgress,
            animationSpec = if (isDragging) spring(1f, 800f) else spring(0.6f, 400f),
            finishedListener = { if (isExiting) onDismiss() },
            label = "progress"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            dragOffset = Offset.Zero
                            committed = false
                            isDragging = true
                            isExiting = false

                            gestureEdge = when {
                                startPos.x < edgeZone -> GestureEdge.LEFT
                                startPos.x > screenWidth - edgeZone -> GestureEdge.RIGHT
                                else -> GestureEdge.BOTTOM
                            }

                            do {
                                val event = awaitPointerEvent()
                                val drag = event.changes.firstOrNull()

                                if (drag != null && drag.pressed) {
                                    val delta = drag.position - drag.previousPosition
                                    dragOffset += delta

                                    // Commit logic for edge gestures
                                    if (!committed && gestureEdge != GestureEdge.BOTTOM) {
                                        val hDrag = abs(dragOffset.x)
                                        val vDrag = abs(dragOffset.y)
                                        if (hDrag > 20f) {
                                            if (hDrag > vDrag * 1.5f) {
                                                committed = true
                                                val correctDir = when (gestureEdge) {
                                                    GestureEdge.LEFT -> dragOffset.x > 0
                                                    GestureEdge.RIGHT -> dragOffset.x < 0
                                                    else -> true
                                                }
                                                if (!correctDir) gestureEdge = GestureEdge.NONE
                                            } else if (vDrag > hDrag) {
                                                gestureEdge = GestureEdge.BOTTOM
                                                committed = true
                                            }
                                        }
                                    } else if (!committed && gestureEdge == GestureEdge.BOTTOM) {
                                        if (dragOffset.y > 20f) committed = true
                                    }

                                    // Constrain drag direction
                                    dragOffset = when (gestureEdge) {
                                        GestureEdge.LEFT -> Offset(dragOffset.x.coerceAtLeast(0f), dragOffset.y)
                                        GestureEdge.RIGHT -> Offset(dragOffset.x.coerceAtMost(0f), dragOffset.y)
                                        GestureEdge.BOTTOM -> Offset(dragOffset.x, dragOffset.y.coerceAtLeast(0f))
                                        GestureEdge.NONE -> dragOffset
                                    }

                                    drag.consume()
                                }
                            } while (event.changes.any { it.pressed })

                            isDragging = false

                            if (committed) {
                                val shouldDismiss = when (gestureEdge) {
                                    GestureEdge.LEFT -> dragOffset.x > maxDrag * 0.25f
                                    GestureEdge.RIGHT -> dragOffset.x < -maxDrag * 0.25f
                                    GestureEdge.BOTTOM -> dragOffset.y > maxDrag * 0.25f
                                    GestureEdge.NONE -> false
                                }
                                if (shouldDismiss) {
                                    isExiting = true
                                } else {
                                    dragOffset = Offset.Zero
                                    gestureEdge = GestureEdge.NONE
                                }
                            } else {
                                dragOffset = Offset.Zero
                                gestureEdge = GestureEdge.NONE
                            }
                        }
                    }
                }
        ) {
            BottomSheet(
                progress = progress,
                gestureEdge = gestureEdge,
                isExiting = isExiting
            )
        }
    }
}

@Composable
private fun BoxScope.BottomSheet(
    progress: Float,
    gestureEdge: GestureEdge,
    isExiting: Boolean
) {
    val cornerRadius = lerp(32f, 24f, progress).dp
    val scale = lerp(1f, 0.5f, progress)
    val contentAlpha = lerp(1f, 0f, (progress * 2f).coerceIn(0f, 1f))
    val bgAlpha = lerp(1f, 0f, (progress * 1.5f).coerceIn(0f, 1f))
    val glowAlpha = if (isExiting) {
        lerp(1f, 0f, ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f))
    } else 1f

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(265.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                when (gestureEdge) {
                    GestureEdge.LEFT -> {
                        translationX = lerp(0f, 150f, progress)
                        rotationY = lerp(0f, -12f, progress)
                    }
                    GestureEdge.RIGHT -> {
                        translationX = lerp(0f, -150f, progress)
                        rotationY = lerp(0f, 12f, progress)
                    }
                    else -> {
                        translationY = lerp(0f, 300f, progress)
                    }
                }
                clip = true
                shape = RoundedCornerShape(cornerRadius)
            }
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .alpha(bgAlpha)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E).copy(alpha = 0.92f),
                            Color(0xFF16213E).copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // Top highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .alpha(bgAlpha)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Glow border
        GlowBorder(intensity = glowAlpha, cornerRadius = cornerRadius)

        // Content
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .alpha(contentAlpha)
        ) {
            // Drag handle
            Box(
                Modifier
                    .size(40.dp, 4.dp)
                    .alpha(0.5f)
                    .background(Color.White, CircleShape)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Drako Spatial AI",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Swipe from edges for back gesture, or swipe down to dismiss.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.7f)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Color(0xFF00FF88), CircleShape)
                )
                Text(
                    "Overlay Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.5f)
                )
            }
        }
    }
}

@Composable
private fun GlowBorder(intensity: Float, cornerRadius: Dp) {
    val transition = rememberInfiniteTransition(label = "glow")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(intensity)
            .drawWithCache {
                val cornerPx = cornerRadius.toPx()
                val strokeWidth = 6.dp.toPx()
                val staticStroke = 2.dp.toPx()

                onDrawBehind {
                    val angle = rotation / 360f

                    // Animated glow
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            (angle - 0.08f) to Color.Transparent,
                            (angle - 0.02f) to Color.Cyan.copy(alpha = 0.3f),
                            angle to Color.Cyan,
                            (angle + 0.02f) to Color.Cyan.copy(alpha = 0.3f),
                            (angle + 0.08f) to Color.Transparent,
                            center = center
                        ),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        cornerRadius = CornerRadius(cornerPx)
                    )

                    // Static border
                    drawRoundRect(
                        color = Color.Cyan.copy(alpha = 0.15f),
                        style = Stroke(width = staticStroke),
                        cornerRadius = CornerRadius(cornerPx)
                    )
                }
            }
    )
}
