package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

/**
 * 蜘蛛丝索射程扩展效果
 * 检测安全改装插件，若存在则增加射程
 *
 * @author catleafleaf
 * @version 2025-04-12 08:28:29
 */
public class SpiderRange implements EveryFrameWeaponEffectPlugin {

    // 核心参数
    private static final String HULLMOD_ID = "safetyoverrides";     // 需要检测的插件ID
    private static final String WEAPON_ID = "MCS_SpiderWire";       // 目标武器ID
    private static final float RANGE_BONUS = 2100f;                 // 射程加成

    // 状态追踪
    private boolean wasActive = false;
    private boolean initialized = false;
    private float baseRange = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null) return;

        // 获取所属舰船
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        // 初始化基础射程
        if (!initialized) {
            initialize(weapon);
        }


        boolean hasHullMod = ship.getVariant().hasHullMod(HULLMOD_ID);
        if (weapon.getId().equals(WEAPON_ID)) {
            if (hasHullMod && !wasActive) {
                // 激活射程加成
                weapon.getSpec().getMaxRange();
                weapon.getSpec().setMaxRange(baseRange + RANGE_BONUS);
                wasActive = true;
            } else if (!hasHullMod && wasActive) {
                // 移除射程加成
                weapon.getSpec().setMaxRange(baseRange);
                wasActive = false;
            }
        }
    }

    private void initialize(WeaponAPI weapon) {
        if (weapon.getId().equals(WEAPON_ID)) {
            WeaponSpecAPI spec = weapon.getSpec();
            baseRange = spec.getMaxRange();
        }
        initialized = true;
    }
}