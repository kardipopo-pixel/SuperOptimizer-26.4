#version 330
#extension GL_ARB_separate_shader_objects : require

const vec2[] corners = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0,  1.0)
);

layout(location = 0) out vec2 texCoord;

void main() {
    vec2 p = corners[gl_VertexIndex];
    gl_Position = vec4(p, 0.0, 1.0);
    texCoord = p * 0.5 + 0.5;
}