package data.scripts.weapons;


import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

/**
 * 蜘蛛丝索效果
 * 命中目标时给自身一个向前的推力
 * 并使目标过载1秒
 *
 * @author catleafleaf
 * @version 2025-04-12 06:25:44
 */
public class SpiderWire implements BeamEffectPlugin {
    // 核心参数
    private static final float THRUST_FORCE = 500000f;   // 推进力度
    private static final float THRUST_DURATION = 0.5f;   // 推进持续时间
    private static final float OVERLOAD_DURATION = 1f;   // 过载持续时间

    // 推力状态
    private float thrustTimer = 0f;
    private boolean isThrusting = false;

    // 当前目标
    private ShipAPI currentTarget = null;
    private ShipAPI lastOverloadedTarget = null;  // 记录上次过载的目标

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;

        // 获取光束源和目标
        ShipAPI source = beam.getSource();
        CombatEntityAPI target = beam.getDamageTarget();

        // 目标有效性检查
        boolean isValidHit = isValidTarget(target);

        // 更新当前目标
        ShipAPI newTarget = isValidHit ? (ShipAPI) target : null;
        if (newTarget != currentTarget) {
            currentTarget = newTarget;
            if (currentTarget != null) {
                // 新目标被命中，启动推进
                startThrust();

                // 如果是新目标且未过载，则施加过载效果
                if (currentTarget != lastOverloadedTarget && !currentTarget.getFluxTracker().isOverloaded()) {
                    forceOverload(currentTarget);
                    lastOverloadedTarget = currentTarget;
                }
            }
        }

        // 如果正在推进，应用推力
        if (isThrusting && source != null) {
            applyThrust(source, amount);
        }

    }

    private void forceOverload(ShipAPI target) {
        if (target == null) return;

        FluxTrackerAPI fluxTracker = target.getFluxTracker();
        if (!fluxTracker.isOverloaded()) {
            fluxTracker.beginOverloadWithTotalBaseDuration(OVERLOAD_DURATION);
        }
    }

    private boolean isValidTarget(CombatEntityAPI target) {
        if (!(target instanceof ShipAPI)) return false;
        ShipAPI ship = (ShipAPI) target;

        return ship.isAlive()
                && !ship.isDrone()
                && !ship.isFighter()
                && !ship.isShuttlePod();
    }


    private void startThrust() {
        thrustTimer = THRUST_DURATION;
        isThrusting = true;
    }

    private void applyThrust(ShipAPI source, float amount) {
        if (thrustTimer > 0) {
            float facing = source.getFacing();
            float facingRad = (float) Math.toRadians(facing);
            Vector2f thrustDirection = new Vector2f(
                    (float) Math.cos(facingRad),
                    (float) Math.sin(facingRad)
            );

            // 计算推力
            float force = THRUST_FORCE * (1f / source.getMass());
            Vector2f thrust = new Vector2f(thrustDirection);
            thrust.scale(force * amount);

            // 应用推力
            Vector2f.add(source.getVelocity(), thrust, source.getVelocity());

            // 更新计时器
            thrustTimer -= amount;
            if (thrustTimer <= 0) {
                isThrusting = false;
            }
        }
    }

}