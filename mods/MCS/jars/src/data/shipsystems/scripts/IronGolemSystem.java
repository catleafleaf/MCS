package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;


public class IronGolemSystem extends BaseShipSystemScript {


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        // 系统退出时
        if (state == ShipSystemStatsScript.State.OUT) {
            // 移除速度和转向修改器，使舰船恢复正常速度
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
        } else {
            // 系统激活时的效果
            // 增加最大速度
            stats.getMaxSpeed().modifyFlat(id, 50f);
            // 提升加速度和减速度（最高200%）
            stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getDeceleration().modifyPercent(id, 200f * effectLevel);
            // 提升转向加速度
            stats.getTurnAcceleration().modifyFlat(id, 30f * effectLevel);
            stats.getTurnAcceleration().modifyPercent(id, 200f * effectLevel);
            // 提升最大转向速率
            stats.getMaxTurnRate().modifyFlat(id, 15f);
            stats.getMaxTurnRate().modifyPercent(id, 100f);
        }

        // 引擎效果代码（当前未启用）
        if (stats.getEntity() instanceof ShipAPI && false) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            String key = ship.getId() + "_" + id;
            Object test = Global.getCombatEngine().getCustomData().get(key);
            // 系统启动时
            if (state == State.IN) {
                // 当效果等级超过0.2且未激活时
                if (test == null && effectLevel > 0.2f) {
                    Global.getCombatEngine().getCustomData().put(key, new Object());
                    // 延长引擎火焰
                    ship.getEngineController().getExtendLengthFraction().advance(1f);
                    // 为所有系统激活的引擎设置最大火焰等级
                    for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                        if (engine.isSystemActivated()) {
                            ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 1f);
                        }
                    }
                }
            } else {
                // 系统关闭时移除标记
                Global.getCombatEngine().getCustomData().remove(key);
            }
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        // 移除所有状态修改
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }


    public StatusData getStatusData(int index, State state, float effectLevel) {
        // 返回UI显示的状态信息
        if (index == 0) {
            return new StatusData("提高机动性", false);
        } else if (index == 1) {
            return new StatusData("+50 最高航速", false);
        }
        return null;
    }
}