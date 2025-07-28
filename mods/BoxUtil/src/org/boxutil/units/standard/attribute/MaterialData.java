package org.boxutil.units.standard.attribute;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.boxutil.define.BoxEnum;
import org.boxutil.util.CommonUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.boxutil.define.BoxDatabase;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;

// The material is only 2D-Texture supported.
public class MaterialData {
    protected final SpriteAPI[] textures = new SpriteAPI[]{BoxDatabase.BUtil_ONE, BoxDatabase.BUtil_Z, BoxDatabase.BUtil_ONE, BoxDatabase.BUtil_NONE};
    protected final int[] glTex = new int[]{this.textures[0].getTextureId(), this.textures[1].getTextureId(), this.textures[2].getTextureId(), this.textures[3].getTextureId()};
    protected byte cullFace = 0;
    protected final float[] state = new float[]{BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ONE, 0.0f, BoxEnum.ONE};
    protected final boolean[] stateB = new boolean[]{true, false}; // additionEmissive, ignoreIllumination

    public MaterialData() {}

    public void clearTextures() {
        this.textures[0] = BoxDatabase.BUtil_ONE;
        this.glTex[0] = this.textures[0].getTextureId();
        this.textures[1] = BoxDatabase.BUtil_Z;
        this.glTex[1] = this.textures[1].getTextureId();
        this.textures[2] = BoxDatabase.BUtil_ONE;
        this.glTex[2] = this.textures[2].getTextureId();
        this.textures[3] = BoxDatabase.BUtil_NONE;
        this.glTex[3] = this.textures[3].getTextureId();
    }

    public void syncTextures(ModelData entity) {
        this.textures[0] = entity.getDiffuse();
        this.glTex[0] = this.textures[0].getTextureId();
        this.textures[1] = entity.getNormal();
        this.glTex[1] = this.textures[1].getTextureId();
        this.textures[2] = entity.getAO();
        this.glTex[2] = this.textures[2].getTextureId();
        this.textures[3] = entity.getEmissive();
        this.glTex[3] = this.textures[3].getTextureId();
    }

    /**
     * Without textures.
     */
    public void reset() {
        this.cullFace = 0;
        this.state[0] = BoxEnum.ONE;
        this.state[1] = BoxEnum.ONE;
        this.state[2] = BoxEnum.ONE;
        this.state[3] = BoxEnum.ONE;
        this.state[4] = BoxEnum.ONE;
        this.state[5] = BoxEnum.ONE;
        this.state[6] = BoxEnum.ONE;
        this.state[7] = BoxEnum.ONE;
        this.state[8] = BoxEnum.ONE;
        this.state[9] = 0.0f;
        this.state[10] = BoxEnum.ONE;
    }

