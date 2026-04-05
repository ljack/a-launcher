# F9: Magnifying Glass (Visual Spatial Search)

A liquid glass lens that magnifies app icons under it, allowing purely visual
browsing of the spiral at any zoom level. Complements F4 (text search).

## Interaction Design

### Activation
- **Long-press anywhere** (>300ms) activates the magnifying glass
- Quick touch + fast drag = normal pan (unchanged)
- The distinction: velocity of initial movement. Fast = pan, still = magnify.

### Movement
- After activation, drag to move the lens across the spiral
- Lens appears with a small **vertical offset above the finger** so content is visible
- **Lift finger**: lens STAYS on screen at last position (persistent state)
- **Tap an app inside the lens**: launches it
- **Touch lens again + drag**: continues moving it
- **Touch outside lens**: dismisses it
- **Pinch zoom while lens active**: adjusts magnification level (zoom in = more magnification, zoom out = less, dismiss at minimum)

### Magnification
- Default: ~2.5x magnification inside the lens
- Pinch-to-zoom adjusts: 1.5x → 4x range
- Icons inside the lens get their **labels shown** (even if normally hidden at current zoom level)
- Smooth transition at lens edge (not a hard cut)

### Visual Style
- Circular liquid glass lens
- Snell-Descartes refraction at the edge (reuse our shader!)
- Subtle glass tint inside (like our aqua zones)
- Specular rim highlight
- Soft shadow underneath
- Size: ~180dp diameter (configurable)

### Exit
- Touch outside the lens
- Pinch-zoom out below minimum magnification
- Back gesture
- Double-tap outside

## Implementation Plan

### Phase 1: Basic magnification
- Long-press detection in gesture handler
- AGSL shader: circular region with scale transform (magnify center, refract edges)
- Persistent lens state (position, magnification, active/inactive)
- Tap-through to app launch

### Phase 2: Polish
- Specular rim animation
- Smooth activation/deactivation animation (scale up from 0)
- Label rendering inside lens zone
- Edge profile experimentation (circular vs spherical)

## Configurable Properties
| Property | Type | Default | Description |
|---|---|---|---|
| `magnifyEnabled` | Boolean | `true` | Enable/disable magnifying glass |
| `magnifySize` | Float (dp) | `180` | Lens diameter |
| `magnifyScale` | Float | `2.5` | Default magnification level |
| `magnifyScaleMin` | Float | `1.5` | Minimum magnification |
| `magnifyScaleMax` | Float | `4.0` | Maximum magnification |
| `magnifyOffsetY` | Float (dp) | `-60` | Vertical offset above finger |
| `magnifyActivationMs` | Long | `300` | Long-press threshold |
