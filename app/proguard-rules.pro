# 1. Protect AGSL and RenderEffect Pipeline
# These are often accessed via reflection or native calls in the graphics layer.
-keep class android.graphics.RuntimeShader { *; }
-keep class android.graphics.RenderEffect { *; }
-keep class android.graphics.RenderNode { *; }

# 2. Preserve Haptic Primitives (Android 15/16)
# Prevents R8 from stripping the enum-like constants for PRIMITIVE_TICK/LOW_TICK.
-keep class android.os.VibrationEffect$Composition { *; }
-keep class android.os.VibratorManager { *; }

# 3. Predictive Back & Velocity APIs (Android 16)
# Ensure the new BackEventCompat and velocity accessors are not renamed.
-keep class androidx.activity.BackEventCompat { *; }
-keepnames class androidx.activity.BackEventCompat {
    public float getTouchX();
    public float getVelocity();
}

# 4. Jetpack Compose Shared Transitions
# Protect the internal keys used for shared element matching.
-keep class androidx.compose.animation.SharedTransitionScope** { *; }
-keep class androidx.compose.animation.AnimatedVisibilityScope** { *; }

# 5. Optimization: Prevent merging of Graphics Classes
# Merging these can cause subtle issues in the GPU command buffer.
-nomerge class android.graphics.RuntimeShader
-nomerge class android.graphics.RenderEffect

# 6. Metadata for Kotlin 2.3.0 Reflective access
# If you use reflection for AI state or dynamic shader uniforms.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,EnclosingMethod