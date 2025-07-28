package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
public class BOWBeam implements BeamEffectPlugin {

    private static final float BASE_DAMAGE_MULT = 100f;       // 基础伤害倍率
    private static final float AMMO_DAMAGE_MULT = 100f;       // 每发弹药提供的伤害倍率
    private static final float EFFECT_DURATION = 0.7f;        // 效果持续时间
    private static final int MAX_AMMO = 50;                   // 最大弹药数限制
    private static final String DAMAGE_SOURCE = "bow_beam_damage";  // 伤害来源标识符

    private int lastAmmoCount = 0;      // 开火时的弹药数
    private float effectTimer = 0f;     // 效果计时器
    private boolean fired = false;      // 开火状态追踪

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // 基础检查
        if (engine == null || engine.isPaused() || beam == null) return;

        WeaponAPI weapon = beam.getWeapon();
        if (weapon == null) return;

        // 只处理BOW武器
        if (!"MCS_BOW".equals(weapon.getId())) return;

        // 开火检测和效果触发
        if (beam.getBrightness() >= 1f && !fired) {
            fired = true;
            lastAmmoCount = Math.min(weapon.getAmmo(), MAX_AMMO);
            effectTimer = EFFECT_DURATION;
        } else if (beam.getBrightness() < 1f) {
            fired = false;
        }

        // 效果计时和伤害调整
        if (effectTimer > 0) {
            effectTimer -= amount;
            // 计算总伤害倍率
            // 公式: 基础倍率 + (当前弹药量 * 每发弹药倍率)
            float damageMult = BASE_DAMAGE_MULT + (lastAmmoCount * AMMO_DAMAGE_MULT);
            beam.getDamage().getModifier().modifyPercent(DAMAGE_SOURCE, damageMult);
        }


    }
}