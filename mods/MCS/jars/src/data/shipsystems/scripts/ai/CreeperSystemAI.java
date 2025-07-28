package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import java.util.List;
import java.util.ArrayList;

public class CreeperSystemAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipAPI primaryTarget;
    private final IntervalUtil targetUpdateTimer = new IntervalUtil(0.3f, 0.5f);
    private boolean systemArmed=false;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused() || ship == null || engine == null) return;

        // 更新目标（兼容原版舰船列表）
        targetUpdateTimer.advance(amount);
        if (targetUpdateTimer.intervalElapsed()) {
            primaryTarget = findPriorityTargetLegacy();
        }

        // 执行移动指令
        if (primaryTarget != null && primaryTarget.isAlive()) {
            float currentDistance = distanceBetween(ship.getLocation(), primaryTarget.getLocation());
            checkSystemActivation(currentDistance);
            implementMovementProtocol(primaryTarget.getLocation(), 5000f);
        }
    }
    private void checkSystemActivation(float distance) {
        ShipSystemAPI system = ship.getSystem();

        // 系统有效性检查
        if (system == null || system.isCoolingDown() || system.isActive()) return;

        // 距离触发条件
        if (distance <= 1000f && !systemArmed) {
            ship.useSystem();
            systemArmed = true; // 防止重复激活
        } else if (distance > 1000f) {
            systemArmed = false; // 超出距离则重置触发器
        }
        }
    //■■ 原生目标选择逻辑（不依赖第三方库）■■
    private ShipAPI findPriorityTargetLegacy() {
        ShipAPI result = null;
        float closest = Float.MAX_VALUE;
        ShipAPI.HullSize largestSize = ShipAPI.HullSize.FIGHTER;

        // 获取所有敌方舰船实体
        List<ShipAPI> enemies = new ArrayList<>();
        for (ShipAPI tmp : engine.getShips()) {
            if (tmp.getOwner() != ship.getOwner() && tmp.isAlive() && !tmp.isFighter()) {
                enemies.add(tmp);
            }
        }

        // 双层判定：先找最大舰船，再找最近的
        for (ShipAPI candidate : enemies) {
            // 最大级别筛选
            if (candidate.getHullSize().ordinal() > largestSize.ordinal()) {
                largestSize = candidate.getHullSize();
                closest = Float.MAX_VALUE; // 刷新距离判定
            }

            // 同一级别下的距离比较
            if (candidate.getHullSize() == largestSize) {
                float dist = distanceBetween(ship.getLocation(), candidate.getLocation());
                if (dist < closest && dist <= 5000f) { // 五千米范围硬限制
                    closest = dist;
                    result = candidate;
                }
            }
        }
        return result;
    }

    //■■ 基础位移控制协议（不依赖任何高级库）■■
    private void implementMovementProtocol(Vector2f goalPosition, float activationRadius) {
        float actualDistance = distanceBetween(ship.getLocation(), goalPosition);
        if (actualDistance > 100f) {
            // 基本速度控制体系
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            if (actualDistance > activationRadius * 0.8f) {
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
            }

            // 转向系统性控制（不含传感器数据运算）
            angleTurningControl(ship, goalPosition);

            // 旧版本引擎力状态维护技术
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
        }
    }

    //■■ 面向核心算法（基于基础三角学）■■
    private void angleTurningControl(ShipAPI source, Vector2f targetPos) {
        float deltaX = targetPos.x - source.getLocation().x;
        float deltaY = targetPos.y - source.getLocation().y;
        double targetAngleRad = Math.atan2(deltaY, deltaX);
        float targetAngle = (float) Math.toDegrees(targetAngleRad);

        // 标准化航向角度差值
        float angleDiff = targetAngle - source.getFacing();
        while (angleDiff < -180f) angleDiff += 360f;
        while (angleDiff > 180f) angleDiff -= 360f;

        // 直接发布转向指令
        if (angleDiff > 5f) {
            ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
        } else if (angleDiff < -5f) {
            ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
        }
    }

    //■■ 自定义数学工具（避免格式兼容风险）■■
    private float distanceBetween(Vector2f p1, Vector2f p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }

}