package ai.drako.system

/**
 * Feature tiers for adaptive overlay rendering.
 *
 * The overlay gracefully degrades through these tiers based on device
 * resource availability (thermal state, memory, frame rate).
 *
 * Tier transitions are smooth - effects fade out/in rather than
 * switching abruptly.
 */
enum class FeatureTier(val level: Int) {
    /**
     * Full effects: Real blur, animated glow, spring physics, 60fps
     * Requirements: Cool device, plenty of RAM, flagship GPU
     */
    FULL(4),

    /**
     * High effects: Animated glow, spring physics, no blur
     * Requirements: Warm device OR mid-range GPU
     */
    HIGH(3),

    /**
     * Medium effects: Static glow border, spring physics
     * Requirements: Hot device OR low RAM
     */
    MEDIUM(2),

    /**
     * Light effects: Static border, simple easing (no springs)
     * Requirements: Very hot device OR critically low RAM
     */
    LIGHT(1),

    /**
     * Minimal: No effects, instant transitions
     * Requirements: Thermal emergency OR memory critical
     */
    MINIMAL(0);

    val hasBlur: Boolean get() = this == FULL
    val hasAnimatedGlow: Boolean get() = level >= HIGH.level
    val hasStaticGlow: Boolean get() = level >= MEDIUM.level
    val hasSpringPhysics: Boolean get() = level >= MEDIUM.level
    val hasAnyEffects: Boolean get() = level >= LIGHT.level

    /**
     * Get the next tier down (for degradation)
     */
    fun degrade(): FeatureTier = when (this) {
        FULL -> HIGH
        HIGH -> MEDIUM
        MEDIUM -> LIGHT
        LIGHT -> MINIMAL
        MINIMAL -> MINIMAL
    }

    /**
     * Get the next tier up (for recovery)
     */
    fun upgrade(): FeatureTier = when (this) {
        MINIMAL -> LIGHT
        LIGHT -> MEDIUM
        MEDIUM -> HIGH
        HIGH -> FULL
        FULL -> FULL
    }

    companion object {
        fun fromLevel(level: Int): FeatureTier = entries.find { it.level == level } ?: MINIMAL
    }
}
