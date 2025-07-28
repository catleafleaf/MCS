/*
 * Author: catleafleaf
 * Current Date: 2025-05-31 02:39:14 UTC
 */

package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

public class MCSSlimeSplitPlugin extends BaseEveryFrameCombatPlugin {
    public static final String PLUGIN_KEY = "MCS_SlimeSplit_key";
    private static final float FADE_DURATION = 1f;
    private static final float SPLIT_DELAY = 2f;
    private static final String SOURCE_CRUISER = "MCS_Slime";
    private static final String SOURCE_FRIGATE = "MCS_SlimeTiny";
    private static final String SPLIT_FRIGATE = "MCS_SlimeTiny_variant";
    private static final String SPLIT_FIGHTER = "MCS_SlimeFighter_variant";

    private final Map<ShipAPI, SplitData> splitMap = new HashMap<>();
    private CombatEngineAPI engine;
    private final Random rand = new Random();

    private static class SplitData {
        boolean isDone = false;
        float splitTimer = 0f;
        float originalAlpha;
        boolean isDisabled = false;

        public SplitData(ShipAPI ship) {
            this.originalAlpha = ship.getSpriteAPI().getAlphaMult();
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        if (engine != null) {
            this.splitMap.clear();
            engine.getCustomData().put(PLUGIN_KEY, this);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) {
            engine = Global.getCombatEngine();
            if (engine == null) return;
        }

        if (engine.isPaused()) return;

        List<ShipAPI> shipsToRemove = new ArrayList<>();
        List<ShipAPI> shipsToProcess = new ArrayList<>();

        for (Map.Entry<ShipAPI, SplitData> entry : splitMap.entrySet()) {
            ShipAPI ship = entry.getKey();
            SplitData data = entry.getValue();

            if (!ship.isHulk() || !engine.isEntityInPlay(ship)) {
                shipsToRemove.add(ship);
                continue;
            }
            if (data.isDone) {
                shipsToRemove.add(ship);
                continue;
            }

            if (!data.isDisabled) {
                initSplitState(ship);
                data.isDisabled = true;
            }

            data.splitTimer += amount;

            if (data.splitTimer < SPLIT_DELAY) {
                ship.setHitpoints(ship.getMaxHitpoints());
                ship.getMutableStats().getHullDamageTakenMult().modifyMult("SlimeSplitInvuln", 0f);
                continue;
            }

            float fadeProgress = (data.splitTimer - SPLIT_DELAY) / FADE_DURATION;
            if (fadeProgress > 1f) fadeProgress = 1f;
            ship.getSpriteAPI().setAlphaMult(data.originalAlpha * (1f - fadeProgress));

            if (fadeProgress >= 1f) {
                shipsToProcess.add(ship);
                shipsToRemove.add(ship);
            }
        }

        for (ShipAPI ship : shipsToProcess) {
            String hullId = ship.getHullSpec().getHullId();
            if (SOURCE_FRIGATE.equals(hullId)) {
                handleFrigateSplit(ship);
            } else if (SOURCE_CRUISER.equals(hullId)) {
                handleSplit(ship);
            }
            splitMap.get(ship).isDone = true;
        }

        for (ShipAPI ship : shipsToRemove) {
            splitMap.remove(ship);
            ship.getMutableStats().getHullDamageTakenMult().unmodify("SlimeSplitInvuln");
        }
    }

    private void initSplitState(ShipAPI ship) {
        disableShip(ship);

        if (ship.getOriginalOwner() == 0 || ship.getOriginalOwner() == 1) {
            engine.setCombatNotOverForAtLeast(SPLIT_DELAY + FADE_DURATION + 1f);
        }
    }

    private void handleFrigateSplit(ShipAPI frigate) {
        if (engine == null) return;

        int fighterCount = 2 + rand.nextInt(2);
        float radiusScale = 0.3f;
        float angleStep = 360f / fighterCount;
        float startAngle = rand.nextFloat() * 360f;

        int owner = frigate.getOriginalOwner();
        if (owner != 0 && owner != 1) return;

        CombatFleetManagerAPI fleetManager = engine.getFleetManager(owner);
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);

        PersonAPI wingCommander = Global.getSettings().createPerson();
        wingCommander.setPersonality(Personalities.RECKLESS);

