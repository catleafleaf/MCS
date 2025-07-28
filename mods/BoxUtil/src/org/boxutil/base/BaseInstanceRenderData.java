package org.boxutil.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.boxutil.base.api.InstanceDataAPI;
import org.boxutil.base.api.InstanceRenderAPI;
import org.boxutil.base.api.SimpleVAOAPI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.standard.attribute.MaterialData;
import org.boxutil.util.CommonUtil;
import org.boxutil.util.TrigUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseInstanceRenderData extends BaseRenderData implements InstanceRenderAPI {
    protected static final byte _FINAL_MATRIX = 0;
    protected static final byte _FIXED_FINAL_MATRIX_RG = 3;
    protected static final byte _TIMER = 1;
    protected static final byte _STATE = 2;
    protected static final byte _COLOR = 3;
    // final(vec4 * 2), timer(vec4), state(vec4 * 2), color(ivec4)
    // 2 * tex, 1 * tex, 2 * tex, 1 * tex
    protected static final byte[] _TBO_COUNT_2D = new byte[]{2, 1, 2, 1};
    // final(vec4 * 3), timer(vec4), state(vec4 * 4), color(ivec4)
    // 3 * tex, 1 * tex, 4 * tex, 1 * tex
    protected static final byte[] _TBO_COUNT_3D = new byte[]{3, 1, 4, 1};
    protected final int[] instanceDataFormat = new int[]{GL30.GL_RGBA32F, GL30.GL_RGBA32F, GL30.GL_RGBA32F, GL30.GL_RG32F}; // final, timer, state, fixedFinalRG
    protected List<InstanceDataAPI> instanceData = null;
    // [0][0]final0       = vec4
    // [0][1]final1       = vec4
    // [0][2]final2       = vec4 opt.
    // [1][0]timer        = vec4 **
    // [2][0]state0       = vec4
    // [2][1]state1-3/1-2 = vec12/vec8
    protected final FloatBuffer[][] _instanceDataJVM = new FloatBuffer[][]{null, null, null};
    // final rgba16f * 2/3 / rgba32f * 2/3
    // timer rgba32f * 1
    // state rgba16f * 2/4 / rgba32f * 2/4
    // color rgba16f * 1
    protected final int[][] _instanceDataTBOTex = new int[][]{new int[_TBO_COUNT_3D[_FINAL_MATRIX]], new int[_TBO_COUNT_3D[_TIMER]], new int[_TBO_COUNT_3D[_STATE]], new int[_TBO_COUNT_3D[_COLOR]]};
    protected final int[][] _instanceDataTBO = new int[][]{new int[_TBO_COUNT_3D[_FINAL_MATRIX]], new int[_TBO_COUNT_3D[_TIMER]], new int[_TBO_COUNT_3D[_STATE]], new int[_TBO_COUNT_3D[_COLOR]]};
    // [0][0]final0       => share
    // [0][1]final1       => share
    // [0][2]final2       => share opt.
    // [1][0]timer        => cl
    // [2][0]state0       => share
    // [2][1]state1-3/1-2 => cl
//    protected final CLMem[][] _instanceDataMemory = new CLMem[][]{null, null, null, null}; // none color tex, global
    protected final int[] instanceRefreshState = new int[]{0, 0, 0, 0}; // index, size, lastSize, renderingCount
    protected final byte[] instanceDataType = new byte[]{0, BoxDatabase.FLOAT_SIZE}; // {0-2D, 1-3D, 2-Custom}, data format
    protected float instanceTimerOverride = -1.0f;
    protected final boolean[] needRefreshInstanceData = new boolean[]{false, false, false}; // once, always, isFixed

    public BaseInstanceRenderData() {}

    public void delete() {
        super.delete();
        this.resetInstanceData();
    }

    public void glDraw() {
        SimpleVAOAPI quad = ShaderCore.getDefaultQuadObject();
        if (quad == null || !quad.isValid()) return;
        quad.glDraw(Math.min(this.getValidInstanceDataCount(), this.getRenderingCount()));
    }

    /**
     * When call it, will reset data and delete related objects.<p>
     * A.K.A. <strong>free()</strong> for instance data resources.
     */
    public void resetInstanceData() {
        this.instanceData = null;
        this.instanceRefreshState[0] = 0;
        this.instanceRefreshState[1] = 0;
        final boolean instance3DCheck = this.isInstanceData3D();
        if (this.haveValidInstanceData() && BoxConfigs.isTBOSupported()) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
            if (BoxConfigs.isJVMParallel()) {
                this._instanceDataJVM[0] = null;
                this._instanceDataJVM[1] = null;
                this._instanceDataJVM[2] = null;
            } else if (BoxConfigs.isGLParallel()) {
                GL11.glDeleteTextures(this._instanceDataTBOTex[_TIMER][0]);
                GL15.glDeleteBuffers(this._instanceDataTBO[_TIMER][0]);
                GL11.glDeleteTextures(this._instanceDataTBOTex[_STATE][1]);
                GL15.glDeleteBuffers(this._instanceDataTBO[_STATE][1]);
                if (instance3DCheck) {
                    GL11.glDeleteTextures(this._instanceDataTBOTex[_STATE][2]);
                    GL15.glDeleteBuffers(this._instanceDataTBO[_STATE][2]);
                    GL11.glDeleteTextures(this._instanceDataTBOTex[_STATE][3]);
                    GL15.glDeleteBuffers(this._instanceDataTBO[_STATE][3]);
                }
                this._instanceDataTBOTex[_TIMER][0] = 0;
                this._instanceDataTBO[_TIMER][0] = 0;
                this._instanceDataTBOTex[_STATE][1] = 0;
                this._instanceDataTBO[_STATE][1] = 0;
                this._instanceDataTBOTex[_STATE][2] = 0;
                this._instanceDataTBO[_STATE][2] = 0;
                this._instanceDataTBOTex[_STATE][3] = 0;
                this._instanceDataTBO[_STATE][3] = 0;
            }/* else if (BoxConfigs.isCLParallel()) {
                CL10.clReleaseMemObject(this._instanceDataMemory[_FINAL_MATRIX][0]);
                CL10.clReleaseMemObject(this._instanceDataMemory[_FINAL_MATRIX][3]);
                CL10.clReleaseMemObject(this._instanceDataMemory[_FINAL_MATRIX][1]);
                CL10.clReleaseMemObject(this._instanceDataMemory[_FINAL_MATRIX][4]);
                CL10.clReleaseMemObject(this._instanceDataMemory[_TIMER][0]);
                CL10.clReleaseMemObject(this._instanceDataMemory[_STATE][2]);
                if (instance3DCheck) {
                    CL10.clReleaseMemObject(this._instanceDataMemory[_STATE][0]);
                    CL10.clReleaseMemObject(this._instanceDataMemory[_STATE][1]);
                    CL10.clReleaseMemObject(this._instanceDataMemory[_FINAL_MATRIX][2]);
                    CL10.clReleaseMemObject(this._instanceDataMemory[_FINAL_MATRIX][5]);
                }
                CL10.clReleaseMemObject(this._instanceDataMemory[3][0]);
                this._instanceDataMemory[_FINAL_MATRIX] = null;
                this._instanceDataMemory[_TIMER] = null;
                this._instanceDataMemory[_STATE] = null;
                this._instanceDataMemory[3] = null;
            }*/
            GL11.glDeleteTextures(this._instanceDataTBOTex[_FINAL_MATRIX][0]);
            GL15.glDeleteBuffers(this._instanceDataTBO[_FINAL_MATRIX][0]);
            GL11.glDeleteTextures(this._instanceDataTBOTex[_FINAL_MATRIX][1]);
            GL15.glDeleteBuffers(this._instanceDataTBO[_FINAL_MATRIX][1]);
            GL11.glDeleteTextures(this._instanceDataTBOTex[_COLOR][0]);
            GL15.glDeleteBuffers(this._instanceDataTBO[_COLOR][0]);
            if (instance3DCheck) {
                GL11.glDeleteTextures(this._instanceDataTBOTex[_FINAL_MATRIX][2]);
                GL15.glDeleteBuffers(this._instanceDataTBO[_FINAL_MATRIX][2]);
                GL11.glDeleteTextures(this._instanceDataTBOTex[_STATE][0]);
                GL15.glDeleteBuffers(this._instanceDataTBO[_STATE][0]);
            }
            this._instanceDataTBOTex[_FINAL_MATRIX][0] = 0;
            this._instanceDataTBO[_FINAL_MATRIX][0] = 0;
            this._instanceDataTBOTex[_FINAL_MATRIX][1] = 0;
            this._instanceDataTBO[_FINAL_MATRIX][1] = 0;
            this._instanceDataTBOTex[_FINAL_MATRIX][2] = 0;
            this._instanceDataTBO[_FINAL_MATRIX][2] = 0;
            this._instanceDataTBOTex[_STATE][0] = 0;
            this._instanceDataTBO[_STATE][0] = 0;
            this._instanceDataTBOTex[_COLOR][0] = 0;
            this._instanceDataTBO[_COLOR][0] = 0;
        }
        this.instanceRefreshState[2] = 0;
        this.instanceRefreshState[3] = 0;
        this.needRefreshInstanceData[0] = false;
        this.needRefreshInstanceData[1] = false;
        this.needRefreshInstanceData[2] = false;
        this.setUseDefaultInstanceData();
        this.setUseDefaultFormatInstanceData();
    }

    public List<InstanceDataAPI> getInstanceData() {
        return this.instanceData;
    }

    /**
     * For rendering a lots of entities (such as 10k+ entities) at once.<p>
     * For all render objects, location/facing/size/color/emissive/alpha data all form {@link InstanceDataAPI}, and overlay to {@link org.boxutil.base.api.RenderDataAPI} implements.<p>
     * Render count decided by <strong>List.size()</strong>.<p>
     * Have size limit, pick smallest from {@link BoxConfigs#getMaxInstanceDataSize()}.<p>
     * This entity attribute, {@link MaterialData#getColor()} and {@link MaterialData#getEmissiveColor()} both apply to each instance when rendering.<p>
     * For model matrix, each instance is also derived from this entity.
     *
     * @param instanceData set to 'null' or empty 'List' -> clear, or a 'List' what isn't empty.
     * @return return {@link BoxEnum#STATE_SUCCESS} when success, return {@link BoxEnum#STATE_SUCCESS} when null list that set to empty list.
     */
    public byte setInstanceData(@Nullable List<InstanceDataAPI> instanceData) {
        if (instanceData == null) {
            this.instanceData = new ArrayList<>(8);
            return BoxEnum.STATE_FAILED;
        }
        List<InstanceDataAPI> check = instanceData;
        int limit = BoxConfigs.getMaxInstanceDataSize();
        if (instanceData.size() > limit) check = check.subList(limit - 1, instanceData.size());
        float[] checkTimer = new float[3];
        float[] tmp;
        for (InstanceDataAPI data : check) {
            tmp = data.getTimer();
            if (checkTimer[0] > -500.0f && tmp[1] < checkTimer[0]) checkTimer[0] = tmp[1];
            if (checkTimer[1] > -500.0f && tmp[2] < checkTimer[1]) checkTimer[1] = tmp[2];
            if (checkTimer[2] > -500.0f && tmp[3] < checkTimer[2]) checkTimer[2] = tmp[3];
        }
        this.globalTimer[1] = checkTimer[0];
        this.globalTimer[2] = checkTimer[1];
        this.globalTimer[3] = checkTimer[2];
        this.instanceData = check;
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * Reset global timer by manual.<p>
     * Similar to {@link #setInstanceData(List)}.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success, return {@link BoxEnum#STATE_FAILED} when null list that set to empty list.
     */
    public byte setInstanceData(@Nullable List<InstanceDataAPI> instanceData, float fadeIn, float full, float fadeOut) {
        if (instanceData == null) {
            this.instanceData = new ArrayList<>(8);
            return BoxEnum.STATE_FAILED;
        }
        List<InstanceDataAPI> check = instanceData;
        int limit = BoxConfigs.getMaxInstanceDataSize();
        if (instanceData.size() > limit) check = check.subList(limit - 1, instanceData.size());
        this.setGlobalTimer(fadeIn, full, fadeOut);
        this.instanceData = check;
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * Have size limit {@link BoxConfigs#getMaxInstanceDataSize()}.<p>
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success, return {@link BoxEnum#STATE_FAILED} when over limit.
     */
    public byte addInstanceData(@NotNull InstanceDataAPI instanceData) {
        if (this.instanceData == null) this.instanceData = new ArrayList<>();
        if (this.instanceData.size() > BoxConfigs.getMaxInstanceDataSize()) return BoxEnum.STATE_FAILED;
        float[] tmp = instanceData.getTimer();
        if (tmp[1] < this.globalTimer[1] && tmp[1] > -500.0f)
            this.globalTimer[1] = tmp[1];
        if (tmp[2] < this.globalTimer[2] && tmp[2] > -500.0f)
            this.globalTimer[2] = tmp[2];
        if (tmp[3] < this.globalTimer[3] && tmp[3] > -500.0f)
            this.globalTimer[3] = tmp[3];
        this.instanceData.add(instanceData);
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * Use it when you changed the instance data.<p>
     * Put all data, but without calculating.<p>
     * For calculating, if running on JVM mode, it will not create any tmp-TBO, will get a matrix-TBO finally.<p>
     * <p>
     * <strong>Should have reserved size for instance-list, if adds more data later. If not, will clear previous data of all in texture objects when submit.</strong><p>
     * If list size has increased, will force to submit for all data.<p>
     * For submit, here will use {@link GL15#glBufferSubData(int, long, ByteBuffer)} for update TBOs in once ergodic, else {@link GL15#glMapBuffer(int, int, ByteBuffer)} is unable to meet demand.<p>
     * <p>
     * In half-float mode, Cost <strong>64Byte</strong> of vRAM each 2D-instance, when running on GL mode, entirely GPU calculate; In other words, rendering <strong>32768</strong> instances will cost <strong>1.75MB</strong> of vRAM.<p>
     * For 3D-instance cost <strong>88Byte</strong> and <strong>32768</strong> instances cost <strong>2.5MB</strong> of vRAM.<p>
     * Cost double vRAM in normal-float mode for each instance data.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when an empty instance data list or refresh count is zero.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte submitInstanceData() {
        if (this.instanceData == null || this.instanceData.isEmpty()) return BoxEnum.STATE_FAILED;
        if (!BoxConfigs.isTBOSupported() || this.isCalledFixedSubmit()) return BoxEnum.STATE_FAILED_OTHER;
        final int size = this.instanceData.size();
        final boolean newTex = size > this.instanceRefreshState[2];
        final int refreshIndex = newTex ? 0 : this.instanceRefreshState[0];
        final int refreshCount = newTex ? size - refreshIndex : this.instanceRefreshState[1];
        if (refreshCount < 1) return BoxEnum.STATE_FAILED;
        final int refreshLimit = refreshIndex + refreshCount;
        final int pixelSize = refreshCount * 4;
        final long dynamicSize = pixelSize * (long) this.getInstanceDataFormat();
        final long bit32Size = pixelSize * 4L;
        final boolean instance3DCheck = this.isInstanceData3D();
        final boolean normalFloatCheck = this.isNormalFloatFormatInstanceData();
        final long byteRefreshIndex = refreshIndex * 4L;
        final long byte4RefreshIndex = byteRefreshIndex * 4L;

        if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0) {
            final byte baseBufferSize = (byte) (instance3DCheck ? 5 : 3);
            IntBuffer tboObject = BufferUtils.createIntBuffer(baseBufferSize);
            IntBuffer tboTex = BufferUtils.createIntBuffer(baseBufferSize);
            GL15.glGenBuffers(tboObject);
            GL11.glGenTextures(tboTex);
            this._instanceDataTBO[_FINAL_MATRIX][0] = tboObject.get(0);
            this._instanceDataTBOTex[_FINAL_MATRIX][0] = tboTex.get(0);
            this._instanceDataTBO[_FINAL_MATRIX][1] = tboObject.get(1);
            this._instanceDataTBOTex[_FINAL_MATRIX][1] = tboTex.get(1);
            this._instanceDataTBO[_COLOR][0] = tboObject.get(2);
            this._instanceDataTBOTex[_COLOR][0] = tboTex.get(2);
            if (instance3DCheck) {
                this._instanceDataTBO[_FINAL_MATRIX][2] = tboObject.get(4);
                this._instanceDataTBOTex[_FINAL_MATRIX][2] = tboTex.get(4);
                this._instanceDataTBO[_STATE][0] = tboObject.get(5);
                this._instanceDataTBOTex[_STATE][0] = tboTex.get(5);
            }
            if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0 || this._instanceDataTBOTex[_FINAL_MATRIX][0] == 0) return BoxEnum.STATE_FAILED_OTHER;
        }

        byte[][] colorTmp = new byte[4][4];
        // 4 channels; for per pixel: r, g, b, a; for per channel: lowColor=0xFF000000, highColor=0x00FF0000, lowEmissive=0x0000FF00, highEmissive=0x000000FF
        IntBuffer colorBuffer = BufferUtils.createIntBuffer(pixelSize);
        if (newTex) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_COLOR][0]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, bit32Size, GL15.GL_STREAM_DRAW);
            if (instance3DCheck) {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][2]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][2]);
            }
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][1]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][1]);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        }

        if (BoxConfigs.isJVMParallel()) {
            final byte[] chose = instance3DCheck ? new byte[]{3, 4} : new byte[]{2, 2};
            if (this._instanceDataJVM[_FINAL_MATRIX] == null) {
                this._instanceDataJVM[_FINAL_MATRIX] = new FloatBuffer[chose[0]];
                this._instanceDataJVM[_TIMER] = new FloatBuffer[1];
                this._instanceDataJVM[_STATE] = new FloatBuffer[chose[1]];
            }
            final int refreshIndexJVM = refreshIndex * 4;
            if (newTex) {
                this._instanceDataJVM[_FINAL_MATRIX][0] = BufferUtils.createFloatBuffer(pixelSize);
                this._instanceDataJVM[_FINAL_MATRIX][1] = BufferUtils.createFloatBuffer(pixelSize);
                this._instanceDataJVM[_TIMER][0] = BufferUtils.createFloatBuffer(pixelSize);
                this._instanceDataJVM[_STATE][0] = BufferUtils.createFloatBuffer(pixelSize);
                this._instanceDataJVM[_STATE][1] = BufferUtils.createFloatBuffer(pixelSize);
                if (instance3DCheck) {
                    this._instanceDataJVM[_FINAL_MATRIX][2] = BufferUtils.createFloatBuffer(pixelSize);
                    this._instanceDataJVM[_STATE][2] = BufferUtils.createFloatBuffer(pixelSize);
                    this._instanceDataJVM[_STATE][3] = BufferUtils.createFloatBuffer(pixelSize);
                }
            }
            this._instanceDataJVM[_FINAL_MATRIX][0].position(refreshIndexJVM);
            this._instanceDataJVM[_TIMER][0].position(refreshIndexJVM);
            this._instanceDataJVM[_STATE][0].position(refreshIndexJVM);
            this._instanceDataJVM[_STATE][1].position(refreshIndexJVM);
            if (instance3DCheck) {
                this._instanceDataJVM[_STATE][2].position(refreshIndexJVM);
                this._instanceDataJVM[_STATE][3].position(refreshIndexJVM);
            }
            float[] finalTmp, timerTmp;
            float[][] stateTmp;
            finalTmp = new float[4];
            timerTmp = new float[4];
            stateTmp = new float[4][4];
            for (int i = refreshIndex; i < refreshLimit; i++) {
                InstanceDataAPI data = this.instanceData.get(i);
                if (data == null) {
                    timerTmp[0] = 0.0f;
                } else {
                    finalTmp = data.pickFinal_vec4();
                    timerTmp = data.pickTimer_vec4();
                    stateTmp = data.pickState_vec4();
                    colorTmp = data.pickColor_vec4x4();
                }
                this._instanceDataJVM[_FINAL_MATRIX][0].put(finalTmp);
                this._instanceDataJVM[_TIMER][0].put(timerTmp);
                this._instanceDataJVM[_STATE][0].put(stateTmp[0]);
                this._instanceDataJVM[_STATE][1].put(stateTmp[1]);
                if (instance3DCheck) {
                    this._instanceDataJVM[_STATE][2].put(stateTmp[2]);
                    this._instanceDataJVM[_STATE][3].put(stateTmp[3]);
                }
                CommonUtil.putPackingBytes(colorBuffer, colorTmp[0], colorTmp[1], colorTmp[2], colorTmp[3]);
            }
            this._instanceDataJVM[_FINAL_MATRIX][0].position(0);
            this._instanceDataJVM[_FINAL_MATRIX][0].limit(this._instanceDataJVM[_FINAL_MATRIX][0].capacity());
            this._instanceDataJVM[_FINAL_MATRIX][1].position(0);
            this._instanceDataJVM[_FINAL_MATRIX][1].limit(this._instanceDataJVM[_FINAL_MATRIX][1].capacity());
            this._instanceDataJVM[_TIMER][0].position(0);
            this._instanceDataJVM[_TIMER][0].limit(this._instanceDataJVM[_TIMER][0].capacity());
            this._instanceDataJVM[_STATE][0].position(0);
            this._instanceDataJVM[_STATE][0].limit(this._instanceDataJVM[_STATE][0].capacity());
            this._instanceDataJVM[_STATE][1].position(0);
            this._instanceDataJVM[_STATE][1].limit(this._instanceDataJVM[_STATE][1].capacity());
            if (instance3DCheck) {
                this._instanceDataJVM[_FINAL_MATRIX][2].position(0);
                this._instanceDataJVM[_FINAL_MATRIX][2].limit(this._instanceDataJVM[_FINAL_MATRIX][2].capacity());
                this._instanceDataJVM[_STATE][2].position(0);
                this._instanceDataJVM[_STATE][2].limit(this._instanceDataJVM[_STATE][2].capacity());
                this._instanceDataJVM[_STATE][3].position(0);
                this._instanceDataJVM[_STATE][3].limit(this._instanceDataJVM[_STATE][3].capacity());
            }
        } else if (BoxConfigs.isGLParallel()) {
            final byte[] chose = instance3DCheck ? new byte[]{5, 4} : new byte[]{3, 3};
            final Buffer[] buffersF = new Buffer[chose[0]];
            final FloatBuffer timerBuffer = BufferUtils.createFloatBuffer(pixelSize);
            if (normalFloatCheck) for (byte i = 0; i < chose[0]; i++) buffersF[i] = BufferUtils.createFloatBuffer(pixelSize);
            else for (byte i = 0; i < chose[0]; i++) buffersF[i] = BufferUtils.createShortBuffer(pixelSize);

            float[] finalTmp, timerTmp;
            float[][] stateTmp;
            finalTmp = new float[4];
            timerTmp = new float[4];
            stateTmp = new float[4][4];
            for (int i = refreshIndex; i < refreshLimit; i++) {
                InstanceDataAPI data = this.instanceData.get(i);
                if (data == null) {
                    timerTmp[0] = 0.0f;
                } else {
                    finalTmp = data.pickFinal_vec4();
                    timerTmp = data.pickTimer_vec4();
                    stateTmp = data.pickState_vec4();
                    colorTmp = data.pickColor_vec4x4();
                }
                timerBuffer.put(timerTmp);
                if (normalFloatCheck) {
                    ((FloatBuffer) buffersF[0]).put(finalTmp);
                    ((FloatBuffer) buffersF[1]).put(stateTmp[0]);
                    ((FloatBuffer) buffersF[2]).put(stateTmp[1]);
                    if (instance3DCheck) {
                        ((FloatBuffer) buffersF[3]).put(stateTmp[2]);
                        ((FloatBuffer) buffersF[4]).put(stateTmp[3]);
                    }
                } else {
                    CommonUtil.putFloat16((ShortBuffer) buffersF[0], finalTmp);
                    CommonUtil.putFloat16((ShortBuffer) buffersF[1], stateTmp[0]);
                    CommonUtil.putFloat16((ShortBuffer) buffersF[2], stateTmp[1]);
                    if (instance3DCheck) {
                        CommonUtil.putFloat16((ShortBuffer) buffersF[3], stateTmp[2]);
                        CommonUtil.putFloat16((ShortBuffer) buffersF[4], stateTmp[3]);
                    }
                }
                CommonUtil.putPackingBytes(colorBuffer, colorTmp[0], colorTmp[1], colorTmp[2], colorTmp[3]);
            }
            for (Buffer buffer : buffersF) {
                buffer.position(0);
                buffer.limit(buffer.capacity());
            }
            timerBuffer.position(0);
            timerBuffer.limit(timerBuffer.capacity());
            if (this._instanceDataTBO[_TIMER][0] == 0) {
                IntBuffer tboObject = BufferUtils.createIntBuffer(chose[1]);
                IntBuffer tboTex = BufferUtils.createIntBuffer(chose[1]);
                GL15.glGenBuffers(tboObject);
                GL11.glGenTextures(tboTex);
                this._instanceDataTBO[_TIMER][0] = tboObject.get(0);
                this._instanceDataTBOTex[_TIMER][0] = tboTex.get(0);
                this._instanceDataTBO[_STATE][1] = tboObject.get(1);
                this._instanceDataTBOTex[_STATE][1] = tboTex.get(1);
                if (instance3DCheck) {
                    this._instanceDataTBO[_STATE][2] = tboObject.get(2);
                    this._instanceDataTBOTex[_STATE][2] = tboTex.get(2);
                    this._instanceDataTBO[_STATE][3] = tboObject.get(3);
                    this._instanceDataTBOTex[_STATE][3] = tboTex.get(3);
                } else {
                    this._instanceDataTBO[_STATE][0] = tboObject.get(2);
                    this._instanceDataTBOTex[_STATE][0] = tboTex.get(2);
                }
                if (this._instanceDataTBO[_TIMER][0] == 0 || this._instanceDataTBOTex[_TIMER][0] == 0) return BoxEnum.STATE_FAILED_OTHER;
            }
            if (newTex) {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_TIMER][0]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, bit32Size, GL15.GL_STREAM_DRAW);
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][1]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                if (instance3DCheck) {
                    GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][2]);
                    GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                    GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][3]);
                    GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                } else {
                    GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
                    GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                }
            }
            final long tboRefreshIndex = byteRefreshIndex * this.getInstanceDataFormat();
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
            if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (FloatBuffer) buffersF[0]);
            else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (ShortBuffer) buffersF[0]);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][0]);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_TIMER][0]);
            GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, byte4RefreshIndex, timerBuffer);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_TIMER][0]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_TIMER], this._instanceDataTBO[_TIMER][0]);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
            if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (FloatBuffer) buffersF[1]);
            else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (ShortBuffer) buffersF[1]);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][0]);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][1]);
            if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (FloatBuffer) buffersF[2]);
            else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (ShortBuffer) buffersF[2]);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][1]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][1]);
            if (instance3DCheck) {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][2]);
                if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (FloatBuffer) buffersF[3]);
                else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (ShortBuffer) buffersF[3]);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][2]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][2]);
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][3]);
                if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (FloatBuffer) buffersF[4]);
                else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (ShortBuffer) buffersF[4]);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][3]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][3]);
            }
        }/* else if (BoxConfigs.isCLParallel()) {
            final byte chose = (byte) (instance3DCheck ? 3 : 2);
            if (this._instanceDataMemory[0] == null) {
                this._instanceDataMemory[_FINAL_MATRIX] = new CLMem[6];
                this._instanceDataMemory[_TIMER] = new CLMem[1];
                this._instanceDataMemory[_STATE] = new CLMem[3];
                this._instanceDataMemory[_FINAL_MATRIX][0] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0], 0, false, null);
                this._instanceDataMemory[_FINAL_MATRIX][3] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0], 0, true, null);
                this._instanceDataMemory[_FINAL_MATRIX][1] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1], 0, false, null);
                this._instanceDataMemory[_FINAL_MATRIX][4] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1], 0, true, null);
                if (instance3DCheck) {
                    this._instanceDataMemory[_STATE][0] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0], 0, false, null);
                    this._instanceDataMemory[_STATE][1] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0], 0, true, null);
                    this._instanceDataMemory[_FINAL_MATRIX][2] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2], 0, false, null);
                    this._instanceDataMemory[_FINAL_MATRIX][5] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2], 0, true, null);
                }
                if (this._instanceDataMemory[_FINAL_MATRIX][0] == null) return BoxEnum.STATE_FAILED_OTHER;
            }
            if (newTex) {
                if (instance3DCheck) {
                    GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
                    GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, f16Size, GL15.GL_STREAM_DRAW);
                }

                this._instanceDataMemory[_TIMER][0] = KernelUtil.ioBuffer(CL10.CL_MEM_READ_WRITE, bit32Size, null);
                this._instanceDataMemory[_STATE][2] = KernelUtil.ioBuffer(CL10.CL_MEM_READ_WRITE, bit32Size * chose, null);
                this._instanceDataMemory[3][0] = KernelUtil.ioBuffer(CL10.CL_MEM_READ_WRITE, 8, null);
            }
            final int[] clMemSize = new int[]{pixelSize, pixelSize * chose};
            final FloatBuffer timerBuffer = BufferUtils.createFloatBuffer(clMemSize[0]);
            final FloatBuffer stateBuffer = BufferUtils.createFloatBuffer(clMemSize[1]);
            final ShortBuffer finalBuffer = BufferUtils.createShortBuffer(pixelSize);
            ShortBuffer glStateBuffer = null;
            if (instance3DCheck) glStateBuffer = BufferUtils.createShortBuffer(pixelSize);
            float[] finalTmp, timerTmp;
            float[][] stateTmp;
            finalTmp = new float[4];
            timerTmp = new float[4];
            stateTmp = new float[4][4];
            for (int i = refreshIndex; i < refreshLimit; i++) {
                InstanceDataAPI data = this.instanceData.get(i);
                if (data == null) {
                    timerTmp[0] = 0.0f;
                } else {
                    finalTmp = data.pickFinal_16F();
                    timerTmp = data.pickTimer_32F();
                    stateTmp = data.pickState_16F();
                    colorTmp = data.pickColor_8F();
                }
                CommonUtil.putFloat16(finalBuffer, finalTmp);
                timerBuffer.put(timerTmp);
                if (instance3DCheck) {
                    stateBuffer.put(stateTmp[1]);
                    stateBuffer.put(stateTmp[2]);
                    stateBuffer.put(stateTmp[3]);
                    CommonUtil.putFloat16(glStateBuffer, stateTmp[0]);
                } else {
                    stateBuffer.put(stateTmp[0]);
                    stateBuffer.put(stateTmp[1]);
                }
                colorBuffer.put(colorTmp[0]);
                emissiveBuffer.put(colorTmp[1]);
            }
            finalBuffer.flip();
            timerBuffer.flip();
            stateBuffer.flip();
            final int tboRefreshIndex = refreshIndex * 4;
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
            GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, finalBuffer);
            if (instance3DCheck) {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
                GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, glStateBuffer);
            }
            long startIndex = refreshIndex * 4L;
            CL10.clEnqueueWriteBuffer(KernelCore.getDefaultCLQueue(), this._instanceDataMemory[_TIMER][0], CL10.CL_TRUE, startIndex, timerBuffer, null, null);
            CL10.clEnqueueWriteBuffer(KernelCore.getDefaultCLQueue(), this._instanceDataMemory[_STATE][1], CL10.CL_TRUE, startIndex * chose, stateBuffer, null, null);
        }*/
        colorBuffer.position(0);
        colorBuffer.limit(colorBuffer.capacity());
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_COLOR][0]);
        GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, byte4RefreshIndex, colorBuffer);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_COLOR][0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32UI, this._instanceDataTBO[_COLOR][0]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        if (newTex) this.instanceRefreshState[2] = size;
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * Optional.<p>
     * Just <strong>malloc()</strong> without any submit call.
     *
     * @param dataNum must be positive integer.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when parameter error.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte mallocInstanceData(int dataNum) {
        if (dataNum < 1) return BoxEnum.STATE_FAILED;
        if (this.isCalledFixedSubmit()) return BoxEnum.STATE_FAILED_OTHER;
        final int pixelSize = Math.min(dataNum, BoxConfigs.getMaxInstanceDataSize()) * 4;
        final long dynamicSize = pixelSize * (long) this.getInstanceDataFormat();
        final long byte4Size = pixelSize * 4L;
        final boolean instance3DCheck = this.isInstanceData3D();

        if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0) {
            final int baseBufferSize = instance3DCheck ? 5 : 3;
            IntBuffer tboObject = BufferUtils.createIntBuffer(baseBufferSize);
            IntBuffer tboTex = BufferUtils.createIntBuffer(baseBufferSize);
            GL15.glGenBuffers(tboObject);
            GL11.glGenTextures(tboTex);
            this._instanceDataTBO[_FINAL_MATRIX][0] = tboObject.get(0);
            this._instanceDataTBOTex[_FINAL_MATRIX][0] = tboTex.get(0);
            this._instanceDataTBO[_FINAL_MATRIX][1] = tboObject.get(1);
            this._instanceDataTBOTex[_FINAL_MATRIX][1] = tboTex.get(1);
            this._instanceDataTBO[_COLOR][0] = tboObject.get(2);
            this._instanceDataTBOTex[_COLOR][0] = tboTex.get(2);
            if (instance3DCheck) {
                this._instanceDataTBO[_FINAL_MATRIX][2] = tboObject.get(4);
                this._instanceDataTBOTex[_FINAL_MATRIX][2] = tboTex.get(4);
                this._instanceDataTBO[_STATE][0] = tboObject.get(5);
                this._instanceDataTBOTex[_STATE][0] = tboTex.get(5);
            }
            if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0 || this._instanceDataTBOTex[_FINAL_MATRIX][0] == 0) return BoxEnum.STATE_FAILED_OTHER;
        }

        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][0]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_COLOR][0]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, byte4Size, GL15.GL_STREAM_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_COLOR][0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32UI, this._instanceDataTBO[_COLOR][0]);
        if (instance3DCheck) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][0]);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][2]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][2]);
        }
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][1]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][1]);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);

        if (BoxConfigs.isJVMParallel()) {
            final byte[] chose = instance3DCheck ? new byte[]{3, 4} : new byte[]{2, 2};
            if (this._instanceDataJVM[_FINAL_MATRIX] == null) {
                this._instanceDataJVM[_FINAL_MATRIX] = new FloatBuffer[chose[0]];
                this._instanceDataJVM[_TIMER] = new FloatBuffer[1];
                this._instanceDataJVM[_STATE] = new FloatBuffer[chose[1]];
            }
            this._instanceDataJVM[_FINAL_MATRIX][0] = BufferUtils.createFloatBuffer(pixelSize);
            this._instanceDataJVM[_FINAL_MATRIX][1] = BufferUtils.createFloatBuffer(pixelSize);
            this._instanceDataJVM[_TIMER][0] = BufferUtils.createFloatBuffer(pixelSize);
            this._instanceDataJVM[_STATE][0] = BufferUtils.createFloatBuffer(pixelSize);
            this._instanceDataJVM[_STATE][1] = BufferUtils.createFloatBuffer(pixelSize);
            if (instance3DCheck) {
                this._instanceDataJVM[_FINAL_MATRIX][2] = BufferUtils.createFloatBuffer(pixelSize);
                this._instanceDataJVM[_STATE][2] = BufferUtils.createFloatBuffer(pixelSize);
                this._instanceDataJVM[_STATE][3] = BufferUtils.createFloatBuffer(pixelSize);
            }
        } else if (BoxConfigs.isGLParallel()) {
            final byte chose = (byte) (instance3DCheck ? 4 : 3);
            if (this._instanceDataTBO[_TIMER][0] == 0) {
                IntBuffer tboObject = BufferUtils.createIntBuffer(chose);
                IntBuffer tboTex = BufferUtils.createIntBuffer(chose);
                GL15.glGenBuffers(tboObject);
                GL11.glGenTextures(tboTex);
                this._instanceDataTBO[_TIMER][0] = tboObject.get(0);
                this._instanceDataTBOTex[_TIMER][0] = tboTex.get(0);
                this._instanceDataTBO[_STATE][1] = tboObject.get(1);
                this._instanceDataTBOTex[_STATE][1] = tboTex.get(1);
                if (instance3DCheck) {
                    this._instanceDataTBO[_STATE][2] = tboObject.get(2);
                    this._instanceDataTBOTex[_STATE][2] = tboTex.get(2);
                    this._instanceDataTBO[_STATE][3] = tboObject.get(3);
                    this._instanceDataTBOTex[_STATE][3] = tboTex.get(3);
                } else {
                    this._instanceDataTBO[_STATE][0] = tboObject.get(2);
                    this._instanceDataTBOTex[_STATE][0] = tboTex.get(2);
                }
                if (this._instanceDataTBO[_TIMER][0] == 0 || this._instanceDataTBOTex[_TIMER][0] == 0) return BoxEnum.STATE_FAILED_OTHER;
            }

            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_TIMER][0]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, byte4Size, GL15.GL_STREAM_DRAW);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_TIMER][0]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_TIMER], this._instanceDataTBO[_TIMER][0]);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][1]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][1]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][1]);
            if (instance3DCheck) {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][2]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][2]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][2]);
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][3]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][3]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][3]);
            } else {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][0]);
            }
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        }/* else if (BoxConfigs.isCLParallel()) {
            final byte chose = (byte) (instance3DCheck ? 3 : 2);
            if (this._instanceDataMemory[0] == null) {
                this._instanceDataMemory[_FINAL_MATRIX] = new CLMem[6];
                this._instanceDataMemory[_TIMER] = new CLMem[1];
                this._instanceDataMemory[_STATE] = new CLMem[3];
                this._instanceDataMemory[_FINAL_MATRIX][0] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0], 0, false, null);
                this._instanceDataMemory[_FINAL_MATRIX][3] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0], 0, true, null);
                this._instanceDataMemory[_FINAL_MATRIX][1] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1], 0, false, null);
                this._instanceDataMemory[_FINAL_MATRIX][4] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1], 0, true, null);
                if (instance3DCheck) {
                    this._instanceDataMemory[_STATE][0] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0], 0, false, null);
                    this._instanceDataMemory[_STATE][1] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0], 0, true, null);
                    this._instanceDataMemory[_FINAL_MATRIX][2] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2], 0, false, null);
                    this._instanceDataMemory[_FINAL_MATRIX][5] = KernelUtil.shareTextureAny(CL12GL.CL_GL_OBJECT_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2], 0, true, null);
                }
                if (this._instanceDataMemory[_FINAL_MATRIX][0] == null) return BoxEnum.STATE_FAILED_OTHER;
            }

            if (instance3DCheck) {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, f16Size, GL15.GL_STREAM_DRAW);
            }

            this._instanceDataMemory[_TIMER][0] = KernelUtil.ioBuffer(CL10.CL_MEM_READ_WRITE, byte4Size, null);
            this._instanceDataMemory[_STATE][2] = KernelUtil.ioBuffer(CL10.CL_MEM_READ_WRITE, byte4Size * chose, null);
            this._instanceDataMemory[3][0] = KernelUtil.ioBuffer(CL10.CL_MEM_READ_WRITE, 8, null);
        }*/
        this.instanceRefreshState[2] = dataNum;
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * For instance data if it is closely related to CPU data or calculating.<p>
     * Use it when you changed the instance data.<p>
     * Put all data, and it is unneeded to calculate, so <strong>donot</strong> call any refresh method, just submit it and set rendering count.<p>
     * <p>
     * <strong>Should have reserved size for instance-list, if adds more data later. If not, will clear previous data of all in texture objects when submit.</strong><p>
     * If list size has increased, will force to submit for all data.<p>
     * For submit, here will use {@link GL15#glBufferSubData(int, long, ByteBuffer)} for update TBOs(why SSBO in 4.3) in once ergodic, else {@link GL15#glMapBuffer(int, int, ByteBuffer)} is unable to meet demand.<p>
     * <p>
     * In half-float mode, Cost <strong>32Byte</strong> of vRAM each 2D-instance, when running on GL mode, entirely GPU calculate; In other words, rendering <strong>32768</strong> instances will cost <strong>1.0MB</strong> of vRAM.<p>
     * For 3D-instance cost <strong>48Byte</strong> and <strong>32768</strong> instances cost <strong>1.5MB</strong> of vRAM.<p>
     * Cost double vRAM in normal-float mode for each instance data.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success, return {@link BoxEnum#STATE_FAILED} when over limit, return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte submitFixedInstanceData() {
        if (this.instanceData == null || this.instanceData.isEmpty()) return BoxEnum.STATE_FAILED;
        if (!BoxConfigs.isTBOSupported()) return BoxEnum.STATE_FAILED_OTHER;
        final int size = this.instanceData.size();
        final boolean newTex = size > this.instanceRefreshState[2];
        final int refreshIndex = newTex ? 0 : this.instanceRefreshState[0];
        final int refreshCount = newTex ? size - refreshIndex : this.instanceRefreshState[1];
        if (refreshCount < 1) return BoxEnum.STATE_FAILED;
        final int refreshLimit = refreshIndex + refreshCount;
        final int pixelSizeHalf = refreshCount + refreshCount;
        final int pixelSize = pixelSizeHalf + pixelSizeHalf;
        final long dynamicSizeHalf = pixelSizeHalf * (long) this.getInstanceDataFormat();
        final long dynamicSize = dynamicSizeHalf + dynamicSizeHalf;
        final long bit16Size = pixelSize * 2L;
        final boolean instance3DCheck = this.isInstanceData3D();
        final boolean normalFloatCheck = this.isNormalFloatFormatInstanceData();
        final long byteRefreshHalfIndex = refreshIndex * 2L;
        final long tboHalfRefreshIndex = byteRefreshHalfIndex * this.getInstanceDataFormat();
        final long tboRefreshIndex = tboHalfRefreshIndex + tboHalfRefreshIndex;
        final long colorRefreshIndex = byteRefreshHalfIndex * 4L;
        this.needRefreshInstanceData[2] = true;

        if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0) {
            final byte baseBufferSize = (byte) (instance3DCheck ? 4 : 3);
            IntBuffer tboObject = BufferUtils.createIntBuffer(baseBufferSize);
            IntBuffer tboTex = BufferUtils.createIntBuffer(baseBufferSize);
            GL15.glGenBuffers(tboObject);
            GL11.glGenTextures(tboTex);
            this._instanceDataTBO[_FINAL_MATRIX][0] = tboObject.get(0);
            this._instanceDataTBOTex[_FINAL_MATRIX][0] = tboTex.get(0);
            this._instanceDataTBO[_FINAL_MATRIX][1] = tboObject.get(1);
            this._instanceDataTBOTex[_FINAL_MATRIX][1] = tboTex.get(1);
            this._instanceDataTBO[_COLOR][0] = tboObject.get(2);
            this._instanceDataTBOTex[_COLOR][0] = tboTex.get(2);
            if (instance3DCheck) {
                this._instanceDataTBO[_FINAL_MATRIX][2] = tboObject.get(4);
                this._instanceDataTBOTex[_FINAL_MATRIX][2] = tboTex.get(4);
            }
            if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0 || this._instanceDataTBOTex[_FINAL_MATRIX][0] == 0) return BoxEnum.STATE_FAILED_OTHER;
        }

        ShortBuffer colorBuffer = BufferUtils.createShortBuffer(pixelSize);
        if (newTex) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][1]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, instance3DCheck ? dynamicSize : dynamicSizeHalf, GL15.GL_STREAM_DRAW);
            if (instance3DCheck) {
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][2]);
                GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSizeHalf, GL15.GL_STREAM_DRAW);
            }
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_COLOR][0]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, bit16Size, GL15.GL_STREAM_DRAW);
        }

        final Buffer[] buffersF = new Buffer[instance3DCheck ? 2 : 1];
        final Buffer buffersRGF = normalFloatCheck ? BufferUtils.createFloatBuffer(pixelSizeHalf) : BufferUtils.createShortBuffer(pixelSizeHalf);
        byte[][] colorTmp;
        float[][] finalTmp;
        if (normalFloatCheck) for (byte i = 0; i < buffersF.length; i++) buffersF[i] = BufferUtils.createFloatBuffer(pixelSize);
        else for (byte i = 0; i < buffersF.length; i++) buffersF[i] = BufferUtils.createShortBuffer(pixelSize);
        for (int i = refreshIndex; i < refreshLimit; i++) {
            InstanceDataAPI data = this.instanceData.get(i);
            if (data == null) continue;
            finalTmp = data.pickFixedFinal_vec4();
            colorTmp = data.pickFixedColor_vec4x2();
            if (normalFloatCheck) {
                ((FloatBuffer) buffersF[0]).put(finalTmp[0]);
                if (instance3DCheck) {
                    ((FloatBuffer) buffersF[1]).put(finalTmp[1]);
                    ((FloatBuffer) buffersRGF).put(finalTmp[2]);
                } else ((FloatBuffer) buffersRGF).put(finalTmp[1]);
            } else {
                CommonUtil.putFloat16((ShortBuffer) buffersF[0], finalTmp[0]);
                if (instance3DCheck) {
                    CommonUtil.putFloat16((ShortBuffer) buffersF[1], finalTmp[1]);
                    CommonUtil.putFloat16((ShortBuffer) buffersRGF, finalTmp[2]);
                } else CommonUtil.putFloat16((ShortBuffer) buffersRGF, finalTmp[1]);
            }
            CommonUtil.putPackingBytes(colorBuffer, colorTmp[0], colorTmp[1]);
        }
        for (Buffer buffer : buffersF) {
            buffer.position(0);
            buffer.limit(buffer.capacity());
        }
        buffersRGF.position(0);
        buffersRGF.limit(buffersRGF.capacity());
        colorBuffer.position(0);
        colorBuffer.limit(colorBuffer.capacity());

        final byte f1FormatPicker = instance3DCheck ? _FINAL_MATRIX : _FIXED_FINAL_MATRIX_RG;
        final long f1IPicker = instance3DCheck ? tboRefreshIndex : tboHalfRefreshIndex;
        final Buffer f1BufPicker = instance3DCheck ? buffersF[1] : buffersRGF;
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
        if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (FloatBuffer) buffersF[0]);
        else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboRefreshIndex, (ShortBuffer) buffersF[0]);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][0]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][1]);
        if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, f1IPicker, (FloatBuffer) f1BufPicker);
        else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, f1IPicker, (ShortBuffer) f1BufPicker);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[f1FormatPicker], this._instanceDataTBO[_FINAL_MATRIX][1]);
        if (instance3DCheck) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][2]);
            if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboHalfRefreshIndex, (FloatBuffer) buffersRGF);
            else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, tboHalfRefreshIndex, (ShortBuffer) buffersRGF);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FIXED_FINAL_MATRIX_RG], this._instanceDataTBO[_FINAL_MATRIX][2]);
        }
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_COLOR][0]);
        GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, colorRefreshIndex, colorBuffer);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_COLOR][0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA16UI, this._instanceDataTBO[_COLOR][0]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        if (newTex) this.instanceRefreshState[2] = size;
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * Optional.<p>
     * Just <strong>malloc()</strong> without any submit call.
     *
     * @param dataNum must be positive integer.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when parameter error.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte mallocFixedInstanceData(int dataNum) {
        if (dataNum < 1) return BoxEnum.STATE_FAILED;
        final int pixelSizeHalf = Math.min(dataNum, BoxConfigs.getMaxInstanceDataSize()) * 2;
        final long dynamicSizeHalf = pixelSizeHalf * (long) this.getInstanceDataFormat();
        final long dynamicSize = dynamicSizeHalf + dynamicSizeHalf;
        final long byte2Size = pixelSizeHalf * 4L;
        final boolean instance3DCheck = this.isInstanceData3D();
        this.needRefreshInstanceData[2] = true;

        if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0) {
            final byte baseBufferSize = (byte) (instance3DCheck ? 4 : 3);
            IntBuffer tboObject = BufferUtils.createIntBuffer(baseBufferSize);
            IntBuffer tboTex = BufferUtils.createIntBuffer(baseBufferSize);
            GL15.glGenBuffers(tboObject);
            GL11.glGenTextures(tboTex);
            this._instanceDataTBO[_FINAL_MATRIX][0] = tboObject.get(0);
            this._instanceDataTBOTex[_FINAL_MATRIX][0] = tboTex.get(0);
            this._instanceDataTBO[_FINAL_MATRIX][1] = tboObject.get(1);
            this._instanceDataTBOTex[_FINAL_MATRIX][1] = tboTex.get(1);
            this._instanceDataTBO[_COLOR][0] = tboObject.get(2);
            this._instanceDataTBOTex[_COLOR][0] = tboTex.get(2);
            if (instance3DCheck) {
                this._instanceDataTBO[_FINAL_MATRIX][2] = tboObject.get(4);
                this._instanceDataTBOTex[_FINAL_MATRIX][2] = tboTex.get(4);
            }
            if (this._instanceDataTBO[_FINAL_MATRIX][0] == 0 || this._instanceDataTBOTex[_FINAL_MATRIX][0] == 0) return BoxEnum.STATE_FAILED_OTHER;
        }

        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSize, GL15.GL_STREAM_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][0]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][1]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, instance3DCheck ? dynamicSize : dynamicSizeHalf, GL15.GL_STREAM_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[instance3DCheck ? _FINAL_MATRIX : _FIXED_FINAL_MATRIX_RG], this._instanceDataTBO[_FINAL_MATRIX][1]);
        if (instance3DCheck) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][2]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, dynamicSizeHalf, GL15.GL_STREAM_DRAW);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FIXED_FINAL_MATRIX_RG], this._instanceDataTBO[_FINAL_MATRIX][2]);
        }
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_COLOR][0]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, byte2Size, GL15.GL_STREAM_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_COLOR][0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA16UI, this._instanceDataTBO[_COLOR][0]);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);

        this.instanceRefreshState[2] = dataNum;
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * For generally, automatic running in rendering manager, not required calling it.
     */
    public void sysRefreshInstanceData(float amount, boolean isPaused) {
        if (isPaused && !this.isTimingWhenPaused() || !BoxConfigs.isTBOSupported() || this.isCalledFixedSubmit()) return;
        final boolean instance3DCheck = isInstanceData3D();
        final boolean normalFloatCheck = this.isNormalFloatFormatInstanceData();
        final int currValidSize = Math.min(this.getValidInstanceDataCount(), this.getRenderingCount());
        if (currValidSize < 1) return;
        if (BoxConfigs.isJVMParallel()) { // always JVM mode for default.
            this._instanceDataJVM[_FINAL_MATRIX][0].position(0);
            this._instanceDataJVM[_FINAL_MATRIX][0].limit(this._instanceDataJVM[_FINAL_MATRIX][0].capacity());
            this._instanceDataJVM[_FINAL_MATRIX][1].position(0);
            this._instanceDataJVM[_FINAL_MATRIX][1].limit(this._instanceDataJVM[_FINAL_MATRIX][1].capacity());
            this._instanceDataJVM[_TIMER][0].position(0);
            this._instanceDataJVM[_TIMER][0].limit(this._instanceDataJVM[_TIMER][0].capacity());
            this._instanceDataJVM[_STATE][0].position(0);
            this._instanceDataJVM[_STATE][0].limit(this._instanceDataJVM[_STATE][0].capacity());
            this._instanceDataJVM[_STATE][1].position(0);
            this._instanceDataJVM[_STATE][1].limit(this._instanceDataJVM[_STATE][1].capacity());
            if (instance3DCheck) {
                this._instanceDataJVM[_FINAL_MATRIX][2].position(0);
                this._instanceDataJVM[_FINAL_MATRIX][2].limit(this._instanceDataJVM[_FINAL_MATRIX][2].capacity());
                this._instanceDataJVM[_STATE][2].position(0);
                this._instanceDataJVM[_STATE][2].limit(this._instanceDataJVM[_STATE][2].capacity());
                this._instanceDataJVM[_STATE][3].position(0);
                this._instanceDataJVM[_STATE][3].limit(this._instanceDataJVM[_STATE][3].capacity());
            }
            final int finalMatrixFactor = instance3DCheck ? 3 : 2;
            final int bufferSize = currValidSize * 4;
            final Buffer[] finalBuffers = new Buffer[instance3DCheck ? 4 : 2];
            float[][] finalMatrix = new float[finalMatrixFactor][4];
            float[] matrixQ = new float[instance3DCheck ? 9 : 2];
            float[] currentRotate, currentScale, dynamicLoc, dynamicRotate, dynamicScale;
            currentRotate = new float[3];
            currentScale = new float[3];
            dynamicLoc = new float[3];
            dynamicRotate = new float[3];
            dynamicScale = new float[3];
            if (normalFloatCheck) for (byte i = 0; i < finalBuffers.length; i++) finalBuffers[i] = BufferUtils.createFloatBuffer(bufferSize);
            else for (byte i = 0; i < finalBuffers.length; i++) finalBuffers[i] = BufferUtils.createShortBuffer(bufferSize);

            int indexX, indexY, indexZ, indexW;
            float total, alpha, rotateX, rotateY, rotateZ, sinX, sinY, sinZ, cosX, cosY, cosZ, wq, xq, yq, zq, dqx, dqy, dqz;
            float[] tmpTimer, tmpState;
            tmpTimer = new float[]{-512.0f, 0};
            tmpState = new float[4];
            boolean finishSet;
            for (int i = 0; i < currValidSize; i++) {
                indexX = i * 4;
                indexY = indexX + 1;
                indexZ = indexX + 2;
                indexW = indexX + 3;

                total = this._instanceDataJVM[_TIMER][0].get(indexX);
                if (total < -1000.0f) continue;

                alpha = 10.0f;
                finishSet = total < -500.0f;
                if (finishSet) {
                    tmpTimer[0] = tmpTimer[1] = -512.0f;
                } else {
                    tmpTimer[0] = this._instanceDataJVM[_TIMER][0].get(indexW);
                    tmpTimer[1] = -10.0f;
                }
                if (total > 2.0f) {
                    alpha = Math.abs(total - 3.0f);
                    tmpTimer[0] = this._instanceDataJVM[_TIMER][0].get(indexY);
                    tmpTimer[1] = 2.0f;
                } else if (total > 1.0f) {
                    tmpTimer[0] = this._instanceDataJVM[_TIMER][0].get(indexZ);
                    tmpTimer[1] = 1.0f;
                    alpha = 21.0f;
                } else if (total > 0.0f) {
                    alpha = total + 10.0f;
                }
                total = tmpTimer[0] > -500.0f ? (total - tmpTimer[0] * amount) : tmpTimer[1];
                if (total <= 0.0f && total > -500.0f) {
                    total = -512.0f;
                } else if (finishSet) total = -1024.0f;
                this._instanceDataJVM[_TIMER][0].put(indexX, total);

                finalMatrix[0][3] = this._instanceDataJVM[_FINAL_MATRIX][0].get(indexW);
                finalMatrix[0][2] = this._instanceDataJVM[_FINAL_MATRIX][0].get(indexZ);
                if (instance3DCheck) {
                    finalMatrix[0][1] = this._instanceDataJVM[_FINAL_MATRIX][0].get(indexY);
                    currentRotate[0] = this._instanceDataJVM[_STATE][0].get(indexX);
                    currentRotate[1] = this._instanceDataJVM[_STATE][0].get(indexY);
                    currentRotate[2] = this._instanceDataJVM[_STATE][0].get(indexZ);
                    currentScale[0] = this._instanceDataJVM[_STATE][1].get(indexX);
                    currentScale[1] = this._instanceDataJVM[_STATE][1].get(indexY);
                    currentScale[2] = this._instanceDataJVM[_STATE][1].get(indexZ);
                    dynamicLoc[0] = this._instanceDataJVM[_STATE][1].get(indexW) * amount;
                    dynamicLoc[1] = this._instanceDataJVM[_STATE][2].get(indexX) * amount;
                    dynamicLoc[2] = this._instanceDataJVM[_STATE][2].get(indexY) * amount;
                    dynamicRotate[0] = this._instanceDataJVM[_STATE][2].get(indexZ) * amount;
                    dynamicRotate[1] = this._instanceDataJVM[_STATE][2].get(indexW) * amount;
                    dynamicRotate[2] = this._instanceDataJVM[_STATE][3].get(indexX) * amount;
                    dynamicScale[0] = this._instanceDataJVM[_STATE][3].get(indexY) * amount;
                    dynamicScale[1] = this._instanceDataJVM[_STATE][3].get(indexZ) * amount;
                    dynamicScale[2] = this._instanceDataJVM[_STATE][3].get(indexW) * amount;
                } else {
                    currentRotate[2] = this._instanceDataJVM[_STATE][0].get(indexZ);
                    currentScale[0] = this._instanceDataJVM[_STATE][0].get(indexX);
                    currentScale[1] = this._instanceDataJVM[_STATE][0].get(indexY);
                    dynamicLoc[0] = this._instanceDataJVM[_STATE][1].get(indexX) * amount;
                    dynamicLoc[1] = this._instanceDataJVM[_STATE][1].get(indexY) * amount;
                    dynamicRotate[2] = this._instanceDataJVM[_STATE][0].get(indexW) * amount;
                    dynamicScale[0] = this._instanceDataJVM[_STATE][1].get(indexZ) * amount;
                    dynamicScale[1] = this._instanceDataJVM[_STATE][1].get(indexW) * amount;
                }

                rotateZ = currentRotate[2] * 0.5f;
                if (instance3DCheck) {
                    rotateX = currentRotate[0] * 0.5f;
                    rotateY = currentRotate[1] * 0.5f;
                    sinX = (float) Math.sin(Math.toRadians(rotateX));
                    cosX = TrigUtil.cosFormSinF(sinX, rotateX);
                    sinY = (float) Math.sin(Math.toRadians(rotateY));
                    cosY = TrigUtil.cosFormSinF(sinY, rotateY);
                    sinZ = (float) Math.sin(Math.toRadians(rotateZ));
                    cosZ = TrigUtil.cosFormSinF(sinZ, rotateZ);
                    wq = cosX * cosY * cosZ - sinX * sinY * sinZ;
                    xq = sinX * cosY * cosZ - cosX * sinY * sinZ;
                    yq = cosX * sinY * cosZ + sinX * cosY * sinZ;
                    zq = cosX * cosY * sinZ + sinX * sinY * cosZ;

                    dqx = xq + xq;
                    dqy = yq + yq;
                    dqz = zq + zq;
                    matrixQ[0] = dqx * xq;
                    matrixQ[1] = dqy * yq;
                    matrixQ[2] = dqz * zq;
                    matrixQ[3] = dqx * yq;
                    matrixQ[4] = dqx * zq;
                    matrixQ[5] = dqx * wq;
                    matrixQ[6] = dqy * zq;
                    matrixQ[7] = dqy * wq;
                    matrixQ[8] = dqz * wq;
                } else {
                    sinZ = (float) Math.sin(Math.toRadians(rotateZ));
                    dqz = rotateZ + rotateZ;
                    matrixQ[0] = dqz * sinZ;
                    matrixQ[1] = dqz * TrigUtil.cosFormSinF(sinZ, rotateZ);
                }

                finalMatrix[0][3] += dynamicLoc[0];
                finalMatrix[0][2] += dynamicLoc[1];
                this._instanceDataJVM[_FINAL_MATRIX][0].put(indexW, finalMatrix[0][3]);
                this._instanceDataJVM[_FINAL_MATRIX][0].put(indexZ, finalMatrix[0][2]);
                if (instance3DCheck) {
                    finalMatrix[0][1] += dynamicLoc[2];
                    this._instanceDataJVM[_FINAL_MATRIX][0].put(indexY, finalMatrix[0][1]);
                    tmpState[0] = currentRotate[0] + dynamicRotate[0];
                    tmpState[1] = currentRotate[1] + dynamicRotate[1];
                    tmpState[2] = currentRotate[2] + dynamicRotate[2];
                    tmpState[3] = alpha;
                    this._instanceDataJVM[_STATE][0].put(indexX, tmpState[0]);
                    this._instanceDataJVM[_STATE][0].put(indexY, tmpState[1]);
                    this._instanceDataJVM[_STATE][0].put(indexZ, tmpState[2]);
                    this._instanceDataJVM[_STATE][0].put(indexW, alpha);
                    this._instanceDataJVM[_STATE][1].put(indexX, currentScale[0] + dynamicScale[0]);
                    this._instanceDataJVM[_STATE][1].put(indexY, currentScale[1] + dynamicScale[1]);
                    this._instanceDataJVM[_STATE][1].put(indexZ, currentScale[2] + dynamicScale[2]);

                    finalMatrix[0][0] = currentScale[0] - (matrixQ[1] + matrixQ[2]) * currentScale[0];
                    finalMatrix[1][0] = (matrixQ[3] + matrixQ[8]) * currentScale[0];
                    finalMatrix[1][1] = currentScale[1] - (matrixQ[2] + matrixQ[0]) * currentScale[1];
                    finalMatrix[1][2] = (matrixQ[6] - matrixQ[5]) * currentScale[2];
                    finalMatrix[1][3] = (matrixQ[3] - matrixQ[8]) * currentScale[1];

                    finalMatrix[2][0] = (matrixQ[4] - matrixQ[7]) * currentScale[0];
                    finalMatrix[2][1] = (matrixQ[6] + matrixQ[5]) * currentScale[1];
                    finalMatrix[2][2] = currentScale[2] - (matrixQ[1] + matrixQ[0]) * currentScale[2];
                    finalMatrix[2][3] = (matrixQ[4] + matrixQ[7]) * currentScale[2];
                } else {
                    this._instanceDataJVM[_STATE][0].put(indexX, currentScale[0] + dynamicScale[0]);
                    this._instanceDataJVM[_STATE][0].put(indexY, currentScale[1] + dynamicScale[1]);
                    this._instanceDataJVM[_STATE][0].put(indexZ, currentRotate[2] + dynamicRotate[2]);

                    finalMatrix[0][0] = currentScale[0] - matrixQ[0] * currentScale[0];
                    finalMatrix[0][1] = -matrixQ[1] * currentScale[1];
                    finalMatrix[1][0] = matrixQ[1] * currentScale[0];
                    finalMatrix[1][1] = currentScale[1] - matrixQ[0] * currentScale[1];
                    finalMatrix[1][3] = alpha;
                }

                if (normalFloatCheck) {
                    ((FloatBuffer) finalBuffers[0]).put(finalMatrix[0]);
                    ((FloatBuffer) finalBuffers[1]).put(finalMatrix[1]);
                    if (instance3DCheck) {
                        ((FloatBuffer) finalBuffers[2]).put(finalMatrix[2]);
                        ((FloatBuffer) finalBuffers[3]).put(tmpState);
                    }
                } else {
                    CommonUtil.putFloat16((ShortBuffer) finalBuffers[0], finalMatrix[0]);
                    CommonUtil.putFloat16((ShortBuffer) finalBuffers[1], finalMatrix[1]);
                    if (instance3DCheck) {
                        CommonUtil.putFloat16((ShortBuffer) finalBuffers[2], finalMatrix[2]);
                        CommonUtil.putFloat16((ShortBuffer) finalBuffers[3], tmpState);
                    }
                }
            }
            finalBuffers[0].position(0);
            finalBuffers[0].limit(finalBuffers[0].capacity());
            finalBuffers[1].position(0);
            finalBuffers[1].limit(finalBuffers[1].capacity());
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][0]);
            if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (FloatBuffer) finalBuffers[0]);
            else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (ShortBuffer) finalBuffers[0]);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][0]);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][1]);
            if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (FloatBuffer) finalBuffers[1]);
            else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (ShortBuffer) finalBuffers[1]);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1]);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][1]);
            if (instance3DCheck) {
                finalBuffers[2].position(0);
                finalBuffers[2].limit(finalBuffers[2].capacity());
                finalBuffers[3].position(0);
                finalBuffers[3].limit(finalBuffers[3].capacity());
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_FINAL_MATRIX][2]);
                if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (FloatBuffer) finalBuffers[2]);
                else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (ShortBuffer) finalBuffers[2]);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_FINAL_MATRIX], this._instanceDataTBO[_FINAL_MATRIX][2]);
                GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBO[_STATE][0]);
                if (normalFloatCheck) GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (FloatBuffer) finalBuffers[3]);
                else GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, (ShortBuffer) finalBuffers[3]);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0]);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, this.instanceDataFormat[_STATE], this._instanceDataTBO[_STATE][0]);
            }
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        } else if (BoxConfigs.isGLParallel()) { // GL mode.
            BaseShaderData program = normalFloatCheck ? ShaderCore.getInstanceMatrixF32Program() : ShaderCore.getInstanceMatrixF16Program();
            final int itemDim = (int) Math.ceil(Math.sqrt(currValidSize / (BoxDatabase.isGLDeviceAMD() ? 64.0f : 32.0f)));
            program.putBindingImageTexture(0, this._instanceDataTBOTex[_FINAL_MATRIX][0], this.instanceDataFormat[_FINAL_MATRIX]);
            program.putBindingImageTexture(1, this._instanceDataTBOTex[_FINAL_MATRIX][1], this.instanceDataFormat[_FINAL_MATRIX]);
            program.putBindingImageTexture(3, this._instanceDataTBOTex[_STATE][0], this.instanceDataFormat[_STATE]);
            program.putBindingImageTexture(4, this._instanceDataTBOTex[_STATE][1], this.instanceDataFormat[_STATE]);
            program.putBindingImageTexture(7, this._instanceDataTBOTex[_TIMER][0], this.instanceDataFormat[_TIMER]);
            if (instance3DCheck) {
                program.putBindingImageTexture(2, this._instanceDataTBOTex[_FINAL_MATRIX][2], this.instanceDataFormat[_FINAL_MATRIX]);
                program.putBindingImageTextureReadOnly(5, this._instanceDataTBOTex[_STATE][2], this.instanceDataFormat[_STATE]);
                program.putBindingImageTextureReadOnly(6, this._instanceDataTBOTex[_STATE][3], this.instanceDataFormat[_STATE]);
            }
            GL20.glUniform2f(program.location[0], amount, currValidSize);
            GL43.glDispatchCompute(1, itemDim, itemDim);
        }/* else if (BoxConfigs.isCLParallel()) { // CL mode.
            boolean isAMD = BoxDatabase.getGLState().GL_CURRENT_DEVICE_VENDOR_BYTE == BoxEnum.GL_DEVICE_AMD_ATI;
            final long itemDim = (long) Math.ceil(Math.sqrt(this.getValidInstanceDataCount() / (BoxDatabase.isGLDeviceAMD() ? 64.0f : 32.0f)));
            BaseKernelData kernel = instance3DCheck ? KernelCore.getInstanceKernel3D() : KernelCore.getInstanceKernel2D();
            kernel.bindKernelArg(0, this.getInstanceDataMemory()[_FINAL_MATRIX][0]);
            kernel.bindKernelArg(1, this.getInstanceDataMemory()[_FINAL_MATRIX][3]);
            kernel.bindKernelArg(2, this.getInstanceDataMemory()[_FINAL_MATRIX][1]);
            kernel.bindKernelArg(3, this.getInstanceDataMemory()[_FINAL_MATRIX][4]);
            final int[] index = instance3DCheck ? new int[]{6, 7, 8, 9, 10, 11} : new int[]{4, 5, 6, 7, 8, 9};
            if (instance3DCheck) {
                kernel.bindKernelArg(4, this.getInstanceDataMemory()[_FINAL_MATRIX][2]);
                kernel.bindKernelArg(5, this.getInstanceDataMemory()[_FINAL_MATRIX][5]);
            }
            kernel.bindKernelArg(index[0], this.getInstanceDataMemory()[_TIMER][0]);
            kernel.bindKernelArg(index[1], this.getInstanceDataMemory()[_STATE][0]);
            kernel.bindKernelArg(index[2], this.getInstanceDataMemory()[_STATE][1]);
            kernel.bindKernelArg(index[3], this.getInstanceDataMemory()[_STATE][2]);
            kernel.bindKernelArg(index[4], KernelCore.getInstanceSampler());
            FloatBuffer buffer = BufferUtils.createFloatBuffer(2);
            buffer.put(amount);
            buffer.put(this.getValidInstanceDataCount());
            buffer.flip();
            CL10.clEnqueueWriteBuffer(KernelCore.getDefaultCLQueue(), this.getInstanceDataMemory()[3][0], CL10.CL_TRUE, 0, buffer, null, null);
            kernel.bindKernelArg(index[5], this.getInstanceDataMemory()[3][0]);
            final long[] local = isAMD ? new long[]{4, 16} : new long[]{4, 8};
            kernel.enqueueND(2, new long[]{itemDim, itemDim}, local);
        }*/
        if (!this.isAlwaysRefreshInstanceData()) this.callRefreshInstanceData(false);
    }

    public boolean isNeedRefreshInstanceData() {
        return this.needRefreshInstanceData[0] || this.needRefreshInstanceData[1];
    }

    public boolean isAlwaysRefreshInstanceData() {
        return this.needRefreshInstanceData[1];
    }

    /**
     * When changed the instance data(Not fixed instance data), call it.
     */
    public void callRefreshInstanceData(boolean refresh) {
        this.needRefreshInstanceData[0] = refresh;
    }

    public void setAlwaysRefreshInstanceData(boolean refresh) {
        this.needRefreshInstanceData[1] = refresh;
    }

    /**
     * @return true if after called {@link InstanceRenderAPI#submitFixedInstanceData()} or {@link InstanceRenderAPI#mallocFixedInstanceData(int)}.
     */
    public boolean isCalledFixedSubmit() {
        return this.needRefreshInstanceData[2];
    }

    public boolean haveInstanceData() {
        return this.instanceData != null && !this.instanceData.isEmpty();
    }

    public boolean haveValidInstanceData() {
        return this.getValidInstanceDataCount() > 0;
    }

    public int getValidInstanceDataCount() {
        return this.instanceRefreshState[2];
    }

    public FloatBuffer[][] getInstanceDataTmpBufferJVM() {
        return this._instanceDataJVM;
    }

    public int[][] getInstanceDataTBO() {
        return this._instanceDataTBO;
    }

    public int[][] getInstanceDataTBOTex() {
        return this._instanceDataTBOTex;
    }

