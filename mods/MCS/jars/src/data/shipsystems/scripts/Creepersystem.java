package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;

/**
 * 自爆系统脚本
 * - 激活后延迟3秒触发大爆炸
 * - 爆炸对周围舰船造成多重伤害
 * - 最后自毁
 * - 每0.5秒检测是否存活，若存活则再次执行自毁
 * @author catleafleaf
 * @version 2025-04-14 12:27:17
 */
public class Creepersystem extends BaseShipSystemScript {

    // 系统配置参数
    private static final float ACTIVATION_DELAY = 3f;        // 激活延迟时间
    private static final float TOTAL_DAMAGE = 1500f;        // 总伤害值
    private static final int DAMAGE_POINTS = 16;            // 伤害点数量
    private static final Color EXPLOSION_COLOR = new Color(255, 255, 255, 255); // 爆炸颜色
    private static final float SURVIVAL_CHECK_INTERVAL = 0.5f; // 存活检测间隔
    private static final float EXPLOSION_RANGE = 1500f;     // 爆炸范围限制

    // 内部状态
    private float timer = 0f;                               // 计时器
    private boolean triggered = false;                      // 是否已触发
    private float survivalCheckTimer = 0f;                 // 存活检测计时器
    private boolean hasExploded = false;                   // 是否已经发生过爆炸

    /**
     * 系统激活时的主要逻辑
     */
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null || ship.isHulk()) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        float elapsed = engine.getElapsedInLastFrame();
        timer += elapsed;

        // 首次触发爆炸
        if (!triggered && timer >= ACTIVATION_DELAY) {
            triggered = true;
            hasExploded = true;
            spawnOmniExplosion(ship, engine);              // 生成全方位爆炸效果
            applyDistributedDamage(ship, engine);          // 对周围目标造成伤害
            executeSelfDestruct(ship, engine);             // 执行自毁
        }

        // 已触发后的存活检测
        if (triggered) {
            survivalCheckTimer += elapsed;
            if (survivalCheckTimer >= SURVIVAL_CHECK_INTERVAL) {
                survivalCheckTimer = 0f;
                if (!ship.isHulk() && ship.getHitpoints() > 1f) {
                    // 如果还活着，继续自毁（但不再触发爆炸）
                    executeSelfDestruct(ship, engine);
                }
            }
        }
    }

    /**
     * 对目标应用护盾效果和伤害
     */
    private void applyDamageWithShieldEffect(ShipAPI target, CombatEngineAPI engine, ShipAPI source, Vector2f point) {
        // 如果命中护盾，生成电弧特效
        if (target.getShield() != null && target.getShield().isWithinArc(point)) {
            engine.spawnEmpArcVisual(
                    point,
                    source,
                    target.getLocation(),
                    target,
                    6f,                                    // 调整到60%：10f -> 6f
                    EXPLOSION_COLOR,
                    new Color(200, 240, 255, 150)
            );
        }

        // 应用复合伤害
        applyCompositeDamage(target, engine, source, point);
    }

    /**
     * 应用混合伤害（高爆40%，动能30%，能量30%）
     */
    private void applyCompositeDamage(ShipAPI target, CombatEngineAPI engine, ShipAPI source, Vector2f point) {
        // 高爆伤害（40%）
        engine.applyDamage(
                target,
                point,
                TOTAL_DAMAGE * 0.4f,
                DamageType.HIGH_EXPLOSIVE,
                0,
                true,
                false,
                source
        );

        // 动能伤害（30%）
        engine.applyDamage(
                target,
                point,
                TOTAL_DAMAGE * 0.3f,
                DamageType.KINETIC,
                0,
                false,
                false,
                source
        );

        // 能量伤害（30%）
        engine.applyDamage(
                target,
                point,
                TOTAL_DAMAGE * 0.3f,
                DamageType.ENERGY,
                0,
                false,
                false,
                source
        );
    }

    /**
     * 执行自毁逻辑
     */
    private void executeSelfDestruct(ShipAPI ship, CombatEngineAPI engine) {
        ship.setHitpoints(1f);
        engine.applyDamage(
                ship,
                ship.getLocation(),
                ship.getMaxHitpoints() * 3,
                DamageType.HIGH_EXPLOSIVE,
                0,
                true,
                false,
                ship
        );
    }

    /**
     * 生成全方位爆炸特效
     */
    private void spawnOmniExplosion(ShipAPI source, CombatEngineAPI engine) {
        Vector2f center = source.getLocation();
        float radius = source.getCollisionRadius();

        // 中心爆炸效果
        engine.spawnExplosion(
                center,
                new Vector2f(),
                EXPLOSION_COLOR,
                radius * 1.5f,                             // 调整到60%：2.5f -> 1.5f
                1.8f
        );

        // 环形粒子效果
        for (int i = 0; i < 24; i++) {
            float angle = 15f * i;
            Vector2f offset = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle)
                    .scale(radius * 0.6f);                 // 调整到60%：radius -> radius * 0.6f
            Vector2f.add(center, offset, offset);

            engine.addHitParticle(
                    offset,
                    (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle + 90f).scale(120f), // 调整到60%：200f -> 120f
                    radius * 0.48f,                        // 调整到60%：0.8f -> 0.48f
                    1f,
                    0.6f,
                    EXPLOSION_COLOR
            );
        }
    }

    /**
     * 对周围目标应用分布式伤害
     */
    private void applyDistributedDamage(ShipAPI source, CombatEngineAPI engine) {
        Vector2f sourceLocation = source.getLocation();

        for (ShipAPI target : engine.getShips()) {
            if (target.isHulk() || target == source) continue;

            // 检查目标是否在爆炸范围内
            if (Misc.getDistance(sourceLocation, target.getLocation()) > EXPLOSION_RANGE) continue;

            float targetRadius = target.getCollisionRadius();
            Vector2f targetCenter = target.getLocation();

            // 在目标周围均匀分布伤害点
            for (int i = 0; i < DAMAGE_POINTS; i++) {
                float angle = 360f * i / DAMAGE_POINTS;
                Vector2f point = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle)
                        .scale(targetRadius * 0.63f);      // 调整到60%：1.05f -> 0.63f
                Vector2f.add(targetCenter, point, point);

                applyDamageWithShieldEffect(target, engine, source, point);
            }
        }
    }

    /**
     * 系统停用时重置状态
     */
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        timer = 0f;
        triggered = false;
        survivalCheckTimer = 0f;
        hasExploded = false;
    }

    /**
     * 显示系统状态信息
     */
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (!system.isActive()) return null;

        if (!triggered) {
            return String.format("系统过载 %.1fs", ACTIVATION_DELAY - timer);
        } else {
            return "自毁进行中";
        }
    }
}