package org.boxutil.units.builtin.shader;

/**
 * <strong>DO NOT EDIT THEM.</strong>
 */
public final class BUtil_ShaderSources {
    public final static class Common {
        private Common() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define COLOR 0\n" +
                "#define EMISSIVE_COLOR 1\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define LIGHT_COLOR 3\n" +
                "#define SHADOW_COLOR 4\n" +
                "#define LIGHT_DIR 5\n" +
                "\n" +
                "subroutine vec3 vertexStateCompute(in mat4 inModel, out vec3 faceNormal);\n" +
                "subroutine uniform vertexStateCompute vertexState;\n" +
                "subroutine void instanceStateCompute(out mat4 model, out vec4 mColor, out vec4 mEColor);\n" +
                "subroutine uniform instanceStateCompute instanceState;\n" +
                "\n" +
                "layout (location = 0) in vec3 vertex;\n" +
                "layout (location = 1) in vec3 normal;\n" +
                "layout (location = 2) in vec2 uv;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "\n" +
                "// vec4*3(final), vec4(state0), vec4(color)\n" +
                "layout (binding = 10) uniform samplerBuffer dataPackage_Final0;\n" +
                "layout (binding = 11) uniform samplerBuffer dataPackage_Final1;\n" +
                "layout (binding = 12) uniform samplerBuffer dataPackage_Final2;\n" +
                "layout (binding = 13) uniform samplerBuffer dataPackage_State0;\n" +
                "layout (binding = 14) uniform usamplerBuffer dataPackage_Color;\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(lightColor), vec4(shadowColor), vec4(lightDirection, 0.0)\n" +
                "uniform vec4 statePackage[6];\n" +
                "uniform float instanceDataExtra;\n" +
                "uniform vec3 baseSize;\n" +
                "\n" +
                "layout (binding = 8) uniform samplerBuffer vertexData_TBN_A;\n" +
                "layout (binding = 9) uniform samplerBuffer vertexData_TBN_B;\n" +
                "\n" +
                "out VERTEX_BLOCK {\n" +
                "\tvec3 fragNormal;\n" +
                "\tvec2 fragUV;\n" +
                "\tvec4 fragEntityColor;\n" +
                "\tvec4 fragMixEmissive;\n" +
                "} vb_data;\n" +
                "flat out vec3 fragLight;\n" +
                "\n" +
                "subroutine(vertexStateCompute) vec3 commonMode(in mat4 inModel, out vec3 faceNormal) {\n" +
                "\tmat3 modelInv = inverse(mat3(inModel));\n" +
                "\tmat3 normalMatrix = transpose(modelInv);\n" +
                "\tint realIndex = int(floor(float(gl_VertexID) / 3.0));\n" +
                "\tmat3 TBN = mat3(texelFetch(vertexData_TBN_A, realIndex), texelFetch(vertexData_TBN_B, realIndex).xy, vec3(0.0));\n" +
                "\tTBN[0] = normalize(normalMatrix * TBN[0]);\n" +
                "\tTBN[1] = normalize(normalMatrix * TBN[1]);\n" +
                "\tTBN[2] = cross(TBN[0], TBN[1]);\n" +
                "\tTBN = transpose(TBN);\n" +
                "\tfaceNormal = TBN * normalize(normal * modelInv) * 0.5 + 0.5;\n" +
                "\treturn TBN * -statePackage[LIGHT_DIR].xyz;\n" +
                "}\n" +
                "\n" +
                "subroutine(vertexStateCompute) vec3 colorMode(in mat4 inModel, out vec3 faceNorma) {\n" +
                "\tfaceNorma = vec3(0.5, 0.5, 1.0);\n" +
                "\treturn vec3(0.0);\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void noneData(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tmodel = modelMatrix;\n" +
                "\tfloat alpha = statePackage[EMISSIVE_SA].w;\n" +
                "\tmColor = statePackage[COLOR] * alpha;\n" +
                "\tmEColor = statePackage[EMISSIVE_COLOR] * alpha;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final1.w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tfinal1.w = final0.z;\n" +
                "\tfinal0.z = 0.0;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, vec4(0.0, 0.0, 1.0, 0.0), vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tvec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec2 final1 = texelFetch(dataPackage_Final1, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final0.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tfloat pryFacing = radians(final0.y * 0.5);\n" +
                "\tfloat pryCos = cos(pryFacing);\n" +
                "\tfloat prySin = sin(pryFacing);\n" +
                "\tfloat dqz = prySin + prySin;\n" +
                "\tfloat q22 = dqz * prySin;\n" +
                "\tfloat q23 = dqz * pryCos;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - q22 * final1.x, q23 * final1.x, 0.0, 0.0),\n" +
                "\tvec4(-q23 * final1.y, final1.y - q22 * final1.y, 0.0, 0.0),\n" +
                "\tvec4(0.0, 0.0, 1.0, 0.0),\n" +
                "\tvec4(final0.zw, 0.0, 1.0));\n" +
                "\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec4 final2 = texelFetch(dataPackage_Final2, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : texelFetch(dataPackage_State0, gl_InstanceID).w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tvec2 tmpZY = final0.zy;\n" +
                "\tfinal0.yz = vec2(final1.w, final2.w);\n" +
                "\tfinal1.w = tmpZY.x;\n" +
                "\tfinal2.w = tmpZY.y;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, final2, vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tvec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec2 final2 = texelFetch(dataPackage_Final2, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final2.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tvec3 pryRotate = radians(final0.xyz * 0.5);\n" +
                "\tvec3 pryCos = cos(pryRotate);\n" +
                "\tvec3 prySin = sin(pryRotate);\n" +
                "\tfloat wq = pryCos.x * pryCos.y * pryCos.z - prySin.x * prySin.y * prySin.z;\n" +
                "\tfloat xq = prySin.x * pryCos.y * pryCos.z - pryCos.x * prySin.y * prySin.z;\n" +
                "\tfloat yq = pryCos.x * prySin.y * pryCos.z + prySin.x * pryCos.y * prySin.z;\n" +
                "\tfloat zq = pryCos.x * pryCos.y * prySin.z + prySin.x * prySin.y * pryCos.z;\n" +
                "\tfloat dqx = xq + xq;\n" +
                "\tfloat dqy = yq + yq;\n" +
                "\tfloat dqz = zq + zq;\n" +
                "\tfloat q00 = dqx * xq;\n" +
                "\tfloat q11 = dqy * yq;\n" +
                "\tfloat q22 = dqz * zq;\n" +
                "\tfloat q01 = dqx * yq;\n" +
                "\tfloat q02 = dqx * zq;\n" +
                "\tfloat q03 = dqx * wq;\n" +
                "\tfloat q12 = dqy * zq;\n" +
                "\tfloat q13 = dqy * wq;\n" +
                "\tfloat q23 = dqz * wq;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - (q11 + q22) * final1.x, (q01 + q23) * final1.x, (q02 - q13) * final1.x, 0.0),\n" +
                "\tvec4((q01 - q23) * final1.y, final1.y - (q22 + q00) * final1.y, (q12 + q03) * final1.y, 0.0),\n" +
                "\tvec4((q02 + q13) * final1.z, (q12 - q03) * final1.z, final1.z - (q11 + q00) * final1.z, 0.0),\n" +
                "\tvec4(final0.w, final1.w, final2.y, 1.0));\n" +
                "\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tmat4 currentMatrix;\n" +
                "\tvec4 entityColor;\n" +
                "\tvec4 entityEmissiveColor;\n" +
                "\tvec3 faceNormal;\n" +
                "\tinstanceState(currentMatrix, entityColor, entityEmissiveColor);\n" +
                "\tentityEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));\n" +
                "\tvb_data.fragUV = uv;\n" +
                "\tvb_data.fragEntityColor = entityColor;\n" +
                "\tvb_data.fragMixEmissive = entityEmissiveColor;\n" +
                "\tfragLight = vertexState(currentMatrix, faceNormal);\n" +
                "\tvb_data.fragNormal = faceNormal;\n" +
                "\tvec4 vertexPos = gameViewport * currentMatrix * vec4(vertex * baseSize, 1.0);\n" +
                "\tvertexPos.z /= baseSize.z;\n" +
                "\tif (max(entityColor.w, entityEmissiveColor.w) <= 0.0) vertexPos.xyz = vec3(-65536.0);\n" +
                "\tgl_Position = vertexPos;\n" +
                "}\n";

        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define LIGHT_COLOR 3\n" +
                "#define SHADOW_COLOR 4\n" +
                "#define ALPHA_THRESHOLD 0.003\n" +
                "\n" +
                "subroutine void surfaceStateDraw(inout vec3 diffuseParam, out float brightness, in vec3 currentNormal);\n" +
                "subroutine uniform surfaceStateDraw surfaceState;\n" +
                "\n" +
                "in VERTEX_BLOCK {\n" +
                "    vec3 fragNormal;\n" +
                "    vec2 fragUV;\n" +
                "    vec4 fragEntityColor;\n" +
                "    vec4 fragMixEmissive;\n" +
                "} vb_data;\n" +
                "flat in vec3 fragLight;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(alphaMix, colorMix, glowPower, timerAlpha), vec4(lightColor), vec4(shadowColor), vec4(lightDirection, reserved)\n" +
                "uniform vec4 statePackage[6];\n" +
                "uniform int additionEmissive;\n" +
                "// diffuse, normal, ao, emissive\n" +
                "layout (binding = 0) uniform sampler2D diffuseMap;\n" +
                "layout (binding = 1) uniform sampler2D normalMap;\n" +
                "layout (binding = 2) uniform sampler2D aoMap;\n" +
                "layout (binding = 3) uniform sampler2D emissiveMap;\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)\n" +
                "layout (location = 2) out vec4 fragEmissiveColor;\n" +
                "layout (location = 3) out vec4 fragNormal;\n" +
                "\n" +
                "vec3 normalBlend(in vec3 face, in vec3 tex) {\n" +
                "    vec3 t = face * 2.0 + vec3(-1.0, -1.0, 0.0);\n" +
                "    vec3 u = tex * vec3(-2.0, -2.0, 2.0) + vec3(1.0, 1.0, -1.0);\n" +
                "    return normalize(t * dot(t, u) / t.z - u);\n" +
                "}\n" +
                "\n" +
                "subroutine(surfaceStateDraw) void commonMode(inout vec3 diffuseParam, out float brightness, in vec3 currentNormal) {\n" +
                "    float brightnessRaw = max(dot(fragLight, currentNormal), 0.0);\n" +
                "    vec3 shadowMix = mix(vec3(1.0), statePackage[SHADOW_COLOR].xyz, statePackage[SHADOW_COLOR].w);\n" +
                "    vec3 lightMix = mix(vec3(1.0), statePackage[LIGHT_COLOR].xyz, statePackage[LIGHT_COLOR].w);\n" +
                "    diffuseParam *= mix(shadowMix, lightMix, brightnessRaw) * mix(texture(aoMap, vb_data.fragUV).x, 1.0, smoothstep(0.1, 1.0, brightnessRaw * brightnessRaw * brightnessRaw) * 0.5);\n" +
                "    brightness = brightnessRaw;\n" +
                "}\n" +
                "\n" +
                "subroutine(surfaceStateDraw) void colorMode(inout vec3 diffuseParam, out float brightness, in vec3 currentNormal) {\n" +
                "    diffuseParam *= statePackage[LIGHT_COLOR].w * texture(aoMap, vb_data.fragUV).x;\n" +
                "    brightness = 1.0;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec4 diffuse = texture(diffuseMap, vb_data.fragUV);\n" +
                "    vec4 emissive = texture(emissiveMap, vb_data.fragUV) * vb_data.fragMixEmissive;\n" +
                "    diffuse *= vb_data.fragEntityColor;\n" +
                "    if (diffuse.w + emissive.w <= ALPHA_THRESHOLD) discard;\n" +
                "\n" +
                "    float brightness;\n" +
                "    vec4 normalRaw = texture(normalMap, vb_data.fragUV);\n" +
                "    normalRaw.w *= diffuse.w;\n" +
                "    if (normalRaw.w <= 0.0) normalRaw.xyz = vec3(0.5, 0.5, 1.0);\n" +
                "    normalRaw.xyz = normalBlend(vb_data.fragNormal, normalRaw.xyz);\n" +
                "    surfaceState(diffuse.xyz, brightness, normalRaw.xyz);\n" +
                "\n" +
                "    fragColor = additionEmissive == 1 ? (diffuse + emissive * emissive.w) : mix(diffuse, emissive, emissive.w);\n" +
                "    fragCombineData = vec4(brightness * diffuse.w, gl_FragCoord.z, statePackage[EMISSIVE_SA].z, max(diffuse.w, emissive.w));\n" +
                "    fragEmissiveColor = emissive * statePackage[EMISSIVE_SA].z;\n" +
                "    fragNormal = vec4(normalRaw.xyz * 0.5 + 0.5, normalRaw.w);\n" +
                "}\n";
    }

    public final static class Sprite {
        private Sprite() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define COLOR 0\n" +
                "#define EMISSIVE_COLOR 1\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define TILE_STATE 3\n" +
                "#define UV_START_END 4\n" +
                "#define ENTITY_STATE 5\n" +
                "\n" +
                "subroutine void uvMappingState(out vec2 uvStartP, out vec2 uvEndP);\n" +
                "subroutine uniform uvMappingState uvMapping;\n" +
                "subroutine void instanceStateCompute(out mat4 model, out vec4 mColor, out vec4 mEColor);\n" +
                "subroutine uniform instanceStateCompute instanceState;\n" +
                "\n" +
                "layout (location = 0) in vec2 vertex;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "\n" +
                "// vec4*3(final), vec4(state0), vec4(color)\n" +
                "layout (binding = 10) uniform samplerBuffer dataPackage_Final0;\n" +
                "layout (binding = 11) uniform samplerBuffer dataPackage_Final1;\n" +
                "layout (binding = 12) uniform samplerBuffer dataPackage_Final2;\n" +
                "layout (binding = 13) uniform samplerBuffer dataPackage_State0;\n" +
                "layout (binding = 14) uniform usamplerBuffer dataPackage_Color;\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha)\n" +
                "// [vec2(tile), startIndex, randomEach], [vec2(start), vec2(end)], hashCode, totalTilesMinusOne, vec2(baseSize)\n" +
                "uniform vec4 statePackage[6];\n" +
                "uniform float instanceDataExtra;\n" +
                "\n" +
                "out VERTEX_BLOCK {\n" +
                "\tvec2 fragUV;\n" +
                "\tvec4 fragEntityColor;\n" +
                "\tvec4 fragMixEmissive;\n" +
                "} vb_data;\n" +
                "\n" +
                "float hash12(vec2 p)\n" +
                "{\n" +
                "\tvec3 p3 = fract(vec3(p.xyx) * 0.1031);\n" +
                "\tp3 += dot(p3, p3.yzx + 33.33);\n" +
                "\treturn fract((p3.x + p3.y) * p3.z);\n" +
                "}\n" +
                "\n" +
                "subroutine(uvMappingState) void commonUV(out vec2 uvStartP, out vec2 uvEndP) {\n" +
                "\tuvStartP = statePackage[UV_START_END].xy;\n" +
                "\tuvEndP = statePackage[UV_START_END].zw;\n" +
                "}\n" +
                "\n" +
                "subroutine(uvMappingState) void tileUV(out vec2 uvStartP, out vec2 uvEndP) {\n" +
                "\tfloat tileX = mod(statePackage[TILE_STATE].z, statePackage[TILE_STATE].x);\n" +
                "\tfloat tileY = ceil((statePackage[TILE_STATE].z - tileX) / statePackage[TILE_STATE].y);\n" +
                "\tvec2 tileSizeVec = 1.0 / statePackage[TILE_STATE].xy * (statePackage[UV_START_END].zw - statePackage[UV_START_END].xy);\n" +
                "\tuvStartP = tileSizeVec * vec2(tileX, tileY) + statePackage[UV_START_END].xy;\n" +
                "\tuvEndP = uvStartP + tileSizeVec;\n" +
                "}\n" +
                "\n" +
                "subroutine(uvMappingState) void tileRUV(out vec2 uvStartP, out vec2 uvEndP) {\n" +
                "\tvec4 tileState = statePackage[TILE_STATE];\n" +
                "\tvec2 seed = vec2(statePackage[ENTITY_STATE].x, 0.0);\n" +
                "\tif (tileState.w > 0.0) seed.y = float(gl_InstanceID << 1 + 7);\n" +
                "\tfloat finalIndex = round(hash12(seed) * statePackage[ENTITY_STATE].y) + tileState.z;\n" +
                "\tif (finalIndex >= statePackage[ENTITY_STATE].y) finalIndex -= statePackage[ENTITY_STATE].y;\n" +
                "\tfloat tileX = mod(finalIndex, tileState.x);\n" +
                "\tfloat tileY = ceil((finalIndex - tileX) / tileState.y);\n" +
                "\tvec2 tileSizeVec = 1.0 / tileState.xy * (statePackage[UV_START_END].zw - statePackage[UV_START_END].xy);\n" +
                "\tuvStartP = tileSizeVec * vec2(tileX, tileY) + statePackage[UV_START_END].xy;\n" +
                "\tuvEndP = uvStartP + tileSizeVec;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void noneData(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tmodel = modelMatrix;\n" +
                "\tfloat alpha = statePackage[EMISSIVE_SA].w;\n" +
                "\tmColor = statePackage[COLOR] * alpha;\n" +
                "\tmEColor = statePackage[EMISSIVE_COLOR] * alpha;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final1.w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tfinal1.w = final0.z;\n" +
                "\tfinal0.z = 0.0;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, vec4(0.0, 0.0, 1.0, 0.0), vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tvec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec2 final1 = texelFetch(dataPackage_Final1, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final0.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tfloat pryFacing = radians(final0.y * 0.5);\n" +
                "\tfloat pryCos = cos(pryFacing);\n" +
                "\tfloat prySin = sin(pryFacing);\n" +
                "\tfloat dqz = prySin + prySin;\n" +
                "\tfloat q22 = dqz * prySin;\n" +
                "\tfloat q23 = dqz * pryCos;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - q22 * final1.x, q23 * final1.x, 0.0, 0.0),\n" +
                "\tvec4(-q23 * final1.y, final1.y - q22 * final1.y, 0.0, 0.0),\n" +
                "\tvec4(0.0, 0.0, 1.0, 0.0),\n" +
                "\tvec4(final0.zw, 0.0, 1.0));\n" +
                "\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec4 final2 = texelFetch(dataPackage_Final2, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : texelFetch(dataPackage_State0, gl_InstanceID).w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tvec2 tmpZY = final0.zy;\n" +
                "\tfinal0.yz = vec2(final1.w, final2.w);\n" +
                "\tfinal1.w = tmpZY.x;\n" +
                "\tfinal2.w = tmpZY.y;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, final2, vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tvec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec2 final2 = texelFetch(dataPackage_Final2, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final2.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tvec3 pryRotate = radians(final0.xyz * 0.5);\n" +
                "\tvec3 pryCos = cos(pryRotate);\n" +
                "\tvec3 prySin = sin(pryRotate);\n" +
                "\tfloat wq = pryCos.x * pryCos.y * pryCos.z - prySin.x * prySin.y * prySin.z;\n" +
                "\tfloat xq = prySin.x * pryCos.y * pryCos.z - pryCos.x * prySin.y * prySin.z;\n" +
                "\tfloat yq = pryCos.x * prySin.y * pryCos.z + prySin.x * pryCos.y * prySin.z;\n" +
                "\tfloat zq = pryCos.x * pryCos.y * prySin.z + prySin.x * prySin.y * pryCos.z;\n" +
                "\tfloat dqx = xq + xq;\n" +
                "\tfloat dqy = yq + yq;\n" +
                "\tfloat dqz = zq + zq;\n" +
                "\tfloat q00 = dqx * xq;\n" +
                "\tfloat q11 = dqy * yq;\n" +
                "\tfloat q22 = dqz * zq;\n" +
                "\tfloat q01 = dqx * yq;\n" +
                "\tfloat q02 = dqx * zq;\n" +
                "\tfloat q03 = dqx * wq;\n" +
                "\tfloat q12 = dqy * zq;\n" +
                "\tfloat q13 = dqy * wq;\n" +
                "\tfloat q23 = dqz * wq;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - (q11 + q22) * final1.x, (q01 + q23) * final1.x, (q02 - q13) * final1.x, 0.0),\n" +
                "\tvec4((q01 - q23) * final1.y, final1.y - (q22 + q00) * final1.y, (q12 + q03) * final1.y, 0.0),\n" +
                "\tvec4((q02 + q13) * final1.z, (q12 - q03) * final1.z, final1.z - (q11 + q00) * final1.z, 0.0),\n" +
                "\tvec4(final0.w, final1.w, final2.y, 1.0));\n" +
                "\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tmat4 currentMatrix;\n" +
                "\tvec4 entityColor;\n" +
                "\tvec4 entityEmissiveColor;\n" +
                "\tinstanceState(currentMatrix, entityColor, entityEmissiveColor);\n" +
                "\tentityEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));\n" +
                "\tvec2 startUV;\n" +
                "\tvec2 endUV;\n" +
                "\tuvMapping(startUV, endUV);\n" +
                "\tvec2 uvs[] = vec2[4](startUV, vec2(endUV.x, startUV.y), vec2(startUV.x, endUV.y), endUV);\n" +
                "\tvb_data.fragUV = uvs[gl_VertexID];\n" +
                "\tvb_data.fragEntityColor = entityColor;\n" +
                "\tvb_data.fragMixEmissive = entityEmissiveColor;\n" +
                "\tvec4 vertexPos = gameViewport * currentMatrix * vec4(vertex * statePackage[ENTITY_STATE].zw, 0.0, 1.0);\n" +
                "\tif (max(entityColor.w, entityEmissiveColor.w) <= 0.0) vertexPos.xyz = vec3(-65536.0);\n" +
                "\tgl_Position = vertexPos;\n" +
                "}\n";

        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define ALPHA_THRESHOLD 0.003\n" +
                "\n" +
                "in VERTEX_BLOCK {\n" +
                "    vec2 fragUV;\n" +
                "    vec4 fragEntityColor;\n" +
                "    vec4 fragMixEmissive;\n" +
                "} vb_data;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec2(tile), startIndex, randomEach, vec2(start), vec2(end), hashCode, totalTilesMinusOne, vec2(baseSize)\n" +
                "uniform vec4 statePackage[6];\n" +
                "uniform int additionEmissive;\n" +
                "// diffuse, normal, ao, emissive\n" +
                "layout (binding = 0) uniform sampler2D diffuseMap;\n" +
                "layout (binding = 2) uniform sampler2D aoMap;\n" +
                "layout (binding = 3) uniform sampler2D emissiveMap;\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)\n" +
                "layout (location = 2) out vec4 fragEmissiveColor;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec4 diffuse = texture(diffuseMap, vb_data.fragUV);\n" +
                "    vec4 emissive = texture(emissiveMap, vb_data.fragUV) * vb_data.fragMixEmissive;\n" +
                "    diffuse *= vb_data.fragEntityColor;\n" +
                "    if (diffuse.w + emissive.w <= ALPHA_THRESHOLD) discard;\n" +
                "    diffuse.xyz *= texture(aoMap, vb_data.fragUV).x;\n" +
                "\n" +
                "    fragColor = additionEmissive == 1 ? (diffuse + emissive * emissive.w) : mix(diffuse, emissive, emissive.w);\n" +
                "    fragCombineData = vec4(statePackage[EMISSIVE_SA].z * emissive.w, gl_FragCoord.z, statePackage[EMISSIVE_SA].z, max(diffuse.w, emissive.w));\n" +
                "    emissive.w *= statePackage[EMISSIVE_SA].z;\n" +
                "    fragEmissiveColor = emissive;\n" +
                "}\n";
    }

