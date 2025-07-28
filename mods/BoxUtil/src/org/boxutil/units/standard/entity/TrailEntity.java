package org.boxutil.units.standard.entity;

import com.fs.starfarer.api.util.Misc;
import org.boxutil.base.BaseRenderData;
import org.boxutil.base.api.MaterialRenderAPI;
import org.boxutil.base.api.SimpleVAOAPI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.standard.attribute.MaterialData;
import org.boxutil.util.CommonUtil;
import org.boxutil.util.CurveUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a 2D trail.<p>
 * Trail entity will not apply AA if depth based AA is enabled, and only rendering on color mode.<p>
 * Use vanilla beam texture uv(transversal beam texture).
 */
public class TrailEntity extends BaseRenderData implements MaterialRenderAPI {
    protected final static byte _BUFFER_DATA_SIZE = 3;
    // {vec2(loc), vec2(tangent left), vec2(tangent right), vec4(color), vec4(emissive), width, mixFactor}
    protected List<Vector2f> nodeList = null;
    protected int _lastNodeLength = 0;
    protected int _lastRenderingLength = 0;
    protected List<Float> _distance = null;
    protected int shouldRenderingCount = 0;
    // {vec2(loc), float(distance)}
    protected int _TBO = 0;
    protected int _TBOTex = 0;
    protected final int[] nodeRefreshState = new int[]{0, 0, 8};
    // vec4(start), vec4(end), vec4(startEmissive), vec4(endEmissive)
    protected final float[] colorState = new float[]{BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE};
    // startWidth, endWidth, mixFactor, texture speed, texture pixels, fillStart, fillEnd, startFactor, endFactor, jitterPower, flickerMix, flickerCode, uvOffset
    protected final float[] state = new float[]{1.0f, 1.0f, 1.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, this.hashCode() * 0.00066667f, 0.0f};
    protected boolean flowWhenPaused = false;
    protected boolean flickWhenPaused = false;
    protected boolean flickToggle = false;
    protected MaterialData material = new MaterialData();

    public TrailEntity() {
        this.getMaterialData().setDisableCullFace();
    }

