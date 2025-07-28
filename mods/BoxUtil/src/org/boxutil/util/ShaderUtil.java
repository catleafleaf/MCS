package org.boxutil.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.boxutil.base.BaseShaderData;
import org.boxutil.define.BoxEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.boxutil.define.BoxDatabase;
import org.boxutil.manager.ShaderCore;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * For all result texture, recommend to use storage texture for performance.
 */
public final class ShaderUtil {
    private final static Logger _LOG = Global.getLogger(ShaderUtil.class);
    private final static int[] _GEN_SDF_FORMAT = new int[]{GL30.GL_RGBA16UI, GL30.GL_R8, GL30.GL_R16};
    private final static HashMap<Integer, String> _SHADER_TYPE = new HashMap<>();
    private final static HashMap<String, Integer> _SHADER_FORMAT = new HashMap<>();
    static {
        _SHADER_TYPE.put(GL20.GL_VERTEX_SHADER, "Vertex");
        _SHADER_TYPE.put(GL40.GL_TESS_CONTROL_SHADER, "Tess-Control");
        _SHADER_TYPE.put(GL40.GL_TESS_EVALUATION_SHADER, "Tess-Evaluation");
        _SHADER_TYPE.put(GL32.GL_GEOMETRY_SHADER, "Geometry");
        _SHADER_TYPE.put(GL20.GL_FRAGMENT_SHADER, "Fragment");
        _SHADER_TYPE.put(GL43.GL_COMPUTE_SHADER, "Compute");
        _SHADER_FORMAT.put("vert", GL20.GL_VERTEX_SHADER);
        _SHADER_FORMAT.put("vsh", GL20.GL_VERTEX_SHADER);
        _SHADER_FORMAT.put("tesc", GL40.GL_TESS_CONTROL_SHADER);
        _SHADER_FORMAT.put("tese", GL40.GL_TESS_EVALUATION_SHADER);
        _SHADER_FORMAT.put("geom", GL32.GL_GEOMETRY_SHADER);
        _SHADER_FORMAT.put("gsh", GL32.GL_GEOMETRY_SHADER);
        _SHADER_FORMAT.put("frag", GL20.GL_FRAGMENT_SHADER);
        _SHADER_FORMAT.put("fsh", GL20.GL_FRAGMENT_SHADER);
        _SHADER_FORMAT.put("comp", GL43.GL_COMPUTE_SHADER);
        _SHADER_FORMAT.put("csh", GL43.GL_COMPUTE_SHADER);
    }

    public static String getShaderName(int type) {
        if (!_SHADER_TYPE.containsKey(type)) return "Shader type '" + type + "' not found.";
        return _SHADER_TYPE.get(type);
    }

    public static int getTypeFromPath(@NotNull String path) {
        String[] sp = path.split("\\.");
        String format = sp[sp.length - 1];
        return getTypeFromFormat(format);
    }

    public static int getTypeFromFormat(@NotNull String format) {
        String lower = format.toLowerCase();
        if (!_SHADER_FORMAT.containsKey(lower)) return GL11.GL_ZERO;
        return _SHADER_FORMAT.get(lower);
    }

