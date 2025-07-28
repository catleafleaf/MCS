package org.boxutil.units.builtin.shader;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import org.boxutil.manager.CampaignRenderingManager;

public final class BUtil_CampaignRenderingEntity extends BaseCustomEntityPlugin {
    private static CampaignRenderingManager _plugin = null;

    public void init(SectorEntityToken entity, Object pluginParams) {
        this.entity = entity;
        _plugin = (CampaignRenderingManager) pluginParams;
    }

    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (_plugin != null) _plugin.render(layer, viewport, this.entity);
    }

    public float getRenderRange() {
        return Float.MAX_VALUE;
    }

    public static void check(CampaignRenderingManager plugin) {
        if (_plugin != plugin) _plugin = plugin;
    }
}
