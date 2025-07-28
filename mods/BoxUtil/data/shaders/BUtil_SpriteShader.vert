#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define COLOR 0
#define EMISSIVE_COLOR 1
#define EMISSIVE_SA 2
#define TILE_STATE 3
#define UV_START_END 4
#define ENTITY_STATE 5

subroutine void uvMappingState(out vec2 uvStartP, out vec2 uvEndP);
subroutine uniform uvMappingState uvMapping;
subroutine void instanceStateCompute(out mat4 model, out vec4 mColor, out vec4 mEColor);
subroutine uniform instanceStateCompute instanceState;

layout (location = 0) in vec2 vertex;

layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData
{
	mat4 gameViewport;
	vec4 gameScreenBorder; // vec4(screenLB, screenSize)
};

// vec4*3(final), vec4(state0), vec4(color)
layout (binding = 10) uniform samplerBuffer dataPackage_Final0;
layout (binding = 11) uniform samplerBuffer dataPackage_Final1;
layout (binding = 12) uniform samplerBuffer dataPackage_Final2;
layout (binding = 13) uniform samplerBuffer dataPackage_State0;
layout (binding = 14) uniform usamplerBuffer dataPackage_Color;
uniform mat4 modelMatrix;
// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha)
// [vec2(tile), startIndex, randomEach], [vec2(start), vec2(end)], hashCode, totalTilesMinusOne, vec2(baseSize)
uniform vec4 statePackage[6];
uniform float instanceDataExtra;

out VERTEX_BLOCK {
	vec2 fragUV;
	vec4 fragEntityColor;
	vec4 fragMixEmissive;
} vb_data;

float hash12(vec2 p)
{
	vec3 p3 = fract(vec3(p.xyx) * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

subroutine(uvMappingState) void commonUV(out vec2 uvStartP, out vec2 uvEndP) {
	uvStartP = statePackage[UV_START_END].xy;
	uvEndP = statePackage[UV_START_END].zw;
}

subroutine(uvMappingState) void tileUV(out vec2 uvStartP, out vec2 uvEndP) {
	float tileX = mod(statePackage[TILE_STATE].z, statePackage[TILE_STATE].x);
	float tileY = ceil((statePackage[TILE_STATE].z - tileX) / statePackage[TILE_STATE].y);
	vec2 tileSizeVec = 1.0 / statePackage[TILE_STATE].xy * (statePackage[UV_START_END].zw - statePackage[UV_START_END].xy);
	uvStartP = tileSizeVec * vec2(tileX, tileY) + statePackage[UV_START_END].xy;
	uvEndP = uvStartP + tileSizeVec;
}

subroutine(uvMappingState) void tileRUV(out vec2 uvStartP, out vec2 uvEndP) {
	vec4 tileState = statePackage[TILE_STATE];
	vec2 seed = vec2(statePackage[ENTITY_STATE].x, 0.0);
	if (tileState.w > 0.0) seed.y = float(gl_InstanceID << 1 + 7);
	float finalIndex = round(hash12(seed) * statePackage[ENTITY_STATE].y) + tileState.z;
	if (finalIndex >= statePackage[ENTITY_STATE].y) finalIndex -= statePackage[ENTITY_STATE].y;
	float tileX = mod(finalIndex, tileState.x);
	float tileY = ceil((finalIndex - tileX) / tileState.y);
	vec2 tileSizeVec = 1.0 / tileState.xy * (statePackage[UV_START_END].zw - statePackage[UV_START_END].xy);
	uvStartP = tileSizeVec * vec2(tileX, tileY) + statePackage[UV_START_END].xy;
	uvEndP = uvStartP + tileSizeVec;
}

subroutine(instanceStateCompute) void noneData(out mat4 model, out vec4 mColor, out vec4 mEColor) {
	model = modelMatrix;
	float alpha = statePackage[EMISSIVE_SA].w;
	mColor = statePackage[COLOR] * alpha;
	mEColor = statePackage[EMISSIVE_COLOR] * alpha;
}

subroutine(instanceStateCompute) void haveData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final1.w;
	alpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);
	final1.w = final0.z;
	final0.z = 0.0;
	model = modelMatrix * transpose(mat4(final0, final1, vec4(0.0, 0.0, 1.0, 0.0), vec4(0.0, 0.0, 0.0, 1.0)));
	uvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);
	vec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);
	vec4 highColor = vec4((colorPackage >> 16) & 0xFFu);
	vec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);
	vec4 highEmissive = vec4(colorPackage & 0xFFu);
	mat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;
	vec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];
	resultColor.w *= alpha;
	vec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];
	resultEmissive.w *= alpha;
	mColor = resultColor;
	mEColor = resultEmissive;
}

subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec2 final1 = texelFetch(dataPackage_Final1, gl_InstanceID).xy;
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final0.x;
	alpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);

	float pryFacing = radians(final0.y * 0.5);
	float pryCos = cos(pryFacing);
	float prySin = sin(pryFacing);
	float dqz = prySin + prySin;
	float q22 = dqz * prySin;
	float q23 = dqz * pryCos;
	model = modelMatrix * mat4(
	vec4(final1.x - q22 * final1.x, q23 * final1.x, 0.0, 0.0),
	vec4(-q23 * final1.y, final1.y - q22 * final1.y, 0.0, 0.0),
	vec4(0.0, 0.0, 1.0, 0.0),
	vec4(final0.zw, 0.0, 1.0));

	uvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);
	vec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];
	resultColor.w *= alpha;
	vec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];
	resultEmissive.w *= alpha;
	mColor = resultColor;
	mEColor = resultEmissive;
}

subroutine(instanceStateCompute) void haveData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);
	vec4 final2 = texelFetch(dataPackage_Final2, gl_InstanceID);
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : texelFetch(dataPackage_State0, gl_InstanceID).w;
	alpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);
	vec2 tmpZY = final0.zy;
	final0.yz = vec2(final1.w, final2.w);
	final1.w = tmpZY.x;
	final2.w = tmpZY.y;
	model = modelMatrix * transpose(mat4(final0, final1, final2, vec4(0.0, 0.0, 0.0, 1.0)));
	uvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);
	vec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);
	vec4 highColor = vec4((colorPackage >> 16) & 0xFFu);
	vec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);
	vec4 highEmissive = vec4(colorPackage & 0xFFu);
	mat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;
	vec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];
	resultColor.w *= alpha;
	vec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];
	resultEmissive.w *= alpha;
	mColor = resultColor;
	mEColor = resultEmissive;
}

subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);
	vec2 final2 = texelFetch(dataPackage_Final2, gl_InstanceID).xy;
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final2.x;
	alpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);

	vec3 pryRotate = radians(final0.xyz * 0.5);
	vec3 pryCos = cos(pryRotate);
	vec3 prySin = sin(pryRotate);
	float wq = pryCos.x * pryCos.y * pryCos.z - prySin.x * prySin.y * prySin.z;
	float xq = prySin.x * pryCos.y * pryCos.z - pryCos.x * prySin.y * prySin.z;
	float yq = pryCos.x * prySin.y * pryCos.z + prySin.x * pryCos.y * prySin.z;
	float zq = pryCos.x * pryCos.y * prySin.z + prySin.x * prySin.y * pryCos.z;
	float dqx = xq + xq;
	float dqy = yq + yq;
	float dqz = zq + zq;
	float q00 = dqx * xq;
	float q11 = dqy * yq;
	float q22 = dqz * zq;
	float q01 = dqx * yq;
	float q02 = dqx * zq;
	float q03 = dqx * wq;
	float q12 = dqy * zq;
	float q13 = dqy * wq;
	float q23 = dqz * wq;
	model = modelMatrix * mat4(
	vec4(final1.x - (q11 + q22) * final1.x, (q01 + q23) * final1.x, (q02 - q13) * final1.x, 0.0),
	vec4((q01 - q23) * final1.y, final1.y - (q22 + q00) * final1.y, (q12 + q03) * final1.y, 0.0),
	vec4((q02 + q13) * final1.z, (q12 - q03) * final1.z, final1.z - (q11 + q00) * final1.z, 0.0),
	vec4(final0.w, final1.w, final2.y, 1.0));

	uvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);
	vec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];
	resultColor.w *= alpha;
	vec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];
	resultEmissive.w *= alpha;
	mColor = resultColor;
	mEColor = resultEmissive;
}

void main()
{
	mat4 currentMatrix;
	vec4 entityColor;
	vec4 entityEmissiveColor;
	instanceState(currentMatrix, entityColor, entityEmissiveColor);
	entityEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));
	vec2 startUV;
	vec2 endUV;
	uvMapping(startUV, endUV);
	vec2 uvs[] = vec2[4](startUV, vec2(endUV.x, startUV.y), vec2(startUV.x, endUV.y), endUV);
	vb_data.fragUV = uvs[gl_VertexID];
	vb_data.fragEntityColor = entityColor;
	vb_data.fragMixEmissive = entityEmissiveColor;
	vec4 vertexPos = gameViewport * currentMatrix * vec4(vertex * statePackage[ENTITY_STATE].zw, 0.0, 1.0);
	if (max(entityColor.w, entityEmissiveColor.w) <= 0.0) vertexPos.xyz = vec3(-65536.0);
	gl_Position = vertexPos;
}