    public void delete() {
        super.delete();
        this.material = null;
        this._lastNodeLength = 0;
        this._lastRenderingLength = 0;
        this.nodeList = null;
        this._distance = null;
        if (BoxConfigs.isTBOSupported()) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
            if (this._TBOTex != 0) GL11.glDeleteTextures(this._TBOTex);
            if (this._TBO != 0) GL15.glDeleteBuffers(this._TBO);
        }
    }

    public void glDraw() {
        SimpleVAOAPI line = ShaderCore.getDefaultLineObject();
        if (line == null || !line.isValid() || !this.isHaveValidNodeCount()) return;
        GL30.glBindVertexArray(line.getVAO());
        GL31.glDrawArraysInstanced(GL11.GL_LINES, 0, 2, this.glPrimCount());
    }

    public void reset() {
        super.reset();
        this.colorState[0] = this.colorState[1] = this.colorState[2] = this.colorState[3] = BoxEnum.ONE;
        this.colorState[4] = this.colorState[5] = this.colorState[6] = this.colorState[7] = BoxEnum.ONE;
        this.colorState[8] = this.colorState[9] = this.colorState[10] = this.colorState[11] = BoxEnum.ONE;
        this.colorState[12] = this.colorState[13] = this.colorState[14] = this.colorState[15] = BoxEnum.ONE;
        this.state[0] = 1.0f;
        this.state[1] = 1.0f;
        this.state[2] = 1.0f;
        this.state[3] = 4.0f;
        this.state[4] = 1.0f;
        this.state[5] = 1.0f;
        this.state[6] = 1.0f;
        this.state[7] = 1.0f;
        this.state[8] = 1.0f;
        this.state[9] = 0.0f;
        this.state[10] = 1.0f;
        this.state[11] = this.hashCode() * 0.00066667f;
        this.state[12] = 0.0f;
        this.flowWhenPaused = false;
        this.flickWhenPaused = false;
        this.flickToggle = false;
    }

    public void resetNodes() {
        this.nodeList.clear();
        this._distance.clear();
        this._lastNodeLength = 0;
        this._lastRenderingLength = 0;
        this.nodeRefreshState[0] = 0;
        this.nodeRefreshState[1] = 0;
        this.nodeRefreshState[2] = 8;
    }

    /**
     * Use it when you changed the node data.<p>
     * Will not keeping any node data if beyonds the initial size, recommend to call {@link TrailEntity#mallocNodeData(int)} for allocate enough size before.<p>
     * Cost <strong>12Byte * n</strong> of vRAM when have number of <strong>n</strong> nodes<strong>(n is non-zero even number)</strong>, that if it had <strong>8192</strong> nodeList will cost <strong>96KB</strong> of vRAM.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when an empty node list or refresh count is zero.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte submitNodes() {
        if (this.nodeList == null || this.nodeList.size() < 2) return BoxEnum.STATE_FAILED;
        if (!BoxConfigs.isTBOSupported()) return BoxEnum.STATE_FAILED_OTHER;
        final int nodeSize = this.nodeList.size();
        final boolean newBuffer = nodeSize > this._lastNodeLength;
        final int refreshIndex = this._lastNodeLength == 0 ? 0 : this.nodeRefreshState[0];
        final int refreshCount = newBuffer ? nodeSize - refreshIndex : this.nodeRefreshState[1];
        if (refreshCount < 1) return BoxEnum.STATE_FAILED;
        final int refreshLimit = refreshIndex + refreshCount;
        final int bufferSizeLoc = refreshCount * _BUFFER_DATA_SIZE;
        final long bufferSize = (long) bufferSizeLoc * BoxDatabase.FLOAT_SIZE;
        final long bufferIndex = (long) refreshIndex * _BUFFER_DATA_SIZE * BoxDatabase.FLOAT_SIZE;

        if (this._TBO == 0) {
            this._TBO = GL15.glGenBuffers();
            this._TBOTex = GL11.glGenTextures();
            if (this._TBO == 0 || this._TBOTex == 0) return BoxEnum.STATE_FAILED_OTHER;
        }
        if (newBuffer) {
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
        }

        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO);
        ByteBuffer buffer = GL30.glMapBufferRange(GL31.GL_TEXTURE_BUFFER, bufferIndex, bufferSize, GL30.GL_MAP_WRITE_BIT, null);
        if (buffer == null) {
            GL15.glUnmapBuffer(GL31.GL_TEXTURE_BUFFER);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
            return BoxEnum.STATE_FAILED_OTHER;
        }
        buffer.order(ByteOrder.nativeOrder());
        Vector2f currLoc, lastLoc;
        FloatBuffer newData = buffer.asFloatBuffer();
        int index = 0;
        float distance, tmpX, tmpY;
        if (refreshIndex == 0) distance = 0.0f;
        else {
            int getIndex = refreshIndex - 1;
            distance = (this._distance != null && this._distance.size() > getIndex) ? this._distance.get(getIndex) : 0.0f;
        }
        if (this._distance == null) this._distance = new ArrayList<>(nodeSize);
        for (int i = refreshIndex; i < refreshLimit; ++i) {
            currLoc = this.nodeList.get(i);
            newData.put(index, currLoc.x);
            ++index;
            newData.put(index, currLoc.y);
            ++index;
            if (i != 0) {
                lastLoc = this.nodeList.get(i - 1);
                tmpX = currLoc.x - lastLoc.x;
                tmpY = currLoc.y - lastLoc.y;
                distance += (float) Math.sqrt(tmpX * tmpX + tmpY * tmpY);
            }
            if (i >= this._distance.size()) this._distance.add(distance); else this._distance.set(i, distance);
            newData.put(index, distance);
            ++index;
        }
        buffer.position(0);
        buffer.limit(buffer.capacity());
        GL15.glUnmapBuffer(GL31.GL_TEXTURE_BUFFER);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGB32F, this._TBO); // damn it where my RGB16F is
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        this._lastNodeLength = nodeSize;
        this._lastRenderingLength = this.shouldRenderingCount = this._lastNodeLength - 1;
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * Optional.<p>
     * Just <strong>malloc()</strong> without any submit call.
     *
     * @param nodeNum must be positive integer.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when parameter error.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte mallocNodeData(int nodeNum) {
        if (nodeNum < 1) return BoxEnum.STATE_FAILED;
        if (!BoxConfigs.isTBOSupported()) return BoxEnum.STATE_FAILED_OTHER;
        final long bufferSize = (long) nodeNum * _BUFFER_DATA_SIZE * BoxDatabase.FLOAT_SIZE;

        if (this._TBO == 0) {
            this._TBO = GL15.glGenBuffers();
            this._TBOTex = GL11.glGenTextures();
            if (this._TBO == 0 || this._TBOTex == 0) return BoxEnum.STATE_FAILED_OTHER;
        }

        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGB32F, this._TBO);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        this._lastNodeLength = nodeNum;
        this._lastRenderingLength = this.shouldRenderingCount = this._lastNodeLength - 1;

        return BoxEnum.STATE_SUCCESS;
    }

    public int getNodeRefreshIndex() {
        return this.nodeRefreshState[0];
    }

    /**
     * @param index Will refresh node data start from this index.
     */
    public void setNodeRefreshIndex(int index) {
        int clamp = this.nodeList == null ? 0 : Math.max(this.nodeList.size() - 1, 0);
        this.nodeRefreshState[0] = Math.min(Math.max(index, 0), clamp);
    }

    /**
     * @param size Will refresh node data count.
     */
    public void setNodeRefreshSize(int size) {
        if (this.nodeList == null) return;
        this.nodeRefreshState[1] = this.nodeRefreshState[0] + size > this.nodeList.size() ? this.nodeList.size() - this.nodeRefreshState[0] : size;
    }

    public void setNodeRefreshAllFromCurrentIndex() {
        if (this.nodeList == null) return;
        this.nodeRefreshState[1] = this.nodeList.size() - this.nodeRefreshState[0];
    }

    public int getNodesTBO() {
        return this._TBO;
    }

    public int getNodesTBOTex() {
        return this._TBOTex;
    }

    public int getValidNodeCount() {
        return this._lastNodeLength;
    }

    public int getValidRenderingNodeCount() {
        return this._lastRenderingLength;
    }

    public boolean isHaveValidNodeCount() {
        return this._lastRenderingLength >= 1;
    }

    public int getNodeRenderingCount() {
        return this.shouldRenderingCount + 1;
    }

    public void setNodeRenderingCount(int nodeCount) {
        this.shouldRenderingCount = nodeCount < 2 ? 0 : nodeCount - 1;
    }

    protected int glPrimCount() {
        return Math.min(this.shouldRenderingCount, this.getValidRenderingNodeCount());
    }

    public List<Vector2f> getNodes() {
        return this.nodeList;
    }

    /**
     * Recommend to use relative coordinate.<p>
     * Node at index 0 will be the end point of the trail.
     *
     * @param nodeList Cannot have any null-node, and at least have two nodes, if not then pass this entity when rendering.
     */
    public void setNodes(@NotNull List<Vector2f> nodeList) {
        if (nodeList.size() > BoxConfigs.getMaxInstanceDataSize()) this.nodeList = nodeList.subList(0, BoxConfigs.getMaxInstanceDataSize());
        else this.nodeList = nodeList;
        if (this._distance == null) this._distance = new ArrayList<>(this.nodeList.size());
    }

    /**
     * Node at index 0 will be the end point of the trail, and draw from index end to the last index/ the start point.
     */
    public void addNode(@NotNull Vector2f node) {
        if (this.nodeList == null) this.nodeList = new ArrayList<>();
        if (this.nodeList.size() > BoxConfigs.getMaxInstanceDataSize()) return;
        this.nodeList.add(node);
        if (this._distance == null) this._distance = new ArrayList<>();
    }

    public void putShaderTrailData() {
        GL13.glActiveTexture(GL13.GL_TEXTURE10);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex);
    }

    public float[] getColorState() {
        return this.colorState;
    }

    public void copyColorState(float[] colorState) {
        System.arraycopy(colorState, 0, this.colorState, 0, this.colorState.length);
    }

    public float[] getStartColorArray() {
        return new float[]{this.colorState[0], this.colorState[1], this.colorState[2], this.colorState[3]};
    }

    public Color getStartColorC() {
        return CommonUtil.toCommonColor(this.getStartColor());
    }

    public Vector4f getStartColor() {
        return new Vector4f(this.colorState[0], this.colorState[1], this.colorState[2], this.colorState[3]);
    }

    public float getStartColorAlpha() {
        return this.colorState[3];
    }

    public void setStartColor(@NotNull Vector4f color) {
        this.colorState[0] = color.x;
        this.colorState[1] = color.y;
        this.colorState[2] = color.z;
        this.colorState[3] = color.w;
    }

    public void setStartColor(float r, float g, float b, float a) {
        this.colorState[0] = r;
        this.colorState[1] = g;
        this.colorState[2] = b;
        this.colorState[3] = a;
    }

    public void setStartColor(Color color) {
        this.colorState[0] = color.getRed() / 255.0f;
        this.colorState[1] = color.getGreen() / 255.0f;
        this.colorState[2] = color.getBlue() / 255.0f;
        this.colorState[3] = color.getAlpha() / 255.0f;
    }

    public void setStartColorAlpha(float alpha) {
        this.colorState[3] = alpha;
    }

    public float[] getEndColorArray() {
        return new float[]{this.colorState[4], this.colorState[5], this.colorState[6], this.colorState[7]};
    }

    public Color getEndColorC() {
        return CommonUtil.toCommonColor(this.getEndColor());
    }

    public Vector4f getEndColor() {
        return new Vector4f(this.colorState[4], this.colorState[5], this.colorState[6], this.colorState[7]);
    }

    public float getEndColorAlpha() {
        return this.colorState[7];
    }

    public void setEndColor(@NotNull Vector4f color) {
        this.colorState[4] = color.x;
        this.colorState[5] = color.y;
        this.colorState[6] = color.z;
        this.colorState[7] = color.w;
    }

    public void setEndColor(float r, float g, float b, float a) {
        this.colorState[4] = r;
        this.colorState[5] = g;
        this.colorState[6] = b;
        this.colorState[7] = a;
    }

    public void setEndColor(Color color) {
        this.colorState[4] = color.getRed() / 255.0f;
        this.colorState[5] = color.getGreen() / 255.0f;
        this.colorState[6] = color.getBlue() / 255.0f;
        this.colorState[7] = color.getAlpha() / 255.0f;
    }

    public void setEndColorAlpha(float alpha) {
        this.colorState[7] = alpha;
    }

    public float[] getStartEmissiveArray() {
        return new float[]{this.colorState[8], this.colorState[9], this.colorState[10], this.colorState[11]};
    }

    public Color getStartEmissiveC() {
        return CommonUtil.toCommonColor(this.getStartEmissive());
    }

    public Vector4f getStartEmissive() {
        return new Vector4f(this.colorState[8], this.colorState[9], this.colorState[10], this.colorState[11]);
    }

    public float getStartEmissiveAlpha() {
        return this.colorState[11];
    }

    public void setStartEmissive(@NotNull Vector4f color) {
        this.colorState[8] = color.x;
        this.colorState[9] = color.y;
        this.colorState[10] = color.z;
        this.colorState[11] = color.w;
    }

    public void setStartEmissive(float r, float g, float b, float a) {
        this.colorState[8] = r;
        this.colorState[9] = g;
        this.colorState[10] = b;
        this.colorState[11] = a;
    }

    public void setStartEmissive(Color color) {
        this.colorState[8] = color.getRed() / 255.0f;
        this.colorState[9] = color.getGreen() / 255.0f;
        this.colorState[10] = color.getBlue() / 255.0f;
        this.colorState[11] = color.getAlpha() / 255.0f;
    }

    public void setStartEmissiveAlpha(float alpha) {
        this.colorState[11] = alpha;
    }

    public float[] getEndEmissiveArray() {
        return new float[]{this.colorState[12], this.colorState[13], this.colorState[14], this.colorState[15]};
    }

    public Color getEndEmissiveC() {
        return CommonUtil.toCommonColor(this.getEndEmissive());
    }

    public Vector4f getEndEmissive() {
        return new Vector4f(this.colorState[12], this.colorState[13], this.colorState[14], this.colorState[15]);
    }

    public float getEndEmissiveAlpha() {
        return this.colorState[15];
    }

    public void setEndEmissive(@NotNull Vector4f color) {
        this.colorState[12] = color.x;
        this.colorState[13] = color.y;
        this.colorState[14] = color.z;
        this.colorState[15] = color.w;
    }

    public void setEndEmissive(float r, float g, float b, float a) {
        this.colorState[12] = r;
        this.colorState[13] = g;
        this.colorState[14] = b;
        this.colorState[15] = a;
    }

    public void setEndEmissive(Color color) {
        this.colorState[12] = color.getRed() / 255.0f;
        this.colorState[13] = color.getGreen() / 255.0f;
        this.colorState[14] = color.getBlue() / 255.0f;
        this.colorState[15] = color.getAlpha() / 255.0f;
    }

    public void setEndEmissiveAlpha(float alpha) {
        this.colorState[15] = alpha;
    }

    public float getStartWidth() {
        return this.state[0];
    }

    public void setStartWidth(float width) {
        this.state[0] = width * 0.5f;
    }

    public void setStartWidthDirect(float widthHalf) {
        this.state[0] = widthHalf;
    }

    public float getEndWidth() {
        return this.state[1];
    }

    public void setEndWidth(float width) {
        this.state[1] = width * 0.5f;
    }

    public void setEndWidthDirect(float widthHalf) {
        this.state[1] = widthHalf;
    }

    public float getMixFactor() {
        return this.state[2];
    }

    public void setMixFactor(float factor) {
        this.state[2] = factor;
    }

    public float getTextureSpeed() {
        return this.state[3];
    }

    /**
     * It is working together with {@link CurveEntity#setTexturePixels(float)}.
     *
     * @param textureSpeed looks flowing forward when less than zero.
     */
    public void setTextureSpeed(float textureSpeed) {
        this.state[3] = textureSpeed;
    }

    public float getTexturePixels() {
        return this.state[4];
    }

    /**
     * @param texturePixels texture fill size.
     */
    public void setTexturePixels(float texturePixels) {
        this.state[4] = texturePixels;
    }

    public boolean isFlowWhenPaused() {
        return this.flowWhenPaused;
    }

    public void setFlowWhenPaused(boolean flow) {
        this.flowWhenPaused = flow;
    }

    public float getUVOffset() {
        return this.state[12];
    }

    public void setUVOffset(float offset) {
        this.state[12] = offset;
    }

    public float getFillStartAlpha() {
        return this.state[5];
    }

    public void setFillStartAlpha(float alpha) {
        this.state[5] = alpha;
    }

    public float getFillEndAlpha() {
        return this.state[6];
    }

    public void setFillEndAlpha(float alpha) {
        this.state[6] = alpha;
    }

    public float getFillStartFactor() {
        return this.state[7];
    }

    public void setFillStartFactor(float factor) {
        this.state[7] = factor;
    }

    public float getFillEndFactor() {
        return this.state[8];
    }

    public void setFillEndFactor(float factor) {
        this.state[8] = factor;
    }

    public float getJitterPower() {
        return this.state[9];
    }

    /**
     * perpendicular to trail, effect on trail texture uv.
     */
    public void setJitterPower(float power) {
        this.state[9] = power;
    }

    public boolean isFlick() {
        return this.flickToggle;
    }

    public void setFlick(boolean flick) {
        this.flickToggle = flick;
    }

    public boolean isFlickWhenPaused() {
        return this.flickWhenPaused;
    }

    public void setFlickWhenPaused(boolean flick) {
        this.flickWhenPaused = flick;
    }

    public float getFlickMixValue() {
        return this.state[10];
    }

    public void setFlickMixValue(float mix) {
        this.state[10] = mix;
    }

    public float getCurrentFlickerSyncValue() {
        return this.state[11];
    }

    /**
     * @param code java instance {@link #hashCode()} * 0.00066667f for default.
     */
    public void setFlickerSyncCode(int code) {
        this.state[11] = code;
    }

    public @NotNull MaterialData getMaterialData() {
        if (this.hasDelete()) return new MaterialData();
        return this.material;
    }

    public void setMaterialData(@NotNull MaterialData material) {
        this.material = material == null ? new MaterialData() : material;
    }

    public FloatBuffer pickDataPackage_vec4() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(40);
        buffer.put(this.material.getState());
        buffer.put(11, this.getGlobalTimerAlpha());
        buffer.put(12, this.state[4]);
        buffer.put(14, this.glPrimCount());
        buffer.put(15, this.state[2]);
        buffer.put(16, this.state[5]);
        buffer.put(17, this.state[6]);
        buffer.put(18, this.state[7]);
        buffer.put(19, this.state[8]);
        buffer.put(20, this.state[0]);
        buffer.put(21, this.state[1]);
        buffer.put(22, this.state[9]);
        buffer.put(23, this.isFlick() ? this.getFlickMixValue() : -10.0f);
        buffer.put(24, this.colorState[0]);
        buffer.put(25, this.colorState[1]);
        buffer.put(26, this.colorState[2]);
        buffer.put(27, this.colorState[3]);
        buffer.put(28, this.colorState[4]);
        buffer.put(29, this.colorState[5]);
        buffer.put(30, this.colorState[6]);
        buffer.put(31, this.colorState[7]);
        buffer.put(32, this.colorState[8]);
        buffer.put(33, this.colorState[9]);
        buffer.put(34, this.colorState[10]);
        buffer.put(35, this.colorState[11]);
        buffer.put(36, this.colorState[12]);
        buffer.put(37, this.colorState[13]);
        buffer.put(38, this.colorState[14]);
        buffer.put(39, this.colorState[15]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    @Deprecated
    public byte getDrawMode() {return BoxEnum.MODE_COLOR;}

    @Deprecated
    public void setDrawMode(byte drawMode) {}
}
