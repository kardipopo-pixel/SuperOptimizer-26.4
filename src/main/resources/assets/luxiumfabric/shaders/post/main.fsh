#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D SceneSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform LuxiumVisualConfig {
    float Exposure;
    float Contrast;
    float Saturation;
    float BloomStrength;
    float VignetteStrength;
    float HighlightRollOff;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec3 tonemap(vec3 x) {
    x *= Exposure;
    const float A = 0.22; const float B = 0.30; const float C = 0.10; const float D = 0.20; const float E = 0.01; const float F = 0.30;
    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B * D) + D * F)) - E / F;
}

vec3 contrastSaturation(vec3 c) {
    c = mix(vec3(0.5), c, Contrast);
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(luma), c, Saturation);
}

void main() {
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    vec2 px = 1.0 / max(InSize, vec2(1.0));
    vec3 bloom = vec3(0.0);
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            bloom += texture(BloomSampler, texCoord + px * vec2(x, y)).rgb;
        }
    }
    bloom /= 9.0;

    float edge = distance(texCoord, vec2(0.5));
    float vignette = 1.0 - smoothstep(0.45, 0.78, edge) * VignetteStrength;
    float luma = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    float roll = 1.0 / (1.0 + max(luma - 1.0, 0.0) * max(HighlightRollOff, 0.0));

    vec3 mapped = tonemap(scene * roll + bloom * BloomStrength);
    mapped = contrastSaturation(mapped) * vignette;
    fragColor = vec4(clamp(mapped, 0.0, 1.0), 1.0);
}