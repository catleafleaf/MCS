// Created by catleafleaf on 2025-05-10 10:12:26
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.List;

public class EnderDefence extends BaseHullMod {
    private static final float TELEPORT_RANGE = 600f;
    private static final Color EFFECT_COLOR = new Color(138, 43, 226);
    private static final float HULL_DAMAGE_THRESHOLD = 0.1f;
    private static final float SEARCH_RANGE = 2000f;

    private float lastHullValue;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        lastHullValue = ship.getHitpoints();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        float currentHull = ship.getHitpoints();
        float maxHull = ship.getMaxHitpoints();

        if ((lastHullValue - currentHull) / maxHull >= HULL_DAMAGE_THRESHOLD) {
            Vector2f escapeVector = getEscapeVector(ship);

            // 计算新位置
            Vector2f newPos = new Vector2f(
                    ship.getLocation().x + escapeVector.x * TELEPORT_RANGE,
                    ship.getLocation().y + escapeVector.y * TELEPORT_RANGE
            );

            // 执行传送
            Vector2f oldPos = new Vector2f(ship.getLocation());
            ship.getLocation().set(newPos);

            spawnEffect(oldPos);
            spawnEffect(newPos);

            lastHullValue = currentHull;
        }
    }

    private Vector2f getEscapeVector(ShipAPI ship) {
        ShipAPI nearestShip = null;
        float shortestDistance = Float.MAX_VALUE;

        List<ShipAPI> nearbyShips = CombatUtils.getShipsWithinRange(ship.getLocation(), SEARCH_RANGE);
        for (ShipAPI other : nearbyShips) {
            if (other == ship || !other.isAlive() || other.isFighter()) continue;

            float distance = Vector2f.sub(other.getLocation(), ship.getLocation(), new Vector2f()).length();
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestShip = other;
            }
        }

        if (nearestShip != null) {
            Vector2f escapeDir = Vector2f.sub(
                    ship.getLocation(),
                    nearestShip.getLocation(),
                    new Vector2f()
            );
            float length = escapeDir.length();
            if (length > 0) {
                escapeDir.scale(1f / length);
                return escapeDir;
            }
        }

        // 如果没找到其他船或距离为0，随机方向
        float angle = (float)(Math.random() * Math.PI * 2);
        return new Vector2f(
                (float)Math.cos(angle),
                (float)Math.sin(angle)
        );
    }

    private void spawnEffect(Vector2f loc) {
        CombatEngineAPI engine = Global.getCombatEngine();

        engine.addHitParticle(
                loc,
                new Vector2f(0, 0),
                100f,
                1f,
                0.1f,
                EFFECT_COLOR
        );

        for (int i = 0; i < 12; i++) {
            float angle = i * 30f;
            Vector2f vel = new Vector2f(
                    (float)Math.cos(Math.toRadians(angle)) * 100f,
                    (float)Math.sin(Math.toRadians(angle)) * 100f
            );

            engine.addHitParticle(
                    loc,
                    vel,
                    5f,
                    0.8f,
                    0.5f,
                    EFFECT_COLOR
            );
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "10%";
        if (index == 1) return String.format("%.0f", TELEPORT_RANGE);
        return null;
    }
}