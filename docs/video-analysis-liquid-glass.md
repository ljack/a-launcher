# Video Analysis: "Liquid Glass in the Browser" by Chris Fjoel

**Source:** https://www.youtube.com/watch?v=p1ORlG2dCK8
**Speaker:** Chris Fjoel
**Topic:** Recreating Apple's liquid glass refraction with CSS/SVG

## Key Insights

### 1. History of Depth in UI
- Windows 95: 4 colors for diffuse reflection (light from top-left)
- Windows XP: Still diffuse reflection with images, not real-time
- macOS Aqua (OS X): Light from TOP, specular highlight, caustic shadows (translucent look)
- Windows Aero (2006): First real-time diffuse refraction (Gaussian blur = frosted glass)
- Flat design era: Zune → iOS 7, stripped all depth
- 2025 Apple Liquid Glass: Real-time refraction with **volume** + **dynamic specular rim**

### 2. What Makes Liquid Glass Different
- **Real-time rendered** (not images)
- **Refraction on the sides** suggests volume/3D depth of the object
- **Specular rim**: NEW — dynamic, symmetric, adapts to movement direction
- Content is king — you see what's behind the glass element

### 3. Snell-Descartes Implementation
- `n1 * sin(θ1) = n2 * sin(θ2)` — we already implement this ✅
- n1=1 (air), n2=1.5 (glass) → ray bends toward normal
- n1=n2 → no refraction (pointless)
- n2 < n1 → can cause total internal reflection (avoid)
- **Orthogonal rays don't refract** — only edge rays create visible distortion

### 4. Edge Profile Is Everything
- A flat surface DOES NOT refract (rays are perpendicular to surface)
- You need an **edge profile** with:
  - **Edge width**: how wide the curved edge zone is
  - **Edge height**: the thickness/depth of the glass at the edge
  - **Profile function**: maps position (0..1) along edge width to height (0..1)
- Profile functions he demonstrated:
  - **Circular** (quarter circle) — sharp edge, strong refraction
  - **Spherical** — smoother, more natural
  - **Concave** — pushes light outward (avoid for now, needs sampling outside object)
  - **Custom** — any continuous function works
- We use raised cosine — should try spherical/circular too

### 5. Displacement Map Technique
- Sample rays across the edge profile
- Compute where each ray ACTUALLY hits vs where it WOULD have hit without refraction
- The difference = displacement vector
- Normalize all vectors by max displacement → values in [-1, 1]
- Apply symmetry — only compute one side, mirror it
- Creates a **vector field** around the entire object
- Convert vectors to pixel colors (R=X, G=Y, 128=zero, 0=-1, 255=+1)
- Pass to displacement map filter with scale = max displacement

### 6. Specular Rim (TODO for us)
- Apple's liquid glass has a **dynamic specular rim**
- Light vector can move (not fixed from top)
- Illuminates both sides symmetrically
- Reacts to user movement by rotating the light vector
- Creates the "living glass" feel — glass seems to catch light as you interact

### 7. Demos He Built
- **Bouncing bubble** — draggable glass circle that refracts content behind it
- **Switch component** — custom equation that shrinks middle, refracts sides inward
- **Slider** — glass thumb lets you see the value behind your finger (precision!)
- **Magnifying glass** — displacement map magnifies content under the lens ← EXACTLY our feature
- **Search box + music player** — full UI components with refraction

### 8. Library: "Refractive"
- Partnering with HashiCorp/Ash
- npm package: `@ash-intel/refractive`
- Focus on refraction effects (not just liquid glass)
- Simple API: wrap any element to make it refractive
- Blog: cube.io/blog

## What We Should Implement Next
1. ✅ Snell-Descartes refraction shader — DONE
2. ✅ Edge profile (width + height) — DONE
3. 🆕 **Magnifying glass lens** — draggable glass that magnifies app icons
4. 🆕 **Dynamic specular rim** — light direction follows user interaction
5. 🆕 **Try different edge profiles** — circular, spherical vs our raised cosine
6. 🆕 **Per-component glass** — apply glass effect to individual UI elements (buttons, cards)
7. 🆕 **Concave lens** — could be used for "push away" effects on non-focused areas