    public final static class Curve {
        private Curve() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define COLOR 0\n" +
                "#define EMISSIVE_COLOR 1\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define CURVE_STATE 3\n" +
                "\n" +
                "subroutine void instanceStateCompute(out mat4 model, out vec4 mColor, out vec4 mEColor);\n" +
                "subroutine uniform instanceStateCompute instanceState;\n" +
                "\n" +
                "layout (location = 0) in vec2 vertex;\n" +
                "layout (location = 1) in vec4 tangent;\n" +
                "layout (location = 2) in float width;\n" +
                "layout (location = 3) in float mixFactor;\n" +
                "layout (location = 4) in float nodeDistance;\n" +
                "layout (location = 5) in vec4 curveColor;\n" +
                "layout (location = 6) in vec4 curveEmissive;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "\n" +
                "// vec4*3(final), vec4(state0), vec4(color)\n" +
                "layout (binding = 10) uniform samplerBuffer dataPackage_Final0;\n" +
                "layout (binding = 11) uniform samplerBuffer dataPackage_Final1;\n" +
                "layout (binding = 12) uniform samplerBuffer dataPackage_Final2;\n" +
                "layout (binding = 13) uniform samplerBuffer dataPackage_State0;\n" +
                "layout (binding = 14) uniform usamplerBuffer dataPackage_Color;\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, globalUV, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "uniform float instanceDataExtra;\n" +
                "\n" +
                "out VERT_TESC_BLOCK {\n" +
                "\tflat mat4 tescMatrix;\n" +
                "\tflat vec4 tescPoints;\n" +
                "\tflat vec4 tescColor;\n" +
                "\tflat vec4 tescEmissiveColor;\n" +
                "\tflat float tescWidth;\n" +
                "\tflat float tescMixFactor;\n" +
                "\tflat float tescID;\n" +
                "\tflat float tescDistance;\n" +
                "} vtb_data;\n" +
                "\n" +
                "subroutine(instanceStateCompute) void noneData(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tmodel = modelMatrix;\n" +
                "\tfloat alpha = statePackage[EMISSIVE_SA].w;\n" +
                "\tmColor = statePackage[COLOR] * alpha;\n" +
                "\tmEColor = statePackage[EMISSIVE_COLOR] * alpha;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final1.w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tfinal1.w = final0.z;\n" +
                "\tfinal0.z = 0.0;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, vec4(0.0, 0.0, 1.0, 0.0), vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tvec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec2 final1 = texelFetch(dataPackage_Final1, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final0.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tfloat pryFacing = radians(final0.y * 0.5);\n" +
                "\tfloat pryCos = cos(pryFacing);\n" +
                "\tfloat prySin = sin(pryFacing);\n" +
                "\tfloat dqz = prySin + prySin;\n" +
                "\tfloat q22 = dqz * prySin;\n" +
                "\tfloat q23 = dqz * pryCos;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - q22 * final1.x, q23 * final1.x, 0.0, 0.0),\n" +
                "\tvec4(-q23 * final1.y, final1.y - q22 * final1.y, 0.0, 0.0),\n" +
                "\tvec4(0.0, 0.0, 1.0, 0.0),\n" +
                "\tvec4(final0.zw, 0.0, 1.0));\n" +
                "\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec4 final2 = texelFetch(dataPackage_Final2, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : texelFetch(dataPackage_State0, gl_InstanceID).w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tvec2 tmpZY = final0.zy;\n" +
                "\tfinal0.yz = vec2(final1.w, final2.w);\n" +
                "\tfinal1.w = tmpZY.x;\n" +
                "\tfinal2.w = tmpZY.y;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, final2, vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tvec4 resultColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = mix(colorMat[2], colorMat[3], alpha) * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out vec4 mColor, out vec4 mEColor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec2 final2 = texelFetch(dataPackage_Final2, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final2.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tvec3 pryRotate = radians(final0.xyz * 0.5);\n" +
                "\tvec3 pryCos = cos(pryRotate);\n" +
                "\tvec3 prySin = sin(pryRotate);\n" +
                "\tfloat wq = pryCos.x * pryCos.y * pryCos.z - prySin.x * prySin.y * prySin.z;\n" +
                "\tfloat xq = prySin.x * pryCos.y * pryCos.z - pryCos.x * prySin.y * prySin.z;\n" +
                "\tfloat yq = pryCos.x * prySin.y * pryCos.z + prySin.x * pryCos.y * prySin.z;\n" +
                "\tfloat zq = pryCos.x * pryCos.y * prySin.z + prySin.x * prySin.y * pryCos.z;\n" +
                "\tfloat dqx = xq + xq;\n" +
                "\tfloat dqy = yq + yq;\n" +
                "\tfloat dqz = zq + zq;\n" +
                "\tfloat q00 = dqx * xq;\n" +
                "\tfloat q11 = dqy * yq;\n" +
                "\tfloat q22 = dqz * zq;\n" +
                "\tfloat q01 = dqx * yq;\n" +
                "\tfloat q02 = dqx * zq;\n" +
                "\tfloat q03 = dqx * wq;\n" +
                "\tfloat q12 = dqy * zq;\n" +
                "\tfloat q13 = dqy * wq;\n" +
                "\tfloat q23 = dqz * wq;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - (q11 + q22) * final1.x, (q01 + q23) * final1.x, (q02 - q13) * final1.x, 0.0),\n" +
                "\tvec4((q01 - q23) * final1.y, final1.y - (q22 + q00) * final1.y, (q12 + q03) * final1.y, 0.0),\n" +
                "\tvec4((q02 + q13) * final1.z, (q12 - q03) * final1.z, final1.z - (q11 + q00) * final1.z, 0.0),\n" +
                "\tvec4(final0.w, final1.w, final2.y, 1.0));\n" +
                "\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 resultColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[COLOR];\n" +
                "\tresultColor.w *= alpha;\n" +
                "\tvec4 resultEmissive = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[EMISSIVE_COLOR];\n" +
                "\tresultEmissive.w *= alpha;\n" +
                "\tmColor = resultColor;\n" +
                "\tmEColor = resultEmissive;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tmat4 currentMatrix;\n" +
                "\tvec4 entityColor;\n" +
                "\tvec4 entityEmissiveColor;\n" +
                "\tinstanceState(currentMatrix, entityColor, entityEmissiveColor);\n" +
                "\tentityColor *= curveColor;\n" +
                "\tentityEmissiveColor *= curveEmissive;\n" +
                "\n" +
                "\tvtb_data.tescMatrix = gameViewport * currentMatrix;\n" +
                "\tvtb_data.tescPoints = vertex.xyxy + tangent;\n" +
                "\tvtb_data.tescColor = entityColor;\n" +
                "\tvtb_data.tescEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));\n" +
                "\tvtb_data.tescWidth = width;\n" +
                "\tvtb_data.tescMixFactor = mixFactor;\n" +
                "\tvtb_data.tescID = gl_VertexID == 0 ? 0.0 : ceil(float(gl_VertexID) / 2.0);\n" +
                "\tvtb_data.tescDistance = nodeDistance;\n" +
                "\tgl_Position = vec4(vertex, 0.0, 1.0);\n" +
                "}\n";

        public final static String TESC = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define CURVE_STATE 3\n" +
                "\n" +
                "layout(vertices = 2) out;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, globalUV, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "\n" +
                "in VERT_TESC_BLOCK {\n" +
                "    flat mat4 tescMatrix;\n" +
                "    flat vec4 tescPoints;\n" +
                "    flat vec4 tescColor;\n" +
                "    flat vec4 tescEmissiveColor;\n" +
                "    flat float tescWidth;\n" +
                "    flat float tescMixFactor;\n" +
                "    flat float tescID;\n" +
                "    flat float tescDistance;\n" +
                "} vtb_datas[];\n" +
                "\n" +
                "out TESC_TESE_BLOCK {\n" +
                "    flat mat4 teseMatrix;\n" +
                "    flat vec4 tesePoints;\n" +
                "    flat vec4 teseColor;\n" +
                "    flat vec4 teseEmissiveColor;\n" +
                "    flat float teseWidth;\n" +
                "    flat float teseMixFactor;\n" +
                "    flat float teseID;\n" +
                "    flat float teseDistance;\n" +
                "} ttb_datas[];\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ttb_datas[gl_InvocationID].teseMatrix = vtb_datas[gl_InvocationID].tescMatrix;\n" +
                "    ttb_datas[gl_InvocationID].tesePoints = vtb_datas[gl_InvocationID].tescPoints;\n" +
                "    ttb_datas[gl_InvocationID].teseColor = vtb_datas[gl_InvocationID].tescColor;\n" +
                "    ttb_datas[gl_InvocationID].teseEmissiveColor = vtb_datas[gl_InvocationID].tescEmissiveColor;\n" +
                "    ttb_datas[gl_InvocationID].teseWidth = vtb_datas[gl_InvocationID].tescWidth;\n" +
                "    ttb_datas[gl_InvocationID].teseMixFactor = vtb_datas[gl_InvocationID].tescMixFactor;\n" +
                "    ttb_datas[gl_InvocationID].teseID = vtb_datas[gl_InvocationID].tescID;\n" +
                "    ttb_datas[gl_InvocationID].teseDistance = vtb_datas[gl_InvocationID].tescDistance;\n" +
                "    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;\n" +
                "    if (gl_InvocationID == 0) {\n" +
                "        gl_TessLevelOuter[0] = 1.0;\n" +
                "        gl_TessLevelOuter[1] = statePackage[CURVE_STATE].x;\n" +
                "    }\n" +
                "}\n";

        public final static String TESE = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define CURVE_STATE 3\n" +
                "#define CURVE_FILL 4\n" +
                "\n" +
                "layout(isolines, equal_spacing, ccw) in;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, globalUV, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "uniform float totalNodes;\n" +
                "\n" +
                "in TESC_TESE_BLOCK {\n" +
                "    flat mat4 teseMatrix;\n" +
                "    flat vec4 tesePoints;\n" +
                "    flat vec4 teseColor;\n" +
                "    flat vec4 teseEmissiveColor;\n" +
                "    flat float teseWidth;\n" +
                "    flat float teseMixFactor;\n" +
                "    flat float teseID;\n" +
                "    flat float teseDistance;\n" +
                "} ttb_datas[];\n" +
                "\n" +
                "out TESE_GEOM_BLOCK {\n" +
                "    flat mat4 geomMatrix;\n" +
                "    flat vec2 geomNormal;\n" +
                "    flat vec4 geomColor;\n" +
                "    flat vec4 geomEmissiveColor;\n" +
                "    flat float geomWidth;\n" +
                "    flat float geomUV;\n" +
                "} tgb_data;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    bool directCheck = statePackage[CURVE_STATE].x <= 1.0;\n" +
                "    float factor1 = gl_TessCoord.x;\n" +
                "    float factor1P2 = factor1 + factor1;\n" +
                "    float factor2 = factor1 * factor1;\n" +
                "    float factor2P2 = factor2 + factor2;\n" +
                "    float factor3 = factor2 * factor1;\n" +
                "    float mixFactor = pow(factor1, ttb_datas[0].teseMixFactor);\n" +
                "    float oneMinusF1 = 1.0 - factor1;\n" +
                "    float oneMinusF1M2 = oneMinusF1 * oneMinusF1;\n" +
                "    vec4 midPoints = vec4(ttb_datas[0].tesePoints.zw, ttb_datas[1].tesePoints.xy) * 3.0;\n" +
                "    vec2 tmp0 = oneMinusF1M2 * oneMinusF1 * gl_in[0].gl_Position.xy;\n" +
                "    vec2 tmp1 = (factor1 - factor2P2 + factor3) * midPoints.xy;\n" +
                "    vec2 tmp2 = (factor2 - factor3) * midPoints.zw;\n" +
                "    vec2 tmp3 = factor3 * gl_in[1].gl_Position.xy;\n" +
                "\n" +
                "    vec2 tmpT0 = oneMinusF1M2 * gl_in[0].gl_Position.xy * - 3.0;\n" +
                "    vec2 tmpT1 = (oneMinusF1M2 - factor1P2 + factor2P2) * midPoints.xy;\n" +
                "    vec2 tmpT2 = (factor1P2 - factor2 - factor2P2) * midPoints.zw;\n" +
                "    vec2 tmpT3 = factor2 * gl_in[1].gl_Position.xy * 3.0;\n" +
                "\n" +
                "    bool useGlobal = statePackage[CURVE_STATE].z > 0.0;\n" +
                "    vec2 ids = vec2(ttb_datas[0].teseID, ttb_datas[1].teseID);\n" +
                "    vec2 uv = useGlobal ? (statePackage[CURVE_STATE].z * ids) : (vec2(ttb_datas[0].teseDistance, ttb_datas[1].teseDistance) / statePackage[CURVE_STATE].y);\n" +
                "    tgb_data.geomMatrix = ttb_datas[0].teseMatrix;\n" +
                "    vec2 currentTangent = directCheck ? (gl_in[1].gl_Position.xy - gl_in[0].gl_Position.xy) : vec2(tmpT0 + tmpT1 + tmpT2 + tmpT3);\n" +
                "    tgb_data.geomNormal = normalize(vec2(-currentTangent.y, currentTangent.x));\n" +
                "    ids /= totalNodes;\n" +
                "    float fillUV = mix(ids.x, ids.y, factor1);\n" +
                "    vec2 fillMix = smoothstep(statePackage[CURVE_FILL].zw, vec2(1.0), vec2(1.0 - fillUV, fillUV)) * (1.0 - statePackage[CURVE_FILL].xy);\n" +
                "    fillMix = 1.0 - fillMix;\n" +
                "    float fillFactor = clamp(fillMix.x * fillMix.y, 0.0, 1.0);\n" +
                "    vec4 color = mix(ttb_datas[0].teseColor, ttb_datas[1].teseColor, mixFactor);\n" +
                "    color.w *= fillFactor;\n" +
                "    tgb_data.geomColor = color;\n" +
                "    vec4 emissive = mix(ttb_datas[0].teseEmissiveColor, ttb_datas[1].teseEmissiveColor, mixFactor);\n" +
                "    emissive.w *= fillFactor;\n" +
                "    tgb_data.geomEmissiveColor = emissive;\n" +
                "    tgb_data.geomWidth = mix(ttb_datas[0].teseWidth, ttb_datas[1].teseWidth, mixFactor);\n" +
                "    tgb_data.geomUV = mix(uv.x, uv.y, factor1) - statePackage[CURVE_STATE].w;\n" +
                "    gl_Position = directCheck ? gl_in[uint(factor1)].gl_Position : vec4(tmp0 + tmp1 + tmp2 + tmp3, 0.0, 1.0);\n" +
                "}\n";

        public final static String GEOM = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define CURVE_STATE 3\n" +
                "\n" +
                "layout (lines) in;\n" +
                "layout (triangle_strip, max_vertices = 4) out;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, globalUV, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "\n" +
                "in TESE_GEOM_BLOCK {\n" +
                "\tflat mat4 geomMatrix;\n" +
                "\tflat vec2 geomNormal;\n" +
                "\tflat vec4 geomColor;\n" +
                "\tflat vec4 geomEmissiveColor;\n" +
                "\tflat float geomWidth;\n" +
                "\tflat float geomUV;\n" +
                "} tgb_datas[];\n" +
                "\n" +
                "out GEOM_FRAG_BLOCK {\n" +
                "\tvec2 fragUV;\n" +
                "\tvec4 fragEntityColor;\n" +
                "\tvec4 fragMixEmissive;\n" +
                "} gfb_data;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tfloat time = statePackage[CURVE_STATE].w;\n" +
                "\tmat4 matrix = tgb_datas[0].geomMatrix;\n" +
                "\tvec4 startOffset = vec4(tgb_datas[0].geomNormal * tgb_datas[0].geomWidth, 0.0, 0.0);\n" +
                "\tvec4 endOffset = vec4(tgb_datas[1].geomNormal * tgb_datas[1].geomWidth, 0.0, 0.0);\n" +
                "\n" +
                "\tif (max(tgb_datas[0].geomColor.w, tgb_datas[0].geomEmissiveColor.w) <= 0.0 && max(tgb_datas[1].geomColor.w, tgb_datas[1].geomEmissiveColor.w) <= 0.0) {\n" +
                "\t\tmatrix = mat4(0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, 1.0);\n" +
                "\t\tstartOffset = endOffset = vec4(0.0);\n" +
                "\t}\n" +
                "\tgfb_data.fragEntityColor = tgb_datas[0].geomColor;\n" +
                "\tgfb_data.fragMixEmissive = tgb_datas[0].geomEmissiveColor;\n" +
                "\tgfb_data.fragUV = vec2(tgb_datas[0].geomUV, 1.0);\n" +
                "\tgl_Position = matrix * (gl_in[0].gl_Position + startOffset);\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragUV = vec2(tgb_datas[0].geomUV, 0.0);\n" +
                "\tgl_Position = matrix * (gl_in[0].gl_Position - startOffset);\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragEntityColor = tgb_datas[1].geomColor;\n" +
                "\tgfb_data.fragMixEmissive = tgb_datas[1].geomEmissiveColor;\n" +
                "\tgfb_data.fragUV = vec2(tgb_datas[1].geomUV, 1.0);\n" +
                "\tgl_Position = matrix * (gl_in[1].gl_Position + endOffset);\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragUV = vec2(tgb_datas[1].geomUV, 0.0);\n" +
                "\tgl_Position = matrix * (gl_in[1].gl_Position - endOffset);\n" +
                "\tEmitVertex();\n" +
                "\tEndPrimitive();\n" +
                "}\n";
        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define ALPHA_THRESHOLD 0.003\n" +
                "\n" +
                "in GEOM_FRAG_BLOCK {\n" +
                "    vec2 fragUV;\n" +
                "    vec4 fragEntityColor;\n" +
                "    vec4 fragMixEmissive;\n" +
                "} gfb_data;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, globalUV, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "uniform int additionEmissive;\n" +
                "// diffuse, normal, ao, emissive\n" +
                "layout (binding = 0) uniform sampler2D diffuseMap;\n" +
                "layout (binding = 2) uniform sampler2D aoMap;\n" +
                "layout (binding = 3) uniform sampler2D emissiveMap;\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)\n" +
                "layout (location = 2) out vec4 fragEmissiveColor;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec4 diffuse = texture(diffuseMap, gfb_data.fragUV);\n" +
                "    vec4 emissive = texture(emissiveMap, gfb_data.fragUV) * gfb_data.fragMixEmissive;\n" +
                "    diffuse *= gfb_data.fragEntityColor;\n" +
                "    if (diffuse.w + emissive.w <= ALPHA_THRESHOLD) discard;\n" +
                "    diffuse.xyz *= texture(aoMap, gfb_data.fragUV).x;\n" +
                "\n" +
                "    fragColor = additionEmissive == 1 ? (diffuse + emissive * emissive.w) : mix(diffuse, emissive, emissive.w);\n" +
                "    fragCombineData = vec4(statePackage[EMISSIVE_SA].z * emissive.w, gl_FragCoord.z, statePackage[EMISSIVE_SA].z, max(diffuse.w, emissive.w));\n" +
                "    emissive.w *= statePackage[EMISSIVE_SA].z;\n" +
                "    fragEmissiveColor = emissive;\n" +
                "}\n";
    }

    public final static class Segment {
        private Segment() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define COLOR 0\n" +
                "#define EMISSIVE_COLOR 1\n" +
                "#define EMISSIVE_SA 2\n" +
                "\n" +
                "layout (location = 0) in vec2 disabled;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "\n" +
                "// vec4*3(final), vec4(state0), vec4(color)\n" +
                "layout (binding = 10) uniform samplerBuffer stateMap;\n" +
                "layout (binding = 11) uniform samplerBuffer mixFactorMap;\n" +
                "layout (binding = 12) uniform samplerBuffer colorMap;\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, reversed, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "\n" +
                "out VERT_TESC_BLOCK {\n" +
                "\tflat mat4 tescMatrix;\n" +
                "\tflat vec4 tescPoints;\n" +
                "\tflat vec4 tescColor;\n" +
                "\tflat vec4 tescEmissiveColor;\n" +
                "\tflat float tescWidth;\n" +
                "\tflat float tescMixFactor;\n" +
                "\tflat float tescDistance;\n" +
                "} vtb_data;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tint indexA = gl_InstanceID * 4 + gl_VertexID * 2;\n" +
                "\tint indexB = indexA + 1;\n" +
                "\tvec4 vertexA = texelFetch(stateMap, indexA); // vec4(vec2(loc), vec2(tangent left)),\n" +
                "\tvec4 vertexB = texelFetch(stateMap, indexB); // vec4(vec2(tangent right), width, distance)\n" +
                "\tvec4 entityColor = statePackage[COLOR] * texelFetch(colorMap, indexA) * statePackage[EMISSIVE_SA].w;\n" +
                "\tvec4 entityEmissiveColor = statePackage[EMISSIVE_COLOR] * texelFetch(colorMap, indexB) * statePackage[EMISSIVE_SA].w;\n" +
                "\n" +
                "\tvtb_data.tescMatrix = gameViewport * modelMatrix;\n" +
                "\tvtb_data.tescPoints = vertexA.xy.xyxy + vec4(vertexA.zw, vertexB.xy);\n" +
                "\tvtb_data.tescColor = entityColor;\n" +
                "\tvtb_data.tescEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));\n" +
                "\tvtb_data.tescWidth = vertexB.z;\n" +
                "\tvtb_data.tescMixFactor = texelFetch(mixFactorMap, gl_InstanceID * 2 + gl_VertexID).x;\n" +
                "\tvtb_data.tescDistance = vertexB.w;\n" +
                "\tgl_Position = vec4(vertexA.xy, 0.0, 1.0);\n" +
                "}\n";

