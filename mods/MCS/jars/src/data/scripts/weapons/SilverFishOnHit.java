// Created by catleafleaf on 2025-05-11 15:03:01
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

/**
 * 银鱼武器命中效果
 * - 命中装甲或结构时额外造成高爆伤害
 */
public class SilverFishOnHit implements OnHitEffectPlugin {
    private static final float EXTRA_DAMAGE = 100f; // 额外伤害值

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // 如果击中护盾或目标不是舰船，则直接返回
        if (shieldHit || !(target instanceof ShipAPI)) return;

        ShipAPI ship = (ShipAPI) target;

        // 生成高爆伤害
        engine.applyDamage(
                target,      // 目标
                point,       // 命中点
                EXTRA_DAMAGE,// 伤害值
                DamageType.HIGH_EXPLOSIVE, // 高爆伤害类型
                0f,         // EMP伤害
                false,      // 是否绕过护盾
                true,      // 是否由玩家造成
                projectile.getSource()  // 伤害来源
        );

        // 添加命中特效
        engine.addHitParticle(
                point,
                ship.getVelocity(),
                100f,
                1f,
                0.15f,
                new Color(255, 140, 0) // 橙色
        );
    }
}