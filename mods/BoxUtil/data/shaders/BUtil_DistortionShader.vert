#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define INOUT_FACTOR 2
#define STATE 4

subroutine void instanceStateCompute(out mat4 model, out uint timerState, out float mixFactor);
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
uniform mat4 modelMatrix;
// vec4(sizeIn, powerIn, powerFull), vec4(sizeFull, powerOut, hardnessRing), vec4(sizeOut, fadeInFactor, fadeOutFactor)
// vec4(sizeInRatio, sizeFullRatio), vec4(sizeOutRatio, hardnessInner, globalTimerRaw), vec4(arcStart, arcEnd, innerCenter)
uniform vec4 statePackage[6];
uniform float instanceDataExtra;
uniform float screenScale;

out VERTEX_BLOCK {
	vec4 fragUVMask;
	vec4 fragUVScreen;
} vb_data;

vec2 getUV(vec2 location) {
	return (location - gameScreenBorder.xy) / gameScreenBorder.zw;
}

subroutine(instanceStateCompute) void noneData(out mat4 model, out uint timerState, out float mixFactor) {
	model = modelMatrix;
	float check = statePackage[STATE].w;
	vec2 tmp = vec2(1.0);
	uint tmpU = 2u;
	if (check > 2.0) {
		tmp = vec2(abs(3.0 - check), statePackage[INOUT_FACTOR].z);
		tmpU = 0u;
	}
	if (check <= 1.0 && check > -500.0) {
		tmp = vec2(check, statePackage[INOUT_FACTOR].w);
		tmpU = 1u;
	}
	timerState = tmpU;
	mixFactor = pow(tmp.x, tmp.y);
}

subroutine(instanceStateCompute) void haveData2D(out mat4 model, out uint timerState, out float mixFactor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final1.w;
	final1.w = final0.z;
	final0.z = 0.0;
	model = modelMatrix * transpose(mat4(final0, final1, vec4(0.0, 0.0, 1.0, 0.0), vec4(0.0, 0.0, 0.0, 1.0)));
	float ft = trunc(alpha * 0.1);
	uint ut =  uint(ft);
	timerState = ut;
	float getPowValue = 1.0;
	if (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;
	if (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;
	mixFactor = pow(alpha - ft * 10.0, getPowValue);
}

subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out uint timerState, out float mixFactor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec2 final1 = texelFetch(dataPackage_Final1, gl_InstanceID).xy;
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final0.x;

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

	float ft = trunc(alpha * 0.1);
	uint ut =  uint(ft);
	timerState = ut;
	float getPowValue = 1.0;
	if (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;
	if (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;
	mixFactor = pow(alpha - ft * 10.0, getPowValue);
}

subroutine(instanceStateCompute) void haveData3D(out mat4 model, out uint timerState, out float mixFactor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);
	vec4 final2 = texelFetch(dataPackage_Final2, gl_InstanceID);
	vec2 tmpZY = final0.zy;
	final0.yz = vec2(final1.w, final2.w);
	final1.w = tmpZY.x;
	final2.w = tmpZY.y;
	model = modelMatrix * transpose(mat4(final0, final1, final2, vec4(0.0, 0.0, 0.0, 1.0)));
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : texelFetch(dataPackage_State0, gl_InstanceID).w;
	float ft = trunc(alpha * 0.1);
	uint ut =  uint(ft);
	timerState = ut;
	float getPowValue = 1.0;
	if (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;
	if (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;
	mixFactor = pow(alpha - ft * 10.0, getPowValue);
}

subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out uint timerState, out float mixFactor) {
	vec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);
	vec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);
	vec2 final2 = texelFetch(dataPackage_Final2, gl_InstanceID).xy;
	float alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final2.x;

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

	float ft = trunc(alpha * 0.1);
	uint ut =  uint(ft);
	timerState = ut;
	float getPowValue = 1.0;
	if (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;
	if (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;
	mixFactor = pow(alpha - ft * 10.0, getPowValue);
}

void main()
{
	mat4 currentMatrix;
	uint mixStateValue;
	float mixFactorValue;
	instanceState(currentMatrix, mixStateValue, mixFactorValue);
	vec4 size = vec4(statePackage[1].xy, statePackage[3].zw);
	vec4 sizeMix = size;
	float power = statePackage[0].w;
	float powerMix = power;
	if (mixStateValue == 0u) {
		sizeMix = vec4(statePackage[0].xy, statePackage[3].xy);
		powerMix = statePackage[0].z;
	}
	if (mixStateValue == 1u) {
		sizeMix = vec4(statePackage[2].xy, statePackage[4].xy);
		powerMix = statePackage[1].z;
	}
	size = mix(sizeMix, size, mixFactorValue);
	power = mix(powerMix, power, mixFactorValue);
	mat3 rotateScale = mat3(currentMatrix[0].x, currentMatrix[0].y, 0.0, currentMatrix[1].x, currentMatrix[1].y, 0.0, vec3(0.0, 0.0, 1.0));
	vec3 locationPre = vec3(vertex * size.xy, 1.0);
	vec4 location = currentMatrix * vec4(locationPre.xy, 0.0, 1.0);
	vec3 locationPost = abs(rotateScale * locationPre);
	vec4 currPos = gameViewport * location;
	if (power == 0.0) currPos.xyz = vec3(-65536.0);
	gl_Position = currPos;
	vb_data.fragUVMask = vec4(vertex, vertex / size.zw - statePackage[5].zw);
	vb_data.fragUVScreen = vec4(getUV(location.xy) * screenScale, locationPost.xy / gameScreenBorder.zw * vertex * 0.5 * screenScale * power);
}
