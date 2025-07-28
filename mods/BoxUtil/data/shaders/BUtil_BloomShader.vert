#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

#define RADIUS_SCALE OVERWRITE_RADIUS_SCALE
#define SCREENSTEP_X OVERWRITE_SCREEN_X * RADIUS_SCALE
#define SCREENSTEP_X2 SCREENSTEP_X * 2.0
#define SCREENSTEP_Y OVERWRITE_SCREEN_Y * RADIUS_SCALE
#define SCREENSTEP_Y2 SCREENSTEP_Y * 2.0

subroutine mat4 sampleMode(in vec2 uv);
subroutine uniform sampleMode sampleModeState;

layout (location = 0) in vec2 vertex;

uniform float scale;

smooth out mat4 fragUV;

subroutine(sampleMode) mat4 downSample(in vec2 uv) {
	return mat4(vec4(0.0, 0.0, -SCREENSTEP_X, -SCREENSTEP_Y), vec4(SCREENSTEP_X, -SCREENSTEP_Y, SCREENSTEP_X, SCREENSTEP_Y), vec4(-SCREENSTEP_X, SCREENSTEP_Y, 0.0, 0.0), vec4(0.0)) + mat4(uv, uv, uv, uv, uv, uv, uv, uv);
}

subroutine(sampleMode) mat4 upSample(in vec2 uv) {
	return mat4(vec4(-SCREENSTEP_X, -SCREENSTEP_Y, SCREENSTEP_X, -SCREENSTEP_Y), vec4(SCREENSTEP_X, SCREENSTEP_Y, -SCREENSTEP_X, SCREENSTEP_Y), vec4(-SCREENSTEP_X2, 0.0, 0.0, -SCREENSTEP_Y2), vec4(SCREENSTEP_X2, 0.0, 0.0, SCREENSTEP_Y2)) + mat4(uv, uv, uv, uv, uv, uv, uv, uv);
}

void main() {
	gl_Position = vec4(vertex, 0.0, 1.0);
	fragUV = sampleModeState(max(vertex, vec2(0.0))) / scale;
}
