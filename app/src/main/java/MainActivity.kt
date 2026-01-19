package ai.drako

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Drako Spatial AI - Launcher Activity
 *
 * This activity handles overlay permission and launches the DrakoOverlayService.
 * Shows a permission request screen explaining what permission is needed.
 */
class MainActivity : ComponentActivity() {

    private var permissionState by mutableStateOf(PermissionState.CHECKING)

    private enum class PermissionState {
        CHECKING,
        NEEDS_PERMISSION,
        PERMISSION_DENIED,
        GRANTED
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            permissionState = PermissionState.GRANTED
            launchOverlayAndGoHome()
        } else {
            permissionState = PermissionState.PERMISSION_DENIED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PermissionScreen(
                state = permissionState,
                onGrantPermission = { requestOverlayPermission() },
                onRetry = { requestOverlayPermission() },
                onExit = { finish() }
            )
        }

        // Check permission status on launch
        if (Settings.canDrawOverlays(this)) {
            permissionState = PermissionState.GRANTED
            launchOverlayAndGoHome()
        } else {
            permissionState = PermissionState.NEEDS_PERMISSION
        }
    }

    private fun requestOverlayPermission() {
        permissionState = PermissionState.CHECKING
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun launchOverlayAndGoHome() {
        // Start the overlay service
        startService(Intent(this, DrakoOverlayService::class.java))

        // Navigate to home screen so user sees overlay over their apps
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

// Dark color scheme matching the overlay style
private val DrakoDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FFFF),
    onPrimary = Color(0xFF003333),
    surface = Color(0xFF1A1A2E),
    onSurface = Color.White,
    background = Color(0xFF16213E),
    onBackground = Color.White
)

@Composable
private fun PermissionScreen(
    state: MainActivity.PermissionState,
    onGrantPermission: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    MaterialTheme(colorScheme = DrakoDarkColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E),
                                Color(0xFF16213E)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated glow orb
                    GlowOrb()

                    Spacer(Modifier.height(48.dp))

                    Text(
                        text = "Drako Spatial AI",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )

                    Spacer(Modifier.height(16.dp))

                    when (state) {
                        MainActivity.PermissionState.CHECKING -> {
                            Text(
                                text = "Checking permissions...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }

                        MainActivity.PermissionState.NEEDS_PERMISSION -> {
                            Text(
                                text = "Drako needs permission to display over other apps to show the AI overlay.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(32.dp))

                            Button(
                                onClick = onGrantPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FFFF),
                                    contentColor = Color(0xFF003333)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    text = "Grant Permission",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "You'll be taken to system settings.\nFind Drako and enable the permission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }

                        MainActivity.PermissionState.PERMISSION_DENIED -> {
                            Text(
                                text = "Permission not granted.\nDrako needs overlay permission to function.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFFF6B6B),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(32.dp))

                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FFFF),
                                    contentColor = Color(0xFF003333)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    text = "Try Again",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            TextButton(onClick = onExit) {
                                Text(
                                    text = "Exit",
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        MainActivity.PermissionState.GRANTED -> {
                            Text(
                                text = "Permission granted! Starting overlay...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF00FF88),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlowOrb() {
    val transition = rememberInfiniteTransition(label = "glow")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .alpha(pulse)
            .drawWithCache {
                val strokeWidth = 4.dp.toPx()
                onDrawBehind {
                    val angle = rotation / 360f

                    // Animated glow ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            (angle - 0.15f) to Color.Transparent,
                            (angle - 0.05f) to Color.Cyan.copy(alpha = 0.4f),
                            angle to Color.Cyan,
                            (angle + 0.05f) to Color.Cyan.copy(alpha = 0.4f),
                            (angle + 0.15f) to Color.Transparent
                        ),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Static inner ring
                    drawCircle(
                        color = Color.Cyan.copy(alpha = 0.2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
    ) {
        // Inner filled circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00FFFF).copy(alpha = 0.3f),
                            Color(0xFF00FFFF).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
