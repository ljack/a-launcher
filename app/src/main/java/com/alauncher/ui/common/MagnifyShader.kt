package com.alauncher.ui.common

import android.graphics.RenderEffect
import android.graphics.RuntimeShader

/**
 * AGSL shader for the magnifying glass lens.
 *
 * Inside the lens circle: magnifies content (samples from scaled coordinates)
 * At the lens edge: Snell-Descartes refraction for liquid glass look
 * Outside: passthrough
 *
 * The magnification works by displacing sample coordinates toward the lens center,
 * making content appear larger. The edge uses a refraction profile for the glass rim.
 */
private const val MAGNIFY_SHADER = """
    uniform shader inputTexture;
    uniform float2 iResolution;
    uniform float2 iCenter;       // lens center in pixels
    uniform float iRadius;        // lens radius in pixels
    uniform float iMagnification; // e.g. 2.5 = 2.5x zoom
    uniform float iEdgeWidth;     // glass rim width in pixels
    uniform float iRefractiveIndex;
    uniform float iAlpha;         // 0..1 for fade in/out
    uniform float2 iHighlight;    // screen position of highlighted app (0,0 = none)
    uniform float iHighlightRadius; // radius of highlight ring

    // Glass edge profile: smooth raised cosine
    float edgeProfile(float t) {
        return 0.5 * (1.0 + cos(t * 3.14159));
    }

    half4 main(float2 fragCoord) {
        float2 delta = fragCoord - iCenter;
        float dist = length(delta);

        // Outside lens entirely
        if (dist > iRadius + iEdgeWidth) {
            return inputTexture.eval(fragCoord);
        }

        float2 dir = dist > 0.001 ? delta / dist : float2(0.0, 1.0);

        // Inside the clear magnification zone
        if (dist < iRadius - iEdgeWidth) {
            // Magnify: sample from coordinates closer to center
            float2 magnified = iCenter + delta / iMagnification;
            magnified = clamp(magnified, float2(0.0), iResolution);
            half4 color = inputTexture.eval(magnified);

            // Subtle glass tint
            color.rgb += half3(0.02, 0.04, 0.06) * iAlpha;

            // Highlight ring around selected app
            if (iHighlight.x > 0.0 || iHighlight.y > 0.0) {
                // Map the highlight position through magnification
                float2 highlightInLens = iCenter + (iHighlight - iCenter) * iMagnification;
                float highlightDist = length(magnified - iHighlight);
                float ringDist = abs(highlightDist - iHighlightRadius) / iMagnification;
                // Glowing ring
                float ring = exp(-ringDist * ringDist * 0.5) * 0.8;
                color.rgb += half3(ring * 0.5, ring * 0.9, ring); // cyan glow
                // Soft fill
                float fill = smoothstep(iHighlightRadius * 1.2, iHighlightRadius * 0.3, highlightDist);
                color.rgb += half3(fill * 0.03, fill * 0.06, fill * 0.08);
            }

            return color;
        }

        // Edge zone: transition from magnified to normal with refraction
        float edgeDist;
        bool insideLens;
        if (dist < iRadius) {
            // Inner edge (inside lens, approaching rim)
            edgeDist = (iRadius - dist) / iEdgeWidth; // 0 at rim, 1 at inner edge start
            insideLens = true;
        } else {
            // Outer edge (outside lens, glass rim refraction)
            edgeDist = (dist - iRadius) / iEdgeWidth; // 0 at rim, 1 at outer edge end
            insideLens = false;
        }

        float profile = edgeProfile(edgeDist);

        // Refraction displacement at the edge
        float eta = 1.0 / iRefractiveIndex;
        // Surface normal at the edge is radial
        float2 normal = -dir;
        // Displacement increases with glass thickness
        float thickness = profile * iEdgeWidth * 0.5;

        // Compute refracted sample position
        float2 samplePos;
        if (insideLens) {
            // Blend between magnified and edge-refracted
            float2 magnified = iCenter + delta / iMagnification;
            float2 refracted = fragCoord + normal * thickness * (1.0 - eta);
            float blend = smoothstep(0.0, 1.0, edgeDist);
            samplePos = mix(refracted, magnified, blend);
        } else {
            // Outside: refract inward (objects bend toward lens center)
            samplePos = fragCoord + normal * thickness * (1.0 - eta) * 0.5;
        }

        samplePos = clamp(samplePos, float2(0.0), iResolution);
        half4 color = inputTexture.eval(samplePos);

        // Caustic brightening at the rim
        float rimGlow = exp(-edgeDist * 3.0) * 0.2 * iAlpha;
        color.rgb += half3(rimGlow * 0.4, rimGlow * 0.7, rimGlow);

        // Specular rim highlight (bright line at the glass edge)
        float specular = exp(-edgeDist * 8.0) * 0.35 * iAlpha;
        color.rgb += half3(specular);

        return color;
    }
"""

/**
 * Creates a magnifying glass RenderEffect.
 */
fun createMagnifyEffect(
    width: Float,
    height: Float,
    centerX: Float,
    centerY: Float,
    radius: Float = 250f,
    magnification: Float = 2.5f,
    edgeWidth: Float = 30f,
    refractiveIndex: Float = 1.5f,
    alpha: Float = 1f,
    highlightX: Float = 0f,
    highlightY: Float = 0f,
    highlightRadius: Float = 35f,
): RenderEffect? {
    return try {
        val shader = RuntimeShader(MAGNIFY_SHADER)
        shader.setFloatUniform("iResolution", width, height)
        shader.setFloatUniform("iCenter", centerX, centerY)
        shader.setFloatUniform("iRadius", radius)
        shader.setFloatUniform("iMagnification", magnification)
        shader.setFloatUniform("iEdgeWidth", edgeWidth)
        shader.setFloatUniform("iRefractiveIndex", refractiveIndex)
        shader.setFloatUniform("iAlpha", alpha)
        shader.setFloatUniform("iHighlight", highlightX, highlightY)
        shader.setFloatUniform("iHighlightRadius", highlightRadius)
        RenderEffect.createRuntimeShaderEffect(shader, "inputTexture")
    } catch (e: Exception) {
        null
    }
}
