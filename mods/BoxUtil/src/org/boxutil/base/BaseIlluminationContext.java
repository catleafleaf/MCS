package org.boxutil.base;

/**
 * WIP
 */
public abstract class BaseIlluminationContext {
    public boolean init() {
        return true;
    }

    public void destroy() {

    }

    public void advance(float amount) {

    }

    public BaseShaderData getInfiniteLightProgram() {
        return null;
    }

    public BaseShaderData getPointLightProgram() {
        return null;
    }

    public BaseShaderData getSpotLightProgram() {
        return null;
    }

    public BaseShaderData getAreaLightProgram() {
        return null;
    }

    public boolean applyIllumination() {
        return true;
    }

    public boolean applyPost() {
        return true;
    }
}
