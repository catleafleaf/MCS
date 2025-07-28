package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class Agglomeration extends BaseHullMod {
    private static final float SPEED_BONUS_PER_SHIP = 3f;
    private static final float DAMAGE_BONUS_PER_SHIP = 1f;
    private static final String MOD_ID = "Agglomeration";

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk()) return;

        int count = 0;
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other.getOwner() == ship.getOwner() &&
                    !other.isHulk() &&
                    !other.isFighter() &&
                    other.getHullSpec().getHullId().equals(ship.getHullSpec().getHullId()) &&
                    other.getVariant().getHullMods().contains(MOD_ID)) {
                count++;
            }
        }

        float speedBonus = count * SPEED_BONUS_PER_SHIP ;
        float damageBonus = count * DAMAGE_BONUS_PER_SHIP ;

        MutableShipStatsAPI stats = ship.getMutableStats();

        stats.getMaxSpeed().modifyPercent(MOD_ID, speedBonus);
        stats.getAcceleration().modifyPercent(MOD_ID, speedBonus);
        stats.getDeceleration().modifyPercent(MOD_ID, speedBonus);
        stats.getTurnAcceleration().modifyPercent(MOD_ID, speedBonus);
        stats.getMaxTurnRate().modifyPercent(MOD_ID, speedBonus);

        stats.getEnergyWeaponDamageMult().modifyPercent(MOD_ID, damageBonus);
        stats.getBallisticWeaponDamageMult().modifyPercent(MOD_ID, damageBonus);

        if (Global.getCombatEngine().getPlayerShip() == ship && count > 0) {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    MOD_ID + "_bonus",
                    "graphics/icons/hullsys/high_energy_focus.png",
                    "编队增益",
                    String.format("伤害 +%.0f%% 机动性 +%.0f%%", damageBonus, speedBonus),
                    false
            );
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "3%";
        if (index == 1) return "1%";
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && !ship.isFighter();
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship == null) return "不能安装在无效舰船上";
        if (ship.isFighter()) return "不能安装在战机上";
        return null;
    }
}