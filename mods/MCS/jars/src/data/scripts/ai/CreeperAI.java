package data.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class CreeperAI implements ShipSystemAIScript { // 确保类名一致

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipAPI target;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused()) return;

        // 更新目标
        this.target = findBestTarget();

        // 执行核心行为
        if (this.target != null) {
            executeRammingBehavior(amount); // 确保方法名完全一致
        }
    }

    // ==== 关键方法定义 ====
    private void executeRammingBehavior(float delta) {
        // 1. 计算目标方向
        Vector2f targetPos = target.getLocation();
        float desiredAngle = Misc.getAngleInDegrees(ship.getLocation(), targetPos);

        // 2. 转向控制
        float angleDiff = Misc.getAngleDiff(ship.getFacing(), desiredAngle);
        if (angleDiff > 5f) {
            ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
        } else if (angleDiff < -5f) {
            ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
        }

        // 3. 推进控制（保持最大速度）
        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        ship.getVelocity().scale(1.15f); // 突破速度限制

        // 4. 碰撞前减速（防止擦碰）
        if (MathUtils.getDistance(ship, target) < ship.getCollisionRadius() * 3f) {
            ship.giveCommand(ShipCommand.DECELERATE, null, 0);
        }
    }

    private ShipAPI findBestTarget() {
        ShipAPI bestTarget = null;
        float maxPriority = -Float.MAX_VALUE;

        for (ShipAPI candidate : AIUtils.getEnemiesOnMap(ship)) {
            if (!candidate.isAlive()) continue;

            // 目标优先级 = 船体尺寸系数 * 距离倒数
            float priority = candidate.getHullSize().ordinal() * 100f
                    / (MathUtils.getDistance(ship, candidate) + 1f);

            if (priority > maxPriority) {
                maxPriority = priority;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }
}
