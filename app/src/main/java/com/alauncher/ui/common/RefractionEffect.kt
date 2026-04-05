package com.alauncher.ui.common

import android.graphics.RuntimeShader
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.GraphicsLayerScope

/**
 * AGSL shader implementing Snell-Descartes refraction law.
 *
 * n1 * sin(θ1) = n2 * sin(θ2)
 *
 * The wave surface provides the normal vector. Light rays passing
 * through the glass/water boundary are bent according to the
 * refractive index ratio, creating realistic distortion.
 *
 * Inputs:
 * - iResolution: screen size
 * - iTime: animation time (drives the wave)
 * - iRefractiveIndex: n2/n1 ratio (1.33 = water, 1.5 = glass)
 * - iWaveAmplitude: height of waves in pixels
 * - iWaveFrequency: number of wave cycles across screen
 * - iZoneTop: top of the refraction zone (in pixels from top)
 * - iZoneBottom: bottom of the refraction zone
 * - iDistortionStrength: scales the refraction offset
 */
private const val REFRACTION_SHADER = """
    uniform shader inputTexture;
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iRefractiveIndex;
    uniform float iWaveAmplitude;
    uniform float iWaveFrequency;
    uniform float iZoneTopStart;
    uniform float iZoneTopEnd;
    uniform float iZoneBotStart;
    uniform float iZoneBotEnd;
    uniform float iDistortionStrength;

    // Wave surface height at position x
    float waveHeight(float x, float phase) {
        return iWaveAmplitude * sin(x * iWaveFrequency * 6.2832 / iResolution.x + phase);
    }

    // Surface normal from wave derivative (Snell's law needs the normal)
    float2 waveNormal(float x, float phase) {
        float epsilon = 1.0;
        float h0 = waveHeight(x - epsilon, phase);
        float h1 = waveHeight(x + epsilon, phase);
        float dhdx = (h1 - h0) / (2.0 * epsilon);
        // Normal to the wave surface: (-dh/dx, 1) normalized
        return normalize(float2(-dhdx, 1.0));
    }

    // Apply Snell's law: n1*sin(theta1) = n2*sin(theta2)
    // Returns the refracted direction offset
    float2 snellRefract(float2 incident, float2 normal, float eta) {
        // eta = n1/n2
        float cosI = dot(-incident, normal);
        float sinT2 = eta * eta * (1.0 - cosI * cosI);

        // Total internal reflection check
        if (sinT2 > 1.0) {
            return float2(0.0, 0.0);
        }

        float cosT = sqrt(1.0 - sinT2);
        float2 refracted = eta * incident + (eta * cosI - cosT) * normal;
        return refracted;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord;

        // Check if we're in a refraction zone
        float phase = iTime * 1.5708; // quarter turn per second

        // Top zone: from iZoneTopStart to iZoneTopEnd
        bool inTopZone = uv.y >= iZoneTopStart && uv.y <= iZoneTopEnd;
        // Bottom zone: from iZoneBotStart to iZoneBotEnd  
        bool inBotZone = uv.y >= iZoneBotStart && uv.y <= iZoneBotEnd;

        if (inTopZone || inBotZone) {
            float2 normal;
            float depth; // how deep into the zone (0 at edge, 1 at solid end)

            if (inTopZone) {
                float waveY = iZoneTopEnd + waveHeight(uv.x, phase);
                depth = 1.0 - (uv.y - iZoneTopStart) / (iZoneTopEnd - iZoneTopStart);
                normal = waveNormal(uv.x, phase);
            } else {
                float waveY = iZoneBotStart + waveHeight(uv.x, phase);
                depth = (uv.y - iZoneBotStart) / (iZoneBotEnd - iZoneBotStart);
                normal = waveNormal(uv.x, phase);
                normal.y = -normal.y; // flip for bottom zone
            }

            // Incident ray (straight down from viewer)
            float2 incident = float2(0.0, 1.0);

            // Apply Snell's law
            float eta = 1.0 / iRefractiveIndex; // n1/n2
            float2 refracted = snellRefract(incident, normal, eta);

            // Scale distortion by depth (more distortion deeper into the glass)
            float distortion = depth * iDistortionStrength;
            float2 offset = refracted * distortion;

            // Displaced texture lookup
            float2 displaced = uv + offset;
            displaced = clamp(displaced, float2(0.0), iResolution);

            half4 color = inputTexture.eval(displaced);

            // Subtle brightness boost at the wave edge (caustic-like)
            float edgeDist;
            if (inTopZone) {
                edgeDist = abs(uv.y - (iZoneTopEnd + waveHeight(uv.x, phase)));
            } else {
                edgeDist = abs(uv.y - (iZoneBotStart + waveHeight(uv.x, phase)));
            }
            float caustic = exp(-edgeDist * 0.05) * 0.15;
            color.rgb += half3(caustic * 0.5, caustic * 0.8, caustic);

            return color;
        }

        return inputTexture.eval(uv);
    }
"""

/**
 * Creates a refraction RenderEffect using Snell-Descartes law.
 */
fun createRefractionEffect(
    width: Float,
    height: Float,
    time: Float,
    topZoneEnd: Float,
    bottomZoneStart: Float,
    refractiveIndex: Float = 1.33f, // water
    waveAmplitude: Float = 12f,
    waveFrequency: Float = 3f,
    distortionStrength: Float = 25f,
): RenderEffect? {
    return try {
        val shader = RuntimeShader(REFRACTION_SHADER)
        shader.setFloatUniform("iResolution", width, height)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("iRefractiveIndex", refractiveIndex)
        shader.setFloatUniform("iWaveAmplitude", waveAmplitude)
        shader.setFloatUniform("iWaveFrequency", waveFrequency)
        shader.setFloatUniform("iZoneTopStart", 0f)
        shader.setFloatUniform("iZoneTopEnd", topZoneEnd)
        shader.setFloatUniform("iZoneBotStart", bottomZoneStart)
        shader.setFloatUniform("iZoneBotEnd", height)
        shader.setFloatUniform("iDistortionStrength", distortionStrength)

        RenderEffect.createRuntimeShaderEffect(shader, "inputTexture")
    } catch (e: Exception) {
        // Fallback: no effect if shader fails
        null
    }
}
