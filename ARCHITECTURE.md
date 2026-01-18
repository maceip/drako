# Drako Overlay Architecture

## Synthesized Goals (from current implementation)

The current implementation aims to create a **floating AI assistant interface** that:

### Core Purpose
1. **System-wide presence** - Overlay that floats above all apps via `TYPE_APPLICATION_OVERLAY`
2. **Non-intrusive** - Bottom sheet form factor, doesn't block main content
3. **Instant access** - Always available, quick to invoke and dismiss

### UX Goals
1. **Predictive back integration** - Feels native to Android's gesture navigation
2. **Fluid dismiss gestures** - Swipe down or edge swipe to dismiss
3. **Visual polish** - Animated glow border, frosted glass aesthetic
4. **Responsive feedback** - Sheet shrinks/transforms as user drags

### Animation Goals
1. **Layered fade-out** - Content fades first, glow border persists longest
2. **Direction-aware transforms** - Different animations for left/right/down dismiss
3. **Spring physics** - Natural, bouncy feel during gestures

### Technical Goals
1. **Service-based** - Runs independently of any activity
2. **Lifecycle-aware** - Proper Compose integration in service context
3. **Crash-resistant** - Handle all edge cases gracefully

---

## Implementation Variants

### Variant 1: High-End (flagship phones)
**Target:** Phones with large GPU, 8GB+ RAM, Android 12+

Features:
- Real `RenderEffect` blur (frosted glass)
- Native `PredictiveBackHandler` integration
- Shared element transitions
- High-fidelity glow animation (60fps)
- Rich spring physics

### Variant 2: Adaptive (mid-tier phones)
**Target:** Phones with varying capabilities, Android 10+

Features:
- Graceful feature degradation based on:
  - Thermal status (PowerManager API)
  - Available memory (ActivityManager.MemoryInfo)
  - Frame rate monitoring
- Feature tiers:
  - **Full:** Blur + glow animation + spring physics
  - **Medium:** No blur + glow animation + spring physics
  - **Light:** No blur + static border + simple easing
  - **Minimal:** No effects, instant transitions
- Smooth transitions between tiers (no jarring switches)
- Automatic recovery when resources return

---

## File Structure

```
app/src/main/java/ai/drako/
├── DrakoOverlayService.kt      # Service infrastructure (shared)
├── overlay/
│   ├── HighEndOverlay.kt       # Variant 1: Full effects
│   ├── AdaptiveOverlay.kt      # Variant 2: Graceful degradation
│   └── OverlayContent.kt       # Shared content composables
├── effects/
│   ├── GlowBorder.kt           # Animated glow border
│   ├── FrostedGlass.kt         # Blur/glass effects
│   └── AdaptiveEffects.kt      # Degradable effect wrappers
├── gestures/
│   ├── PredictiveBack.kt       # Native predictive back
│   └── EdgeGestures.kt         # Manual edge detection
└── system/
    ├── ResourceMonitor.kt      # Thermal + memory monitoring
    └── FeatureTier.kt          # Feature level management
```
