#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define FRINGE_COLOR 0
#define CORE_COLOR 1
#define STATE_A 2
#define STATE_B 3
#define STATE_EXT 4

subroutine void instanceStateCompute(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha);
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
// vec4(fringeColor), vec4(coreColor), vec4(size, aspect, flick/syncFlick), vec4(alpha, hashCode, glowPower, frameAmount), vec4(noisePower, flickMix, globalAlpha, discRatio)
uniform vec4 statePackage[5];
uniform float instanceDataExtra;

out VERTEX_BLOCK {
	vec2 fragUV;
	flat vec4 fragFringeColor;
	flat vec4 fragCoreColor;
	flat vec2 fragNoiseOffsetAlpha;
} vb_data;

float flickRandom(in float seed) {
	vec2 tanSeed = tan(vec2(seed) * vec2(0.42, 4.2) + vec2(12.7, 51.97));
	float result = fract(smoothstep(1.0, 0.0, sin(tanSeed.x) + abs(tanSeed.y)) * 2.0);
	return 1.0 - sqrt(result);
}

float getFlick() {
	uint flickState = uint(statePackage[STATE_A].w);
	float flickOffset = ((flickState & 1u) == 1u) ? statePackage[STATE_B].y : (float(gl_InstanceID << 2) - statePackage[STATE_B].y) * 0.01;
	return (flickState > 2u) ? mix(1.0, flickRandom(flickOffset + statePackage[STATE_B].w), statePackage[STATE_EXT].y) : 1.0;
}

subroutine(instanceStateCompute) void noneData(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {
	model = modelMatrix;
	float flick = getFlick();
	vec4 resultF = statePackage[FRINGE_COLOR];
	vec4 resultC = statePackage[CORE_COLOR];
	mFColor = resultF;
	mCColor = resultC;;
	currAlpha = statePackage[STATE_B].x * flick;
}

subroutine(instanceStateCompute) void haveData2D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {
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
	mCColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[CORE_COLOR];
	mFColor = mix(colorMat[2], colorMat[3], alpha) * statePackage[FRINGE_COLOR];
	currAlpha = alpha * getFlick();;
}

subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {
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
	mCColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[CORE_COLOR];
	mFColor = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[FRINGE_COLOR];
	currAlpha = alpha * getFlick();;
}

subroutine(instanceStateCompute) void haveData3D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {
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
	mCColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[CORE_COLOR];
	mFColor = mix(colorMat[2], colorMat[3], alpha) * statePackage[FRINGE_COLOR];
	currAlpha = alpha * getFlick();;
}

subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {
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
	mCColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[CORE_COLOR];
	mFColor = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[FRINGE_COLOR];
	currAlpha = alpha * getFlick();
}

void main()
{
	mat4 currentMatrix;
	vec4 entityFringeColor;
	vec4 entityCoreColor;
	float entityAlpha;
	instanceState(currentMatrix, entityCoreColor, entityFringeColor, entityAlpha);

	vb_data.fragUV = vertex;
	vb_data.fragFringeColor = entityFringeColor;
	vb_data.fragCoreColor = entityCoreColor;
	vb_data.fragNoiseOffsetAlpha = vec2(mod(length(vec3(currentMatrix[3].xyz)), 100.0), entityAlpha * statePackage[STATE_EXT].z);
	vec4 vertexPos = gameViewport * currentMatrix * vec4(vertex * statePackage[STATE_A].xy, 0.0, 1.0);
	if (max(entityFringeColor.w, entityCoreColor.w) <= 0.0) vertexPos.xyz = vec3(-65536.0);
	gl_Position = vertexPos;
}
