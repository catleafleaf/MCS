package org.boxutil.base.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.units.standard.attribute.MaterialData;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

public interface InstanceRenderAPI {
    /**
     * When call it, will reset data and delete related objects.<p>
     * A.K.A. <strong>free()</strong> for instance data resources.
     */
    void resetInstanceData();

    List<InstanceDataAPI> getInstanceData();

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
    byte setInstanceData(@Nullable List<InstanceDataAPI> instanceData);

    /**
     * Reset global timer by manual.<p>
     * Similar to {@link #setInstanceData(List)}.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success, return {@link BoxEnum#STATE_FAILED} when over limit.
     */
    byte setInstanceData(@Nullable List<InstanceDataAPI> instanceData, float fadeIn, float full, float fadeOut);

    /**
     * Have size limit {@link BoxConfigs#getMaxInstanceDataSize()}.<p>
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success, return {@link BoxEnum#STATE_FAILED} when over limit, return {@link BoxEnum#STATE_FAILED_OTHER} when added a null object.
     */
    byte addInstanceData(@NotNull InstanceDataAPI instanceData);

    /**
     * Use it when you changed the instance data.<p>
     * Put all data, but without calculating.<p>
     * For calculating, if running on JVM mode, it will not create any tmp-TBO, will get a matrix-TBO finally.<p>
     * <p>
     * <strong>Should have reserved size for instance-list, if adds more data later. If not, will clear previous data of all in texture objects when submit.</strong><p>
     * If list size has increased, will force to submit for all data.<p>
     * For submit, here will use {@link GL15#glBufferSubData(int, long, ByteBuffer)} for update TBOs(why SSBO in 4.3) in once ergodic, else {@link GL15#glMapBuffer(int, int, ByteBuffer)} is unable to meet demand.<p>
     * <p>
     * In half-float mode, Cost <strong>64Byte</strong> of vRAM each 2D-instance, when running on GL mode, entirely GPU calculate; In other words, rendering <strong>32768</strong> instances will cost <strong>1.75MB</strong> of vRAM.<p>
     * For 3D-instance cost <strong>88Byte</strong> and <strong>32768</strong> instances cost <strong>2.5MB</strong> of vRAM.<p>
     * Cost double vRAM in normal-float mode for each instance data.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success, return {@link BoxEnum#STATE_FAILED} when over limit, return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    byte submitInstanceData();

    /**
     * Optional.<p>
     * Just <strong>malloc()</strong> without any submit call.
     *
     * @param dataNum must be positive integer.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when parameter error.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    byte mallocInstanceData(int dataNum);

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
    byte submitFixedInstanceData();

    /**
     * Optional.<p>
     * Just <strong>malloc()</strong> without any submit call.
     *
     * @param dataNum must be positive integer.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when parameter error.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    byte mallocFixedInstanceData(int dataNum);

    /**
     * For generally, automatic running in rendering manager, not required calling it.
     */
    void sysRefreshInstanceData(float amount, boolean isPaused);

    boolean isNeedRefreshInstanceData();

    boolean isAlwaysRefreshInstanceData();

    /**
     * When changed the instance data(Not fixed instance data), call it.
     */
    void callRefreshInstanceData(boolean refresh);

    void setAlwaysRefreshInstanceData(boolean refresh);

    /**
     * @return true if after called {@link InstanceRenderAPI#submitFixedInstanceData()} or {@link InstanceRenderAPI#mallocFixedInstanceData(int)}.
     */
    boolean isCalledFixedSubmit();

    boolean haveInstanceData();

    boolean haveValidInstanceData();

    int getValidInstanceDataCount();

    FloatBuffer[][] getInstanceDataTmpBufferJVM();

    int[][] getInstanceDataTBO();

    int[][] getInstanceDataTBOTex();

//    CLMem[][] getInstanceDataMemory();

    void putShaderInstanceData();

    int getInstanceDataRefreshIndex();

    int getInstanceDataRefreshSize();

    /**
     * @param index will refresh instance data start from this index.
     */
    void setInstanceDataRefreshIndex(int index);

    /**
     * @param size Will refresh instance data count.
     */
    void setInstanceDataRefreshSize(int size);

    void setInstanceDataRefreshAllFromCurrentIndex();

    int getRenderingCount();

    void setRenderingCount(int num);

    byte getInstanceDataType();

    boolean isInstanceData2D();

    /**
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    void setUseInstanceData2D();

    boolean isInstanceData3D();

    /**
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    void setUseInstanceData3D();

    boolean isInstanceDataCustom();

    /**
     * Will be recognized as 3D-data.<p>
     * If call it, should override {@link InstanceRenderAPI#submitInstanceData()} and {@link InstanceRenderAPI#sysRefreshInstanceData(float, boolean)} in your class.<p>
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    void setUseInstanceDataCustom();

    /**
     * Use 2D-data for default.<p>
     * Must call it before {@link InstanceRenderAPI#submitInstanceData()}, or after {@link InstanceRenderAPI#resetInstanceData} has called.
     */
    void setUseDefaultInstanceData();

    /**
     * Also it is byte size.
     */
    byte getInstanceDataFormat();

    boolean isNormalFloatFormatInstanceData();

    void setUseNormalFormatFloatInstanceData();

    boolean isHalfFloatFormatInstanceData();

    void setUseHalfFloatFormatInstanceData();

    void setUseDefaultFormatInstanceData();

    float getInstanceTimerOverride();

    /**
     * Override timer of all in-vRAM instance data to this value.
     *
     * @param alpha set less than 0 to disable override.
     * @param state valid state: {@link BoxEnum#TIMER_IN}, {@link BoxEnum#TIMER_FULL}, {@link BoxEnum#TIMER_OUT}.
     */
    void setInstanceTimerOverride(float alpha, byte state);

    void copyInstanceTimerOverride(InstanceRenderAPI renderData);
}
