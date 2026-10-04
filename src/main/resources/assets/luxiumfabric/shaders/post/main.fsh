#version 330

uniform sampler2D SceneSampler;
uniform sampler2D BloomSampler;
in vec2 texCoord;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform LuxiumConfig { float Exposure; float Contrast; float Saturation; float BloomStrength; float Gamma; };
out vec4 fragColor;

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
    for (int y = -1; y <= 1; ++y) for (int x = -1; x <= 1; ++x) bloom += texture(BloomSampler, texCoord + px * vec2(x,y)).rgb;
    bloom /= 9.0;
    vec3 mapped = tonemap(scene + bloom * BloomStrength);
    mapped = contrastSaturation(mapped);
    mapped = pow(max(mapped, vec3(0.0)), vec3(1.0 / max(Gamma, 0.01)));
    fragColor = vec4(clamp(mapped, 0.0, 1.0), 1.0);
}