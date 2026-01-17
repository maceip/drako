package ai.drako

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Drako Spatial AI - Launcher Activity
 *
 * This activity handles overlay permission and launches the DrakoOverlayService.
 * Once the overlay is started, it navigates to the home screen so the user
 * can see the floating bottom sheet over their normal apps.
 */
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            launchOverlayAndGoHome()
        } else {
            // User denied permission, just finish
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.canDrawOverlays(this)) {
            launchOverlayAndGoHome()
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
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
