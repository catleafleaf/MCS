
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.List;

public class ShulkerOnHit implements OnHitEffectPlugin {
    private static final float BUFF_DURATION = 5f; // buff基础持续时间

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit || !(target instanceof ShipAPI)) {
            if (shieldHit) {
                engine.addFloatingText(point, "护盾抵挡!", 20f, Color.CYAN, target, 0.5f, 1.0f);
            }
            return;
        }

        ShipAPI ship = (ShipAPI) target;
        boolean hasExistingEffect = ship.getMutableStats().getMaxSpeed().getFlatStatMod("shulker_levitation") != null;

        if (hasExistingEffect) {
            extendLevitationEffect(ship, engine, point);
        } else {
            applyLevitationEffect(ship, engine, point);
        }
    }

    private void applyLevitationEffect(final ShipAPI ship, final CombatEngineAPI engine, Vector2f point) {
        ship.getMutableStats().getMaxSpeed().modifyFlat("shulker_levitation", 50f);
        ship.getMutableStats().getAcceleration().modifyFlat("shulker_levitation", 100f);

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                elapsed += amount;
                if (elapsed >= BUFF_DURATION) {
                    ship.getMutableStats().getMaxSpeed().unmodify("shulker_levitation");
                    ship.getMutableStats().getAcceleration().unmodify("shulker_levitation");
                    engine.removePlugin(this);
                }

                if (elapsed % 0.25f < 0.1f) {
                    engine.addHitParticle(
                            MathUtils.getRandomPointInCircle(ship.getLocation(), 50f),
                            new Vector2f(0f, 50f),
                            5f,
                            1f,
                            0.1f,
                            new Color(255, 192, 203)
                    );
                }
            }
        });

        engine.addFloatingText(point, "Levitation!", 30f, new Color(255, 192, 203), ship, 0.5f, 1.0f);
    }

    private void extendLevitationEffect(ShipAPI ship, CombatEngineAPI engine, Vector2f point) {
        ShulkerEveryFrame.notifyEffectExtension(ship);

        engine.addFloatingText(point, "Duration Extended!", 25f, new Color(255, 192, 203), ship, 0.5f, 1.0f);

        for (int i = 0; i < 5; i++) {
            Vector2f particlePos = MathUtils.getRandomPointInCircle(point, 30f);
            engine.addHitParticle(
                    particlePos,
                    new Vector2f(0f, 40f),
                    7f,
                    1f,
                    0.3f,
                    new Color(255, 215, 230)
            );
        }
    }
}