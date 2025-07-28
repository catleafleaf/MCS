// Created by catleafleaf on 2025-04-29
// Last updated on 2025-04-29 10:26:18 UTC
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lazywizard.lazylib.MathUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * 引力场效果船插
 * - 2000范围内敌舰减速和降低机动性
 * - 相位船的相位间隔时间提升
 * - 效果随距离指数衰减，200范围内达到最大
 *
 * @author catleafleaf
 * @version 2025-04-29 10:26:18
 */
public class GravityFieldHullmod extends BaseHullMod {
    // 核心参数
    private static final float MAX_RANGE = 2000f;         // 最大影响范围
    private static final float MAX_EFFECT_RANGE = 200f;   // 最大效果范围
    private static final float MAX_SPEED_REDUCE = 1f;     // 最大速度减少（100%）
    private static final float MAX_MANEUVER_REDUCE = 1f;  // 最大机动性减少（100%）
    private static final float MAX_PHASE_INCREASE = 1f;   // 最大相位间隔增加（100%）

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;
        CombatEngineAPI engine = Global.getCombatEngine();

        // 获取范围内的所有敌舰
        List<ShipAPI> nearbyEnemies = new ArrayList<>();
        for (ShipAPI otherShip : engine.getShips()) {
            if (otherShip.isHulk() || !otherShip.isAlive() || otherShip.getOwner() == ship.getOwner()) continue;

            float distance = MathUtils.getDistance(ship.getLocation(), otherShip.getLocation());
            if (distance <= MAX_RANGE) {
                nearbyEnemies.add(otherShip);
            }
        }

        // 应用效果到范围内的敌舰
        for (ShipAPI enemy : nearbyEnemies) {
            float distance = MathUtils.getDistance(ship.getLocation(), enemy.getLocation());
            float effectLevel = calculateEffectLevel(distance);

            // 应用减速效果
            String speedId = ship.getId() + "_gravity_speed";
            enemy.getMutableStats().getMaxSpeed().modifyMult(speedId, 1f - (effectLevel * MAX_SPEED_REDUCE));

            // 应用转向减速
            String turnId = ship.getId() + "_gravity_turn";
            enemy.getMutableStats().getAcceleration().modifyMult(turnId, 1f - (effectLevel * MAX_MANEUVER_REDUCE));
            enemy.getMutableStats().getDeceleration().modifyMult(turnId, 1f - (effectLevel * MAX_MANEUVER_REDUCE));
            enemy.getMutableStats().getTurnAcceleration().modifyMult(turnId, 1f - (effectLevel * MAX_MANEUVER_REDUCE));
            enemy.getMutableStats().getMaxTurnRate().modifyMult(turnId, 1f - (effectLevel * MAX_MANEUVER_REDUCE));

            // 相位船特殊处理
            if (enemy.getPhaseCloak() != null) {
                String phaseId = ship.getId() + "_gravity_phase";
                enemy.getMutableStats().getPhaseCloakCooldownBonus().modifyMult(phaseId, 1f + (effectLevel * MAX_PHASE_INCREASE));
            }
        }
    }

    /**
     * 根据距离计算效果强度
     * 使用指数曲线使得效果随距离快速衰减
     */
    private float calculateEffectLevel(float distance) {
        if (distance <= MAX_EFFECT_RANGE) return 1f;
        if (distance >= MAX_RANGE) return 0f;

        float normalized = (distance - MAX_EFFECT_RANGE) / (MAX_RANGE - MAX_EFFECT_RANGE);
        return (float) Math.pow(1f - normalized, 2); // 平方衰减
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return String.format("%.0f", MAX_RANGE);
        if (index == 1) return String.format("%.0f", MAX_EFFECT_RANGE);
        if (index == 2) return String.format("%.0f", MAX_SPEED_REDUCE * 100f);
        if (index == 3) return String.format("%.0f", MAX_PHASE_INCREASE * 100f);
        return null;
    }
}