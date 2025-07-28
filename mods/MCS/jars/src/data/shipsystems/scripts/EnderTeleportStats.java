// Created by catleafleaf on 2025-04-26
// Last updated on 2025-04-26 04:16:43 UTC
package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

/**
 * 瞬时位移系统
 * - 记录原位置并高亮显示
 * - 10秒后返回原位置
 *
 * @author catleafleaf
 * @version 2025-04-26 04:16:43
 */
public class EnderTeleportStats extends BaseShipSystemScript {
    private static final Color EFFECT_COLOR = new Color(138, 43, 226);  // 特效颜色
    private static final Color MARKER_COLOR = new Color(138, 43, 226, 200); // 标记点颜色
    private static final float RETURN_TIME = 10000f;             // 返回时间

    private Vector2f originalLocation;                         // 原始位置
    private float returnTimer = 0f;                           // 返回计时器
    private boolean hasTriggered = false;                     // 是否已触发
    private float effectTimer = 0f;                           // 特效计时器

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        float amount = Global.getCombatEngine().getElapsedInLastFrame();

        // 系统激活时
        if (state == State.IN && !hasTriggered) {
            // 保存原始位置
            originalLocation = new Vector2f(ship.getLocation());
            hasTriggered = true;
            effectTimer = 0f;
            returnTimer = 0f;  // 重置返回计时器

            // 生成特效
            spawnTeleportEffect(engine, ship.getLocation());
        }

        // 系统持续期间
        if (hasTriggered) {
            effectTimer += amount;
            returnTimer += amount;  // 累加时间

            // 在原位置绘制标记
            engine.addSmoothParticle(
                    originalLocation,
                    new Vector2f(0, 0),
                    100f, // 标记大小
                    10f, // 亮度
                    10f, // 持续时间
                    MARKER_COLOR
            );

            // 在原位置周围添加粒子效果
            for (int i = 0; i < 4; i++) {
                float angle = (effectTimer * 2f + i * 90f) % 360f;
                float radius = 30f;
                Vector2f particlePos = new Vector2f(
                        originalLocation.x + (float) Math.cos(Math.toRadians(angle)) * radius,
                        originalLocation.y + (float) Math.sin(Math.toRadians(angle)) * radius
                );

                engine.addSmoothParticle(
                        particlePos,
                        new Vector2f(0, 0),
                        10f,
                        0.7f,
                        0.1f,
                        MARKER_COLOR
                );
            }

            // 检查是否到达返回时间
            if (returnTimer >= RETURN_TIME) {
                // 返回原位置
                ship.getLocation().set(originalLocation);
                spawnTeleportEffect(engine, ship.getLocation());

                // 重置状态
                hasTriggered = false;
                returnTimer = 0f;
            }
        }
    }



    private void spawnTeleportEffect(CombatEngineAPI engine, Vector2f loc) {
        // 中心闪光
        engine.addHitParticle(
                loc,
                new Vector2f(0, 0),
                100f,
                10f,
                10f,
                EFFECT_COLOR
        );

        // 环形粒子效果
        for (int i = 0; i < 12; i++) {
            float angle = i * 30f;
            float radians = (float) Math.toRadians(angle);
            Vector2f particleVel = new Vector2f(
                    (float) Math.cos(radians) * 100f,
                    (float) Math.sin(radians) * 100f
            );

            engine.addHitParticle(
                    loc,
                    particleVel,
                    5f,
                    0.8f,
                    0.5f,
                    EFFECT_COLOR
            );
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && hasTriggered) {
            return new StatusData("RETURNING IN " + (int)Math.ceil(RETURN_TIME - returnTimer) + "s", false);
        }
        return null;
    }
}