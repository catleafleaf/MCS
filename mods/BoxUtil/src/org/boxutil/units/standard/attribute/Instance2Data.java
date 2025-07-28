package org.boxutil.units.standard.attribute;

import org.boxutil.base.api.InstanceRenderAPI;
import org.jetbrains.annotations.NotNull;
import org.boxutil.base.api.InstanceDataAPI;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;

/**
 * <strong>Priority use this if renders more than one entity.</strong><p>
 * Timer value used 32bit-float in GL.<p>
 * Color used unsigned byte in GL.<p>
 * Other data is changeable by user.
 */
public class Instance2Data implements InstanceDataAPI {
    // vec2(location), float(facing), vec2(scale), vec2(offset), float(turnRate), vec2(scaleRate), vec4(timer), rawAlpha
    protected final float[] state = new float[15];
    // vec4(lowColor), vec4(highColor), vec4(lowEmissive), vec4(highEmissive)
    protected final byte[] colorState = new byte[]{BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR};

    public Instance2Data() {
        this.state[10] = this.state[11] = this.state[12] = this.state[13] = -512.0f;
        this.state[14] = 21.0f;
    }

    public Instance2Data(Instance2Data data) {
        this();
        System.arraycopy(data.state, 0, this.state, 0, this.state.length);
        System.arraycopy(data.colorState, 0, this.colorState, 0, this.colorState.length);
    }

    /**
     * For built-in pipeline.<p>
     * {<p>
     * 0, 0, loc.y, loc.x<p>
     * 0, 0, 0,     alpha<p>
     * }
     */
    public float[] pickFinal_vec4() {
        return new float[]{0.0f, 0.0f, this.state[1], this.state[0]};
    }

    /**
     * For built-in pipeline.<p>
     * {<p>
     * rawAlpha, facing, loc.x, loc.y<p>
     * scale.x, scale.y<p>
     * }
     */
    public float[][] pickFixedFinal_vec4() {
        return new float[][]{
                new float[]{this.state[14], this.state[2], this.state[0], this.state[1],},
                new float[]{this.state[3], this.state[4],},
        };
    }

    /**
     * For built-in pipeline.<p>
     * {<p>
     * total, fadeIn, full, fadeOut<p>
     * }
     */
    public float[] pickTimer_vec4() {
        return new float[]{this.state[10], this.state[11], this.state[12], this.state[13]};
    }

    /**
     * For built-in pipeline.
     * {<p>
     * scale.x, scale.y, facing,   turnRate<p>
     * dLoc.x,  dLoc.y,  dScale.x, dScale.y<p>
     * }
     */
    public float[][] pickState_vec4() {
        return new float[][]{
                new float[]{this.state[3], this.state[4], this.state[2], this.state[7],},
                new float[]{this.state[5], this.state[6], this.state[8], this.state[9],},
        };
    }

    /**
     * For built-in pipeline.
     * {<p>
     * X: clR, chR, elR, ehR<p>
     * Y: clG, chG, elG, ehG<p>
     * Z: clB, chB, elB, ehB<p>
     * W: clA, chA, elA, ehA<p>
     * }
     */
    public byte[][] pickColor_vec4x4() {
        return new byte[][]{
                new byte[]{this.colorState[0], this.colorState[1], this.colorState[2], this.colorState[3],},
                new byte[]{this.colorState[4], this.colorState[5], this.colorState[6], this.colorState[7],},
                new byte[]{this.colorState[8], this.colorState[9], this.colorState[10], this.colorState[11],},
                new byte[]{this.colorState[12], this.colorState[13], this.colorState[14], this.colorState[15],},
        };
    }

    /**
     * For built-in pipeline.
     * {<p>
     * X: chR, ehR<p>
     * Y: chG, ehG<p>
     * Z: chB, ehB<p>
     * W: chA, ehA<p>
     * }
     */
    public byte[][] pickFixedColor_vec4x2() {
        return new byte[][]{
                new byte[]{this.colorState[4], this.colorState[5], this.colorState[6], this.colorState[7],},
                new byte[]{this.colorState[12], this.colorState[13], this.colorState[14], this.colorState[15],},
        };
    }

    public float[] getState() {
        return this.state;
    }

    public byte[] getColorState() {
        return this.colorState;
    }