    /**
     * For usual to creating shader program.
     */
    public static int createShaderVFFormPath(@Nullable String loggerTag, String vertPath, String fragPath) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        String vertLocal, fragLocal;
        try {
            vertLocal = Global.getSettings().loadText(vertPath);
            fragLocal = Global.getSettings().loadText(fragPath);
        } catch (IOException ex) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' shader file(s) loading error." + ex.getMessage());
            return 0;
        }
        return createShaderVF(tag, vertLocal, fragLocal);
    }

    /**
     * For usual to creating shader program.
     */
    public static int createShaderVF(@Nullable String loggerTag, String vert, String frag) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        if (!GLContext.getCapabilities().OpenGL20) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.warn("'BoxUtil' platform is not supported shader program.");
            return 0;
        }
        return createShaderProgram(tag, new int[]{GL20.GL_VERTEX_SHADER, GL20.GL_FRAGMENT_SHADER}, vert, frag);
    }

    public static int createShaderVGF(@Nullable String loggerTag, String vert, String geom, String frag) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        if (!GLContext.getCapabilities().OpenGL32 || !GLContext.getCapabilities().GL_ARB_geometry_shader4) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.warn("'BoxUtil' platform is not supported OpenGL3.2.");
            return 0;
        }
        return createShaderProgram(tag, new int[]{GL20.GL_VERTEX_SHADER, GL32.GL_GEOMETRY_SHADER, GL20.GL_FRAGMENT_SHADER}, vert, geom, frag);
    }

    public static int createShaderVTF(@Nullable String loggerTag, String vert, String tessC, String tessE, String frag) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        if (!GLContext.getCapabilities().OpenGL40 || !GLContext.getCapabilities().GL_ARB_tessellation_shader) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.warn("'BoxUtil' platform is not supported OpenGL4.0.");
            return 0;
        }
        return createShaderProgram(tag, new int[]{GL20.GL_VERTEX_SHADER, GL40.GL_TESS_CONTROL_SHADER, GL40.GL_TESS_EVALUATION_SHADER, GL20.GL_FRAGMENT_SHADER}, vert, tessC, tessE, frag);
    }

    public static int createShaderVTGF(@Nullable String loggerTag, String vert, String tessC, String tessE, String geom, String frag) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        if (!GLContext.getCapabilities().OpenGL40 || !GLContext.getCapabilities().GL_ARB_geometry_shader4 || !GLContext.getCapabilities().GL_ARB_tessellation_shader) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.warn("'BoxUtil' platform is not supported OpenGL4.0.");
            return 0;
        }
        return createShaderProgram(tag, new int[]{GL20.GL_VERTEX_SHADER, GL40.GL_TESS_CONTROL_SHADER, GL40.GL_TESS_EVALUATION_SHADER, GL32.GL_GEOMETRY_SHADER, GL20.GL_FRAGMENT_SHADER}, vert, tessC, tessE, geom, frag);
    }

    /**
     * 1-step get a shader program.
     */
    public static int createComputeShadersFormPath(@Nullable String loggerTag, String... shadersPath) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        String[] sources = new String[shadersPath.length];
        try {
            for (int i = 0; i < shadersPath.length; i++) {
                sources[i] = Global.getSettings().loadText(shadersPath[i]);
            }
        } catch (IOException ex) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' shader file(s) loading error." + ex.getMessage());
            return 0;
        }
        return createComputeShaders(tag, sources);
    }

    public static int createComputeShaders(@Nullable String loggerTag, String... source) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        if (!GLContext.getCapabilities().OpenGL43 && GLContext.getCapabilities().GL_ARB_compute_shader) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' platform is not supported OpenGL4.3.");
            return 0;
        }
        int[] types = new int[source.length];
        Arrays.fill(types, GL43.GL_COMPUTE_SHADER);
        return createShaderProgram(tag, types, source);
    }

    /**
     * @param loggerTag for locating in log when created or failed.
     */
    public static int createShader(@Nullable String loggerTag, String shader, int shaderType) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        if (!GLContext.getCapabilities().OpenGL20) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' platform is not supported shader program.");
            return 0;
        }
        String shaderTypeGetter = ShaderUtil._SHADER_TYPE.containsKey(shaderType) ? ShaderUtil._SHADER_TYPE.get(shaderType) : String.valueOf(shaderType);
        int shaderID = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shaderID, shader);
        GL20.glCompileShader(shaderID);
        if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' shader type '" + shaderTypeGetter + "' compilation failed:\n" + GL20.glGetShaderInfoLog(shaderID, GL20.glGetShaderi(shaderID, GL20.GL_INFO_LOG_LENGTH)));
            GL20.glDeleteShader(shaderID);
            return 0;
        } else {
            return shaderID;
        }
    }

    /**
     * @param loggerTag for locating in log when created or failed.
     */
    public static int createShaderProgramFromPath(@Nullable String loggerTag, String... shadersPath) {
        String tag = loggerTag == null ? "None marked" : loggerTag;
        int length = shadersPath.length;
        int[] types = new int[length];
        String[] sources = new String[length];
        try {
            for (int i = 0; i < length; i++) {
                String path = shadersPath[i];
                int type = getTypeFromPath(path);
                if (type == 0) {
                    _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
                    _LOG.error("'BoxUtil' error file format at: '" + path + "'.");
                    return 0;
                }
                sources[i] = Global.getSettings().loadText(path);
                types[i] = type;
            }
        } catch (IOException ex) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' shader file(s) loading error." + ex.getMessage());
            return 0;
        }
        return createShaderProgram(loggerTag, types, sources);
    }

    /**
     * @param loggerTag for locating in log when created or failed.
     */
    public static int createShaderProgram(@Nullable String loggerTag, int[] types, String... shaders) {
        if (!GLContext.getCapabilities().OpenGL20) {
            _LOG.warn("'BoxUtil' platform is not supported OpenGL2.0.");
            return 0;
        }
        String tag = loggerTag == null ? "None marked" : loggerTag;
        if (shaders.length != types.length || shaders.length == 0) {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' shader file's list's length and shader type list's length is mismatching.");
            return 0;
        }
        List<Integer> tmpShaders = new ArrayList<>();
        int programID = GL20.glCreateProgram();
        for (int i = 0; i < shaders.length; i++) {
            int shaderID = createShader(loggerTag, shaders[i], types[i]);
            if (shaderID == 0) {
                String shaderTypeGetter = ShaderUtil._SHADER_TYPE.containsKey(types[i]) ? ShaderUtil._SHADER_TYPE.get(types[i]) : String.valueOf(types[i]);
                _LOG.error("'BoxUtil' shader file error with type: '" + shaderTypeGetter + "', creating program has canceled.");
                GL20.glDeleteProgram(programID);
                return 0;
            }
            GL20.glAttachShader(programID, shaderID);
            tmpShaders.add(shaderID);
        }
        GL20.glLinkProgram(programID);

        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            _LOG.info("'BoxUtil' shader program tag: '" + tag + "'.");
            _LOG.error("'BoxUtil' shader program linking failed:\n" + GL20.glGetProgramInfoLog(programID, GL20.glGetProgrami(programID, GL20.GL_INFO_LOG_LENGTH)));
            GL20.glDeleteProgram(programID);
            if (!tmpShaders.isEmpty()) {
                for (int toDelete : tmpShaders) {
                    GL20.glDetachShader(programID, toDelete);
                    GL20.glDeleteShader(toDelete);
                }
            }
            return 0;
        } else {
            _LOG.info("'BoxUtil' shader creating tag: '" + tag + "'.");
            _LOG.info("'BoxUtil' shader program has created.");
            return programID;
        }
    }

    public static long createBindlessTexture(int texture) {
        if (texture == 0) return 0;
        long id = 0;
        if (BoxDatabase.getGLState().GL_BINDLESS_TEXTURE) {
            if (GLContext.getCapabilities().GL_NV_bindless_texture) {
                id = NVBindlessTexture.glGetTextureHandleNV(texture);
                if (id != 0 && !NVBindlessTexture.glIsTextureHandleResidentNV(id)) NVBindlessTexture.glMakeTextureHandleResidentNV(id);;
            } else {
                id = ARBBindlessTexture.glGetTextureHandleARB(texture);
                if (id != 0 && !ARBBindlessTexture.glIsTextureHandleResidentARB(id)) ARBBindlessTexture.glMakeTextureHandleResidentARB(id);
            }
        }
        return id;
    }

    public static long createBindlessImage(int texture, int internalFormat) {
        return createBindlessImage(texture, 0, false, 0, GL15.GL_READ_WRITE, internalFormat);
    }

    public static long createBindlessImage(int texture, int level, boolean layered, int layer, int access, int internalFormat) {
        if (texture == 0) return 0;
        long id = 0;
        if (BoxDatabase.getGLState().GL_BINDLESS_TEXTURE) {
            if (GLContext.getCapabilities().GL_NV_bindless_texture) {
                id = NVBindlessTexture.glGetImageHandleNV(texture, level, layered, layer, internalFormat);
                if (id != 0 && !NVBindlessTexture.glIsImageHandleResidentNV(id))  NVBindlessTexture.glMakeImageHandleResidentNV(id, access);
            } else {
                id = ARBBindlessTexture.glGetImageHandleARB(texture, level, layered, layer, internalFormat);
                if (id != 0 && !ARBBindlessTexture.glIsImageHandleResidentARB(id)) ARBBindlessTexture.glMakeImageHandleResidentARB(id, access);
            }
        }
        return id;
    }

    public static void releaseBindlessTexture(long texture) {
        if (texture == 0) return;
        if (BoxDatabase.getGLState().GL_BINDLESS_TEXTURE) {
            if (GLContext.getCapabilities().GL_NV_bindless_texture) {
                if (NVBindlessTexture.glIsTextureHandleResidentNV(texture)) NVBindlessTexture.glMakeTextureHandleNonResidentNV(texture);
            } else {
                if (ARBBindlessTexture.glIsTextureHandleResidentARB(texture)) ARBBindlessTexture.glMakeTextureHandleNonResidentARB(texture);
            }
        }
    }

    public static void releaseBindlessImage(long image) {
        if (image == 0) return;
        if (BoxDatabase.getGLState().GL_BINDLESS_TEXTURE) {
            if (GLContext.getCapabilities().GL_NV_bindless_texture) {
                if (NVBindlessTexture.glIsImageHandleResidentNV(image)) NVBindlessTexture.glMakeImageHandleNonResidentNV(image);
            } else {
                if (ARBBindlessTexture.glIsImageHandleResidentARB(image)) ARBBindlessTexture.glMakeImageHandleNonResidentARB(image);
            }
        }
    }

    public static void blitFBO(int read, int draw, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
    }

    public static void blitFBO(int read, int draw, int srcX1, int srcY1, int dstX1, int dstY1) {
        blitFBO(read, draw, 0, 0, srcX1, srcY1, 0, 0, dstX1, dstY1);
    }

    public static void blitFBO(int read, int draw, int width, int height) {
        blitFBO(read, draw, 0, 0, width, height, 0, 0, width, height);
    }

    public static void blitFBO(int read, int draw) {
        final int width = ShaderCore.getScreenScaleWidth();
        final int height = ShaderCore.getScreenScaleHeight();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
    }

    public static void copyFromScreen(int fbo) {
        final int width = ShaderCore.getScreenScaleWidth();
        final int height = ShaderCore.getScreenScaleHeight();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
    }

    public static void blitToScreen(int fbo) {
        final int width = ShaderCore.getScreenScaleWidth();
        final int height = ShaderCore.getScreenScaleHeight();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
    }

    private static void initGLTex(int tex, int internalFormat, int width, int height, int format, int type) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static void initGLStorageTex(int tex, int internalFormat, int width, int height) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL42.glTexStorage2D(GL11.GL_TEXTURE_2D, 1, internalFormat, width, height);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static int[] genSDFCore(int source, int checkChannel, int localWidth, int localHeight, int finalWidth, int finalHeight, int[] border, float outsideThreshold, byte step, float resultInsidePreMultiply, float resultOutsidePreMultiply, int resultTex, boolean genResultTex, boolean bit16OutMode) {
        int[] result = new int[3];
        result[0] = resultTex;
        if (localWidth < 1 || localHeight < 1) return result;
        result[1] = finalWidth;
        result[2] = finalHeight;
        if (!ShaderCore.isSDFGenValid() || !GLContext.getCapabilities().GL_ARB_texture_storage || source < 1 || (resultTex < 1 && !genResultTex)) return result;

        int tmpTex = GL11.glGenTextures();
        initGLStorageTex(tmpTex, _GEN_SDF_FORMAT[0], result[1], result[2]);
        if (genResultTex) {
            byte picker = (byte) (bit16OutMode ? 2 : 1);
            result[0] = GL11.glGenTextures();
            initGLStorageTex(result[0], _GEN_SDF_FORMAT[picker], result[1], result[2]);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        final int itemDimX = (int) Math.ceil(result[1] / (BoxDatabase.isGLDeviceAMD() ? 8.0f : 4.0f));
        final int itemDimY = (int) Math.ceil(result[2] / 8.0f);
        int channelPick = 3;
        switch (checkChannel) {
            case GL11.GL_RED:
                channelPick = 0;
                break;
            case GL11.GL_GREEN:
                channelPick = 1;
                break;
            case GL11.GL_BLUE:
                channelPick = 2;
                break;
            case GL11.GL_ALPHA:
                break;
            case GL11.GL_RGB:
                channelPick = 4;
                break;
        }
        BaseShaderData program = ShaderCore.getSDFInitProgram();
        program.active();
        program.putUniformSubroutine(GL43.GL_COMPUTE_SHADER, 0, channelPick);
        program.putBindingImageTextureReadOnly(0, source, GL11.GL_RGBA8);
        program.putBindingImageTextureWriteOnly(1, tmpTex, GL30.GL_RGBA16I);
        GL20.glUniform4i(program.location[0], localWidth, localHeight, result[1], result[2]);
        GL20.glUniform2i(program.location[1], border[0], border[1]);
        GL20.glUniform1f(program.location[2], outsideThreshold);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        program = ShaderCore.getSDFProcessProgram();
        program.active();
        program.putBindingImageTexture(0, tmpTex, GL30.GL_RGBA16I);
        GL20.glUniform2i(program.location[0], result[1], result[2]);
        for (int i = 1 << Math.min(Math.max(step, 0), 30); i > 0; i = i >>> 1) {
            GL20.glUniform1i(program.location[1], i);
            GL43.glDispatchCompute(itemDimX, itemDimY, 1);
            GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }
        program = ShaderCore.getSDFResultProgram();
        program.active();
        program.putUniformSubroutine(GL43.GL_COMPUTE_SHADER, 0, bit16OutMode ? 1 : 0);
        program.putBindingImageTextureReadOnly(0, tmpTex, GL30.GL_RGBA16I);
        program.putBindingImageTextureWriteOnly(bit16OutMode ? 2 : 1, result[0], bit16OutMode ? GL30.GL_R16 : GL30.GL_R8);
        GL20.glUniform2i(program.location[0], result[1], result[2]);
        GL20.glUniform2f(program.location[1], resultInsidePreMultiply, resultOutsidePreMultiply);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        program.close();
        GL11.glDeleteTextures(tmpTex);
        return result;
    }

    /**
     * Fast GPU SDF generation method.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 texture.
     * @param checkChannel valid value: {@link GL11#GL_RED}, {@link GL11#GL_GREEN}, {@link GL11#GL_BLUE}, {@link GL11#GL_ALPHA}, {@link GL11#GL_RGB}; default is {@link GL11#GL_ALPHA}.
     * @param extraWidth border width for sdf texture, positive integer value required.
     * @param extraHeight border height for sdf texture, positive integer value required.
     * @param outsideThreshold when pixel check value less than or equal the value, it will be considered as outside.
     * @param step 8 or 9 for general usage, range from 0 to 30; also you can use <code>CalculateUtil.getExponentPOTMin(Math.max(localWidth, localHeight))</code> for automatic step calculation.
     * @param resultInsidePreMultiply 0.01 or (1.0f / required thickness) for general usage.
     * @param resultOutsidePreMultiply 0.01 or (1.0f / max(extraWidth, extraHeight)) for general usage.
     * @param resultTex texture to store result, must be R8 texture and size must be greater than or equal to the final size.
     * @param bit16OutMode true for R16 texture, false for R8 texture.
     * @return generated sdf texture with R8/R16 NPOT at [0.0, 1.0], texture return 0 if failed; int[] = {texture, width, height}
     */
    public static int[] genSDF(int source, int checkChannel, int localWidth, int localHeight, int extraWidth, int extraHeight, float outsideThreshold, byte step, float resultInsidePreMultiply, float resultOutsidePreMultiply, int resultTex, boolean bit16OutMode) {
        int[] border = new int[]{Math.abs(extraWidth), Math.abs(extraHeight)};
        return genSDFCore(source, checkChannel, localWidth, localHeight, localWidth + border[0] + border[0], localHeight + border[1] + border[1], border, outsideThreshold, step, resultInsidePreMultiply, resultOutsidePreMultiply, resultTex, false, bit16OutMode);
    }

    /**
     * Fast GPU SDF generation method.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 texture.
     * @param checkChannel valid value: {@link GL11#GL_RED}, {@link GL11#GL_GREEN}, {@link GL11#GL_BLUE}, {@link GL11#GL_ALPHA}, {@link GL11#GL_RGB}; default is {@link GL11#GL_ALPHA}.
     * @param extraWidth border width for sdf texture, positive integer value required.
     * @param extraHeight border height for sdf texture, positive integer value required.
     * @param outsideThreshold when pixel check value less than or equal the value, it will be considered as outside.
     * @param step 8 or 9 for general usage, range from 0 to 30; also you can use <code>CalculateUtil.getExponentPOTMin(Math.max(localWidth, localHeight))</code> for automatic step calculation.
     * @param resultInsidePreMultiply 0.01 or (1.0f / required thickness) for general usage.
     * @param resultOutsidePreMultiply 0.01 or (1.0f / max(extraWidth, extraHeight)) for general usage.
     * @param resultTex texture to store result, must be R8 texture and size must be greater than or equal to the final size.
     * @return generated sdf texture with R8 NPOT at [0.0, 1.0], texture return 0 if failed; int[] = {texture, width, height}
     */
    public static int[] genSDF(int source, int checkChannel, int localWidth, int localHeight, int extraWidth, int extraHeight, float outsideThreshold, byte step, float resultInsidePreMultiply, float resultOutsidePreMultiply, int resultTex) {
        return genSDF(source, checkChannel, localWidth, localHeight, extraWidth, extraHeight, outsideThreshold, step, resultInsidePreMultiply, resultOutsidePreMultiply, resultTex, false);
    }

    /**
     * Fast GPU SDF generation method.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 texture.
     * @param checkChannel valid value: {@link GL11#GL_RED}, {@link GL11#GL_GREEN}, {@link GL11#GL_BLUE}, {@link GL11#GL_ALPHA}, {@link GL11#GL_RGB}; default is {@link GL11#GL_ALPHA}.
     * @param extraWidth border width for sdf texture, positive integer value required.
     * @param extraHeight border height for sdf texture, positive integer value required.
     * @param outsideThreshold when pixel check value less than or equal the value, it will be considered as outside.
     * @param step 8 or 9 for general usage, range from 0 to 30; also you can use <code>CalculateUtil.getExponentPOTMin(Math.max(localWidth, localHeight))</code> for automatic step calculation.
     * @param resultInsidePreMultiply 0.01 or (1.0f / required thickness) for general usage.
     * @param resultOutsidePreMultiply 0.01 or (1.0f / max(extraWidth, extraHeight)) for general usage.
     * @param bit16OutMode true for R16 texture, false for R8 texture.
     * @return generated sdf texture with R8/R16 NPOT at [0.0, 1.0], texture return 0 if failed; int[] = {texture, width, height}
     */
    public static int[] genSDF(int source, int checkChannel, int localWidth, int localHeight, int extraWidth, int extraHeight, float outsideThreshold, byte step, float resultInsidePreMultiply, float resultOutsidePreMultiply, boolean bit16OutMode) {
        int[] border = new int[]{Math.abs(extraWidth), Math.abs(extraHeight)};
        return genSDFCore(source, checkChannel, localWidth, localHeight, localWidth + border[0] + border[0], localHeight + border[1] + border[1], border, outsideThreshold, step, resultInsidePreMultiply, resultOutsidePreMultiply, 0, true, bit16OutMode);
    }

    /**
     * Fast GPU SDF generation method.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 texture.
     * @param checkChannel valid value: {@link GL11#GL_RED}, {@link GL11#GL_GREEN}, {@link GL11#GL_BLUE}, {@link GL11#GL_ALPHA}, {@link GL11#GL_RGB}; default is {@link GL11#GL_ALPHA}.
     * @param extraWidth border width for sdf texture, positive integer value required.
     * @param extraHeight border height for sdf texture, positive integer value required.
     * @param outsideThreshold when pixel check value less than or equal the value, it will be considered as outside.
     * @param step 8 or 9 for general usage, range from 0 to 30; also you can use <code>CalculateUtil.getExponentPOTMin(Math.max(localWidth, localHeight))</code> for automatic step calculation.
     * @param resultInsidePreMultiply 0.01 or (1.0f / required thickness) for general usage.
     * @param resultOutsidePreMultiply 0.01 or (1.0f / max(extraWidth, extraHeight)) for general usage.
     * @return generated sdf texture with R8 NPOT at [0.0, 1.0], texture return 0 if failed; int[] = {texture, width, height}
     */
    public static int[] genSDF(int source, int checkChannel, int localWidth, int localHeight, int extraWidth, int extraHeight, float outsideThreshold, byte step, float resultInsidePreMultiply, float resultOutsidePreMultiply) {
        return genSDF(source, checkChannel, localWidth, localHeight, extraWidth, extraHeight, outsideThreshold, step, resultInsidePreMultiply, resultOutsidePreMultiply, false);
    }

    /**
     * Classical radial blur effect.<p>
     * Draw blur effect form source to attachment of current framebuffer, source texture size should be consistent with attachment of current framebuffer size.<p>
     * <strong>OpenGL 2.0 required.</strong>
     *
     * @param center center of blur effect, mapping range from 0.0 to 1.0.
     * @param samples blur iteration, minimum value is 1 (No any effect), general value is 32, larger is better but slower.
     * @param radius the blur "strength", general value is 0.1, but higher value will need more samples to get better effect.
     * @param alphaStrength default value is 1.0.
     */
    public static void applyRadialBlur(int source, @NotNull Vector2f center, short samples, float radius, float alphaStrength, boolean isAdditiveBlend) {
        if (!ShaderCore.isRadialBlurValid() || source < 1) return;
        BaseShaderData program = ShaderCore.getRadialBlurProgram();
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        program.active();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, isAdditiveBlend ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
        program.bindTexture2D(0, source);
        float samplesInv = 1.0f / Math.max(samples, 1);
        GL20.glUniform4f(program.location[0], center.x, center.y, samplesInv * radius, samplesInv);
        GL20.glUniform1f(program.location[1], alphaStrength);
        GL11.glVertexPointer(2, GL11.GL_BYTE, 0, CommonUtil.createByteBuffer(BoxEnum.NEG_ONE, BoxEnum.NEG_ONE, BoxEnum.ONE, BoxEnum.NEG_ONE, BoxEnum.NEG_ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE));
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        program.close();
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
    }

    /**
     * Classical radial blur effect.<p>
     * Draw blur effect form source to attachment of current framebuffer, source texture size should be consistent with attachment of current framebuffer size.<p>
     * <strong>OpenGL 2.0 required.</strong>
     *
     * @param center center of blur effect, mapping range from 0.0 to 1.0.
     * @param samples blur iteration, minimum value is 1 (No any effect), general value is 32, larger is better but slower.
     * @param radius the blur "strength", general value is 0.1, but higher value will need more samples to get better effect.
     * @param alphaStrength default value is 1.0.
     */
    public static void applyRadialBlur(int source, @NotNull Vector2f center, short samples, float radius, float alphaStrength, int dstX, int dstY, int dstWidth, int dstHeight, boolean isAdditiveBlend) {
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
        GL11.glViewport(dstX, dstY, dstWidth, dstHeight);
        applyRadialBlur(source, center, samples, radius, alphaStrength, isAdditiveBlend);
        GL11.glPopAttrib();
    }

    /**
     * Simple gaussian blur computing, only for RGBA and R texture.
     *
     * @param useRed true for Red texture, false for RGBA texture.
     * @param bit16InMode true for 16bit per channel texture, false for 8bit per channel texture.
     * @param bit16OutMode true for 16bit per channel texture, false for 8bit per channel texture.
     */
    public static void applyImageGaussianBlur(int source, boolean useRed, byte step, int texWidth, int texHeight, int result, boolean bit16InMode, boolean bit16OutMode) {
        if (!ShaderCore.isCompGaussianBlurValid() || source < 1 || result < 1 || texWidth < 1 || texHeight < 1) return;
        BaseShaderData program = useRed ? ShaderCore.getCompGaussianBlurRedProgram() : ShaderCore.getCompGaussianBlurProgram();
        final int itemDimX = (int) Math.ceil(texWidth / (BoxDatabase.isGLDeviceAMD() ? 8.0f : 4.0f));
        final int itemDimY = (int) Math.ceil(texHeight / 8.0f);
        final int formatIn = useRed ? (bit16InMode ? GL30.GL_R16 : GL30.GL_R8) : (bit16InMode ? GL11.GL_RGBA16 : GL11.GL_RGBA8);
        final int formatOut = useRed ? (bit16OutMode ? GL30.GL_R16 : GL30.GL_R8) : (bit16OutMode ? GL11.GL_RGBA16 : GL11.GL_RGBA8);
        final int[] sub = new int[]{
                bit16InMode ? program.subroutineLocation[0][1] : program.subroutineLocation[0][0],
                bit16OutMode ? program.subroutineLocation[0][3] : program.subroutineLocation[0][2]
        };
        float perStep = 1.5219615f / Math.max(step, 1);
        program.active();
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        GL20.glUniform3i(program.location[0], texWidth, texHeight, step);
        GL20.glUniform1i(program.location[1], 0);
        GL20.glUniform1f(program.location[2], perStep);
        program.putBindingImageTextureReadOnly(bit16InMode ? 2 : 0, source, formatIn);
        program.putBindingImageTextureWriteOnly(bit16OutMode ? 3 : 1, result, formatOut);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        GL20.glUniform1i(program.location[1], 1);
        sub[0] = bit16OutMode ? program.subroutineLocation[0][1] : program.subroutineLocation[0][0];
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        program.putBindingImageTextureReadOnly(bit16OutMode ? 2 : 0, result, formatOut);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        program.close();
    }

    /**
     * Simple gaussian blur computing, only for RGBA and R texture.
     *
     * @param useRed true for Red texture, false for RGBA texture.
     * @param bit16InMode true for 16bit per channel texture, false for 8bit per channel texture.
     * @param bit16OutMode true for 16bit per channel texture, false for 8bit per channel texture.
     */
    public static void applyImageGaussianBlur(int source, boolean useRed, byte step, int texWidth, int texHeight, boolean bit16InMode, boolean bit16OutMode) {
        applyImageGaussianBlur(source, useRed, step, texWidth, texHeight, source, bit16InMode, bit16OutMode);
    }

    /**
     * Separated GPU bilateral filter.
     *
     * @param useRed true for Red texture, false for RGBA texture.
     * @param result Not recommended use same result texture as source texture in separated bilateral filter, else will lose some detail and edge.
     * @param bit16InMode true for 16bit per channel texture, false for 8bit per channel texture.
     * @param bit16OutMode true for 16bit per channel texture, false for 8bit per channel texture.
     */
    public static void applyImageBilateralFilter(int source, boolean useRed, byte radius, float sigmaSpace, float sigmaRange, int texWidth, int texHeight, int result, boolean bit16InMode, boolean bit16OutMode) {
        if (!ShaderCore.isCompBilateralFilterValid() || source < 1 || result < 1 || texWidth < 1 || texHeight < 1) return;
        BaseShaderData program = useRed ? ShaderCore.getCompBilateralFilterRedProgram() : ShaderCore.getCompBilateralFilterProgram();
        final int itemDimX = (int) Math.ceil(texWidth / (BoxDatabase.isGLDeviceAMD() ? 8.0f : 4.0f));
        final int itemDimY = (int) Math.ceil(texHeight / 8.0f);
        final int formatIn = useRed ? (bit16InMode ? GL30.GL_R16 : GL30.GL_R8) : (bit16InMode ? GL11.GL_RGBA16 : GL11.GL_RGBA8);
        final int formatOut = useRed ? (bit16OutMode ? GL30.GL_R16 : GL30.GL_R8) : (bit16OutMode ? GL11.GL_RGBA16 : GL11.GL_RGBA8);
        final int[] sub = new int[]{
                bit16InMode ? program.subroutineLocation[0][1] : program.subroutineLocation[0][0],
                bit16OutMode ? program.subroutineLocation[0][3] : program.subroutineLocation[0][2],
                bit16InMode ? program.subroutineLocation[0][5] : program.subroutineLocation[0][4]
        };
        float gsInv = sigmaSpace * sigmaSpace;
        gsInv = -1.0f / (gsInv + gsInv);
        float grInv = sigmaRange * sigmaRange;
        grInv = -1.0f / (grInv + grInv);
        program.active();
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        program.putBindingImageTextureReadOnly(bit16InMode ? 1 : 0, source, formatIn);
        program.putBindingImageTextureReadOnly(bit16InMode ? 4 : 2, source, formatIn);
        program.putBindingImageTextureWriteOnly(bit16OutMode ? 5 : 3, result, formatOut);
        GL20.glUniform3i(program.location[0], texWidth, texHeight, radius);
        GL20.glUniform1i(program.location[1], 0);
        GL20.glUniform2f(program.location[2], gsInv, grInv);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        sub[0] = bit16OutMode ? program.subroutineLocation[0][1] : program.subroutineLocation[0][0];
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        program.putBindingImageTextureReadOnly(bit16OutMode ? 4 : 2, result, formatOut);
        GL20.glUniform1i(program.location[1], 1);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        program.close();
    }

    /**
     * Separated GPU bilateral filter.<p>
     * Not recommended use this method, it uses same result texture as source texture that will lose some detail and edge.
     *
     * @param useRed true for Red texture, false for RGBA texture.
     * @param bit16InMode true for 16bit per channel texture, false for 8bit per channel texture.
     * @param bit16OutMode true for 16bit per channel texture, false for 8bit per channel texture.
     */
    public static void applyImageBilateralFilter(int source, boolean useRed, byte radius, float sigmaSpace, float sigmaRange, int texWidth, int texHeight, boolean bit16InMode, boolean bit16OutMode) {
        applyImageBilateralFilter(source, useRed, radius, sigmaSpace, sigmaRange, texWidth, texHeight, source, bit16InMode, bit16OutMode);
    }

    private static int imageDFTCore(int source, boolean useRed, int texWidth, int texHeight, int result, boolean genResultTex, boolean f16InMode, boolean f16OutMode) {
        if (!ShaderCore.isDiscreteFourierValid() || !GLContext.getCapabilities().GL_ARB_texture_storage || source < 1 || (!genResultTex && result < 1) || texWidth < 1 || texHeight < 1) return 0;
        final int itemDimX = (int) Math.ceil(texWidth / (BoxDatabase.isGLDeviceAMD() ? 8.0f : 4.0f));
        final int itemDimY = (int) Math.ceil(texHeight / 8.0f);
        final int formatIn = useRed ? (f16InMode ? GL30.GL_R16F : GL30.GL_R8) : (f16InMode ? GL30.GL_RGBA16F : GL11.GL_RGBA8);
        final int formatOut = useRed ? (f16OutMode ? GL30.GL_R16F : GL30.GL_R32F) : (f16OutMode ? GL30.GL_RGBA16F : GL30.GL_RGBA32F);

        int tmpTex = GL11.glGenTextures();
        initGLStorageTex(tmpTex, formatOut, texWidth + texWidth, texHeight);
        if (genResultTex) {
            result = GL11.glGenTextures();
            initGLStorageTex(result, formatOut, texWidth + texWidth, texHeight);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        BaseShaderData program = useRed ? ShaderCore.getDFTRedProgram() : ShaderCore.getDFTProgram();
        final int[] sub = new int[]{
                f16InMode ? program.subroutineLocation[0][1] : program.subroutineLocation[0][0],
                f16OutMode ? program.subroutineLocation[0][4] : program.subroutineLocation[0][5],
        };
        program.active();
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        program.putBindingImageTextureReadOnly(f16InMode ? 2 : 0, source, formatIn);
        program.putBindingImageTextureWriteOnly(f16OutMode ? 3 : 5, tmpTex, formatOut);
        GL20.glUniform2i(program.location[0], texWidth, texHeight);
        GL20.glUniform1i(program.location[1], 0b100);
        GL20.glUniform2f(program.location[2], 1.0f / texWidth, 1.0f / texHeight);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        sub[0] = f16OutMode ? program.subroutineLocation[0][1] : program.subroutineLocation[0][2];
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        program.putBindingImageTextureReadOnly(f16OutMode ? 2 : 4, tmpTex, formatOut);
        program.putBindingImageTextureWriteOnly(f16OutMode ? 3 : 5, result, formatOut);
        GL20.glUniform1i(program.location[1], 0b10);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        program.close();
        GL11.glDeleteTextures(tmpTex);
        return result;
    }

    private static int imageIDFTCore(int source, boolean useRed, int texWidth, int texHeight, int result, boolean genResultTex, boolean f16InMode, boolean f16OutMode) {
        if (!ShaderCore.isDiscreteFourierValid() || !GLContext.getCapabilities().GL_ARB_texture_storage || source < 1 || (!genResultTex && result < 1) || texWidth < 1 || texHeight < 1) return 0;
        final int itemDimX = (int) Math.ceil(texWidth / (BoxDatabase.isGLDeviceAMD() ? 8.0f : 4.0f));
        final int itemDimY = (int) Math.ceil(texHeight / 8.0f);
        final int formatIn = useRed ? (f16InMode ? GL30.GL_R16F : GL30.GL_R32F) : (f16InMode ? GL30.GL_RGBA16F : GL30.GL_RGBA32F);
        final int formatOut = useRed ? (f16OutMode ? GL30.GL_R16F : GL30.GL_R8) : (f16OutMode ? GL30.GL_RGBA16F : GL11.GL_RGBA8);

        int tmpTex = GL11.glGenTextures();
        initGLStorageTex(tmpTex, formatIn, texWidth + texWidth, texHeight);
        if (genResultTex) {
            result = GL11.glGenTextures();
            initGLStorageTex(result, formatOut, texWidth, texHeight);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        BaseShaderData program = useRed ? ShaderCore.getDFTRedProgram() : ShaderCore.getDFTProgram();
        int[] sub = f16InMode ? new int[]{
                program.subroutineLocation[0][1],
                program.subroutineLocation[0][4],
        } : new int[]{
                program.subroutineLocation[0][2],
                program.subroutineLocation[0][5],
        };
        program.active();
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        program.putBindingImageTextureReadOnly(f16InMode ? 2 : 4, source, formatIn);
        program.putBindingImageTextureWriteOnly(f16InMode ? 3 : 5, tmpTex, formatIn);
        GL20.glUniform2i(program.location[0], texWidth, texHeight);
        GL20.glUniform1i(program.location[1], 0b1);
        GL20.glUniform2f(program.location[2], 1.0f / texWidth, 1.0f / texHeight);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        sub[1] = f16OutMode ? program.subroutineLocation[0][4] : program.subroutineLocation[0][3];
        program.putUniformSubroutines(GL43.GL_COMPUTE_SHADER, 0, sub);
        program.putBindingImageTextureReadOnly(f16InMode ? 2 : 4, tmpTex, formatIn);
        program.putBindingImageTextureWriteOnly(f16OutMode ? 3 : 1, result, formatOut);
        GL20.glUniform1i(program.location[1], 0b1011);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        program.close();
        GL11.glDeleteTextures(tmpTex);
        return result;
    }

    /**
     * GPU DFT(Discrete Fourier Transform), slow but still faster more than CPU computing.
     *
     * @param useRed true for Red texture, false for RGBA texture.
     * @param texWidth spatial domain texture width, <strong>NOT</strong> frequency domain texture width.
     * @param f16InMode true for f16 per channel texture, false for 8bit per channel texture.
     * @param f16OutMode true for f16 per channel texture, false for f32 per channel texture.
     *
     * @return generated centered f16/f32 complex spectrum texture;<p>
     *     R with double size(left area R for real part of complex, right area R for imaginary part of complex) for useRed;<p>
     *     RGBA with double size(left area R+iG/B+iA complex, right area R+iG/B+iA complex) for not useRed;<p>
     *     returns 0 if failed.
     */
    public static int imageDFT(int source, boolean useRed, int texWidth, int texHeight, int result, boolean f16InMode, boolean f16OutMode) {
        return imageDFTCore(source, useRed, texWidth, texHeight, result, false, f16InMode, f16OutMode);
    }

    /**
     * GPU DFT(Discrete Fourier Transform), slow but still faster more than CPU computing.
     *
     * @param useRed true for Red texture, false for RGBA texture.
     * @param texWidth spatial domain texture width, <strong>NOT</strong> frequency domain texture width.
     * @param f16InMode true for f16 per channel texture, false for 8bit per channel texture.
     * @param f16OutMode true for f16 per channel texture, false for f32 per channel texture.
     *
     * @return generated centered f16/f32 complex spectrum texture;<p>
     *     R with double size(left area R for real part of complex, right area R for imaginary part of complex) for useRed;<p>
     *     RGBA with double size(left area R+iG/B+iA complex, right area R+iG/B+iA complex) for not useRed;<p>
     *     returns 0 if failed.
     */
    public static int imageDFT(int source, boolean useRed, int texWidth, int texHeight, boolean f16InMode, boolean f16OutMode) {
        return imageDFTCore(source, useRed, texWidth, texHeight, 0, true, f16InMode, f16OutMode);
    }

    /**
     * GPU IDFT(Inverse Discrete Fourier Transform), slow but still faster more than CPU computing.
     *
     * @param useRed true for double size Red complex, false for double size RGBA complex.
     * @param texWidth spatial domain texture width, <strong>NOT</strong> frequency domain texture width.
     * @param f16InMode true for f16 per channel texture, false for f32 per channel texture.
     * @param f16OutMode true for f16 per channel texture, false for 8bit per channel texture.
     *
     * @return generated 8bit/16bit per channel texture from complex spectrum, R for double size R complex, RGBA for double size RGBA complex, if failed return 0.
     */
    public static int imageIDFT(int source, boolean useRed, int texWidth, int texHeight, int result, boolean f16InMode, boolean f16OutMode) {
        return imageIDFTCore(source, useRed, texWidth, texHeight, result, false, f16InMode, f16OutMode);
    }

    /**
     * GPU IDFT(Inverse Discrete Fourier Transform), slow but still faster more than CPU computing.
     *
     * @param useRed true for double size Red complex, false for double size RGBA complex.
     * @param texWidth spatial domain texture width, <strong>NOT</strong> frequency domain texture width.
     * @param f16InMode true for f16 per channel texture, false for f32 per channel texture.
     * @param f16OutMode true for f16 per channel texture, false for 8bit per channel texture.
     *
     * @return generated 8bit/16bit per channel texture from complex spectrum, R for double size R complex, RGBA for double size RGBA complex, if failed return 0.
     */
    public static int imageIDFT(int source, boolean useRed, int texWidth, int texHeight, boolean f16InMode, boolean f16OutMode) {
        return imageIDFTCore(source, useRed, texWidth, texHeight, 0, true, f16InMode, f16OutMode);
    }

    public static class NormalMapGenParam {
        /**
         * 1.0 for no effect and it is general value, and effects details.
         */
        public float srcPowFactor = 1.0f;
        /**
         * The details weight of source texture in normal map.
         */
        public float srcStrength = 0.8f;
        /**
         * Effects source and details.
         */
        public float srcBrightness = 1.0f;
        /**
         * Effects source and details.
         */
        public float srcContrast = 1.0f;
        /**
         * Effects source and details.
         */
        public float srcSmoothstepMix = 0.66667f;
        /**
         * For bilateral filter
         */
        public byte filterRadius = 13;
        /**
         * For bilateral filter
         */
        public float filterSigmaSpace = 7.0f;
        /**
         * For bilateral filter, set to less than or equal 0 that filter disabled.
         */
        public float filterSigmaRange = 0.05f;
        /**
         * The blur 'strength' of soure texture.
         */
        public byte srcBlurStep = 2;
        /**
         * Nullable, {scale, blurStep(integer), applyStrength}, set null that disabled; the scale general value is 0.01~0.02 for generate.
         */
        public Vector3f volume = new Vector3f(0.01f, 7.0f, 0.85f);
        /**
         * Apply edge smooth to the 'bottom' and 'top' of volume map.
         */
        public boolean volumeSmoothMix = true;
        /**
         * Nullable, {blurStep(integer), applyMix}, set null that disabled; general 12 step for chunk surface.
         */
        public Vector2f details = new Vector2f(12.0f, 0.7f);
        /**
         * General value is 2.5 and recommended 5.0 for maximum.
         */
        public float normalStrength = 2.5f;
        /**
         * True for default.
         */
        public boolean keepSrcAlpha = true;
        /**
         * False for default.
         */
        public boolean flipX = false;
        /**
         * True for default.
         */
        public boolean flipY = true;
        /**
         * Gen POT texture if needed.
         */
        public boolean alignPOT = false;
        /**
         * Const texture, faster than other texture.
         */
        public boolean useTextureStorage = true;
    }

    private static int genNormalMapFromRGBCore(int source, int srcLocalWidth, int srcLocalHeight, byte srcBlurStep, float srcPowFactor, float srcStrength, float srcBrightness, float srcContrast, float srcSmoothstepMix, byte srcBFRadius, float srcBFSigmaSpace, float srcBFSigmaRange, @Nullable Vector3f volume, boolean volumeSmoothMix, @Nullable Vector2f details, float normalStrength, boolean keepSrcAlpha, boolean flipX, boolean flipY, int resultTex, boolean genResultTex, boolean alignPOT, boolean useTextureStorage) {
        int result = resultTex;
        int resultTmp = GL11.glGenTextures();
        if (!ShaderCore.isNormalMapGenValid() || !ShaderCore.isCompGaussianBlurValid() || !GLContext.getCapabilities().GL_ARB_texture_storage || source < 1 || srcLocalWidth < 1 || srcLocalHeight < 1 || (resultTex < 1 && !genResultTex) || resultTmp < 1) return result;
        initGLStorageTex(resultTmp, GL11.GL_RGBA16, srcLocalWidth, srcLocalHeight);
        if (genResultTex) {
            result = GL11.glGenTextures();
            int resultWidth = alignPOT ? CalculateUtil.getPOTMax(srcLocalWidth) : srcLocalWidth, resultHeight = alignPOT ? CalculateUtil.getPOTMax(srcLocalHeight) : srcLocalHeight;
            if (useTextureStorage) initGLStorageTex(result, GL11.GL_RGBA8, resultWidth, resultHeight);
            else initGLTex(result, GL11.GL_RGBA8, resultWidth, resultHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        int[] detailsTex = new int[2];
        if (volume != null && volume.x > 0.0f && volume.z > 0.0f) {
            detailsTex[0] = genSDF(source, GL11.GL_ALPHA, srcLocalWidth, srcLocalHeight, 0, 0, 0.5f, CalculateUtil.getExponentPOTMin(Math.max(srcLocalWidth, srcLocalHeight)), volume.x, 0.0f, true)[0];
            if (volume.y > 0.0f && detailsTex[0] > 0) applyImageGaussianBlur(detailsTex[0], true, (byte) Math.min(Math.ceil(volume.y), 127), srcLocalWidth, srcLocalHeight, true, true);
        }
        boolean haveBF = srcBFSigmaRange > 0.0f;
        if (haveBF) applyImageBilateralFilter(source, false, (srcBFRadius > 0) ? srcBFRadius : (byte) Math.min((int) Math.ceil(Math.abs(srcBFSigmaSpace) + 0.1f), 32), srcBFSigmaSpace, srcBFSigmaRange, srcLocalWidth, srcLocalHeight, resultTmp, false, true);
        if (details != null && details.x > 0.0f && details.y > 0.0f) {
            detailsTex[1] = GL11.glGenTextures();
            initGLStorageTex(detailsTex[1], GL11.GL_RGBA16, srcLocalWidth, srcLocalHeight);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            applyImageGaussianBlur(haveBF ? resultTmp : source, false, (byte) Math.min(Math.ceil(details.x), 127), srcLocalWidth, srcLocalHeight, detailsTex[1], haveBF, true);
        }
        applyImageGaussianBlur(haveBF ? resultTmp : source, false, srcBlurStep, srcLocalWidth, srcLocalHeight, resultTmp, haveBF, true);

        final int itemDimX = (int) Math.ceil(srcLocalWidth / (BoxDatabase.isGLDeviceAMD() ? 8.0f : 4.0f));
        final int itemDimY = (int) Math.ceil(srcLocalHeight / 8.0f);
        boolean volumeValid = detailsTex[0] > 0, detailsValid = detailsTex[1] > 0;
        int subroutine = 0;
        if (volumeValid) subroutine |= 0b1;
        if (detailsValid) subroutine |= 0b10;
        BaseShaderData program = ShaderCore.getNormalMapGenInitProgram();
        program.active();
        program.putUniformSubroutine(GL43.GL_COMPUTE_SHADER, 0, subroutine);
        GL20.glUniform2i(program.location[0], srcLocalWidth, srcLocalHeight);
        GL20.glUniform4(program.location[1], CommonUtil.createFloatBuffer(srcStrength, srcPowFactor, volume != null ? volume.z : 0.0f, details != null ? details.y : 0.0f, srcBrightness, srcContrast, srcSmoothstepMix, volumeSmoothMix ? 1.0f : 0.0f));
        program.putBindingImageTexture(0, resultTmp, GL11.GL_RGBA16);
        if (volumeValid) program.putBindingImageTextureReadOnly(1, detailsTex[0], GL30.GL_R16);
        if (detailsValid) program.putBindingImageTextureReadOnly(2, detailsTex[1], GL11.GL_RGBA16);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        int resultStateBit = flipY ? 0b1 : 0b0;
        if (flipX) resultStateBit |= 0b10;
        if (keepSrcAlpha) resultStateBit |= 0b100;
        program = ShaderCore.getNormalMapGenResultProgram();
        program.active();
        GL20.glUniform3i(program.location[0], srcLocalWidth, srcLocalHeight, resultStateBit);
        GL20.glUniform1f(program.location[1], normalStrength);
        program.putBindingImageTextureWriteOnly(0, result, GL11.GL_RGBA8);
        program.putBindingImageTextureReadOnly(1, resultTmp, GL11.GL_RGBA16);
        program.putBindingImageTextureReadOnly(2, source, GL11.GL_RGBA8);
        GL43.glDispatchCompute(itemDimX, itemDimY, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        program.close();
        GL11.glDeleteTextures(resultTmp);
        if (volumeValid) GL11.glDeleteTextures(detailsTex[0]);
        if (detailsValid) GL11.glDeleteTextures(detailsTex[1]);
        return result;
    }

    /**
     * GPU normal map generation from RGB texture for sprites.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 2D-texture.
     * @param srcPowFactor 1.0 for no effect and it is general value, and effects details.
     * @param volume {scale, blurStep(integer), applyStrength}, set null that disabled; the scale general value is 0.01~0.02 for generate.
     * @param details {blurStep(integer), applyMix}, set null that disabled; general 12 step for chunk surface.
     * @param normalStrength general value is 2.5 and recommended 5.0 for maximum.
     * @param keepSrcAlpha true for default.
     * @param flipY true for default.
     *
     * @return returns NPOT-texture.
     */
    @Deprecated
    public static int genNormalMapFromRGB(int source, int srcLocalWidth, int srcLocalHeight, byte srcBlurStep, float srcPowFactor, float srcStrength, @Nullable Vector3f volume, @Nullable Vector2f details, float normalStrength, boolean keepSrcAlpha, boolean flipY, int resultTex) {
        return genNormalMapFromRGBCore(source, srcLocalWidth, srcLocalHeight, srcBlurStep, srcPowFactor, srcStrength, 1.0f, 1.0f, 0.75f, (byte) 13, 7.0f, 0.05f, volume, true, details, normalStrength, keepSrcAlpha, false, flipY, resultTex, false, false, true);
    }

    /**
     * GPU normal map generation from RGB texture for sprites.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 2D-texture.
     * @param srcPowFactor 1.0 for no effect and it is general value, and effects details.
     * @param volume {scale, blurStep(integer), applyStrength}, set null that disabled; the scale general value is 0.01~0.02 for generate.
     * @param details {blurStep(integer), applyMix}, set null that disabled; general 12 step for chunk surface.
     * @param normalStrength general value is 2.5 and recommended 5.0 for maximum.
     * @param keepSrcAlpha true for default.
     * @param flipY true for default.
     *
     * @return returns NPOT-texture.
     */
    @Deprecated
    public static int genNormalMapFromRGB(int source, int srcLocalWidth, int srcLocalHeight, byte srcBlurStep, float srcPowFactor, float srcStrength, @Nullable Vector3f volume, @Nullable Vector2f details, float normalStrength, boolean keepSrcAlpha, boolean flipY) {
        return genNormalMapFromRGBCore(source, srcLocalWidth, srcLocalHeight, srcBlurStep, srcPowFactor, srcStrength, 1.0f, 1.0f, 0.75f, (byte) 13, 7.0f, 0.05f, volume, true, details, normalStrength, keepSrcAlpha, false, flipY, 0, true, false, true);
    }

    private static int genNormalMapFromRGBParamCore(int source, int srcLocalWidth, int srcLocalHeight, NormalMapGenParam param, int resultTex, boolean genResultTex) {
        return genNormalMapFromRGBCore(source, srcLocalWidth, srcLocalHeight, param.srcBlurStep, param.srcPowFactor, param.srcStrength, param.srcBrightness, param.srcContrast, param.srcSmoothstepMix, param.filterRadius, param.filterSigmaSpace, param.filterSigmaRange, param.volume, param.volumeSmoothMix, param.details, param.normalStrength, param.keepSrcAlpha, param.flipX, param.flipY, resultTex, genResultTex, param.alignPOT, param.useTextureStorage);
    }

    /**
     * GPU normal map generation from RGB texture for sprites.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 2D-texture.
     *
     * @return returns NPOT-texture.
     */
    public static int genNormalMapFromRGB(int source, int srcLocalWidth, int srcLocalHeight, NormalMapGenParam param, int resultTex) {
        return genNormalMapFromRGBParamCore(source, srcLocalWidth, srcLocalHeight, param, resultTex, false);
    }

    /**
     * GPU normal map generation from RGB texture for sprites.<p>
     * <strong>OpenGL 4.3 required, compute shader supported required.</strong>
     *
     * @param source must be RGBA8 2D-texture.
     *
     * @return returns NPOT-texture.
     */
    public static int genNormalMapFromRGB(int source, int srcLocalWidth, int srcLocalHeight, NormalMapGenParam param) {
        return genNormalMapFromRGBParamCore(source, srcLocalWidth, srcLocalHeight, param, 0, true);
    }

    private ShaderUtil() {}
}
