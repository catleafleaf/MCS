package org.boxutil.units.standard.entity;

import com.fs.starfarer.api.util.Misc;
import org.boxutil.util.CurveUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.boxutil.base.BaseRenderData;
import org.boxutil.base.api.MaterialRenderAPI;
import org.boxutil.base.api.SimpleVAOAPI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.standard.attribute.MaterialData;
import org.boxutil.units.standard.attribute.NodeData;
import org.boxutil.util.CommonUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a 2D curve.<p>
 * Segment entity will not apply AA if depth based AA is enabled, and only rendering on color mode.<p>
 * Use vanilla beam texture uv(transversal beam texture).
 */
public class SegmentEntity extends BaseRenderData implements MaterialRenderAPI {
    protected final static float _CIRCLE_FIX = 1.3333334f;
    protected final static byte _TBO_COUNT = 3;
    protected final static byte _BUFFER_DATA_SIZE = 8;
    // {vec2(loc), vec2(tangent left), vec2(tangent right), vec4(color), vec4(emissive), width, mixFactor}
    protected List<NodeData> nodeList = null;
    protected int _lastNodeLength = 0;
    protected int _lastRenderingLength = 0;
    protected int shouldRenderingCount = 0;
    // {vec4(vec2(loc), vec2(tangent left)), vec4(vec2(tangent right), width, distance), vec4(color), vec4(emissive)}
    protected int[] _TBO = new int[_TBO_COUNT];
    protected int[] _TBOTex = new int[_TBO_COUNT];
    protected final int[] nodeRefreshState = new int[]{0, 0, 8};
    protected ShortBuffer _lastBuffer = null;
    protected ShortBuffer _lastMixFactorBuffer = null;
    protected ByteBuffer _lastColorBuffer = null;
    protected List<Float> _distance = null;
    protected short interpolation = 0;
    protected final float[] state = new float[]{4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f}; // texture speed, texture pixels, fillStart, fillEnd, startFactor, endFactor, uvOffset
    protected boolean flowWhenPaused = false;
    protected MaterialData material = new MaterialData();