        public final static String TESC = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define CURVE_STATE 3\n" +
                "\n" +
                "layout(vertices = 2) out;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, reversed, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "\n" +
                "in VERT_TESC_BLOCK {\n" +
                "    flat mat4 tescMatrix;\n" +
                "    flat vec4 tescPoints;\n" +
                "    flat vec4 tescColor;\n" +
                "    flat vec4 tescEmissiveColor;\n" +
                "    flat float tescWidth;\n" +
                "    flat float tescMixFactor;\n" +
                "    flat float tescDistance;\n" +
                "} vtb_datas[];\n" +
                "\n" +
                "out TESC_TESE_BLOCK {\n" +
                "    flat mat4 teseMatrix;\n" +
                "    flat vec4 tesePoints;\n" +
                "    flat vec4 teseColor;\n" +
                "    flat vec4 teseEmissiveColor;\n" +
                "    flat float teseWidth;\n" +
                "    flat float teseMixFactor;\n" +
                "    flat float teseDistance;\n" +
                "} ttb_datas[];\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ttb_datas[gl_InvocationID].teseMatrix = vtb_datas[gl_InvocationID].tescMatrix;\n" +
                "    ttb_datas[gl_InvocationID].tesePoints = vtb_datas[gl_InvocationID].tescPoints;\n" +
                "    ttb_datas[gl_InvocationID].teseColor = vtb_datas[gl_InvocationID].tescColor;\n" +
                "    ttb_datas[gl_InvocationID].teseEmissiveColor = vtb_datas[gl_InvocationID].tescEmissiveColor;\n" +
                "    ttb_datas[gl_InvocationID].teseWidth = vtb_datas[gl_InvocationID].tescWidth;\n" +
                "    ttb_datas[gl_InvocationID].teseMixFactor = vtb_datas[gl_InvocationID].tescMixFactor;\n" +
                "    ttb_datas[gl_InvocationID].teseDistance = vtb_datas[gl_InvocationID].tescDistance;\n" +
                "    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;\n" +
                "    if (gl_InvocationID == 0) {\n" +
                "        gl_TessLevelOuter[0] = 1.0;\n" +
                "        gl_TessLevelOuter[1] = statePackage[CURVE_STATE].x;\n" +
                "    }\n" +
                "}\n";

        public final static String TESE = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define CURVE_STATE 3\n" +
                "#define CURVE_FILL 4\n" +
                "\n" +
                "layout(isolines, equal_spacing, ccw) in;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(interpolationFloat + 1, texturePixels, reversed, time), vec4(fillStart, fillEnd, startFactor, endFactor)\n" +
                "uniform vec4 statePackage[5];\n" +
                "\n" +
                "in TESC_TESE_BLOCK {\n" +
                "    flat mat4 teseMatrix;\n" +
                "    flat vec4 tesePoints;\n" +
                "    flat vec4 teseColor;\n" +
                "    flat vec4 teseEmissiveColor;\n" +
                "    flat float teseWidth;\n" +
                "    flat float teseMixFactor;\n" +
                "    flat float teseDistance;\n" +
                "} ttb_datas[];\n" +
                "\n" +
                "out TESE_GEOM_BLOCK {\n" +
                "    flat mat4 geomMatrix;\n" +
                "    flat vec2 geomNormal;\n" +
                "    flat vec4 geomColor;\n" +
                "    flat vec4 geomEmissiveColor;\n" +
                "    flat float geomWidth;\n" +
                "    flat float geomUV;\n" +
                "} tgb_data;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    bool directCheck = statePackage[CURVE_STATE].x <= 1.0;\n" +
                "    float factor1 = gl_TessCoord.x;\n" +
                "    float factor1P2 = factor1 + factor1;\n" +
                "    float factor2 = factor1 * factor1;\n" +
                "    float factor2P2 = factor2 + factor2;\n" +
                "    float factor3 = factor2 * factor1;\n" +
                "    float mixFactor = pow(factor1, ttb_datas[0].teseMixFactor);\n" +
                "    float oneMinusF1 = 1.0 - factor1;\n" +
                "    float oneMinusF1M2 = oneMinusF1 * oneMinusF1;\n" +
                "    vec4 midPoints = vec4(ttb_datas[0].tesePoints.zw, ttb_datas[1].tesePoints.xy) * 3.0;\n" +
                "    vec2 tmp0 = oneMinusF1M2 * oneMinusF1 * gl_in[0].gl_Position.xy;\n" +
                "    vec2 tmp1 = (factor1 - factor2P2 + factor3) * midPoints.xy;\n" +
                "    vec2 tmp2 = (factor2 - factor3) * midPoints.zw;\n" +
                "    vec2 tmp3 = factor3 * gl_in[1].gl_Position.xy;\n" +
                "\n" +
                "    vec2 tmpT0 = oneMinusF1M2 * gl_in[0].gl_Position.xy * -3.0;\n" +
                "    vec2 tmpT1 = (oneMinusF1M2 - factor1P2 + factor2P2) * midPoints.xy;\n" +
                "    vec2 tmpT2 = (factor1P2 - factor2 - factor2P2) * midPoints.zw;\n" +
                "    vec2 tmpT3 = factor2 * gl_in[1].gl_Position.xy * 3.0;\n" +
                "\n" +
                "    vec2 uv = vec2(ttb_datas[0].teseDistance, ttb_datas[1].teseDistance) / statePackage[CURVE_STATE].y;\n" +
                "    tgb_data.geomMatrix = ttb_datas[0].teseMatrix;\n" +
                "    vec2 currentTangent = directCheck ? (gl_in[1].gl_Position.xy - gl_in[0].gl_Position.xy) : vec2(tmpT0 + tmpT1 + tmpT2 + tmpT3);\n" +
                "    tgb_data.geomNormal = normalize(vec2(-currentTangent.y, currentTangent.x));\n" +
                "    vec2 fillMix = smoothstep(statePackage[CURVE_FILL].zw, vec2(1.0), vec2(1.0 - factor1, factor1)) * (1.0 - statePackage[CURVE_FILL].xy);\n" +
                "    fillMix = 1.0 - fillMix;\n" +
                "    float fillFactor = clamp(fillMix.x * fillMix.y, 0.0, 1.0);\n" +
                "    vec4 color = mix(ttb_datas[0].teseColor, ttb_datas[1].teseColor, mixFactor);\n" +
                "    color.w *= fillFactor;\n" +
                "    tgb_data.geomColor = color;\n" +
                "    vec4 emissive = mix(ttb_datas[0].teseEmissiveColor, ttb_datas[1].teseEmissiveColor, mixFactor);\n" +
                "    emissive.w *= fillFactor;\n" +
                "    tgb_data.geomEmissiveColor = emissive;\n" +
                "    tgb_data.geomWidth = mix(ttb_datas[0].teseWidth, ttb_datas[1].teseWidth, mixFactor);\n" +
                "    tgb_data.geomUV = mix(uv.x, uv.y, factor1) - statePackage[CURVE_STATE].w;\n" +
                "    gl_Position = directCheck ? gl_in[uint(factor1)].gl_Position : vec4(tmp0 + tmp1 + tmp2 + tmp3, 0.0, 1.0);\n" +
                "}\n";
    }

    public final static class Trail {
        private Trail() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define COLOR 0\n" +
                "#define EMISSIVE_COLOR 1\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define COM_STATE 3\n" +
                "#define FILL_DATA 4\n" +
                "#define WIDTH_DATA 5\n" +
                "#define START_COLOR 6\n" +
                "#define END_COLOR 7\n" +
                "#define START_EMISSIVE 8\n" +
                "#define END_EMISSIVE 9\n" +
                "\n" +
                "layout (location = 0) in vec2 disabled;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "\n" +
                "// vec2(point)\n" +
                "layout (binding = 10) uniform samplerBuffer nodeMap;\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(texturePixels, time, nodeCount, mixFactor), vec4(fillStart, fillEnd, startFactor, endFactor), vec4(startWidth, endWidth, jitterPower, flickerMix)\n" +
                "// 6: vec4(start), 7: vec4(end), 8: vec4(startEmissive), 9: vec4(endEmissive)\n" +
                "uniform vec4 statePackage[10];\n" +
                "// hashCode, hashCodeTime\n" +
                "uniform vec2 extraData;\n" +
                "\n" +
                "out VERT_GEOM_BLOCK {\n" +
                "\tflat mat4 geomMatrix;\n" +
                "\tflat vec4 geomColor;\n" +
                "\tflat vec4 geomEmissiveColor;\n" +
                "\tflat vec4 geomNormalWidthDistance;\n" +
                "\tflat float geomFactor;\n" +
                "} vgb_data;\n" +
                "\n" +
                "float flickRandom(in float seed) {\n" +
                "\tvec2 tanSeed = tan(vec2(seed) * vec2(0.42, 4.2) + vec2(12.7, 51.97));\n" +
                "\tfloat result = fract(smoothstep(1.0, 0.0, sin(tanSeed.x) + abs(tanSeed.y)) * 2.0);\n" +
                "\treturn 1.0 - sqrt(result);\n" +
                "}\n" +
                "\n" +
                "float getFlick() {\n" +
                "\treturn (statePackage[WIDTH_DATA].w <= -1.0f) ? 1.0 : mix(1.0, flickRandom(extraData.y), statePackage[WIDTH_DATA].w);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tint currIndex = gl_InstanceID + gl_VertexID;\n" +
                "\tfloat factor = clamp(float(currIndex) / statePackage[COM_STATE].z, 0.0, 1.0);\n" +
                "\tfloat factorPow = pow(factor, statePackage[COM_STATE].w);\n" +
                "\tint leftIndex = max(currIndex - 1, 0);\n" +
                "\tint rightIndex = min(currIndex + 1, int(statePackage[COM_STATE].z));\n" +
                "\tvec3 rawNodeData = texelFetch(nodeMap, currIndex).xyz;\n" +
                "\tvec2 leftTangent = rawNodeData.xy - texelFetch(nodeMap, leftIndex).xy;\n" +
                "\tif (leftIndex != currIndex) {\n" +
                "\t\tleftTangent = normalize(leftTangent);\n" +
                "\t\tleftTangent = vec2(-leftTangent.y, leftTangent.x);\n" +
                "\t} else leftTangent = vec2(0.0);\n" +
                "\tvec2 rightTangent = texelFetch(nodeMap, rightIndex).xy - rawNodeData.xy;\n" +
                "\tif (rightIndex != currIndex) {\n" +
                "\t\trightTangent = normalize(rightTangent);\n" +
                "\t\trightTangent = vec2(-rightTangent.y, rightTangent.x);\n" +
                "\t} else rightTangent = vec2(0.0);\n" +
                "\tvec2 currNormal = normalize(leftTangent + rightTangent);\n" +
                "\n" +
                "\tfloat flicker = getFlick();\n" +
                "\tvec4 entityColor = mix(statePackage[START_COLOR], statePackage[END_COLOR], factorPow) * statePackage[COLOR] * statePackage[EMISSIVE_SA].w * flicker;\n" +
                "\tvec4 entityEmissiveColor = mix(statePackage[START_EMISSIVE], statePackage[END_EMISSIVE], factorPow) * statePackage[EMISSIVE_COLOR] * statePackage[EMISSIVE_SA].w * flicker;\n" +
                "\n" +
                "\tvgb_data.geomMatrix = gameViewport * modelMatrix;\n" +
                "\tvgb_data.geomColor = entityColor;\n" +
                "\tvgb_data.geomEmissiveColor = mix(entityEmissiveColor, entityEmissiveColor * entityColor, vec4(vec3(statePackage[EMISSIVE_SA].y), statePackage[EMISSIVE_SA].x));\n" +
                "\tvgb_data.geomNormalWidthDistance = vec4(currNormal, mix(statePackage[WIDTH_DATA].x, statePackage[WIDTH_DATA].y, factorPow), rawNodeData.z);\n" +
                "\tvgb_data.geomFactor = factor;\n" +
                "\tgl_Position = vec4(rawNodeData.xy, 0.0, 1.0);\n" +
                "}\n";

        public final static String GEOM = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define COM_STATE 3\n" +
                "\n" +
                "layout (lines) in;\n" +
                "layout (triangle_strip, max_vertices = 4) out;\n" +
                "\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(texturePixels, time, nodeCount, mixFactor), vec4(fillStart, fillEnd, startFactor, endFactor), vec4(startWidth, endWidth, jitterPower, flickerMix)\n" +
                "// 6: vec4(start), 7: vec4(end), 8: vec4(startEmissive), 9: vec4(endEmissive)\n" +
                "uniform vec4 statePackage[10];\n" +
                "\n" +
                "in VERT_GEOM_BLOCK {\n" +
                "\tflat mat4 geomMatrix;\n" +
                "\tflat vec4 geomColor;\n" +
                "\tflat vec4 geomEmissiveColor;\n" +
                "\tflat vec4 geomNormalWidthDistance;\n" +
                "\tflat float geomFactor;\n" +
                "} vgb_datas[];\n" +
                "\n" +
                "out GEOM_FRAG_BLOCK {\n" +
                "\tvec4 fragUVSeedFactor;\n" +
                "\tvec4 fragEntityColor;\n" +
                "\tvec4 fragMixEmissive;\n" +
                "} gfb_data;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tfloat time = statePackage[COM_STATE].y;\n" +
                "\tmat4 matrix = vgb_datas[0].geomMatrix;\n" +
                "\tvec4 startOffset = vec4(vgb_datas[0].geomNormalWidthDistance.xy * vgb_datas[0].geomNormalWidthDistance.z, 0.0, 0.0);\n" +
                "\tvec4 endOffset = vec4(vgb_datas[1].geomNormalWidthDistance.xy * vgb_datas[1].geomNormalWidthDistance.z, 0.0, 0.0);\n" +
                "\tvec2 uv = (vec2(vgb_datas[0].geomNormalWidthDistance.w, vgb_datas[1].geomNormalWidthDistance.w) / statePackage[COM_STATE].x) - statePackage[COM_STATE].y;\n" +
                "\tfloat seed;\n" +
                "\n" +
                "\tif (max(vgb_datas[0].geomColor.w, vgb_datas[0].geomEmissiveColor.w) <= 0.0 && max(vgb_datas[1].geomColor.w, vgb_datas[1].geomEmissiveColor.w) <= 0.0) {\n" +
                "\t\tmatrix = mat4(0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, -65536.0, 0.0, 0.0, 0.0, 1.0);\n" +
                "\t\tstartOffset = endOffset = vec4(0.0);\n" +
                "\t}\n" +
                "\tseed = fract((gl_in[0].gl_Position.x + gl_in[0].gl_Position.y) * 0.255) + 127.0;\n" +
                "\tseed *= 640.0;\n" +
                "\tgfb_data.fragEntityColor = vgb_datas[0].geomColor;\n" +
                "\tgfb_data.fragMixEmissive = vgb_datas[0].geomEmissiveColor;\n" +
                "\tgfb_data.fragUVSeedFactor = vec4(uv.x, 1.0, seed, vgb_datas[0].geomFactor);\n" +
                "\tgl_Position = matrix * (gl_in[0].gl_Position + startOffset);\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragUVSeedFactor = vec4(uv.x, 0.0, seed, vgb_datas[0].geomFactor);\n" +
                "\tgl_Position = matrix * (gl_in[0].gl_Position - startOffset);\n" +
                "\tEmitVertex();\n" +
                "\tseed = fract((gl_in[1].gl_Position.x + gl_in[1].gl_Position.y) * 0.255) + 127.0;\n" +
                "\tseed *= 640.0;\n" +
                "\tgfb_data.fragEntityColor = vgb_datas[1].geomColor;\n" +
                "\tgfb_data.fragMixEmissive = vgb_datas[1].geomEmissiveColor;\n" +
                "\tgfb_data.fragUVSeedFactor = vec4(uv.y, 1.0, seed, vgb_datas[1].geomFactor);\n" +
                "\tgl_Position = matrix * (gl_in[1].gl_Position + endOffset);\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragUVSeedFactor = vec4(uv.y, 0.0, seed, vgb_datas[1].geomFactor);\n" +
                "\tgl_Position = matrix * (gl_in[1].gl_Position - endOffset);\n" +
                "\tEmitVertex();\n" +
                "\tEndPrimitive();\n" +
                "}\n";

        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define EMISSIVE_SA 2\n" +
                "#define FILL_DATA 4\n" +
                "#define WIDTH_DATA 5\n" +
                "#define ALPHA_THRESHOLD 0.003\n" +
                "\n" +
                "in GEOM_FRAG_BLOCK {\n" +
                "    vec4 fragUVSeedFactor;\n" +
                "    vec4 fragEntityColor;\n" +
                "    vec4 fragMixEmissive;\n" +
                "} gfb_data;\n" +
                "\n" +
                "// vec4(color), vec4(emissiveColor), vec4(emissiveState, timerAlpha), vec4(texturePixels, time, nodeCount, mixFactor), vec4(fillStart, fillEnd, startFactor, endFactor), vec4(startWidth, endWidth, jitterPower, flickerMix)\n" +
                "// 6: vec4(start), 7: vec4(end), 8: vec4(startEmissive), 9: vec4(endEmissive)\n" +
                "uniform vec4 statePackage[10];\n" +
                "// hashCode, hashCodeTime\n" +
                "uniform vec2 extraData;\n" +
                "uniform int additionEmissive;\n" +
                "// diffuse, normal, ao, emissive\n" +
                "layout (binding = 0) uniform sampler2D diffuseMap;\n" +
                "layout (binding = 2) uniform sampler2D aoMap;\n" +
                "layout (binding = 3) uniform sampler2D emissiveMap;\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)\n" +
                "layout (location = 2) out vec4 fragEmissiveColor;\n" +
                "\n" +
                "float hash12(vec2 p)\n" +
                "{\n" +
                "    vec3 p3 = fract(vec3(p.xyx) * 0.1031);\n" +
                "    p3 += dot(p3, p3.yzx + 33.33);\n" +
                "    return fract((p3.x + p3.y) * p3.z);\n" +
                "}\n" +
                "\n" +
                "float getJitter(in float uv, in float seed) {\n" +
                "    float uvO = hash12(vec2(round(seed) + 255.0, extraData.x)) * 2.0 - 1.0;\n" +
                "    float posFactor = step(0.0, uvO);\n" +
                "\n" +
                "    float result = uv - posFactor;\n" +
                "    uvO = abs(uvO) + 1.0;\n" +
                "    result = mix(result, result * uvO, statePackage[WIDTH_DATA].z) + posFactor;\n" +
                "    result = clamp(result, 0.0, 1.0);\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec2 uv = gfb_data.fragUVSeedFactor.xy;\n" +
                "    uv.y = getJitter(uv.y, gfb_data.fragUVSeedFactor.z);\n" +
                "    vec2 fillMix = smoothstep(statePackage[FILL_DATA].zw, vec2(1.0), vec2(1.0 - gfb_data.fragUVSeedFactor.w, gfb_data.fragUVSeedFactor.w)) * (1.0 - statePackage[FILL_DATA].xy);\n" +
                "    fillMix = 1.0 - fillMix;\n" +
                "    float fillFactor = clamp(fillMix.x * fillMix.y, 0.0, 1.0);\n" +
                "\n" +
                "    vec4 diffuse = texture(diffuseMap, uv) * gfb_data.fragEntityColor;\n" +
                "    vec4 emissive = texture(emissiveMap, uv) * gfb_data.fragMixEmissive;\n" +
                "    diffuse.w *= fillFactor;\n" +
                "    emissive.w *= fillFactor;\n" +
                "    if (diffuse.w + emissive.w <= ALPHA_THRESHOLD) discard;\n" +
                "    diffuse.xyz *= texture(aoMap, uv).x;\n" +
                "\n" +
                "    fragColor = additionEmissive == 1 ? (diffuse + emissive * emissive.w) : mix(diffuse, emissive, emissive.w);\n" +
                "    fragCombineData = vec4(statePackage[EMISSIVE_SA].z * emissive.w, gl_FragCoord.z, statePackage[EMISSIVE_SA].z, max(diffuse.w, emissive.w));\n" +
                "    emissive.w *= statePackage[EMISSIVE_SA].z;\n" +
                "    fragEmissiveColor = emissive;\n" +
                "}\n";
    }