    /**
     * Transform data of base.<p>
     * Scale use factor, it's not a target size.<p>
     * Base value.
     */
    public float[] getBaseState() {
        return new float[]{this.state[0], this.state[1], this.state[2], this.state[3], this.state[4]};
    }

    public float[] getLocationArray() {
        return new float[]{this.state[0], this.state[1]};
    }

    public Vector2f getLocation() {
        return new Vector2f(this.state[0], this.state[1]);
    }

    public void setLocation(@NotNull Vector2f location) {
        this.setLocation(location.x, location.y);
    }

    public void setLocation(float x, float y) {
        this.state[0] = x;
        this.state[1] = y;
    }

    public float[] getLocationArrayDirect() {
        return new float[]{this.state[0], this.state[1]};
    }

    public Vector2f getLocationDirect() {
        return new Vector2f(this.state[0], this.state[1]);
    }

    public void setLocationDirect(@NotNull Vector2f location) {
        this.setLocationDirect(location.x, location.y);
    }

    public void setLocationDirect(float x, float y) {
        this.state[0] = x;
        this.state[1] = y;
    }

    public float getFacing() {
        return this.state[2];
    }

    public void setFacing(float facing) {
        this.state[2] = facing;
    }

    public float[] getScaleArray() {
        return new float[]{this.state[3], this.state[4]};
    }

    public Vector2f getScale() {
        return new Vector2f(this.state[3], this.state[4]);
    }

    public void setScale(@NotNull Vector2f rotate) {
        this.setScale(rotate.x, rotate.y);
    }

    public void setScale(float x, float y) {
        this.state[3] = x;
        this.state[4] = y;
    }

    public void setScaleAll(float factor) {
        this.setScale(factor, factor);
    }

    /**
     * Transform data per second.<p>
     * Fixed values.
     */
    public float[] getDynamicState() {
        return new float[]{this.state[5], this.state[6], this.state[7], this.state[8], this.state[9]};
    }

    public float[] getVelocityArray() {
        return new float[]{this.state[5], this.state[6]};
    }

    public Vector2f getVelocity() {
        return new Vector2f(this.state[5], this.state[6]);
    }

    public void setVelocity(@NotNull Vector2f location) {
        this.setVelocity(location.x, location.y);
    }

    public void setVelocity(float x, float y) {
        this.state[5] = x;
        this.state[6] = y;
    }

    public void setVelocityDirect(@NotNull Vector2f location) {
        this.setVelocityDirect(location.x, location.y);
    }

    public void setVelocityDirect(float x, float y) {
        this.state[5] = x;
        this.state[6] = y;
    }

    public float getTurnRate() {
        return this.state[7];
    }

    public void setTurnRate(float turnRate) {
        this.state[7] = turnRate;
    }

    public float[] getScaleRateArray() {
        return new float[]{this.state[8], this.state[9]};
    }

    public Vector2f getScaleRate() {
        return new Vector2f(this.state[8], this.state[9]);
    }

    public void setScaleRate(@NotNull Vector2f rotate) {
        this.setScaleRate(rotate.x, rotate.y);
    }

    public void setScaleRate(float x, float y) {
        this.state[8] = x;
        this.state[9] = y;
    }

    public void setScaleRateAll(float factor) {
        this.setScaleRate(factor, factor);
    }

    /**
     * @return high color.
     */
    public byte[] getColorArray() {
        return new byte[]{this.colorState[4], this.colorState[5], this.colorState[6], this.colorState[7]};
    }

    public byte[] getLowColorArray() {
        return new byte[]{this.colorState[0], this.colorState[1], this.colorState[2], this.colorState[3]};
    }

    public byte[] getHighColorArray() {
        return this.getColorArray();
    }

    /**
     * @return high color.
     */
    public Color getColor() {
        return new Color(
                this.colorState[4] & 0xFF,
                this.colorState[5] & 0xFF,
                this.colorState[6] & 0xFF,
                this.colorState[7] & 0xFF);
    }

    public Color getLowColor() {
        return new Color(
                this.colorState[0] & 0xFF,
                this.colorState[1] & 0xFF,
                this.colorState[2] & 0xFF,
                this.colorState[3] & 0xFF);
    }

    public Color getHighColor() {
        return this.getColor();
    }

