package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class IronGolemSystemAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;

    // 设置常量
    private static final float TARGET_SEARCH_RANGE = 10000f;  // 搜索范围
    private static final float MAINTAIN_RANGE = 0f;         // 保持距离
    private static final float AGGRESSIVE_RANGE = 50f;        // 血量低时的接近距离
    private static final float LOW_HEALTH_THRESHOLD = 0.3f;   // 低血量阈值 (30%)

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || ship == null) return;

        // 如果没有指定目标，寻找最近的敌方舰船
        if (target == null) {
            target = findNearestEnemy();
        }
        if (target == null) return;

        // 计算与目标的距离
        float distance = MathUtils.getDistance(ship.getLocation(), target.getLocation());

        // 检查后退状态并防止后退
        preventRetreat();

        // 检查血量状态并决定行为模式
        boolean isLowHealth = ship.getHitpoints() / ship.getMaxHitpoints() <= LOW_HEALTH_THRESHOLD;

        // 根据距离和血量状态执行不同的行为
        if (distance > (isLowHealth ? AGGRESSIVE_RANGE : MAINTAIN_RANGE)) {
            handleLongRangeEngagement(target, isLowHealth);
        } else {
            handleCloseRangeEngagement(distance, target, isLowHealth);
        }
    }

    private ShipAPI findNearestEnemy() {
        ShipAPI nearestEnemy = null;
        float nearestDistance = TARGET_SEARCH_RANGE;

        for (ShipAPI otherShip : engine.getShips()) {
            // 跳过非敌对、已死亡、导弹和战机
            if (otherShip.getOwner() == ship.getOwner() ||
                    !otherShip.isAlive() ||
                    otherShip.isFighter() ||
                    otherShip.isDrone()) continue;

            float distance = MathUtils.getDistance(ship, otherShip);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestEnemy = otherShip;
            }
        }

        return nearestEnemy;
    }

    private void handleLongRangeEngagement(ShipAPI target, boolean isLowHealth) {
        // 保持系统激活
        if (system != null && !system.isActive()) {
            ship.useSystem();
        }

        // 在低血量状态下更激进地接近
        if (isLowHealth) {
            // 全速冲向目标
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            // 在接近时额外使用横向推进以增加机动性
            ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
        } else {
            // 正常接近
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        }
    }

    private void handleCloseRangeEngagement(float distance, ShipAPI target, boolean isLowHealth) {
        float desiredRange = isLowHealth ? AGGRESSIVE_RANGE : MAINTAIN_RANGE;

        if (isLowHealth) {
            // 低血量时的激进战术
            if (distance < desiredRange * 0.5f) {
                // 距离过近时快速调整位置
                ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            } else if (distance > desiredRange * 1.5f) {
                // 保持贴近
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
                // 使用战术系统
                if (system != null && !system.isActive()) {
                    ship.useSystem();
                }
            }

            // 低血量时更频繁地使用战术系统
            if (system != null && !system.isActive() && Math.random() < 0.3) {
                ship.useSystem();
            }
        } else {
            // 正常战术
            if (distance < desiredRange * 0.9f) {
                ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
            } else if (distance > desiredRange * 1.1f) {
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            }
        }
    }

    private void preventRetreat() {
        // 检查速度方向
        Vector2f velocity = ship.getVelocity();
        if (velocity.length() > 0) {
            float velocityAngle = VectorUtils.getFacing(velocity);
            float angleDiff = MathUtils.getShortestRotation(ship.getFacing(), velocityAngle);

            // 如果在向后移动，给出向前的命令
            if (Math.abs(angleDiff) > 90f) {
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            }
        }
    }
}