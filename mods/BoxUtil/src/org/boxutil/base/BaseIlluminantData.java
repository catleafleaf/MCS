package org.boxutil.base;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import org.boxutil.base.api.DirectDrawEntity;
import org.boxutil.base.api.IlluminantAPI;
import org.boxutil.util.CommonUtil;
import org.boxutil.util.RenderingUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;

/**
 * WIP
 */
public class BaseIlluminantData extends BaseInstanceRenderData implements IlluminantAPI, DirectDrawEntity {
    protected final static byte _FULL = 1;
    protected final float[] state = new float[]{_FULL, _FULL, _FULL, _FULL};
    protected byte stateBit = 0; // 0bXX: attenuationMode, 0bX00: visible,

    public float[] getColorArray() {
        return new float[]{this.state[0], this.state[1], this.state[2], this.state[3]};
    }

    public Color getColorC() {
        return CommonUtil.toCommonColor(this.getColor());
    }

    public Vector4f getColor() {
        return new Vector4f(this.state[0], this.state[1], this.state[2], this.state[3]);
    }

    public void setColor(@NotNull Vector4f color) {
        this.state[0] = color.x;
        this.state[1] = color.y;
        this.state[2] = color.z;
        this.state[3] = color.w;
    }

    /**
     * @param a strength of light.
     */
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
        this.state[3] = color.getAlpha();
    }

    public float getStrength() {
        return this.state[3];
    }

    public void setStrength(float strength) {
        this.state[3] = strength;
    }

    public boolean isVisible() {
        return (this.stateBit & 0b100) == 0b100;
    }

    public void setVisible(boolean visible) {
        if (visible) this.stateBit |= 0b100; else this.stateBit &= 0b011;
    }

    public void setDefaultAttenuation() {
        this.stateBit &= 0b100;
        this.stateBit |= 0b10;
    }

    public boolean isNoneAttenuation() {
        return (this.stateBit & 0b11) == 0b00;
    }

    public void setNoneAttenuation() {
        this.stateBit &= 0b100;
    }

    public boolean isLinearAttenuation() {
        return (this.stateBit & 0b11) == 0b01;
    }

    public void setLinearAttenuation() {
        this.stateBit &= 0b100;
        this.stateBit |= 0b01;
    }

    public boolean isSquareAttenuation() {
        return (this.stateBit & 0b11) == 0b10;
    }

    public void setSquareAttenuation() {
        this.setDefaultAttenuation();
    }

    public boolean isSqrtAttenuation() {
        return (this.stateBit & 0b11) == 0b11;
    }

    public void setSqrtAttenuation() {
        this.stateBit |= 0b11;
    }

    @Deprecated
    public int getBlendColorSRC() {
        return GL11.GL_SRC_ALPHA;
    }

    @Deprecated
    public int getBlendColorDST() {
        return GL11.GL_ONE_MINUS_SRC_ALPHA;
    }

    @Deprecated
    public int getBlendAlphaSRC() {
        return GL11.GL_SRC_ALPHA;
    }

    @Deprecated
    public int getBlendAlphaDST() {
        return GL11.GL_ONE_MINUS_SRC_ALPHA;
    }

    @Deprecated
    public int getBlendEquation() {
        return GL14.GL_FUNC_ADD;
    }

    @Deprecated
    public byte getBlendState() {
        return 0;
    }

    @Deprecated
    public void setBlendFunc(int srcFactor, int dstFactor) {}

    @Deprecated
    public void setBlendFuncSeparate(int srcColorFactor, int dstColorFactor, int srcAlphaFactor, int dstAlphaFactor) {}

    @Deprecated
    public void setBlendEquation(int mode) {}

    @Deprecated
    public void setAdditiveBlend() {}

    @Deprecated
    public void setNormalBlend() {}

    @Deprecated
    public void setDisableBlend() {}

    @Deprecated
    public Object getLayer() {
        return null;
    }

    @Deprecated
    public CombatEngineLayers getCombatLayer() {
        return RenderingUtil.getHighestCombatLayer();
    }

    @Deprecated
    public CampaignEngineLayers getCampaignLayer() {
        return RenderingUtil.getHighestCampaignLayer();
    }

    @Deprecated
    public void setLayer(Object layer) {}

    @Deprecated
    public void setDefaultCombatLayer() {}

    @Deprecated
    public void setDefaultCampaignLayer() {}
}
