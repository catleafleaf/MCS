package org.boxutil.base;

import org.jetbrains.annotations.NotNull;
import org.boxutil.base.api.MaterialRenderAPI;
import org.boxutil.define.BoxEnum;
import org.boxutil.units.standard.attribute.MaterialData;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public abstract class BaseMIRenderData extends BaseInstanceRenderData implements MaterialRenderAPI {
    protected byte drawMode = BoxEnum.MODE_COMMON;
    protected MaterialData material = new MaterialData();

    public void delete() {
        super.delete();
        this.material = null;
    }

    public void reset() {
        super.reset();
        this.drawMode = BoxEnum.MODE_COMMON;
    }

    public byte getDrawMode() {
        return this.drawMode;
    }

    /**
     * Ignore normal when color mode.
     *
     * @see BoxEnum valid values: {@link BoxEnum#MODE_COMMON}, {@link BoxEnum#MODE_COLOR}.
     */
    public void setDrawMode(byte drawMode) {
        this.drawMode = drawMode;
    }

    public @NotNull MaterialData getMaterialData() {
        if (this.hasDelete()) return new MaterialData();
        return this.material;
    }

    public void setMaterialData(@NotNull MaterialData material) {
        this.material = material == null ? new MaterialData() : material;
    }

    public FloatBuffer pickDataPackage_vec4() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(12);
        buffer.put(this.material.getState());
        buffer.put(this.getGlobalTimerAlpha());
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }
}
