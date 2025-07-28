package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

/**
 * 舰船推进武器系统
 * @author catleafleaf
 * @version 2025-05-24 12:22:51
 */
public class MCSBoosterWeaponEveryFrame implements EveryFrameWeaponEffectPlugin {
    private static final float CHECK_TIME = 0.05f;
    private static final float BOOST_DURATION = 0.2f;
    private static final float BOOST_COOLDOWN = 0.7f;

    private static final float TARGET_SPEED = 150f;
    private static final float AI_RANGE = 500f;
    private static final float AI_ANGLE = 45f;
    private static final float BOOST_FORCE_MULT = 0.1f;

    private final IntervalUtil timer = new IntervalUtil(CHECK_TIME, CHECK_TIME);
    private boolean wasDisabled = false;
    private boolean isActive = false;
    private float currentBoostTime = 0f;
    private float cooldownTime = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon == null) return;

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        if (weapon.isDisabled()) {
            wasDisabled = true;
            resetBoostState();
            return;
        }

        if (wasDisabled && !weapon.isDisabled()) {
            wasDisabled = false;
            return;
        }

        timer.advance(amount);
        if (!timer.intervalElapsed()) return;

        if (cooldownTime > 0) {
            cooldownTime -= amount;
        }

        boolean shouldActivate = ship.isAlive() && cooldownTime <= 0 && (
                checkPlayerMovementIntent(ship) ||
                        (ship.getAIFlags() != null && checkAIMovementIntent(ship))
        );

        if (shouldActivate && !isActive) {
            startBoost(ship, weapon.getCurrAngle());
        } else if (isActive) {
            updateBoost(amount);
        }
    }

    private void resetBoostState() {
        isActive = false;
        currentBoostTime = 0f;
        cooldownTime = 0f;
    }

    private boolean checkPlayerMovementIntent(ShipAPI ship) {
        return ship.getEngineController().isAccelerating() ||
                ship.getEngineController().isTurningLeft() ||
                ship.getEngineController().isTurningRight() ||
                ship.getEngineController().isStrafingLeft() ||
                ship.getEngineController().isStrafingRight();
    }

    private boolean checkAIMovementIntent(ShipAPI ship) {
        ShipAPI target = ship.getShipTarget();
        if (target == null) return false;

        float distance = Vector2f.sub(target.getLocation(), ship.getLocation(), new Vector2f()).length();
        float angleDiff = Math.abs(ship.getFacing() -
                Vector2f.angle(target.getLocation(), ship.getLocation()));

        return distance <= AI_RANGE &&
                angleDiff <= AI_ANGLE &&
                (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PURSUING) ||
                        ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF) ||
                        ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.MAINTAINING_STRIKE_RANGE));
    }

    private void startBoost(ShipAPI ship, float angle) {
        float mass = ship.getMass();
        float currentSpeed = ship.getVelocity().length();
        float speedDiff = TARGET_SPEED - currentSpeed;

        if (speedDiff <= 0) return;

        float force = mass * speedDiff * BOOST_FORCE_MULT;
        Vector2f direction = new Vector2f(
                (float) Math.cos(Math.toRadians(angle)),
                (float) Math.sin(Math.toRadians(angle))
        );

        direction.scale(force);
        Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());

        isActive = true;
        currentBoostTime = 0f;
    }

    private void updateBoost(float amount) {
        currentBoostTime += amount;

        if (currentBoostTime >= BOOST_DURATION) {
            resetBoostState();
            cooldownTime = BOOST_COOLDOWN;
        }
    }
}