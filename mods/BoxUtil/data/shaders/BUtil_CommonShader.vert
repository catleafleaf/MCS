#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define COLOR 0
#define EMISSIVE_COLOR 1
#define EMISSIVE_SA 2
#define LIGHT_COLOR 3
#define SHADOW_COLOR 4
#define LIGHT_DIR 5

subroutine vec3 vertexStateCompute(in mat4 inModel, out vec3 faceNormal);
subroutine uniform vertexStateCompute vertexState;
subroutine void instanceStateCompute(out mat4 model, out vec4 mColor, out vec4 mEColor);
subroutine uniform instanceStateCompute instanceState;

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;

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
// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(lightColor), vec4(shadowColor), vec4(lightDirection, 0.0)
uniform vec4 statePackage[6];
uniform float instanceDataExtra;
uniform vec3 baseSize;

layout (binding = 8) uniform samplerBuffer vertexData_TBN_A;
layout (binding = 9) uniform samplerBuffer vertexData_TBN_B;

out VERTEX_BLOCK {
	vec3 fragNormal;
	vec2 fragUV;
	vec4 fragEntityColor;
	vec4 fragMixEmissive;
} vb_data;
flat out vec3 fragLight;

subroutine(vertexStateCompute) vec3 commonMode(in mat4 inModel, out vec3 faceNormal) {
	mat3 modelInv = inverse(mat3(inModel));
	mat3 normalMatrix = transpose(modelInv);
	int realIndex = int(floor(float(gl_VertexID) / 3.0));
	mat3 TBN = mat3(texelFetch(vertexData_TBN_A, realIndex), texelFetch(vertexData_TBN_B, realIndex).xy, vec3(0.0));
	TBN[0] = normalize(normalMatrix * TBN[0]);
	TBN[1] = normalize(normalMatrix * TBN[1]);
	TBN[2] = cross(TBN[0], TBN[1]);
	TBN = transpose(TBN);
	faceNormal = TBN * normalize(normal * modelInv) * 0.5 + 0.5;
	return TBN * -statePackage[LIGHT_DIR].xyz;
}

subroutine(vertexStateCompute) vec3 colorMode(in mat4 inModel, out vec3 faceNorma) {
	faceNorma = vec3(0.5, 0.5, 1.0);
	return vec3(0.0);
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
	vec3 faceNormal;
	instanceState(currentMatrix, entityColor, entityEmissiveColor);
	entityEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));
	vb_data.fragUV = uv;
	vb_data.fragEntityColor = entityColor;
	vb_data.fragMixEmissive = entityEmissiveColor;
	fragLight = vertexState(currentMatrix, faceNormal);
	vb_data.fragNormal = faceNormal;
	vec4 vertexPos = gameViewport * currentMatrix * vec4(vertex * baseSize, 1.0);
	vertexPos.z /= baseSize.z;
	if (max(entityColor.w, entityEmissiveColor.w) <= 0.0) vertexPos.xyz = vec3(-65536.0);
	gl_Position = vertexPos;
}
