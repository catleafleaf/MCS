#version OVERWRITE_VERSION

precision OVERWRITE_PRECISION float;

layout (location = 0) in vec2 vertex;

smooth out vec2 fragUV;

void main()
{
	gl_Position = vec4(vertex, 0.0, 1.0);
	fragUV = max(vertex, vec2(0.0));
}