        for (int i = 0; i < fighterCount; i++) {
            float angle = startAngle + angleStep * i;
            Vector2f spawnPos = Misc.getUnitVectorAtDegreeAngle(angle);
            spawnPos.scale(frigate.getCollisionRadius() * radiusScale);
            Vector2f.add(spawnPos, frigate.getLocation(), spawnPos);

            ShipAPI fighter = fleetManager.spawnShipOrWing(
                    SPLIT_FIGHTER,
                    spawnPos,
                    angle,
                    0f,
                    wingCommander
            );

            if (fighter != null) {
                fighter.getSpriteAPI().setAlphaMult(0f);

                Vector2f velocity = new Vector2f(frigate.getVelocity());
                velocity.scale(0.5f);
                Vector2f extraVel = Misc.getUnitVectorAtDegreeAngle(angle);
                extraVel.scale(50f + rand.nextFloat() * 25f);
                Vector2f.add(velocity, extraVel, velocity);
                fighter.getVelocity().set(velocity);

                fighter.setCollisionClass(CollisionClass.FIGHTER);
                fighter.setOwner(owner);
                fighter.setOriginalOwner(owner);

                addFadeInEffect(fighter);
            }
        }

        fleetManager.setSuppressDeploymentMessages(wasSuppressed);
        Global.getSoundPlayer().playSound(
                "system_emp_emitter_impact",
                1f,
                1f,
                frigate.getLocation(),
                frigate.getVelocity()
        );

        engine.removeEntity(frigate);
    }

    private void handleSplit(ShipAPI original) {
        if (engine == null) return;

        int frigateCount = 3 + (rand.nextFloat() < 0.5f ? 1 : 0);
        float radiusScale = 0.5f;
        float angleStep = 360f / frigateCount;
        float startAngle = rand.nextFloat() * 360f;

        int owner = original.getOriginalOwner();
        if (owner != 0 && owner != 1) return;

        CombatFleetManagerAPI fleetManager = engine.getFleetManager(owner);
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);

        PersonAPI captain = Global.getSettings().createPerson();
        captain.setPersonality(Personalities.STEADY);

        for (int i = 0; i < frigateCount; i++) {
            float angle = startAngle + angleStep * i;
            Vector2f spawnPos = Misc.getUnitVectorAtDegreeAngle(angle);
            spawnPos.scale(original.getCollisionRadius() * radiusScale);
            Vector2f.add(spawnPos, original.getLocation(), spawnPos);

            ShipAPI frigate = fleetManager.spawnShipOrWing(
                    SPLIT_FRIGATE,
                    spawnPos,
                    angle,
                    0f,
                    captain
            );

            if (frigate != null) {
                frigate.getSpriteAPI().setAlphaMult(0f);

                Vector2f velocity = new Vector2f(original.getVelocity());
                velocity.scale(0.5f);
                frigate.getVelocity().set(velocity);

                frigate.setOwner(owner);
                frigate.setOriginalOwner(owner);

                addFadeInEffect(frigate);
                addSplit(frigate);
            }
        }

        fleetManager.setSuppressDeploymentMessages(wasSuppressed);
        Global.getSoundPlayer().playSound(
                "system_emp_emitter_impact",
                1f,
                1f,
                original.getLocation(),
                original.getVelocity()
        );

        engine.removeEntity(original);
    }

    private void disableShip(ShipAPI ship) {
        ship.getEngineController().forceFlameout();

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            weapon.disable();
        }

        if (ship.getSystem() != null) {
            ship.getSystem().deactivate();
        }

        ShipAIPlugin ai = ship.getShipAI();
        if (ai != null) {
            ai.cancelCurrentManeuver();
            ship.setShipAI(null);
        }

        ship.setAngularVelocity(0f);

        Vector2f currentVel = ship.getVelocity();
        currentVel.scale(0.5f);
        ship.getVelocity().set(currentVel);
    }

    private void addFadeInEffect(final ShipAPI ship) {
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float fadeIn = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (engine.isPaused()) return;
                fadeIn += amount;
                float alpha = Misc.interpolate(0f, 1f, Math.min(1f, fadeIn / FADE_DURATION));
                ship.getSpriteAPI().setAlphaMult(alpha);
                if (fadeIn >= FADE_DURATION) {
                    engine.removePlugin(this);
                }
            }
        });
    }

    public void addSplit(ShipAPI ship) {
        if (ship == null) return;
        if (!splitMap.containsKey(ship)) {
            splitMap.put(ship, new SplitData(ship));
        }
    }
}