    public SegmentEntity() {
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
            for (byte i = 0; i < _TBO_COUNT; i++) {
                if (this._TBOTex[i] != 0) GL11.glDeleteTextures(this._TBOTex[i]);
                if (this._TBO[i] != 0) GL15.glDeleteBuffers(this._TBO[i]);
            }
        }
        this._lastBuffer = null;
        this._lastMixFactorBuffer = null;
        this._lastColorBuffer = null;
    }

    public void glDraw() {
        SimpleVAOAPI line = ShaderCore.getDefaultLineObject();
        if (line == null || !line.isValid() || !this.isHaveValidNodeCount()) return;
        GL30.glBindVertexArray(line.getVAO());
        GL31.glDrawArraysInstanced(GL40.GL_PATCHES, 0, 2, Math.min(this.getSegmentsRenderingCount(), this.getValidRenderingNodeCount()));
    }

    public void reset() {
        super.reset();
        this.interpolation = 0;
        this.state[0] = 4.0f;
        this.state[1] = 1.0f;
        this.state[2] = 1.0f;
        this.state[3] = 1.0f;
        this.state[4] = 1.0f;
        this.state[5] = 1.0f;
        this.state[6] = 0.0f;
        this.flowWhenPaused = false;
    }

    public void resetNodes() {
        this.nodeList.clear();
        this._distance.clear();
        this._lastNodeLength = 0;
        this._lastRenderingLength = 0;
        this._lastBuffer = null;
        this._lastMixFactorBuffer = null;
        this._lastColorBuffer = null;
        this.nodeRefreshState[0] = 0;
        this.nodeRefreshState[1] = 0;
        this.nodeRefreshState[2] = 8;
    }

    /**
     * Use it when you changed the node data.<p>
     * Different from instance data submit call, node submit call is always keeping all node data, however, will cost some RAM.<p>
     * Cost <strong>26Byte * n</strong> of vRAM when have number of <strong>n</strong> nodes<strong>(n is non-zero even number)</strong>, that if it had <strong>8192</strong> nodeList will cost <strong>208KB</strong> of vRAM.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when an empty node list or refresh count is zero.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte submitNodes() {
        if (this.nodeList == null || this.nodeList.size() < 2) return BoxEnum.STATE_FAILED;
        if (!BoxConfigs.isTBOSupported()) return BoxEnum.STATE_FAILED_OTHER;
        final int nodeSize = this.getListValidNodeCount();
        final boolean newBuffer = nodeSize > this._lastNodeLength;
        final int refreshIndex = this._lastNodeLength == 0 ? 0 : this.nodeRefreshState[0];
        final int refreshCount = newBuffer ? nodeSize - refreshIndex : this.nodeRefreshState[1];
        if (refreshCount < 1) return BoxEnum.STATE_FAILED;
        final int refreshLimit = refreshIndex + refreshCount;
        final int bufferSize = refreshCount * _BUFFER_DATA_SIZE;
        final int bufferIndex = refreshIndex * _BUFFER_DATA_SIZE;

        if (this._TBO[0] == 0) {
            IntBuffer tboObject = BufferUtils.createIntBuffer(_TBO_COUNT);
            IntBuffer tboTex = BufferUtils.createIntBuffer(_TBO_COUNT);
            GL15.glGenBuffers(tboObject);
            GL11.glGenTextures(tboTex);
            for (byte i = 0; i < _TBO_COUNT; i++) {
                this._TBO[i] = tboObject.get(i);
                this._TBOTex[i] = tboTex.get(i);
            }
            if (this._TBO[0] == 0 || this._TBOTex[0] == 0) return BoxEnum.STATE_FAILED_OTHER;
        }
        if (newBuffer) {
            ShortBuffer tmpBuffer = BufferUtils.createShortBuffer(bufferSize);
            ShortBuffer tmpMixFactorBuffer = BufferUtils.createShortBuffer(refreshCount);
            ByteBuffer tmpColorBuffer = BufferUtils.createByteBuffer(bufferSize);
            if (this._lastBuffer != null) tmpBuffer.put(this._lastBuffer);
            if (this._lastMixFactorBuffer != null) tmpMixFactorBuffer.put(this._lastMixFactorBuffer);
            if (this._lastColorBuffer != null) tmpColorBuffer.put(this._lastColorBuffer);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[0]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, (long) bufferSize * BoxDatabase.HALF_FLOAT_SIZE, GL15.GL_DYNAMIC_DRAW);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[1]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, (long) refreshCount * BoxDatabase.HALF_FLOAT_SIZE, GL15.GL_DYNAMIC_DRAW);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[2]);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
            this._lastBuffer = tmpBuffer;
            this._lastMixFactorBuffer = tmpMixFactorBuffer;
            this._lastColorBuffer = tmpColorBuffer;
        }

        NodeData data;
        ShortBuffer newData = BufferUtils.createShortBuffer(bufferSize);
        ShortBuffer newMixFactorData = BufferUtils.createShortBuffer(refreshCount);
        ByteBuffer newColorData = BufferUtils.createByteBuffer(bufferSize);
        float nodeDis;
        if (refreshIndex == 0) nodeDis = 0.0f;
        else {
            int getIndex = refreshIndex - 1;
            nodeDis = (this._distance != null && this._distance.size() > getIndex) ? this._distance.get(getIndex) : 0.0f;
        }
        if (this._distance == null) this._distance = new ArrayList<>(nodeSize);
        float nodeDisTmp;
        boolean isNodeEnd = (refreshIndex & 1) == 1;
        short[] dataPackage;
        for (int i = refreshIndex; i < refreshLimit; i++) {
            data = this.nodeList.get(i);
            dataPackage = CommonUtil.float16ToShort(data.getState());
            if (i != 0) {
                nodeDisTmp = CurveUtil.getCurveLength(data, this.nodeList.get(i - 1), this.nodeRefreshState[2]);
                if (isNodeEnd && nodeDisTmp > 1.0E-05f) nodeDis = 0.0f; else nodeDis += nodeDisTmp;
            }
            if (i >= this._distance.size()) this._distance.add(nodeDis); else this._distance.set(i, nodeDis);
            newData.put(dataPackage[0]);
            newData.put(dataPackage[1]);
            newData.put(dataPackage[2]);
            newData.put(dataPackage[3]);
            newData.put(dataPackage[4]);
            newData.put(dataPackage[5]);
            newData.put(dataPackage[6]);
            newData.put(CommonUtil.float16ToShort(nodeDis));
            newMixFactorData.put(dataPackage[7]);
            newColorData.put(data.getColorState());
            isNodeEnd = (i & 1) == 1;
        }

        newData.position(0);
        newData.limit(newData.capacity());
        newMixFactorData.position(0);
        newMixFactorData.limit(newMixFactorData.capacity());
        newColorData.position(0);
        newColorData.limit(newColorData.capacity());
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[0]);
        GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, (long) bufferIndex * BoxDatabase.HALF_FLOAT_SIZE, newData);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA16F, this._TBO[0]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[1]);
        GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, (long) refreshIndex * BoxDatabase.HALF_FLOAT_SIZE, newMixFactorData);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[1]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_R16F, this._TBO[1]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[2]);
        GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, bufferIndex, newColorData);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[2]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL11.GL_RGBA8, this._TBO[2]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        this._lastBuffer.position(bufferIndex);
        this._lastBuffer.put(newData);
        this._lastBuffer.position(0);
        this._lastBuffer.limit(this._lastBuffer.capacity());
        this._lastMixFactorBuffer.position(refreshIndex);
        this._lastMixFactorBuffer.put(newMixFactorData);
        this._lastMixFactorBuffer.position(0);
        this._lastMixFactorBuffer.limit(this._lastMixFactorBuffer.capacity());
        this._lastColorBuffer.position(bufferIndex);
        this._lastColorBuffer.put(newColorData);
        this._lastColorBuffer.position(0);
        this._lastColorBuffer.limit(this._lastColorBuffer.capacity());
        this._lastNodeLength = nodeSize;
        this._lastRenderingLength = this.shouldRenderingCount = this._lastNodeLength / 2;
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
        final int realSize = nodeNum - (nodeNum & 1);
        if (realSize < 1) return BoxEnum.STATE_FAILED;
        if (!BoxConfigs.isTBOSupported()) return BoxEnum.STATE_FAILED_OTHER;
        final int bufferSize = realSize * _BUFFER_DATA_SIZE;

        if (this._TBO[0] == 0) {
            IntBuffer tboObject = BufferUtils.createIntBuffer(_TBO_COUNT);
            IntBuffer tboTex = BufferUtils.createIntBuffer(_TBO_COUNT);
            GL15.glGenBuffers(tboObject);
            GL11.glGenTextures(tboTex);
            for (byte i = 0; i < _TBO_COUNT; i++) {
                this._TBO[i] = tboObject.get(i);
                this._TBOTex[i] = tboTex.get(i);
            }
            if (this._TBO[0] == 0 || this._TBOTex[0] == 0) return BoxEnum.STATE_FAILED_OTHER;
        }
        ShortBuffer tmpBuffer = BufferUtils.createShortBuffer(bufferSize);
        ShortBuffer tmpMixFactorBuffer = BufferUtils.createShortBuffer(realSize);
        ByteBuffer tmpColorBuffer = BufferUtils.createByteBuffer(bufferSize);
        if (this._lastBuffer != null) {
            this._lastBuffer.position(0);
            this._lastBuffer.limit(Math.min(tmpBuffer.capacity(), this._lastBuffer.capacity()));
            tmpBuffer.put(this._lastBuffer);
        }
        if (this._lastMixFactorBuffer != null) {
            this._lastMixFactorBuffer.position(0);
            this._lastMixFactorBuffer.limit(Math.min(tmpMixFactorBuffer.capacity(), this._lastMixFactorBuffer.capacity()));
            tmpMixFactorBuffer.put(this._lastMixFactorBuffer);
        }
        if (this._lastColorBuffer != null) {
            this._lastColorBuffer.position(0);
            this._lastColorBuffer.limit(Math.min(tmpColorBuffer.capacity(), this._lastColorBuffer.capacity()));
            tmpColorBuffer.put(this._lastColorBuffer);
        }
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[0]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, (long) bufferSize * BoxDatabase.HALF_FLOAT_SIZE, GL15.GL_DYNAMIC_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA16F, this._TBO[0]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[1]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, (long) realSize * BoxDatabase.HALF_FLOAT_SIZE, GL15.GL_DYNAMIC_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[1]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_R16F, this._TBO[1]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._TBO[2]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[2]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL11.GL_RGBA8, this._TBO[2]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        this._lastBuffer = tmpBuffer;
        this._lastMixFactorBuffer = tmpMixFactorBuffer;
        this._lastColorBuffer = tmpColorBuffer;
        this._lastBuffer.position(0);
        this._lastBuffer.limit(this._lastBuffer.capacity());
        this._lastMixFactorBuffer.position(0);
        this._lastMixFactorBuffer.limit(this._lastMixFactorBuffer.capacity());
        this._lastColorBuffer.position(0);
        this._lastColorBuffer.limit(this._lastColorBuffer.capacity());
        this._lastNodeLength = realSize;
        this._lastRenderingLength = this.shouldRenderingCount = this._lastNodeLength / 2;

        return BoxEnum.STATE_SUCCESS;
    }

    public int getListValidNodeCount() {
        if (this.nodeList == null) return 0;
        return this.nodeList.size() - (this.nodeList.size() & 1);
    }

    public int getNodeRefreshIndex() {
        return this.nodeRefreshState[0];
    }

    /**
     * @param index Will refresh node data start from this index.
     */
    public void setNodeRefreshIndex(int index) {
        int clamp = this.nodeList == null ? 0 : Math.max(this.getListValidNodeCount() - 1, 0);
        this.nodeRefreshState[0] = Math.min(Math.max(index, 0), clamp);
    }

    /**
     * @param size Will refresh node data count.
     */
    public void setNodeRefreshSize(int size) {
        if (this.nodeList == null) return;
        int total = this.getListValidNodeCount();
        this.nodeRefreshState[1] = this.nodeRefreshState[0] + size > total ? total - this.nodeRefreshState[0] : size;
    }

    public void setNodeRefreshAllFromCurrentIndex() {
        if (this.nodeList == null) return;
        this.nodeRefreshState[1] = this.getListValidNodeCount() - this.nodeRefreshState[0];
    }

    public int getSubmitLengthCalculatingStep() {
        return this.nodeRefreshState[2];
    }

    /**
     * @param step must be greater than zero.
     */
    public void setSubmitLengthCalculatingStep(int step) {
        this.nodeRefreshState[2] = Math.max(step, 1);
    }

    public int[] getNodesTBO() {
        return this._TBO;
    }

    public int[] getNodesTBOTex() {
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

    public int getSegmentsRenderingCount() {
        return this.shouldRenderingCount;
    }

    public void setSegmentsRenderingCount(int segmentsCount) {
        this.shouldRenderingCount = segmentsCount;
    }

    public List<NodeData> getNodes() {
        return this.nodeList;
    }

    /**
     * Each two nodes be a segment, for example: <strong>{Node-0, Node-1, Node-2, Node-3} => {Segment-0{Node-0, Node-1}, Segment-1{Node-2, Node-3}}</strong>.<p>
     * <strong>Ignore isolated node.</strong>
     *
     * @param nodeList Cannot have any null-node, and at least have two nodeList, if not then pass this entity when rendering.
     */
    public void setNodes(@NotNull List<NodeData> nodeList) {
        if (nodeList.size() > BoxConfigs.getMaxSegmentNodeSize()) this.nodeList = nodeList.subList(0, BoxConfigs.getMaxSegmentNodeSize());
        else this.nodeList = nodeList;
        if (this._distance == null) this._distance = new ArrayList<>(this.nodeList.size());
    }

    public void addNode(@NotNull NodeData node) {
        if (this.nodeList == null) this.nodeList = new ArrayList<>();
        if (this.nodeList.size() > BoxConfigs.getMaxSegmentNodeSize()) return;
        this.nodeList.add(node);
        if (this._distance == null) this._distance = new ArrayList<>();
    }

    /**
     * Create a line-strip as {@link GL11#GL_LINE_STRIP}, and submit nodeList.<p>
     * Flat tangent, just a line-strip.
     *
     * @param points size must be larger than 2.
     */
    public SegmentEntity initLineStrip(@NotNull List<Vector2f> points, Color color, Color emissiveColor, float width, boolean closed) {
        if (points.size() < 2) return this;
        List<NodeData> lineStrip = new ArrayList<>(closed ? points.size() + 1 : points.size());
        NodeData lastNode = new NodeData(points.get(0).x, points.get(0).y, -1.0f, 0.0f, 1.0f, 0.0f);
        lastNode.setColor(color);
        lastNode.setEmissiveColor(emissiveColor);
        lastNode.setWidth(width);
        for (int i = 1; i < points.size(); i++) {
            lineStrip.add(lastNode);
            NodeData node = new NodeData(points.get(i).x, points.get(i).y, -1.0f, 0.0f, 1.0f, 0.0f);
            node.setColor(color);
            node.setEmissiveColor(emissiveColor);
            node.setWidth(width);
            lineStrip.add(node);
            lastNode = node;
        }
        if (closed) {
            lineStrip.add(lastNode);
            lineStrip.add(lineStrip.get(0));
        }
        this.setNodes(lineStrip);
        this.setNodeRefreshAllFromCurrentIndex();
        this.submitNodes();
        return this;
    }

    /**
     * Create a two-points line, and submit nodeList.<p>
     * Flat tangent, just a line.
     */
    public SegmentEntity initLine(@Nullable Vector2f offset, float length, Color color, Color emissiveColor, float width) {
        if (offset == null) offset = new Vector2f(0.0f, 0.0f);
        List<NodeData> line = new ArrayList<>(2);
        NodeData from = new NodeData(offset.x, offset.y, -1.0f, 0.0f, 1.0f, 0.0f);
        from.setColor(color);
        from.setEmissiveColor(emissiveColor);
        from.setWidth(width);
        NodeData to = new NodeData(offset.x + length, offset.y, -1.0f, 0.0f, 1.0f, 0.0f);
        to.setColor(color);
        to.setEmissiveColor(emissiveColor);
        to.setWidth(width);
        line.add(from);
        line.add(to);
        this.setNodes(line);
        this.setNodeRefreshAllFromCurrentIndex();
        this.submitNodes();
        return this;
    }

    /**
     * Rough fitting but more performance.<p>
     * Create a circle with three-points, and submit nodeList.<p>
     * Interpolation should be greater than zero.
     */
    public SegmentEntity initCircle(@Nullable Vector2f offset, float radius, Color color, Color emissiveColor, float width) {
        return this.initElliptic(offset, radius, radius, color, emissiveColor, width);
    }

    /**
     * Rough fitting but more performance.<p>
     * Create an elliptic with three-points, and submit nodeList.<p>
     * Interpolation should be greater than zero.
     */
    public SegmentEntity initElliptic(@Nullable Vector2f offset, float radiusW, float radiusH, Color color, Color emissiveColor, float width) {
        if (offset == null) offset = new Vector2f(0.0f, 0.0f);
        float heightFix = radiusH * _CIRCLE_FIX;
        List<NodeData> line = new ArrayList<>(3);
        NodeData left = new NodeData(offset.x - radiusW, offset.y, 0.0f, -heightFix, 0.0f, heightFix);
        left.setColor(color);
        left.setEmissiveColor(emissiveColor);
        left.setWidth(width);
        NodeData right = new NodeData(offset.x + radiusW, offset.y, 0.0f, heightFix, 0.0f, -heightFix);
        right.setColor(color);
        right.setEmissiveColor(emissiveColor);
        right.setWidth(width);
        line.add(left);
        line.add(right);
        line.add(right);
        line.add(left);
        this.setNodes(line);
        this.setNodeRefreshAllFromCurrentIndex();
        this.submitNodes();
        return this;
    }

    public void putShaderSegmentData() {
        GL13.glActiveTexture(GL13.GL_TEXTURE10);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[0]);
        GL13.glActiveTexture(GL13.GL_TEXTURE11);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[1]);
        GL13.glActiveTexture(GL13.GL_TEXTURE12);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._TBOTex[2]);
    }

    public int getInterpolation() {
        return this.interpolation;
    }

    /**
     * From zero.<p>
     * Will add points at geometry shader.
     */
    public void setInterpolation(short interpolation) {
        this.interpolation = (short) Math.max(Math.min(interpolation, BoxConfigs.getMaxCurveInterpolation()), 0);
    }

    public float[] getCurveState() {
        return this.state;
    }

    public float getTextureSpeed() {
        return this.state[0];
    }

    /**
     * It is working together with {@link CurveEntity#setTexturePixels(float)}.
     *
     * @param textureSpeed looks flowing forward when less than zero.
     */
    public void setTextureSpeed(float textureSpeed) {
        this.state[0] = textureSpeed;
    }

    public float getTexturePixels() {
        return this.state[1];
    }

    /**
     * @param texturePixels texture fill size.
     */
    public void setTexturePixels(float texturePixels) {
        this.state[1] = texturePixels;
    }

    public boolean isFlowWhenPaused() {
        return this.flowWhenPaused;
    }

    public void setFlowWhenPaused(boolean flow) {
        this.flowWhenPaused = flow;
    }

    public float getUVOffset() {
        return this.state[6];
    }

    public void setUVOffset(float offset) {
        this.state[6] = offset;
    }

    public float getFillStartAlpha() {
        return this.state[2];
    }

    public void setFillStartAlpha(float alpha) {
        this.state[2] = alpha;
    }

    public float getFillEndAlpha() {
        return this.state[3];
    }

    public void setFillEndAlpha(float alpha) {
        this.state[3] = alpha;
    }

    public float getFillStartFactor() {
        return this.state[4];
    }

    public void setFillStartFactor(float factor) {
        this.state[4] = factor;
    }

    public float getFillEndFactor() {
        return this.state[5];
    }

    public void setFillEndFactor(float factor) {
        this.state[5] = factor;
    }

    public @NotNull MaterialData getMaterialData() {
        if (this.hasDelete()) return new MaterialData();
        return this.material;
    }

    public void setMaterialData(@NotNull MaterialData material) {
        this.material = material == null ? new MaterialData() : material;
    }

    public FloatBuffer pickDataPackage_vec4() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(20);
        buffer.put(this.material.getState());
        buffer.put(11, this.getGlobalTimerAlpha());
        final float iPO = this.interpolation + 1;
        buffer.put(12, iPO);
        buffer.put(13, this.state[1]);
        buffer.put(16, this.state[2]);
        buffer.put(17, this.state[3]);
        buffer.put(18, this.state[4]);
        buffer.put(19, this.state[5]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    @Deprecated
    public byte getDrawMode() {return BoxEnum.MODE_COLOR;}

    @Deprecated
    public void setDrawMode(byte drawMode) {}
}
