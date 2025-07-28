package org.boxutil.manager;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.boxutil.base.BaseShaderData;
import org.boxutil.base.api.SimpleVAOAPI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.units.builtin.buffer.BUtil_RenderingBuffer;
import org.boxutil.units.builtin.shader.BUtil_ShaderProgram;
import org.boxutil.units.builtin.shader.BUtil_ShaderSources;
import org.boxutil.units.standard.misc.LineObject;
import org.boxutil.units.standard.misc.PointObject;
import org.boxutil.units.standard.misc.PublicFBO;
import org.boxutil.units.standard.misc.QuadObject;
import org.boxutil.util.CalculateUtil;
import org.boxutil.util.CommonUtil;
import org.boxutil.util.ShaderUtil;
import org.boxutil.util.TransformUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;

public final class ShaderCore {
    private final static byte _SHADER_COUNT = 30;
    private final static byte _COMMON = 0;
    private final static byte _SPRITE = 1;
    private final static byte _CURVE = 2;
    private final static byte _SEGMENT = 3;
    private final static byte _TRAIL = 4;
    private final static byte _FLARE = 5;
    private final static byte _TEXT = 6;
    private final static byte _DIST = 7;
    private final static byte _DIRECT = 8;
    private final static byte _BLOOM = 9;
    private final static byte _FXAA_C = 10;
    private final static byte _FXAA_Q = 11;
    private final static byte _MATRIX_F32 = 12;
    private final static byte _MATRIX_F16 = 13;
    private final static byte _SDF_INIT = 14;
    private final static byte _SDF_PROCESS = 15;
    private final static byte _SDF_RESULT = 16;
    private final static byte _RADIAL_BLUR = 17;
    private final static byte _GAUSSIAN_BLUR = 18;
    private final static byte _GAUSSIAN_BLUR_RED = 19;
    private final static byte _BILATERAL_FLITER = 20;
    private final static byte _BILATERAL_FLITER_RED = 21;
    private final static byte _DFT = 22;
    private final static byte _DFT_RED = 23;
    private final static byte _NORMAL_GEN_INIT = 24;
    private final static byte _NORMAL_GEN_RESULT = 25;
    private final static byte _SIMPLE_NUMBER = 26;
    private final static byte _SIMPLE_ARC = 27;
    private final static byte _SIMPLE_TEX_ARC = 28;
    private final static byte _MISSION_BG = 29;
    private final static String _GLSL_VERSION = "420";
    private final static String _GLSL_VERSION_TITLE = "OVERWRITE_VERSION";
    private final static String _GLSL_PRECISION = "highp";
    private final static String _GLSL_PRECISION_TITLE = "OVERWRITE_PRECISION";
    private final static String _GLSL_MATRIX_UBO_TITLE = "OVERWRITE_MATRIX_UBO";
    private final static String _GLSL_INSTANCE_FORMAT_TITLE = "INSTANCE_FORMAT";
    private final static String _GLSL_INSTANCE_FORMAT_F32 = "rgba32f";
    private final static String _GLSL_INSTANCE_FORMAT_F16 = "rgba16f";
    private final static String _GLSL_WORKGROUP_SIZE_TITLE = "WORKGROUP_SIZE_VALUE";
    private final static String _GLSL_BLOOM_RADIUS_TITLE = "OVERWRITE_RADIUS_SCALE";
    private final static String _GLSL_WEIGHTED_LUMINANCE_TITLE = "LINEAR_VALUES";
    private final static String _GLSL_COMPUTE_DIM_REPLACE_TITLE = "RESET_VALUE";
    private final static byte _GLSL_MATRIX_UBO_BINDING = 0;
    private final static FloatBuffer _NONE_GAME_MATRIX = CommonUtil.createFloatBuffer(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
    private final static BaseShaderData[] _SHADER_PROGRAM = new BaseShaderData[_SHADER_COUNT];
    private static BUtil_RenderingBuffer renderingBuffer = null;
    private static PublicFBO publicFBO = null;
    private static SimpleVAOAPI defaultPointObject = null;
    private static SimpleVAOAPI defaultLineObject = null;
    private static SimpleVAOAPI defaultQuadObject = null;
    private static int matrixUBO = 0;
    private static boolean _miscShaderInit = false;
    private static boolean glValid = false;
    private static boolean glFinished = false;
    private static boolean glMainProgramValid = true;
    private static boolean glDistortionValid = false;
    private static boolean glBloomValid = false;
    private static boolean glFXAACValid = false;
    private static boolean glFXAAQValid = false;
    private static boolean glInstanceMatrixValid = false;
    private static boolean glSDFGenValid = false;
    private static boolean glRadialBlurValid = false;
    private static boolean glCompGaussianBlurValid = false;
    private static boolean glCompBilateralFilterValid = false;
    private static boolean glDiscreteFourierValid = false;
    private static boolean glNormalMapGenValid = false;
    private static final int[] screenSize = new int[2];
    private static final int[] screenSizeScale = new int[2];
    private static final int[] screenSizeFix = new int[2];
    private static final float[] screenSizeUV = new float[2];

    /**
     * Loading after {@link BoxConfigs#init()}.
     */
    public static void init() {
        if (glFinished) return;
        Global.getLogger(ShaderCore.class).info("'BoxUtil' OpenGL context running on: '" + BoxDatabase.getGLState().GL_CURRENT_DEVICE_NAME + "' with drive version: '" + BoxDatabase.getGLState().GL_CURRENT_DEVICE_VERSION + "'.");
        if (!BoxConfigs.isBaseGL42Supported()) {
            Global.getLogger(ShaderCore.class).warn("'BoxUtil' platform is not supported 'OpenGL4.2'.");
            closeShader();
            return;
        }
        if (!BoxConfigs.isShaderEnable()) {
            Global.getLogger(ShaderCore.class).warn("'BoxUtil' shader core has been disabled.");
            closeShader();
            return;
        }
        String vertCommon, fragCommon,
                vertSprite, fragSprite,
                vertCurve, tescCurve, teseCurve, geomCurve, fragCurve,
                vertSeg, tescSeg, teseSeg,
                vertTrail, geomTrail, fragTrail,
                vertFlare, fragFlare,
                vertText, geomText, fragText,
                vertDist, fragDist,
                vertPost, fragDirect, vertBloom, fragBloom, fragFXAAC, fragFXAAQ,
                compMatrixF32, compMatrixF16,
                compSDFInit, compSDFProcess, compSDFResult,
                compGaussianBlur, compGaussianBlurRed,
                compBilateralFilter, compBilateralFilterRed,
                compDFT, compDFTRed,
                compNormalInit, compNormalResult;
        final String gl_linear = "0.2126729, 0.7151522, 0.0721750";
        final String gl_screenXStep = String.format("%.9f", 1.0f / (float) screenSizeScale[0]);
        final String gl_screenYStep = String.format("%.9f", 1.0f / (float) screenSizeScale[1]);
        final boolean vendorCheckRed = BoxDatabase.getGLState().GL_CURRENT_DEVICE_VENDOR_BYTE == BoxEnum.GL_DEVICE_AMD_ATI;
        final String gl_localWorkDim = vendorCheckRed ? "16" : "8";
        final String gl_localWorkDimSDF = vendorCheckRed ? "8" : "4";
        final String gl_localWorkSize = vendorCheckRed ? "64" : "32";
        final String gl_matrixUBO = _GLSL_MATRIX_UBO_BINDING + "";
        final String gl_bloomRadius = "" + (4.0 * Global.getSettings().getScreenScaleMult());
        vertCommon = BUtil_ShaderSources.Common.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        fragCommon = BUtil_ShaderSources.Common.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertSprite = BUtil_ShaderSources.Sprite.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        fragSprite = BUtil_ShaderSources.Sprite.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertCurve = BUtil_ShaderSources.Curve.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        tescCurve = BUtil_ShaderSources.Curve.TESC.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        teseCurve = BUtil_ShaderSources.Curve.TESE.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        geomCurve = BUtil_ShaderSources.Curve.GEOM.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        fragCurve = BUtil_ShaderSources.Curve.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertSeg = BUtil_ShaderSources.Segment.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        tescSeg = BUtil_ShaderSources.Segment.TESC.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        teseSeg = BUtil_ShaderSources.Segment.TESE.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertTrail = BUtil_ShaderSources.Trail.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        geomTrail = BUtil_ShaderSources.Trail.GEOM.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        fragTrail = BUtil_ShaderSources.Trail.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertFlare = BUtil_ShaderSources.Flare.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        fragFlare = BUtil_ShaderSources.Flare.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertText = BUtil_ShaderSources.TextField.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        geomText = BUtil_ShaderSources.TextField.GEOM.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        fragText = BUtil_ShaderSources.TextField.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertDist = BUtil_ShaderSources.Distortion.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace(_GLSL_MATRIX_UBO_TITLE, gl_matrixUBO);
        fragDist = BUtil_ShaderSources.Distortion.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertPost = BUtil_ShaderSources.Share.POST_VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        fragDirect = BUtil_ShaderSources.Share.DIRECT_FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        vertBloom = BUtil_ShaderSources.Bloom.VERT.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION).replace("OVERWRITE_SCREEN_Y", gl_screenYStep).replace("OVERWRITE_SCREEN_X", gl_screenXStep)
                .replace(_GLSL_BLOOM_RADIUS_TITLE, gl_bloomRadius);
        fragBloom = BUtil_ShaderSources.Bloom.FRAG.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION);
        fragFXAAC = BUtil_ShaderSources.FXAA.CONSOLE.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace("OVERWRITE_SCREEN_X", gl_screenXStep).replace("OVERWRITE_SCREEN_Y", gl_screenYStep).replace(_GLSL_WEIGHTED_LUMINANCE_TITLE, gl_linear);
        fragFXAAQ = BUtil_ShaderSources.FXAA.QUALITY.replace(_GLSL_VERSION_TITLE, _GLSL_VERSION)
                .replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace("OVERWRITE_SCREEN_X", gl_screenXStep).replace("OVERWRITE_SCREEN_Y", gl_screenYStep).replace(_GLSL_WEIGHTED_LUMINANCE_TITLE, gl_linear);
        compMatrixF32 = BUtil_ShaderSources.InstanceMatrix.COMP.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDim).replace(_GLSL_WORKGROUP_SIZE_TITLE, gl_localWorkSize);
        compMatrixF16 = compMatrixF32.replace(_GLSL_INSTANCE_FORMAT_TITLE, _GLSL_INSTANCE_FORMAT_F16);
        compMatrixF32 = compMatrixF32.replace(_GLSL_INSTANCE_FORMAT_TITLE, _GLSL_INSTANCE_FORMAT_F32);
        compSDFInit = BUtil_ShaderSources.SDF.INIT.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF).replace(_GLSL_WEIGHTED_LUMINANCE_TITLE, gl_linear);
        compSDFProcess = BUtil_ShaderSources.SDF.PROCESS.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compSDFResult = BUtil_ShaderSources.SDF.RESULT.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compGaussianBlur = BUtil_ShaderSources.GaussianBlur.RGBA.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compGaussianBlurRed = BUtil_ShaderSources.GaussianBlur.RED.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compBilateralFilter = BUtil_ShaderSources.BilateralFilter.RGBA.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compBilateralFilterRed = BUtil_ShaderSources.BilateralFilter.RED.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compDFT = BUtil_ShaderSources.FourierTransform.DFT.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compDFTRed = BUtil_ShaderSources.FourierTransform.DFT_RED.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        compNormalInit = BUtil_ShaderSources.NormalMapGen.INIT.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF).replace(_GLSL_WEIGHTED_LUMINANCE_TITLE, gl_linear);
        compNormalResult = BUtil_ShaderSources.NormalMapGen.RESULT.replace(_GLSL_PRECISION_TITLE, _GLSL_PRECISION)
                .replace(_GLSL_COMPUTE_DIM_REPLACE_TITLE, gl_localWorkDimSDF);
        _SHADER_PROGRAM[_COMMON] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-CommonShader", vertCommon, fragCommon));
        _SHADER_PROGRAM[_SPRITE] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-SpriteShader", vertSprite, fragSprite));
        _SHADER_PROGRAM[_CURVE] = new BUtil_ShaderProgram(ShaderUtil.createShaderVTGF("BoxUtil-CurveShader", vertCurve, tescCurve, teseCurve, geomCurve, fragCurve));
        _SHADER_PROGRAM[_SEGMENT] = new BUtil_ShaderProgram(ShaderUtil.createShaderVTGF("BoxUtil-SegmentShader", vertSeg, tescSeg, teseSeg, geomCurve, fragCurve));
        _SHADER_PROGRAM[_TRAIL] = new BUtil_ShaderProgram(ShaderUtil.createShaderVGF("BoxUtil-TrailShader", vertTrail, geomTrail, fragTrail));
        _SHADER_PROGRAM[_FLARE] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-FlareShader", vertFlare, fragFlare));
        _SHADER_PROGRAM[_TEXT] = new BUtil_ShaderProgram(ShaderUtil.createShaderVGF("BoxUtil-TextShader", vertText, geomText, fragText));
        _SHADER_PROGRAM[_DIST] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-DistortionShader", vertDist, fragDist));
        _SHADER_PROGRAM[_DIRECT] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-DirectShader", vertPost, fragDirect));
        _SHADER_PROGRAM[_BLOOM] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-BloomShader", vertBloom, fragBloom));
        _SHADER_PROGRAM[_FXAA_C] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-FXAA-ConsoleShader", vertPost, fragFXAAC));
        _SHADER_PROGRAM[_FXAA_Q] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-FXAA-QualityShader", vertPost, fragFXAAQ));
        if (BoxConfigs.isGLParallelSupported()) {
            if (BoxConfigs.isGLParallel()) {
                _SHADER_PROGRAM[_MATRIX_F32] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-MatrixComputeShader-F32", compMatrixF32));
                _SHADER_PROGRAM[_MATRIX_F16] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-MatrixComputeShader-F16", compMatrixF16));
            }
            _SHADER_PROGRAM[_SDF_INIT] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-SDFGenInitShader", compSDFInit));
            _SHADER_PROGRAM[_SDF_PROCESS] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-SDFGenProcessShader", compSDFProcess));
            _SHADER_PROGRAM[_SDF_RESULT] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-SDFGenResultShader", compSDFResult));

            _SHADER_PROGRAM[_GAUSSIAN_BLUR] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-CompGaussianBlurShader", compGaussianBlur));
            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-CompGaussianBlurRedShader", compGaussianBlurRed));
            _SHADER_PROGRAM[_BILATERAL_FLITER] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-CompBilateralFilterShader", compBilateralFilter));
            _SHADER_PROGRAM[_BILATERAL_FLITER_RED] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-CompBilateralFilterRedShader", compBilateralFilterRed));
            _SHADER_PROGRAM[_DFT] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-DFTShader", compDFT));
            _SHADER_PROGRAM[_DFT_RED] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-DFTRedShader", compDFTRed));
            _SHADER_PROGRAM[_NORMAL_GEN_INIT] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-NormalMapInitShader", compNormalInit));
            _SHADER_PROGRAM[_NORMAL_GEN_RESULT] = new BUtil_ShaderProgram(ShaderUtil.createComputeShaders("BoxUtil-NormalMapResultShader", compNormalResult));
        }

        initShaderPrograms();
        refreshRenderingBuffer();
        refreshDefaultVAO();
        if (!isMainProgramValid() || !isRenderingFramebufferValid() || !isDefaultVAOValid()) {
            closeShader();
            Global.getLogger(ShaderCore.class).error("'BoxUtil' base shader resource init failed, main program: " + isMainProgramValid() + ", rendering framebuffer: " + isRenderingFramebufferValid() + ", default VAO: " + isDefaultVAOValid() + ".");
            return;
        }
        matrixUBO = GL15.glGenBuffers();
        if (matrixUBO == 0) {
            closeShader();
            Global.getLogger(ShaderCore.class).error("'BoxUtil' shader UBO init failed. ");
            return;
        }

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matrixUBO);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, 20 * BoxDatabase.FLOAT_SIZE, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, _GLSL_MATRIX_UBO_BINDING, matrixUBO);
        glValid = true;
        glFinished = true;

        initDistortionProgram();
        initBloomProgram();
        initFXAACProgram();
        initFXAAQProgram();
        initMatrixProgram();
        initSDFGenProgram();
        initCompGaussianBlurProgram();
        initCompBilateralFilterProgram();
        initDiscreteFourierProgram();
        initNormalMapGenProgram();
    }

    public static void initMiscShaderPrograms() {
        if (_miscShaderInit) return;
        _miscShaderInit = true;
        _SHADER_PROGRAM[_SIMPLE_NUMBER] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-NumberShader", BUtil_ShaderSources.Number.VERT, BUtil_ShaderSources.Number.FRAG));
        _SHADER_PROGRAM[_SIMPLE_ARC] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-ArcShader", BUtil_ShaderSources.Arc.VERT, BUtil_ShaderSources.Arc.FRAG));
        _SHADER_PROGRAM[_SIMPLE_TEX_ARC] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-TexArcShader", BUtil_ShaderSources.Arc.VERT, BUtil_ShaderSources.TexArc.FRAG));
        _SHADER_PROGRAM[_MISSION_BG] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-TestMissionShader", BUtil_ShaderSources.Mission.VERT, BUtil_ShaderSources.Mission.FRAG));
        if (_SHADER_PROGRAM[_SIMPLE_NUMBER].isValid()) {
            int[] uniforms = new int[]{
                    _SHADER_PROGRAM[_SIMPLE_NUMBER].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_SIMPLE_NUMBER].getUniformIndex("charLength")
            };
            _SHADER_PROGRAM[_SIMPLE_NUMBER].location = uniforms;
        }

        if (_SHADER_PROGRAM[_SIMPLE_ARC].isValid()) {
            int[] uniforms = new int[]{
                    _SHADER_PROGRAM[_SIMPLE_ARC].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_SIMPLE_ARC].getUniformIndex("arcValue")
            };
            _SHADER_PROGRAM[_SIMPLE_ARC].location = uniforms;
        }

        if (_SHADER_PROGRAM[_SIMPLE_TEX_ARC].isValid()) {
            int[] uniforms = new int[]{
                    _SHADER_PROGRAM[_SIMPLE_TEX_ARC].getUniformIndex("statePackage")
            };
            _SHADER_PROGRAM[_SIMPLE_TEX_ARC].location = uniforms;
        }

        if (_SHADER_PROGRAM[_MISSION_BG].isValid()) {
            int[] uniforms = new int[]{
                    _SHADER_PROGRAM[_MISSION_BG].getUniformIndex("time")
            };
            _SHADER_PROGRAM[_MISSION_BG].location = uniforms;
        }

        initRadialBlurProgram();
    }

    public static void closeShader() {
        glValid = false;
        glFinished = true;
        BoxConfigs.sysCheck();
    }

    private static void initShaderPrograms() {
        if (_SHADER_PROGRAM[_COMMON].isValid()) {
            int[] commonU = new int[]{
                    _SHADER_PROGRAM[_COMMON].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_COMMON].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_COMMON].getUniformIndex("baseSize"),
                    _SHADER_PROGRAM[_COMMON].getUniformIndex("additionEmissive"),
                    _SHADER_PROGRAM[_COMMON].getUniformIndex("instanceDataExtra")
            };
            int[] commonUBOU = new int[]{
                    _SHADER_PROGRAM[_COMMON].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING)
            };
            int[][] commonSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "commonMode"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "colorMode"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "noneData"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData2D"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData2D"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData3D"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData3D")
                    },
                    {
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "commonMode"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "colorMode")
                    }
            };
            int[][] commonSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_COMMON].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "vertexState"),
                            _SHADER_PROGRAM[_COMMON].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "instanceState")
                    },
                    {
                            _SHADER_PROGRAM[_COMMON].getSubroutineUniformLocation(GL20.GL_FRAGMENT_SHADER, "surfaceState")
                    }
            };
            _SHADER_PROGRAM[_COMMON].location = commonU;
            _SHADER_PROGRAM[_COMMON].uboLocation = commonUBOU;
            _SHADER_PROGRAM[_COMMON].subroutineLocation = commonSU;
            _SHADER_PROGRAM[_COMMON].subroutineUniformLocation = commonSUL;
            _SHADER_PROGRAM[_COMMON].initMaxSubroutineUniformLocation();
        } else glMainProgramValid = false;

        if (_SHADER_PROGRAM[_SPRITE].isValid()) {
            int[] spriteU = new int[]{
                    _SHADER_PROGRAM[_SPRITE].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_SPRITE].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_SPRITE].getUniformIndex("additionEmissive"),
                    _SHADER_PROGRAM[_SPRITE].getUniformIndex("instanceDataExtra")
            };
            int[] spriteUBOU = new int[]{
                    _SHADER_PROGRAM[_SPRITE].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING),
            };
            int[][] spriteSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "commonUV"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "tileUV"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "tileRUV"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "noneData"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData2D"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData2D"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData3D"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData3D")
                    }
            };
            int[][] spriteSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_SPRITE].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "uvMapping"),
                            _SHADER_PROGRAM[_SPRITE].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "instanceState")
                    }
            };
            _SHADER_PROGRAM[_SPRITE].location = spriteU;
            _SHADER_PROGRAM[_SPRITE].uboLocation = spriteUBOU;
            _SHADER_PROGRAM[_SPRITE].subroutineLocation = spriteSU;
            _SHADER_PROGRAM[_SPRITE].subroutineUniformLocation = spriteSUL;
            _SHADER_PROGRAM[_SPRITE].initMaxSubroutineUniformLocation();
        } else glMainProgramValid = false;

        if (_SHADER_PROGRAM[_CURVE].isValid()) {
            int[] curveU = new int[]{
                    _SHADER_PROGRAM[_CURVE].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_CURVE].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_CURVE].getUniformIndex("totalNodes"),
                    _SHADER_PROGRAM[_CURVE].getUniformIndex("additionEmissive"),
                    _SHADER_PROGRAM[_CURVE].getUniformIndex("instanceDataExtra")
            };
            int[] curveUBOU = new int[]{
                    _SHADER_PROGRAM[_CURVE].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING),
            };
            int[][] curveSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_CURVE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "noneData"),
                            _SHADER_PROGRAM[_CURVE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData2D"),
                            _SHADER_PROGRAM[_CURVE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData2D"),
                            _SHADER_PROGRAM[_CURVE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData3D"),
                            _SHADER_PROGRAM[_CURVE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData2D")
                    }
            };
            int[][] curveSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_CURVE].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "instanceState")
                    }
            };
            _SHADER_PROGRAM[_CURVE].location = curveU;
            _SHADER_PROGRAM[_CURVE].uboLocation = curveUBOU;
            _SHADER_PROGRAM[_CURVE].subroutineLocation = curveSU;
            _SHADER_PROGRAM[_CURVE].subroutineUniformLocation = curveSUL;
            _SHADER_PROGRAM[_CURVE].initMaxSubroutineUniformLocation();
        } else glMainProgramValid = false;

        if (_SHADER_PROGRAM[_SEGMENT].isValid()) {
            int[] curveU = new int[]{
                    _SHADER_PROGRAM[_SEGMENT].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_SEGMENT].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_SEGMENT].getUniformIndex("additionEmissive")
            };
            int[] curveUBOU = new int[]{
                    _SHADER_PROGRAM[_SEGMENT].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING),
            };
            _SHADER_PROGRAM[_SEGMENT].location = curveU;
            _SHADER_PROGRAM[_SEGMENT].uboLocation = curveUBOU;
        } else glMainProgramValid = false;

        if (_SHADER_PROGRAM[_TRAIL].isValid()) {
            int[] trailU = new int[]{
                    _SHADER_PROGRAM[_TRAIL].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_TRAIL].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_TRAIL].getUniformIndex("extraData"),
                    _SHADER_PROGRAM[_TRAIL].getUniformIndex("additionEmissive")
            };
            int[] trailUBOU = new int[]{
                    _SHADER_PROGRAM[_TRAIL].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING),
            };
            _SHADER_PROGRAM[_TRAIL].location = trailU;
            _SHADER_PROGRAM[_TRAIL].uboLocation = trailUBOU;
        } else glMainProgramValid = false;

        if (_SHADER_PROGRAM[_FLARE].isValid()) {
            int[] flareU = new int[]{
                    _SHADER_PROGRAM[_FLARE].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_FLARE].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_FLARE].getUniformIndex("instanceDataExtra")
            };
            int[] flareUBOU = new int[]{
                    _SHADER_PROGRAM[_FLARE].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING),
            };
            int[][] flareSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "noneData"),
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData2D"),
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData2D"),
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData3D"),
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData3D")
                    },
                    {
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "smoothMode"),
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "sharpMode"),
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "smoothDiscMode"),
                            _SHADER_PROGRAM[_FLARE].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "sharpDiscMode")
                    }
            };
            int[][] flareSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_FLARE].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "instanceState")
                    },
                    {
                            _SHADER_PROGRAM[_FLARE].getSubroutineUniformLocation(GL20.GL_FRAGMENT_SHADER, "flareState")
                    }
            };
            _SHADER_PROGRAM[_FLARE].location = flareU;
            _SHADER_PROGRAM[_FLARE].uboLocation = flareUBOU;
            _SHADER_PROGRAM[_FLARE].subroutineLocation = flareSU;
            _SHADER_PROGRAM[_FLARE].subroutineUniformLocation = flareSUL;
            _SHADER_PROGRAM[_FLARE].initMaxSubroutineUniformLocation();
        } else glMainProgramValid = false;

        if (_SHADER_PROGRAM[_TEXT].isValid()) {
            int[] textU = new int[]{
                    _SHADER_PROGRAM[_TEXT].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_TEXT].getUniformIndex("fontMap[0]"),
                    _SHADER_PROGRAM[_TEXT].getUniformIndex("fontMap[1]"),
                    _SHADER_PROGRAM[_TEXT].getUniformIndex("fontMap[2]"),
                    _SHADER_PROGRAM[_TEXT].getUniformIndex("fontMap[3]"),
                    _SHADER_PROGRAM[_TEXT].getUniformIndex("italicFactor"),
                    _SHADER_PROGRAM[_TEXT].getUniformIndex("globalColor")
            };
            int[] textUBOU = new int[]{
                    _SHADER_PROGRAM[_TEXT].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING),
            };
            _SHADER_PROGRAM[_TEXT].location = textU;
            _SHADER_PROGRAM[_TEXT].uboLocation = textUBOU;
            _SHADER_PROGRAM[_TEXT].putDefaultTextureUnit(_SHADER_PROGRAM[_TEXT].location[1], 0);
            _SHADER_PROGRAM[_TEXT].putDefaultTextureUnit(_SHADER_PROGRAM[_TEXT].location[2], 1);
            _SHADER_PROGRAM[_TEXT].putDefaultTextureUnit(_SHADER_PROGRAM[_TEXT].location[3], 2);
            _SHADER_PROGRAM[_TEXT].putDefaultTextureUnit(_SHADER_PROGRAM[_TEXT].location[4], 3);
        } else glMainProgramValid = false;

        if (_SHADER_PROGRAM[_DIRECT].isValid()) {
            int[] directU = new int[]{
                    _SHADER_PROGRAM[_DIRECT].getUniformIndex("alphaFix")
            };
            _SHADER_PROGRAM[_DIRECT].location = directU;
        } else glMainProgramValid = false;
    }

    private static void initDistortionProgram() {
        if (_SHADER_PROGRAM[_DIST].isValid()) {
            int[] distU = new int[]{
                    _SHADER_PROGRAM[_DIST].getUniformIndex("modelMatrix"),
                    _SHADER_PROGRAM[_DIST].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_DIST].getUniformIndex("screenScale"),
                    _SHADER_PROGRAM[_DIST].getUniformIndex("instanceDataExtra")
            };
            int[] distUBOU = new int[]{
                    _SHADER_PROGRAM[_DIST].getUBOIndex("BUtilGlobalData", _GLSL_MATRIX_UBO_BINDING),
            };
            int[][] distSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_DIST].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "noneData"),
                            _SHADER_PROGRAM[_DIST].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData2D"),
                            _SHADER_PROGRAM[_DIST].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData2D"),
                            _SHADER_PROGRAM[_DIST].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveData3D"),
                            _SHADER_PROGRAM[_DIST].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "haveFixedData3D")
                    }
            };
            int[][] distSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_DIST].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "instanceState")
                    }
            };
            _SHADER_PROGRAM[_DIST].location = distU;
            _SHADER_PROGRAM[_DIST].uboLocation = distUBOU;
            _SHADER_PROGRAM[_DIST].subroutineLocation = distSU;
            _SHADER_PROGRAM[_DIST].subroutineUniformLocation = distSUL;
            _SHADER_PROGRAM[_DIST].initMaxSubroutineUniformLocation();
            glDistortionValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' distortion program init failed.");
    }

    private static void initBloomProgram() {
        if (_SHADER_PROGRAM[_BLOOM].isValid()) {
            int[] bloomU = new int[]{
                    _SHADER_PROGRAM[_BLOOM].getUniformIndex("scale")
            };
            int[][] bloomSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_BLOOM].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "downSample"),
                            _SHADER_PROGRAM[_BLOOM].getSubroutineIndex(GL20.GL_VERTEX_SHADER, "upSample")
                    },
                    {
                            _SHADER_PROGRAM[_BLOOM].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "downSample"),
                            _SHADER_PROGRAM[_BLOOM].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "upSample")
                    }
            };
            int[][] bloomSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_BLOOM].getSubroutineUniformLocation(GL20.GL_VERTEX_SHADER, "sampleModeState")
                    },
                    {
                            _SHADER_PROGRAM[_BLOOM].getSubroutineUniformLocation(GL20.GL_FRAGMENT_SHADER, "sampleModeState")
                    }
            };
            _SHADER_PROGRAM[_BLOOM].location = bloomU;
            _SHADER_PROGRAM[_BLOOM].subroutineLocation = bloomSU;
            _SHADER_PROGRAM[_BLOOM].subroutineUniformLocation = bloomSUL;
            _SHADER_PROGRAM[_BLOOM].initMaxSubroutineUniformLocation();
            glBloomValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' bloom program init failed.");
    }

    private static void initFXAACProgram() {
        if (_SHADER_PROGRAM[_FXAA_C].isValid()) {
            int[][] fxaaCSU = new int[][]{
                    {},
                    {
                            _SHADER_PROGRAM[_FXAA_C].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "fromRaw"),
                            _SHADER_PROGRAM[_FXAA_C].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "fromDepth"),
                            _SHADER_PROGRAM[_FXAA_C].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "commonDisplay"),
                            _SHADER_PROGRAM[_FXAA_C].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "edgeDisplay")
                    }
            };
            int[][] fxaaSUL = new int[][]{
                    {},
                    {
                            _SHADER_PROGRAM[_FXAA_C].getSubroutineUniformLocation(GL20.GL_FRAGMENT_SHADER, "sampleMethodState"),
                            _SHADER_PROGRAM[_FXAA_C].getSubroutineUniformLocation(GL20.GL_FRAGMENT_SHADER, "displayMethodState")
                    }
            };
            _SHADER_PROGRAM[_FXAA_C].subroutineLocation = fxaaCSU;
            _SHADER_PROGRAM[_FXAA_C].subroutineUniformLocation = fxaaSUL;
            _SHADER_PROGRAM[_FXAA_C].initMaxSubroutineUniformLocation();
            glFXAACValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' fxaa-console program init failed.");
    }

    private static void initFXAAQProgram() {
        if (_SHADER_PROGRAM[_FXAA_Q].isValid()) {
            int[][] fxaaQSU = new int[][]{
                    {},
                    {
                            _SHADER_PROGRAM[_FXAA_Q].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "fromRaw"),
                            _SHADER_PROGRAM[_FXAA_Q].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "fromDepth"),
                            _SHADER_PROGRAM[_FXAA_Q].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "commonDisplay"),
                            _SHADER_PROGRAM[_FXAA_Q].getSubroutineIndex(GL20.GL_FRAGMENT_SHADER, "edgeDisplay")
                    }
            };
            int[][] fxaaSUL = new int[][]{
                    {},
                    {
                            _SHADER_PROGRAM[_FXAA_Q].getSubroutineUniformLocation(GL20.GL_FRAGMENT_SHADER, "sampleMethodState"),
                            _SHADER_PROGRAM[_FXAA_Q].getSubroutineUniformLocation(GL20.GL_FRAGMENT_SHADER, "displayMethodState")
                    }
            };
            _SHADER_PROGRAM[_FXAA_Q].subroutineLocation = fxaaQSU;
            _SHADER_PROGRAM[_FXAA_Q].subroutineUniformLocation = fxaaSUL;
            _SHADER_PROGRAM[_FXAA_Q].initMaxSubroutineUniformLocation();
            glFXAAQValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' fxaa-quality program init failed.");
    }

    private static void initMatrixProgram() {
        if (BoxConfigs.isGLParallel() && _SHADER_PROGRAM[_MATRIX_F32].isValid() && _SHADER_PROGRAM[_MATRIX_F16].isValid()) {
            int[] matrix32U = new int[]{
                    _SHADER_PROGRAM[_MATRIX_F32].getUniformIndex("instanceState"),
            };
            int[][] matrix32SU = new int[][]{
                    {
                            _SHADER_PROGRAM[_MATRIX_F32].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "instance2D"),
                            _SHADER_PROGRAM[_MATRIX_F32].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "instance3D")
                    }
            };
            int[][] matrix32SUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_MATRIX_F32].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "instanceTypeState")
                    }
            };
            _SHADER_PROGRAM[_MATRIX_F32].location = matrix32U;
            _SHADER_PROGRAM[_MATRIX_F32].subroutineLocation = matrix32SU;
            _SHADER_PROGRAM[_MATRIX_F32].subroutineUniformLocation = matrix32SUL;
            _SHADER_PROGRAM[_MATRIX_F32].initMaxSubroutineUniformLocation();


            int[] matrix16U = new int[]{
                    _SHADER_PROGRAM[_MATRIX_F16].getUniformIndex("instanceState"),
            };
            int[][] matrix16SU = new int[][]{
                    {
                            _SHADER_PROGRAM[_MATRIX_F16].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "instance2D"),
                            _SHADER_PROGRAM[_MATRIX_F16].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "instance3D")
                    }
            };
            int[][] matrix16SUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_MATRIX_F16].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "instanceTypeState")
                    }
            };
            _SHADER_PROGRAM[_MATRIX_F16].location = matrix16U;
            _SHADER_PROGRAM[_MATRIX_F16].subroutineLocation = matrix16SU;
            _SHADER_PROGRAM[_MATRIX_F16].subroutineUniformLocation = matrix16SUL;
            _SHADER_PROGRAM[_MATRIX_F16].initMaxSubroutineUniformLocation();
            glInstanceMatrixValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' matrix program init failed.");
    }

    private static void initSDFGenProgram() {
        if (_SHADER_PROGRAM[_SDF_INIT].isValid() && _SHADER_PROGRAM[_SDF_PROCESS].isValid() && _SHADER_PROGRAM[_SDF_RESULT].isValid()) {
            int[] initU = new int[]{
                    _SHADER_PROGRAM[_SDF_INIT].getUniformIndex("sizeState"),
                    _SHADER_PROGRAM[_SDF_INIT].getUniformIndex("border"),
                    _SHADER_PROGRAM[_SDF_INIT].getUniformIndex("threshold")
            };
            int[][] initSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_SDF_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "fromRed"),
                            _SHADER_PROGRAM[_SDF_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "fromGreen"),
                            _SHADER_PROGRAM[_SDF_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "fromBlue"),
                            _SHADER_PROGRAM[_SDF_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "fromAlpha"),
                            _SHADER_PROGRAM[_SDF_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "fromRGB")
                    }
            };
            int[][] initSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_SDF_INIT].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "sampleMethodState")
                    }
            };
            _SHADER_PROGRAM[_SDF_INIT].location = initU;
            _SHADER_PROGRAM[_SDF_INIT].subroutineLocation = initSU;
            _SHADER_PROGRAM[_SDF_INIT].subroutineUniformLocation = initSUL;
            _SHADER_PROGRAM[_SDF_INIT].initMaxSubroutineUniformLocation();

            int[] processU = new int[]{
                    _SHADER_PROGRAM[_SDF_PROCESS].getUniformIndex("size"),
                    _SHADER_PROGRAM[_SDF_PROCESS].getUniformIndex("step")
            };
            _SHADER_PROGRAM[_SDF_PROCESS].location = processU;

            int[] resultU = new int[]{
                    _SHADER_PROGRAM[_SDF_RESULT].getUniformIndex("size"),
                    _SHADER_PROGRAM[_SDF_RESULT].getUniformIndex("preMultiply")
            };
            int[][] resultSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_SDF_RESULT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Store"),
                            _SHADER_PROGRAM[_SDF_RESULT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Store")
                    }
            };
            int[][] resultSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_SDF_RESULT].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerStoreState")
                    }
            };
            _SHADER_PROGRAM[_SDF_RESULT].location = resultU;
            _SHADER_PROGRAM[_SDF_RESULT].subroutineLocation = resultSU;
            _SHADER_PROGRAM[_SDF_RESULT].subroutineUniformLocation = resultSUL;
            _SHADER_PROGRAM[_SDF_RESULT].initMaxSubroutineUniformLocation();
            glSDFGenValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' SDF-generate program init failed.");
    }

    private static void initRadialBlurProgram() {
        _SHADER_PROGRAM[_RADIAL_BLUR] = new BUtil_ShaderProgram(ShaderUtil.createShaderVF("BoxUtil-RadialBlurShader", BUtil_ShaderSources.RadialBlur.VERT, BUtil_ShaderSources.RadialBlur.FRAG));
        if (_SHADER_PROGRAM[_RADIAL_BLUR].isValid()) {
            int[] radialBlurU = new int[]{
                    _SHADER_PROGRAM[_RADIAL_BLUR].getUniformIndex("statePackage"),
                    _SHADER_PROGRAM[_RADIAL_BLUR].getUniformIndex("alphaStrength"),
                    _SHADER_PROGRAM[_RADIAL_BLUR].getUniformIndex("tex")
            };
            _SHADER_PROGRAM[_RADIAL_BLUR].location = radialBlurU;
            glRadialBlurValid = true;
        }
    }

    private static void initCompGaussianBlurProgram() {
        if (_SHADER_PROGRAM[_GAUSSIAN_BLUR].isValid() && _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].isValid()) {
            int[] compGaussianU = new int[]{
                    _SHADER_PROGRAM[_GAUSSIAN_BLUR].getUniformIndex("sizeStep"),
                    _SHADER_PROGRAM[_GAUSSIAN_BLUR].getUniformIndex("vertical"),
                    _SHADER_PROGRAM[_GAUSSIAN_BLUR].getUniformIndex("perStep")
            };
            int[][] compGaussianSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Load"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Load"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Store"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Store")
                    }
            };
            int[][] compGaussianSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadState"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerStoreState")
                    }
            };
            _SHADER_PROGRAM[_GAUSSIAN_BLUR].location = compGaussianU;
            _SHADER_PROGRAM[_GAUSSIAN_BLUR].subroutineLocation = compGaussianSU;
            _SHADER_PROGRAM[_GAUSSIAN_BLUR].subroutineUniformLocation = compGaussianSUL;
            _SHADER_PROGRAM[_GAUSSIAN_BLUR].initMaxSubroutineUniformLocation();

            int[] compGaussianRedU = new int[]{
                    _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getUniformIndex("sizeStep"),
                    _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getUniformIndex("vertical"),
                    _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getUniformIndex("perStep")
            };
            int[][] compGaussianRedSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Load"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Load"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Store"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Store")
                    }
            };
            int[][] compGaussianRedSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadState"),
                            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerStoreState")
                    }
            };
            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].location = compGaussianRedU;
            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].subroutineLocation = compGaussianRedSU;
            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].subroutineUniformLocation = compGaussianRedSUL;
            _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED].initMaxSubroutineUniformLocation();
            glCompGaussianBlurValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' comp-gaussian blur program init failed.");
    }

    private static void initCompBilateralFilterProgram() {
        if (_SHADER_PROGRAM[_BILATERAL_FLITER].isValid() && _SHADER_PROGRAM[_BILATERAL_FLITER_RED].isValid()) {
            int[] compBilateralU = new int[]{
                    _SHADER_PROGRAM[_BILATERAL_FLITER].getUniformIndex("sizeStep"),
                    _SHADER_PROGRAM[_BILATERAL_FLITER].getUniformIndex("vertical"),
                    _SHADER_PROGRAM[_BILATERAL_FLITER].getUniformIndex("gSigmaSRInv")
            };
            int[][] compBilateralSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Load"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Load"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Store"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Store"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8LoadSrc"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16LoadSrc")
                    }
            };
            int[][] compBilateralSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadState"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerStoreState"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadSrcState")
                    }
            };
            _SHADER_PROGRAM[_BILATERAL_FLITER].location = compBilateralU;
            _SHADER_PROGRAM[_BILATERAL_FLITER].subroutineLocation = compBilateralSU;
            _SHADER_PROGRAM[_BILATERAL_FLITER].subroutineUniformLocation = compBilateralSUL;
            _SHADER_PROGRAM[_BILATERAL_FLITER].initMaxSubroutineUniformLocation();

            int[] compBilateralRedU = new int[]{
                    _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getUniformIndex("sizeStep"),
                    _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getUniformIndex("vertical"),
                    _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getUniformIndex("gSigmaSRInv")
            };
            int[][] compBilateralRedSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Load"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Load"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Store"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Store"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8LoadSrc"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16LoadSrc")
                    }
            };
            int[][] compBilateralRedSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadState"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerStoreState"),
                            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadSrcState")
                    }
            };
            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].location = compBilateralRedU;
            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].subroutineLocation = compBilateralRedSU;
            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].subroutineUniformLocation = compBilateralRedSUL;
            _SHADER_PROGRAM[_BILATERAL_FLITER_RED].initMaxSubroutineUniformLocation();
            glCompBilateralFilterValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' comp-bilateral filter program init failed.");
    }

    private static void initDiscreteFourierProgram() {
        if (_SHADER_PROGRAM[_DFT].isValid() && _SHADER_PROGRAM[_DFT_RED].isValid()) {
            int[] compDFTU = new int[]{
                    _SHADER_PROGRAM[_DFT].getUniformIndex("size"),
                    _SHADER_PROGRAM[_DFT].getUniformIndex("state"),
                    _SHADER_PROGRAM[_DFT].getUniformIndex("sizeDiv")
            };
            int[][] compDFTSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_DFT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Load"),
                            _SHADER_PROGRAM[_DFT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Load"),
                            _SHADER_PROGRAM[_DFT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit32Load"),
                            _SHADER_PROGRAM[_DFT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Store"),
                            _SHADER_PROGRAM[_DFT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Store"),
                            _SHADER_PROGRAM[_DFT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit32Store")
                    }
            };
            int[][] compDFTSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_DFT].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadState"),
                            _SHADER_PROGRAM[_DFT].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerStoreState")
                    }
            };
            _SHADER_PROGRAM[_DFT].location = compDFTU;
            _SHADER_PROGRAM[_DFT].subroutineLocation = compDFTSU;
            _SHADER_PROGRAM[_DFT].subroutineUniformLocation = compDFTSUL;
            _SHADER_PROGRAM[_DFT].initMaxSubroutineUniformLocation();

            int[] compDFTRedU = new int[]{
                    _SHADER_PROGRAM[_DFT_RED].getUniformIndex("size"),
                    _SHADER_PROGRAM[_DFT_RED].getUniformIndex("state"),
                    _SHADER_PROGRAM[_DFT_RED].getUniformIndex("sizeDiv")
            };
            int[][] compDFTRedSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Load"),
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Load"),
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit32Load"),
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit8Store"),
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit16Store"),
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "bit32Store")
                    }
            };
            int[][] compDFTRedSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerLoadState"),
                            _SHADER_PROGRAM[_DFT_RED].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "formatPickerStoreState")
                    }
            };
            _SHADER_PROGRAM[_DFT_RED].location = compDFTRedU;
            _SHADER_PROGRAM[_DFT_RED].subroutineLocation = compDFTRedSU;
            _SHADER_PROGRAM[_DFT_RED].subroutineUniformLocation = compDFTRedSUL;
            _SHADER_PROGRAM[_DFT_RED].initMaxSubroutineUniformLocation();
            glDiscreteFourierValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' discrete fourier program init failed.");
    }

    private static void initNormalMapGenProgram() {
        if (_SHADER_PROGRAM[_NORMAL_GEN_INIT].isValid() && _SHADER_PROGRAM[_NORMAL_GEN_RESULT].isValid()) {
            int[] initU = new int[]{
                    _SHADER_PROGRAM[_NORMAL_GEN_INIT].getUniformIndex("size"),
                    _SHADER_PROGRAM[_NORMAL_GEN_INIT].getUniformIndex("state")
            };
            int[][] initSU = new int[][]{
                    {
                            _SHADER_PROGRAM[_NORMAL_GEN_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "texOnly"),
                            _SHADER_PROGRAM[_NORMAL_GEN_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "withVolume"),
                            _SHADER_PROGRAM[_NORMAL_GEN_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "withDetails"),
                            _SHADER_PROGRAM[_NORMAL_GEN_INIT].getSubroutineIndex(GL43.GL_COMPUTE_SHADER, "withBoth")
                    }
            };
            int[][] initSUL = new int[][]{
                    {
                            _SHADER_PROGRAM[_NORMAL_GEN_INIT].getSubroutineUniformLocation(GL43.GL_COMPUTE_SHADER, "texInputState")
                    }
            };
            _SHADER_PROGRAM[_NORMAL_GEN_INIT].location = initU;
            _SHADER_PROGRAM[_NORMAL_GEN_INIT].subroutineLocation = initSU;
            _SHADER_PROGRAM[_NORMAL_GEN_INIT].subroutineUniformLocation = initSUL;
            _SHADER_PROGRAM[_NORMAL_GEN_INIT].initMaxSubroutineUniformLocation();

            int[] resultU = new int[]{
                    _SHADER_PROGRAM[_NORMAL_GEN_RESULT].getUniformIndex("sizeState"),
                    _SHADER_PROGRAM[_NORMAL_GEN_RESULT].getUniformIndex("normalStrength")
            };
            _SHADER_PROGRAM[_NORMAL_GEN_RESULT].location = resultU;
            glNormalMapGenValid = true;
        } else Global.getLogger(ShaderCore.class).warn("'BoxUtil' normal map generate program init failed.");
    }

    public static void glBeginDraw() {
        GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, screenSizeScale[0], screenSizeScale[1]);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL13.GL_MULTISAMPLE);
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glDepthRange(-1.0f, 1.0f);
    }

    public static void glEndDraw() {
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
    }

    public static int glDrawBloom() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        if (!BoxConfigs.isBloomEnabled()) return -1;
        int getter = 0;
        int[] buffer = new int[]{renderingBuffer.getFBO(1), renderingBuffer.getFBO(0)};
        int[] tex = new int[]{renderingBuffer.getEmissiveResult(0), renderingBuffer.getColorResult(1)};
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, buffer[getter]);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        _SHADER_PROGRAM[_BLOOM].active();
        _SHADER_PROGRAM[_BLOOM].putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 0);
        _SHADER_PROGRAM[_BLOOM].putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 0);

        for (byte i = 1; i < BUtil_RenderingBuffer.getLayerCount(); i++) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, buffer[getter]);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            _SHADER_PROGRAM[_BLOOM].bindTexture2D(tex[getter]);
            renderingBuffer.setScaleViewport(i);
            GL20.glUniform1f(_SHADER_PROGRAM[_BLOOM].location[0], renderingBuffer.getScaleFactor(i - 1));
            defaultQuadObject.glDraw();
            getter = 1 - getter;
            if (i == 1) {
                tex[0] = renderingBuffer.getColorResult(0);
            }
        }

        _SHADER_PROGRAM[_BLOOM].putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 1);
        _SHADER_PROGRAM[_BLOOM].putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 1);
        for (byte i = (byte) (BUtil_RenderingBuffer.getLayerCount() - 2); i > -1; i--) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, buffer[getter]);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            _SHADER_PROGRAM[_BLOOM].bindTexture2D(tex[getter]);
            renderingBuffer.setScaleViewport(i);
            GL20.glUniform1f(_SHADER_PROGRAM[_BLOOM].location[0], renderingBuffer.getScaleFactor(i + 1));
            defaultQuadObject.glDraw();
            getter = 1 - getter;
        }
        _SHADER_PROGRAM[_BLOOM].close();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return tex[getter];
    }

    public static void glApplyAA() {
        if (BoxConfigs.isAADisabled()) return;
        byte type = BoxConfigs.isFXAAC() ? _FXAA_C : _FXAA_Q;
        int[] subroutines = new int[2];
        subroutines[0] = _SHADER_PROGRAM[type].subroutineLocation[1][BoxConfigs.isDepthAA() ? 1 : 0];
        subroutines[1] = _SHADER_PROGRAM[type].subroutineLocation[1][BoxConfigs.isAAShowEdge() ? 3 : 2];
        ShaderUtil.copyFromScreen(renderingBuffer.getFBO(1));
        _SHADER_PROGRAM[type].active();
        _SHADER_PROGRAM[type].putUniformSubroutines(GL20.GL_FRAGMENT_SHADER, 1, subroutines);
        _SHADER_PROGRAM[type].bindTexture2D(0, renderingBuffer.getColorResult(1));
        _SHADER_PROGRAM[type].bindTexture2D(1, renderingBuffer.getDataResult(0));
        defaultQuadObject.glDraw();
        _SHADER_PROGRAM[type].close();
    }

    public static boolean glMultiPass() {
        int texture;
        switch (BoxConfigs.getMultiPassMode()) {
            case BoxEnum.MP_BEAUTY: return false;
            case BoxEnum.MP_DATA: {
                texture = renderingBuffer.getDataResult(0);
                break;
            }
            case BoxEnum.MP_EMISSIVE: {
                texture = renderingBuffer.getEmissiveResult(0);
                break;
            }
            case BoxEnum.MP_NORMAL: {
                texture = renderingBuffer.getNormalResult(0);
                break;
            }
            default: return false;
        }
        _SHADER_PROGRAM[_DIRECT].active();
        GL20.glUniform1f(_SHADER_PROGRAM[_DIRECT].location[0], 1.0f);
        _SHADER_PROGRAM[_DIRECT].bindTexture2D(0, texture);
        defaultQuadObject.glDraw();
        _SHADER_PROGRAM[_DIRECT].close();
        return true;
    }

    public static void initScreenSize() {
        screenSize[0] = (int) (Global.getSettings().getScreenWidth() * Display.getPixelScaleFactor());
        screenSizeScale[0] = (int) (screenSize[0] * Global.getSettings().getScreenScaleMult());
        screenSize[1] = (int) (Global.getSettings().getScreenHeight() * Display.getPixelScaleFactor());
        screenSizeScale[1] = (int) (screenSize[1] * Global.getSettings().getScreenScaleMult());
        screenSizeFix[0] = CalculateUtil.getPOTMax(screenSize[0]);
        screenSizeFix[1] = CalculateUtil.getPOTMax(screenSize[1]);
        screenSizeUV[0] = (float) ((double) screenSize[0] / (double) screenSizeFix[0]);
        screenSizeUV[1] = (float) ((double) screenSize[1] / (double) screenSizeFix[1]);
    }

    public static int getScreenWidth() {
        return screenSize[0];
    }

    public static int getScreenHeight() {
        return screenSize[1];
    }

    public static int getScreenScaleWidth() {
        return screenSizeScale[0];
    }

    public static int getScreenScaleHeight() {
        return screenSizeScale[1];
    }

    /**
     * Needless usual.
     */
    public static int getScreenFixWidth() {
        return screenSizeFix[0];
    }

    /**
     * Needless usual.
     */
    public static int getScreenFixHeight() {
        return screenSizeFix[1];
    }

    /**
     * Needless usual.
     */
    public static float getScreenFixU() {
        return screenSizeUV[0];
    }

    /**
     * Needless usual.
     */
    public static float getScreenFixV() {
        return screenSizeUV[1];
    }

    public static BaseShaderData getCommonProgram() {
        return _SHADER_PROGRAM[_COMMON];
    }

    public static BaseShaderData getSpriteProgram() {
        return _SHADER_PROGRAM[_SPRITE];
    }

    public static BaseShaderData getCurveProgram() {
        return _SHADER_PROGRAM[_CURVE];
    }

    public static BaseShaderData getSegmentProgram() {
        return _SHADER_PROGRAM[_SEGMENT];
    }

    public static BaseShaderData getTrailProgram() {
        return _SHADER_PROGRAM[_TRAIL];
    }

    public static BaseShaderData getFlareProgram() {
        return _SHADER_PROGRAM[_FLARE];
    }

    public static BaseShaderData getTextProgram() {
        return _SHADER_PROGRAM[_TEXT];
    }

    public static BaseShaderData getDistortionProgram() {
        return _SHADER_PROGRAM[_DIST];
    }

    public static BaseShaderData getDirectDrawProgram() {
        return _SHADER_PROGRAM[_DIRECT];
    }

    public static BaseShaderData getBloomProgram() {
        return _SHADER_PROGRAM[_BLOOM];
    }

    public static BaseShaderData getFXAACProgram() {
        return _SHADER_PROGRAM[_FXAA_C];
    }

    public static BaseShaderData getFXAAQProgram() {
        return _SHADER_PROGRAM[_FXAA_Q];
    }

    public static BaseShaderData getInstanceMatrixF32Program() {
        return _SHADER_PROGRAM[_MATRIX_F32];
    }

    public static BaseShaderData getInstanceMatrixF16Program() {
        return _SHADER_PROGRAM[_MATRIX_F16];
    }

    public static BaseShaderData getSDFInitProgram() {
        return _SHADER_PROGRAM[_SDF_INIT];
    }

    public static BaseShaderData getSDFProcessProgram() {
        return _SHADER_PROGRAM[_SDF_PROCESS];
    }

    public static BaseShaderData getSDFResultProgram() {
        return _SHADER_PROGRAM[_SDF_RESULT];
    }

    public static BaseShaderData getRadialBlurProgram() {
        return _SHADER_PROGRAM[_RADIAL_BLUR];
    }

    public static BaseShaderData getCompGaussianBlurProgram() {
        return _SHADER_PROGRAM[_GAUSSIAN_BLUR];
    }

    public static BaseShaderData getCompGaussianBlurRedProgram() {
        return _SHADER_PROGRAM[_GAUSSIAN_BLUR_RED];
    }

    public static BaseShaderData getCompBilateralFilterProgram() {
        return _SHADER_PROGRAM[_BILATERAL_FLITER];
    }

    public static BaseShaderData getCompBilateralFilterRedProgram() {
        return _SHADER_PROGRAM[_BILATERAL_FLITER_RED];
    }

    public static BaseShaderData getDFTProgram() {
        return _SHADER_PROGRAM[_DFT];
    }

    public static BaseShaderData getDFTRedProgram() {
        return _SHADER_PROGRAM[_DFT_RED];
    }

    public static BaseShaderData getNormalMapGenInitProgram() {
        return _SHADER_PROGRAM[_NORMAL_GEN_INIT];
    }

    public static BaseShaderData getNormalMapGenResultProgram() {
        return _SHADER_PROGRAM[_NORMAL_GEN_RESULT];
    }

    public static BaseShaderData getNumberProgram() {
        return _SHADER_PROGRAM[_SIMPLE_NUMBER];
    }

    public static BaseShaderData getArcProgram() {
        return _SHADER_PROGRAM[_SIMPLE_ARC];
    }

    public static BaseShaderData getTexArcProgram() {
        return _SHADER_PROGRAM[_SIMPLE_ARC];
    }

    public static BaseShaderData getTestMissionProgram() {
        return _SHADER_PROGRAM[_MISSION_BG];
    }

    public static BUtil_RenderingBuffer getRenderingBuffer() {
        return renderingBuffer;
    }

    public static PublicFBO getPublicFBO() {
        return publicFBO;
    }

    public static void refreshGameViewportMatrix(ViewportAPI viewport) {
        if (matrixUBO == 0) return;
        FloatBuffer matrix = CommonUtil.createFloatBuffer(TransformUtil.createGameOrthoMatrix(viewport, new Matrix4f()));
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matrixUBO);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, matrix);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public static void refreshGameViewportMatrix(FloatBuffer matrix) {
        if (matrixUBO == 0) return;
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matrixUBO);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, matrix);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public static void refreshGameViewportMatrixNone() {
        if (matrixUBO == 0) return;
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matrixUBO);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, _NONE_GAME_MATRIX);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public static void refreshGameScreenState(ViewportAPI viewport) {
        if (matrixUBO == 0) return;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        float llx = viewport.getLLX();
        float lly = viewport.getLLY();
        buffer.put(llx);
        buffer.put(lly);
        buffer.put(Math.abs(viewport.convertScreenXToWorldX(viewport.getVisibleWidth()) - llx));
        buffer.put(Math.abs(viewport.convertScreenYToWorldY(viewport.getVisibleHeight()) - lly));
        buffer.position(0);
        buffer.limit(buffer.capacity());
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matrixUBO);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 16, buffer);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public static void refreshGameVanillaViewportUBOAll(ViewportAPI viewport) {
        if (matrixUBO == 0) return;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(20);
        TransformUtil.createGameOrthoMatrix(viewport, new Matrix4f()).store(buffer);
        float llx = viewport.getLLX();
        float lly = viewport.getLLY();
        buffer.put(llx);
        buffer.put(lly);
        buffer.put(Math.abs(viewport.convertScreenXToWorldX(viewport.getVisibleWidth()) - llx));
        buffer.put(Math.abs(viewport.convertScreenYToWorldY(viewport.getVisibleHeight()) - lly));
        buffer.position(0);
        buffer.limit(buffer.capacity());
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matrixUBO);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, buffer);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public static void refreshGameVanillaViewportUBOAll(FloatBuffer matrix, ViewportAPI viewport) {
        if (matrixUBO == 0) return;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        float llx = viewport.getLLX();
        float lly = viewport.getLLY();
        buffer.put(0, llx);
        buffer.put(1, lly);
        buffer.put(2, Math.abs(viewport.convertScreenXToWorldX(viewport.getVisibleWidth()) - llx));
        buffer.put(3, Math.abs(viewport.convertScreenYToWorldY(viewport.getVisibleHeight()) - lly));
        buffer.position(0);
        buffer.limit(buffer.capacity());
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matrixUBO);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, matrix);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 16 * BoxDatabase.FLOAT_SIZE, buffer);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public static boolean isFramebufferValid() {
        return renderingBuffer != null && renderingBuffer.isFinished(0) && renderingBuffer.isFinished(1);
    }

    public static void refreshRenderingBuffer() {
        if (renderingBuffer != null) {
            for (byte i = 0; i < BUtil_RenderingBuffer.getBufferCount(); i++) {
                if (renderingBuffer.isFinished(i)) renderingBuffer.delete(i);
            }
        }
        renderingBuffer = new BUtil_RenderingBuffer();
        for (byte i = 0; i < BUtil_RenderingBuffer.getBufferCount(); i++) {
            if (renderingBuffer.isFinished(i))
                Global.getLogger(ShaderCore.class).info("'BoxUtil' rendering framebuffer-" + i + " has refreshed.");
            else Global.getLogger(ShaderCore.class).error("'BoxUtil' rendering framebuffer-" + i + " refresh failed.");
        }
    }

    public static boolean isRenderingFramebufferValid() {
        return renderingBuffer != null && renderingBuffer.isFinished(0) && renderingBuffer.isFinished(1);
    }

    public static boolean isPublicFBOValid() {
        return publicFBO != null && publicFBO.isFinished();
    }

    public static void refreshPublicFBO() {
        if (publicFBO != null && publicFBO.isFinished()) publicFBO.delete();

        publicFBO = new PublicFBO();
        int instance = publicFBO.hashCode();
        if (publicFBO.isFinished()) Global.getLogger(ShaderCore.class).info("'BoxUtil' public framebuffer \"" + instance + "\" has refreshed.");
        else Global.getLogger(ShaderCore.class).error("'BoxUtil' public framebuffer \"" + instance + "\" refresh failed.");
    }

    public static SimpleVAOAPI getDefaultPointObject() {
        return defaultPointObject;
    }

    public static void refreshDefaultPointObject() {
        if (defaultPointObject != null) defaultPointObject.destroy();
        defaultPointObject = new PointObject();
        if (defaultPointObject.isValid())
            Global.getLogger(ShaderCore.class).info("'BoxUtil' default point object has refreshed.");
        else Global.getLogger(ShaderCore.class).error("'BoxUtil' default point object refresh failed.");
    }

    public static SimpleVAOAPI getDefaultLineObject() {
        return defaultLineObject;
    }

    public static void refreshDefaultLineObject() {
        if (defaultLineObject != null) defaultLineObject.destroy();
        defaultLineObject = new LineObject();
        if (defaultLineObject.isValid())
            Global.getLogger(ShaderCore.class).info("'BoxUtil' default line object has refreshed.");
        else Global.getLogger(ShaderCore.class).error("'BoxUtil' default line object refresh failed.");
    }

    public static SimpleVAOAPI getDefaultQuadObject() {
        return defaultQuadObject;
    }

    public static void refreshDefaultQuadObject() {
        if (defaultQuadObject != null) defaultQuadObject.destroy();
        defaultQuadObject = new QuadObject();
        if (defaultQuadObject.isValid())
            Global.getLogger(ShaderCore.class).info("'BoxUtil' default quad object has refreshed.");
        else Global.getLogger(ShaderCore.class).error("'BoxUtil' default quad object refresh failed.");
    }

    public static boolean isDefaultVAOValid() {
        if (defaultPointObject == null || defaultLineObject == null || defaultQuadObject == null) return false;
        return defaultPointObject.isValid() && defaultLineObject.isValid() && defaultQuadObject.isValid();
    }

    public static void refreshDefaultVAO() {
        refreshDefaultPointObject();
        refreshDefaultLineObject();
        refreshDefaultQuadObject();
    }

    public static int getMatrixUBO() {
        return matrixUBO;
    }

    public static byte getMatrixUBOBinding() {
        return _GLSL_MATRIX_UBO_BINDING;
    }

    public static boolean isInitialized() {
        return glFinished;
    }

    public static boolean isValid() {
        return glValid;
    }

    public static boolean isMainProgramValid() {
        return glMainProgramValid;
    }

    public static boolean isDistortionValid() {
        return glDistortionValid;
    }

    public static boolean isBloomValid() {
        return glBloomValid;
    }

    public static boolean isFXAACValid() {
        return glFXAACValid;
    }

    public static boolean isFXAAQValid() {
        return glFXAAQValid;
    }

    public static boolean isMatrixProgramValid() {
        return glInstanceMatrixValid;
    }

    public static boolean isSDFGenValid() {
        return glSDFGenValid;
    }

    public static boolean isRadialBlurValid() {
        return glRadialBlurValid;
    }

    public static boolean isCompGaussianBlurValid() {
        return glCompGaussianBlurValid;
    }

    public static boolean isCompBilateralFilterValid() {
        return glCompBilateralFilterValid;
    }

    public static boolean isDiscreteFourierValid() {
        return glDiscreteFourierValid;
    }

    public static boolean isNormalMapGenValid() {
        return glNormalMapGenValid;
    }

    private ShaderCore() {}
}
