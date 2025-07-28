package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

/**
 * BOW武器系统
 * 版本: 2025-04-13 04:14:51
 *
 * @author catleafleaf
 */
public class BOW implements EveryFrameWeaponEffectPlugin {

    // 弹药射程计算参数
    private static final float SKELETON_MAX_RANGE = 2500f;   // MCS_Skeleton的最大射程
    private static final float DEFAULT_MAX_RANGE = 1500f;    // 其他船只的最大射程
    private static final float BASE_RANGE = 600f;            // 基础射程
    private static final float RANGE_PER_AMMO = 18f;         // 每发弹药提供的额外射程

    // 其他参数
    private static final float EFFECT_DURATION = 0.5f;   // 效果持续时间
    private static final int MAX_AMMO = 50;             // 最大弹药数
    private static final float BASE_FLUX_PER_SECOND = 100f;    // 基础辐能消耗
    private static final float AMMO_FLUX_PER_SECOND = 100f;    // 每发弹药增加的辐能消耗

    private boolean fired = false;                // 开火状态追踪
    private float effectTimer = 0f;              // 效果计时器
    private int lastAmmoCount = 0;               // 开火时的弹药数
    private boolean isGeneratingFlux = false;    // 是否正在产生辐能
    private float fluxGenerationTimer = 0f;      // 辐能产生计时器

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused() || weapon == null) return;

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        // 开火检测
        if (weapon.isFiring() && !fired) {
            fired = true;
            lastAmmoCount = Math.min(weapon.getAmmo() + 1, MAX_AMMO);  // +1因为这是开火前的检查
            effectTimer = EFFECT_DURATION;
            isGeneratingFlux = true;
            fluxGenerationTimer = EFFECT_DURATION;
            handleWeaponFire(engine, weapon);
        } else if (!weapon.isFiring()) {
            fired = false;
        }


        // 2. 计算基于弹药量的实际射程
        float maxRange = "MCS_Skeleton".equals(ship.getHullSpec().getHullId()) ?
                SKELETON_MAX_RANGE : DEFAULT_MAX_RANGE;

        if (effectTimer > 0) {
            effectTimer -= amount;
            float actualRange = Math.min(maxRange, BASE_RANGE + (RANGE_PER_AMMO * lastAmmoCount));
            weapon.getSpec().setMaxRange(actualRange);
        } else if (effectTimer <= 0 && weapon.getAmmo() > 0) {
            float actualRange = Math.min(maxRange, BASE_RANGE + (RANGE_PER_AMMO * weapon.getAmmo()));
            weapon.getSpec().setMaxRange(actualRange);
        }

        // 辐能产生
        if (isGeneratingFlux && fluxGenerationTimer > 0) {
            fluxGenerationTimer -= amount;
            float fluxPerSecond = BASE_FLUX_PER_SECOND + (lastAmmoCount * AMMO_FLUX_PER_SECOND);
            ship.getFluxTracker().increaseFlux(fluxPerSecond * amount, false);

            if (fluxGenerationTimer <= 0) {
                isGeneratingFlux = false;
            }
        }
    }

    private void handleWeaponFire(CombatEngineAPI engine, WeaponAPI weapon) {
        Vector2f weaponLoc = weapon.getLocation();
        if (weaponLoc != null) {
            float particleSize = 10f * (1f + lastAmmoCount * 0.02f);
            float intensity = Math.min(1f, 0.3f + (lastAmmoCount * 0.014f));

            engine.addHitParticle(
                    weaponLoc,
                    weapon.getShip().getVelocity(),
                    particleSize,
                    intensity,
                    0.1f,
                    new Color(1f,
                            0.6f * intensity,
                            0.2f * intensity,
                            intensity)
            );
            if (lastAmmoCount > 25) {
                for (int i = 0; i < 3; i++) {
                    engine.addHitParticle(
                            weaponLoc,
                            new Vector2f(
                                    (float)(Math.random() * 100 - 50),
                                    (float)(Math.random() * 100 - 50)
                            ),
                            particleSize * 0.5f,
                            intensity * 0.7f,
                            0.05f,
                            new Color(1f, 0.3f, 0.1f, intensity * 0.5f)
                    );
                }
            }
        }

        // 清空弹夹
        weapon.setAmmo(0);
    }
}