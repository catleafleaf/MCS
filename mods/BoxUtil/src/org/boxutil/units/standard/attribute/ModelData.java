package org.boxutil.units.standard.attribute;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.units.builtin.legacy.array.TriIndex;
import org.boxutil.util.CommonUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

public class ModelData {
    protected final String _rawID;
    protected final int[] _objectData = new int[3];
    // TBN, vn, vt
    protected final int[] _verticesDataTBO = new int[2];
    protected final int[] _verticesDataTBOTex = new int[2];
    protected final int _dataType;
    protected final SpriteAPI[] _textures = new SpriteAPI[4];
    protected boolean isValid = false;

    public ModelData(String rawID, ModelData entity, SpriteAPI diffuse, SpriteAPI normal, SpriteAPI ao, SpriteAPI emissive) {
        this._rawID = rawID;
        this._dataType = entity.getDataType();
        this._objectData[0] = entity.getVAO();
        this._objectData[1] = entity.getVBO();
        this._objectData[2] = entity.getPatchCount();
        this._verticesDataTBO[0] = entity.getTBNDataTBO()[0];
        this._verticesDataTBO[1] = entity.getTBNDataTBO()[1];
        this._verticesDataTBOTex[0] = entity.getTBNDataTBOTex()[0];
        this._verticesDataTBOTex[1] = entity.getTBNDataTBOTex()[1];
        if (diffuse == null) this._textures[0] = BoxDatabase.BUtil_ONE; else this._textures[0] = diffuse;
        if (normal == null) this._textures[1] = BoxDatabase.BUtil_Z; else this._textures[1] = normal;
        if (ao == null) this._textures[2] = BoxDatabase.BUtil_ONE; else this._textures[2] = ao;
        if (emissive == null) this._textures[3] = BoxDatabase.BUtil_NONE; else this._textures[3] = emissive;
        if (this._objectData[0] > 0 && this._objectData[1] > 0 && this._objectData[2] > 0) isValid = true;
    }

