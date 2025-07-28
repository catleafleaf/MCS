// Created by catleafleaf on 2025-04-20
// Last updated on 2025-04-20 04:46:04 UTC
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

/**
 * 复仇之斧武器插件
 * 50%概率在命中护盾时强制关闭目标护盾
 *
 * @author catleafleaf
 * @version 2025-04-20 04:46:04
 * @since 2025-04-20
 */
public class VindicatorAxePlugin implements OnHitEffectPlugin {

    private static final float SHIELD_OFF_CHANCE = 0.5f; // 50%概率关闭护盾

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI)) {
            return;
        }

        ShipAPI targetShip = (ShipAPI) target;
        if (shieldHit && targetShip.getShield() != null && targetShip.getShield().isOn()) {
            // 添加50%概率判定
            if (Math.random() < SHIELD_OFF_CHANCE) {
                forceShieldOff(targetShip);
            }
        }
    }

    /**
     * 强制关闭目标舰船的护盾
     *
     * @param ship 目标舰船
     */
    private void forceShieldOff(ShipAPI ship) {
        if (ship.getShield() != null && !ship.getShield().isOff()) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        }
    }
}