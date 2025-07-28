#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define EMISSIVE_SA 2
#define LIGHT_COLOR 3
#define SHADOW_COLOR 4
#define ALPHA_THRESHOLD 0.003

subroutine void surfaceStateDraw(inout vec3 diffuseParam, out float brightness, in vec3 currentNormal);
subroutine uniform surfaceStateDraw surfaceState;

in VERTEX_BLOCK {
    vec3 fragNormal;
    vec2 fragUV;
    vec4 fragEntityColor;
    vec4 fragMixEmissive;
} vb_data;
flat in vec3 fragLight;

// vec4(color), vec4(emissiveColor), vec4(alphaMix, colorMix, glowPower, timerAlpha), vec4(lightColor), vec4(shadowColor), vec4(lightDirection, reserved)
uniform vec4 statePackage[6];
uniform int additionEmissive;
// diffuse, normal, ao, emissive
layout (binding = 0) uniform sampler2D diffuseMap;
layout (binding = 1) uniform sampler2D normalMap;
layout (binding = 2) uniform sampler2D aoMap;
layout (binding = 3) uniform sampler2D emissiveMap;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)
layout (location = 2) out vec4 fragEmissiveColor;
layout (location = 3) out vec4 fragNormal;

vec3 normalBlend(in vec3 face, in vec3 tex) {
    vec3 t = face * 2.0 + vec3(-1.0, -1.0, 0.0);
    vec3 u = tex * vec3(-2.0, -2.0, 2.0) + vec3(1.0, 1.0, -1.0);
    return normalize(t * dot(t, u) / t.z - u);
}

subroutine(surfaceStateDraw) void commonMode(inout vec3 diffuseParam, out float brightness, in vec3 currentNormal) {
    float brightnessRaw = max(dot(fragLight, currentNormal), 0.0);
    vec3 shadowMix = mix(vec3(1.0), statePackage[SHADOW_COLOR].xyz, statePackage[SHADOW_COLOR].w);
    vec3 lightMix = mix(vec3(1.0), statePackage[LIGHT_COLOR].xyz, statePackage[LIGHT_COLOR].w);
    diffuseParam *= mix(shadowMix, lightMix, brightnessRaw) * mix(texture(aoMap, vb_data.fragUV).x, 1.0, smoothstep(0.1, 1.0, brightnessRaw * brightnessRaw * brightnessRaw) * 0.5);
    brightness = brightnessRaw;
}

subroutine(surfaceStateDraw) void colorMode(inout vec3 diffuseParam, out float brightness, in vec3 currentNormal) {
    diffuseParam *= statePackage[LIGHT_COLOR].w * texture(aoMap, vb_data.fragUV).x;
    brightness = 1.0;
}

void main()
{
    vec4 diffuse = texture(diffuseMap, vb_data.fragUV);
    vec4 emissive = texture(emissiveMap, vb_data.fragUV) * vb_data.fragMixEmissive;
    diffuse *= vb_data.fragEntityColor;
    if (diffuse.w + emissive.w <= ALPHA_THRESHOLD) discard;

    float brightness;
    vec4 normalRaw = texture(normalMap, vb_data.fragUV);
    normalRaw.w *= diffuse.w;
    if (normalRaw.w <= 0.0) normalRaw.xyz = vec3(0.5, 0.5, 1.0);
    normalRaw.xyz = normalBlend(vb_data.fragNormal, normalRaw.xyz);
    surfaceState(diffuse.xyz, brightness, normalRaw.xyz);

    fragColor = additionEmissive == 1 ? (diffuse + emissive * emissive.w) : mix(diffuse, emissive, emissive.w);
    fragCombineData = vec4(brightness * diffuse.w, gl_FragCoord.z, statePackage[EMISSIVE_SA].z, max(diffuse.w, emissive.w));
    fragEmissiveColor = emissive * statePackage[EMISSIVE_SA].z;
    fragNormal = vec4(normalRaw.xyz * 0.5 + 0.5, normalRaw.w);
}