//    public CLMem[][] getInstanceDataMemory() {
//        return this._instanceDataMemory;
//    }

    public void putShaderInstanceData() {
        boolean instance3DCheck = isInstanceData3D();
        GL13.glActiveTexture(GL13.GL_TEXTURE10);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][0]);
        GL13.glActiveTexture(GL13.GL_TEXTURE11);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][1]);
        if (instance3DCheck) {
            GL13.glActiveTexture(GL13.GL_TEXTURE12);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_FINAL_MATRIX][2]);
            GL13.glActiveTexture(GL13.GL_TEXTURE13);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_STATE][0]);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE14);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._instanceDataTBOTex[_COLOR][0]);
    }

    public int getInstanceDataRefreshIndex() {
        return this.instanceRefreshState[0];
    }

    public int getInstanceDataRefreshSize() {
        return this.instanceRefreshState[1];
    }

    /**
     * @param index will refresh instance data start from this index.
     */
    public void setInstanceDataRefreshIndex(int index) {
        if (this.instanceData == null) return;
        this.instanceRefreshState[0] = Math.min(Math.max(index, 0), Math.max(this.instanceData.size() - 1, 0));
    }

    /**
     * @param size Will refresh instance data count.
     */
    public void setInstanceDataRefreshSize(int size) {
        if (this.instanceData == null) return;
        this.instanceRefreshState[1] = this.instanceRefreshState[0] + size > this.instanceData.size() ? this.instanceData.size() - this.instanceRefreshState[0] : size;
    }

    public void setInstanceDataRefreshAllFromCurrentIndex() {
        if (this.instanceData == null) return;
        this.instanceRefreshState[1] = this.instanceData.size() - this.instanceRefreshState[0];
    }

    public int getRenderingCount() {
        return this.instanceRefreshState[3];
    }

    public void setRenderingCount(int num) {
        this.instanceRefreshState[3] = Math.max(num, 0);
    }

    public byte getInstanceDataType() {
        return this.instanceDataType[0];
    }

    public boolean isInstanceData2D() {
        return this.instanceDataType[0] == 0;
    }

    /**
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    public void setUseInstanceData2D() {
        this.instanceDataType[0] = 0;
    }

    public boolean isInstanceData3D() {
        return this.instanceDataType[0] == 1;
    }

    /**
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    public void setUseInstanceData3D() {
        this.instanceDataType[0] = 1;
    }

    public boolean isInstanceDataCustom() {
        return this.instanceDataType[0] == 2;
    }

    /**
     * Will be recognized as 3D-data.<p>
     * If call it, should override {@link InstanceRenderAPI#submitInstanceData()} and {@link InstanceRenderAPI#sysRefreshInstanceData(float, boolean)} in your class.<p>
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    public void setUseInstanceDataCustom() {
        this.instanceDataType[0] = 2;
    }

    /**
     * Use 2D-data for default.<p>
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    public void setUseDefaultInstanceData() {
        this.instanceDataType[0] = 0;
    }

    /**
     * Also it is byte size.
     */
    public byte getInstanceDataFormat() {
        return this.instanceDataType[1];
    }

    public boolean isNormalFloatFormatInstanceData() {
        return this.instanceDataType[1] == BoxDatabase.FLOAT_SIZE;
    }

    public void setUseNormalFormatFloatInstanceData() {
        this.instanceDataType[1] = BoxDatabase.FLOAT_SIZE;
        this.instanceDataFormat[0] = GL30.GL_RGBA32F;
        this.instanceDataFormat[2] = GL30.GL_RGBA32F;
        this.instanceDataFormat[3] = GL30.GL_RG32F;
    }

    public boolean isHalfFloatFormatInstanceData() {
        return this.instanceDataType[1] == BoxDatabase.HALF_FLOAT_SIZE;
    }

    public void setUseHalfFloatFormatInstanceData() {
        this.instanceDataType[1] = BoxDatabase.HALF_FLOAT_SIZE;
        this.instanceDataFormat[0] = GL30.GL_RGBA16F;
        this.instanceDataFormat[2] = GL30.GL_RGBA16F;
        this.instanceDataFormat[3] = GL30.GL_RG16F;
    }

    public void setUseDefaultFormatInstanceData() {
        this.instanceDataType[1] = BoxDatabase.FLOAT_SIZE;
        this.instanceDataFormat[0] = GL30.GL_RGBA32F;
        this.instanceDataFormat[2] = GL30.GL_RGBA32F;
        this.instanceDataFormat[3] = GL30.GL_RG32F;
    }

    public float getInstanceTimerOverride() {
        return this.instanceTimerOverride;
    }

    /**
     * Override timer of all in-vRAM instance data to this value.
     *
     * @param alpha set less than 0 to disable override.
     * @param state valid state: {@link BoxEnum#TIMER_IN}, {@link BoxEnum#TIMER_FULL}, {@link BoxEnum#TIMER_OUT}.
     */
    public void setInstanceTimerOverride(float alpha, byte state) {
        this.instanceTimerOverride = Math.min(alpha, 5.0f);
        if (this.instanceTimerOverride >= 0.0f) {
            if (state == BoxEnum.TIMER_FULL) this.instanceTimerOverride += 20.0f;
            else if (state == BoxEnum.TIMER_OUT) this.instanceTimerOverride += 10.0f;
            else if (state != BoxEnum.TIMER_IN) this.instanceTimerOverride = -1.0f;
        }
    }

    public void copyInstanceTimerOverride(InstanceRenderAPI renderData) {
        this.instanceTimerOverride = renderData.getInstanceTimerOverride();
    }

    public byte getGlobalTimerState() {
        if (this.haveValidInstanceData() && this.globalTimer[0] > 0.0f) return BoxEnum.TIMER_FULL;
        else if (this.globalTimer[0] > 2.0f) return BoxEnum.TIMER_IN;
        else if (this.globalTimer[0] > 1.0f) return BoxEnum.TIMER_FULL;
        else if (this.globalTimer[0] > 0.0f) return BoxEnum.TIMER_OUT;
        else return this.isGlobalTimerOnce() ? BoxEnum.TIMER_ONCE : BoxEnum.TIMER_INVALID;
    }
}
