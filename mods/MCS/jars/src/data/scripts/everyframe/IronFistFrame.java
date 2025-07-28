/**
 * MCS_IronFist武器缩放动画与开火同步效果
 * - 监视并同步槽位WS0003和WS0004的开火状态
 * - 执行垂直方向的缩放动画
 * - 在1/4和3/4周期切换动画帧
 *
 * @author catleafleaf
 * @version 2.5.0
 * @since 2025-03-29 02:20:15
 */
package data.scripts.everyframe;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.FastTrig;

public class IronFistFrame implements EveryFrameWeaponEffectPlugin {

    // ====== 配置常量 ======
    /** 武器ID */
    private static final String WEAPON_ID = "MCS_IronFist";
    /** 动画周期（秒） */
    private static final float PERIOD = 0.8f;
    /** 角速度 (2π/周期) */
    private static final float OMEGA = (float) (2.0f * Math.PI / PERIOD);
    /** 监视的武器槽位 */
    private static final String[] TARGET_SLOTS = {"WS0003", "WS0004"};
    /** y值接近0的阈值 */
    private static final float ZERO_THRESHOLD = 0.001f;
    /** 开火检测阈值 */
    private static final float FIRING_CHECK_INTERVAL = 0.1f;
    /** 周期节点 */
    private static final float QUARTER_PERIOD = PERIOD * 0.25f;
    private static final float THREE_QUARTER_PERIOD = PERIOD * 0.75f;

    // ====== 状态变量 ======
    /** 原始贴图宽度 */
    private float baseWidth = 0f;
    /** 原始贴图高度 */
    private float baseHeight = 0f;
    /** 动画计时器 */
    private float timer = 0f;
    /** 开火检测计时器 */
    private float firingCheckTimer = 0f;
    /** 初始化标志 */
    private boolean initialized = false;
    /** 动画激活标志 */
    private boolean active = false;
    /** 监视的武器列表 */
    private WeaponAPI[] targetWeapons = new WeaponAPI[TARGET_SLOTS.length];
    /** 上一帧的y值 */
    private float lastY = 1f;
    /** 当前武器开火状态 */
    private boolean currentlyFiring = false;
    /** 当前动画帧 */
    private int currentFrame = 0;
    /** 帧切换标志 */
    private boolean hasFrameSwitched = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // 暂停或非目标武器时跳过
        if (engine.isPaused() || weapon.getId() == null || !WEAPON_ID.equals(weapon.getId())) {
            return;
        }

        // 首次运行初始化
        if (!initialized) {
            initializeWeapon(weapon);
            return;
        }

        // 更新开火状态
        updateFiringState(amount, weapon);

        // 检测开火状态并更新动画
        updateAnimationState(weapon);

