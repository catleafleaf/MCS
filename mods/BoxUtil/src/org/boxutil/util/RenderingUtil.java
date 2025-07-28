package org.boxutil.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.boxutil.units.standard.entity.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.boxutil.base.api.InstanceDataAPI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CampaignRenderingManager;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.attribute.Instance2Data;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class RenderingUtil {
    private static final CombatEngineLayers[] _COMBAT_LAYER = CombatEngineLayers.values();
    private static final HashMap<CombatEngineLayers, Integer> _COMBAT_LAYER_LINKED = new HashMap<>();
    private static final CombatEngineLayers _COMBAT_HIGHEST_LAYER = _COMBAT_LAYER[_COMBAT_LAYER.length - 1];
    private static final CombatEngineLayers _COMBAT_LOWEST_LAYER = _COMBAT_LAYER[0];
    private static final CampaignEngineLayers[] _CAMPAIGN_LAYER = CampaignEngineLayers.values();
    private static final HashMap<CampaignEngineLayers, Integer> _CAMPAIGN_LAYER_LINKED = new HashMap<>();
    private static final CampaignEngineLayers _CAMPAIGN_HIGHEST_LAYER = _CAMPAIGN_LAYER[_CAMPAIGN_LAYER.length - 1];
    private static final CampaignEngineLayers _CAMPAIGN_LOWEST_LAYER = _CAMPAIGN_LAYER[0];
    static {
        for (int i = 0; i < _COMBAT_LAYER.length; i++) {
            _COMBAT_LAYER_LINKED.put(_COMBAT_LAYER[i], i);
        }
        for (int i = 0; i < _CAMPAIGN_LAYER.length; i++) {
            _CAMPAIGN_LAYER_LINKED.put(_CAMPAIGN_LAYER[i], i);
        }
    }

    public static CombatEngineLayers getHighestCombatLayer() {
        return _COMBAT_HIGHEST_LAYER;
    }

    public static CombatEngineLayers getLowestCombatLayer() {
        return _COMBAT_LOWEST_LAYER;
    }

    public static CampaignEngineLayers getHighestCampaignLayer() {
        return _CAMPAIGN_HIGHEST_LAYER;
    }

    public static CampaignEngineLayers getLowestCampaignLayer() {
        return _CAMPAIGN_LOWEST_LAYER;
    }

    public static CombatEngineLayers getPreCombatLayer(CombatEngineLayers layer) {
        int index = _COMBAT_LAYER_LINKED.get(layer) - 1;
        return _COMBAT_LAYER[index < 0 ? _COMBAT_LAYER.length - 1 : index];
    }

    public static CombatEngineLayers getNextCombatLayer(CombatEngineLayers layer) {
        int index = _COMBAT_LAYER_LINKED.get(layer) + 1;
        return _COMBAT_LAYER[index >= _COMBAT_LAYER.length ? 0 : index];
    }

    public static CampaignEngineLayers getPreCampaignLayer(CampaignEngineLayers layer) {
        int index = _CAMPAIGN_LAYER_LINKED.get(layer) - 1;
        return _CAMPAIGN_LAYER[index < 0 ? _CAMPAIGN_LAYER.length - 1 : index];
    }

    public static CampaignEngineLayers getNextCampaignLayer(CampaignEngineLayers layer) {
        int index = _CAMPAIGN_LAYER_LINKED.get(layer) + 1;
        return _CAMPAIGN_LAYER[index >= _CAMPAIGN_LAYER.length ? 0 : index];
    }

    /**
     * Curve beam entity.
     */
    public static TrailEntity createBeamVisual(Vector2f location, float facing, float length, float width, Color coreColor, @Nullable Color fringeColor, SpriteAPI core, @Nullable SpriteAPI fringe, float fadeIn, float full, float fadeOut, boolean isAdditiveBlend) {
        TrailEntity entity = new TrailEntity();
        entity.addNode(new Vector2f(length, 0.0f));
        entity.addNode(new Vector2f());
        entity.submitNodes();
        entity.getMaterialData().setColor(coreColor);
        if (fringeColor != null) entity.getMaterialData().setEmissiveColor(fringeColor);
        entity.setStartWidth(width);
        entity.setEndWidth(width);
        entity.setGlobalTimer(fadeIn, full, fadeOut);
        if (isAdditiveBlend) entity.setAdditiveBlend();
        entity.getMaterialData().setDiffuse(core);
        if (fringe != null) entity.getMaterialData().setEmissive(fringe);
        TransformUtil.createModelMatrixVanilla(location, facing, entity.getModelMatrix());
        return entity;
    }

    /**
     * Curve beam entity.
     */
    public static Pair<TrailEntity, Byte> addCampaignBeamVisual(Vector2f location, float facing, float length, float width, Color coreColor, SpriteAPI core, float full, float fadeOut, CampaignEngineLayers layer) {
        return addCampaignBeamVisual(location, facing, length, width, coreColor, null, core, null, 0.0f, full, fadeOut, true, layer);
    }

    /**
     * Curve beam entity.
     */
    public static Pair<TrailEntity, Byte> addCampaignBeamVisual(Vector2f location, float facing, float length, float width, Color coreColor, @Nullable Color fringeColor, SpriteAPI core, @Nullable SpriteAPI fringe, float fadeIn, float full, float fadeOut, boolean isAdditiveBlend, CampaignEngineLayers layer) {
        TrailEntity entity = createBeamVisual(location, facing, length, width, coreColor, fringeColor, core, fringe, fadeIn, full, fadeOut, isAdditiveBlend);
        entity.setLayer(layer);
        return new Pair<>(entity, CampaignRenderingManager.addEntity(BoxEnum.ENTITY_CURVE, entity));
    }

    /**
     * Curve beam entity.
     */
    public static Pair<TrailEntity, Byte> addCombatBeamVisual(Vector2f location, float facing, float length, float width, Color coreColor, SpriteAPI core, float full, float fadeOut, CombatEngineLayers layer) {
        return addCombatBeamVisual(location, facing, length, width, coreColor, null, core, null, 0.0f, full, fadeOut, true, layer);
    }

    /**
     * Curve beam entity.
     */
    public static Pair<TrailEntity, Byte> addCombatBeamVisual(Vector2f location, float facing, float length, float width, Color coreColor, @Nullable Color fringeColor, SpriteAPI core, @Nullable SpriteAPI fringe, float fadeIn, float full, float fadeOut, boolean isAdditiveBlend, CombatEngineLayers layer) {
        TrailEntity entity = createBeamVisual(location, facing, length, width, coreColor, fringeColor, core, fringe, fadeIn, full, fadeOut, isAdditiveBlend);
        entity.setLayer(layer);
        return new Pair<>(entity, CombatRenderingManager.addEntity(BoxEnum.ENTITY_CURVE, entity));
    }

    public static Pair<TrailEntity, FlareEntity> spawnEmpArcVisual(@Nullable Vector2f offset, float width, Vector2f start, Vector2f end, Color fringe, @Nullable Color core, float jitterPower, float full, float fadeOut) {
        TrailEntity arc = new TrailEntity();
        arc.getMaterialData().setDiffuse(Global.getSettings().getSprite("graphics/fx/beamcoreb.png"));
        arc.getMaterialData().setEmissive(Global.getSettings().getSprite("graphics/fx/beamfringeb.png"));
        if (core != null) arc.getMaterialData().setColor(core);
        arc.getMaterialData().setEmissiveColor(fringe);
        arc.setJitterPower(0.1f);
        arc.setFlick(true);
        Vector2f normal = new Vector2f(start.y - end.y, end.x - start.x), curr;
        float arcLength = normal.length(), jitterLength = (float) Math.sqrt(arcLength) * jitterPower, factor, minFactor;
        int maxJitterNode = (int) Math.floor(jitterLength) - 2;
        normal.scale(1.0f / arcLength);
        arc.addNode(end);
        minFactor = 0.8f / maxJitterNode;
        for (int i = maxJitterNode - 1; i > 0; --i) {
            factor = (float) i / (float) maxJitterNode;
            factor += ((float) Math.random() - 0.5f) * minFactor;
            curr = CalculateUtil.mix(start, end, new Vector2f(), factor);
            curr.x += normal.x * jitterLength;
            curr.y += normal.y * jitterLength;
            if ((float) Math.random() >= 0.5f) curr.set(-curr.x, -curr.y);
            arc.addNode(curr);
        }
        arc.addNode(start);
        arc.setNodeRefreshAllFromCurrentIndex();
        arc.submitNodes();
        arc.setStartWidth(width);
        arc.setEndWidth(width);
        float smoothFactor = Math.max(arcLength - 4.0f, 0.0f) / arcLength;
        arc.setFillStartAlpha(0.0f);
        arc.setFillStartFactor(smoothFactor);
        arc.setFillEndAlpha(0.0f);
        arc.setFillEndFactor(smoothFactor);
        arc.setGlobalTimer(0.0f, full, fadeOut);

        FlareEntity ends = new FlareEntity();
        float endsSize = width * 2.0f;
        List<InstanceDataAPI> dataList = new ArrayList<>();
        Instance2Data data = new Instance2Data();
        data.setLocation(start);
        dataList.add(data);
        data = new Instance2Data();
        data.setLocation(end);
        dataList.add(data);
        ends.setInstanceData(dataList, 0.0f, full, fadeOut);
        ends.setInstanceDataRefreshAllFromCurrentIndex();
        ends.submitInstanceData();
        ends.setRenderingCount(2);
        ends.setAlwaysRefreshInstanceData(true);
        ends.setSmooth();
        ends.setFlick(true);
        ends.setSyncFlick(true);
        if (core != null) ends.setCoreColor(core);
        ends.setFringeColor(fringe);
        ends.setFlickerSyncCode(arc.hashCode());
        ends.setGlobalTimer(0.0f, full, fadeOut);
        ends.setSize(endsSize, endsSize);
        if (offset != null) {
            arc.setLocation(offset);
            ends.setLocation(offset);
        }
        return new Pair<>(arc, ends);
    }

    public static Pair<Byte, Pair<TrailEntity, FlareEntity>> spawnCombatEmpArcVisual(Vector2f start, Vector2f end, float width, Color fringe, @Nullable Color core, float jitterPower, float full, float fadeOut) {
        Vector2f realEnd = Vector2f.sub(end, start, new Vector2f());
        Pair<TrailEntity, FlareEntity> result = spawnEmpArcVisual(start, width, new Vector2f(), realEnd, fringe, core, jitterPower, full, fadeOut);
        result.one.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        result.two.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        byte code = CombatRenderingManager.addEntity(BoxEnum.ENTITY_CURVE, result.one);
        code |= CombatRenderingManager.addEntity(BoxEnum.ENTITY_CURVE, result.two);
        return new Pair<>(code, result);
    }

    public static Pair<Byte, Pair<TrailEntity, FlareEntity>> spawnCombatEmpArcVisual(Vector2f start, Vector2f end, float width, Color fringe, @Nullable Color core) {
        return spawnCombatEmpArcVisual(start, end, width, fringe, core, 1.0f, (float) Math.random() * 2.0f + 0.5f, 0.5f);
    }

    public static Pair<Byte, Pair<TrailEntity, FlareEntity>> spawnCampaignEmpArcVisual(Vector2f start, Vector2f end, float width, Color fringe, @Nullable Color core, float jitterPower, float full, float fadeOut) {
        Vector2f realEnd = Vector2f.sub(end, start, new Vector2f());
        Pair<TrailEntity, FlareEntity> result = spawnEmpArcVisual(start, width, new Vector2f(), realEnd, fringe, core, jitterPower, full, fadeOut);
        result.one.setLayer(CampaignEngineLayers.TERRAIN_8);
        result.two.setLayer(CampaignEngineLayers.TERRAIN_8);
        byte code = CombatRenderingManager.addEntity(BoxEnum.ENTITY_CURVE, result.one);
        code |= CombatRenderingManager.addEntity(BoxEnum.ENTITY_CURVE, result.two);
        return new Pair<>(code, result);
    }

    public static Pair<Byte, Pair<TrailEntity, FlareEntity>> spawnCampaignEmpArcVisual(Vector2f start, Vector2f end, float width, Color fringe, @Nullable Color core) {
        return spawnCampaignEmpArcVisual(start, end, width, fringe, core, 1.0f, (float) Math.random() * 2.0f + 0.5f, 0.5f);
    }

    public static SpriteEntity createParticleField(Vector2f location, int count, float facing, float arc, @Nullable Vector2f baseSpreadRange, @Nullable Vector2f velocityRange, @Nullable Vector2f facingRange, @Nullable Vector2f turnRateRange, Vector4f sizeRangeXY, @Nullable Vector4f sizeGrowScaleRangeXY, @Nullable Color baseColor, @Nullable Color baseColorShift, @Nullable Color baseEmissiveColor, @Nullable Color baseEmissiveColorShift, SpriteAPI diffuse, @Nullable SpriteAPI emissive, float fadeIn, float full, float fadeOut, float timerOffsetRange, boolean isAdditiveBlend) {
        SpriteEntity entity = new SpriteEntity();
        if (baseColor == null) baseColor = Color.WHITE;
        if (baseEmissiveColor == null) baseEmissiveColor = Color.WHITE;
        if (isAdditiveBlend) entity.setAdditiveBlend();
        entity.getMaterialData().setDiffuse(diffuse);
        if (emissive != null) entity.getMaterialData().setEmissive(emissive);
        TransformUtil.createModelMatrixVanilla(location, facing, entity.getModelMatrix());

        final int finalCount = Math.min(BoxConfigs.getMaxInstanceDataSize(), count);
        final float finalArc = arc / 2.0f;
        final boolean haveColorShift = baseColorShift != null;
        final boolean haveEmissiveColorShift = baseEmissiveColorShift != null;
        final boolean haveSpreadRange = baseSpreadRange != null;
        final boolean haveVelocityRange = velocityRange != null;
        List<InstanceDataAPI> dataList = new ArrayList<>();
        for (int i = 0; i < finalCount; i++) {
            float factor = (float) Math.random();
            float factor2 = (float) Math.random();
            float timerOffset = timerOffsetRange * factor;
            Instance2Data data = new Instance2Data();
            float angle = facing + finalArc * (factor2 * 2.0f - 1.0f);
            if (angle < 0.0f) angle += 360.0f;
            if (angle > 360.0f) angle -= 360.0f;
            float baseX = (float) Math.cos(Math.toRadians(angle));
            float baseY = TrigUtil.sinFormCosF(baseX, angle);
            Color finalColor;
            if (haveColorShift) {
                finalColor = CalculateUtil.mix(baseColor, baseColorShift, true, factor);
            } else finalColor = baseColor;
            Color finalEmissiveColor;
            if (haveEmissiveColorShift) {
                finalEmissiveColor = CalculateUtil.mix(baseEmissiveColor, baseEmissiveColorShift, true, factor);
            } else finalEmissiveColor = baseEmissiveColor;
            if (haveSpreadRange) {
                float locationLength = (baseSpreadRange.y - baseSpreadRange.x) * factor + baseSpreadRange.x;
                data.setLocation(locationLength * baseX, locationLength * baseY);
            }
            if (haveVelocityRange) {
                float velocityLength = (velocityRange.x - velocityRange.y) * factor + velocityRange.x;
                data.setVelocity(velocityLength * baseX, velocityLength * baseY);
            }
            float sizeX = (sizeRangeXY.z - sizeRangeXY.x) * factor + sizeRangeXY.x;
            float sizeY = (sizeRangeXY.w - sizeRangeXY.y) * factor + sizeRangeXY.y;
            data.setScale(sizeX * 0.5f, sizeY * 0.5f);
            if (sizeGrowScaleRangeXY != null) {
                float growX = (sizeGrowScaleRangeXY.z - sizeGrowScaleRangeXY.x) * factor + sizeGrowScaleRangeXY.x;
                float growY = (sizeGrowScaleRangeXY.w - sizeGrowScaleRangeXY.y) * factor + sizeGrowScaleRangeXY.y;
                data.setScaleRate(growX * 0.5f, growY * 0.5f);
            }
            data.setColor(finalColor);
            data.setEmissiveColor(finalEmissiveColor);
            if (facingRange != null) data.setFacing((facingRange.y - facingRange.x) * factor + facingRange.x);
            if (turnRateRange != null) data.setTurnRate((turnRateRange.y - turnRateRange.x) * factor + turnRateRange.x);
            data.setTimer(fadeIn + timerOffset, full + timerOffset, fadeOut + timerOffset);
            dataList.add(data);
        }
        float timerOffsetCheck = Math.max(timerOffsetRange, 0.0f);
        entity.setInstanceData(dataList, fadeIn + timerOffsetCheck, full + timerOffsetCheck, fadeOut + timerOffsetCheck);
        entity.setInstanceDataRefreshAllFromCurrentIndex();
        entity.submitInstanceData();
        entity.setRenderingCount(finalCount);
        entity.setAlwaysRefreshInstanceData(true);
        return entity;
    }

    public static Pair<SpriteEntity, Byte> addCampaignParticleField(Vector2f location, int count, float facing, float arc, float fieldRadius, Vector4f sizeRangeXY, @Nullable Color baseColor, SpriteAPI diffuse, float full, float fadeOut, CampaignEngineLayers layer) {
        return addCampaignParticleField(location, count, facing, arc, null, new Vector2f(0.0f, fieldRadius), new Vector2f(0.0f, 360.0f), null, sizeRangeXY, null, baseColor, null, null, null, diffuse, null, 0.0f, full, fadeOut, 0.0f, true, layer);
    }

    public static Pair<SpriteEntity, Byte> addCampaignParticleField(Vector2f location, int count, float facing, float arc, @Nullable Vector2f baseSpreadRange, @Nullable Vector2f velocityRange, @Nullable Vector2f facingRange, @Nullable Vector2f turnRateRange, Vector4f sizeRangeXY, @Nullable Vector4f sizeGrowScaleRangeXY, @Nullable Color baseColor, @Nullable Color baseColorShift, @Nullable Color baseEmissiveColor, @Nullable Color baseEmissiveColorShift, SpriteAPI diffuse, @Nullable SpriteAPI emissive, float fadeIn, float full, float fadeOut, float timerOffsetRange, boolean isAdditiveBlend, CampaignEngineLayers layer) {
        SpriteEntity entity = createParticleField(location, count, facing, arc, baseSpreadRange, velocityRange, facingRange, turnRateRange, sizeRangeXY, sizeGrowScaleRangeXY, baseColor, baseColorShift, baseEmissiveColor, baseEmissiveColorShift, diffuse, emissive, fadeIn, full, fadeOut, timerOffsetRange, isAdditiveBlend);
        entity.setLayer(layer);
        return new Pair<>(entity, CampaignRenderingManager.addEntity(BoxEnum.ENTITY_SPRITE, entity));
    }

    public static Pair<SpriteEntity, Byte> addCombatParticleField(Vector2f location, int count, float facing, float arc, float fieldRadius, Vector4f sizeRangeXY, @Nullable Color baseColor, SpriteAPI diffuse, float full, float fadeOut, CombatEngineLayers layer) {
        return addCombatParticleField(location, count, facing, arc, null, new Vector2f(0.0f, fieldRadius), new Vector2f(0.0f, 360.0f), null, sizeRangeXY, null, baseColor, null, null, null, diffuse, null, 0.0f, full, fadeOut, 0.0f, true, layer);
    }

    public static Pair<SpriteEntity, Byte> addCombatParticleField(Vector2f location, int count, float facing, float arc, @Nullable Vector2f baseSpreadRange, @Nullable Vector2f velocityRange, @Nullable Vector2f facingRange, @Nullable Vector2f turnRateRange, Vector4f sizeRangeXY, @Nullable Vector4f sizeGrowScaleRangeXY, @Nullable Color baseColor, @Nullable Color baseColorShift, @Nullable Color baseEmissiveColor, @Nullable Color baseEmissiveColorShift, SpriteAPI diffuse, @Nullable SpriteAPI emissive, float fadeIn, float full, float fadeOut, float timerOffsetRange, boolean isAdditiveBlend, CombatEngineLayers layer) {
        SpriteEntity entity = createParticleField(location, count, facing, arc, baseSpreadRange, velocityRange, facingRange, turnRateRange, sizeRangeXY, sizeGrowScaleRangeXY, baseColor, baseColorShift, baseEmissiveColor, baseEmissiveColorShift, diffuse, emissive, fadeIn, full, fadeOut, timerOffsetRange, isAdditiveBlend);
        entity.setLayer(layer);
        return new Pair<>(entity, CombatRenderingManager.addEntity(BoxEnum.ENTITY_SPRITE, entity));
    }

    public static FlareEntity createFlareField(Vector2f location, int count, float facing, float arc, @Nullable Vector2f baseSpreadRange, @Nullable Vector2f facingRange, Vector4f sizeRangeXY, @Nullable Color baseFringeColor, @Nullable Color baseFringeColorShift, @Nullable Color baseCoreColor, @Nullable Color baseCoreColorShift, boolean flick, boolean syncFlick, float fadeIn, float full, float fadeOut, float timerOffsetRange, boolean isAdditiveBlend) {
        FlareEntity entity = new FlareEntity();
        if (baseFringeColor == null) baseFringeColor = Color.WHITE;
        if (baseCoreColor == null) baseCoreColor = Color.WHITE;
        entity.setFlick(flick);
        entity.setSyncFlick(syncFlick);
        entity.setAspect((sizeRangeXY.x + sizeRangeXY.z) / (sizeRangeXY.y + sizeRangeXY.w));
        if (isAdditiveBlend) entity.setAdditiveBlend();
        TransformUtil.createModelMatrixVanilla(location, facing, entity.getModelMatrix());

        final int finalCount = Math.min(BoxConfigs.getMaxInstanceDataSize(), count);
        final float finalArc = arc / 2.0f;
        final boolean haveFringeColorShift = baseFringeColorShift != null;
        final boolean haveCoreColorShift = baseCoreColorShift != null;
        final boolean haveSpreadRange = baseSpreadRange != null;
        List<InstanceDataAPI> dataList = new ArrayList<>();
        for (int i = 0; i < finalCount; i++) {
            float factor = (float) Math.random();
            float factor2 = (float) Math.random();
            float timerOffset = timerOffsetRange * factor;
            Instance2Data data = new Instance2Data();
            float angle = facing + finalArc * (factor2 * 2.0f - 1.0f);
            if (angle < 0.0f) angle += 360.0f;
            if (angle > 360.0f) angle -= 360.0f;
            float baseX = (float) Math.cos(Math.toRadians(angle));
            float baseY = TrigUtil.sinFormCosF(baseX, angle);
            Color finalFringeColor;
            if (haveFringeColorShift) {
                finalFringeColor = CalculateUtil.mix(baseFringeColor, baseFringeColorShift, true, factor);
            } else finalFringeColor = baseFringeColor;
            Color finalCoreColor;
            if (haveCoreColorShift) {
                finalCoreColor = CalculateUtil.mix(baseCoreColor, baseCoreColorShift, true, factor);
            } else finalCoreColor = baseCoreColor;
            if (haveSpreadRange) {
                float locationLength = (baseSpreadRange.y - baseSpreadRange.x) * factor + baseSpreadRange.x;
                data.setLocation(locationLength * baseX, locationLength * baseY);
            }
            float sizeX = (sizeRangeXY.z - sizeRangeXY.x) * factor + sizeRangeXY.x;
            float sizeY = (sizeRangeXY.w - sizeRangeXY.y) * factor + sizeRangeXY.y;
            data.setScale(sizeX * 0.5f, sizeY * 0.5f);
            data.setColor(finalCoreColor);
            data.setEmissiveColor(finalFringeColor);
            if (facingRange != null) data.setFacing((facingRange.y - facingRange.x) * factor + facingRange.x);
            data.setTimer(fadeIn + timerOffset, full + timerOffset, fadeOut + timerOffset);
            dataList.add(data);
        }
        float timerOffsetCheck = Math.max(timerOffsetRange, 0.0f);
        entity.setInstanceData(dataList, fadeIn + timerOffsetCheck, full + timerOffsetCheck, fadeOut + timerOffsetCheck);
        entity.setInstanceDataRefreshAllFromCurrentIndex();
        entity.submitInstanceData();
        entity.setRenderingCount(finalCount);
        entity.setAlwaysRefreshInstanceData(true);
        return entity;
    }

    public static Pair<FlareEntity, Byte> addCampaignFlareField(Vector2f location, int count, float facing, float arc, float fieldRadius, Vector4f sizeRangeXY, @Nullable Color fringeColor, @Nullable Color coreColor, float full, float fadeOut, CampaignEngineLayers layer) {
        return addCampaignFlareField(location, count, facing, arc, new Vector2f(0.0f, fieldRadius), null, sizeRangeXY, fringeColor, null, coreColor, null, true, false, 0.0f, full, fadeOut, 0.0f, true, layer);
    }

    public static Pair<FlareEntity, Byte> addCampaignFlareField(Vector2f location, int count, float facing, float arc, @Nullable Vector2f baseSpreadRange, @Nullable Vector2f facingRange, Vector4f sizeRangeXY, @Nullable Color baseFringeColor, @Nullable Color baseFringeColorShift, @Nullable Color baseCoreColor, @Nullable Color baseCoreColorShift, boolean flick, boolean syncFlick, float fadeIn, float full, float fadeOut, float timerOffsetRange, boolean isAdditiveBlend, CampaignEngineLayers layer) {
        FlareEntity entity = createFlareField(location, count, facing, arc, baseSpreadRange, facingRange, sizeRangeXY, baseFringeColor, baseFringeColorShift, baseCoreColor, baseCoreColorShift, flick, syncFlick, fadeIn, full, fadeOut, timerOffsetRange, isAdditiveBlend);
        entity.setLayer(layer);
        return new Pair<>(entity, CampaignRenderingManager.addEntity(BoxEnum.ENTITY_FLARE, entity));
    }

    public static Pair<FlareEntity, Byte> addCombatFlareField(Vector2f location, int count, float facing, float arc, float fieldRadius, Vector4f sizeRangeXY, @Nullable Color fringeColor, @Nullable Color coreColor, float full, float fadeOut, CombatEngineLayers layer) {
        return addCombatFlareField(location, count, facing, arc, new Vector2f(0.0f, fieldRadius), null, sizeRangeXY, fringeColor, null, coreColor, null, true, false, 0.0f, full, fadeOut, 0.0f, true, layer);
    }

    public static Pair<FlareEntity, Byte> addCombatFlareField(Vector2f location, int count, float facing, float arc, @Nullable Vector2f baseSpreadRange, @Nullable Vector2f facingRange, Vector4f sizeRangeXY, @Nullable Color baseFringeColor, @Nullable Color baseFringeColorShift, @Nullable Color baseCoreColor, @Nullable Color baseCoreColorShift, boolean flick, boolean syncFlick, float fadeIn, float full, float fadeOut, float timerOffsetRange, boolean isAdditiveBlend, CombatEngineLayers layer) {
        FlareEntity entity = createFlareField(location, count, facing, arc, baseSpreadRange, facingRange, sizeRangeXY, baseFringeColor, baseFringeColorShift, baseCoreColor, baseCoreColorShift, flick, syncFlick, fadeIn, full, fadeOut, timerOffsetRange, isAdditiveBlend);
        entity.setLayer(layer);
        return new Pair<>(entity, CombatRenderingManager.addEntity(BoxEnum.ENTITY_FLARE, entity));
    }

    public static TextFieldEntity createTextField(@NotNull String fontPath, float width, float height, @NotNull String text, float padding, Color baseColor, boolean italic, boolean underline, boolean strikeout, Color highlightColor, String... highlight) {
        TextFieldEntity entity = new TextFieldEntity(fontPath);
        float pad = padding;
        String[] splits = text.split("%s");
        String split;
        String hlText;
        for (int i = 0; i < splits.length; i++) {
            split = splits[i];
            entity.addText(split, pad, baseColor, false, italic, underline, strikeout, 0);
            if (i == (splits.length - 1)) break;
            if (i == 0) pad = 0.0f;
            hlText = (highlight != null && i < highlight.length) ? highlight[i] : "null";
            entity.addText(hlText, 0.0f, highlightColor, false, italic, underline, strikeout, 0);
        }
        entity.setFieldSize(width, height);
        entity.setTextDataRefreshAllFromCurrentIndex();
        entity.submitText();
        return entity;
    }

    public static TextFieldEntity createTextField(@NotNull String fontPath, float width, float height, @NotNull String text, float padding, Color baseColor, Color highlightColor, String... highlight) {
        return createTextField(fontPath, width, height, text, padding, baseColor, false, false, false, highlightColor, highlight);
    }

    public static TextFieldEntity createTextField(@NotNull String fontPath, float width, float height, @NotNull String text, float padding, Color baseColor) {
        return createTextField(fontPath, width, height, text, padding, baseColor, Misc.getHighlightColor(), (String) null);
    }

    public static TextFieldEntity createTextField(@NotNull String fontPath, float width, float height, @NotNull String text) {
        return createTextField(fontPath, width, height, text, 0.0f, Misc.getTextColor(), Misc.getHighlightColor(), (String) null);
    }

    private RenderingUtil() {}
}