    public final static class Flare {
        private Flare() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define FRINGE_COLOR 0\n" +
                "#define CORE_COLOR 1\n" +
                "#define STATE_A 2\n" +
                "#define STATE_B 3\n" +
                "#define STATE_EXT 4\n" +
                "\n" +
                "subroutine void instanceStateCompute(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha);\n" +
                "subroutine uniform instanceStateCompute instanceState;\n" +
                "\n" +
                "layout (location = 0) in vec2 vertex;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "\n" +
                "// vec4*3(final), vec4(state0), vec4(color)\n" +
                "layout (binding = 10) uniform samplerBuffer dataPackage_Final0;\n" +
                "layout (binding = 11) uniform samplerBuffer dataPackage_Final1;\n" +
                "layout (binding = 12) uniform samplerBuffer dataPackage_Final2;\n" +
                "layout (binding = 13) uniform samplerBuffer dataPackage_State0;\n" +
                "layout (binding = 14) uniform usamplerBuffer dataPackage_Color;\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(fringeColor), vec4(coreColor), vec4(size, aspect, flick/syncFlick), vec4(alpha, hashCode, glowPower, frameAmount), vec4(noisePower, flickMix, globalAlpha, discRatio)\n" +
                "uniform vec4 statePackage[5];\n" +
                "uniform float instanceDataExtra;\n" +
                "\n" +
                "out VERTEX_BLOCK {\n" +
                "\tvec2 fragUV;\n" +
                "\tflat vec4 fragFringeColor;\n" +
                "\tflat vec4 fragCoreColor;\n" +
                "\tflat vec2 fragNoiseOffsetAlpha;\n" +
                "} vb_data;\n" +
                "\n" +
                "float flickRandom(in float seed) {\n" +
                "\tvec2 tanSeed = tan(vec2(seed) * vec2(0.42, 4.2) + vec2(12.7, 51.97));\n" +
                "\tfloat result = fract(smoothstep(1.0, 0.0, sin(tanSeed.x) + abs(tanSeed.y)) * 2.0);\n" +
                "\treturn 1.0 - sqrt(result);\n" +
                "}\n" +
                "\n" +
                "float getFlick() {\n" +
                "\tuint flickState = uint(statePackage[STATE_A].w);\n" +
                "\tfloat flickOffset = ((flickState & 1u) == 1u) ? statePackage[STATE_B].y : (float(gl_InstanceID << 2) - statePackage[STATE_B].y) * 0.01;\n" +
                "\treturn (flickState > 2u) ? mix(1.0, flickRandom(flickOffset + statePackage[STATE_B].w), statePackage[STATE_EXT].y) : 1.0;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void noneData(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {\n" +
                "\tmodel = modelMatrix;\n" +
                "\tfloat flick = getFlick();\n" +
                "\tvec4 resultF = statePackage[FRINGE_COLOR];\n" +
                "\tvec4 resultC = statePackage[CORE_COLOR];\n" +
                "\tmFColor = resultF;\n" +
                "\tmCColor = resultC;;\n" +
                "\tcurrAlpha = statePackage[STATE_B].x * flick;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData2D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final1.w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tfinal1.w = final0.z;\n" +
                "\tfinal0.z = 0.0;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, vec4(0.0, 0.0, 1.0, 0.0), vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\t\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tmCColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[CORE_COLOR];\n" +
                "\tmFColor = mix(colorMat[2], colorMat[3], alpha) * statePackage[FRINGE_COLOR];\n" +
                "\tcurrAlpha = alpha * getFlick();;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec2 final1 = texelFetch(dataPackage_Final1, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final0.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tfloat pryFacing = radians(final0.y * 0.5);\n" +
                "\tfloat pryCos = cos(pryFacing);\n" +
                "\tfloat prySin = sin(pryFacing);\n" +
                "\tfloat dqz = prySin + prySin;\n" +
                "\tfloat q22 = dqz * prySin;\n" +
                "\tfloat q23 = dqz * pryCos;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - q22 * final1.x, q23 * final1.x, 0.0, 0.0),\n" +
                "\tvec4(-q23 * final1.y, final1.y - q22 * final1.y, 0.0, 0.0),\n" +
                "\tvec4(0.0, 0.0, 1.0, 0.0),\n" +
                "\tvec4(final0.zw, 0.0, 1.0));\n" +
                "\t\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tmCColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[CORE_COLOR];\n" +
                "\tmFColor = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[FRINGE_COLOR];\n" +
                "\tcurrAlpha = alpha * getFlick();;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData3D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec4 final2 = texelFetch(dataPackage_Final2, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : texelFetch(dataPackage_State0, gl_InstanceID).w;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\tvec2 tmpZY = final0.zy;\n" +
                "\tfinal0.yz = vec2(final1.w, final2.w);\n" +
                "\tfinal1.w = tmpZY.x;\n" +
                "\tfinal2.w = tmpZY.y;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, final2, vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tvec4 lowColor = vec4((colorPackage >> 24) & 0xFFu);\n" +
                "\tvec4 highColor = vec4((colorPackage >> 16) & 0xFFu);\n" +
                "\tvec4 lowEmissive = vec4((colorPackage >> 8) & 0xFFu);\n" +
                "\tvec4 highEmissive = vec4(colorPackage & 0xFFu);\n" +
                "\tmat4 colorMat = mat4(lowColor, highColor, lowEmissive, highEmissive) * 0.0039215;\n" +
                "\tmCColor = mix(colorMat[0], colorMat[1], alpha) * statePackage[CORE_COLOR];\n" +
                "\tmFColor = mix(colorMat[2], colorMat[3], alpha) * statePackage[FRINGE_COLOR];\n" +
                "\tcurrAlpha = alpha * getFlick();;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out vec4 mCColor, out vec4 mFColor, out float currAlpha) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec2 final2 = texelFetch(dataPackage_Final2, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final2.x;\n" +
                "\talpha = max(alpha - trunc(alpha * 0.1) * 10.0, 0.0);\n" +
                "\n" +
                "\tvec3 pryRotate = radians(final0.xyz * 0.5);\n" +
                "\tvec3 pryCos = cos(pryRotate);\n" +
                "\tvec3 prySin = sin(pryRotate);\n" +
                "\tfloat wq = pryCos.x * pryCos.y * pryCos.z - prySin.x * prySin.y * prySin.z;\n" +
                "\tfloat xq = prySin.x * pryCos.y * pryCos.z - pryCos.x * prySin.y * prySin.z;\n" +
                "\tfloat yq = pryCos.x * prySin.y * pryCos.z + prySin.x * pryCos.y * prySin.z;\n" +
                "\tfloat zq = pryCos.x * pryCos.y * prySin.z + prySin.x * prySin.y * pryCos.z;\n" +
                "\tfloat dqx = xq + xq;\n" +
                "\tfloat dqy = yq + yq;\n" +
                "\tfloat dqz = zq + zq;\n" +
                "\tfloat q00 = dqx * xq;\n" +
                "\tfloat q11 = dqy * yq;\n" +
                "\tfloat q22 = dqz * zq;\n" +
                "\tfloat q01 = dqx * yq;\n" +
                "\tfloat q02 = dqx * zq;\n" +
                "\tfloat q03 = dqx * wq;\n" +
                "\tfloat q12 = dqy * zq;\n" +
                "\tfloat q13 = dqy * wq;\n" +
                "\tfloat q23 = dqz * wq;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - (q11 + q22) * final1.x, (q01 + q23) * final1.x, (q02 - q13) * final1.x, 0.0),\n" +
                "\tvec4((q01 - q23) * final1.y, final1.y - (q22 + q00) * final1.y, (q12 + q03) * final1.y, 0.0),\n" +
                "\tvec4((q02 + q13) * final1.z, (q12 - q03) * final1.z, final1.z - (q11 + q00) * final1.z, 0.0),\n" +
                "\tvec4(final0.w, final1.w, final2.y, 1.0));\n" +
                "\t\n" +
                "\tuvec4 colorPackage = texelFetch(dataPackage_Color, gl_InstanceID);\n" +
                "\tmCColor = vec4((colorPackage >> 8) & 0xFFu) * 0.0039215 * statePackage[CORE_COLOR];\n" +
                "\tmFColor = vec4(colorPackage & 0xFFu) * 0.0039215 * statePackage[FRINGE_COLOR];\n" +
                "\tcurrAlpha = alpha * getFlick();\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tmat4 currentMatrix;\n" +
                "\tvec4 entityFringeColor;\n" +
                "\tvec4 entityCoreColor;\n" +
                "\tfloat entityAlpha;\n" +
                "\tinstanceState(currentMatrix, entityCoreColor, entityFringeColor, entityAlpha);\n" +
                "\n" +
                "\tvb_data.fragUV = vertex;\n" +
                "\tvb_data.fragFringeColor = entityFringeColor;\n" +
                "\tvb_data.fragCoreColor = entityCoreColor;\n" +
                "\tvb_data.fragNoiseOffsetAlpha = vec2(mod(length(vec3(currentMatrix[3].xyz)), 100.0), entityAlpha * statePackage[STATE_EXT].z);\n" +
                "\tvec4 vertexPos = gameViewport * currentMatrix * vec4(vertex * statePackage[STATE_A].xy, 0.0, 1.0);\n" +
                "\tif (max(entityFringeColor.w, entityCoreColor.w) <= 0.0) vertexPos.xyz = vec3(-65536.0);\n" +
                "\tgl_Position = vertexPos;\n" +
                "}\n";

        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define SHARP_EDGE_SMOOTH 0.42\n" +
                "#define DISC_THICKNESS_SCALE 2.0\n" +
                "#define SHARP_DISC_THICKNESS_SCALE 4.0\n" +
                "#define STATE_A 2\n" +
                "#define STATE_B 3\n" +
                "#define STATE_EXT 4\n" +
                "\n" +
                "subroutine vec4 flareStateDraw(in vec2 uv);\n" +
                "subroutine uniform flareStateDraw flareState;\n" +
                "\n" +
                "in VERTEX_BLOCK {\n" +
                "    vec2 fragUV;\n" +
                "    flat vec4 fragFringeColor;\n" +
                "    flat vec4 fragCoreColor;\n" +
                "    flat vec2 fragNoiseOffsetAlpha;\n" +
                "} vb_data;\n" +
                "\n" +
                "// vec4(fringeColor), vec4(coreColor), vec4(size, aspect, flick/syncFlick), vec4(alpha, hashCode, glowPower, frameAmount), vec4(noisePower, flickMix, globalAlpha, discRatio)\n" +
                "uniform vec4 statePackage[5];\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "layout (location = 1) out vec4 fragCombineData; // Brightness, Depth(depth fxaa), GlowPower(glow shader), AlphaFragCheck(emissive mix)\n" +
                "layout (location = 2) out vec4 fragEmissiveColor;\n" +
                "\n" +
                "float hash(in float p) {\n" +
                "    float f = fract(p * 0.011);\n" +
                "    f *= f + 7.5;\n" +
                "    return fract(f * (f + f));\n" +
                "}\n" +
                "\n" +
                "float noise(in float x) {\n" +
                "    float f = fract(x);\n" +
                "    return mix(hash(x), hash(x + 1.0), f * f * (3.0 - 2.0 * f));\n" +
                "}\n" +
                "\n" +
                "float fbm(in float x) {\n" +
                "    float v = 0.0;\n" +
                "    float a = 0.5;\n" +
                "    float f = x;\n" +
                "    for (int i = 0; i < 3; i++) {\n" +
                "        v += a * noise(f);\n" +
                "        f = f * 2.0 + 100.0;\n" +
                "        a *= 0.5;\n" +
                "    }\n" +
                "    return v;\n" +
                "}\n" +
                "\n" +
                "float fi(in float a, in float b) {\n" +
                "    return 1.0 - (1.0 - a) * (1.0 - b);\n" +
                "}\n" +
                "\n" +
                "subroutine(flareStateDraw) vec4 smoothMode(in vec2 uv) {\n" +
                "    float fringe = smoothstep(1.0, 0.0, length(uv)) * 2.0;\n" +
                "    float core = smoothstep(1.0, 0.0, length(uv * 2.0));\n" +
                "    vec4 fringeColor = vb_data.fragFringeColor;\n" +
                "    fringeColor.w *= fringe;\n" +
                "    return mix(fringeColor, vb_data.fragCoreColor, core);\n" +
                "}\n" +
                "\n" +
                "subroutine(flareStateDraw) vec4 sharpMode(in vec2 uv) {\n" +
                "    vec2 uvAbs = abs(uv);\n" +
                "    float fringe = smoothstep(1.0, 0.0, uvAbs.x) * 2.0;\n" +
                "    float core = smoothstep(1.0, 0.0, uvAbs.x * 2.0);\n" +
                "    vec4 fringeColor = vb_data.fragFringeColor;\n" +
                "    fringeColor.w *= fringe;\n" +
                "    vec4 resultColor = mix(fringeColor, vb_data.fragCoreColor, core);\n" +
                "    resultColor.w *= smoothstep(0.5, SHARP_EDGE_SMOOTH, uvAbs.y);\n" +
                "    return resultColor;\n" +
                "}\n" +
                "\n" +
                "subroutine(flareStateDraw) vec4 smoothDiscMode(in vec2 uv) {\n" +
                "    vec2 coreUV = vec2(uv.x * statePackage[STATE_A].z, uv.y);\n" +
                "    vec2 discUV = vec2(uv.x, uv.y * statePackage[STATE_EXT].w * DISC_THICKNESS_SCALE);\n" +
                "    float fringe = fi(smoothstep(1.0, 0.0, length(coreUV)), smoothstep(1.0, 0.0, length(discUV)));\n" +
                "    float core = fi(smoothstep(1.0, 0.0, length(coreUV * 2.0)), smoothstep(1.0, 0.0, length(discUV * 2.0)));\n" +
                "    vec4 fringeColor = vb_data.fragFringeColor;\n" +
                "    fringeColor.w *= fringe;\n" +
                "    return mix(fringeColor, vb_data.fragCoreColor, core);\n" +
                "}\n" +
                "\n" +
                "subroutine(flareStateDraw) vec4 sharpDiscMode(in vec2 uv) {\n" +
                "    vec2 uvAbs = abs(uv);\n" +
                "    vec2 coreUV = vec2(uv.x * statePackage[STATE_A].z, uv.y);\n" +
                "    vec2 discUV = vec2(uvAbs.x, uvAbs.y * statePackage[STATE_EXT].w * SHARP_DISC_THICKNESS_SCALE);\n" +
                "    vec2 discMask = vec2(smoothstep(1.0, 0.0, discUV.x), smoothstep(1.0, 0.0, discUV.x * 2.0)) * smoothstep(0.5, SHARP_EDGE_SMOOTH, discUV.y);\n" +
                "    float fringe = fi(smoothstep(1.0, 0.0, length(coreUV)), discMask.x);\n" +
                "    float core = fi(smoothstep(1.0, 0.0, length(coreUV * 2.0)), discMask.y);\n" +
                "    vec4 fringeColor = vb_data.fragFringeColor;\n" +
                "    fringeColor.w *= fringe;\n" +
                "    return mix(fringeColor, vb_data.fragCoreColor, core);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec2 uv = vb_data.fragUV;\n" +
                "    uv.x *= smoothstep(0.0, 1.0, fbm(uv.y * 0.5 + statePackage[STATE_B].w * 10.0 + vb_data.fragNoiseOffsetAlpha.x)) * statePackage[STATE_EXT].x + 1.0;;\n" +
                "    vec4 finalColor = flareState(uv);\n" +
                "    finalColor.w *= vb_data.fragNoiseOffsetAlpha.y;\n" +
                "    fragColor = finalColor;\n" +
                "    fragCombineData = vec4(statePackage[STATE_B].z * fragColor.w, 0.0, statePackage[STATE_B].z, finalColor.w);\n" +
                "    finalColor.w *= statePackage[STATE_B].z;\n" +
                "    fragEmissiveColor = finalColor;\n" +
                "}\n\n";
    }

    public final static class TextField {
        private TextField() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "layout (location = 0) in vec4 uv; // uvBL, uvTR\n" +
                "layout (location = 1) in vec4 location; // x, y, topStyleUV, bottomStyleUV\n" +
                "layout (location = 2) in float styleF; // invert(1+8) + italic(1+7) + underline(1+6) + strikeout(1+5) + channel(3+2) + handelIndex(2+0); must cast to uint after, fuck glsl, integer type have tons bugs in vertex attributes\n" +
                "layout (location = 3) in vec4 color;\n" +
                "layout (location = 4) in vec2 size; // size, edge\n" +
                "layout (location = 5) in vec2 edge; // edge\n" +
                "layout (location = 6) in float fill;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "uniform vec4 globalColor;\n" +
                "uniform mat4 modelMatrix;\n" +
                "\n" +
                "out VERT_GEOM_BLOCK {\n" +
                "\tmat4 geomMatrix;\n" +
                "\tvec4 geomUV;\n" +
                "\tvec2 geomStyleUV;\n" +
                "\tvec4 geomSize;\n" +
                "\tflat float geomFill;\n" +
                "\n" +
                "\tflat vec4 geomColor;\n" +
                "\tflat uvec4 geomStyleState; // fuck intel\n" +
                "\tflat uvec3 geomState; // cahnnel, texIndex, reserved\n" +
                "} vgb_data;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tvgb_data.geomMatrix = gameViewport * modelMatrix;\n" +
                "\tvgb_data.geomUV = uv;\n" +
                "\tvgb_data.geomStyleUV = location.zw;\n" +
                "\tvgb_data.geomSize = vec4(size, edge);\n" +
                "\tvgb_data.geomFill = fill;\n" +
                "\tvgb_data.geomColor = color * globalColor;\n" +
                "\tuint style = uint(styleF);\n" +
                "\tvgb_data.geomStyleState = uvec4(style >> 8, style >> 7, style >> 6, style >> 5) & 1u;\n" +
                "\tvgb_data.geomState = uvec3(style & 28u, style & 3u, ((uv.x < -500.0) ? 1u : 0u));\n" +
                "\tgl_Position = vec4(location.xy, 0.0, 1.0);\n" +
                "}\n";

        public final static String GEOM = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "layout (points) in;\n" +
                "layout (triangle_strip, max_vertices = 16) out;\n" +
                "\n" +
                "uniform float italicFactor;\n" +
                "\n" +
                "in VERT_GEOM_BLOCK {\n" +
                "\tmat4 geomMatrix;\n" +
                "\tvec4 geomUV;\n" +
                "\tvec2 geomStyleUV;\n" +
                "\tvec4 geomSize;\n" +
                "\tflat float geomFill;\n" +
                "\n" +
                "\tflat vec4 geomColor;\n" +
                "\tflat uvec4 geomStyleState; // fuck intel\n" +
                "\tflat uvec3 geomState; // cahnnel, texIndex, reserved\n" +
                "} vgb_datas[];\n" +
                "\n" +
                "out GEOM_FRAG_BLOCK {\n" +
                "\tvec3 fragUV;\n" +
                "\tflat uint fragIsEdge;\n" +
                "\n" +
                "\tflat vec4 fragColor;\n" +
                "\tflat uvec3 fragStyleState; // fuck intel\n" +
                "\tflat uvec3 fragState; // cahnnel, texIndex, reserved\n" +
                "} gfb_data;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tmat4 matrix = vgb_datas[0].geomMatrix;\n" +
                "\tvec4 size = vgb_datas[0].geomSize;\n" +
                "\tvec4 uv = vgb_datas[0].geomUV;\n" +
                "\tvec2 styleUV = vgb_datas[0].geomStyleUV;\n" +
                "\tfloat fill = vgb_datas[0].geomFill;\n" +
                "\tvec2 basePoint = gl_in[0].gl_Position.xy;\n" +
                "\n" +
                "\tbool isItalic = vgb_datas[0].geomStyleState.y == 1u;\n" +
                "\tvec4 upPoint = vec4(basePoint, 0.0, 1.0);\n" +
                "\tif (isItalic) upPoint.x += italicFactor * (size.y + size.w);\n" +
                "\tvec4 upPointM = matrix * upPoint;\n" +
                "\tvec4 bottomPoint = vec4(basePoint.x, basePoint.y - size.y, 0.0, 1.0);\n" +
                "\tif (isItalic) bottomPoint.x += italicFactor * size.w;\n" +
                "\tvec4 bottomPointM = matrix * bottomPoint;\n" +
                "\n" +
                "\tvec2 upEdgeUpPoint = vec2(basePoint.x, basePoint.y + size.z);\n" +
                "\tvec2 bottomEdgeBottomPoint = vec2(basePoint.x, basePoint.y - size.y - size.w);\n" +
                "\tif (isItalic) upEdgeUpPoint.x += italicFactor * (size.y + size.z + size.w);\n" +
                "\tvec4 topEdgeL = vec4(upEdgeUpPoint, 0.0, 1.0);\n" +
                "\tvec4 topEdgeLM = matrix * topEdgeL;\n" +
                "\tvec4 topEdgeR = matrix * vec4(upEdgeUpPoint.x + size.x, upEdgeUpPoint.y, 0.0, 1.0);\n" +
                "\tvec4 bottomEdgeL = vec4(bottomEdgeBottomPoint, 0.0, 1.0);\n" +
                "\tvec4 bottomEdgeLM = matrix * bottomEdgeL;\n" +
                "\tvec4 bottomEdgeR = matrix * vec4(bottomEdgeBottomPoint.x + size.x, bottomEdgeBottomPoint.y, 0.0, 1.0);\n" +
                "\n" +
                "\tbool setZeroB = vgb_datas[0].geomColor.w > 0.0;\n" +
                "\tbool topValid = (size.z > 0.0) && setZeroB;\n" +
                "\tbool bottomValid = (size.w > 0.0) && setZeroB;\n" +
                "\tbool leftFill = (fill > 0.0) && setZeroB;\n" +
                "\tvec4 fillVertex[] = vec4[](upPoint, upPointM, bottomPoint, bottomPointM);\n" +
                "\tvec2 fillUv = vec2(styleUV.x, styleUV.y);\n" +
                "\tif (topValid) {\n" +
                "\t\tfillVertex[0] = topEdgeL;\n" +
                "\t\tfillVertex[1] = topEdgeLM;\n" +
                "\t\tfillUv.x = 1.0;\n" +
                "\t}\n" +
                "\tif (bottomValid) {\n" +
                "\t\tfillVertex[2] = bottomEdgeL;\n" +
                "\t\tfillVertex[3] = bottomEdgeLM;\n" +
                "\t\tfillUv.y = 0.0;\n" +
                "\t}\n" +
                "\tfillVertex[0].x -= fill;\n" +
                "\tfillVertex[2].x -= fill;\n" +
                "\n" +
                "\tvec4 upPointRight = matrix * vec4(upPoint.x + size.x, upPoint.y, 0.0, 1.0);\n" +
                "\tvec4 bottomPointRight = matrix * vec4(bottomPoint.x + size.x, bottomPoint.y, 0.0, 1.0);\n" +
                "\tif (!setZeroB) upPointM = upPointRight = bottomPointM = bottomPointRight = vec4(vec3(-65536.0), 1.0);\n" +
                "\n" +
                "\tgfb_data.fragColor = vgb_datas[0].geomColor;\n" +
                "\tgfb_data.fragStyleState = uvec3(vgb_datas[0].geomStyleState.x, vgb_datas[0].geomStyleState.z, vgb_datas[0].geomStyleState.w);\n" +
                "\tgfb_data.fragState = vgb_datas[0].geomState;\n" +
                "\n" +
                "\tgfb_data.fragIsEdge = 0u;\n" +
                "\tgfb_data.fragUV = vec3(uv.xy, styleUV.x);\n" +
                "\tgl_Position = upPointM;\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragUV = vec3(uv.zy, styleUV.x);\n" +
                "\tgl_Position = upPointRight;\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragUV = vec3(uv.xw, styleUV.y);\n" +
                "\tgl_Position = bottomPointM;\n" +
                "\tEmitVertex();\n" +
                "\tgfb_data.fragUV = vec3(uv.zw, styleUV.y);\n" +
                "\tgl_Position = bottomPointRight;\n" +
                "\tEmitVertex();\n" +
                "\tEndPrimitive();\n" +
                "\n" +
                "\tgfb_data.fragIsEdge = 1u;\n" +
                "\tif (topValid) { // topEdge\n" +
                "\t\tgfb_data.fragUV = vec3(1.0);\n" +
                "\t\tgl_Position = topEdgeLM;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgl_Position = topEdgeR;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgfb_data.fragUV = vec3(styleUV.x);\n" +
                "\t\tgl_Position = upPointM;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgl_Position = upPointRight;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tEndPrimitive();\n" +
                "\t}\n" +
                "\n" +
                "\tif (bottomValid) { // bottomEdge\n" +
                "\t\tgfb_data.fragUV = vec3(0.0);\n" +
                "\t\tgl_Position = bottomEdgeLM;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgl_Position = bottomEdgeR;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgfb_data.fragUV = vec3(styleUV.y);\n" +
                "\t\tgl_Position = bottomPointM;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgl_Position = bottomPointRight;\n" +
                "\t\tEmitVertex();\n" +
                "\t\tEndPrimitive();\n" +
                "\t}\n" +
                "\n" +
                "\tif (leftFill) {\n" +
                "\t\tgfb_data.fragUV = vec3(fillUv.x);\n" +
                "\t\tgl_Position = matrix * fillVertex[0];\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgl_Position = fillVertex[1];\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgfb_data.fragUV = vec3(fillUv.y);\n" +
                "\t\tgl_Position = matrix * fillVertex[2];\n" +
                "\t\tEmitVertex();\n" +
                "\t\tgl_Position = fillVertex[3];\n" +
                "\t\tEmitVertex();\n" +
                "\t\tEndPrimitive();\n" +
                "\t}\n" +
                "}\n";

        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "uniform sampler2D fontMap[4];\n" +
                "\n" +
                "in GEOM_FRAG_BLOCK {\n" +
                "    vec3 fragUV;\n" +
                "    flat uint fragIsEdge;\n" +
                "\n" +
                "    flat vec4 fragColor;\n" +
                "    flat uvec3 fragStyleState; // fuck intel\n" +
                "    flat uvec3 fragState; // cahnnel, texIndex, reserved\n" +
                "} gfb_data;\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "\n" +
                "float getUnderline(in float uv) {\n" +
                "    return smoothstep(0.05, 0.04, abs(uv - 0.1));\n" +
                "}\n" +
                "\n" +
                "float getStrikeout(in float uv) {\n" +
                "    return smoothstep(0.07, 0.06, abs(uv - 0.5));\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec4 result = texture(fontMap[gfb_data.fragState.y], gfb_data.fragUV.xy);\n" +
                "    if (gfb_data.fragState.x == 4u) result = vec4(result.x);\n" +
                "    if (gfb_data.fragState.x == 8u) result = vec4(result.y);\n" +
                "    if (gfb_data.fragState.x == 12u) result = vec4(result.z);\n" +
                "    if (gfb_data.fragState.x == 16u) result = vec4(result.w);\n" +
                "    if (bool(gfb_data.fragIsEdge)) result = vec4(1.0, 1.0, 1.0, 0.0);\n" +
                "    if (bool(gfb_data.fragStyleState.y)) result = max(result, vec4(getUnderline(gfb_data.fragUV.z)));\n" +
                "    if (bool(gfb_data.fragStyleState.z)) result = max(result, vec4(getStrikeout(gfb_data.fragUV.z)));\n" +
                "    if (bool(gfb_data.fragStyleState.x)) result = vec4(1.0, 1.0, 1.0, 1.0 - result.w);\n" +
                "    result *= gfb_data.fragColor;\n" +
                "    if (bool(gfb_data.fragState.z)) result = vec4(gfb_data.fragColor.xyz, 0.0);\n" +
                "    fragColor = result;\n" +
                "}\n\n";
    }

