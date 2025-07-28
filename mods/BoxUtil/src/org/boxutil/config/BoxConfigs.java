package org.boxutil.config;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Pair;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.builtin.gui.BUtil_BaseTrackbar;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import java.io.IOException;

public final class BoxConfigs {
    // Global config values.
    private static boolean BUtil_EnableShader = true;
    private static boolean BUtil_EnableShaderLocal = true;
    private static boolean BUtil_EnableShaderDisplay = true;
    private static byte BUtil_ParallelType = BoxEnum.PARALLEL_GL;
    private static byte BUtil_ParallelTypeLocal = BoxEnum.PARALLEL_GL;
    private static byte BUtil_ParallelTypeDisplay = BoxEnum.PARALLEL_GL;

    // Dynamic config values.
    private static byte BUtil_ShaderMode = BoxEnum.MODE_COMMON;
    private static boolean BUtil_EnableBloom = true;
    private static boolean BUtil_EnableBloomDisplay = true;
    private static byte BUtil_AntiAliasingState = BoxEnum.AA_FXAA_QUALITY;
    private static byte BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_QUALITY;
    private static boolean BUtil_DepthAA = false;
    private static int BUtil_InstanceClamp = 8192;
    private static short BUtil_CurveNode = 32;
    private static short BUtil_CurveInterpolation = 32;
    private static boolean BUtil_EnableDistortion = true;
    private static boolean BUtil_EnableDistortionDisplay = true;

    // Misc config values.
    private final static boolean _SHOW_CN_TRANSLATION_CREDITS = Global.getSettings().getBoolean("showCNTranslationCredits");
    private static String BUtil_Language = _SHOW_CN_TRANSLATION_CREDITS ? BoxDatabase.ZH_CN :BoxDatabase.EN_US;
    private static boolean BUtil_EnableDebug = false;
    private static boolean BUtil_EnableDebugLocal = false;
    private static boolean BUtil_EnableDebugDisplay = false;

    // Dev config values.
    private static boolean BUtil_AAStatus = false;
    private static byte BUtil_MultiPassMode = BoxEnum.MP_BEAUTY;

    private static boolean configInit = false;
    private static JSONObject data = null;

    public static String getRebootValueRealString(byte master, byte item) {
        String result = "BUtil_ConfigPanel_";
        switch (master) {
            case 0: {
                result += "Global_";
                switch (item) {
                    case 0: {
                        result = BUtil_EnableShader ? "BUtil_ConfigPanel_ValueValid" : "BUtil_ConfigPanel_ValueInvalid";
                        break;
                    }
                    case 1: {
                        result += "01V";
                        result += BUtil_ParallelType;
                        result += BoxDatabase.NONE_LANG;
                    }
                }
                break;
            }
            case 2: {
                result += "Misc_";
                switch (item) {
                    case 1: {
                        result = BUtil_EnableDebug ? "BUtil_ConfigPanel_ValueValid" : "BUtil_ConfigPanel_ValueInvalid";
                    }
                }
            }
        }
        return result;
    }

