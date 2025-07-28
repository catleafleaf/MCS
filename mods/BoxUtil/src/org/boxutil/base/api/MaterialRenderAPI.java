package org.boxutil.base.api;

import org.jetbrains.annotations.NotNull;
import org.boxutil.define.BoxEnum;
import org.boxutil.units.standard.attribute.MaterialData;

public interface MaterialRenderAPI {
    @Deprecated
    byte getDrawMode();

    /**
     * Ignore normal when color mode.
     *
     * @see BoxEnum valid values: {@link BoxEnum#MODE_COMMON}, {@link BoxEnum#MODE_COLOR}.
     */
    @Deprecated
    void setDrawMode(byte drawMode);

    @NotNull MaterialData getMaterialData();

    void setMaterialData(@NotNull MaterialData material);
}
