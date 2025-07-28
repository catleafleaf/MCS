package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.*;

public class PhantomClone extends BaseHullMod {
    private static final float SPAWN_RANGE = 200f;
    private static final float TRIGGER_INTERVAL = 90f;
    private static final Color CLONE_COLOR = new Color(100, 100, 255, 155);
    private static final Color INFO_COLOR = new Color(150, 150, 255, 200);

    private final Map<String, Float> timers = new HashMap<>();
    private final Map<String, Float> lastPeakTime = new HashMap<>();
    private final Map<String, Float> lastCR = new HashMap<>();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive() || Global.getCombatEngine() == null) return;

        String shipId = ship.getId();
        CombatEngineAPI engine = Global.getCombatEngine();

        float currentPeak = ship.getMutableStats().getPeakCRDuration().computeEffective(0f);
        float currentDeployed = ship.getTimeDeployedForCRReduction();
        float remainingPeak = currentPeak - currentDeployed;
        float currentCR = ship.getCurrentCR();

        if (!lastPeakTime.containsKey(shipId)) {
            lastPeakTime.put(shipId, remainingPeak);
            timers.put(shipId, 0f);
            lastCR.put(shipId, currentCR);
            return;
        }

        float previousPeak = lastPeakTime.get(shipId);
        float previousCR = lastCR.get(shipId);
        float timer = timers.get(shipId);

        if (remainingPeak < previousPeak && currentCR >= previousCR) {
            timer += amount;

            if (engine.getTotalElapsedTime(false) % 1f < amount) {
                engine.addFloatingText(
                        ship.getLocation(),
                        String.format("计时: %.1f", timer),
                        15f,
                        INFO_COLOR,
                        ship,
                        0.5f,
                        1.0f
                );
            }

            if ((int)(timer / TRIGGER_INTERVAL) > (int)((timer - amount) / TRIGGER_INTERVAL)) {
                spawnClone(ship, engine);
            }
        }

        lastPeakTime.put(shipId, remainingPeak);
        lastCR.put(shipId, currentCR);
        timers.put(shipId, timer);
    }

    private void spawnClone(ShipAPI originalShip, CombatEngineAPI engine) {
        Vector2f spawnLocation = MathUtils.getRandomPointInCircle(
                originalShip.getLocation(),
                SPAWN_RANGE
        );

        FleetMemberAPI member = originalShip.getFleetMember();
        if (member != null) {
            ShipAPI clone = engine.getFleetManager(originalShip.getOwner()).spawnFleetMember(
                    member,
                    spawnLocation,
                    originalShip.getFacing(),
                    1f
            );

            engine.addFloatingText(
                    spawnLocation,
                    "复制",
                    20f,
                    CLONE_COLOR,
                    clone,
                    0.5f,
                    1.0f
            );
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)TRIGGER_INTERVAL;
        if (index == 1) return "" + (int)SPAWN_RANGE;
        return null;
    }
}