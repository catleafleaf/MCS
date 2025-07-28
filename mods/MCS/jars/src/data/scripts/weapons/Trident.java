package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

/**
 * 相位穿透导弹的武器脚本
 * - 可以击中相位状态的舰船
 * - 附带EMP伤害和视觉效果
 *
 * @author catleafleaf
 * @version 1.0.0
 * @since 2025-04-04 08:51:10
 */
public class Trident implements EveryFrameWeaponEffectPlugin {

    private static final Color EXPLOSION_COLOR = new Color(100, 50, 255, 255);
    private static final Color PARTICLE_COLOR = new Color(150, 100, 255, 255);
    private static final float EXPLOSION_RADIUS = 100f;
    private static final float EXPLOSION_DURATION = 0.2f;
    private static final float BASE_DAMAGE = 2000f; // 基础伤害值

    private boolean initialized = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) return;

        // 初始化时设置武器属性
        if (!initialized) {
            initialized = true;
            weapon.ensureClonedSpec();
        }

        // 获取所有导弹
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            if (!(proj instanceof MissileAPI) || proj.getWeapon() != weapon) continue;
            if (proj.isFading()) continue;

            // 设置导弹属性（每帧）
            proj.setCollisionClass(CollisionClass.PROJECTILE_NO_FF);

            // 检查附近的舰船
            for (ShipAPI ship : engine.getShips()) {
                if (ship.getHitpoints() <= 0 || !ship.isPhased()) continue;

                // 检查是否命中
                float distance = Vector2f.sub(proj.getLocation(), ship.getLocation(), new Vector2f()).length();
                if (distance <= ship.getCollisionRadius()) {
                    // 创建爆炸效果
                    engine.spawnExplosion(
                            proj.getLocation(),    // 位置
                            new Vector2f(0, 0),    // 速度
                            EXPLOSION_COLOR,       // 颜色
                            EXPLOSION_RADIUS,      // 半径
                            EXPLOSION_DURATION     // 持续时间
                    );

                    // 添加视觉效果
                    for (int i = 0; i < 20; i++) {
                        float angle = (float) (Math.random() * 360);
                        float dist = (float) (Math.random() * EXPLOSION_RADIUS);
                        float dx = (float) Math.cos(Math.toRadians(angle)) * dist;
                        float dy = (float) Math.sin(Math.toRadians(angle)) * dist;

                        Vector2f particleLoc = new Vector2f(
                                proj.getLocation().x + dx,
                                proj.getLocation().y + dy
                        );

                        engine.addSmoothParticle(
                                particleLoc,
                                new Vector2f(dx * 2, dy * 2),
                                20f, // 大小
                                1f,  // 亮度
                                0.5f, // 持续时间
                                PARTICLE_COLOR
                        );
                    }

                    // 对目标造成伤害
                    engine.applyDamage(
                            ship,                  // 目标
                            ship.getLocation(),    // 位置
                            BASE_DAMAGE,           // 伤害值
                            DamageType.HIGH_EXPLOSIVE, // 伤害类型
                            BASE_DAMAGE * 0f,   // EMP伤害
                            false,                 // 是否穿透护盾
                            true,                  // 是否击中船体
                            proj.getSource()       // 伤害来源
                    );

                    // 移除导弹
                    engine.removeEntity(proj);
                    break;
                }
            }
        }
    }
}