    public final static class Distortion {
        private Distortion() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define INOUT_FACTOR 2\n" +
                "#define STATE 4\n" +
                "\n" +
                "subroutine void instanceStateCompute(out mat4 model, out uint timerState, out float mixFactor);\n" +
                "subroutine uniform instanceStateCompute instanceState;\n" +
                "\n" +
                "layout (location = 0) in vec2 vertex;\n" +
                "\n" +
                "layout (std140, binding = OVERWRITE_MATRIX_UBO) uniform BUtilGlobalData\n" +
                "{\n" +
                "\tmat4 gameViewport;\n" +
                "\tvec4 gameScreenBorder; // vec4(screenLB, screenSize)\n" +
                "};\n" +
                "\n" +
                "// vec4*3(final), vec4(state0), vec4(color)\n" +
                "layout (binding = 10) uniform samplerBuffer dataPackage_Final0;\n" +
                "layout (binding = 11) uniform samplerBuffer dataPackage_Final1;\n" +
                "layout (binding = 12) uniform samplerBuffer dataPackage_Final2;\n" +
                "layout (binding = 13) uniform samplerBuffer dataPackage_State0;\n" +
                "uniform mat4 modelMatrix;\n" +
                "// vec4(sizeIn, powerIn, powerFull), vec4(sizeFull, powerOut, hardnessRing), vec4(sizeOut, fadeInFactor, fadeOutFactor)\n" +
                "// vec4(sizeInRatio, sizeFullRatio), vec4(sizeOutRatio, hardnessInner, globalTimerRaw), vec4(arcStart, arcEnd, innerCenter)\n" +
                "uniform vec4 statePackage[6];\n" +
                "uniform float instanceDataExtra;\n" +
                "uniform float screenScale;\n" +
                "\n" +
                "out VERTEX_BLOCK {\n" +
                "\tvec4 fragUVMask;\n" +
                "\tvec4 fragUVScreen;\n" +
                "} vb_data;\n" +
                "\n" +
                "vec2 getUV(vec2 location) {\n" +
                "\treturn (location - gameScreenBorder.xy) / gameScreenBorder.zw;\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void noneData(out mat4 model, out uint timerState, out float mixFactor) {\n" +
                "\tmodel = modelMatrix;\n" +
                "\tfloat check = statePackage[STATE].w;\n" +
                "\tvec2 tmp = vec2(1.0);\n" +
                "\tuint tmpU = 2u;\n" +
                "\tif (check > 2.0) {\n" +
                "\t\ttmp = vec2(abs(3.0 - check), statePackage[INOUT_FACTOR].z);\n" +
                "\t\ttmpU = 0u;\n" +
                "\t}\n" +
                "\tif (check <= 1.0 && check > -500.0) {\n" +
                "\t\ttmp = vec2(check, statePackage[INOUT_FACTOR].w);\n" +
                "\t\ttmpU = 1u;\n" +
                "\t}\n" +
                "\ttimerState = tmpU;\n" +
                "\tmixFactor = pow(tmp.x, tmp.y);\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData2D(out mat4 model, out uint timerState, out float mixFactor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final1.w;\n" +
                "\tfinal1.w = final0.z;\n" +
                "\tfinal0.z = 0.0;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, vec4(0.0, 0.0, 1.0, 0.0), vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tfloat ft = trunc(alpha * 0.1);\n" +
                "\tuint ut =  uint(ft);\n" +
                "\ttimerState = ut;\n" +
                "\tfloat getPowValue = 1.0;\n" +
                "\tif (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;\n" +
                "\tif (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;\n" +
                "\tmixFactor = pow(alpha - ft * 10.0, getPowValue);\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData2D(out mat4 model, out uint timerState, out float mixFactor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec2 final1 = texelFetch(dataPackage_Final1, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final0.x;\n" +
                "\n" +
                "\tfloat pryFacing = radians(final0.y * 0.5);\n" +
                "\tfloat pryCos = cos(pryFacing);\n" +
                "\tfloat prySin = sin(pryFacing);\n" +
                "\tfloat dqz = prySin + prySin;\n" +
                "\tfloat q22 = dqz * prySin;\n" +
                "\tfloat q23 = dqz * pryCos;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - q22 * final1.x, q23 * final1.x, 0.0, 0.0),\n" +
                "\tvec4(-q23 * final1.y, final1.y - q22 * final1.y, 0.0, 0.0),\n" +
                "\tvec4(0.0, 0.0, 1.0, 0.0),\n" +
                "\tvec4(final0.zw, 0.0, 1.0));\n" +
                "\n" +
                "\tfloat ft = trunc(alpha * 0.1);\n" +
                "\tuint ut =  uint(ft);\n" +
                "\ttimerState = ut;\n" +
                "\tfloat getPowValue = 1.0;\n" +
                "\tif (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;\n" +
                "\tif (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;\n" +
                "\tmixFactor = pow(alpha - ft * 10.0, getPowValue);\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveData3D(out mat4 model, out uint timerState, out float mixFactor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec4 final2 = texelFetch(dataPackage_Final2, gl_InstanceID);\n" +
                "\tvec2 tmpZY = final0.zy;\n" +
                "\tfinal0.yz = vec2(final1.w, final2.w);\n" +
                "\tfinal1.w = tmpZY.x;\n" +
                "\tfinal2.w = tmpZY.y;\n" +
                "\tmodel = modelMatrix * transpose(mat4(final0, final1, final2, vec4(0.0, 0.0, 0.0, 1.0)));\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : texelFetch(dataPackage_State0, gl_InstanceID).w;\n" +
                "\tfloat ft = trunc(alpha * 0.1);\n" +
                "\tuint ut =  uint(ft);\n" +
                "\ttimerState = ut;\n" +
                "\tfloat getPowValue = 1.0;\n" +
                "\tif (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;\n" +
                "\tif (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;\n" +
                "\tmixFactor = pow(alpha - ft * 10.0, getPowValue);\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceStateCompute) void haveFixedData3D(out mat4 model, out uint timerState, out float mixFactor) {\n" +
                "\tvec4 final0 = texelFetch(dataPackage_Final0, gl_InstanceID);\n" +
                "\tvec4 final1 = texelFetch(dataPackage_Final1, gl_InstanceID);\n" +
                "\tvec2 final2 = texelFetch(dataPackage_Final2, gl_InstanceID).xy;\n" +
                "\tfloat alpha = (instanceDataExtra >= 0.0) ? instanceDataExtra : final2.x;\n" +
                "\n" +
                "\tvec3 pryRotate = radians(final0.xyz * 0.5);\n" +
                "\tvec3 pryCos = cos(pryRotate);\n" +
                "\tvec3 prySin = sin(pryRotate);\n" +
                "\tfloat wq = pryCos.x * pryCos.y * pryCos.z - prySin.x * prySin.y * prySin.z;\n" +
                "\tfloat xq = prySin.x * pryCos.y * pryCos.z - pryCos.x * prySin.y * prySin.z;\n" +
                "\tfloat yq = pryCos.x * prySin.y * pryCos.z + prySin.x * pryCos.y * prySin.z;\n" +
                "\tfloat zq = pryCos.x * pryCos.y * prySin.z + prySin.x * prySin.y * pryCos.z;\n" +
                "\tfloat dqx = xq + xq;\n" +
                "\tfloat dqy = yq + yq;\n" +
                "\tfloat dqz = zq + zq;\n" +
                "\tfloat q00 = dqx * xq;\n" +
                "\tfloat q11 = dqy * yq;\n" +
                "\tfloat q22 = dqz * zq;\n" +
                "\tfloat q01 = dqx * yq;\n" +
                "\tfloat q02 = dqx * zq;\n" +
                "\tfloat q03 = dqx * wq;\n" +
                "\tfloat q12 = dqy * zq;\n" +
                "\tfloat q13 = dqy * wq;\n" +
                "\tfloat q23 = dqz * wq;\n" +
                "\tmodel = modelMatrix * mat4(\n" +
                "\tvec4(final1.x - (q11 + q22) * final1.x, (q01 + q23) * final1.x, (q02 - q13) * final1.x, 0.0),\n" +
                "\tvec4((q01 - q23) * final1.y, final1.y - (q22 + q00) * final1.y, (q12 + q03) * final1.y, 0.0),\n" +
                "\tvec4((q02 + q13) * final1.z, (q12 - q03) * final1.z, final1.z - (q11 + q00) * final1.z, 0.0),\n" +
                "\tvec4(final0.w, final1.w, final2.y, 1.0));\n" +
                "\n" +
                "\tfloat ft = trunc(alpha * 0.1);\n" +
                "\tuint ut =  uint(ft);\n" +
                "\ttimerState = ut;\n" +
                "\tfloat getPowValue = 1.0;\n" +
                "\tif (ut == 0u) getPowValue = statePackage[INOUT_FACTOR].z;\n" +
                "\tif (ut == 1u) getPowValue = statePackage[INOUT_FACTOR].w;\n" +
                "\tmixFactor = pow(alpha - ft * 10.0, getPowValue);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tmat4 currentMatrix;\n" +
                "\tuint mixStateValue;\n" +
                "\tfloat mixFactorValue;\n" +
                "\tinstanceState(currentMatrix, mixStateValue, mixFactorValue);\n" +
                "\tvec4 size = vec4(statePackage[1].xy, statePackage[3].zw);\n" +
                "\tvec4 sizeMix = size;\n" +
                "\tfloat power = statePackage[0].w;\n" +
                "\tfloat powerMix = power;\n" +
                "\tif (mixStateValue == 0u) {\n" +
                "\t\tsizeMix = vec4(statePackage[0].xy, statePackage[3].xy);\n" +
                "\t\tpowerMix = statePackage[0].z;\n" +
                "\t}\n" +
                "\tif (mixStateValue == 1u) {\n" +
                "\t\tsizeMix = vec4(statePackage[2].xy, statePackage[4].xy);\n" +
                "\t\tpowerMix = statePackage[1].z;\n" +
                "\t}\n" +
                "\tsize = mix(sizeMix, size, mixFactorValue);\n" +
                "\tpower = mix(powerMix, power, mixFactorValue);\n" +
                "\tmat3 rotateScale = mat3(currentMatrix[0].x, currentMatrix[0].y, 0.0, currentMatrix[1].x, currentMatrix[1].y, 0.0, vec3(0.0, 0.0, 1.0));\n" +
                "\tvec3 locationPre = vec3(vertex * size.xy, 1.0);\n" +
                "\tvec4 location = currentMatrix * vec4(locationPre.xy, 0.0, 1.0);\n" +
                "\tvec3 locationPost = abs(rotateScale * locationPre);\n" +
                "\tvec4 currPos = gameViewport * location;\n" +
                "\tif (power == 0.0) currPos.xyz = vec3(-65536.0);\n" +
                "\tgl_Position = currPos;\n" +
                "\tvb_data.fragUVMask = vec4(vertex, vertex / size.zw - statePackage[5].zw);\n" +
                "\tvb_data.fragUVScreen = vec4(getUV(location.xy) * screenScale, locationPost.xy / gameScreenBorder.zw * vertex * 0.5 * screenScale * power);\n" +
                "}\n";

        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define HARDNESS_RING 1\n" +
                "#define HARDNESS_INNER 4\n" +
                "\n" +
                "in VERTEX_BLOCK {\n" +
                "    vec4 fragUVMask;\n" +
                "    vec4 fragUVScreen;\n" +
                "} vb_data;\n" +
                "\n" +
                "// vec4(sizeIn, powerIn, powerFull), vec4(sizeFull, powerOut, hardnessRing), vec4(sizeOut, fadeInFactor, fadeOutFactor)\n" +
                "// vec4(sizeInRatio, sizeFullRatio), vec4(sizeOutRatio, hardnessInner, globalTimerRaw), vec4(arcStart, arcEnd, innerCenter)\n" +
                "uniform vec4 statePackage[6];\n" +
                "// screen\n" +
                "layout(binding = 0) uniform sampler2D texturePackage;\n" +
                "\n" +
                "out vec4 fragColor;\n" +
                "\n" +
                "float inverseLerp(in float edgeL, in float edgeR, in float value) {\n" +
                "    return (value - edgeL) / (edgeR - edgeL);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    float ring = length(vb_data.fragUVMask.xy);\n" +
                "    float inner = length(vb_data.fragUVMask.zw);\n" +
                "    float arc = min(inverseLerp(statePackage[5].y, statePackage[5].x, vb_data.fragUVMask.x / ring), 1.0);\n" +
                "    ring = statePackage[HARDNESS_RING].w >= 1.0 ? step(ring, 1.0) : smoothstep(1.0, statePackage[HARDNESS_RING].w, ring);\n" +
                "    inner = statePackage[HARDNESS_INNER].z >= 1.0 ? step(1.0, inner) : smoothstep(statePackage[HARDNESS_INNER].z, 1.0, inner);\n" +
                "    float mask = ring * inner * arc;\n" +
                "    if (mask <= 0.0) discard;\n" +
                "    vec4 result = texture(texturePackage, vb_data.fragUVScreen.xy - (vb_data.fragUVScreen.zw * mask));\n" +
                "    result.w = 1.0;\n" +
                "    fragColor = result;\n" +
                "}\n";
    }

    public final static class InstanceMatrix {
        private InstanceMatrix() {}

        public final static String COMP = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define WORKGROUP_SIZE WORKGROUP_SIZE_VALUE\n" +
                "\n" +
                "layout(local_size_x = 1, local_size_y = RESET_VALUE, local_size_z = 4) in;\n" +
                "\n" +
                "subroutine void instanceType(in int indexNow, in float alpha);\n" +
                "subroutine uniform instanceType instanceTypeState;\n" +
                "\n" +
                "// 2D: vec4*2(final), vec4*2(state)\n" +
                "// 3D: vec4*3(final), vec4*4(state)\n" +
                "layout(binding = 0, INSTANCE_FORMAT) uniform imageBuffer dataPackage_Final0;\n" +
                "layout(binding = 1, INSTANCE_FORMAT) uniform imageBuffer dataPackage_Final1;\n" +
                "layout(binding = 2, INSTANCE_FORMAT) uniform imageBuffer dataPackage_Final2;\n" +
                "layout(binding = 3, INSTANCE_FORMAT) uniform imageBuffer dataPackage_State0;\n" +
                "layout(binding = 4, INSTANCE_FORMAT) uniform imageBuffer dataPackage_State1;\n" +
                "layout(binding = 5, INSTANCE_FORMAT) uniform readonly imageBuffer dataPackage_State2;\n" +
                "layout(binding = 6, INSTANCE_FORMAT) uniform readonly imageBuffer dataPackage_State3;\n" +
                "// vec4(timer)\n" +
                "layout(binding = 7, rgba32f) uniform imageBuffer timerPackage;\n" +
                "// frameAmount, lastSize\n" +
                "uniform vec2 instanceState;\n" +
                "\n" +
                "subroutine(instanceType) void instance2D(in int indexNow, in float alpha) {\n" +
                "\tvec4 final0 = imageLoad(dataPackage_Final0, indexNow);\n" +
                "\tvec4 final1 = imageLoad(dataPackage_Final1, indexNow);\n" +
                "\tvec4 state0 = imageLoad(dataPackage_State0, indexNow);\n" +
                "\tvec4 state1 = imageLoad(dataPackage_State1, indexNow);\n" +
                "\n" +
                "\tvec2 loc = final0.wz;\n" +
                "\tfloat facing = state0.z;\n" +
                "\tvec2 scale = state0.xy;\n" +
                "\n" +
                "\t// dLoc, dScale\n" +
                "\tvec4 dynamic = state1 * instanceState.x;\n" +
                "\tfloat turnRate = state0.w * instanceState.x;\n" +
                "\n" +
                "\tfloat pryFacing = radians(facing * 0.5);\n" +
                "\tfloat pryCos = cos(pryFacing);\n" +
                "\tfloat prySin = sin(pryFacing);\n" +
                "\n" +
                "\tfloat dqz = prySin + prySin;\n" +
                "\tfloat q22 = dqz * prySin;\n" +
                "\tfloat q23 = dqz * pryCos;\n" +
                "\n" +
                "\tloc += dynamic.xy;\n" +
                "\tfacing = mod(facing + turnRate + 360.0, 360.0);\n" +
                "\tscale += dynamic.zw;\n" +
                "\timageStore(dataPackage_Final0, indexNow, vec4(\n" +
                "\tscale.x - q22 * scale.x,\n" +
                "\t-q23 * scale.y,\n" +
                "\tloc.y,\n" +
                "\tloc.x\n" +
                "\t));\n" +
                "\timageStore(dataPackage_Final1, indexNow, vec4(\n" +
                "\tq23 * scale.x,\n" +
                "\tscale.y - q22 * scale.y,\n" +
                "\t0.0,\n" +
                "\talpha\n" +
                "\t));\n" +
                "\n" +
                "\timageStore(dataPackage_State0, indexNow, vec4(scale, facing, state0.w));\n" +
                "}\n" +
                "\n" +
                "subroutine(instanceType) void instance3D(in int indexNow, in float alpha) {\n" +
                "\tvec4 final0 = imageLoad(dataPackage_Final0, indexNow);\n" +
                "\tvec4 final1 = imageLoad(dataPackage_Final1, indexNow);\n" +
                "\tvec4 final2 = imageLoad(dataPackage_Final2, indexNow);\n" +
                "\tvec4 state0 = imageLoad(dataPackage_State0, indexNow);\n" +
                "\tvec4 state1 = imageLoad(dataPackage_State1, indexNow);\n" +
                "\tvec4 state2 = imageLoad(dataPackage_State2, indexNow);\n" +
                "\tvec4 state3 = imageLoad(dataPackage_State3, indexNow);\n" +
                "\n" +
                "\tvec3 loc = final0.wyz;\n" +
                "\tvec3 rotate = vec3(state0.x, state0.y, state0.z);\n" +
                "\tvec3 scale = vec3(state1.x, state1.y, state1.z);\n" +
                "\n" +
                "\t// dLoc, dRotate, dScale\n" +
                "\tmat3 dynamic = mat3(\n" +
                "\tstate1.w, state2.x, state2.y,\n" +
                "\tstate2.z, state2.w, state3.x,\n" +
                "\tstate3.y, state3.z, state3.w\n" +
                "\t);\n" +
                "\tdynamic *= instanceState.x;\n" +
                "\n" +
                "\tvec3 pryRotate = radians(rotate * 0.5);\n" +
                "\tvec3 pryCos = cos(pryRotate);\n" +
                "\tvec3 prySin = sin(pryRotate);\n" +
                "\n" +
                "\tfloat wq = pryCos.x * pryCos.y * pryCos.z - prySin.x * prySin.y * prySin.z;\n" +
                "\tfloat xq = prySin.x * pryCos.y * pryCos.z - pryCos.x * prySin.y * prySin.z;\n" +
                "\tfloat yq = pryCos.x * prySin.y * pryCos.z + prySin.x * pryCos.y * prySin.z;\n" +
                "\tfloat zq = pryCos.x * pryCos.y * prySin.z + prySin.x * prySin.y * pryCos.z;\n" +
                "\n" +
                "\tfloat dqx = xq + xq;\n" +
                "\tfloat dqy = yq + yq;\n" +
                "\tfloat dqz = zq + zq;\n" +
                "\tfloat q00 = dqx * xq;\n" +
                "\tfloat q11 = dqy * yq;\n" +
                "\tfloat q22 = dqz * zq;\n" +
                "\tfloat q01 = dqx * yq;\n" +
                "\tfloat q02 = dqx * zq;\n" +
                "\tfloat q03 = dqx * wq;\n" +
                "\tfloat q12 = dqy * zq;\n" +
                "\tfloat q13 = dqy * wq;\n" +
                "\tfloat q23 = dqz * wq;\n" +
                "\n" +
                "\tloc += dynamic[0];\n" +
                "\trotate = mod(rotate + dynamic[1] + 360.0, 360.0);\n" +
                "\tscale += dynamic[2];\n" +
                "\timageStore(dataPackage_Final0, indexNow, vec4(\n" +
                "\tscale.x - (q11 + q22) * scale.x,\n" +
                "\tloc.z,\n" +
                "\tloc.y,\n" +
                "\tloc.x\n" +
                "\t));\n" +
                "\timageStore(dataPackage_Final1, indexNow, vec4(\n" +
                "\t(q01 + q23) * scale.x,\n" +
                "\tscale.y - (q22 + q00) * scale.y,\n" +
                "\t(q12 - q03) * scale.z,\n" +
                "\t(q01 - q23) * scale.y\n" +
                "\t));\n" +
                "\timageStore(dataPackage_Final2, indexNow, vec4(\n" +
                "\t(q02 - q13) * scale.x,\n" +
                "\t(q12 + q03) * scale.y,\n" +
                "\tscale.z - (q11 + q00) * scale.z,\n" +
                "\t(q02 + q13) * scale.z\n" +
                "\t));\n" +
                "\n" +
                "\timageStore(dataPackage_State0, indexNow, vec4(rotate, alpha));\n" +
                "\timageStore(dataPackage_State1, indexNow, vec4(scale, state1.w));\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tint localIndex = int(gl_LocalInvocationID.y * gl_WorkGroupSize.z + gl_LocalInvocationID.z);\n" +
                "\tint globalIndex = int(gl_WorkGroupID.y * gl_NumWorkGroups.z + gl_WorkGroupID.z) * WORKGROUP_SIZE;\n" +
                "\tint indexNow = localIndex + globalIndex;\n" +
                "\tif (indexNow >= int(instanceState.y)) return;\n" +
                "\tvec4 timer = imageLoad(timerPackage, indexNow);\n" +
                "\tif (timer.x < -1000.0) return;\n" +
                "\n" +
                "\tfloat alpha = 10.0;\n" +
                "\tbool finishSet = timer.x < -500.0;\n" +
                "\tvec2 timerTmp = finishSet ? vec2(-512.0) : vec2(timer.w, -10.0);\n" +
                "\tif (timer.x > 2.0) {\n" +
                "\t\talpha = abs(timer.x - 3.0);\n" +
                "\t\ttimerTmp = vec2(timer.y, 2.0);\n" +
                "\t} else if (timer.x > 1.0) {\n" +
                "\t\ttimerTmp = vec2(timer.z, 1.0);\n" +
                "\t\talpha = 21.0;\n" +
                "\t} else if (timer.x > 0.0) {\n" +
                "\t\talpha = timer.x + 10.0;\n" +
                "\t}\n" +
                "\ttimer.x = timerTmp.x > -500.0 ? (timer.x - timerTmp.x * instanceState.x) : timerTmp.y;\n" +
                "\tif (timer.x <= 0.0 && timer.x > -500.0) {\n" +
                "\t\ttimer.x = -512.0;\n" +
                "\t} else if (finishSet) timer.x = -1024.0;\n" +
                "\timageStore(timerPackage, indexNow, timer);\n" +
                "\n" +
                "\tinstanceTypeState(indexNow, alpha);\n" +
                "}\n";
    }

