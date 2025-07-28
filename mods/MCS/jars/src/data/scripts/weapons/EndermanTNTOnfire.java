// Created by catleafleaf on 2025-05-10
// Last updated on 2025-05-10 09:52:43 UTC
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

/**
 * 末影人TNT发射效果插件
 * - 飞行3秒后引爆
 * - 引爆后立即清除弹体
 *
 * @author catleafleaf
 * @version 2025-05-10 09:52:43
 */
public class EndermanTNTOnfire implements OnFireEffectPlugin {

    private static final float DETONATION_TIME = 3.0f;      // 引爆时间(秒)
    private static final Color EXPLOSION_COLOR = Color.WHITE;
    private static final float EXPLOSION_RADIUS = 500f;     // 爆炸范围

    private float timer = 0f;

    @Override
    public void onFire(final DamagingProjectileAPI projectile, WeaponAPI weapon, final CombatEngineAPI engine) {
        if (projectile == null || engine == null) return;

        timer = 0f;
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, java.util.List<InputEventAPI> events) {
                if (!engine.isEntityInPlay(projectile)) {
                    engine.removePlugin(this);
                    return;
                }

                timer += amount;

                if (timer >= DETONATION_TIME) {
                    detonate(projectile, engine);
                    engine.removePlugin(this);
                }
            }
        });
    }

    private void detonate(DamagingProjectileAPI projectile, CombatEngineAPI engine) {
        Vector2f loc = projectile.getLocation();
        float damage = projectile.getDamageAmount();

        // 创建爆炸特效
        engine.spawnExplosion(
                loc,
                new Vector2f(0, 0),
                EXPLOSION_COLOR,
                EXPLOSION_RADIUS,
                1.0f
        );

        // 生成范围伤害
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.1f,
                EXPLOSION_RADIUS,
                EXPLOSION_RADIUS/2,
                damage,
                damage/2,
                CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
                CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
                10f,
                10f,
                0.5f,
                20,
                EXPLOSION_COLOR,
                EXPLOSION_COLOR
        );

        engine.spawnDamagingExplosion(spec, projectile.getSource(), loc);

        // 完全清除弹体
        engine.removeEntity(projectile);
    }
}