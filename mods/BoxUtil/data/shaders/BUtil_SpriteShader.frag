#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define EMISSIVE_SA 2
#define ALPHA_THRESHOLD 0.003

in VERTEX_BLOCK {
    vec2 fragUV;
    vec4 fragEntityColor;
    vec4 fragMixEmissive;
} vb_data;

// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec2(tile), startIndex, randomEach, vec2(start), vec2(end), hashCode, totalTilesMinusOne, vec2(baseSize)
uniform vec4 statePackage[6];
uniform int additionEmissive;
// diffuse, normal, ao, emissive
layout (binding = 0) uniform sampler2D diffuseMap;
layout (binding = 2) uniform sampler2D aoMap;
layout (binding = 3) uniform sampler2D emissiveMap;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)
layout (location = 2) out vec4 fragEmissiveColor;

void main()
{
    vec4 diffuse = texture(diffuseMap, vb_data.fragUV);
    vec4 emissive = texture(emissiveMap, vb_data.fragUV) * vb_data.fragMixEmissive;
    diffuse *= vb_data.fragEntityColor;
    if (diffuse.w + emissive.w <= ALPHA_THRESHOLD) discard;
    diffuse.xyz *= texture(aoMap, vb_data.fragUV).x;

    fragColor = additionEmissive == 1 ? (diffuse + emissive * emissive.w) : mix(diffuse, emissive, emissive.w);
    fragCombineData = vec4(statePackage[EMISSIVE_SA].z * emissive.w, gl_FragCoord.z, statePackage[EMISSIVE_SA].z, max(diffuse.w, emissive.w));
    emissive.w *= statePackage[EMISSIVE_SA].z;
    fragEmissiveColor = emissive;
}