    public final static class Bloom {
        private Bloom() {}

        public final static String VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define RADIUS_SCALE OVERWRITE_RADIUS_SCALE\n" +
                "#define SCREENSTEP_X OVERWRITE_SCREEN_X * RADIUS_SCALE\n" +
                "#define SCREENSTEP_X2 SCREENSTEP_X * 2.0\n" +
                "#define SCREENSTEP_Y OVERWRITE_SCREEN_Y * RADIUS_SCALE\n" +
                "#define SCREENSTEP_Y2 SCREENSTEP_Y * 2.0\n" +
                "\n" +
                "subroutine mat4 sampleMode(in vec2 uv);\n" +
                "subroutine uniform sampleMode sampleModeState;\n" +
                "\n" +
                "layout (location = 0) in vec2 vertex;\n" +
                "\n" +
                "uniform float scale;\n" +
                "\n" +
                "smooth out mat4 fragUV;\n" +
                "\n" +
                "subroutine(sampleMode) mat4 downSample(in vec2 uv) {\n" +
                "\treturn mat4(vec4(0.0, 0.0, -SCREENSTEP_X, -SCREENSTEP_Y), vec4(SCREENSTEP_X, -SCREENSTEP_Y, SCREENSTEP_X, SCREENSTEP_Y), vec4(-SCREENSTEP_X, SCREENSTEP_Y, 0.0, 0.0), vec4(0.0)) + mat4(uv, uv, uv, uv, uv, uv, uv, uv);\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMode) mat4 upSample(in vec2 uv) {\n" +
                "\treturn mat4(vec4(-SCREENSTEP_X, -SCREENSTEP_Y, SCREENSTEP_X, -SCREENSTEP_Y), vec4(SCREENSTEP_X, SCREENSTEP_Y, -SCREENSTEP_X, SCREENSTEP_Y), vec4(-SCREENSTEP_X2, 0.0, 0.0, -SCREENSTEP_Y2), vec4(SCREENSTEP_X2, 0.0, 0.0, SCREENSTEP_Y2)) + mat4(uv, uv, uv, uv, uv, uv, uv, uv);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "\tgl_Position = vec4(vertex, 0.0, 1.0);\n" +
                "\tfragUV = sampleModeState(max(vertex, vec2(0.0))) / scale;\n" +
                "}\n";

        public final static String FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "subroutine vec4 sampleMode();\n" +
                "subroutine uniform sampleMode sampleModeState;\n" +
                "\n" +
                "smooth in mat4 fragUV;\n" +
                "\n" +
                "layout (binding = 0) uniform sampler2D screen;\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "\n" +
                "subroutine(sampleMode) vec4 downSample() {\n" +
                "    vec4 result = texture(screen, fragUV[0].zw);\n" +
                "    result += texture(screen, fragUV[1].xy);\n" +
                "    result += texture(screen, fragUV[1].zw);\n" +
                "    result += texture(screen, fragUV[2].xy);\n" +
                "    return result * 0.125 + texture(screen, fragUV[0].xy) * 0.5;\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMode) vec4 upSample() {\n" +
                "    vec4 resultA = texture(screen, fragUV[0].xy);\n" +
                "    resultA += texture(screen, fragUV[0].zw);\n" +
                "    resultA += texture(screen, fragUV[1].xy);\n" +
                "    resultA += texture(screen, fragUV[1].zw);\n" +
                "    vec4 resultB = texture(screen, fragUV[2].xy);\n" +
                "    resultB += texture(screen, fragUV[2].zw);\n" +
                "    resultB += texture(screen, fragUV[3].xy);\n" +
                "    resultB += texture(screen, fragUV[3].zw);\n" +
                "    return resultA * 0.1666667 + resultB * 0.0833333;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    fragColor = sampleModeState() * 1.2;\n" +
                "}\n";
    }

    public final static class FXAA {
        private FXAA() {}

        public final static String CONSOLE = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define SCREENSTEPX OVERWRITE_SCREEN_X\n" +
                "#define SCREENSTEPY OVERWRITE_SCREEN_Y\n" +
                "#define FXAA_SHARPNESS 0.5\n" +
                "#define FXAA_ABSOLUTE_LUMA_THRESHOLD 0.05\n" +
                "#define FXAA_RELATIVE_LUMA_THRESHOLD 0.1\n" +
                "#define OFFSETSIZE 5\n" +
                "#define NW 0\n" +
                "#define NE 1\n" +
                "#define C 2\n" +
                "#define SW 3\n" +
                "#define SE 4\n" +
                "#define GLOW_TEX 2\n" +
                "\n" +
                "subroutine float sampleMethod(in vec2 sampleUV);\n" +
                "subroutine uniform sampleMethod sampleMethodState;\n" +
                "subroutine vec4 displayMethod(in vec4 finalColor);\n" +
                "subroutine uniform displayMethod displayMethodState;\n" +
                "\n" +
                "smooth in vec2 fragUV;\n" +
                "\n" +
                "layout(binding = 0) uniform sampler2D screen;\n" +
                "layout(binding = 1) uniform sampler2D fragData;\n" +
                "\n" +
                "out vec4 fragColor;\n" +
                "\n" +
                "subroutine(sampleMethod) float fromRaw(in vec2 sampleUV) {\n" +
                "    return dot(texture(screen, sampleUV).xyz, vec3(LINEAR_VALUES));\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMethod) float fromDepth(in vec2 sampleUV) {\n" +
                "    return texture(fragData, sampleUV).y;\n" +
                "}\n" +
                "\n" +
                "subroutine(displayMethod) vec4 commonDisplay(in vec4 finalColor) {\n" +
                "    return finalColor;\n" +
                "}\n" +
                "\n" +
                "subroutine(displayMethod) vec4 edgeDisplay(in vec4 finalColor) {\n" +
                "    return vec4(0.0, 1.0, 0.0, 1.0);\n" +
                "}\n" +
                "\n" +
                "const vec2 SCREEN_OFFSET[] = vec2[](vec2(-0.5, 0.5), vec2(0.5, 0.5), vec2( 0.0, 0.0), vec2(-0.5,-0.5), vec2(0.5,-0.5));\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec2 screenStep = vec2(SCREENSTEPX, SCREENSTEPY);\n" +
                "    vec4 fragRaw = texture(screen, fragUV);\n" +
                "\n" +
                "    float luma[OFFSETSIZE];\n" +
                "    for (int i = 0; i < OFFSETSIZE; i++) {\n" +
                "        vec2 eachUV = vec2(fragUV + screenStep * SCREEN_OFFSET[i]);\n" +
                "        luma[i] = sampleMethodState(eachUV);\n" +
                "    }\n" +
                "\n" +
                "    float lumaMax = max(luma[NW], max(max(luma[NE], luma[C]), max(luma[SW], luma[SE])));\n" +
                "    float lumaMin = min(luma[NW], min(min(luma[NE], luma[C]), min(luma[SW], luma[SE])));\n" +
                "    float lumaContrast = lumaMax - lumaMin;\n" +
                "    if(lumaContrast < max(FXAA_ABSOLUTE_LUMA_THRESHOLD, lumaMax * FXAA_RELATIVE_LUMA_THRESHOLD)) {\n" +
                "        fragColor = fragRaw;\n" +
                "        return;\n" +
                "    }\n" +
                "\n" +
                "    vec2 dir = vec2(0.0);\n" +
                "    dir.x = (luma[SW] + luma[SE]) - (luma[NW] + luma[NE]);\n" +
                "    dir.y = (luma[NE] + luma[SW]) - (luma[NE] + luma[SE]);\n" +
                "    dir = normalize(dir);\n" +
                "    vec4 P1 = texture(screen, fragUV + (dir * screenStep * 0.5));\n" +
                "    vec4 N1 = texture(screen, fragUV - (dir * screenStep * 0.5));\n" +
                "\n" +
                "    float dirAbsMinTimesC = min(abs(dir.x), abs(dir.y)) * FXAA_SHARPNESS;\n" +
                "    vec2 minDir = clamp(dir / dirAbsMinTimesC, -1.0, 1.0) * 0.5;\n" +
                "    vec4 P2 = texture(screen, fragUV + (minDir * screenStep));\n" +
                "    vec4 N2 = texture(screen, fragUV - (minDir * screenStep));\n" +
                "\n" +
                "    vec4 S1 = P1 + N1;\n" +
                "    vec4 S2 = (P2 + N2 + S1) / 4.0;\n" +
                "    float brightness = dot(S2.xyz, vec3(LINEAR_VALUES));\n" +
                "    if (brightness < lumaMin || brightness > lumaMax) {\n" +
                "        S2.xyz = S1.xyz * 0.5;\n" +
                "    }\n" +
                "    fragColor = displayMethodState(S2);\n" +
                "}\n";

        public final static String QUALITY = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "#define SCREENSTEPX OVERWRITE_SCREEN_X\n" +
                "#define SCREENSTEPY OVERWRITE_SCREEN_Y\n" +
                "#define FXAA_SHARPNESS 0.5\n" +
                "#define FXAA_ABSOLUTE_LUMA_THRESHOLD 0.1\n" +
                "#define FXAA_RELATIVE_LUMA_THRESHOLD 0.15\n" +
                "#define OFFSETSIZE 9\n" +
                "#define EDGESIZE 4\n" +
                "#define NW 0\n" +
                "#define N 1\n" +
                "#define NE 2\n" +
                "#define W 3\n" +
                "#define C 4\n" +
                "#define E 5\n" +
                "#define SW 6\n" +
                "#define S 7\n" +
                "#define SE 8\n" +
                "\n" +
                "subroutine float sampleMethod(in vec2 sampleUV);\n" +
                "subroutine uniform sampleMethod sampleMethodState;\n" +
                "subroutine vec4 displayMethod(in vec2 finalUV);\n" +
                "subroutine uniform displayMethod displayMethodState;\n" +
                "\n" +
                "smooth in vec2 fragUV;\n" +
                "\n" +
                "layout(binding = 0) uniform sampler2D screen;\n" +
                "layout(binding = 1) uniform sampler2D fragData;\n" +
                "\n" +
                "out vec4 fragColor;\n" +
                "\n" +
                "subroutine(sampleMethod) float fromRaw(in vec2 sampleUV) {\n" +
                "    return dot(texture(screen, sampleUV).xyz, vec3(LINEAR_VALUES));\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMethod) float fromDepth(in vec2 sampleUV) {\n" +
                "    return texture(fragData, sampleUV).y;\n" +
                "}\n" +
                "\n" +
                "subroutine(displayMethod) vec4 commonDisplay(in vec2 finalUV) {\n" +
                "    return texture(screen, finalUV);\n" +
                "}\n" +
                "\n" +
                "subroutine(displayMethod) vec4 edgeDisplay(in vec2 finalUV) {\n" +
                "    return vec4(0.0, 1.0, 0.0, 1.0);\n" +
                "}\n" +
                "\n" +
                "const vec2 SCREEN_OFFSET[] = vec2[](vec2(-1.0, 1.0), vec2(0.0, 1.0), vec2(1.0, 1.0), vec2(-1.0, 0.0), vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(-1.0,-1.0), vec2(0.0,-1.0), vec2(1.0,-1.0));\n" +
                "const float EDGE_STEP[] = float[](1.5, 2.0, 2.0, 8.0);\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec2 screenStepVec = vec2(SCREENSTEPX, SCREENSTEPY);\n" +
                "    vec4 fragRaw = texture(screen, fragUV);\n" +
                "\n" +
                "    float luma[OFFSETSIZE];\n" +
                "    for (int i = 0; i < OFFSETSIZE; i++) {\n" +
                "        vec2 eachUV = vec2(fragUV + screenStepVec * SCREEN_OFFSET[i]);\n" +
                "        luma[i] = sampleMethodState(eachUV);\n" +
                "    }\n" +
                "\n" +
                "    float lumaMax = max(luma[N], max(max(luma[W], luma[C]), max(luma[E], luma[S])));\n" +
                "    float lumaMin = min(luma[N], min(min(luma[W], luma[C]), min(luma[E], luma[S])));\n" +
                "    float lumaContrast = lumaMax - lumaMin;\n" +
                "    if (lumaContrast < max(FXAA_ABSOLUTE_LUMA_THRESHOLD, lumaMax * FXAA_RELATIVE_LUMA_THRESHOLD)) {\n" +
                "        fragColor = fragRaw;\n" +
                "        return;\n" +
                "    }\n" +
                "\n" +
                "    float lumaHorzC = luma[N] + luma[S];\n" +
                "    float lumaVertC = luma[W] + luma[E];\n" +
                "    float lumaHorzTR = luma[NE] + luma[SE];\n" +
                "    float lumaVertTR = luma[NW] + luma[NE];\n" +
                "    float lumaHorzBL = luma[NW] + luma[SW];\n" +
                "    float lumaVertBL = luma[SW] + luma[SE];\n" +
                "    float edgeHorz = abs((-2.0 * luma[W])+ lumaHorzBL) + (abs((-2.0 * luma[C]) + lumaHorzC) * 2.0) + abs((-2.0 * luma[E]) + lumaHorzTR);\n" +
                "    float edgeVert = abs((-2.0 * luma[S]) + lumaVertBL) + (abs((-2.0 * luma[C]) + lumaVertC) * 2.0) + abs((-2.0 * luma[N]) + lumaVertTR);\n" +
                "    bool isHorz = edgeHorz >= edgeVert;\n" +
                "\n" +
                "    float screenStep = isHorz ? screenStepVec.y : screenStepVec.x;\n" +
                "    float luma1 = isHorz ? luma[S] : luma[W];\n" +
                "    float luma2 = isHorz ? luma[N] : luma[E];\n" +
                "    float gradient1 = luma1 - luma[C];\n" +
                "    float gradient2 = luma2 - luma[C];\n" +
                "    float gradientScaled = 0.25 * max(abs(gradient1), abs(gradient2));\n" +
                "\n" +
                "    bool is1Steepest = abs(gradient1) >= abs(gradient2);\n" +
                "    float lumaLocalAverage = is1Steepest ? (0.5 * (luma1 + luma[C])) : (0.5 * (luma2 + luma[C]));\n" +
                "    if (is1Steepest) {\n" +
                "        screenStep = -screenStep;\n" +
                "    }\n" +
                "\n" +
                "    vec2 startN = isHorz ? vec2(fragUV.x, fragUV.y + screenStep * 0.5) : vec2(fragUV.x + screenStep * 0.5, fragUV.y);\n" +
                "    vec2 uvOffsetT = isHorz ? vec2(screenStepVec.x, 0.0) : vec2(0.0, screenStepVec.y);\n" +
                "\n" +
                "    vec2 uvL = startN - uvOffsetT;\n" +
                "    vec2 uvR = startN + uvOffsetT;\n" +
                "    float lumaEndL = sampleMethodState(uvL) - lumaLocalAverage;\n" +
                "    float lumaEndR = sampleMethodState(uvR) - lumaLocalAverage;\n" +
                "\n" +
                "    bool reachedL = abs(lumaEndL) >= gradientScaled;\n" +
                "    bool reachedR = abs(lumaEndR) >= gradientScaled;\n" +
                "    bool reachedLR = reachedL && reachedR;\n" +
                "\n" +
                "    if (!reachedL){\n" +
                "        uvL -= uvOffsetT * EDGE_STEP[0];\n" +
                "    }\n" +
                "    if (!reachedR){\n" +
                "        uvR += uvOffsetT * EDGE_STEP[0];\n" +
                "    }\n" +
                "\n" +
                "    if (!reachedLR) {\n" +
                "        for (int i = 1; i < EDGESIZE; i++) {\n" +
                "            if(!reachedL) lumaEndL = sampleMethodState(uvL) - lumaLocalAverage;\n" +
                "            if(!reachedR) lumaEndR = sampleMethodState(uvR) - lumaLocalAverage;\n" +
                "            reachedL = abs(lumaEndL) >= gradientScaled;\n" +
                "            reachedR = abs(lumaEndR) >= gradientScaled;\n" +
                "            reachedLR = reachedL && reachedR;\n" +
                "\n" +
                "            if(!reachedL) uvL -= uvOffsetT * EDGE_STEP[i];\n" +
                "            if(!reachedR) uvR += uvOffsetT * EDGE_STEP[i];\n" +
                "            if(reachedLR) break;\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    float nearestUVL = isHorz ? (fragUV.x - uvL.x) : (fragUV.y - uvL.y);\n" +
                "    float nearestUVR = isHorz ? (uvR.x - fragUV.x) : (uvR.y - fragUV.y);\n" +
                "\n" +
                "    bool isNearestL = nearestUVL <= nearestUVR;\n" +
                "    float nearestUV = min(nearestUVL, nearestUVR);\n" +
                "    float edgeLength = nearestUVL + nearestUVR;\n" +
                "\n" +
                "    bool isLumaCenterSmaller = luma[C] < lumaLocalAverage;\n" +
                "    bool correctVariationL = (lumaEndL < 0.0) != isLumaCenterSmaller;\n" +
                "    bool correctVariationR = (lumaEndR < 0.0) != isLumaCenterSmaller;\n" +
                "    bool correctVariation = isNearestL ? correctVariationL : correctVariationR;\n" +
                "    float finalOffset = correctVariation ? (-nearestUV / edgeLength + 0.5) : 0.0;\n" +
                "\n" +
                "    vec2 finalUV = isHorz ? vec2(fragUV.x, finalOffset * screenStep + fragUV.y) : vec2(finalOffset * screenStep + fragUV.x, fragUV.y);\n" +
                "    fragColor = displayMethodState(finalUV);\n" +
                "}\n";
    }

    public final static class SDF {
        public final static String INIT = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine float sampleMethod(in ivec2 sampleCoord);\n" +
                "subroutine uniform sampleMethod sampleMethodState;\n" +
                "\n" +
                "layout(binding = 0, rgba8) uniform readonly image2D checkMap;\n" +
                "layout(binding = 1, rgba16i) uniform writeonly iimage2D resultMap;\n" +
                "\n" +
                "uniform ivec4 sizeState; // ivec2(checkMapSize), ivec2(resultMapSize)\n" +
                "uniform ivec2 border;\n" +
                "uniform float threshold;\n" +
                "\n" +
                "subroutine(sampleMethod) float fromRed(in ivec2 sampleCoord) {\n" +
                "    return imageLoad(checkMap, sampleCoord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMethod) float fromGreen(in ivec2 sampleCoord) {\n" +
                "    return imageLoad(checkMap, sampleCoord).y;\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMethod) float fromBlue(in ivec2 sampleCoord) {\n" +
                "    return imageLoad(checkMap, sampleCoord).z;\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMethod) float fromAlpha(in ivec2 sampleCoord) {\n" +
                "    return imageLoad(checkMap, sampleCoord).w;\n" +
                "}\n" +
                "\n" +
                "subroutine(sampleMethod) float fromRGB(in ivec2 sampleCoord) {\n" +
                "    return dot(imageLoad(checkMap, sampleCoord).xyz, vec3(LINEAR_VALUES));\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, sizeState.zw))) return;\n" +
                "    ivec2 coordStore = ivec2(coord + 1);\n" +
                "    ivec2 checkCoord = coord - border;\n" +
                "    ivec4 result;\n" +
                "    if (all(bvec4(greaterThanEqual(coord, border), lessThan(checkCoord, sizeState.xy)))) {\n" +
                "        result = (sampleMethodState(checkCoord) >= threshold) ? ivec4(coordStore, 0, 0) : ivec4(0, 0, coordStore);\n" +
                "    } else result = ivec4(0, 0, coordStore);\n" +
                "    imageStore(resultMap, coord, result);\n" +
                "}";

