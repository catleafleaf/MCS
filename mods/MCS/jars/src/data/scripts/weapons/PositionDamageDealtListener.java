package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import data.scripts.plugins.MCSPositionPlugin;
import org.lwjgl.util.vector.Vector2f;

/**
 * Position武器的伤害监听器
 * @author catleafleaf
 * @version 2025-05-21 14:35:50
 */
public class PositionDamageDealtListener implements DamageDealtModifier {
    private final ShipAPI ship;

    public PositionDamageDealtListener(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!MCSPositionPlugin.couldAddPosition(target, shieldHit)) return null;

        WeaponAPI weapon;
        if (param instanceof BeamAPI) {
            weapon = ((BeamAPI) param).getWeapon();
        }
        else if (param instanceof DamagingProjectileAPI) {
            weapon = ((DamagingProjectileAPI) param).getWeapon();
        }
        else {
            return null;
        }

        if (weapon == null || weapon.getShip() != ship) return null;

        MCSPositionPlugin.addPosition(
                weapon.getShip(),
                (ShipAPI)target,
                weapon.getSize(),
                point
        );

        return "position";
    }
}