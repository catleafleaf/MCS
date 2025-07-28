// Created by catleafleaf on 2025-05-11 06:43:27
package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * 骷髅AI系统
 * - 检测弓箭武器弹药量
 * - 弹药量>=40时使用系统并开火
 * - 弹药量不足时后退蓄力
 */
public class SkeletonSystemAI implements ShipSystemAIScript {
    private static final String WEAPON_ID = "MCS_BOW";   // 要检测的武器ID
    private static final float RETREAT_RANGE = 2000f;    // 后退触发射程
    private static final float BASE_RANGE = 1500f;       // 固定基础射程
    private static final int AMMO_THRESHOLD = 40;        // 弹药量阈值

    private ShipAPI ship;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (ship == null || target == null) return;

        WeaponAPI bow = null;
        float weaponRange = BASE_RANGE;

        // 查找弓箭武器
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (WEAPON_ID.equals(weapon.getId())) {
                bow = weapon;
                break;
            }
        }

        if (bow == null) return;

        float distance = MathUtils.getDistance(ship.getLocation(), target.getLocation());
        boolean targetInRange = distance <= weaponRange;
        boolean hasEnoughAmmo = bow.getAmmo() >= AMMO_THRESHOLD;

        if (hasEnoughAmmo) {
            // 弹药充足时：
            if (targetInRange) {
                // 目标在射程内，使用系统并开火
                ship.useSystem();
                bow.setForceFireOneFrame(true);
            }
        } else {
            // 弹药不足时：
            // 1. 强制停止武器开火
            bow.setForceNoFireOneFrame(true);

            // 2. 执行后退
            ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);


        }
    }
}