        public final static String PROCESS = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "#define MAX_DISTANCE 268435456.0\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "layout(binding = 0, rgba16i) uniform iimage2D coordMap;\n" +
                "\n" +
                "uniform ivec2 size;\n" +
                "uniform int step;\n" +
                "\n" +
                "ivec4 advanceJFA(in ivec2 coord, ivec2 maxIndex) {\n" +
                "    ivec4 result = ivec4(0);\n" +
                "\n" +
                "    ivec2 currCoord = ivec2(0);\n" +
                "    ivec4 targetCoord;\n" +
                "    vec4 diffCoord = vec4(0.0);\n" +
                "    vec2 currDistSQ = vec2(0.0), distSQ = vec2(MAX_DISTANCE);\n" +
                "    for (int y = -1; y <= 1; ++y) {\n" +
                "        currCoord.y = y * step + coord.y;\n" +
                "        for (int x = -1; x <= 1; ++x) {\n" +
                "            currCoord.x = x * step + coord.x;\n" +
                "            if (any(bvec4(lessThan(currCoord, ivec2(0)), greaterThanEqual(currCoord, maxIndex)))) continue;\n" +
                "            targetCoord = imageLoad(coordMap, currCoord) - 1;\n" +
                "\n" +
                "            if (targetCoord.x > -1) {\n" +
                "                diffCoord.xy = vec2(coord - targetCoord.xy);\n" +
                "                currDistSQ.x = dot(diffCoord.xy, diffCoord.xy);\n" +
                "                if (currDistSQ.x < distSQ.x) {\n" +
                "                    distSQ.x = currDistSQ.x;\n" +
                "                    result.xy = targetCoord.xy + 1;\n" +
                "                }\n" +
                "            }\n" +
                "            if (targetCoord.z > -1) {\n" +
                "                diffCoord.zw = vec2(coord - targetCoord.zw);\n" +
                "                currDistSQ.y = dot(diffCoord.zw, diffCoord.zw);\n" +
                "                if (currDistSQ.y < distSQ.y) {\n" +
                "                    distSQ.y = currDistSQ.y;\n" +
                "                    result.zw = targetCoord.zw + 1;\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, size))) return;\n" +
                "    ivec2 maxIndex = size - 1;\n" +
                "    imageStore(coordMap, coord, advanceJFA(coord, maxIndex));\n" +
                "}";

        public final static String RESULT = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine void formatPickerStore(in ivec2 coord, in vec4 result);\n" +
                "subroutine uniform formatPickerStore formatPickerStoreState;\n" +
                "\n" +
                "layout(binding = 0, rgba16i) uniform readonly iimage2D coordMap;\n" +
                "layout(binding = 1, r8) uniform writeonly image2D resultMap;\n" +
                "layout(binding = 2, r16) uniform writeonly image2D resultMapR16;\n" +
                "\n" +
                "uniform ivec2 size;\n" +
                "uniform vec2 preMultiply;\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit8Store(in ivec2 coord, in vec4 result) {\n" +
                "    imageStore(resultMap, coord, result);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit16Store(in ivec2 coord, in vec4 result) {\n" +
                "    imageStore(resultMapR16, coord, result);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, size))) return;\n" +
                "    vec4 result = vec4(max(imageLoad(coordMap, coord) - 1, 0) - coord.xyxy);\n" +
                "    float sdf = clamp(length(result.xy) * preMultiply.y, 0.0, 1.0) - clamp(length(result.zw) * preMultiply.x, 0.0, 1.0);\n" +
                "    result = vec4(clamp(sdf * 0.5 + 0.5, 0.0, 1.0), vec3(0.0));\n" +
                "    formatPickerStoreState(coord, result);\n" +
                "}";

        private SDF() {}
    }

    public final static class GaussianBlur {
        private GaussianBlur() {}

