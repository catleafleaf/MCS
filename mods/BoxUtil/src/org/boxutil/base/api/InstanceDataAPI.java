package org.boxutil.base.api;

import org.boxutil.define.BoxEnum;

/**
 * <strong>Priority use this if renders more than one entity.</strong><p>
 * Timer value used 32bit-float in GL.<p>
 * Color used unsigned byte in GL.<p>
 * Other data is changeable by user.
 */
public interface InstanceDataAPI {
    float[] pickFinal_vec4();

    float[][] pickFixedFinal_vec4();

    float[] pickTimer_vec4();

    float[][] pickState_vec4();

    /**
     * For built-in pipeline.
     * {<p>
     * X: clR, chR, elR, ehR<p>
     * Y: clG, chG, elG, ehG<p>
     * Z: clB, chB, elB, ehB<p>
     * W: clA, chA, elA, ehA<p>
     * }
     */
    byte[][] pickColor_vec4x4();

    /**
     * For built-in pipeline.
     * {<p>
     * X: chR, ehR<p>
     * Y: chG, ehG<p>
     * Z: chB, ehB<p>
     * W: chA, ehA<p>
     * }
     */
    byte[][] pickFixedColor_vec4x2();

    float[] getState();

    byte[] getColorState();

    float[] getTimer();

    void setTimer(float fadeIn, float full, float fadeOut);

    /**
     * Only for {@link InstanceRenderAPI#submitFixedInstanceData()}.
     *
     * @param state valid state: {@link BoxEnum#TIMER_IN}, {@link BoxEnum#TIMER_FULL}, {@link BoxEnum#TIMER_OUT}.
     */
    void setFixedInstanceAlpha(float alpha, byte state);

    /**
     * Only for {@link InstanceRenderAPI#submitFixedInstanceData()}.
     */
    void copyFixedInstanceAlphaState(InstanceDataAPI instanceData);
}