    public void putShaderTexture() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTex[0]);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTex[1]);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTex[2]);
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTex[3]);
    }

    public SpriteAPI[] getTextures() {
        return this.textures;
    }

    public int[] getTexturesID() {
        return this.glTex;
    }

    public SpriteAPI getDiffuse() {
        return textures[0];
    }

    public int getDiffuseID() {
        return glTex[0];
    }

    /**
     * Set null for default.
     */
    public void setDiffuse(@Nullable SpriteAPI diffuse) {
        if (diffuse == null) {
            this.textures[0] = BoxDatabase.BUtil_ONE;
            this.glTex[0] = this.textures[0].getTextureId();
        } else {
            this.textures[0] = diffuse;
            this.glTex[0] = diffuse.getTextureId();
        }
    }

    public void setDiffuse(int diffuse) {
        this.glTex[0] = Math.max(diffuse, 0);
    }

    public SpriteAPI getNormal() {
        return textures[1];
    }

    public int getNormalID() {
        return glTex[1];
    }

    /**
     * Set null for default.
     */
    public void setNormal(@Nullable SpriteAPI normal) {
        if (normal == null) {
            this.textures[1] = BoxDatabase.BUtil_Z;
            this.glTex[1] = this.textures[1].getTextureId();
        } else {
            this.textures[1] = normal;
            this.glTex[1] = normal.getTextureId();
        }
    }

    public void setNormal(int normal) {
        this.glTex[1] = Math.max(normal, 0);
    }

    @Deprecated
    public SpriteAPI getAO() {
        return textures[2];
    }

    @Deprecated
    public int getAOID() {
        return glTex[2];
    }

    /**
     * Set null for default.
     */
    @Deprecated
    public void setAO(@Nullable SpriteAPI ao) {
        if (ao == null) {
            this.textures[2] = BoxDatabase.BUtil_ONE;
            this.glTex[2] = this.textures[2].getTextureId();
        } else {
            this.textures[2] = ao;
            this.glTex[2] = ao.getTextureId();
        }
    }

    @Deprecated
    public void setAO(int ao) {
        this.glTex[2] = Math.max(ao, 0);
    }

    public SpriteAPI getEmissive() {
        return textures[3];
    }

    public int getEmissiveID() {
        return glTex[3];
    }

    /**
     * Set null for default.
     */
    public void setEmissive(@Nullable SpriteAPI emissive) {
        if (emissive == null) {
            this.textures[3] = BoxDatabase.BUtil_NONE;
            this.glTex[3] = this.textures[3].getTextureId();
        } else {
            this.textures[3] = emissive;
            this.glTex[3] = emissive.getTextureId();
        }
    }

    public void setEmissive(int emissive) {
        this.glTex[3] = Math.max(emissive, 0);
    }

    public void setDisableCullFace() {
        this.cullFace = 3;
    }

    public byte getCullFace() {
        return this.cullFace;
    }

    public void setCullBack() {
        this.cullFace = 0;
    }

    public void setCullFront() {
        this.cullFace = 1;
    }

    public void setCullFrontAndBack() {
        this.cullFace = 2;
    }

    public float[] getColorArray() {
        return new float[]{this.state[0], this.state[1], this.state[2], this.state[3]};
    }

    public Color getColorC() {
        return CommonUtil.toCommonColor(this.getColor());
    }

    public Vector4f getColor() {
        return new Vector4f(this.state[0], this.state[1], this.state[2], this.state[3]);
    }

    public float getColorAlpha() {
        return this.state[3];
    }

    public int getColorAlphaI() {
        return Math.max(Math.min(Math.round(this.state[3] * 255.0f), 255), 0);
    }

    public void setColor(@NotNull Vector4f color) {
        this.state[0] = color.x;
        this.state[1] = color.y;
        this.state[2] = color.z;
        this.state[3] = color.w;
    }

    public void setColor(float r, float g, float b, float a) {
        this.state[0] = r;
        this.state[1] = g;
        this.state[2] = b;
        this.state[3] = a;
    }

    public void setColor(Color color) {
        this.state[0] = color.getRed() / 255.0f;
        this.state[1] = color.getGreen() / 255.0f;
        this.state[2] = color.getBlue() / 255.0f;
        this.state[3] = color.getAlpha() / 255.0f;
    }

    public void setColorAlpha(float alpha) {
        this.state[3] = alpha;
    }

    public void setColorAlphaI(int alpha) {
        this.state[3] = alpha / 255.0f;
    }

    public float[] getEmissiveColorArray() {
        return new float[]{this.state[4], this.state[5], this.state[6], this.state[7]};
    }

    public Color getEmissiveColorC() {
        return CommonUtil.toCommonColor(this.getEmissiveColor());
    }

    public Vector4f getEmissiveColor() {
        return new Vector4f(this.state[4], this.state[5], this.state[6], this.state[7]);
    }

    public float getEmissiveColorAlpha() {
        return this.state[7];
    }

    public int getEmissiveColorAlphaI() {
        return Math.max(Math.min(Math.round(this.state[7] * 255.0f), 255), 0);
    }

    public void setEmissiveColor(@NotNull Vector4f color) {
        this.state[4] = color.x;
        this.state[5] = color.y;
        this.state[6] = color.z;
        this.state[7] = color.w;
    }

    public void setEmissiveColor(float r, float g, float b, float a) {
        this.state[4] = r;
        this.state[5] = g;
        this.state[6] = b;
        this.state[7] = a;
    }

    public void setEmissiveColor(Color color) {
        this.state[4] = color.getRed() / 255.0f;
        this.state[5] = color.getGreen() / 255.0f;
        this.state[6] = color.getBlue() / 255.0f;
        this.state[7] = color.getAlpha() / 255.0f;
    }

    public void setEmissiveColorAlpha(float alpha) {
        this.state[7] = alpha;
    }

    public void setEmissiveColorAlphaI(int alpha) {
        this.state[7] = alpha / 255.0f;
    }

    public float[] getEmissiveStateArray() {
        return new float[]{this.state[8], this.state[9], this.state[10]};
    }

    public Vector3f getEmissiveState() {
        return new Vector3f(this.state[8], this.state[9], this.state[10]);
    }

    public void setEmissiveState(float alphaToEmissive, float colorToEmissive, float glowPower) {
        this.state[8] = alphaToEmissive;
        this.state[9] = colorToEmissive;
        this.state[10] = glowPower;
    }

    public float getAlphaToEmissive() {
        return this.state[8];
    }

    /**
     * @param alphaToEmissive mix level, value: 0.0 to 1.0
     */
    public void setAlphaToEmissive(float alphaToEmissive) {
        this.state[8] = alphaToEmissive;
    }

    public void setAlphaToEmissiveDefault() {
        this.state[8] = BoxEnum.ONE;
    }

    /**
     * Use alpha mix when false.
     * Vanilla rendering: usually is addition.
     */
    public boolean isAdditionEmissive() {
        return this.stateB[0];
    }

    public void setAdditionEmissive(boolean addition) {
        this.stateB[0] = addition;
    }

    public boolean isIgnoreIllumination() {
        return this.stateB[1];
    }

    public void setIgnoreIllumination(boolean ignore) {
        this.stateB[1] = ignore;
    }

    public float isColorToEmissive() {
        return this.state[9];
    }

    /**
     * @param colorToEmissive mix level, value: 0.0 to 1.0
     */
    public void setColorToEmissive(float colorToEmissive) {
        this.state[9] = colorToEmissive;
    }

    public void setColorToEmissiveDefault() {
        this.state[9] = 0.0f;
    }

    public float getGlowPower() {
        return this.state[10];
    }

    /**
     * @param glowPower Decided by final of emissive level; 0 to 1.0f
     */
    public void setGlowPower(float glowPower) {
        this.state[10] = glowPower;
    }

    public void setGlowPowerDefault() {
        this.state[10] = BoxEnum.ONE;
    }

    public float[] getState() {
        return this.state;
    }
}