    /**
     * @return high color.
     */
    public Vector4f getColor4f() {
        return new Vector4f(
                (this.colorState[4] & 0xFF) / 255.0f,
                (this.colorState[5] & 0xFF) / 255.0f,
                (this.colorState[6] & 0xFF) / 255.0f,
                (this.colorState[7] & 0xFF) / 255.0f);
    }

    public Vector4f getLowColor4f() {
        return new Vector4f(
                (this.colorState[0] & 0xFF) / 255.0f,
                (this.colorState[1] & 0xFF) / 255.0f,
                (this.colorState[2] & 0xFF) / 255.0f,
                (this.colorState[3] & 0xFF) / 255.0f);
    }

    public Vector4f getHighColor4f() {
        return this.getColor4f();
    }

    /**
     * @return high color.
     */
    public byte getColorAlpha() {
        return this.colorState[7];
    }

    public byte getLowColorAlpha() {
        return this.colorState[3];
    }

    public byte getHighColorAlpha() {
        return this.getColorAlpha();
    }

    /**
     * @return high color.
     */
    public int getColorAlphaI() {
        return this.getColorAlpha() & 0xFF;
    }

    public int getLowColorAlphaI() {
        return this.getLowColorAlpha() & 0xFF;
    }

    public int getHighColorAlphaI() {
        return this.getColorAlphaI();
    }

    /**
     * @return high color.
     */
    public float getColorAlphaF() {
        return this.getColorAlphaI() / 255.0f;
    }

    public float getLowColorAlphaF() {
        return this.getLowColorAlphaI() / 255.0f;
    }

    public float getHighColorAlphaF() {
        return this.getColorAlphaF();
    }

    public void setLowColor(byte r, byte g, byte b, byte a) {
        this.colorState[0] = r;
        this.colorState[1] = g;
        this.colorState[2] = b;
        this.colorState[3] = a;
    }

    public void setHighColor(byte r, byte g, byte b, byte a) {
        this.colorState[4] = r;
        this.colorState[5] = g;
        this.colorState[6] = b;
        this.colorState[7] = a;
    }

    /**
     * Set both.
     */
    public void setColor(byte r, byte g, byte b, byte a) {
        this.setLowColor(r, g, b, a);
        this.setHighColor(r, g, b, a);
    }

