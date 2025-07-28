#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define EMISSIVE_SA 2
#define FILL_DATA 4
#define WIDTH_DATA 5
#define ALPHA_THRESHOLD 0.003

in GEOM_FRAG_BLOCK {
    vec4 fragUVSeedFactor;
    vec4 fragEntityColor;
    vec4 fragMixEmissive;
} gfb_data;

// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(texturePixels, time, nodeCount, mixFactor), vec4(fillStart, fillEnd, startFactor, endFactor), vec4(startWidth, endWidth, jitterPower, flickerMix)
// 6: vec4(start), 7: vec4(end), 8: vec4(startEmissive), 9: vec4(endEmissive)
uniform vec4 statePackage[10];
// hashCode, hashCodeTime
uniform vec2 extraData;
uniform int additionEmissive;
// diffuse, normal, ao, emissive
layout (binding = 0) uniform sampler2D diffuseMap;
layout (binding = 2) uniform sampler2D aoMap;
layout (binding = 3) uniform sampler2D emissiveMap;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)
layout (location = 2) out vec4 fragEmissiveColor;

float hash12(vec2 p)
{
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float getJitter(in float uv, in float seed) {
    float uvO = hash12(vec2(round(seed) + 255.0, extraData.x)) * 2.0 - 1.0;
    float posFactor = step(0.0, uvO);

    float result = uv - posFactor;
    uvO = abs(uvO) + 1.0;
    result = mix(result, result * uvO, statePackage[WIDTH_DATA].z) + posFactor;
    result = clamp(result, 0.0, 1.0);
    return result;
}

void main()
{
    vec2 uv = gfb_data.fragUVSeedFactor.xy;
    uv.y = getJitter(uv.y, gfb_data.fragUVSeedFactor.z);
    vec2 fillMix = smoothstep(statePackage[FILL_DATA].zw, vec2(1.0), vec2(1.0 - gfb_data.fragUVSeedFactor.w, gfb_data.fragUVSeedFactor.w)) * (1.0 - statePackage[FILL_DATA].xy);
    fillMix = 1.0 - fillMix;
    float fillFactor = clamp(fillMix.x * fillMix.y, 0.0, 1.0);

    vec4 diffuse = texture(diffuseMap, uv) * gfb_data.fragEntityColor;
    vec4 emissive = texture(emissiveMap, uv) * gfb_data.fragMixEmissive;
    diffuse.w *= fillFactor;
    emissive.w *= fillFactor;
    if (diffuse.w + emissive.w <= ALPHA_THRESHOLD) discard;
    diffuse.xyz *= texture(aoMap, uv).x;

    fragColor = additionEmissive == 1 ? (diffuse + emissive * emissive.w) : mix(diffuse, emissive, emissive.w);
    fragCombineData = vec4(statePackage[EMISSIVE_SA].z * emissive.w, gl_FragCoord.z, statePackage[EMISSIVE_SA].z, max(diffuse.w, emissive.w));
    emissive.w *= statePackage[EMISSIVE_SA].z;
    fragEmissiveColor = emissive;
}
