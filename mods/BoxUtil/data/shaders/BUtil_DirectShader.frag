#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

in vec2 fragUV;

layout (binding = 0) uniform sampler2D tex;
uniform float alphaFix;

layout (location = 0) out vec4 fragColor;

void main()
{
    vec4 result = texture(tex, fragUV);
    if (alphaFix > -1.0) result.w = alphaFix;
    fragColor = result;
}
