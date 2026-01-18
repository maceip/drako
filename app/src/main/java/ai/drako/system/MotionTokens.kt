package ai.drako.system

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * M3 Expressive Motion Token System for Drako.
 *
 * Based on Material Design 3 Expressive motion guidelines:
 * - Spatial animations (position, size, shape) can overshoot with low damping
 * - Effect animations (alpha, color) should NOT overshoot - use critical damping
 *
 * This ensures bouncy animations for movement while avoiding unnatural
 * effects like opacity going above 1.0 or negative.
 */
object DrakoMotion {

    /**
     * Spatial tokens for position, size, shape animations.
     *
     * These CAN overshoot (bounce) since spatial properties
     * naturally support values outside their target range.
     */
    object Spatial {
        /**
         * Default spatial animation - balanced bounce and speed.
         * Use for most position/scale animations.
         */
        val default: AnimationSpec<Float> = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        )

        /**
         * Fast spatial animation - quick with subtle bounce.
         * Use for responsive interactions.
         */
        val fast: AnimationSpec<Float> = spring(
            dampingRatio = 0.7f,
            stiffness = 500f
        )

        /**
         * Slow spatial animation - more pronounced bounce.
         * Use for hero moments and emphasis.
         */
        val slow: AnimationSpec<Float> = spring(
            dampingRatio = 0.5f,
            stiffness = 150f
        )

        /**
         * Snappy spatial animation - very responsive.
         * Use for quick feedback during active gestures.
         */
        val snappy: AnimationSpec<Float> = spring(
            dampingRatio = 0.8f,
            stiffness = 800f
        )

        /**
         * Bouncy spatial animation - playful feel.
         * Use sparingly for delightful moments.
         */
        val bouncy: AnimationSpec<Float> = spring(
            dampingRatio = 0.4f,
            stiffness = 200f
        )
    }

    /**
     * Effect tokens for alpha, color, blur animations.
     *
     * These MUST NOT overshoot - alpha should stay in [0,1],
     * colors shouldn't go negative. Use critical damping (1.0).
     */
    object Effects {
        /**
         * Default effect animation - smooth without bounce.
         * Use for most alpha/color animations.
         */
        val default: AnimationSpec<Float> = spring(
            dampingRatio = 1f,
            stiffness = 800f
        )

        /**
         * Fast effect animation - quick fade.
         * Use for responsive opacity changes.
         */
        val fast: AnimationSpec<Float> = spring(
            dampingRatio = 1f,
            stiffness = 1200f
        )

        /**
         * Slow effect animation - gentle fade.
         * Use for subtle transitions.
         */
        val slow: AnimationSpec<Float> = spring(
            dampingRatio = 1f,
            stiffness = 400f
        )

        /**
         * Very slow effect animation - dramatic reveal.
         * Use for emphasis on opacity changes.
         */
        val verySlow: AnimationSpec<Float> = spring(
            dampingRatio = 1f,
            stiffness = 200f
        )
    }

    /**
     * Spring specifications for bounds/layout animations.
     *
     * Use these with animateBounds() or similar layout animation APIs.
     */
    object Bounds {
        /**
         * Default bounds spring - smooth layout transitions.
         */
        val default: AnimationSpec<Float> = spring(
            stiffness = 200f,
            dampingRatio = Spring.DampingRatioMediumBouncy
        )

        /**
         * Fast bounds spring - responsive layout changes.
         */
        val fast: AnimationSpec<Float> = spring(
            stiffness = 400f,
            dampingRatio = Spring.DampingRatioMediumBouncy
        )

        /**
         * Slow bounds spring - dramatic layout transitions.
         */
        val slow: AnimationSpec<Float> = spring(
            stiffness = 100f,
            dampingRatio = Spring.DampingRatioLowBouncy
        )
    }

    /**
     * Generic spring specs for different types.
     */
    object Generic {
        /**
         * Create a spatial spring for any type.
         */
        fun <T> spatialSpring(
            dampingRatio: Float = 0.6f,
            stiffness: Float = 300f,
            visibilityThreshold: T? = null
        ): AnimationSpec<T> = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
            visibilityThreshold = visibilityThreshold
        )

        /**
         * Create an effect spring for any type (no overshoot).
         */
        fun <T> effectSpring(
            stiffness: Float = 800f,
            visibilityThreshold: T? = null
        ): AnimationSpec<T> = spring(
            dampingRatio = 1f,  // Critical damping - no overshoot
            stiffness = stiffness,
            visibilityThreshold = visibilityThreshold
        )
    }
}
