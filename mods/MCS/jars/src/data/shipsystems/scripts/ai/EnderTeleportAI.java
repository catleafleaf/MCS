// Created by catleafleaf on 2025-04-28
// Last updated on 2025-04-28 08:07:05 UTC
package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.util.List;

public class EnderTeleportAI implements ShipSystemAIScript {
    private static final float MAX_TARGET_RANGE = 1700f;
    private static final float FIRE_DELAY = 0.6f;              // 开火延迟

    private ShipAPI ship;                                      // AI控制的船只
    private CombatEngineAPI engine;                           // 战斗引擎
    private final IntervalUtil tracker = new IntervalUtil(0f, 0.2f); // AI思考间隔
    private final IntervalUtil fireTimer = new IntervalUtil(FIRE_DELAY, FIRE_DELAY); // 开火计时器
    private boolean systemTriggered = false;                   // 系统是否已触发
    private boolean hasScheduledFire = false;                  // 是否已安排开火

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || ship == null) return;
        if (engine.isPaused()) return;

        // 如果系统不可用，重置状态
        if (!ship.getSystem().isActive() && systemTriggered) {
            systemTriggered = false;
            hasScheduledFire = false;
        }

        // 开火计时
        if (systemTriggered && !hasScheduledFire) {
            fireTimer.advance(amount);
            if (fireTimer.intervalElapsed()) {
                // 获取武器并开火
                WeaponAPI mainWeapon = findWeaponById(ship, "WS0001");
                if (mainWeapon != null) {
                    mainWeapon.setForceFireOneFrame(true);
                }
                hasScheduledFire = true;
            }
        }

        // AI决策间隔
        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;

        // 如果系统已激活或冷却中，不做决策
        if (ship.getSystem().isActive() || ship.getSystem().getCooldownRemaining() > 0) return;

        // 寻找最佳目标
        ShipAPI bestTarget = findBestTarget();
        if (bestTarget == null) return;

        // 检查是否在范围内且可用
        float distance = MathUtils.getDistance(ship, bestTarget);
        if (distance <= MAX_TARGET_RANGE && ship.getSystem().getCooldownRemaining() <= 0) {
            systemTriggered = true;
            hasScheduledFire = false;
            fireTimer.setElapsed(0f);
            ship.useSystem();
        }
    }

    private WeaponAPI findWeaponById(ShipAPI ship, String weaponId) {
        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (weapons != null) {
            for (WeaponAPI weapon : weapons) {
                if (weaponId.equals(weapon.getSlot().getId())) {
                    return weapon;
                }
            }
        }
        return null;
    }

    private ShipAPI findBestTarget() {
        ShipAPI bestTarget = null;
        float highestValue = -1f;

        for (ShipAPI potentialTarget : Global.getCombatEngine().getShips()) {
            // 跳过非敌方或已损毁的船只
            if (!potentialTarget.isAlive() ||
                    potentialTarget.getOwner() == ship.getOwner() ||
                    potentialTarget.isHulk()) continue;

            // 检查距离
            float distance = MathUtils.getDistance(ship, potentialTarget);
            if (distance > MAX_TARGET_RANGE) continue;

            // 计算目标价值
            float targetValue = calculateTargetValue(potentialTarget, distance);

            // 更新最佳目标
            if (targetValue > highestValue) {
                highestValue = targetValue;
                bestTarget = potentialTarget;
            }
        }

        return bestTarget;
    }

    private float calculateTargetValue(ShipAPI target, float distance) {
        // 主要因素：舰船等级（HullSize的序数）
        float value = target.getHullSize().ordinal() * 10000f;  // 乘以10000确保舰船等级是绝对主要因素

        // 次要因素：距离（越近价值越高）
        // 将距离转换为0-1的比例（越近越接近1）
        float distanceFactor = 1f - (distance / MAX_TARGET_RANGE);
        value += distanceFactor * 100f;  // 距离因素的权重比舰船等级小得多

        return value;
    }
}