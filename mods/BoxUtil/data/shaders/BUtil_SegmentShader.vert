#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define COLOR 0
#define EMISSIVE_COLOR 1
#define EMISSIVE_SA 2

layout (location = 0) in vec2 disabled;

layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData
{
	mat4 gameViewport;
	vec4 gameScreenBorder; // vec4(screenLB, screenSize)
};

// vec4*3(final), vec4(state0), vec4(color)
layout (binding = 10) uniform samplerBuffer stateMap;
layout (binding = 11) uniform samplerBuffer mixFactorMap;
layout (binding = 12) uniform samplerBuffer colorMap;
uniform mat4 modelMatrix;
// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, reversed, time), vec4(fillStart, fillEnd, startFactor, endFactor)
uniform vec4 statePackage[5];

out VERT_TESC_BLOCK {
	flat mat4 tescMatrix;
	flat vec4 tescPoints;
	flat vec4 tescColor;
	flat vec4 tescEmissiveColor;
	flat float tescWidth;
	flat float tescMixFactor;
	flat float tescDistance;
} vtb_data;

void main()
{
	int indexA = gl_InstanceID * 4 + gl_VertexID * 2;
	int indexB = indexA + 1;
	vec4 vertexA = texelFetch(stateMap, indexA); // vec4(vec2(loc), vec2(tangent left)),
	vec4 vertexB = texelFetch(stateMap, indexB); // vec4(vec2(tangent right), width, distance)
	vec4 entityColor = statePackage[COLOR] * texelFetch(colorMap, indexA) * statePackage[EMISSIVE_SA].w;
	vec4 entityEmissiveColor = statePackage[EMISSIVE_COLOR] * texelFetch(colorMap, indexB) * statePackage[EMISSIVE_SA].w;

	vtb_data.tescMatrix = gameViewport * modelMatrix;
	vtb_data.tescPoints = vertexA.xy.xyxy + vec4(vertexA.zw, vertexB.xy);
	vtb_data.tescColor = entityColor;
	vtb_data.tescEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));
	vtb_data.tescWidth = vertexB.z;
	vtb_data.tescMixFactor = texelFetch(mixFactorMap, gl_InstanceID * 2 + gl_VertexID).x;
	vtb_data.tescDistance = vertexB.w;
	gl_Position = vec4(vertexA.xy, 0.0, 1.0);
}
