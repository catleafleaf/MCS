package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/**
 * Position效果管理器
 * @author catleafleaf
 * @version 2025-05-21 11:20:56
 */
public class MCSPositionPlugin extends BaseEveryFrameCombatPlugin {
    private static final String PLUGIN_KEY = "MCS_PositionPlugin_key";
    private final Map<ShipAPI, PositionDataManager> positionMap = new HashMap<>();
    private CombatEngineAPI engine;

    public static MCSPositionPlugin get(CombatEngineAPI engine) {
        return (MCSPositionPlugin)engine.getCustomData().get(PLUGIN_KEY);
    }

    public static boolean couldAddPosition(CombatEntityAPI target, boolean shieldHit) {
        return target instanceof ShipAPI && !shieldHit && !((ShipAPI)target).isHulk();
    }

    public static boolean addPosition(ShipAPI source, ShipAPI target, WeaponAPI.WeaponSize size, Vector2f point) {
        if (!target.isAlive()) return false;

        Map<ShipAPI, PositionDataManager> positionMap = get(Global.getCombatEngine()).positionMap;
        PositionDataManager manager = positionMap.get(target);

        if (manager == null) {
            manager = new PositionDataManager(target);
            positionMap.put(target, manager);
        }
        return manager.addPosition(source, size, point);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.positionMap.clear();
        this.engine = engine;
        engine.getCustomData().put(PLUGIN_KEY, this);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        Iterator<Map.Entry<ShipAPI, PositionDataManager>> it = positionMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ShipAPI, PositionDataManager> entry = it.next();
            PositionDataManager manager = entry.getValue();
            if (manager.shouldExpire()) {
                manager.cleanup();
                it.remove();
            }
        }
    }

    private static class PositionDataManager implements AdvanceableListener {
        private final ShipAPI ship;
        private final List<PositionData> dataList = new ArrayList<>();

        public PositionDataManager(ShipAPI ship) {
            this.ship = ship;
            ship.addListener(this);
        }

        public boolean addPosition(ShipAPI source, WeaponAPI.WeaponSize size, Vector2f point) {
            PositionData data = new PositionData(ship, source, size, point);
            if (data.isFinished()) return false;

            for (PositionData existingData : dataList) {
                if (!existingData.isFinished()) {
                    existingData.refresh(); // 如果已有效果，刷新持续时间
                    return true;
                }
            }

            dataList.add(data);
            return true;
        }

        @Override
        public void advance(float amount) {
            Iterator<PositionData> it = dataList.iterator();
            while (it.hasNext()) {
                PositionData data = it.next();
                data.advance(amount);
                if (data.isFinished()) {
                    it.remove();
                }
            }
        }

        public void cleanup() {
            ship.removeListener(this);
        }

        public boolean shouldExpire() {
            return ship == null || !ship.isAlive();
        }
    }

    private static class PositionData {
        private static final float MAX_DURATION = 10f;
        private static final Map<WeaponAPI.WeaponSize, Float> DAMAGE_PER_SECOND = new HashMap<WeaponAPI.WeaponSize, Float>() {{
            put(WeaponAPI.WeaponSize.SMALL, 100f);
            put(WeaponAPI.WeaponSize.MEDIUM, 180f);
            put(WeaponAPI.WeaponSize.LARGE, 250f);
        }};

        private final ShipAPI ship;
        private final ShipAPI source;
        private final WeaponAPI.WeaponSize size;
        private final Vector2f point;
        private float remainingTime;
        private boolean finished;
        private final IntervalUtil damageInterval = new IntervalUtil(0.45f, 0.55f);

        public PositionData(ShipAPI ship, ShipAPI source, WeaponAPI.WeaponSize size, Vector2f point) {
            this.ship = ship;
            this.source = source;
            this.size = size;
            this.point = point;
            this.remainingTime = MAX_DURATION;
            this.finished = false;
        }

        public void refresh() {
            this.remainingTime = MAX_DURATION;
        }

        public void advance(float amount) {
            if (finished) return;

            remainingTime -= amount;
            if (remainingTime <= 0) {
                finished = true;
                return;
            }

            damageInterval.advance(amount);
            if (damageInterval.intervalElapsed()) {
                float damage = DAMAGE_PER_SECOND.get(size) * damageInterval.getElapsed();
                Global.getCombatEngine().applyDamage(
                        ship, point, damage, DamageType.ENERGY, 0,
                        false, true, source);
            }
        }

        public boolean isFinished() {
            return finished;
        }
    }
}