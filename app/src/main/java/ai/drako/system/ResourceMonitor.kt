package ai.drako.system

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors device resources (thermal state, memory) and recommends
 * appropriate feature tiers for the overlay.
 *
 * Resource changes trigger smooth tier transitions rather than
 * abrupt feature switches.
 *
 * Usage:
 * ```
 * val monitor = ResourceMonitor(context)
 * monitor.start()
 *
 * // Observe tier changes
 * monitor.recommendedTier.collect { tier ->
 *     updateOverlayEffects(tier)
 * }
 *
 * // Cleanup
 * monitor.stop()
 * ```
 */
class ResourceMonitor(private val context: Context) : ComponentCallbacks2 {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val _thermalStatus = MutableStateFlow(ThermalState.NORMAL)
    val thermalStatus: StateFlow<ThermalState> = _thermalStatus.asStateFlow()

    private val _memoryState = MutableStateFlow(MemoryState.NORMAL)
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    private val _recommendedTier = MutableStateFlow(FeatureTier.FULL)
    val recommendedTier: StateFlow<FeatureTier> = _recommendedTier.asStateFlow()

    // Hysteresis: require sustained state before upgrading (prevents flapping)
    private var stableStateCount = 0
    private var lastRecommendation = FeatureTier.FULL
    private val upgradeThreshold = 5  // ~5 seconds of stable state before upgrading

    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    /**
     * Start monitoring device resources.
     */
    fun start() {
        registerThermalListener()
        registerMemoryCallbacks()
        startPolling()
    }

    /**
     * Stop monitoring and release resources.
     */
    fun stop() {
        unregisterThermalListener()
        scope.cancel()
    }

    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                _thermalStatus.value = ThermalState.fromAndroidStatus(status)
                recalculateTier()
            }
            powerManager.addThermalStatusListener(thermalListener!!)
        }
    }

    private fun unregisterThermalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
            powerManager.removeThermalStatusListener(thermalListener!!)
        }
    }

    private fun registerMemoryCallbacks() {
        // ComponentCallbacks2 is registered via the context
        // We check memory state in polling loop instead
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                checkMemoryState()
                recalculateTier()
                delay(1000) // Poll every second
            }
        }
    }

    private fun checkMemoryState() {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        _memoryState.value = when {
            memoryInfo.lowMemory -> MemoryState.CRITICAL
            memoryInfo.availMem < memoryInfo.threshold * 1.5 -> MemoryState.LOW
            memoryInfo.availMem < memoryInfo.threshold * 2.5 -> MemoryState.MODERATE
            else -> MemoryState.NORMAL
        }
    }

    private fun recalculateTier() {
        val thermal = _thermalStatus.value
        val memory = _memoryState.value

        // Calculate ideal tier based on current state
        val idealTier = calculateIdealTier(thermal, memory)

        // Immediate downgrade (safety), delayed upgrade (stability)
        val newTier = when {
            idealTier.level < lastRecommendation.level -> {
                // Downgrade immediately
                stableStateCount = 0
                idealTier
            }
            idealTier.level > lastRecommendation.level -> {
                // Upgrade only after sustained stability
                stableStateCount++
                if (stableStateCount >= upgradeThreshold) {
                    stableStateCount = 0
                    lastRecommendation.upgrade()
                } else {
                    lastRecommendation
                }
            }
            else -> {
                stableStateCount = 0
                idealTier
            }
        }

        lastRecommendation = newTier
        _recommendedTier.value = newTier
    }

    private fun calculateIdealTier(thermal: ThermalState, memory: MemoryState): FeatureTier {
        // Thermal takes priority over memory
        return when {
            thermal == ThermalState.SHUTDOWN || memory == MemoryState.CRITICAL -> FeatureTier.MINIMAL
            thermal == ThermalState.CRITICAL -> FeatureTier.MINIMAL
            thermal == ThermalState.SEVERE || memory == MemoryState.LOW -> FeatureTier.LIGHT
            thermal == ThermalState.SERIOUS -> FeatureTier.MEDIUM
            thermal == ThermalState.MODERATE || memory == MemoryState.MODERATE -> FeatureTier.HIGH
            else -> FeatureTier.FULL
        }
    }

    // ComponentCallbacks2 implementation
    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                _memoryState.value = MemoryState.CRITICAL
                recalculateTier()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                _memoryState.value = MemoryState.LOW
                recalculateTier()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                _memoryState.value = MemoryState.MODERATE
                recalculateTier()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
    override fun onLowMemory() {
        _memoryState.value = MemoryState.CRITICAL
        recalculateTier()
    }
}

/**
 * Thermal states mapped from PowerManager.THERMAL_STATUS_*
 */
enum class ThermalState {
    NORMAL,      // THERMAL_STATUS_NONE or LIGHT
    MODERATE,    // THERMAL_STATUS_MODERATE
    SERIOUS,     // THERMAL_STATUS_SEVERE (confusingly named)
    SEVERE,      // THERMAL_STATUS_CRITICAL
    CRITICAL,    // THERMAL_STATUS_EMERGENCY
    SHUTDOWN;    // THERMAL_STATUS_SHUTDOWN

    companion object {
        fun fromAndroidStatus(status: Int): ThermalState = when (status) {
            PowerManager.THERMAL_STATUS_NONE,
            PowerManager.THERMAL_STATUS_LIGHT -> NORMAL
            PowerManager.THERMAL_STATUS_MODERATE -> MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> SERIOUS
            PowerManager.THERMAL_STATUS_CRITICAL -> SEVERE
            PowerManager.THERMAL_STATUS_EMERGENCY -> CRITICAL
            PowerManager.THERMAL_STATUS_SHUTDOWN -> SHUTDOWN
            else -> NORMAL
        }
    }
}

/**
 * Memory pressure states
 */
enum class MemoryState {
    NORMAL,     // Plenty of free memory
    MODERATE,   // Approaching threshold
    LOW,        // Near threshold
    CRITICAL    // Below threshold (lowMemory = true)
}
