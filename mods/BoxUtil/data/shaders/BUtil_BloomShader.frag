#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

subroutine vec4 sampleMode();
subroutine uniform sampleMode sampleModeState;

smooth in mat4 fragUV;

layout (binding = 0) uniform sampler2D screen;

layout (location = 0) out vec4 fragColor;

subroutine(sampleMode) vec4 downSample() {
    vec4 result = texture(screen, fragUV[0].zw);
    result += texture(screen, fragUV[1].xy);
    result += texture(screen, fragUV[1].zw);
    result += texture(screen, fragUV[2].xy);
    return result * 0.125 + texture(screen, fragUV[0].xy) * 0.5;
}

subroutine(sampleMode) vec4 upSample() {
    vec4 resultA = texture(screen, fragUV[0].xy);
    resultA += texture(screen, fragUV[0].zw);
    resultA += texture(screen, fragUV[1].xy);
    resultA += texture(screen, fragUV[1].zw);
    vec4 resultB = texture(screen, fragUV[2].xy);
    resultB += texture(screen, fragUV[2].zw);
    resultB += texture(screen, fragUV[3].xy);
    resultB += texture(screen, fragUV[3].zw);
    return resultA * 0.1666667 + resultB * 0.0833333;
}

void main() {
    fragColor = sampleModeState() * 1.2;
}
