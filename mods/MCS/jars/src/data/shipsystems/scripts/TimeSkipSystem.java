package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TimeSkipSystem extends BaseShipSystemScript {
    private static final float TIME_SKIP = 3f;
    private static final float RANGE = 1500f;

    private List<ShipPositionData> positionDataList = new ArrayList<>();
    private boolean hasSkipped = false;

    private static class ShipPositionData {
        ShipAPI ship;
        Vector2f originalPos;
        Vector2f originalVel;
        float originalFacing;
        WeaponAPI[] weapons;
        boolean[] wasFiring;
        float[] cooldownRemaining;
        boolean wasSystemActive;
        float systemLevel;

        ShipPositionData(ShipAPI ship) {
            this.ship = ship;
            this.originalPos = new Vector2f(ship.getLocation());
            this.originalVel = new Vector2f(ship.getVelocity());
            this.originalFacing = ship.getFacing();

            List<WeaponAPI> allWeapons = ship.getAllWeapons();
            this.weapons = allWeapons.toArray(new WeaponAPI[0]);
            this.wasFiring = new boolean[weapons.length];
            this.cooldownRemaining = new float[weapons.length];

            for(int i = 0; i < weapons.length; i++) {
                WeaponAPI weapon = weapons[i];
                this.wasFiring[i] = weapon.isFiring();
                this.cooldownRemaining[i] = weapon.getCooldownRemaining();
            }

            // 记录舰船系统状态
            ShipSystemAPI system = ship.getSystem();
            if (system != null) {
                this.wasSystemActive = system.isActive();
                this.systemLevel = system.getEffectLevel();
            }
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();

        if (state == State.IN && !hasSkipped) {
            // 记录范围内所有船的数据
            for (ShipAPI other : engine.getShips()) {
                if (!other.isHulk() && MathUtils.getDistance(other.getLocation(), ship.getLocation()) <= RANGE) {
                    positionDataList.add(new ShipPositionData(other));
                }
            }

            // 计算并应用预测结果
            for (ShipPositionData data : positionDataList) {
                // 基础位置预测
                Vector2f predictedPos = new Vector2f(data.originalPos);
                Vector2f velocity = new Vector2f(data.originalVel);
                predictedPos.x += velocity.x * TIME_SKIP;
                predictedPos.y += velocity.y * TIME_SKIP;

                // 应用预测结果
                data.ship.getLocation().set(predictedPos);

                // 处理舰船系统
                ShipSystemAPI system = data.ship.getSystem();
                if (system != null && data.wasSystemActive) {
                    // 如果系统激活中，直接结束
                    system.deactivate();

                    // 添加系统结束的视觉效果
                    engine.addHitParticle(
                            data.ship.getLocation(),
                            new Vector2f(),
                            100f,
                            0.8f,
                            0.1f,
                            Color.GREEN
                    );

                    // 根据效果等级添加不同的视觉效果
                    if (data.systemLevel > 0.5f) {
                        engine.addHitParticle(
                                data.ship.getLocation(),
                                new Vector2f(),
                                150f,
                                1f,
                                0.2f,
                                Color.CYAN
                        );
                    }
                }

                // 武器预测与应用
                for(int i = 0; i < data.weapons.length; i++) {
                    WeaponAPI weapon = data.weapons[i];
                    if(data.wasFiring[i]) {
                        float predictedCooldown = data.cooldownRemaining[i] - TIME_SKIP;
                        if(predictedCooldown <= 0f && weapon.usesAmmo() && weapon.getAmmo() > 0) {
                            Vector2f weaponLoc = weapon.getLocation();
                            float angle = weapon.getCurrAngle();

                            engine.spawnProjectile(
                                    data.ship,
                                    weapon,
                                    weapon.getId(),
                                    weaponLoc,
                                    angle,
                                    data.ship.getVelocity()
                            );

                            if(weapon.usesAmmo()) {
                                weapon.setAmmo(weapon.getAmmo() - 1);
                            }
                        }
                    }
                }
            }

            hasSkipped = true;

            // 视觉效果
            engine.addHitParticle(
                    ship.getLocation(),
                    new Vector2f(),
                    200f,
                    1f,
                    0.1f,
                    Color.BLUE
            );

            // 添加音效
            Global.getSoundPlayer().playSound("system_temporalshell_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        positionDataList.clear();
        hasSkipped = false;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("时间跳跃中", false);
        }
        return null;
    }
}