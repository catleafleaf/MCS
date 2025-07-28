package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import com.fs.starfarer.api.combat.DamageType;

public class EggOnhit implements OnHitEffectPlugin {

    @Override
    public void onHit(final DamagingProjectileAPI projectile, CombatEntityAPI target, final Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine) {
        Random random = new Random();
        float roll = random.nextFloat();
        int spawnCount = 0;

        if(roll < 0.0015) { // 0.5%
            spawnCount = 4;
        } else if(roll < 0.005) { // 1%
            spawnCount = 3;
        } else if(roll < 0.01) { // 3.5%
            spawnCount = 2;
        } else if(roll < 0.02) { // 5%
            spawnCount = 1;
        }

        if(spawnCount > 0) {
            final int finalSpawnCount = spawnCount;
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                private int remainingSpawns = finalSpawnCount;
                private boolean done = false;
                private List<ShipAPI> spawnedShips = new ArrayList<>();
                private float elapsed = 0f;

                public void advance(float amount, List<InputEventAPI> events) {
                    if (!done) {
                        CombatFleetManagerAPI cfm = engine.getFleetManager(projectile.getSource().getOriginalOwner());
                        cfm.setSuppressDeploymentMessages(true);

                        while(remainingSpawns > 0) {
                            ShipAPI spawned = cfm.spawnShipOrWing("MCS_Chicken_wing_wing", point, 0.0F);
                            if (spawned != null) {
                                spawnedShips.add(spawned);
                            }
                            remainingSpawns--;
                        }

                        cfm.setSuppressDeploymentMessages(false);
                        done = true;
                    }

                    if (!spawnedShips.isEmpty()) {
                        elapsed += amount;
                        if (elapsed >= 10f) { // 改为10秒
                            for (ShipAPI ship : spawnedShips) {
                                if (ship != null && !ship.isHulk() && engine.isEntityInPlay(ship)) {
                                    engine.applyDamage(ship, ship.getLocation(),
                                            99999f,
                                            DamageType.HIGH_EXPLOSIVE,
                                            0f,
                                            true,
                                            false,
                                            null);
                                }
                            }
                            engine.removePlugin(this);
                        }
                    }
                }
            });
        }
    }
}