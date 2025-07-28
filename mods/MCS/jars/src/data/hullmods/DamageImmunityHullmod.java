// Created by catleafleaf on 2025-05-10
// Last updated on 2025-05-10 12:03:33 UTC
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;


public class DamageImmunityHullmod extends BaseHullMod {
    private static final float QUANTUM_VALUE = 64f;
    private static final float MINIMUM_DAMAGE = 16f;
    private static final Color EFFECT_COLOR = new Color(180, 100, 255);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new BlockDamageListener());
    }

    private static class BlockDamageListener implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target,
                                        DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!(target instanceof ShipAPI)) return null;

            float originalDamage = damage.getDamage();
            if (originalDamage <= 0) return null;

            // 量子化伤害，但确保至少造成16点
            float quantizedDamage = QUANTUM_VALUE * (float)Math.floor(originalDamage / QUANTUM_VALUE);
            if (quantizedDamage < MINIMUM_DAMAGE && originalDamage >= MINIMUM_DAMAGE) {
                quantizedDamage = MINIMUM_DAMAGE;
            }

            // 应用量子化伤害
            damage.getModifier().modifyMult("block_defense", quantizedDamage / originalDamage);

            // 只在伤害被实际量化时显示效果
            if (Math.abs(quantizedDamage - originalDamage) > 0.1f) {
                addQuantizationEffect(point);
            }

            return null;
        }

        private static void addQuantizationEffect(Vector2f point) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;

            // 中心扩散效果
            engine.addHitParticle(
                    point,
                    new Vector2f(0, 0),
                    60f,
                    1f,
                    0.1f,
                    EFFECT_COLOR
            );

            // 量子化视觉效果
            for (int i = 0; i < 4; i++) {
                float angle = i * (float)Math.PI / 2f;
                Vector2f vel = new Vector2f(
                        (float)Math.cos(angle) * 50f,
                        (float)Math.sin(angle) * 50f
                );

                engine.addHitParticle(
                        point,
                        vel,
                        5f,
                        0.8f,
                        0.4f,
                        EFFECT_COLOR
                );
            }
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        // 在状态栏显示状态
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "block_defense",
                    "graphics/icons/hullsys/damper_field.png",
                    "方块防御",
                    String.format("伤害量化为%d的倍数", (int)QUANTUM_VALUE),
                    false
            );
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return String.format("%d", (int)QUANTUM_VALUE);
        if (index == 1) return String.format("%d", (int)MINIMUM_DAMAGE);
        return null;
    }
}