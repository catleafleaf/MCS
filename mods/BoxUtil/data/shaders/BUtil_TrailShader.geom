#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define COM_STATE 3

layout (lines) in;
layout (triangle_strip, max_vertices = 4) out;

uniform mat4 modelMatrix;
// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(texturePixels, time, nodeCount, mixFactor), vec4(fillStart, fillEnd, startFactor, endFactor), vec4(startWidth, endWidth, jitterPower, flickerMix)
// 6: vec4(start), 7: vec4(end), 8: vec4(startEmissive), 9: vec4(endEmissive)
uniform vec4 statePackage[10];

in VERT_GEOM_BLOCK {
	flat mat4 geomMatrix;
	flat vec4 geomColor;
	flat vec4 geomEmissiveColor;
	flat vec4 geomNormalWidthDistance;
	flat float geomFactor;
} vgb_datas[];

out GEOM_FRAG_BLOCK {
	vec4 fragUVSeedFactor;
	vec4 fragEntityColor;
	vec4 fragMixEmissive;
} gfb_data;

void main()
{
	float time = statePackage[COM_STATE].y;
	mat4 matrix = vgb_datas[0].geomMatrix;
	vec4 startOffset = vec4(vgb_datas[0].geomNormalWidthDistance.xy * vgb_datas[0].geomNormalWidthDistance.z, 0.0, 0.0);
	vec4 endOffset = vec4(vgb_datas[1].geomNormalWidthDistance.xy * vgb_datas[1].geomNormalWidthDistance.z, 0.0, 0.0);
	vec2 uv = (vec2(vgb_datas[0].geomNormalWidthDistance.w, vgb_datas[1].geomNormalWidthDistance.w) / statePackage[COM_STATE].x) - statePackage[COM_STATE].y;
	float seed;

	if (max(vgb_datas[0].geomColor.w, vgb_datas[0].geomEmissiveColor.w) <= 0.0 && max(vgb_datas[1].geomColor.w, vgb_datas[1].geomEmissiveColor.w) <= 0.0) {
		matrix = mat4(0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, 1.0);
		startOffset = endOffset = vec4(0.0);
	}
	seed = fract((gl_in[0].gl_Position.x + gl_in[0].gl_Position.y) * 0.255) + 127.0;
	seed *= 640.0;
	gfb_data.fragEntityColor = vgb_datas[0].geomColor;
	gfb_data.fragMixEmissive = vgb_datas[0].geomEmissiveColor;
	gfb_data.fragUVSeedFactor = vec4(uv.x, 1.0, seed, vgb_datas[0].geomFactor);
	gl_Position = matrix * (gl_in[0].gl_Position + startOffset);
	EmitVertex();
	gfb_data.fragUVSeedFactor = vec4(uv.x, 0.0, seed, vgb_datas[0].geomFactor);
	gl_Position = matrix * (gl_in[0].gl_Position - startOffset);
	EmitVertex();
	seed = fract((gl_in[1].gl_Position.x + gl_in[1].gl_Position.y) * 0.255) + 127.0;
	seed *= 640.0;
	gfb_data.fragEntityColor = vgb_datas[1].geomColor;
	gfb_data.fragMixEmissive = vgb_datas[1].geomEmissiveColor;
	gfb_data.fragUVSeedFactor = vec4(uv.y, 1.0, seed, vgb_datas[1].geomFactor);
	gl_Position = matrix * (gl_in[1].gl_Position + endOffset);
	EmitVertex();
	gfb_data.fragUVSeedFactor = vec4(uv.y, 0.0, seed, vgb_datas[1].geomFactor);
	gl_Position = matrix * (gl_in[1].gl_Position - endOffset);
	EmitVertex();
	EndPrimitive();
}
