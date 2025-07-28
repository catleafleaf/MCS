package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;
import java.util.List;

/**
 * 舰船推进武器开火效果
 * 提供瞬时加速后延迟制动
 * @author catleafleaf
 * @version 2025-05-23 11:54:35
 */
public class MCSBoosterWeaponOnFire implements OnFireEffectPlugin {
    private static final float TARGET_SPEED = 150f; // 目标加速速度
    private static final float BRAKE_DELAY = 1f; // 制动延迟时间

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, final CombatEngineAPI engine) {
        final ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        // 计算并应用初始推力
        applyBoostForce(ship, projectile.getFacing(), engine);

        // 注册延迟制动效果
        engine.addPlugin(new EveryFrameCombatPlugin() {
            private float elapsed = 0f;
            private boolean hasBraked = false;

            @Override
            public void init(CombatEngineAPI engine) {}

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (engine.isPaused()) return;

                elapsed += amount;

                if (elapsed >= BRAKE_DELAY && !hasBraked) {
                    applyBrakeForce(ship, engine);
                    hasBraked = true;
                    engine.removePlugin(this);
                }
            }

            @Override
            public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

            @Override
            public void renderInWorldCoords(ViewportAPI viewport) {}

            @Override
            public void renderInUICoords(ViewportAPI viewport) {}
        });
    }

    /**
     * 应用初始推进力
     */
    private void applyBoostForce(ShipAPI ship, float angle, CombatEngineAPI engine) {
        // 计算需要的推力
        float mass = ship.getMass();
        float currentSpeed = ship.getVelocity().length();
        float speedDiff = TARGET_SPEED - currentSpeed;

        if (speedDiff <= 0) return;

        float force = mass * speedDiff;

        // 计算力的方向
        Vector2f direction = new Vector2f(
                (float) Math.cos(Math.toRadians(angle)),
                (float) Math.sin(Math.toRadians(angle))
        );

        // 应用推力
        direction.scale(force);
        Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());

        // 视觉效果
        addBoostEffects(ship, engine, speedDiff);
    }

    /**
     * 应用制动力
     */
    private void applyBrakeForce(ShipAPI ship, CombatEngineAPI engine) {
        Vector2f currentVelocity = new Vector2f(ship.getVelocity());
        float currentSpeed = currentVelocity.length();

        if (currentSpeed <= 0) return;

        // 计算制动力
        float mass = ship.getMass();
        float brakeForce = mass * currentSpeed;

        // 获取当前速度的反方向
        Vector2f brakeDirection = new Vector2f(currentVelocity);
        brakeDirection.normalise();
        brakeDirection.scale(-brakeForce);

        // 应用制动力
        Vector2f.add(ship.getVelocity(), brakeDirection, ship.getVelocity());

        // 视觉效果
        addBrakeEffects(ship, engine, currentSpeed);
    }

    /**
     * 添加推进视觉效果
     */
    private void addBoostEffects(ShipAPI ship, CombatEngineAPI engine, float speedDiff) {
        // 粒子效果
        engine.addHitParticle(
                ship.getLocation(),
                ship.getVelocity(),
                100f,
                1f,
                0.1f,
                Global.getSettings().getColor("engineColor")
        );

        // 音效
        Global.getSoundPlayer().playSound("system_plasma_burn", 1f, 1f, ship.getLocation(), ship.getVelocity());

        // 文字提示
        engine.addFloatingText(
                ship.getLocation(),
                String.format("推进器激活! +%.0f速度", speedDiff),
                20f,
                Global.getSettings().getColor("textFriendColor"),
                ship,
                0.5f,
                1.0f
        );
    }

    /**
     * 添加制动视觉效果
     */
    private void addBrakeEffects(ShipAPI ship, CombatEngineAPI engine, float currentSpeed) {
        // 粒子效果
        engine.addHitParticle(
                ship.getLocation(),
                ship.getVelocity(),
                100f,
                1f,
                0.1f,
                Global.getSettings().getColor("textNegativeColor")
        );

        // 音效
        Global.getSoundPlayer().playSound("system_emp_emitter_activate", 0.8f, 0.8f, ship.getLocation(), ship.getVelocity());

        // 文字提示
        engine.addFloatingText(
                ship.getLocation(),
                String.format("制动系统启动! 当前速度%.0f", currentSpeed),
                20f,
                Global.getSettings().getColor("textNegativeColor"),
                ship,
                0.5f,
                1.0f
        );
    }
}