#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define CURVE_STATE 3

layout (lines) in;
layout (triangle_strip, max_vertices = 4) out;

// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, globalUV, time), vec4(fillStart, fillEnd, startFactor, endFactor)
uniform vec4 statePackage[5];

in TESE_GEOM_BLOCK {
	flat mat4 geomMatrix;
	flat vec2 geomNormal;
	flat vec4 geomColor;
	flat vec4 geomEmissiveColor;
	flat float geomWidth;
	flat float geomUV;
} tgb_datas[];

out GEOM_FRAG_BLOCK {
	vec2 fragUV;
	vec4 fragEntityColor;
	vec4 fragMixEmissive;
} gfb_data;

void main()
{
	float time = statePackage[CURVE_STATE].w;
	mat4 matrix = tgb_datas[0].geomMatrix;
	vec4 startOffset = vec4(tgb_datas[0].geomNormal * tgb_datas[0].geomWidth, 0.0, 0.0);
	vec4 endOffset = vec4(tgb_datas[1].geomNormal * tgb_datas[1].geomWidth, 0.0, 0.0);

	if (max(tgb_datas[0].geomColor.w, tgb_datas[0].geomEmissiveColor.w) <= 0.0 && max(tgb_datas[1].geomColor.w, tgb_datas[1].geomEmissiveColor.w) <= 0.0) {
		matrix = mat4(0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, 1.0);
		startOffset = endOffset = vec4(0.0);
	}
	gfb_data.fragEntityColor = tgb_datas[0].geomColor;
	gfb_data.fragMixEmissive = tgb_datas[0].geomEmissiveColor;
	gfb_data.fragUV = vec2(tgb_datas[0].geomUV, 1.0);
	gl_Position = matrix * (gl_in[0].gl_Position + startOffset);
	EmitVertex();
	gfb_data.fragUV = vec2(tgb_datas[0].geomUV, 0.0);
	gl_Position = matrix * (gl_in[0].gl_Position - startOffset);
	EmitVertex();
	gfb_data.fragEntityColor = tgb_datas[1].geomColor;
	gfb_data.fragMixEmissive = tgb_datas[1].geomEmissiveColor;
	gfb_data.fragUV = vec2(tgb_datas[1].geomUV, 1.0);
	gl_Position = matrix * (gl_in[1].gl_Position + endOffset);
	EmitVertex();
	gfb_data.fragUV = vec2(tgb_datas[1].geomUV, 0.0);
	gl_Position = matrix * (gl_in[1].gl_Position - endOffset);
	EmitVertex();
	EndPrimitive();
}
