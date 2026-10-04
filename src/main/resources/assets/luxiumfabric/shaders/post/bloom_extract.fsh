#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform BloomExtractConfig { float Threshold; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    float mask = smoothstep(Threshold, Threshold + 0.35, luma);
    vec3 bright = max(c - vec3(Threshold), vec3(0.0)) * mask;
    fragColor = vec4(bright, 1.0);
}