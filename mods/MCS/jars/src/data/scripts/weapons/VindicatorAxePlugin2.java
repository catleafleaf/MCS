package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

/**
 * VindicatorAxe武器效果插件
 * - 弹体射出后中速旋转
 *
 * @author catleafleaf
 * @version 1.0.0
 * @since 2025-03-30 09:54:31
 */
public class VindicatorAxePlugin2 implements OnFireEffectPlugin {

    // 弹体旋转速度(度/秒)
    private static final float PROJECTILE_ROTATION_SPEED = 540f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile != null) {
            // 设置弹体的旋转速度
            projectile.setAngularVelocity(PROJECTILE_ROTATION_SPEED);
        }
    }
}