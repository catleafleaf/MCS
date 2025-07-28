// Created by catleafleaf on 2025-05-14 13:18:04
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.*;

public class ShulkerEveryFrame implements EveryFrameWeaponEffectPlugin {
    private final Map<ShipAPI, Float> originalMassMap = new HashMap<>();
    private final Map<ShipAPI, Float> durationMap = new HashMap<>();
    private static final Map<ShipAPI, Boolean> pendingExtensions = new HashMap<>();

    private static final float BASE_DURATION = 5f;     // 基础持续时间
    private static final float MASS_REDUCTION_RATIO = 0.1f;
    private static final float SPEED_REDUCTION = -20f; // 航速减少百分比

    public static void notifyEffectExtension(ShipAPI ship) {
        pendingExtensions.put(ship, true);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) return;

        cleanupInvalidEntities(engine);
        processPendingExtensions();
        checkNewAffectedShips(engine);
        updateExistingEffects(amount, engine);
    }

    private void cleanupInvalidEntities(CombatEngineAPI engine) {
        Iterator<Map.Entry<ShipAPI, Float>> it = originalMassMap.entrySet().iterator();
        while (it.hasNext()) {
            ShipAPI ship = it.next().getKey();
            if (!isValidTarget(ship, engine)) {
                durationMap.remove(ship);
                it.remove();
                ship.getMutableStats().getMaxSpeed().unmodify("shulker_speed");
            }
        }
    }

    private boolean isValidTarget(ShipAPI ship, CombatEngineAPI engine) {
        return ship != null && engine.isEntityInPlay(ship) && !ship.isHulk() && ship.isAlive();
    }

    private void processPendingExtensions() {
        for (Map.Entry<ShipAPI, Boolean> entry : pendingExtensions.entrySet()) {
            ShipAPI ship = entry.getKey();
            if (durationMap.containsKey(ship)) {
                float currentDuration = durationMap.get(ship);
                float newDuration = currentDuration + BASE_DURATION;
                durationMap.put(ship, newDuration);
            }
        }
        pendingExtensions.clear();
    }


    private void checkNewAffectedShips(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (isValidTarget(ship, engine) && hasLevitationEffect(ship) && !originalMassMap.containsKey(ship)) {
                float originalMass = ship.getMass();
                originalMassMap.put(ship, originalMass);
                durationMap.put(ship, BASE_DURATION);
                ship.setMass(originalMass * MASS_REDUCTION_RATIO);

                // 添加航速减少效果
                ship.getMutableStats().getMaxSpeed().modifyPercent("shulker_speed", SPEED_REDUCTION);

                engine.addFloatingText(ship.getLocation(), "Levitation!", 20f, Color.PINK, ship, 0.5f, 1.0f);
            }
        }
    }

    private void updateExistingEffects(float amount, CombatEngineAPI engine) {
        Iterator<Map.Entry<ShipAPI, Float>> it = durationMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ShipAPI, Float> entry = it.next();
            ShipAPI ship = entry.getKey();
            float remainingTime = entry.getValue() - amount;

            if (remainingTime > 0) {
                entry.setValue(remainingTime);
                if (Math.random() < 0.1) {
                    addFloatingParticle(ship, engine);
                }
            } else {
                applyEndEffect(ship, engine);
                it.remove();
            }
        }
    }

    private boolean hasLevitationEffect(ShipAPI ship) {
        return ship.getMutableStats().getMaxSpeed().getFlatStatMod("shulker_levitation") != null;
    }

    private void addFloatingParticle(ShipAPI ship, CombatEngineAPI engine) {
        Vector2f particlePos = MathUtils.getRandomPointInCircle(ship.getLocation(), 50f);
        engine.addHitParticle(
                particlePos,
                new Vector2f(0f, 50f),
                5f,
                1f,
                0.1f,
                new Color(255, 192, 203)
        );
    }

    private void applyEndEffect(ShipAPI ship, CombatEngineAPI engine) {
        if (!originalMassMap.containsKey(ship)) return;

        float originalMass = originalMassMap.get(ship);
        float massDiff = ship.getMass();
        float damage = massDiff * BASE_DURATION;

        ship.setMass(originalMass);
        ship.getMutableStats().getMaxSpeed().unmodify("shulker_speed");

        engine.applyDamage(
                ship,
                ship.getLocation(),
                damage,
                DamageType.KINETIC,
                0f,
                false,
                true,
                null
        );

        addEndEffectVisuals(ship, engine, damage);
        originalMassMap.remove(ship);
    }

    private void addEndEffectVisuals(ShipAPI ship, CombatEngineAPI engine, float damage) {
        engine.addFloatingText(
                ship.getLocation(),
                String.format("Impact: %.0f", damage),
                30f,
                Color.CYAN,
                ship,
                0.5f,
                1.0f
        );

        engine.addHitParticle(
                ship.getLocation(),
                new Vector2f(),
                100f,
                1f,
                0.5f,
                Color.CYAN
        );

        for (int i = 0; i < 8; i++) {
            Vector2f particlePos = MathUtils.getRandomPointInCircle(ship.getLocation(), 50f);
            Vector2f particleVel = MathUtils.getPointOnCircumference(null,
                    MathUtils.getRandomNumberInRange(50f, 100f),
                    MathUtils.getRandomNumberInRange(0f, 360f));

            engine.addHitParticle(
                    particlePos,
                    particleVel,
                    MathUtils.getRandomNumberInRange(5f, 15f),
                    1f,
                    0.5f,
                    Color.CYAN
            );
        }
    }
}