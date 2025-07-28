#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define COLOR 0
#define EMISSIVE_COLOR 1
#define EMISSIVE_SA 2
#define COM_STATE 3
#define FILL_DATA 4
#define WIDTH_DATA 5
#define START_COLOR 6
#define END_COLOR 7
#define START_EMISSIVE 8
#define END_EMISSIVE 9

layout (location = 0) in vec2 disabled;

layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData
{
	mat4 gameViewport;
	vec4 gameScreenBorder; // vec4(screenLB, screenSize)
};

// vec2(point)
layout (binding = 10) uniform samplerBuffer nodeMap;
uniform mat4 modelMatrix;
// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(texturePixels, time, nodeCount, mixFactor), vec4(fillStart, fillEnd, startFactor, endFactor), vec4(startWidth, endWidth, jitterPower, flickerMix)
// 6: vec4(start), 7: vec4(end), 8: vec4(startEmissive), 9: vec4(endEmissive)
uniform vec4 statePackage[10];
// hashCode, hashCodeTime
uniform vec2 extraData;

out VERT_GEOM_BLOCK {
	flat mat4 geomMatrix;
	flat vec4 geomColor;
	flat vec4 geomEmissiveColor;
	flat vec4 geomNormalWidthDistance;
	flat float geomFactor;
} vgb_data;

float flickRandom(in float seed) {
	vec2 tanSeed = tan(vec2(seed) * vec2(0.42, 4.2) + vec2(12.7, 51.97));
	float result = fract(smoothstep(1.0, 0.0, sin(tanSeed.x) + abs(tanSeed.y)) * 2.0);
	return 1.0 - sqrt(result);
}

float getFlick() {
	return (statePackage[WIDTH_DATA].w <= -1.0f) ? 1.0 : mix(1.0, flickRandom(extraData.y), statePackage[WIDTH_DATA].w);
}

void main()
{
	int currIndex = gl_InstanceID + gl_VertexID;
	float factor = clamp(float(currIndex) / statePackage[COM_STATE].z, 0.0, 1.0);
	float factorPow = pow(factor, statePackage[COM_STATE].w);
	int leftIndex = max(currIndex - 1, 0);
	int rightIndex = min(currIndex + 1, int(statePackage[COM_STATE].z));
	vec3 rawNodeData = texelFetch(nodeMap, currIndex).xyz;
	vec2 leftTangent = rawNodeData.xy - texelFetch(nodeMap, leftIndex).xy;
	if (leftIndex != currIndex) {
		leftTangent = normalize(leftTangent);
		leftTangent = vec2(-leftTangent.y, leftTangent.x);
	} else leftTangent = vec2(0.0);
	vec2 rightTangent = texelFetch(nodeMap, rightIndex).xy - rawNodeData.xy;
	if (rightIndex != currIndex) {
		rightTangent = normalize(rightTangent);
		rightTangent = vec2(-rightTangent.y, rightTangent.x);
	} else rightTangent = vec2(0.0);
	vec2 currNormal = normalize(leftTangent + rightTangent);

	float flicker = getFlick();
	vec4 entityColor = mix(statePackage[START_COLOR], statePackage[END_COLOR], factorPow) * statePackage[COLOR] * statePackage[EMISSIVE_SA].w * flicker;
	vec4 entityEmissiveColor = mix(statePackage[START_EMISSIVE], statePackage[END_EMISSIVE], factorPow) * statePackage[EMISSIVE_COLOR] * statePackage[EMISSIVE_SA].w * flicker;

	vgb_data.geomMatrix = gameViewport * modelMatrix;
	vgb_data.geomColor = entityColor;
	vgb_data.geomEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));
	vgb_data.geomNormalWidthDistance = vec4(currNormal, mix(statePackage[WIDTH_DATA].x, statePackage[WIDTH_DATA].y, factorPow), rawNodeData.z);
	vgb_data.geomFactor = factor;
	gl_Position = vec4(rawNodeData.xy, 0.0, 1.0);
}