    public static Pair<String, Boolean> getValueString(byte master, byte item) {
        String result = "BUtil_ConfigPanel_";
        boolean valid = true;
        boolean direct = false;
        switch (master) {
            case 0: {
                result += "Global_";
                switch (item) {
                    case 0: {
                        result = BUtil_EnableShaderDisplay ? "BUtil_ConfigPanel_ValueValid" : "BUtil_ConfigPanel_ValueInvalid";

                        valid = isBaseGL42Supported();
                        break;
                    }
                    case 1: {
                        result += "01V";
                        result += BUtil_ParallelTypeDisplay;
                        result += BoxDatabase.NONE_LANG;

                        if (BUtil_ParallelTypeDisplay == BoxEnum.PARALLEL_GL) valid = isGLParallelSupported();
                    }
                }
                break;
            }
            case 1: {
                result += "Common_";
                switch (item) {
                    case 0: {
                        result += "00V";
                        result += BUtil_ShaderMode;
                        break;
                    }
                    case 1: {
                        result = BUtil_EnableBloomDisplay ? "BUtil_ConfigPanel_ValueValid" : "BUtil_ConfigPanel_ValueInvalid";

                        valid = ShaderCore.isBloomValid();
                        break;
                    }
                    case 2: {
                        result += "02V";
                        result += BUtil_AntiAliasingStateDisplay;
                        if (BUtil_AntiAliasingStateDisplay != BoxEnum.AA_DISABLE) result += BoxDatabase.NONE_LANG;

                        if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_QUALITY) valid = ShaderCore.isFXAAQValid();
                        if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_CONSOLE) valid = ShaderCore.isFXAACValid();
                        break;
                    }
                    case 3: {
                        result = BUtil_DepthAA ? "BUtil_ConfigPanel_ValueValid" : "BUtil_ConfigPanel_ValueInvalid";
                        break;
                    }
                    case 4: {
                        direct = true;
                        result = String.valueOf(BUtil_InstanceClamp);
                        break;
                    }
                    case 5: {
                        direct = true;
                        result = String.valueOf(BUtil_CurveNode);
                        break;
                    }
                    case 6: {
                        direct = true;
                        result = String.valueOf(BUtil_CurveInterpolation);
                        break;
                    }
                    case 7: {
                        result = BUtil_EnableDistortionDisplay ? "BUtil_ConfigPanel_ValueValid" : "BUtil_ConfigPanel_ValueInvalid";

                        valid = ShaderCore.isDistortionValid();
                    }
                }
                break;
            }
            case 2: {
                result += "Misc_";
                switch (item) {
                    case 0: {
                        result += "00V";
                        break;
                    }
                    case 1: {
                        result = BUtil_EnableDebugDisplay ? "BUtil_ConfigPanel_ValueValid" : "BUtil_ConfigPanel_ValueInvalid";

                        valid = isGLDebugOutputSupported();
                    }
                }
            }
        }
        return new Pair<>(direct ? result : getString(result), valid);
    }

    public static void setValue(byte master, byte item, boolean right, BUtil_BaseTrackbar trackbar) {
        switch (master) {
            case 0: {
                switch (item) {
                    case 0: {
                        BUtil_EnableShaderDisplay = !BUtil_EnableShaderDisplay;
                        break;
                    }
                    case 1: {
                        BUtil_ParallelTypeDisplay = (BUtil_ParallelTypeDisplay == BoxEnum.PARALLEL_GL) ? BoxEnum.PARALLEL_JVM : BoxEnum.PARALLEL_GL;
                    }
                }
                break;
            }
            case 1: {
                switch (item) {
                    case 0: {
                        if (BUtil_ShaderMode == BoxEnum.MODE_COMMON) BUtil_ShaderMode = BoxEnum.MODE_COLOR; else BUtil_ShaderMode = BoxEnum.MODE_COMMON;
                        break;
                    }
                    case 1: {
                        BUtil_EnableBloomDisplay = !BUtil_EnableBloomDisplay;
                        break;
                    }
                    case 2: {
                        if (right) {
                            if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_QUALITY) BUtil_AntiAliasingStateDisplay = BoxEnum.AA_DISABLE;
                            else if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_DISABLE) BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_CONSOLE;
                            else if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_CONSOLE) BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_QUALITY;
                            else BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_QUALITY;
                        } else {
                            if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_QUALITY) BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_CONSOLE;
                            else if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_DISABLE) BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_QUALITY;
                            else if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_CONSOLE) BUtil_AntiAliasingStateDisplay = BoxEnum.AA_DISABLE;
                            else BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_QUALITY;
                        }
                        break;
                    }
                    case 3: {
                        BUtil_DepthAA = !BUtil_DepthAA;
                        break;
                    }
                    case 4: {
                        if (trackbar != null) {
                            if (trackbar.getCurrStep() == trackbar.getMaxStep()) BUtil_InstanceClamp = -1;
                            else if (trackbar.getCurrStep() == 0) BUtil_InstanceClamp = 0;
                            else BUtil_InstanceClamp = 128 << trackbar.getCurrStep();
                        }
                        break;
                    }
                    case 5: {
                        if (trackbar != null) {
                            if (trackbar.getCurrStep() == trackbar.getMaxStep()) BUtil_CurveNode = -1;
                            else if (trackbar.getCurrStep() == 0) BUtil_CurveNode = 0;
                            else BUtil_CurveNode = (short) (4 << trackbar.getCurrStep());
                        }
                        break;
                    }
                    case 6: {
                        if (trackbar != null) {
                            if (trackbar.getCurrStep() == trackbar.getMaxStep()) BUtil_CurveInterpolation = -1;
                            else if (trackbar.getCurrStep() == 0) BUtil_CurveInterpolation = 0;
                            else BUtil_CurveInterpolation = (short) (8 << trackbar.getCurrStep());
                        }
                        break;
                    }
                    case 7: {
                        BUtil_EnableDistortionDisplay = !BUtil_EnableDistortionDisplay;
                    }
                }
                break;
            }
            case 2: {
                switch (item) {
                    case 0: {
                        if (BUtil_Language.contentEquals(BoxDatabase.ZH_CN)) BUtil_Language = BoxDatabase.EN_US;
                        else BUtil_Language = BoxDatabase.ZH_CN;
                        break;
                    }
                    case 1: {
                        BUtil_EnableDebugDisplay = !BUtil_EnableDebugDisplay;
                    }
                }
            }
        }
    }

    /**
     * Loading before all.
     */
    public static void init() {
        if (configInit) return;
        boolean haveData = true;
        try {
            data = new JSONObject(Global.getSettings().readTextFileFromCommon(BoxDatabase.CONFIG_FILE_PATH));
        } catch (IOException | JSONException ignored) {}
        if (data == null || data.length() <= 0) {
            haveData = false;
            data = new JSONObject();
            save();
        }
        if (haveData) {
            BUtil_EnableShader = data.optBoolean("BUtil_EnableShader");
            BUtil_EnableShaderLocal = BUtil_EnableShader;
            BUtil_ParallelType = (byte) data.optInt("BUtil_ParallelType");
            BUtil_ParallelTypeLocal = BUtil_ParallelType;

            BUtil_EnableDebug = data.optBoolean("BUtil_EnableDebug");
            BUtil_EnableDebugLocal = BUtil_EnableDebug;
            load();
        }
        configInit = true;
    }

    public static void sysCheck() {
        if (!ShaderCore.isValid()) {
            BUtil_EnableShader = false;
        }
        if (isGLParallel() && !ShaderCore.isMatrixProgramValid()) {
            BUtil_ParallelType = BoxEnum.PARALLEL_JVM;
        }
    }

    /**
     * After all.
     */
    public static void check() {
        if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_QUALITY && !ShaderCore.isFXAAQValid()) {
            BUtil_AntiAliasingState = BoxEnum.AA_FXAA_CONSOLE;
            BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_CONSOLE;
        }
        if (BUtil_AntiAliasingStateDisplay == BoxEnum.AA_FXAA_CONSOLE && !ShaderCore.isFXAACValid()) {
            BUtil_AntiAliasingState = BoxEnum.AA_DISABLE;
            BUtil_AntiAliasingStateDisplay = BoxEnum.AA_DISABLE;
        }
        if (!ShaderCore.isBloomValid()) {
            BUtil_EnableBloom = false;
            BUtil_EnableBloomDisplay = false;
        }
        if (!ShaderCore.isDistortionValid()) {
            BUtil_EnableDistortion = false;
            BUtil_EnableDistortionDisplay = false;
        }
        if (!isGLDebugOutputSupported()) {
            BUtil_EnableDebug = false;
            BUtil_EnableDebugDisplay = false;
        }
    }

    public static void setDefault() {
        BUtil_EnableShaderDisplay = true;
        BUtil_ParallelTypeDisplay = BoxEnum.PARALLEL_GL;

        BUtil_ShaderMode = BoxEnum.MODE_COMMON;
        BUtil_EnableBloomDisplay = true;
        BUtil_AntiAliasingStateDisplay = BoxEnum.AA_FXAA_QUALITY;
        BUtil_DepthAA = false;
        BUtil_InstanceClamp = 8192;
        BUtil_CurveNode = 32;
        BUtil_CurveInterpolation = 32;
        BUtil_EnableDistortionDisplay = true;

        BUtil_Language = _SHOW_CN_TRANSLATION_CREDITS ? BoxDatabase.ZH_CN : BoxDatabase.EN_US;
        BUtil_EnableDebugDisplay = false;
    }

    public static void load() {
        try {
            BUtil_EnableShaderDisplay = BUtil_EnableShaderLocal;
            BUtil_ParallelTypeDisplay = BUtil_ParallelTypeLocal;

            BUtil_ShaderMode = (byte) data.getInt("BUtil_ShaderMode");
            BUtil_EnableBloomDisplay = data.getBoolean("BUtil_EnableBloom");
            BUtil_AntiAliasingStateDisplay = (byte) data.getInt("BUtil_AntiAliasingState");
            BUtil_DepthAA = data.getBoolean("BUtil_DepthAA");
            {
                int value = data.getInt("BUtil_InstanceClamp");
                if (value > 65536 || value < -1) value = 8192;
                BUtil_InstanceClamp = value;
            }
            {
                short value = (short) data.getInt("BUtil_CurveNode");
                if (value > 512 || value < -1) value = 32;
                BUtil_CurveNode = value;
            }
            {
                short value = (short) data.getInt("BUtil_CurveInterpolation");
                if (value > 256 || value < -1) value = 32;
                BUtil_CurveInterpolation = value;
            }
            BUtil_EnableDistortionDisplay = data.getBoolean("BUtil_EnableDistortion");

            { // language check
                String rawValue = data.getString("BUtil_Language");
                if (rawValue.contentEquals(BoxDatabase.EN_US)) {
                    BUtil_Language = BoxDatabase.EN_US;
                } else BUtil_Language = BoxDatabase.ZH_CN;
            }
            BUtil_EnableDebugDisplay = BUtil_EnableDebugLocal;
        } catch (JSONException e) {
            StackTraceElement[] stacks = e.getStackTrace();
            for (StackTraceElement stack : stacks) {
                if (stack != null) Global.getLogger(BoxConfigs.class).info(stack.toString());
            }
        }
    }

    public static void save() {
        try {
            data.put("BUtil_EnableShader", BUtil_EnableShaderDisplay);
            data.put("BUtil_ParallelType", BUtil_ParallelTypeDisplay);

            data.put("BUtil_ShaderMode", BUtil_ShaderMode);
            data.put("BUtil_EnableBloom", BUtil_EnableBloomDisplay);
            BUtil_EnableBloom = BUtil_EnableBloomDisplay;
            data.put("BUtil_AntiAliasingState", BUtil_AntiAliasingStateDisplay);
            BUtil_AntiAliasingState = BUtil_AntiAliasingStateDisplay;
            data.put("BUtil_DepthAA", BUtil_DepthAA);
            data.put("BUtil_InstanceClamp", BUtil_InstanceClamp);
            data.put("BUtil_CurveNode", BUtil_CurveNode);
            data.put("BUtil_CurveInterpolation", BUtil_CurveInterpolation);
            data.put("BUtil_EnableDistortion", BUtil_EnableDistortionDisplay);
            BUtil_EnableDistortion = BUtil_EnableDistortionDisplay;

            data.put("BUtil_Language", BUtil_Language);
            data.put("BUtil_EnableDebug", BUtil_EnableDebugDisplay);
            Global.getSettings().writeTextFileToCommon(BoxDatabase.CONFIG_FILE_PATH, data.toString(4));
        } catch (IOException | JSONException e) {
            StackTraceElement[] stacks = e.getStackTrace();
            for (StackTraceElement stack : stacks) {
                if (stack != null) Global.getLogger(BoxConfigs.class).info(stack.toString());
            }
        }
    }

    public static boolean isConfigInit() {
        return configInit;
    }

    public static boolean isBaseGL42Supported() {
        ContextCapabilities cap = GLContext.getCapabilities();
        return cap.OpenGL42 && cap.GL_ARB_texture_non_power_of_two && cap.GL_ARB_texture_buffer_object && cap.GL_ARB_uniform_buffer_object && cap.GL_ARB_shader_subroutine;
    }

    public static boolean isGLParallelSupported() {
        ContextCapabilities cap = GLContext.getCapabilities();
        return cap.OpenGL43 && cap.GL_ARB_compute_shader && cap.GL_ARB_shader_image_load_store;
    }

    public static boolean isGLDebugOutputSupported() {
        ContextCapabilities cap = GLContext.getCapabilities();
        return cap.OpenGL43 && cap.GL_KHR_debug;
    }

    public static boolean isTBOSupported() {
        return GLContext.getCapabilities().OpenGL31 && GLContext.getCapabilities().GL_ARB_texture_buffer_object;
    }

    public static boolean isVAOSupported() {
        return GLContext.getCapabilities().OpenGL30 && GLContext.getCapabilities().GL_ARB_vertex_array_object && GLContext.getCapabilities().GL_ARB_vertex_buffer_object;
    }

    public static int getMaxInstanceDataSize() {
        return BUtil_InstanceClamp == -1 ? BoxDatabase.getGLState().DEVICE_MAX_INSTANCE_DATA_SIZE : BUtil_InstanceClamp;
    }

    public static int getMaxSegmentNodeSize() {
        return getMaxInstanceDataSize() / 2;
    }

    public static boolean isShaderEnable() {
        return BUtil_EnableShader;
    }

    public static boolean isShaderCommonMode() {
        return BUtil_ShaderMode == BoxEnum.MODE_COMMON;
    }

    public static boolean isShaderColorMode() {
        return BUtil_ShaderMode == BoxEnum.MODE_COLOR;
    }

    public static short getMaxCurveNodeSize() {
        return BUtil_CurveNode == -1 ? Short.MAX_VALUE : BUtil_CurveNode;
    }

    public static short getMaxCurveInterpolation() {
        return BUtil_CurveInterpolation == -1 ? Short.MAX_VALUE : BUtil_CurveInterpolation;
    }

    public static boolean isBloomEnabled() {
        return BUtil_EnableBloom;
    }

    public static boolean isAADisabled() {
        return BUtil_AntiAliasingState == BoxEnum.AA_DISABLE;
    }

    public static boolean isFXAA() {
        return isFXAAC() || isFXAAQ();
    }

    public static boolean isFXAAC() {
        return BUtil_AntiAliasingState == BoxEnum.AA_FXAA_CONSOLE;
    }

    public static boolean isFXAAQ() {
        return BUtil_AntiAliasingState == BoxEnum.AA_FXAA_QUALITY;
    }

    public static boolean isDepthAA() {
        return BUtil_DepthAA;
    }

    public static boolean isDistortionEnable() {
        return BUtil_EnableDistortion;
    }

    public static boolean isGLParallel() {
        return BUtil_ParallelType == BoxEnum.PARALLEL_GL;
    }

    public static boolean isCLParallel() {
        return BUtil_ParallelType == BoxEnum.PARALLEL_CL;
    }

    public static boolean isJVMParallel() {
        return BUtil_ParallelType == BoxEnum.PARALLEL_JVM;
    }

    public static String getLanguage() {
        return BUtil_Language;
    }

    public static boolean isGLDebugEnable() {
        return BUtil_EnableDebug;
    }

    public static byte getMultiPassMode() {
        return BUtil_MultiPassMode;
    }

    public static boolean isMultiPassBeauty() {
        return BUtil_MultiPassMode == BoxEnum.MP_BEAUTY;
    }

    public static boolean isMultiPassData() {
        return BUtil_MultiPassMode == BoxEnum.MP_DATA;
    }

    public static boolean isMultiPassEmissive() {
        return BUtil_MultiPassMode == BoxEnum.MP_EMISSIVE;
    }

    public static boolean isMultiPassNormal() {
        return BUtil_MultiPassMode == BoxEnum.MP_NORMAL;
    }

    public static boolean isMultiPassBloom() {
        return BUtil_MultiPassMode == BoxEnum.MP_BLOOM;
    }

    public static void setMultiPassMode(byte mode) {
        BUtil_MultiPassMode = mode;
    }

    public static boolean isAAShowEdge() {
        return BUtil_AAStatus;
    }

    public static void setAAShowEdge(boolean value) {
        BUtil_AAStatus = value;
    }

    private static String getString(String id) {
        String current = id;
        if (!id.endsWith(BoxDatabase.NONE_LANG)) current += BoxConfigs.getLanguage();
        return Global.getSettings().getString("ui", current);
    }

    private BoxConfigs() {}
}
