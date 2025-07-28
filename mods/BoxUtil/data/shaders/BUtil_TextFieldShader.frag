#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

uniform sampler2D fontMap[4];

in GEOM_FRAG_BLOCK {
    vec3 fragUV;
    flat uint fragIsEdge;

    flat vec4 fragColor;
    flat uvec3 fragStyleState; // fuck intel
    flat uvec3 fragState; // cahnnel, texIndex, reserved
} gfb_data;

layout (location = 0) out vec4 fragColor;

float getUnderline(in float uv) {
    return smoothstep(0.05, 0.04, abs(uv - 0.1));
}

float getStrikeout(in float uv) {
    return smoothstep(0.07, 0.06, abs(uv - 0.5));
}

void main()
{
    vec4 result = texture(fontMap[gfb_data.fragState.y], gfb_data.fragUV.xy);
    if (gfb_data.fragState.x == 4u) result = vec4(result.x);
    if (gfb_data.fragState.x == 8u) result = vec4(result.y);
    if (gfb_data.fragState.x == 12u) result = vec4(result.z);
    if (gfb_data.fragState.x == 16u) result = vec4(result.w);
    if (bool(gfb_data.fragIsEdge)) result = vec4(1.0, 1.0, 1.0, 0.0);
    if (bool(gfb_data.fragStyleState.y)) result = max(result, vec4(getUnderline(gfb_data.fragUV.z)));
    if (bool(gfb_data.fragStyleState.z)) result = max(result, vec4(getStrikeout(gfb_data.fragUV.z)));
    if (bool(gfb_data.fragStyleState.x)) result = vec4(1.0, 1.0, 1.0, 1.0 - result.w);
    result *= gfb_data.fragColor;
    if (bool(gfb_data.fragState.z)) result = vec4(gfb_data.fragColor.xyz, 0.0);
    fragColor = result;
}
