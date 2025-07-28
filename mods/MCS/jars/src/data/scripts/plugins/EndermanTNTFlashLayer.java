// Created by catleafleaf on 2025-04-25
// Last updated on 2025-04-25 10:02:18 UTC
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.*;

/**
 * 末影TNT闪烁效果渲染层
 * - 实现导弹贴图定期变为纯白色的闪烁效果
 * - 每0.5秒闪烁一次，持续0.5秒
 *
 * @author catleafleaf
 * @version 2025-04-25 10:02:18
 * @since 2025-04-20
 */
public class EndermanTNTFlashLayer extends BaseCombatLayeredRenderingPlugin {

    private static final float FLASH_INTERVAL = 0.5f;    // 闪烁间隔
    private static final float FLASH_DURATION = 0.5f;    // 闪烁持续时间，改为和间隔相同

    // 追踪每个弹体的闪烁时间
    private final Map<DamagingProjectileAPI, Float> projectileTimers = new HashMap<>();
    // 当前正在闪烁的弹体
    private final Set<DamagingProjectileAPI> flashingProjectiles = new HashSet<>();

    public void registerProjectile(DamagingProjectileAPI projectile) {
        if (projectile != null) {
            projectileTimers.put(projectile, 0f);
            flashingProjectiles.add(projectile); // 立即开始第一次闪烁
        }
    }

    @Override
    public void advance(float amount) {
        if (projectileTimers.isEmpty()) return;

        // 遍历并更新所有弹体的计时器
        Iterator<Map.Entry<DamagingProjectileAPI, Float>> it = projectileTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DamagingProjectileAPI, Float> entry = it.next();
            DamagingProjectileAPI proj = entry.getKey();

            // 如果弹体已失效，移除它
            if (proj == null || !Global.getCombatEngine().isEntityInPlay(proj)) {
                it.remove();
                flashingProjectiles.remove(proj);
                continue;
            }

            // 更新计时器
            float oldTime = entry.getValue();
            float newTime = oldTime + amount;
            entry.setValue(newTime);

            // 计算当前时间在闪烁周期中的位置
            float cyclePosition = newTime % FLASH_INTERVAL;

            // 根据周期位置决定是否应该闪烁
            if (cyclePosition < FLASH_DURATION) {
                flashingProjectiles.add(proj);
            } else {
                flashingProjectiles.remove(proj);
            }
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (flashingProjectiles.isEmpty()) return;

        // 设置OpenGL状态
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // 渲染所有正在闪烁的弹体
        for (DamagingProjectileAPI proj : flashingProjectiles) {
            if (proj instanceof MissileAPI) {
                renderFlash((MissileAPI) proj);
            }
        }

        // 恢复OpenGL状态
        GL11.glPopMatrix();
    }

    private void renderFlash(MissileAPI missile) {
        if (missile.getWeapon() == null) return;

        SpriteAPI sprite = missile.getWeapon().getSprite();
        if (sprite == null) return;

        // 保存原始颜色
        Color originalColor = sprite.getColor();
        float originalAlpha = sprite.getAlphaMult();

        // 设置为白色
        sprite.setColor(Color.WHITE);
        sprite.setAlphaMult(1f);

        // 渲染闪光
        Vector2f loc = missile.getLocation();
        float angle = missile.getFacing() - 90f;
        sprite.setAngle(angle);
        sprite.renderAtCenter(loc.x, loc.y);

        // 恢复原始颜色
        sprite.setColor(originalColor);
        sprite.setAlphaMult(originalAlpha);
    }

    @Override
    public float getRenderRadius() {
        return 1000000f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    }
}