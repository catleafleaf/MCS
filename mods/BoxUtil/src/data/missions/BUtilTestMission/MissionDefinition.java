package data.missions.BUtilTestMission;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.boxutil.base.BaseShaderData;
import org.boxutil.base.SimpleParticleControlData;
import org.boxutil.base.api.InstanceDataAPI;
import org.boxutil.define.BoxEnum;
import org.boxutil.define.BoxGeometry;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.standard.attribute.Instance2Data;
import org.boxutil.units.standard.attribute.NodeData;
import org.boxutil.units.standard.entity.*;
import org.boxutil.units.standard.misc.ArcObject;
import org.boxutil.units.standard.misc.CurveObject;
import org.boxutil.units.standard.misc.NumberObject;
import org.boxutil.util.*;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {
    private final static float _MISSION_MAP_SIZE_HALF = 6400.0f;

    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER, "BOX", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "BOX", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "GL");
        api.setFleetTagline(FleetSide.ENEMY, "HF");
        api.addBriefingItem("Just test.");

        FleetMemberAPI fm = api.addToFleet(FleetSide.PLAYER, "onslaught_xiv_Elite", FleetMemberType.SHIP, "Test guy", true);
        fm.getVariant().addTag("TEST");

        api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, "Test guy", true);

        api.initMap(-_MISSION_MAP_SIZE_HALF, _MISSION_MAP_SIZE_HALF, -_MISSION_MAP_SIZE_HALF, _MISSION_MAP_SIZE_HALF);
        api.addPlugin(new Plugin());
    }

    private static final class Plugin extends BaseEveryFrameCombatPlugin {
        private CombatEngineAPI engine = null;
        private CommonEntity commonEntity = null;
        private DistortionEntity distortion = null;
        private FlareEntity flareEntity = null;
        private FlareEntity flareEntity2 = null;
        private CurveEntity curveEntity = null;
        private SegmentEntity segmentEntity = null;
        private TrailEntity trailEntity = null;
        private NumberObject locVec = new NumberObject();
        private ArcObject arc = new ArcObject();
        private SpriteEntity spriteEntity = null;
        private float time = 0.0f;
        private float time2 = 0.0f;
        private float time3 = 0.0f;
        private float time4 = 0.0f;
        private float hanabiTogTimer = 0.0f;
        private boolean tog = false;
        private boolean togHanabi = false;
        private TextFieldEntity textEntity = null;

        public void init(CombatEngineAPI engine) {
            this.engine = engine;
            engine.addLayeredRenderingPlugin(new RenderingPlugin());
            this.locVec.setIntegerLength(4);
            this.locVec.setDecimalLength(2);
            this.locVec.setColorWithoutAlpha(Misc.getPositiveHighlightColor());
            this.arc.setColorWithoutAlpha(Misc.getPositiveHighlightColor());
            this.arc.setRingHardness(0.99f);
            this.arc.setInner(0.95f, 0.95f);
            this.arc.setInnerHardness(0.8f);
            this.arc.setArcDirect(-1.0f);
        }

        public void advance(float amount, List<InputEventAPI> events) {
            this.time = this.engine.getTotalElapsedTime(false);
            ShipAPI ship = this.engine.getPlayerShip();
            if (ship == null) return;

            String id = "BoxTestMission";
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 0.1f);
            ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.1f);
            ship.getMutableStats().getWeaponDamageTakenMult().modifyMult(id, 0.1f);
            ship.getMutableStats().getEngineDamageTakenMult().modifyMult(id, 0.1f);
            ship.getMutableStats().getMaxSpeed().modifyMult(id, 4.0f);
            ship.getMutableStats().getAcceleration().modifyMult(id, 32.0f);
            ship.getMutableStats().getDeceleration().modifyMult(id, 32.0f);
            ship.getMutableStats().getMaxTurnRate().modifyMult(id, 16.0f);
            ship.getMutableStats().getTurnAcceleration().modifyMult(id, 32.0f);

            if (!this.engine.isPaused()) {
                if (this.time2 < 0.5f) this.time2 += amount;
                if (this.time2 >= 0.5f && Keyboard.isKeyDown(Keyboard.KEY_B)) {
                    this.time2 -= 0.5f;
                    NodeData start = new NodeData();
                    NodeData end = new NodeData();
                    start.setTangentRight(200.0f, 0.0f);
                    end.setLocation(1200.0f, 0.0f);
                    float a = (float) Math.random() * TrigUtil.PI2_F, c, s, length = (float) (Math.random() * 420.0f);
                    c = (float) Math.cos(a);
                    s = TrigUtil.sinFormCosRadiansF(c, a);
                    end.setTangentLeft(c * length, s * length);
                    CurveUtil.spawnCurveBeam(this.engine, new Vector3f(ship.getLocation().x, ship.getLocation().y, ship.getFacing()), start, end, ship, 200.0f, DamageType.ENERGY, 20.0f, true, Global.getSettings().getSprite("graphics/fx/beam_rough2_core.png"), Color.WHITE, Global.getSettings().getSprite("graphics/fx/beam_rough2_fringe.png"), Color.ORANGE, 16.0f, 512.0f, 0.1f, 1.5f, 0.9f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                }
                if (this.time3 < 1.0f) this.time3 += amount;
                if (this.time3 >= 0.5f && Keyboard.isKeyDown(Keyboard.KEY_N)) {
                    this.time3 -= 0.5f;
                    NodeData seg;
                    Matrix2f _transform = new Matrix2f();
                    TransformUtil.createSimpleRotateMatrix(ship.getFacing(), _transform);
                    SegmentEntity segmentEntity = new SegmentEntity();
                    for (BoundsAPI.SegmentAPI bound : ship.getExactBounds().getOrigSegments()) {
                        seg = new NodeData(bound.getP1());
                        seg.setWidth(16.0f);
                        seg.setColor(Color.GREEN);
                        segmentEntity.addNode(seg);
                        seg = new NodeData(bound.getP2());
                        seg.setWidth(4.0f);
                        seg.setColor(Color.GREEN);
                        segmentEntity.addNode(seg);
                    }
                    segmentEntity.setLocation(ship.getLocation());
                    segmentEntity.getModelMatrix().m00 = _transform.m00;
                    segmentEntity.getModelMatrix().m01 = _transform.m01;
                    segmentEntity.getModelMatrix().m10 = _transform.m10;
                    segmentEntity.getModelMatrix().m11 = _transform.m11;
                    segmentEntity.setNodeRefreshAllFromCurrentIndex();
                    segmentEntity.submitNodes();
                    segmentEntity.getMaterialData().setDiffuse(Global.getSettings().getSprite("graphics/fx/beam_weave_fringe.png"));
                    segmentEntity.setTexturePixels(256.0f);
                    segmentEntity.setTextureSpeed(100.0f);
                    segmentEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
                    segmentEntity.setAdditiveBlend();
                    segmentEntity.setGlobalTimer(0.0f, 1.5f, 0.5f);
                    CombatRenderingManager.addEntity(BoxEnum.ENTITY_SEGMENT, segmentEntity);

                    float a, c, s;
                    a = (float) Math.toRadians(ship.getFacing());
                    c = (float) Math.cos(a);
                    s = TrigUtil.sinFormCosRadiansF(c, a);
                    Vector2f beamTo = new Vector2f(c * 1500, s * 1500);
                    Vector2f.add(ship.getLocation(), beamTo, beamTo);
                    CurveUtil.spawnDirectBeam(this.engine, ship.getLocation(), beamTo, ship, 200.0f, DamageType.ENERGY, 20.0f, true, Global.getSettings().getSprite("graphics/fx/beam_rough2_core.png"), Color.WHITE, Global.getSettings().getSprite("graphics/fx/beam_rough2_fringe.png"), Color.ORANGE, 16.0f, 512.0f, 0.1f, 1.5f, 0.9f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                }

                if (this.togHanabi) {
                    if (this.time4 >= 0.5f) {
                        this.time4 -= 0.5f;
                        short count = 256;
                        List<InstanceDataAPI> particleList = new ArrayList<>(count);
                        Instance2Data instance;
                        float a, c, s, length;
                        for (short i = 0; i < count; i++) {
                            instance = new Instance2Data();
                            a = (float) Math.random() * TrigUtil.PI2_F;
                            c = (float) Math.cos(a);
                            s = TrigUtil.sinFormCosRadiansF(c, a);
                            length = (float) Math.random() * 128.0f;
                            instance.setLocation(c * length, s * length);
                            length = (float) Math.random() * 150.0f + 200.0f;
                            instance.setVelocity(c * length, s * length);
                            instance.setFacing((float) Math.random() * 360.0f);
                            instance.setTurnRate((float) Math.random() * 45.0f - 22.5f);
                            length = (float) Math.random() + 0.6f;
                            instance.setScale(length, length);
                            instance.setScaleRate(1.0f, 1.0f);
                            instance.setTimer(0.05f, 0.2f, (float) Math.random() + 0.5f);
                            particleList.add(instance);
                        }

                        SpriteEntity particleEntity = new SpriteEntity("graphics/portraits/characters/sebestyen.png");
                        particleEntity.getMaterialData().setColor(new Color(255, 220, 200, 180));
                        particleEntity.setBaseSizePerTiles(16.0f, 16.0f);
                        particleEntity.setAdditiveBlend();
                        particleEntity.setLocation(this.engine.getViewport().convertScreenXToWorldX(Mouse.getX()), this.engine.getViewport().convertScreenYToWorldY(Mouse.getY()));
                        particleEntity.setInstanceData(particleList, 0.05f, 0.2f, 1.5f);
                        particleEntity.setInstanceDataRefreshAllFromCurrentIndex();
                        particleEntity.submitInstanceData();
                        particleEntity.setRenderingCount(count);
                        particleEntity.setAlwaysRefreshInstanceData(true);

                        particleEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
                        CombatRenderingManager.addEntity(BoxEnum.ENTITY_SPRITE, particleEntity);

                    } else this.time4 += amount;
                }

                if (this.hanabiTogTimer >= 0.5f && Keyboard.isKeyDown(Keyboard.KEY_H)) {
                    this.hanabiTogTimer -= 0.5f;
                    this.togHanabi = !this.togHanabi;
                } else if (this.hanabiTogTimer < 0.5f) this.hanabiTogTimer += amount;
            }

            if (this.flareEntity != null) {
                ((Instance2Data) this.flareEntity.getInstanceData().get(0)).setLocation(ship.getLocation());
                this.flareEntity.setInstanceDataRefreshAllFromCurrentIndex();
                this.flareEntity.submitFixedInstanceData();
//                this.flareEntity.appendToEntity(ship);
                this.flareEntity2.setStateVanilla(new Vector2f(ship.getLocation().x, ship.getLocation().y + 100.0f), 0.0f);
//                this.flareEntity.setLocation(ship.getLocation().x + 100.0f, ship.getLocation().y + 200.0f);
            } else {
                this.flareEntity = new FlareEntity();
                this.flareEntity.setSize(640, 16);
                this.flareEntity.setFlick(true);
                this.flareEntity.setFlickWhenPaused(false);
                this.flareEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
                this.flareEntity.setSmoothDisc();
                this.flareEntity.setFringeColor(Misc.getNegativeHighlightColor());
                this.flareEntity.setCoreColor(Color.WHITE);
                this.flareEntity.setCoreAlpha(1.0f);
                this.flareEntity.setFringeAlpha(1.0f);
                this.flareEntity.setNormalBlend();
                this.flareEntity.setNoisePower(0.33f);
                this.flareEntity.autoAspect();
                this.flareEntity.mallocFixedInstanceData(1);
                Instance2Data fixedA = new Instance2Data();
                fixedA.setScaleAll(1.0f);
//                fixedA.setFixedInstanceAlpha(1.0f, BoxEnum.TIMER_FULL);
                this.flareEntity.addInstanceData(fixedA);
                this.flareEntity.setGlobalTimer(0.0f, 8192.0f, 2.0f);
                CombatRenderingManager.addEntity(BoxEnum.ENTITY_FLARE, this.flareEntity);

                Pair<FlareEntity, Byte> p = RenderingUtil.addCombatFlareField(ship.getLocation(), 128, ship.getFacing(), 360, 512.0f, new Vector4f(96, 5, 256, 12), Misc.getPositiveHighlightColor(), Color.WHITE, 8.0f, 3.0f, CombatEngineLayers.ABOVE_PARTICLES);
                this.flareEntity2 = p.one;
                this.flareEntity2.setAdditiveBlend();
                this.flareEntity2.setSmoothDisc();
                this.flareEntity2.setFringeAlpha(1.0f);
                this.flareEntity2.setCoreAlpha(1.0f);
                this.flareEntity2.setGlowPower(1.0f);
            }
            if (this.curveEntity != null) {
                this.curveEntity.setLocation(ship.getLocation().x + 100.0f, ship.getLocation().y - 200.0f);
            } else {
                this.curveEntity = new CurveEntity().initElliptic(null, 256, 128.0f, Misc.getNegativeHighlightColor(), new Color(15, 16, 17, 18), 12.0f);
                this.curveEntity.getMaterialData().setDiffuse(Global.getSettings().getSprite("graphics/fx/beam_weave_fringe.png"));
                this.curveEntity.setInterpolation((short) 64);
                this.curveEntity.setTexturePixels(256.0f);
                this.curveEntity.setTextureSpeed(100.0f);
                this.curveEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
                this.curveEntity.setAdditiveBlend();
                this.curveEntity.setGlobalTimer(1.0f, 8192.0f, 1.0f);
                this.curveEntity.setFillStartAlpha(0.0f);
                this.curveEntity.setFillEndAlpha(0.0f);
                this.curveEntity.setFillStartFactor(0.95f);
                this.curveEntity.setFillEndFactor(0.25f);
                CombatRenderingManager.addEntity(BoxEnum.ENTITY_CURVE, this.curveEntity);
            }
            if (this.segmentEntity != null) {
                this.segmentEntity.setStateVanilla(new Vector2f(ship.getLocation().x, ship.getLocation().y), ship.getFacing() + 90.0f);
            } else {
                List<Vector2f> points = new ArrayList<>(5);
                points.add(new Vector2f(-100.0f, -100.0f));
                points.add(new Vector2f(100.0f, -100.0f));
                points.add(new Vector2f(100.0f, 100.0f));
                points.add(new Vector2f(-100.0f, 100.0f));
                points.add(new Vector2f(-150.0f, 0.0f));
                this.segmentEntity = new SegmentEntity().initLineStrip(points, Color.ORANGE, Color.WHITE, 16.0f, true);
                this.segmentEntity.getMaterialData().setDiffuse(Global.getSettings().getSprite("graphics/fx/beam_weave_fringe.png"));
                this.segmentEntity.getMaterialData().setEmissive(Global.getSettings().getSprite("graphics/fx/beam_rough2_core.png"));
                this.segmentEntity.getMaterialData().setEmissiveColorAlpha(0.7f);
                this.segmentEntity.setTexturePixels(256.0f);
                this.segmentEntity.setTextureSpeed(100.0f);
                this.segmentEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
                this.segmentEntity.setAdditiveBlend();
                this.segmentEntity.setGlobalTimer(0.0f, 8192.0f, 2.0f);
                CombatRenderingManager.addEntity(BoxEnum.ENTITY_SEGMENT, this.segmentEntity);
            }
            if (this.trailEntity != null) {
                this.trailEntity.getNodes().get(1).set(Mouse.getX(), Mouse.getY());
                this.trailEntity.setNodeRefreshIndex(1);
                this.trailEntity.setNodeRefreshSize(2);
                this.trailEntity.submitNodes();
            } else {
                this.trailEntity = new TrailEntity();
                this.trailEntity.getMaterialData().setDiffuse(Global.getSettings().getSprite("graphics/fx/beam_weave_fringe.png"));
                this.trailEntity.getMaterialData().setEmissive(Global.getSettings().getSprite("graphics/fx/beam_rough2_core.png"));
                this.trailEntity.getMaterialData().setEmissiveColor(Color.ORANGE);
                this.trailEntity.getMaterialData().setEmissiveColorAlpha(0.7f);
                this.trailEntity.setEndColor(Color.RED);
                this.trailEntity.setStartWidth(16.0f);
                this.trailEntity.setEndWidth(32.0f);
                this.trailEntity.setJitterPower(0.2f);
                this.trailEntity.setFlick(true);
                this.trailEntity.setFlickMixValue(0.5f);
                this.trailEntity.setTexturePixels(256.0f);
                this.trailEntity.setTextureSpeed(100.0f);
                this.trailEntity.setFillStartAlpha(0.0f);
                this.trailEntity.setFillEndAlpha(0.0f);
                this.trailEntity.setFillStartFactor(0.9f);
                this.trailEntity.setFillEndFactor(0.9f);
                this.trailEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
                this.trailEntity.setAdditiveBlend();
                this.trailEntity.setCustomPrimeMatrix();
                this.trailEntity.setPrimeMatrix(TransformUtil.createWindowOrthoMatrix(null));
                this.trailEntity.addNode(new Vector2f(512.0f, 128.0f));
                this.trailEntity.addNode(new Vector2f(Mouse.getX(), Mouse.getY()));
                this.trailEntity.addNode(new Vector2f(512.0f, 512.0f));
                this.trailEntity.setNodeRefreshAllFromCurrentIndex();
                this.trailEntity.submitNodes();
                this.trailEntity.setGlobalTimer(0.0f, 8192.0f, 2.0f);
                CombatRenderingManager.addEntity(BoxEnum.ENTITY_TRAIL, this.trailEntity);
            }
            if (this.distortion != null) {
                if (Global.getCombatEngine().getPlayerShip() != null) this.distortion.setLocation(this.engine.getPlayerShip().getLocation());
            } else {
                this.distortion = new DistortionEntity();
                this.distortion.setGlobalTimer(5.0f, 3.0f, 5.0f);
                this.distortion.setInnerFull(0.7f, 0.2f);
                this.distortion.setInnerHardness(0.8f);
                this.distortion.setSizeIn(256, 256);
                this.distortion.setPowerIn(0);
                this.distortion.setPowerFull(1);
                this.distortion.setPowerOut(0);
                this.distortion.setSizeFull(128, 128);
                this.distortion.setSizeOut(96, 96);
                CombatRenderingManager.addEntity(BoxEnum.ENTITY_DISTORTION, this.distortion);
            }
            if (this.commonEntity != null) {
                this.commonEntity.setModelMatrix(TransformUtil.createModelMatrixRotateOnly(TransformUtil.rotationZXY(
                                        ship.getFacing(), (this.engine.getTotalElapsedTime(false) * 60.0f) % 360.0f, 0.0f
                                ), null));
                this.commonEntity.getModelMatrix().m30 = ship.getLocation().x;
                this.commonEntity.getModelMatrix().m31 = ship.getLocation().y;
            } else {
                this.commonEntity = new CommonEntity(BoxGeometry.DEMO_BOX, true);
                this.commonEntity.setBaseSize3D(200, 200, 200);
                this.commonEntity.setLayer(CombatEngineLayers.ABOVE_SHIPS_LAYER);
                this.commonEntity.setGlobalTimer(1.0f, 8192.0f, 0.0f);
                CombatRenderingManager.addEntity(BoxEnum.ENTITY_COMMON, this.commonEntity);
            }
            if (this.spriteEntity != null) {
                this.spriteEntity.setLocation(this.engine.getViewport().getCenter());
            } else {
                this.spriteEntity = new SpriteEntity("graphics/fx/dust_clouds_colorless.png");
                this.spriteEntity.setEmissiveSprite("graphics/fx/fx_clouds01.png");
                this.spriteEntity.setAdditiveBlend();
                this.spriteEntity.setTileSize(4, 4);
                this.spriteEntity.setStartingFormIndex(4);
                this.spriteEntity.setRandomTile(true);
                this.spriteEntity.setBaseSizePerTiles(100, 100);
                this.spriteEntity.getMaterialData().setColor(1.0f, 0.2f, 0.0f, 1.0f);
                this.spriteEntity.getMaterialData().setEmissiveColor(1.0f, 0.8f, 0.3f, 1.0f);
                this.spriteEntity.setLayer(CombatEngineLayers.JUST_BELOW_WIDGETS);
                this.spriteEntity.setGlobalTimer(1.0f, 8192.0f, 0.0f);
                CombatRenderingManager.addEntity(BoxEnum.ENTITY_SPRITE, this.spriteEntity);
            }
            if (this.engine.isCombatOver()) {
                if (this.textEntity != null) {
                    this.textEntity.delete();
                    this.textEntity = null;
                }
            } else {
                if (this.textEntity == null) {
                    this.textEntity = new TextFieldEntity("graphics/fonts/insignia15LTaa.fnt");
                    this.textEntity.addText("Text can rendering at everywhere :)" + TextFieldEntity.LINE_FEED_SYMBOL,
                            0.0f, Misc.getButtonTextColor(),
                            false, true, false, false, 0);
                    this.textEntity.addText("Add to 'Rendering manager' or 'directDraw' both ok." + TextFieldEntity.LINE_FEED_SYMBOL,
                            0.0f, Misc.getButtonTextColor(),
                            false, true, true, false, 0);
                    this.textEntity.addText("Press 'B' to spawn curve beam." + TextFieldEntity.LINE_FEED_SYMBOL,
                            5.0f, Misc.getButtonTextColor(),
                            false, true, true, false, 0);
                    this.textEntity.addText("Press 'N' to show your ship bounds for 2 second." + TextFieldEntity.LINE_FEED_SYMBOL,
                            5.0f, Misc.getButtonTextColor(),
                            false, true, true, false, 0);
                    this.textEntity.addText("Press 'H' to toggle switch for enjoy Hanabi.",
                            5.0f, Misc.getNegativeHighlightColor(),
                            false, true, true, true, 0);
                    this.textEntity.setFontSpace(0.0f, 5.0f);
                    this.textEntity.setFieldSize(512.0f, 128.0f);
                    this.textEntity.setTextDataRefreshAllFromCurrentIndex();
                    this.textEntity.submitText();
                    this.textEntity.setCustomPrimeMatrix();
                    this.textEntity.setPrimeMatrix(TransformUtil.createWindowOrthoMatrix(null));
                    this.textEntity.setLocation(2.0f, (ShaderCore.getScreenHeight() + this.textEntity.getCurrentVisualHeight()) * 0.5f);
//                CombatRenderingManager.addEntity(BoxEnum.ENTITY_TEXT, this.textEntity);
                } else {
                    this.textEntity.directDraw(false);
                    if (this.engine.getTotalElapsedTime(false) > 5.0f && !this.tog) {
                        this.tog = true;
                        TextFieldEntity.TextData a = this.textEntity.getTextDataList().get(1);
                        a.text = "change." + TextFieldEntity.LINE_FEED_SYMBOL;
                        this.textEntity.setTextDataRefreshIndex(1);
                        this.textEntity.setTextDataRefreshAllFromCurrentIndex();
                        this.textEntity.submitText();
                    }
                }
            }
        }

        public void renderInUICoords(ViewportAPI viewport) {
            if (this.engine.getPlayerShip() == null) return;
            ShipAPI player = this.engine.getPlayerShip();
            this.locVec.setAlpha(viewport.getAlphaMult() * 0.8f);
            this.arc.setAlpha(viewport.getAlphaMult() * 0.5f);
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glTranslatef(ShaderCore.getScreenWidth() * 0.5f, ShaderCore.getScreenHeight() + 82.0f, 0.0f);
            this.arc.glDraw(225.0f, 225.0f);
            GL11.glTranslatef(-112.0f, -112.0f, 0.0f);
            this.locVec.glDraw(player.getLocation().x, 96.0f, 30.0f);
            GL11.glTranslatef(0.0f, -34.0f, 0.0f);
            this.locVec.glDraw(player.getLocation().y, 96.0f, 30.0f);
            GL11.glTranslatef(0.0f, -34.0f, 0.0f);
            this.locVec.setColorWithoutAlpha(Misc.getButtonTextColor());
            this.locVec.glDraw(player.getAngularVelocity(), 96.0f, 30.0f);
            GL11.glTranslatef(128.0f, 0.0f, 0.0f);
            this.locVec.glDraw(player.getFacing(), 96.0f, 30.0f);
            GL11.glTranslatef(0.0f, 34.0f, 0.0f);
            this.locVec.setColorWithoutAlpha(Misc.getPositiveHighlightColor());
            this.locVec.glDraw(player.getVelocity().y, 96.0f, 30.0f);
            GL11.glTranslatef(0.0f, 34.0f, 0.0f);
            this.locVec.glDraw(player.getVelocity().x, 96.0f, 30.0f);
            GL11.glPopMatrix();
        }

        private final class RenderingPlugin extends BaseCombatLayeredRenderingPlugin {
            private TextFieldEntity text = null;

            public void render(CombatEngineLayers layer, ViewportAPI viewport) {
                if (layer == RenderingUtil.getLowestCombatLayer()) {
                    BaseShaderData program = ShaderCore.getTestMissionProgram();
                    if (program == null || !program.isValid()) return;
                    int currMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                    GL11.glViewport(0, 0, ShaderCore.getScreenScaleWidth(), ShaderCore.getScreenScaleHeight());
                    GL11.glMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPushMatrix();
                    GL11.glLoadIdentity();
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    GL11.glPushMatrix();
                    GL11.glLoadIdentity();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    program.active();
                    GL20.glUniform1f(program.location[0], time);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(-1.0f, -1.0f);
                    GL11.glVertex2f(-1.0f, 1.0f);
                    GL11.glVertex2f(1.0f, 1.0f);
                    GL11.glVertex2f(1.0f, -1.0f);
                    GL11.glEnd();
                    program.close();
                    GL11.glPopMatrix();
                    GL11.glMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPopMatrix();
                    GL11.glPopAttrib();
                    GL11.glMatrixMode(currMatrixMode);
                }
                if (layer == RenderingUtil.getHighestCombatLayer()) {
                    if (engine.isCombatOver()) {
                        if (this.text != null) {
                            this.text.delete();
                            this.text = null;
                        }
                    } else {
                        if (this.text == null) {
                            this.text = new TextFieldEntity("graphics/fonts/orbitron20aabold.fnt");
                            this.text.addText("#TEST-TEST-TEST#" + TextFieldEntity.LINE_FEED_SYMBOL,
                                    0.0f, Misc.getButtonTextColor(),
                                    false, false, true, true, 0);
                            this.text.addText(" !@#$%^&*()_+-=1234567890`~[]{};':\",.<>/?|\\" + TextFieldEntity.LINE_FEED_SYMBOL,
                                    0.0f, Misc.getPositiveHighlightColor(),
                                    false, true, false, false, 0);
                            this.text.addText("AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz" + TextFieldEntity.LINE_FEED_SYMBOL,
                                    0.0f, Misc.getPositiveHighlightColor(),
                                    false, true, true, false, 0);
                            this.text.addText("#test-test-test#",
                                    10.0f, Misc.getNegativeHighlightColor(),
                                    true, false, false, true, 0);
                            this.text.setAlignment(TextFieldEntity.Alignment.MID);
                            this.text.setFontSpace(8.0f, 5.0f);
                            this.text.setFieldSize(350.0f, 512.0f);
                            this.text.setTextDataRefreshAllFromCurrentIndex();
                            this.text.submitText();
                            this.text.setItalicFactor(90.0f);
                        } else {
                            if (engine.getPlayerShip() != null) this.text.setLocation(engine.getPlayerShip().getLocation());
                            this.text.directDraw(false);
                        }
                    }
                }
            }

            public float getRenderRadius() {
                return Float.MAX_VALUE;
            }

            public EnumSet<CombatEngineLayers> getActiveLayers() {
                return EnumSet.allOf(CombatEngineLayers.class);
            }
        }
    }
}