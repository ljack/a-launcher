# Configurable Properties

Properties that should be exposed via DataStore preferences and eventually a settings UI.

| Property | Type | Default | Location | Description |
|---|---|---|---|---|
| `showWaveEdge` | Boolean | `false` | HomeScreen → AquaZone | Show/hide animated wave edge line on liquid glass zones |
| `waveAmplitude` | Float (dp) | `12` | AquaZone | Height of wave ripple when visible |
| `glassAlpha` | Float | `0.7` | HomeScreen → AquaZone | Opacity of the liquid glass tint |
| `refractiveIndex` | Float | `1.33` | HomeScreen → RefractionEffect | Snell's law n2 (1.0=air, 1.33=water, 1.5=glass) |
| `edgeHeight` | Float (px) | `45` | HomeScreen → RefractionEffect | Glass thickness at wave boundary |
| `edgeWidth` | Float (px) | `140` | HomeScreen → RefractionEffect | Width of the lens distortion zone |
| `waveFrequency` | Float | `3` | RefractionEffect | Number of wave cycles across screen |
| `orbSpreadFactor` | Float | `0.85` | SpatialField | Spacing multiplier between app orbs |
| `orbSizePx` | Float | `220` | SpatialField | Base size of app orbs in pixels |
| `breatheEnabled` | Boolean | `true` | SpatialField | Enable/disable orb breathing animation |
| `connectionLinesEnabled` | Boolean | `true` | SpatialField | Show/hide connection lines between nearby apps |
| `glowEnabled` | Boolean | `true` | AppOrb | Enable/disable orb glow effect |
