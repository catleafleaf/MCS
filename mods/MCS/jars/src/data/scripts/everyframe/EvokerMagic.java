package data.scripts.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

public class EvokerMagic implements EveryFrameWeaponEffectPlugin {
    private Set<String> trackedFighterIds = new HashSet<>();
    private static final float PARTICLE_DURATION = 0.15f;
    private static final int PARTICLES_PER_FIRE = 50;
    private static final Color PARTICLE_COLOR = new Color(120, 20, 20, 255);
    private static final float MIN_X = -150f;
    private static final float MAX_X = 150f;
    private static final float MIN_Y = 0f;
    private static final float MAX_Y = 400f;
    private static final float MIN_SIZE = 20f;
    private static final float MAX_SIZE = 40f;
    private List<Particle> activeParticles = new ArrayList<>();
    private Random rand = new Random();
    private List<String> particleSprites = new ArrayList<>();
    {
        for (int i = 0; i <= 6; i++) {
            particleSprites.add(String.format("graphics/fx/MCS_effect/MCS_effect%02d.png", i));
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        ShipAPI ship = weapon.getShip();
        if (ship == null || !ship.hasLaunchBays()) return;

        Set<String> currentFighterIds = new HashSet<>();


        for (FighterWingAPI wing : ship.getAllWings()) {
            // 遍历当前翼队中的所有战机
            for (ShipAPI fighter : wing.getWingMembers()) {
                if (fighter.isAlive()) {
                    String fighterId = fighter.getFleetMemberId();
                    currentFighterIds.add(fighterId);

                    // 如果这是一个新的战机且属于本船
                    if (!trackedFighterIds.contains(fighterId) && fighter.getWing().getSourceShip() == ship) {
                        trackedFighterIds.add(fighterId);
                        weapon.setForceFireOneFrame(true);
                        createParticles(weapon, engine);
                    }
                }
            }
        }


        trackedFighterIds = currentFighterIds;


        updateAndRenderParticles(amount, engine, weapon);
    }

    private void createParticles(WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f weaponLocation = weapon.getLocation();
        float weaponFacing = weapon.getCurrAngle();

        for (int i = 0; i < PARTICLES_PER_FIRE; i++) {
            // 随机选择一张粒子贴图(MCS_effect00.png 到 MCS_effect06.png)
            String spriteId = particleSprites.get(rand.nextInt(7));

            // 在指定区域内随机生成位置
            float x = MIN_X + rand.nextFloat() * (MAX_X - MIN_X);
            float y = MIN_Y + rand.nextFloat() * (MAX_Y - MIN_Y);

            // 坐标系转换：从武器局部坐标转换到世界坐标
            float cos = (float) Math.cos(Math.toRadians(weaponFacing));
            float sin = (float) Math.sin(Math.toRadians(weaponFacing));
            float worldX = weaponLocation.x + x * cos - y * sin;
            float worldY = weaponLocation.y + x * sin + y * cos;

            // 创建新粒子
            Particle particle = new Particle(
                    new Vector2f(worldX, worldY),
                    MIN_SIZE + rand.nextFloat() * (MAX_SIZE - MIN_SIZE),
                    spriteId,
                    PARTICLE_DURATION
            );

            activeParticles.add(particle);
        }
    }

    private void updateAndRenderParticles(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        for (int i = activeParticles.size() - 1; i >= 0; i--) {
            Particle particle = activeParticles.get(i);
            particle.timeLeft -= amount;

            // 移除已经结束的粒子
            if (particle.timeLeft <= 0) {
                activeParticles.remove(i);
                continue;
            }

            // 计算当前透明度
            float alpha = particle.timeLeft / PARTICLE_DURATION;
            Color currentColor = Misc.setAlpha(PARTICLE_COLOR, (int)(255 * alpha));

            // 渲染粒子
            renderParticle(particle.loc.x, particle.loc.y, particle.size, particle.size,
                    currentColor, particle.spriteId, engine);
        }
    }


    private void renderParticle(float x, float y, float width, float height,
                                Color color, String spriteId, CombatEngineAPI engine) {
        SpriteAPI sprite = Global.getSettings().getSprite(spriteId);
        sprite.setColor(color);
        sprite.setSize(width, height);
        sprite.renderAtCenter(x, y);
    }

    private static class Particle {
        Vector2f loc;      // 位置
        float size;        // 大小
        String spriteId;   // 贴图ID
        float timeLeft;    // 剩余持续时间

        Particle(Vector2f loc, float size, String spriteId, float duration) {
            this.loc = loc;
            this.size = size;
            this.spriteId = spriteId;
            this.timeLeft = duration;
        }
    }
}