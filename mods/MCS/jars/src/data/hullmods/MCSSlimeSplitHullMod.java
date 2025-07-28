package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MCSSlimeSplitPlugin;
import java.awt.Color;
import java.util.List;

public class MCSSlimeSplitHullMod extends BaseHullMod {
    private static final String HULL_ID = "MCS_Slime";
    private static final String FRIGATE_ID = "MCS_SlimeTiny";
    private static final Color GREEN = new Color(100, 200, 100);
    private static final float IMMUNITY_RANGE = 50f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBreakProb().modifyMult(id, 0f);
    }
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        if (ship.isHulk() && engine.isEntityInPlay(ship)) {
            handleSplitCheck(ship, engine);
            return;
        }

        boolean hasNearbyEnemy = false;
        List<ShipAPI> nearbyShips = engine.getShips();
        for (ShipAPI other : nearbyShips) {
            if (other == ship || other.isHulk()) continue;

            if (other.getOwner() != ship.getOwner()) {
                float distance = Misc.getDistance(ship.getLocation(), other.getLocation());
                if (distance <= IMMUNITY_RANGE) {
                    hasNearbyEnemy = true;
                    break;
                }
            }
        }
        String immunityId = ship.getId() + "_kinetic_immunity";
        if (hasNearbyEnemy) {
            ship.getMutableStats().getKineticDamageTakenMult().modifyMult(immunityId, 0f);
            ship.setJitterUnder(ship, GREEN, 1f, 3, 5f);
        } else {
            ship.getMutableStats().getKineticDamageTakenMult().unmodify(immunityId);
        }
    }

    private void handleSplitCheck(ShipAPI ship, CombatEngineAPI engine) {
        String hullId = ship.getHullSpec().getHullId();
        if (HULL_ID.equals(hullId) || FRIGATE_ID.equals(hullId)) {
            MCSSlimeSplitPlugin plugin = (MCSSlimeSplitPlugin) engine.getCustomData().get(MCSSlimeSplitPlugin.PLUGIN_KEY);
            if (plugin != null) {
                plugin.addSplit(ship);
            } else {
                plugin = new MCSSlimeSplitPlugin();
                plugin.init(engine);
                engine.addPlugin(plugin);
                plugin.addSplit(ship);
                engine.getCustomData().put(MCSSlimeSplitPlugin.PLUGIN_KEY, plugin);
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null) return false;
        String hullId = ship.getHullSpec().getHullId();
        return HULL_ID.equals(hullId) || FRIGATE_ID.equals(hullId);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship == null) return "无效的舰船";
        return "只能安装在史莱姆舰船上";
    }

    @Override
    public Color getBorderColor() {
        return GREEN;
    }

    @Override
    public Color getNameColor() {
        return GREEN;
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return String.format("在距离敌舰 %.0f 单位内时免疫动能伤害", IMMUNITY_RANGE);
        return null;
    }
}