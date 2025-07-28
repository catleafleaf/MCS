#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

layout (location = 0) in vec4 uv; // uvBL, uvTR
layout (location = 1) in vec4 location; // x, y, topStyleUV, bottomStyleUV
layout (location = 2) in float styleF; // invert(1+8) + italic(1+7) + underline(1+6) + strikeout(1+5) + channel(3+2) + handelIndex(2+0); must cast to uint after, fuck glsl, integer type have tons bugs in vertex attributes
layout (location = 3) in vec4 color;
layout (location = 4) in vec2 size; // size, edge
layout (location = 5) in vec2 edge; // edge
layout (location = 6) in float fill;

layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData
{
	mat4 gameViewport;
	vec4 gameScreenBorder; // vec4(screenLB, screenSize)
};
uniform vec4 globalColor;
uniform mat4 modelMatrix;

out VERT_GEOM_BLOCK {
	mat4 geomMatrix;
	vec4 geomUV;
	vec2 geomStyleUV;
	vec4 geomSize;
	flat float geomFill;

	flat vec4 geomColor;
	flat uvec4 geomStyleState; // fuck intel
	flat uvec3 geomState; // cahnnel, texIndex, reserved
} vgb_data;

void main()
{
	vgb_data.geomMatrix = gameViewport * modelMatrix;
	vgb_data.geomUV = uv;
	vgb_data.geomStyleUV = location.zw;
	vgb_data.geomSize = vec4(size, edge);
	vgb_data.geomFill = fill;
	vgb_data.geomColor = color * globalColor;
	uint style = uint(styleF);
	vgb_data.geomStyleState = uvec4(style >> 8, style >> 7, style >> 6, style >> 5) & 1u;
	vgb_data.geomState = uvec3(style & 28u, style & 3u, ((uv.x < -500.0) ? 1u : 0u));
	gl_Position = vec4(location.xy, 0.0, 1.0);
}
