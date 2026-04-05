package com.alauncher.ui.common

import android.graphics.RuntimeShader
import android.graphics.RenderEffect

/**
 * AGSL shader implementing Snell-Descartes refraction with glass edge geometry.
 *
 * The glass surface has physical thickness (edgeHeight) that creates a lens
 * effect at the wave boundary. Like looking through the curved rim of a
 * glass of water — the thicker the glass, the more light bends.
 *
 * The edge acts as a cylindrical lens:
 * - At the wave boundary: maximum thickness → maximum distortion
 * - Away from boundary: thickness tapers to 0 → no distortion (just tint)
 * - The lens profile follows a smooth curve (raised cosine)
 *
 * Snell's law: n1 * sin(θ1) = n2 * sin(θ2)
 * Ray displacement = refracted_direction × glass_thickness_at_point
 */
private const val REFRACTION_SHADER = """
    uniform shader inputTexture;
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iRefractiveIndex;   // n2 (glass/water IOR), n1=1.0 (air)
    uniform float iWaveAmplitude;     // wave height in pixels
    uniform float iWaveFrequency;     // wave cycles across screen
    uniform float iEdgeHeight;        // glass thickness at wave edge (pixels)
    uniform float iEdgeWidth;         // width of the lens zone (pixels)
    uniform float iZoneTopEnd;        // y where top glass zone ends
    uniform float iZoneBotStart;      // y where bottom glass zone starts

    // Wave surface height at x position
    float waveHeight(float x, float phase) {
        float w1 = sin(x * iWaveFrequency * 6.2832 / iResolution.x + phase);
        float w2 = sin(x * iWaveFrequency * 2.0 * 6.2832 / iResolution.x + phase * 1.3) * 0.3;
        return iWaveAmplitude * (w1 + w2);
    }

    // Surface normal from wave derivative
    float2 waveNormal(float x, float phase) {
        float eps = 1.0;
        float dh = waveHeight(x + eps, phase) - waveHeight(x - eps, phase);
        return normalize(float2(-dh / (2.0 * eps), 1.0));
    }

    // Glass thickness profile: raised cosine centered on the wave edge
    // Returns 0..1 where 1 = maximum thickness at the edge
    float glassThickness(float distFromEdge) {
        float t = clamp(distFromEdge / iEdgeWidth, 0.0, 1.0);
        // Raised cosine: smooth bell curve centered at edge
        return 0.5 * (1.0 + cos(t * 3.14159));
    }

    // Snell refraction: returns displacement vector
    float2 snellRefract(float2 incident, float2 normal, float eta) {
        float cosI = dot(-incident, normal);
        float sinT2 = eta * eta * (1.0 - cosI * cosI);
        if (sinT2 > 1.0) return float2(0.0); // total internal reflection
        float cosT = sqrt(1.0 - sinT2);
        return eta * incident + (eta * cosI - cosT) * normal;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord;
        float phase = iTime * 1.5;

        // Compute wave edge positions
        float topWaveY = iZoneTopEnd + waveHeight(uv.x, phase);
        float botWaveY = iZoneBotStart + waveHeight(uv.x, phase);

        // Distance from nearest wave edge
        float distTop = abs(uv.y - topWaveY);
        float distBot = abs(uv.y - botWaveY);

        bool nearTopEdge = distTop < iEdgeWidth && uv.y < topWaveY + iEdgeWidth;
        bool nearBotEdge = distBot < iEdgeWidth && uv.y > botWaveY - iEdgeWidth;
        bool inTopZone = uv.y <= topWaveY;
        bool inBotZone = uv.y >= botWaveY;

        if (nearTopEdge || nearBotEdge) {
            // We're in the lens zone — glass has thickness here
            float dist = nearTopEdge ? distTop : distBot;
            float thickness = glassThickness(dist) * iEdgeHeight;

            // Surface normal from the wave
            float2 normal = waveNormal(uv.x, phase);
            if (nearBotEdge) normal.y = -normal.y;

            // Add vertical curvature from the glass edge profile
            // The glass curves like a lens, creating vertical refraction too
            float profileSlope;
            if (nearTopEdge) {
                profileSlope = (uv.y < topWaveY) ? -1.0 : 1.0;
            } else {
                profileSlope = (uv.y < botWaveY) ? -1.0 : 1.0;
            }
            float curveAmount = glassThickness(dist + 1.0) - glassThickness(dist - 1.0);
            float2 lensNormal = normalize(normal + float2(0.0, curveAmount * profileSlope * 3.0));

            // Incident ray (viewing direction — straight into screen)
            float2 incident = float2(0.0, 1.0);

            // Apply Snell's law at entry surface
            float eta = 1.0 / iRefractiveIndex;
            float2 refracted = snellRefract(incident, lensNormal, eta);

            // Ray displacement through the glass thickness
            float2 offset = refracted * thickness;

            // Sample displaced texture
            float2 displaced = uv + offset;
            displaced = clamp(displaced, float2(0.0), iResolution);
            half4 color = inputTexture.eval(displaced);

            // Caustic brightening at the edge (light concentration)
            float causticIntensity = glassThickness(dist) * 0.25;
            // Chromatic aberration: slightly different offset per channel
            float2 redOffset = offset * 1.02;
            float2 blueOffset = offset * 0.98;
            float2 uvR = clamp(uv + redOffset, float2(0.0), iResolution);
            float2 uvB = clamp(uv + blueOffset, float2(0.0), iResolution);
            color.r = inputTexture.eval(uvR).r;
            color.b = inputTexture.eval(uvB).b;

            // Caustic highlight
            color.rgb += half3(causticIntensity * 0.3, causticIntensity * 0.6, causticIntensity * 0.8);

            // Fresnel-like edge brightening
            float fresnel = pow(1.0 - abs(dot(incident, lensNormal)), 3.0) * 0.15;
            color.rgb += half3(fresnel * 0.4, fresnel * 0.7, fresnel);

            return color;
        }

        // Outside lens zone: pass through unchanged
        return inputTexture.eval(uv);
    }
"""

/**
 * Creates a refraction RenderEffect using Snell-Descartes law with glass edge geometry.
 *
 * @param edgeHeight Thickness of the glass at the wave edge in pixels. Higher = more distortion.
 * @param edgeWidth Width of the lens zone in pixels. The distortion tapers off over this distance.
 */
fun createRefractionEffect(
    width: Float,
    height: Float,
    time: Float,
    topZoneEnd: Float,
    bottomZoneStart: Float,
    refractiveIndex: Float = 1.33f,
    waveAmplitude: Float = 12f,
    waveFrequency: Float = 3f,
    edgeHeight: Float = 40f,
    edgeWidth: Float = 120f,
): RenderEffect? {
    return try {
        val shader = RuntimeShader(REFRACTION_SHADER)
        shader.setFloatUniform("iResolution", width, height)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("iRefractiveIndex", refractiveIndex)
        shader.setFloatUniform("iWaveAmplitude", waveAmplitude)
        shader.setFloatUniform("iWaveFrequency", waveFrequency)
        shader.setFloatUniform("iEdgeHeight", edgeHeight)
        shader.setFloatUniform("iEdgeWidth", edgeWidth)
        shader.setFloatUniform("iZoneTopEnd", topZoneEnd)
        shader.setFloatUniform("iZoneBotStart", bottomZoneStart)

        RenderEffect.createRuntimeShaderEffect(shader, "inputTexture")
    } catch (e: Exception) {
        null
    }
}
