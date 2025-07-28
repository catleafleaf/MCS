package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class IronGolemMOD extends BaseHullMod {

    // 定义插件的效果参数
    private static final float SPEED_BONUS = 50f; // 提升50%最高航速
    private static final float WEAPON_RANGE_CAP = 400f; // 武器射程上限
    private static final float WEAPON_DAMAGE_BONUS = 100f; // 武器伤害提升100%
    private static final float RANGE_THRESHOLD = 400f; // 武器射程阈值
    private static final float RANGE_MULT = 0f; // 超出射程阈值的射程倍数

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // 在舰船创建之前应用效果
        stats.getMaxSpeed().modifyPercent(id, SPEED_BONUS); // 提升最高航速
        stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD); // 修改武器射程阈值
        stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT); // 修改超出射程阈值的射程倍数

        // 提高武器伤害
        stats.getEnergyWeaponDamageMult().modifyPercent(id, WEAPON_DAMAGE_BONUS); // 能量武器伤害提升
        stats.getBallisticWeaponDamageMult().modifyPercent(id, WEAPON_DAMAGE_BONUS); // 弹道武器伤害提升

    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        // 返回插件的描述参数
        if (index == 0) return "" + (int) SPEED_BONUS + "%"; // 最高航速加成
        if (index == 1) return "" + (int) WEAPON_RANGE_CAP; // 武器射程上限
        if (index == 2) return "" + (int) WEAPON_DAMAGE_BONUS + "%"; // 武器伤害加成
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        // 检查插件是否适用于当前舰船
        return true; // 默认适用于所有舰船
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        // 返回插件不适用时的提示信息
        return "This hullmod can be applied to any ship."; // 默认提示
    }
}