    public ModelData(String rawID, List<Vector3f> vertex, List<Vector3f> normal, List<Vector2f> uv, List<TriIndex> patchIndex, SpriteAPI diffuse, SpriteAPI normalMap, SpriteAPI ao, SpriteAPI emissive, int type, int tbnType) {
        this._rawID = rawID;
        this._dataType = type;
        this._objectData[2] = patchIndex.size() * 3;

        if (diffuse == null) this._textures[0] = BoxDatabase.BUtil_ONE; else this._textures[0] = diffuse;
        if (normalMap == null) this._textures[1] = BoxDatabase.BUtil_Z; else this._textures[1] = normalMap;
        if (ao == null) this._textures[2] = BoxDatabase.BUtil_ONE; else this._textures[2] = ao;
        if (emissive == null) this._textures[3] = BoxDatabase.BUtil_NONE; else this._textures[3] = emissive;

        if (!BoxConfigs.isTBOSupported()) return;

        final boolean normalize = type == GL11.GL_BYTE;
        final int s = type == GL11.GL_BYTE ? BoxDatabase.BYTE_SIZE : (type == GL30.GL_HALF_FLOAT ? BoxDatabase.HALF_FLOAT_SIZE : BoxDatabase.FLOAT_SIZE);
        final int s2 = tbnType == GL11.GL_BYTE ? BoxDatabase.BYTE_SIZE : (tbnType == GL30.GL_HALF_FLOAT ? BoxDatabase.HALF_FLOAT_SIZE : BoxDatabase.FLOAT_SIZE);
        final int patchVertexSize = 8 * s;
        final int[] tbnFormat = new int[]{GL30.GL_RGBA16F, GL30.GL_RG16F};
        if (tbnType == GL11.GL_BYTE) {
            tbnFormat[0] = GL11.GL_RGBA8;
            tbnFormat[1] = GL30.GL_RG8;
        }
        if (tbnType == GL11.GL_FLOAT) {
            tbnFormat[0] = GL30.GL_RGBA32F;
            tbnFormat[1] = GL30.GL_RG32F;
        }

        ByteBuffer vertexBuffer = BufferUtils.createByteBuffer(patchIndex.size() * 24 * s);
        ByteBuffer tbnBuffer_A = BufferUtils.createByteBuffer(patchIndex.size() * 4 * s2);
        ByteBuffer tbnBuffer_B = BufferUtils.createByteBuffer(patchIndex.size() * 2 * s2);

        int patchVertexIndex, patchNormalIndex, patchUVIndex, patchBufferIndex, vertexBufferIndex;
        Vector3f[] patchVertex = new Vector3f[3];
        Vector3f patchNormal;
        Vector2f[] patchUV = new Vector2f[3];
        float[][] TBNData = new float[2][4];
        Vector3f[] TBRaw;
        for (int i = 0; i < patchIndex.size(); i++) {
            TriIndex patch = patchIndex.get(i);
            vertexBufferIndex = i * 24;
            for (byte j = 0; j < 3; j++) {
                patchVertexIndex = patch.getIndex()[j].x;
                patchNormalIndex = patch.getIndex()[j].y;
                patchUVIndex = patch.getIndex()[j].z;

                patchVertex[j] = vertex.get(patchVertexIndex);
                patchNormal = normal.get(patchNormalIndex);
                patchUV[j] = uv.get(patchUVIndex);

                if (type == GL11.GL_BYTE) {
                    vertexBuffer.put(vertexBufferIndex, CommonUtil.normalizedFloatToByte(patchVertex[j].x));
                    vertexBuffer.put(vertexBufferIndex + 1, CommonUtil.normalizedFloatToByte(patchVertex[j].y));
                    vertexBuffer.put(vertexBufferIndex + 2, CommonUtil.normalizedFloatToByte(patchVertex[j].z));

                    vertexBuffer.put(vertexBufferIndex + 3, CommonUtil.normalizedFloatToByte(patchNormal.x));
                    vertexBuffer.put(vertexBufferIndex + 4, CommonUtil.normalizedFloatToByte(patchNormal.y));
                    vertexBuffer.put(vertexBufferIndex + 5, CommonUtil.normalizedFloatToByte(patchNormal.z));

                    vertexBuffer.put(vertexBufferIndex + 6, CommonUtil.normalizedFloatToByte(patchUV[j].x));
                    vertexBuffer.put(vertexBufferIndex + 7, CommonUtil.normalizedFloatToByte(patchUV[j].y));
                } else if (type == GL30.GL_HALF_FLOAT) {
                    vertexBuffer.asShortBuffer().put(vertexBufferIndex, CommonUtil.float16ToShort(patchVertex[j].x));
                    vertexBuffer.asShortBuffer().put(vertexBufferIndex + 1, CommonUtil.float16ToShort(patchVertex[j].y));
                    vertexBuffer.asShortBuffer().put(vertexBufferIndex + 2, CommonUtil.float16ToShort(patchVertex[j].z));

                    vertexBuffer.asShortBuffer().put(vertexBufferIndex + 3, CommonUtil.float16ToShort(patchNormal.x));
                    vertexBuffer.asShortBuffer().put(vertexBufferIndex + 4, CommonUtil.float16ToShort(patchNormal.y));
                    vertexBuffer.asShortBuffer().put(vertexBufferIndex + 5, CommonUtil.float16ToShort(patchNormal.z));

                    vertexBuffer.asShortBuffer().put(vertexBufferIndex + 6, CommonUtil.float16ToShort(patchUV[j].x));
                    vertexBuffer.asShortBuffer().put(vertexBufferIndex + 7, CommonUtil.float16ToShort(patchUV[j].y));
                } else {
                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex, patchVertex[j].x);
                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex + 1, patchVertex[j].y);
                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex + 2, patchVertex[j].z);

                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex + 3, patchNormal.x);
                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex + 4, patchNormal.y);
                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex + 5, patchNormal.z);

                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex + 6, patchUV[j].x);
                    vertexBuffer.asFloatBuffer().put(vertexBufferIndex + 7, patchUV[j].y);
                }
                vertexBufferIndex += 8;
            }

            TBRaw = CommonUtil.tangentMaker(patchVertex[0], patchVertex[1], patchVertex[2], patchUV[0], patchUV[1], patchUV[2]);
            TBNData[0][0] = TBRaw[0].x;
            TBNData[0][1] = TBRaw[0].y;
            TBNData[0][2] = TBRaw[0].z;
            TBNData[0][3] = TBRaw[1].x;

            TBNData[1][0] = TBRaw[1].y;
            TBNData[1][1] = TBRaw[1].z;

            patchBufferIndex = i * 4;
            for (byte j = 0; j < 4; j++) {
                if (tbnType == GL11.GL_BYTE) {
                    tbnBuffer_A.put(patchBufferIndex + j, CommonUtil.normalizedFloatToByte(TBNData[0][j]));
                } else if (tbnType == GL30.GL_HALF_FLOAT) {
                    tbnBuffer_A.asShortBuffer().put(patchBufferIndex + j, CommonUtil.float16ToShort(TBNData[0][j]));
                } else {
                    tbnBuffer_A.asFloatBuffer().put(patchBufferIndex + j, TBNData[0][j]);
                }
            }
            patchBufferIndex = i * 2;
            for (byte j = 0; j < 2; j++) {
                if (tbnType == GL11.GL_BYTE) {
                    tbnBuffer_B.put(patchBufferIndex + j, CommonUtil.normalizedFloatToByte(TBNData[1][j]));
                } else if (tbnType == GL30.GL_HALF_FLOAT) {
                    tbnBuffer_B.asShortBuffer().put(patchBufferIndex + j, CommonUtil.float16ToShort(TBNData[1][j]));
                } else {
                    tbnBuffer_B.asFloatBuffer().put(patchBufferIndex + j, TBNData[1][j]);
                }
            }
        }

        vertexBuffer.position(0);
        vertexBuffer.limit(vertexBuffer.capacity());

        tbnBuffer_A.position(0);
        tbnBuffer_A.limit(tbnBuffer_A.capacity());
        tbnBuffer_B.position(0);
        tbnBuffer_B.limit(tbnBuffer_B.capacity());

        IntBuffer tboObject = BufferUtils.createIntBuffer(2);
        IntBuffer tboTex = BufferUtils.createIntBuffer(2);
        GL15.glGenBuffers(tboObject);
        GL11.glGenTextures(tboTex);

        this._verticesDataTBO[0] = tboObject.get(0);
        this._verticesDataTBOTex[0] = tboTex.get(0);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._verticesDataTBO[0]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, tbnBuffer_A, GL15.GL_STATIC_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._verticesDataTBOTex[0]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, tbnFormat[0], this._verticesDataTBO[0]);
        this._verticesDataTBO[1] = tboObject.get(1);
        this._verticesDataTBOTex[1] = tboTex.get(1);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, this._verticesDataTBO[1]);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, tbnBuffer_B, GL15.GL_STATIC_DRAW);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._verticesDataTBOTex[1]);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, tbnFormat[1], this._verticesDataTBO[1]);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);

        this._objectData[0] = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(this._objectData[0]);

        this._objectData[1] = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this._objectData[1]);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, type, normalize, patchVertexSize, 0); // v
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 3, type, normalize, patchVertexSize, s * 3); // vn
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(2, 2, type, normalize, patchVertexSize, s * 6); // vt
        GL20.glEnableVertexAttribArray(2);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        if (this._objectData[0] > 0 && this._objectData[1] > 0 && this._objectData[2] > 0) isValid = true;
    }

    public String getRawID() {
        return this._rawID;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void putTBNShaderData() {
        GL13.glActiveTexture(GL13.GL_TEXTURE8);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._verticesDataTBOTex[0]);
        GL13.glActiveTexture(GL13.GL_TEXTURE9);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this._verticesDataTBOTex[1]);
    }

    public int getDataType() {
        return this._dataType;
    }

    /**
     * You shouldn't edit it.
     */
    public int getVAO() {
        return this._objectData[0];
    }

    /**
     * You shouldn't edit it.
     */
    public int getVBO() {
        return this._objectData[1];
    }

    public int getPatchCount() {
        return this._objectData[2];
    }

    public int[] getTBNDataTBO() {
        return this._verticesDataTBO;
    }

    public int[] getTBNDataTBOTex() {
        return this._verticesDataTBOTex;
    }

    public SpriteAPI getDiffuse() {
        return this._textures[0];
    }

    public void setDiffuse(SpriteAPI sprite) {
        this._textures[0] = sprite;
    }

    public SpriteAPI getNormal() {
        return _textures[1];
    }

    public void setNormal(SpriteAPI sprite) {
        this._textures[1] = sprite;
    }

    public SpriteAPI getAO() {
        return _textures[2];
    }

    public void setAO(SpriteAPI sprite) {
        this._textures[2] = sprite;
    }

    public SpriteAPI getEmissive() {
        return _textures[3];
    }

    public void setEmissive(SpriteAPI sprite) {
        this._textures[3] = sprite;
    }
}