        // 执行动画更新
        if (active) {
            processAnimation(amount, weapon);
        }
    }

    /**
     * 初始化武器参数
     * @param weapon 当前武器
     */
    private void initializeWeapon(WeaponAPI weapon) {
        // 记录原始尺寸
        SpriteAPI sprite = weapon.getSprite();
        baseWidth = sprite.getWidth();
        baseHeight = sprite.getHeight();

        // 初始化目标武器列表
        if (weapon.getShip() != null) {
            for (int i = 0; i < TARGET_SLOTS.length; i++) {
                String slotId = TARGET_SLOTS[i];
                for (WeaponAPI shipWeapon : weapon.getShip().getAllWeapons()) {
                    if (slotId.equals(shipWeapon.getSlot().getId())) {
                        targetWeapons[i] = shipWeapon;
                        break;
                    }
                }
            }
        }

        // 设置初始状态
        resetSpriteState(sprite);
        resetAnimation(weapon);
        initialized = true;
    }

    /**
     * 更新开火状态
     * @param amount 时间增量
     * @param weapon 当前武器
     */
    private void updateFiringState(float amount, WeaponAPI weapon) {
        firingCheckTimer += amount;

        // 定期检查开火状态
        if (firingCheckTimer >= FIRING_CHECK_INTERVAL) {
            boolean shouldFire = false;

            // 检查目标武器是否有开火
            for (WeaponAPI target : targetWeapons) {
                if (target != null && target.isFiring()) {
                    shouldFire = true;
                    break;
                }
            }

            // 同步开火状态
            if (shouldFire != currentlyFiring) {
                currentlyFiring = shouldFire;
                if (currentlyFiring) {
                    weapon.setForceFireOneFrame(true);
                }
            }

            // 同步当前武器开火状态到目标武器
            if (weapon.isFiring()) {
                for (WeaponAPI target : targetWeapons) {
                    if (target != null) {
                        target.setForceFireOneFrame(true);
                    }
                }
            }

            firingCheckTimer = 0f;
        }
    }

    /**
     * 更新动画状态
     * @param weapon 当前武器
     */
    private void updateAnimationState(WeaponAPI weapon) {
        // 检查是否需要开始动画
        if (currentlyFiring && !active) {
            startAnimation(weapon);
        }
    }

    /**
     * 处理动画逻辑
     * @param amount 时间增量
     * @param weapon 当前武器
     */
    private void processAnimation(float amount, WeaponAPI weapon) {
        timer += amount;

        // 检查动画是否结束
        if (timer >= PERIOD) {
            endAnimation(weapon);
            return;
        }

        // 计算当前y值 (-1到1的余弦值)
        float currentY = (float) FastTrig.cos(OMEGA * timer);

        // 检查帧切换点
        checkFrameSwitch(weapon);

        // 应用缩放效果
        updateSpriteTransform(weapon.getSprite(), currentY);

        // 保存当前y值
        lastY = currentY;
    }

    /**
     * 检查并执行帧切换
     * @param weapon 当前武器
     */
    private void checkFrameSwitch(WeaponAPI weapon) {
        // 1/4周期切换到第二帧
        if (timer >= QUARTER_PERIOD && timer < THREE_QUARTER_PERIOD && !hasFrameSwitched) {
            currentFrame = 1;
            weapon.getAnimation().setFrame(currentFrame);
            hasFrameSwitched = true;
        }
        // 3/4周期切换回第一帧
        else if (timer >= THREE_QUARTER_PERIOD && hasFrameSwitched) {
            currentFrame = 0;
            weapon.getAnimation().setFrame(currentFrame);
            hasFrameSwitched = false;
        }
    }

    /**
     * 更新精灵变换
     * @param sprite 精灵对象
     * @param yValue 当前y值
     */
    private void updateSpriteTransform(SpriteAPI sprite, float yValue) {
        // 计算缩放值和位置
        float scaleY = Math.abs(yValue);
        float newHeight = scaleY * baseHeight;
        float centerY;

        if (yValue >= 0) {
            // 上半周期
            centerY = baseHeight / 2f - (baseHeight - newHeight);
        } else {
            // 下半周期
            centerY = (yValue / 2f) * baseHeight;
        }

        // 应用变换
        sprite.setSize(baseWidth, newHeight);
        sprite.setCenter(baseWidth / 2f, centerY);
    }

    /**
     * 开始动画
     */
    private void startAnimation(WeaponAPI weapon) {
        active = true;
        timer = 0f;
        lastY = 1f;
        currentFrame = 0;
        hasFrameSwitched = false;
        weapon.getAnimation().setFrame(currentFrame);
    }

    /**
     * 结束动画
     * @param weapon 当前武器
     */
    private void endAnimation(WeaponAPI weapon) {
        active = false;
        timer = 0f;
        lastY = 1f;
        resetSpriteState(weapon.getSprite());
        resetAnimation(weapon);
    }

    /**
     * 重置精灵状态
     * @param sprite 精灵对象
     */
    private void resetSpriteState(SpriteAPI sprite) {
        sprite.setSize(baseWidth, baseHeight);
        sprite.setCenter(baseWidth / 2f, baseHeight / 2f);
    }

    /**
     * 重置动画状态
     */
    private void resetAnimation(WeaponAPI weapon) {
        currentFrame = 0;
        hasFrameSwitched = false;
        weapon.getAnimation().setFrame(currentFrame);
    }
}