    /**
     * Set both.
     */
    public void setColor(int r, int g, int b, int a) {
        this.setColor((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public void setLowColor(int r, int g, int b, int a) {
        this.setLowColor((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public void setHighColor(int r, int g, int b, int a) {
        this.setHighColor((byte) r, (byte) g, (byte) b, (byte) a);
    }

    /**
     * Set both.
     */
    public void setColor(@NotNull Color color) {
        this.setColor((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    public void setLowColor(@NotNull Color color) {
        this.setLowColor((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    public void setHighColor(@NotNull Color color) {
        this.setHighColor((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    /**
     * Set both, the value 0.0 to 1.0
     */
    public void setColor(float r, float g, float b, float a) {
        this.setColor((byte) (r * 255.0f), (byte) (g * 255.0f), (byte) (b * 255.0f), (byte) (a * 255.0f));
    }

    public void setLowColor(float r, float g, float b, float a) {
        this.setLowColor((byte) (r * 255.0f), (byte) (g * 255.0f), (byte) (b * 255.0f), (byte) (a * 255.0f));
    }

    public void setHighColor(float r, float g, float b, float a) {
        this.setHighColor((byte) (r * 255.0f), (byte) (g * 255.0f), (byte) (b * 255.0f), (byte) (a * 255.0f));
    }

    public void setLowAlpha(byte a) {
        this.colorState[3] = a;
    }

    public void setHighAlpha(byte a) {
        this.colorState[7] = a;
    }

    /**
     * Set both, the value 0.0 to 1.0
     */
    public void setAlpha(byte a) {
        this.setLowAlpha(a);
        this.setHighAlpha(a);
    }

    public void setAlpha(int a) {
        this.setAlpha((byte) a);
    }

    public void setLowAlpha(int a) {
        this.setLowAlpha((byte) a);
    }

    public void setHighAlpha(int a) {
        this.setHighAlpha((byte) a);
    }

    /**
     * Set both, the value 0.0 to 1.0
     */
    public void setAlpha(float a) {
        this.setAlpha((byte) (a * 255.0f));
    }

    public void setLowAlpha(float a) {
        this.setLowAlpha((byte) (a * 255.0f));
    }

    public void setHighAlpha(float a) {
        this.setHighAlpha((byte) (a * 255.0f));
    }

    /**
     * @return high emissive color.
     */
    public byte[] getEmissiveColorArray() {
        return new byte[]{this.colorState[12], this.colorState[13], this.colorState[14], this.colorState[15]};
    }

    public byte[] getLowEmissiveColorArray() {
        return new byte[]{this.colorState[8], this.colorState[9], this.colorState[10], this.colorState[11]};
    }

    public byte[] getHighEmissiveColorArray() {
        return this.getEmissiveColorArray();
    }

    /**
     * @return high emissive color.
     */
    public Color getEmissiveColor() {
        return new Color(
                this.colorState[12] & 0xFF,
                this.colorState[13] & 0xFF,
                this.colorState[14] & 0xFF,
                this.colorState[15] & 0xFF);
    }

    public Color getLowEmissiveColor() {
        return new Color(
                this.colorState[8] & 0xFF,
                this.colorState[9] & 0xFF,
                this.colorState[10] & 0xFF,
                this.colorState[11] & 0xFF);
    }

    public Color getHighEmissiveColor() {
        return this.getEmissiveColor();
    }

    /**
     * @return high emissive color.
     */
    public Vector4f getEmissiveColor4f() {
        return new Vector4f(
                (this.colorState[12] & 0xFF) / 255.0f,
                (this.colorState[13] & 0xFF) / 255.0f,
                (this.colorState[14] & 0xFF) / 255.0f,
                (this.colorState[15] & 0xFF) / 255.0f);
    }

    public Vector4f getLowEmissiveColor4f() {
        return new Vector4f(
                (this.colorState[8] & 0xFF) / 255.0f,
                (this.colorState[9] & 0xFF) / 255.0f,
                (this.colorState[10] & 0xFF) / 255.0f,
                (this.colorState[11] & 0xFF) / 255.0f);
    }

    public Vector4f getHighEmissiveColor4f() {
        return this.getEmissiveColor4f();
    }

    /**
     * @return high emissive color.
     */
    public byte getEmissiveColorAlpha() {
        return this.colorState[15];
    }

    public byte getLowEmissiveColorAlpha() {
        return this.colorState[11];
    }

    public byte getHighEmissiveColorAlpha() {
        return this.getEmissiveColorAlpha();
    }

    /**
     * @return high emissive color.
     */
    public int getEmissiveColorAlphaI() {
        return this.getEmissiveColorAlpha() & 0xFF;
    }

    public int getLowEmissiveColorAlphaI() {
        return this.getLowEmissiveColorAlpha() & 0xFF;
    }

    public int getHighEmissiveColorAlphaI() {
        return this.getEmissiveColorAlphaI();
    }

    /**
     * @return high emissive color.
     */
    public float getEmissiveColorAlphaF() {
        return this.getEmissiveColorAlphaI() / 255.0f;
    }

    public float getLowEmissiveColorAlphaF() {
        return this.getLowEmissiveColorAlphaI() / 255.0f;
    }

    public float getHighEmissiveColorAlphaF() {
        return this.getEmissiveColorAlphaF();
    }

    public void setLowEmissiveColor(byte r, byte g, byte b, byte a) {
        this.colorState[8] = r;
        this.colorState[9] = g;
        this.colorState[10] = b;
        this.colorState[11] = a;
    }

    public void setHighEmissiveColor(byte r, byte g, byte b, byte a) {
        this.colorState[12] = r;
        this.colorState[13] = g;
        this.colorState[14] = b;
        this.colorState[15] = a;
    }

    /**
     * Set both.
     */
    public void setEmissiveColor(byte r, byte g, byte b, byte a) {
        this.setLowEmissiveColor(r, g, b, a);
        this.setHighEmissiveColor(r, g, b, a);
    }

    public void setLowEmissiveColor(int r, int g, int b, int a) {
        this.setLowEmissiveColor((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public void setHighEmissiveColor(int r, int g, int b, int a) {
        this.setHighEmissiveColor((byte) r, (byte) g, (byte) b, (byte) a);
    }

    /**
     * Set both.
     */
    public void setEmissiveColor(int r, int g, int b, int a) {
        this.setEmissiveColor((byte) r, (byte) g, (byte) b, (byte) a);
    }

    /**
     * Set both.
     */
    public void setEmissiveColor(@NotNull Color color) {
        this.setEmissiveColor((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    public void setLowEmissiveColor(@NotNull Color color) {
        this.setLowEmissiveColor((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    public void setHighEmissiveColor(@NotNull Color color) {
        this.setHighEmissiveColor((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    }

    public void setLowEmissiveColor(float r, float g, float b, float a) {
        this.setLowEmissiveColor((byte) (r * 255.0f), (byte) (g * 255.0f), (byte) (b * 255.0f), (byte) (a * 255.0f));
    }

    public void setHighEmissiveColor(float r, float g, float b, float a) {
        this.setHighEmissiveColor((byte) (r * 255.0f), (byte) (g * 255.0f), (byte) (b * 255.0f), (byte) (a * 255.0f));
    }

    /**
     * Set both, the value 0.0 to 1.0
     */
    public void setEmissiveColor(float r, float g, float b, float a) {
        this.setEmissiveColor((byte) (r * 255.0f), (byte) (g * 255.0f), (byte) (b * 255.0f), (byte) (a * 255.0f));
    }

    public void setLowEmissiveAlpha(byte a) {
        this.colorState[11] = a;
    }

    public void setHighEmissiveAlpha(byte a) {
        this.colorState[15] = a;
    }

    public void setEmissiveAlpha(byte a) {
        this.setLowEmissiveAlpha(a);
        this.setHighEmissiveAlpha(a);
    }

    public void setLowEmissiveAlpha(int a) {
        this.setLowEmissiveAlpha((byte) a);
    }

    public void setHighEmissiveAlpha(int a) {
        this.setHighEmissiveAlpha((byte) a);
    }

    public void setEmissiveAlpha(int a) {
        this.setEmissiveAlpha((byte) a);
    }

    public void setLowEmissiveAlpha(float a) {
        this.setLowEmissiveAlpha((byte) (a * 255.0f));
    }

    public void setHighEmissiveAlpha(float a) {
        this.setHighEmissiveAlpha((byte) (a * 255.0f));
    }

    /**
     * Set both, the value 0.0 to 1.0
     */
    public void setEmissiveAlpha(float a) {
        this.setEmissiveAlpha((byte) (a * 255.0f));
    }

    public float[] getTimer() {
        return new float[]{this.state[10], this.state[11], this.state[12], this.state[13]};
    }

    /**
     * Will pick a max total value for a global timer.<p>
     * Just initialized data values, to use 3d-texture id get the actual data.
     */
    public void setTimer(float fadeIn, float full, float fadeOut) {
        if (fadeIn <= 0.0f && full <= 0.0f && fadeOut <= 0.0f) {
            this.state[11] = this.state[12] = this.state[13] = -512.0f;
            this.state[10] = 0.0f;
            return;
        }
        this.state[10] = 3.0f;
        if (fadeIn <= 0.0f) {
            this.state[11] = -512.0f;
            this.state[10] = 2.0f;
            if (full <= 0.0f) this.state[10] = 1.0f;
        } else this.state[11] = 1.0f / fadeIn;
        this.state[12] = full <= 0.0f ? -512.0f : 1.0f / full;
        this.state[13] = fadeOut <= 0.0f ? -512.0f : 1.0f / fadeOut;
    }

    /**
     * Only for {@link InstanceRenderAPI#submitFixedInstanceData()}.
     *
     * @param state valid state: {@link BoxEnum#TIMER_IN}, {@link BoxEnum#TIMER_FULL}, {@link BoxEnum#TIMER_OUT}.
     */
    public void setFixedInstanceAlpha(float alpha, byte state) {
        this.state[14] = Math.max(Math.min(alpha, 1.0f), 0.0f);
        if (this.state[14] >= 0.0f) {
            if (state == BoxEnum.TIMER_FULL) this.state[14] += 20.0f;
            else if (state == BoxEnum.TIMER_OUT) this.state[14] += 10.0f;
            else if (state != BoxEnum.TIMER_IN) this.state[14] = 0.0f;
        }
    }

    /**
     * Only for {@link InstanceRenderAPI#submitFixedInstanceData()}.
     */
    public void copyFixedInstanceAlphaState(InstanceDataAPI instanceData) {
        this.state[14] = instanceData.getState()[14];
    }
}