        public final static String RGBA = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine vec4 formatPickerLoad(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoad formatPickerLoadState;\n" +
                "subroutine void formatPickerStore(in ivec2 coord, in vec4 result);\n" +
                "subroutine uniform formatPickerStore formatPickerStoreState;\n" +
                "\n" +
                "layout(binding = 0, rgba8) uniform readonly image2D texIn;\n" +
                "layout(binding = 1, rgba8) uniform writeonly image2D texOut;\n" +
                "layout(binding = 2, rgba16) uniform readonly image2D texIn16;\n" +
                "layout(binding = 3, rgba16) uniform writeonly image2D texOut16;\n" +
                "\n" +
                "uniform ivec3 sizeStep;\n" +
                "uniform int vertical;\n" +
                "uniform float perStep; // 1.5219615 / max(step, 1)\n" +
                "\n" +
                "float getGaussian(float i) {\n" +
                "    return 0.3789403 * exp(-1.1904761 * i * i);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) vec4 bit8Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn, coord);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) vec4 bit16Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn16, coord);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit8Store(in ivec2 coord, in vec4 result) {\n" +
                "    imageStore(texOut, coord, result);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit16Store(in ivec2 coord, in vec4 result) {\n" +
                "    imageStore(texOut16, coord, result);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, sizeStep.xy))) return;\n" +
                "    ivec2 maxIndex = sizeStep.xy - 1;\n" +
                "    vec4 result = vec4(0.0);\n" +
                "    ivec2 offset;\n" +
                "    float gaussian, fix = 0.0;\n" +
                "    bool isVertical = bool(vertical);\n" +
                "    for (int i = -sizeStep.z; i <= sizeStep.z; ++i) {\n" +
                "        gaussian = getGaussian(float(i) * perStep);\n" +
                "        fix += gaussian;\n" +
                "        offset = isVertical ? ivec2(0, i) : ivec2(i, 0);\n" +
                "        result += formatPickerLoadState(clamp(offset + coord, ivec2(0), maxIndex)) * gaussian;\n" +
                "    }\n" +
                "    result /= fix;\n" +
                "    formatPickerStoreState(coord, result);\n" +
                "}";

        public final static String RED = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine float formatPickerLoad(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoad formatPickerLoadState;\n" +
                "subroutine void formatPickerStore(in ivec2 coord, in float result);\n" +
                "subroutine uniform formatPickerStore formatPickerStoreState;\n" +
                "\n" +
                "layout(binding = 0, r8) uniform readonly image2D texIn;\n" +
                "layout(binding = 1, r8) uniform writeonly image2D texOut;\n" +
                "layout(binding = 2, r16) uniform readonly image2D texIn16;\n" +
                "layout(binding = 3, r16) uniform writeonly image2D texOut16;\n" +
                "\n" +
                "uniform ivec3 sizeStep;\n" +
                "uniform int vertical;\n" +
                "uniform float perStep; // 1.5219615 / max(step, 1)\n" +
                "\n" +
                "float getGaussian(float i) {\n" +
                "    return 0.3789403 * exp(-1.1904761 * i * i);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) float bit8Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) float bit16Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn16, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit8Store(in ivec2 coord, in float result) {\n" +
                "    imageStore(texOut, coord, vec4(result, vec3(0.0)));\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit16Store(in ivec2 coord, in float result) {\n" +
                "    imageStore(texOut16, coord, vec4(result, vec3(0.0)));\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, sizeStep.xy))) return;\n" +
                "    ivec2 maxIndex = sizeStep.xy - 1;\n" +
                "    float result = 0.0;\n" +
                "    ivec2 offset;\n" +
                "    float gaussian, fix = 0.0;\n" +
                "    bool isVertical = bool(vertical);\n" +
                "    for (int i = -sizeStep.z; i <= sizeStep.z; ++i) {\n" +
                "        gaussian = getGaussian(float(i) * perStep);\n" +
                "        fix += gaussian;\n" +
                "        offset = isVertical ? ivec2(0, i) : ivec2(i, 0);\n" +
                "        result += formatPickerLoadState(clamp(offset + coord, ivec2(0), maxIndex)) * gaussian;\n" +
                "    }\n" +
                "    result /= fix;\n" +
                "    formatPickerStoreState(coord, result);\n" +
                "}";
    }

    public final static class BilateralFilter {
        private BilateralFilter() {}

        public final static String RGBA = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine vec4 formatPickerLoadSrc(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoadSrc formatPickerLoadSrcState;\n" +
                "subroutine vec3 formatPickerLoad(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoad formatPickerLoadState;\n" +
                "subroutine void formatPickerStore(in ivec2 coord, in vec4 result);\n" +
                "subroutine uniform formatPickerStore formatPickerStoreState;\n" +
                "\n" +
                "layout(binding = 0, rgba8) uniform readonly image2D texSrc;\n" +
                "layout(binding = 1, rgba16) uniform readonly image2D texSrc16;\n" +
                "layout(binding = 2, rgba8) uniform readonly image2D texIn;\n" +
                "layout(binding = 3, rgba8) uniform writeonly image2D texOut;\n" +
                "layout(binding = 4, rgba16) uniform readonly image2D texIn16;\n" +
                "layout(binding = 5, rgba16) uniform writeonly image2D texOut16;\n" +
                "\n" +
                "uniform ivec3 sizeStep;\n" +
                "uniform int vertical;\n" +
                "uniform vec2 gSigmaSRInv;\n" +
                "\n" +
                "vec3 getWeight(in vec3 diff, in float i) {\n" +
                "    return exp(gSigmaSRInv.x * i * i + gSigmaSRInv.y * diff * diff);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoadSrc) vec4 bit8LoadSrc(in ivec2 coord) {\n" +
                "    return imageLoad(texSrc, coord);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoadSrc) vec4 bit16LoadSrc(in ivec2 coord) {\n" +
                "    return imageLoad(texSrc16, coord);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) vec3 bit8Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn, coord).xyz;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) vec3 bit16Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn16, coord).xyz;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit8Store(in ivec2 coord, in vec4 result) {\n" +
                "    imageStore(texOut, coord, result);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit16Store(in ivec2 coord, in vec4 result) {\n" +
                "    imageStore(texOut16, coord, result);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, sizeStep.xy))) return;\n" +
                "    vec4 center = formatPickerLoadSrcState(coord);\n" +
                "    bool isVertical = bool(vertical);\n" +
                "    ivec2 checkSize = ivec2(0, isVertical ? sizeStep.y : sizeStep.x);\n" +
                "    ivec2 coordCurr;\n" +
                "    vec3 curr, diff, sum = vec3(0.0), weightCurr, weight = vec3(0.0);\n" +
                "    for (int i = -sizeStep.z; i <= sizeStep.z; ++i) {\n" +
                "        coordCurr = coord + (isVertical ? ivec2(0, i) : ivec2(i, 0));\n" +
                "        checkSize.x = isVertical ? coordCurr.y : coordCurr.x;\n" +
                "        if (checkSize.x < 0 || checkSize.x >= checkSize.y) continue;\n" +
                "        curr = formatPickerLoadState(coordCurr);\n" +
                "        diff = center.xyz - curr;\n" +
                "        weightCurr = getWeight(diff, float(i));\n" +
                "        sum += curr * weightCurr;\n" +
                "        weight += weightCurr;\n" +
                "    }\n" +
                "    formatPickerStoreState(coord, vec4(sum / weight, center.w));\n" +
                "}";

        public final static String RED = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine float formatPickerLoadSrc(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoadSrc formatPickerLoadSrcState;\n" +
                "subroutine float formatPickerLoad(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoad formatPickerLoadState;\n" +
                "subroutine void formatPickerStore(in ivec2 coord, in float result);\n" +
                "subroutine uniform formatPickerStore formatPickerStoreState;\n" +
                "\n" +
                "layout(binding = 0, r8) uniform readonly image2D texSrc;\n" +
                "layout(binding = 1, r16) uniform readonly image2D texSrc16;\n" +
                "layout(binding = 2, r8) uniform readonly image2D texIn;\n" +
                "layout(binding = 3, r8) uniform writeonly image2D texOut;\n" +
                "layout(binding = 4, r16) uniform readonly image2D texIn16;\n" +
                "layout(binding = 5, r16) uniform writeonly image2D texOut16;\n" +
                "\n" +
                "uniform ivec3 sizeStep;\n" +
                "uniform int vertical;\n" +
                "uniform vec2 gSigmaSRInv;\n" +
                "\n" +
                "float getWeight(in float diff, in float i) {\n" +
                "    return exp(gSigmaSRInv.x * i * i + gSigmaSRInv.y * diff * diff);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoadSrc) float bit8LoadSrc(in ivec2 coord) {\n" +
                "    return imageLoad(texSrc, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoadSrc) float bit16LoadSrc(in ivec2 coord) {\n" +
                "    return imageLoad(texSrc16, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) float bit8Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) float bit16Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn16, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit8Store(in ivec2 coord, in float result) {\n" +
                "    imageStore(texOut, coord, vec4(result, vec3(0.0)));\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit16Store(in ivec2 coord, in float result) {\n" +
                "    imageStore(texOut16, coord, vec4(result, vec3(0.0)));\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, sizeStep.xy))) return;\n" +
                "    float center = formatPickerLoadSrcState(coord);\n" +
                "    bool isVertical = bool(vertical);\n" +
                "    ivec2 checkSize = ivec2(0, isVertical ? sizeStep.y : sizeStep.x);\n" +
                "    ivec2 coordCurr;\n" +
                "    float curr, diff, sum = 0.0, weightCurr, weight = 0.0;\n" +
                "    for (int i = -sizeStep.z; i <= sizeStep.z; ++i) {\n" +
                "        coordCurr = coord + (isVertical ? ivec2(0, i) : ivec2(i, 0));\n" +
                "        checkSize.x = isVertical ? coordCurr.y : coordCurr.x;\n" +
                "        if (checkSize.x < 0 || checkSize.x >= checkSize.y) continue;\n" +
                "        curr = formatPickerLoadState(coordCurr);\n" +
                "        diff = center - curr;\n" +
                "        weightCurr = getWeight(diff, float(i));\n" +
                "        sum += curr * weightCurr;\n" +
                "        weight += weightCurr;\n" +
                "    }\n" +
                "    formatPickerStoreState(coord, sum / weight);\n" +
                "}";
    }

    public final static class FourierTransform {
        private FourierTransform() {}

        public final static String DFT = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "#define PI2 6.2831853\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "struct ComplexArray {\n" +
                "    vec2 red;\n" +
                "    vec2 green;\n" +
                "    vec2 blue;\n" +
                "    vec2 alpha;\n" +
                "};\n" +
                "\n" +
                "subroutine vec4 formatPickerLoad(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoad formatPickerLoadState;\n" +
                "subroutine void formatPickerStore(in ivec2 coord, in ComplexArray result, in bool isGenRGBA);\n" +
                "subroutine uniform formatPickerStore formatPickerStoreState;\n" +
                "\n" +
                "layout(binding = 0, rgba8) uniform readonly image2D texIn;\n" +
                "layout(binding = 1, rgba8) uniform writeonly image2D texOut;\n" +
                "layout(binding = 2, rgba16f) uniform readonly image2D texIn16;\n" +
                "layout(binding = 3, rgba16f) uniform writeonly image2D texOut16;\n" +
                "layout(binding = 4, rgba32f) uniform readonly image2D texIn32;\n" +
                "layout(binding = 5, rgba32f) uniform writeonly image2D texOut32;\n" +
                "\n" +
                "uniform ivec2 size;\n" +
                "uniform int state; // 0b1 = inverse, 0b10 = vertical, 0b100 = genFromRGBA, 0b1000 = genRGBA;\n" +
                "uniform vec2 sizeDiv;\n" +
                "\n" +
                "vec2 getEuler(in float theta) {\n" +
                "    return vec2(cos(theta), sin(theta));\n" +
                "}\n" +
                "\n" +
                "vec2 cmul(in vec2 left, in vec2 right)\n" +
                "{\n" +
                "    return vec2(left.x * right.x - left.y * right.y, left.x * right.y + left.y * right.x);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) vec4 bit8Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn, coord);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) vec4 bit16Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn16, coord);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) vec4 bit32Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn32, coord);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit8Store(in ivec2 coord, in ComplexArray result, in bool isGenRGBA) {\n" +
                "    vec4 resultStore = isGenRGBA ? vec4(length(result.red), length(result.green), length(result.blue), length(result.alpha)) : vec4(result.red, result.green);\n" +
                "    imageStore(texOut, coord, resultStore);\n" +
                "    if (!isGenRGBA) imageStore(texOut, ivec2(coord.x + size.x, coord.y), vec4(result.blue, result.alpha));\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit16Store(in ivec2 coord, in ComplexArray result, in bool isGenRGBA) {\n" +
                "    vec4 resultStore = isGenRGBA ? vec4(length(result.red), length(result.green), length(result.blue), length(result.alpha)) : vec4(result.red, result.green);\n" +
                "    imageStore(texOut16, coord, resultStore);\n" +
                "    if (!isGenRGBA) imageStore(texOut16, ivec2(coord.x + size.x, coord.y), vec4(result.blue, result.alpha));\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit32Store(in ivec2 coord, in ComplexArray result, in bool isGenRGBA) {\n" +
                "    vec4 resultStore = isGenRGBA ? vec4(length(result.red), length(result.green), length(result.blue), length(result.alpha)) : vec4(result.red, result.green);\n" +
                "    imageStore(texOut32, coord, resultStore);\n" +
                "    if (!isGenRGBA) imageStore(texOut32, ivec2(coord.x + size.x, coord.y), vec4(result.blue, result.alpha));\n" +
                "}\n" +
                "\n" +
                "ComplexArray transformHFromRGBA(in ivec2 coord, in float centerCoord) {\n" +
                "    float theta, coordF = PI2 * centerCoord * sizeDiv.x;\n" +
                "    vec2 euler;\n" +
                "    vec4 raw;\n" +
                "    ComplexArray result = ComplexArray(vec2(0.0), vec2(0.0), vec2(0.0), vec2(0.0));\n" +
                "    for (int n = 0; n < size.x; ++n) {\n" +
                "        theta = float(n) * coordF;\n" +
                "        euler = getEuler(theta);\n" +
                "        raw = formatPickerLoadState(ivec2(n, coord.y));\n" +
                "        result.red += cmul(vec2(raw.x, 0.0), euler);\n" +
                "        result.green += cmul(vec2(raw.y, 0.0), euler);\n" +
                "        result.blue += cmul(vec2(raw.z, 0.0), euler);\n" +
                "        result.alpha += cmul(vec2(raw.w, 0.0), euler);\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "ComplexArray transformH(in ivec2 coord, in float pi, in float centerCoord) {\n" +
                "    float theta, coordF = pi * centerCoord * sizeDiv.x;\n" +
                "    int coordSub = size.x;\n" +
                "    vec2 euler;\n" +
                "    vec4 comp;\n" +
                "    ComplexArray result = ComplexArray(vec2(0.0), vec2(0.0), vec2(0.0), vec2(0.0));\n" +
                "    for (int n = 0; n < size.x; ++n) {\n" +
                "        theta = float(n) * coordF;\n" +
                "        euler = getEuler(theta);\n" +
                "        comp = formatPickerLoadState(ivec2(n, coord.y));\n" +
                "        result.red += cmul(comp.xy, euler);\n" +
                "        result.green += cmul(comp.zw, euler);\n" +
                "        comp = formatPickerLoadState(ivec2(coordSub, coord.y));\n" +
                "        result.blue += cmul(comp.xy, euler);\n" +
                "        result.alpha += cmul(comp.zw, euler);\n" +
                "        ++coordSub;\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "ComplexArray transformV(in ivec2 coord, in float pi, in float centerCoord) {\n" +
                "    float theta, coordF = pi * centerCoord * sizeDiv.y;\n" +
                "    int coordSub = coord.x + size.x;\n" +
                "    vec2 euler;\n" +
                "    vec4 comp;\n" +
                "    ComplexArray result = ComplexArray(vec2(0.0), vec2(0.0), vec2(0.0), vec2(0.0));\n" +
                "    for (int n = 0; n < size.y; ++n) {\n" +
                "        theta = float(n) * coordF;\n" +
                "        euler = getEuler(theta);\n" +
                "        comp = formatPickerLoadState(ivec2(coord.x, n));\n" +
                "        result.red += cmul(comp.xy, euler);\n" +
                "        result.green += cmul(comp.zw, euler);\n" +
                "        comp = formatPickerLoadState(ivec2(coordSub, n));\n" +
                "        result.blue += cmul(comp.xy, euler);\n" +
                "        result.alpha += cmul(comp.zw, euler);\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, size))) return;\n" +
                "    bool isInverse = (state & 1) == 1;\n" +
                "    bool isVertical = (state & 2) == 2;\n" +
                "    bool isGenFromRGBA = (state & 4) == 4;\n" +
                "    bool isGenRGBA = (state & 8) == 8;\n" +
                "    vec2 centerCoord = isVertical ? vec2(coord.y, size.y) : vec2(coord.x, size.x);\n" +
                "    if (!isInverse) centerCoord.x -= centerCoord.y * 0.5;\n" +
                "\n" +
                "    float pi = isInverse ? -PI2 : PI2;\n" +
                "    ComplexArray result = ComplexArray(vec2(0.0), vec2(0.0), vec2(0.0), vec2(0.0));\n" +
                "    if (isGenFromRGBA) {\n" +
                "        result = transformHFromRGBA(coord, centerCoord.x);\n" +
                "    } else {\n" +
                "        result = isVertical ? transformV(coord, pi, centerCoord.x) : transformH(coord, pi, centerCoord.x);\n" +
                "    }\n" +
                "    if (isInverse) {\n" +
                "        vec2 div = vec2(isVertical ? sizeDiv.y : sizeDiv.x);\n" +
                "        result.red *= div;\n" +
                "        result.green *= div;\n" +
                "        result.blue *= div;\n" +
                "        result.alpha *= div;\n" +
                "    }\n" +
                "    formatPickerStoreState(coord, result, isGenRGBA);\n" +
                "}";

        public final static String DFT_RED = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "#define PI2 6.2831853\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine float formatPickerLoad(in ivec2 coord);\n" +
                "subroutine uniform formatPickerLoad formatPickerLoadState;\n" +
                "subroutine void formatPickerStore(in ivec2 coord, in vec2 result, in bool isGenRed);\n" +
                "subroutine uniform formatPickerStore formatPickerStoreState;\n" +
                "\n" +
                "layout(binding = 0, r8) uniform readonly image2D texIn;\n" +
                "layout(binding = 1, r8) uniform writeonly image2D texOut;\n" +
                "layout(binding = 2, r16f) uniform readonly image2D texIn16;\n" +
                "layout(binding = 3, r16f) uniform writeonly image2D texOut16;\n" +
                "layout(binding = 4, r32f) uniform readonly image2D texIn32;\n" +
                "layout(binding = 5, r32f) uniform writeonly image2D texOut32;\n" +
                "\n" +
                "uniform ivec2 size;\n" +
                "uniform int state; // 0b1 = inverse, 0b10 = vertical, 0b100 = genFromRed, 0b1000 = genRed;\n" +
                "uniform vec2 sizeDiv;\n" +
                "\n" +
                "vec2 getEuler(in float theta) {\n" +
                "    return vec2(cos(theta), sin(theta));\n" +
                "}\n" +
                "\n" +
                "vec2 cmul(in vec2 left, in vec2 right)\n" +
                "{\n" +
                "    return vec2(left.x * right.x - left.y * right.y, left.x * right.y + left.y * right.x);\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) float bit8Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) float bit16Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn16, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerLoad) float bit32Load(in ivec2 coord) {\n" +
                "    return imageLoad(texIn32, coord).x;\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit8Store(in ivec2 coord, in vec2 result, in bool isGenRed) {\n" +
                "    vec4 resultStore = isGenRed ? vec4(length(result), vec3(0.0)) : vec4(result.x, vec3(0.0));\n" +
                "    imageStore(texOut, coord, resultStore);\n" +
                "    if (!isGenRed) imageStore(texOut, ivec2(coord.x + size.x, coord.y), vec4(result.y, vec3(0.0)));\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit16Store(in ivec2 coord, in vec2 result, in bool isGenRed) {\n" +
                "    vec4 resultStore = isGenRed ? vec4(length(result), vec3(0.0)) : vec4(result.x, vec3(0.0));\n" +
                "    imageStore(texOut16, coord, resultStore);\n" +
                "    if (!isGenRed) imageStore(texOut16, ivec2(coord.x + size.x, coord.y), vec4(result.y, vec3(0.0)));\n" +
                "}\n" +
                "\n" +
                "subroutine(formatPickerStore) void bit32Store(in ivec2 coord, in vec2 result, in bool isGenRed) {\n" +
                "    vec4 resultStore = isGenRed ? vec4(length(result), vec3(0.0)) : vec4(result.x, vec3(0.0));\n" +
                "    imageStore(texOut32, coord, resultStore);\n" +
                "    if (!isGenRed) imageStore(texOut32, ivec2(coord.x + size.x, coord.y), vec4(result.y, vec3(0.0)));\n" +
                "}\n" +
                "\n" +
                "vec2 transformHFromRed(in ivec2 coord, in float centerCoord) {\n" +
                "    float theta, coordF = PI2 * centerCoord * sizeDiv.x;\n" +
                "    vec2 euler;\n" +
                "    vec2 result = vec2(0.0);\n" +
                "    for (int n = 0; n < size.x; ++n) {\n" +
                "        theta = float(n) * coordF;\n" +
                "        euler = getEuler(theta);\n" +
                "        result += cmul(vec2(formatPickerLoadState(ivec2(n, coord.y)), 0.0), euler);\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "vec2 transformH(in ivec2 coord, in float pi, in float centerCoord) {\n" +
                "    float theta, coordF = pi * centerCoord * sizeDiv.x;\n" +
                "    int coordSub = size.x;\n" +
                "    vec2 euler;\n" +
                "    vec2 comp = vec2(0.0);\n" +
                "    vec2 result = vec2(0.0);\n" +
                "    for (int n = 0; n < size.x; ++n) {\n" +
                "        theta = float(n) * coordF;\n" +
                "        euler = getEuler(theta);\n" +
                "        comp.x = formatPickerLoadState(ivec2(n, coord.y));\n" +
                "        comp.y = formatPickerLoadState(ivec2(coordSub, coord.y));\n" +
                "        result += cmul(comp, euler);\n" +
                "        ++coordSub;\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "vec2 transformV(in ivec2 coord, in float pi, in float centerCoord) {\n" +
                "    float theta, coordF = pi * centerCoord * sizeDiv.y;\n" +
                "    int coordSub = coord.x + size.x;\n" +
                "    vec2 euler;\n" +
                "    vec2 comp = vec2(0.0);\n" +
                "    vec2 result = vec2(0.0);\n" +
                "    for (int n = 0; n < size.y; ++n) {\n" +
                "        theta = float(n) * coordF;\n" +
                "        euler = getEuler(theta);\n" +
                "        comp.x = formatPickerLoadState(ivec2(coord.x, n));\n" +
                "        comp.y = formatPickerLoadState(ivec2(coordSub, n));\n" +
                "        result += cmul(comp, euler);\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, size))) return;\n" +
                "    bool isInverse = (state & 1) == 1;\n" +
                "    bool isVertical = (state & 2) == 2;\n" +
                "    bool isGenFromRed = (state & 4) == 4;\n" +
                "    bool isGenRed = (state & 8) == 8;\n" +
                "    vec2 centerCoord = isVertical ? vec2(coord.y, size.y) : vec2(coord.x, size.x);\n" +
                "    if (!isInverse) centerCoord.x -= centerCoord.y * 0.5;\n" +
                "\n" +
                "    float pi = isInverse ? -PI2 : PI2;\n" +
                "    vec2 result = vec2(0.0);\n" +
                "    if (isGenFromRed) {\n" +
                "        result = transformHFromRed(coord, centerCoord.x);\n" +
                "    } else {\n" +
                "        result = isVertical ? transformV(coord, pi, centerCoord.x) : transformH(coord, pi, centerCoord.x);\n" +
                "    }\n" +
                "    if (isInverse) {\n" +
                "        vec2 div = vec2(isVertical ? sizeDiv.y : sizeDiv.x);\n" +
                "        result *= div;\n" +
                "    }\n" +
                "    formatPickerStoreState(coord, result, isGenRed);\n" +
                "}";
    }

    public final static class NormalMapGen {
        private NormalMapGen() {}

        public final static String INIT = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "subroutine vec4 texInput(in float luminance, in float alpha, in ivec2 coord);\n" +
                "subroutine uniform texInput texInputState;\n" +
                "\n" +
                "layout(binding = 0, rgba16) uniform coherent image2D tex;\n" +
                "layout(binding = 1, r16) uniform readonly image2D volumeMap;\n" +
                "layout(binding = 2, rgba16) uniform readonly image2D detailsMap;\n" +
                "\n" +
                "uniform ivec2 size;\n" +
                "uniform vec4 state[2]; // vec4(srcStrength, srcPowFactor, violumeApply, DetailsApply), vec4(srcBrightness, srcContrast, srcSmoothstepMix, volumeSmoothMix)\n" +
                "\n" +
                "float extraFix(in float x) {\n" +
                "    float result = pow(x, state[0].y);\n" +
                "    result -= 0.5;\n" +
                "    result *= state[1].y;\n" +
                "    result += 0.5;\n" +
                "    result *= state[1].x;\n" +
                "    result = mix(result, smoothstep(0.0, 1.0, result), state[1].z);\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "vec3 fi3(in float a, in float b) {\n" +
                "    return vec3(1.0 - (1.0 - a) * (1.0 - b));\n" +
                "}\n" +
                "\n" +
                "subroutine(texInput) vec4 texOnly(in float luminance, in float alpha, in ivec2 coord) {\n" +
                "    return vec4(vec3(luminance), alpha);\n" +
                "}\n" +
                "\n" +
                "subroutine(texInput) vec4 withVolume(in float luminance, in float alpha, in ivec2 coord) {\n" +
                "    float volume = 1.0 - min(imageLoad(volumeMap, coord).x * 2.0, 1.0);\n" +
                "    volume = (state[1].w > 0.0) ? smoothstep(0.0, 1.0, volume) : volume;\n" +
                "    return vec4(fi3(luminance, volume * state[0].z), alpha);\n" +
                "}\n" +
                "\n" +
                "subroutine(texInput) vec4 withDetails(in float luminance, in float alpha, in ivec2 coord) {\n" +
                "    float details = dot(imageLoad(detailsMap, coord).xyz, vec3(LINEAR_VALUES));\n" +
                "    details = pow(details, state[0].y);\n" +
                "    return vec4(vec3(mix(luminance, extraFix(details), state[0].w)), alpha);\n" +
                "}\n" +
                "\n" +
                "subroutine(texInput) vec4 withBoth(in float luminance, in float alpha, in ivec2 coord) {\n" +
                "    float volume = 1.0 - min(imageLoad(volumeMap, coord).x * 2.0, 1.0);\n" +
                "    volume = (state[1].w > 0.0) ? smoothstep(0.0, 1.0, volume) : volume;\n" +
                "    float details = dot(imageLoad(detailsMap, coord).xyz, vec3(LINEAR_VALUES));\n" +
                "    return vec4(fi3(mix(luminance, extraFix(details), state[0].w), volume * state[0].z), alpha);\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, size))) return;\n" +
                "    vec4 raw = imageLoad(tex, coord);\n" +
                "    float luminance = dot(raw.xyz, vec3(LINEAR_VALUES));\n" +
                "    luminance = extraFix(luminance) * state[0].x;\n" +
                "    imageStore(tex, coord, texInputState(luminance, raw.w, coord));\n" +
                "}";

        public final static String RESULT = "#version 430\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "precision highp int;\n" +
                "\n" +
                "layout(local_size_x = RESET_VALUE, local_size_y = 8, local_size_z = 1) in;\n" +
                "\n" +
                "layout(binding = 0, rgba8) uniform writeonly image2D tex;\n" +
                "layout(binding = 1, rgba16) uniform readonly image2D texHeight;\n" +
                "layout(binding = 2, rgba8) uniform readonly image2D alphaSrc;\n" +
                "\n" +
                "uniform ivec3 sizeState;\n" +
                "uniform float normalStrength;\n" +
                "\n" +
                "float getPixel(in ivec2 offset, in ivec2 coord, in ivec2 maxIndex) {\n" +
                "    return imageLoad(texHeight, clamp(coord + offset, ivec2(0), maxIndex)).x;\n" +
                "}\n" +
                "\n" +
                "vec3 getNormal(in ivec2 coord) {\n" +
                "    ivec2 maxIndex = sizeState.xy - 1;\n" +
                "    float gLT = getPixel(ivec2(-1, 1), coord, maxIndex);\n" +
                "    float gT = getPixel(ivec2(0, 1), coord, maxIndex);\n" +
                "    float gRT = getPixel(ivec2(1, 1), coord, maxIndex);\n" +
                "    float gL = getPixel(ivec2(-1, 0), coord, maxIndex);\n" +
                "    float gR = getPixel(ivec2(1, 0), coord, maxIndex);\n" +
                "    float gLB = getPixel(ivec2(-1, -1), coord, maxIndex);\n" +
                "    float gB = getPixel(ivec2(0, -1), coord, maxIndex);\n" +
                "    float gRB = getPixel(ivec2(1, -1), coord, maxIndex);\n" +
                "\n" +
                "    float gxL = gL * 0.625;\n" +
                "    gxL += (gLT + gLB) * 0.1875;\n" +
                "    float gxR = gR * 0.625;\n" +
                "    gxR += (gRT + gRB) * 0.1875;\n" +
                "    float gyU = gT * 0.625;\n" +
                "    gyU += (gLT + gRT) * 0.1875;\n" +
                "    float gyD = gB * 0.625;\n" +
                "    gyD += (gLB + gRB) * 0.1875;\n" +
                "    bool flipX = (sizeState.z & 2) == 2;\n" +
                "    bool flipY = (sizeState.z & 1) == 1;\n" +
                "    float dx = flipX ? (gxR - gxL) : (gxL - gxR);\n" +
                "    float dy = flipY ? (gyU - gyD) : (gyD - gyU);\n" +
                "    return normalize(vec3(vec2(dx, dy) * normalStrength, 0.05));\n" +
                "}\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                "    if (any(greaterThanEqual(coord, sizeState.xy))) return;\n" +
                "    bool keepAlpha = (sizeState.z & 4) == 4;\n" +
                "    float alpha = imageLoad(alphaSrc, coord).w;\n" +
                "    if (keepAlpha && alpha <= 0.0) return;\n" +
                "    vec3 normal = getNormal(coord);\n" +
                "    normal = clamp(normal * 0.5 + 0.5, 0.0, 1.0);\n" +
                "    imageStore(tex, coord, vec4(normal, keepAlpha ? alpha : 1.0));\n" +
                "}";
    }

    public final static class RadialBlur {
        private RadialBlur() {}

        public final static String VERT = "#version 110\n" +
                "\n" +
                "// vec4(center, radiusSamples, samplesInv)\n" +
                "uniform vec4 statePackage;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "void main() {\n" +
                "\tfragUV = max(gl_Vertex.xy, vec2(0.0)) - statePackage.xy;\n" +
                "\tgl_Position = gl_Vertex;\n" +
                "}\n";

        public final static String FRAG = "#version 110\n" +
                "\n" +
                "// vec4(center, radiusSamples, samplesInv)\n" +
                "uniform vec4 statePackage;\n" +
                "uniform float alphaStrength;\n" +
                "uniform sampler2D tex;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 result = vec4(0.0);\n" +
                "    int limit = int(1.0 / statePackage.w);\n" +
                "    for (int i = 1; i <= limit; ++i) {\n" +
                "        result += texture2D(tex, fragUV * float(i) * statePackage.z + statePackage.xy);\n" +
                "    }\n" +
                "    gl_FragColor = result * statePackage.w * alphaStrength;\n" +
                "}\n";
    }

    public final static class Share {
        private Share() {}

        public final static String POST_VERT = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "layout (location = 0) in vec2 vertex;\n" +
                "\n" +
                "smooth out vec2 fragUV;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tgl_Position = vec4(vertex, 0.0, 1.0);\n" +
                "\tfragUV = max(vertex, vec2(0.0));\n" +
                "}\n";

        public final static String DIRECT_FRAG = "#version OVERWRITE_VERSION\n" +
                "\n" +
                "precision OVERWRITE_PRECISION float;\n" +
                "\n" +
                "in vec2 fragUV;\n" +
                "\n" +
                "layout (binding = 0) uniform sampler2D tex;\n" +
                "uniform float alphaFix;\n" +
                "\n" +
                "layout (location = 0) out vec4 fragColor;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec4 result = texture(tex, fragUV);\n" +
                "    if (alphaFix > -1.0) result.w = alphaFix;\n" +
                "    fragColor = result;\n" +
                "}\n";
    }

    /**
     * For vanilla and fixed pipeline.
     */
    public final static class Number {
        private Number() {}

        public final static String VERT = "#version 110\n" +
                "\n" +
                "// vec2(length), number, invert, vec4(color)\n" +
                "uniform vec4 statePackage[2];\n" +
                "uniform float charLength;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "void main() {\n" +
                "\tvec4 pos = ftransform();\n" +
                "\tif (statePackage[1].w <= 0.0) pos.xyz = vec3(-65536.0);\n" +
                "\tgl_Position = pos;\n" +
                "\tfragUV = vec2(gl_Vertex.x > 0.0 ? 1.0 : 0.0, gl_Vertex.y > 0.0 ? 1.0 : 0.0);\n" +
                "}\n";

        public final static String FRAG = "#version 110\n" +
                "\n" +
                "#define SIZE vec2(3.0, 5.0)\n" +
                "#define SPACING 4.0\n" +
                "#define SIZE_FACTOR SPACING / SIZE.x\n" +
                "#define POINT 2.0\n" +
                "#define NEG 448.0\n" +
                "\n" +
                "// vec2(length), number, invert, vec4(color)\n" +
                "uniform vec4 statePackage[2];\n" +
                "uniform float charLength;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "float getNumber(int digit) {\n" +
                "    float result = 31599.0;\n" +
                "    if (digit == 1) result = 25751.0;\n" +
                "    if (digit == 2) result = 29671.0;\n" +
                "    if (digit == 3) result = 29647.0;\n" +
                "    if (digit == 4) result = 23497.0;\n" +
                "    if (digit == 5) result = 31183.0;\n" +
                "    if (digit == 6) result = 31215.0;\n" +
                "    if (digit == 7) result = 29257.0;\n" +
                "    if (digit == 8) result = 31727.0;\n" +
                "    if (digit == 9) result = 31689.0;\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "float getSprite(float sprite, vec2 uv) {\n" +
                "    uv = floor(uv);\n" +
                "    float bit = (SIZE.x - uv.x - 1.0) + uv.y * SIZE.x;\n" +
                "    bool bounds = all(greaterThanEqual(uv, vec2(0)));\n" +
                "    bounds = bounds && all(lessThan(uv, SIZE));\n" +
                "    return bounds ? floor(mod(sprite / pow(2.0, bit), 2.0)) : 0.0;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = fragUV * SIZE;\n" +
                "    uv.x *= charLength * SIZE_FACTOR;\n" +
                "    vec2 offset = vec2(0.0);\n" +
                "    float number, clip, result, step;\n" +
                "    bool negative = statePackage[0].z < 0.0;\n" +
                "    number = abs(statePackage[0].z);\n" +
                "    clip = result = step = 0.0;\n" +
                "    int digit = 0;\n" +
                "    for(int i = int(statePackage[0].x); i >= -int(statePackage[0].y); i--) {\n" +
                "        clip = float(number > pow(10.0, float(i)) || i == 0);\n" +
                "        digit = int(mod(number / pow(10.0, float(i)), 10.0));\n" +
                "        step = SPACING * clip;\n" +
                "        if (negative && i == int(statePackage[0].x)) {\n" +
                "            result += getSprite(NEG, uv - offset);\n" +
                "            offset.x += SPACING;\n" +
                "        }\n" +
                "        if(statePackage[0].x != 0.0 && i == -1) {\n" +
                "            result += getSprite(POINT, uv - offset) * clip;\n" +
                "            offset.x += step;\n" +
                "        }\n" +
                "        result += getSprite(getNumber(digit), uv - offset) * clip;\n" +
                "        offset.x += step;\n" +
                "    }\n" +
                "    vec4 col = vec4(result);\n" +
                "    if (statePackage[0].w == 1.0) col = 1.0 - col;\n" +
                "    gl_FragColor = col * statePackage[1];\n" +
                "}\n";
    }

    public final static class Arc {
        private Arc() {}

        public final static String VERT = "#version 110\n" +
                "\n" +
                "// vec2(inner), ringHardness, innerHardness, vec4(color)\n" +
                "uniform vec4 statePackage[2];\n" +
                "uniform float arcValue;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "void main() {\n" +
                "\tvec4 pos = ftransform();\n" +
                "\tif (statePackage[1].w <= 0.0) pos.xyz = vec3(-65536.0);\n" +
                "\tgl_Position = pos;\n" +
                "\tfragUV = vec2(gl_Vertex.x > 0.0 ? 1.0 : -1.0, gl_Vertex.y > 0.0 ? 1.0 : -1.0);\n" +
                "}\n";

        public final static String FRAG = "#version 110\n" +
                "\n" +
                "#define RIGHT vec2(1.0, 0.0)\n" +
                "\n" +
                "// vec2(inner), ringHardness, innerHardness, vec4(color)\n" +
                "uniform vec4 statePackage[2];\n" +
                "uniform float arcValue;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "float smoothStep(in float edgeL, in float edgeR, in float value) {\n" +
                "    float result = clamp((value - edgeL) / (edgeR - edgeL), 0.0, 1.0);\n" +
                "    return result * result * (3.0 - 2.0 * result);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    float ring = length(fragUV);\n" +
                "    float inner = length(fragUV / statePackage[0].xy);\n" +
                "    inner = statePackage[0].w >= 1.0 ? step(inner, 1.0) : smoothStep(1.0, statePackage[0].w, inner);\n" +
                "    float result = (statePackage[0].z >= 1.0 ? step(ring, 1.0) : smoothStep(1.0, statePackage[0].z, ring)) - inner;\n" +
                "    if (dot(normalize(fragUV), RIGHT) <= arcValue) result = 0.0;\n" +
                "    gl_FragColor = result * statePackage[1];\n" +
                "}\n";
    }

    public final static class TexArc {
        private TexArc() {}

        public final static String FRAG = "#version 110\n" +
                "\n" +
                "// innerHardness, ringHardness, innerFactor, arc, vec4(color)\n" +
                "uniform vec4 statePackage[2];\n" +
                "uniform sampler2D diffuseMap;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "float smoothStep(in float edgeL, in float edgeR, in float value) {\n" +
                "    float result = clamp((value - edgeL) / (edgeR - edgeL), 0.0, 1.0);\n" +
                "    return result * result * (3.0 - 2.0 * result);\n" +
                "}\n" +
                "\n" +
                "float inverseLerp(float left, float right, float v) {\n" +
                "    return (v - left) * 1.0 / (right - left);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = vec2(inverseLerp(statePackage[0].z, 0.0, 1.0 - length(fragUV)), atan(-fragUV.y, fragUV.x) * -0.5 / 3.14159265 + 0.5);\n" +
                "    float arc = abs(statePackage[0].w);\n" +
                "    uv.x /= arc;\n" +
                "    uv.x -= ((1.0 / arc) - 1.0) * 0.5;\n" +
                "    float mask = uv.y * 2.0 - 1.0;\n" +
                "    float maskV = (mask < 0.0) ? statePackage[0].x : statePackage[0].y;\n" +
                "    mask = (maskV >= 1.0) ? step(abs(mask), 1.0) : smoothstep(1.0, maskV, abs(mask));\n" +
                "    if ((statePackage[0].w > 0.0) && (uv.x < 0.0 || uv.x > 1.0)) mask = 0.0;\n" +
                "    gl_FragColor = texture2D(diffuseMap, uv) * statePackage[1] * mask;\n" +
                "}\n";
    }

    public final static class Mission {
        private Mission() {}

        public final static String VERT = "#version 110\n" +
                "\n" +
                "uniform float time;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "void main() {\n" +
                "\tgl_Position = gl_Vertex;\n" +
                "\tfragUV = max(gl_Vertex.xy, 0.0);\n" +
                "}\n";

        public final static String FRAG = "#version 110\n" +
                "\n" +
                "uniform float time;\n" +
                "\n" +
                "varying vec2 fragUV;\n" +
                "\n" +
                "float smoothStep(in float edgeL, in float edgeR, in float value) {\n" +
                "    float result = clamp((value - edgeL) / (edgeR - edgeL), 0.0, 1.0);\n" +
                "    return result * result * (3.0 - 2.0 * result);\n" +
                "}\n" +
                "\n" +
                "vec2 smoothStep(in float edgeL, in float edgeR, in vec2 value) {\n" +
                "    vec2 result = clamp((value - edgeL) / (edgeR - edgeL), 0.0, 1.0);\n" +
                "    return result * result * (3.0 - 2.0 * result);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = fragUV;\n" +
                "    float line = uv.y - fract(time / 3.0) * 9.0;\n" +
                "    line = smoothStep(0.15, -0.2, abs(line + 0.3)) * 0.2;\n" +
                "    uv.x += uv.y * mix(-0.1, 0.1, uv.x) * 24.0;\n" +
                "    uv *= 16.0;\n" +
                "    uv.x *= 0.25;\n" +
                "    uv.y += time * 0.5;\n" +
                "    uv = abs(fract(uv) - 0.5);\n" +
                "    uv = smoothStep(0.3, 0.75, uv);\n" +
                "    float result = length(uv);\n" +
                "    vec3 mixCol = 0.3 + 0.15 * sin(time + fragUV.y * vec3(4.5, 2.0, 0.3));\n" +
                "    vec3 resultCol = vec3(0.06 + mixCol.x, 0.3 + mixCol.y, mixCol.z) * 0.3 * result + vec3(0.8, 1.0, 0.1) * 2.0 * sqrt(result * line);\n" +
                "    gl_FragColor = vec4(resultCol, 0.8);\n" +
                "}\n";
    }

    private BUtil_ShaderSources() {}
}
