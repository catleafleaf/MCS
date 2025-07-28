package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MCSPositionPlugin;
import data.scripts.weapons.PositionDamageDealtListener;
import java.awt.Color;

/**
 * Position效果的HullMod
 * @author catleafleaf
 * @version 2025-05-21 14:35:50
 */
public class MCSTacticalBowHullMod extends BaseHullMod {
    private static final String ALT_SPRITES = "mcs_alt_sprites";
    private static final String HULL_ID = "MCS_Skeleton";
    private static final String WEAPON_ID = "MCS_BOW";
    private static final Color BORDER_COLOR = new Color(255, 200, 0);
    private static final Color TEXT_COLOR = new Color(255, 200, 0);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null || !HULL_ID.equals(ship.getHullSpec().getHullId())) return;

        // 检查是否有目标武器
        boolean hasTargetWeapon = false;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (WEAPON_ID.equals(weapon.getId())) {
                hasTargetWeapon = true;
                break;
            }
        }
        if (!hasTargetWeapon) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            engine.addFloatingText(ship.getLocation(),
                    "MCS位置战术模块已激活!", 30f, TEXT_COLOR, ship, 1.0f, 1.0f);
        }

        String spriteId = HULL_ID + "_position";
        SpriteAPI sprite = Global.getSettings().getSprite(ALT_SPRITES, spriteId);

        if (sprite != null) {
            float x = ship.getSpriteAPI().getCenterX();
            float y = ship.getSpriteAPI().getCenterY();
            float alpha = ship.getSpriteAPI().getAlphaMult();
            float angle = ship.getSpriteAPI().getAngle();
            Color color = ship.getSpriteAPI().getColor();

            ship.setSprite(ALT_SPRITES, spriteId);

            ship.getSpriteAPI().setCenter(x, y);
            ship.getSpriteAPI().setAlphaMult(alpha);
            ship.getSpriteAPI().setAngle(angle);
            ship.getSpriteAPI().setColor(color);

            if (engine != null) {
                engine.addFloatingText(ship.getLocation(),
                        "威力外观已应用!", 25f, TEXT_COLOR, ship, 0.5f, 0.5f);
            }
        }

        MCSPositionPlugin plugin = new MCSPositionPlugin();
        Global.getCombatEngine().addPlugin(plugin);
        ship.addListener(new PositionDamageDealtListener(ship));
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null || !HULL_ID.equals(ship.getHullSpec().getHullId())) return false;

        // 检查是否有目标武器
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (WEAPON_ID.equals(weapon.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship == null) return "无效的舰船";
        if (!HULL_ID.equals(ship.getHullSpec().getHullId())) {
            return "只能安装在" + HULL_ID + "舰船上";
        }
        // 检查武器
        boolean hasTargetWeapon = false;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (WEAPON_ID.equals(weapon.getId())) {
                hasTargetWeapon = true;
                break;
            }
        }
        if (!hasTargetWeapon) {
            return "需要装备" + WEAPON_ID + "武器";
        }
        return null;
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return HULL_ID;
        return null;
    }

    @Override
    public Color getBorderColor() {
        return BORDER_COLOR;
    }

    @Override
    public Color getNameColor() {
        return BORDER_COLOR;
    }
}