package org.boxutil.manager;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.boxutil.base.api.*;
import org.jetbrains.annotations.NotNull;
import org.boxutil.base.BaseShaderData;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.units.builtin.shader.BUtil_CampaignRenderingEntity;
import org.boxutil.units.standard.attribute.MaterialData;
import org.boxutil.units.standard.entity.*;
import org.boxutil.util.CalculateUtil;
import org.boxutil.util.RenderingUtil;
import org.boxutil.util.ShaderUtil;
import org.boxutil.util.TransformUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class CampaignRenderingManager implements EveryFrameScript {
    private final static HashMap<CampaignEngineLayers, List<RenderDataAPI>[]> _ENTITIES_MAP = new HashMap<>();
    private final static List<RenderDataAPI>[] _DIRECT_MAP = new ArrayList[BoxDatabase.ENTITY_TOTAL_TYPE_DIRECT];
    static {
        for (CampaignEngineLayers layer : CampaignEngineLayers.values()) {
            if (!_ENTITIES_MAP.containsKey(layer)) {
                ArrayList<RenderDataAPI>[] lists = new ArrayList[BoxDatabase.ENTITY_TOTAL_TYPE_NORMAL];
                for (int i = 0; i < BoxDatabase.ENTITY_TOTAL_TYPE_NORMAL; i++) {
                    lists[i] = new ArrayList<>();
                }
                _ENTITIES_MAP.put(layer, lists);
            }
        }
        for (int i = 0; i < BoxDatabase.ENTITY_TOTAL_TYPE_DIRECT; i++) {
            _DIRECT_MAP[i] = new ArrayList<>();
        }
    }
    private final static Matrix4f _ORTHO_VIEWPORT = new Matrix4f();
    private final static Matrix4f _PERSPECTIVE_VIEWPORT = new Matrix4f();
    private final static float[] _TIMER_FRACTION = new float[2];
    private final transient FloatBuffer vanillaMatrix = BufferUtils.createFloatBuffer(16);
    private final transient FloatBuffer perspectiveMatrix = BufferUtils.createFloatBuffer(16);
    private transient float lastFrameAmount = 0.0f;
    private transient float frameTime = 0.0f;
    private transient float frameTimePausedCheck = 0.0f;
    private transient boolean idle = true;
    private transient boolean needAA = false;
    private transient CampaignFleetAPI playerFleet = null;
    private transient CustomCampaignEntityAPI _campaignManage = null;

    public CampaignRenderingManager() {
        _TIMER_FRACTION[0] = 0.0f;
        _TIMER_FRACTION[1] = 0.0f;
        Global.getLogger(CampaignRenderingManager.class).info("'BoxUtil' Campaign rendering manage invited!");
    }

    public void advance(float amount) {
        if (Global.getSector() == null) return;
        SectorAPI sector = Global.getSector();
        if (sector.getPlayerFleet() == null || sector.getPlayerFleet().getContainingLocation() == null) return;
        this.playerFleet = sector.getPlayerFleet();

        if (this._campaignManage == null) {
            for (CustomCampaignEntityAPI entity : this.playerFleet.getContainingLocation().getCustomEntitiesWithTag(BoxDatabase.CAMPAIGN_MANAGE_TAG)) {
                if (entity == null) continue;
                this.playerFleet.getContainingLocation().removeEntity(entity);
            }
            CustomCampaignEntityAPI BUtil_CampaignManage = this.playerFleet.getContainingLocation().addCustomEntity(BoxDatabase.CAMPAIGN_MANAGE_ID, BoxDatabase.CAMPAIGN_MANAGE_ID, BoxDatabase.CAMPAIGN_MANAGE_ID, Factions.NEUTRAL, this);
            BUtil_CampaignManage.addTag(BoxDatabase.CAMPAIGN_MANAGE_TAG);
            BUtil_CampaignManage.setActiveLayers(CampaignEngineLayers.values());
            this._campaignManage = BUtil_CampaignManage;
        } else {
            if (this._campaignManage.getContainingLocation() != this.playerFleet.getContainingLocation()) {
                CampaignRenderingManager.clearRenderEntity();
                this._campaignManage.getContainingLocation().removeEntity(this._campaignManage);

                for (CustomCampaignEntityAPI entity : this.playerFleet.getContainingLocation().getCustomEntitiesWithTag(BoxDatabase.CAMPAIGN_MANAGE_TAG)) {
                    if (entity == null) continue;
                    this.playerFleet.getContainingLocation().removeEntity(entity);
                }

                this._campaignManage.setContainingLocation(this.playerFleet.getContainingLocation());
                this.playerFleet.getContainingLocation().addEntity(this._campaignManage);
            }
            this._campaignManage.setFixedLocation(this.playerFleet.getLocation().x, this.playerFleet.getLocation().y + ShaderCore.getScreenScaleHeight());
            BUtil_CampaignRenderingEntity.check(this);
        }

        final boolean isGLParallel = BoxConfigs.isGLParallel();
        final boolean isPaused = Global.getSector().isPaused();

        this.lastFrameAmount = amount;
        this.frameTime += amount;
        if (!isPaused) this.frameTimePausedCheck += amount;
        _TIMER_FRACTION[1] += amount;
        if (_TIMER_FRACTION[1] >= 1.0f) _TIMER_FRACTION[1] -= 1.0f;
        if (!isPaused) {
            _TIMER_FRACTION[0] += amount;
            if (_TIMER_FRACTION[0] >= 1.0f) _TIMER_FRACTION[0] -= 1.0f;
        }
        if (isGLParallel) {
            ShaderCore.getInstanceMatrixF32Program().active();
            ShaderCore.getInstanceMatrixF32Program().putUniformSubroutine(GL43.GL_COMPUTE_SHADER, 0, 0);
        }
        for (List<RenderDataAPI>[] lists : _ENTITIES_MAP.values()) {
            for (int i = 0; i < BoxDatabase.ENTITY_TOTAL_TYPE_NORMAL; i++) {
                forEntities(lists[i], amount, isPaused);
            }
        }
        for (List<RenderDataAPI> list : _DIRECT_MAP) {
            forEntities(list, amount, isPaused);
        }
        if (isGLParallel) {
            ShaderCore.getInstanceMatrixF32Program().close();
            GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }
    }

    private static void forEntities(List<RenderDataAPI> list, float amount, boolean isPaused) {
        if (list == null || list.isEmpty()) return;
        RenderDataAPI entity;
        ControlDataAPI data;
        InstanceRenderAPI instance;
        BaseShaderData instanceProgram = ShaderCore.getInstanceMatrixF32Program();
        for (Iterator<RenderDataAPI> entitiesI = list.iterator(); entitiesI.hasNext();) {
            entity = entitiesI.next();
            if (entity == null) continue;

            boolean toRemove;
            data = entity.getControlData();
            boolean haveData = data != null;
            if (entity.hasDelete()) {
                entitiesI.remove();
                continue;
            }
            entity.advanceGlobalTimer(amount, isPaused);
            if (haveData) {
                if (!data.controlRunWhilePaused(entity) && isPaused) continue;
                else data.controlAdvance(entity, amount);
                toRemove = data.controlIsDone(entity);
                if (data.controlRemoveBasedTimer(entity)) toRemove |= entity.getGlobalTimerState() == BoxEnum.TIMER_INVALID;
            } else toRemove = entity.getGlobalTimerState() == BoxEnum.TIMER_INVALID;
            if (BoxConfigs.isShaderEnable() && entity instanceof InstanceRenderAPI && !toRemove) {
                instance = (InstanceRenderAPI) entity;
                if (instance.haveValidInstanceData() && instance.isNeedRefreshInstanceData()) {
                    boolean gl3D = BoxConfigs.isGLParallel();
                    boolean glF16 = gl3D && instance.isHalfFloatFormatInstanceData();
                    boolean glNotCustom = gl3D && !instance.isInstanceDataCustom();
                    gl3D &= instance.isInstanceData3D();
                    if (glNotCustom) {
                        if (glF16) {
                            instanceProgram.close();
                            instanceProgram = ShaderCore.getInstanceMatrixF16Program();
                            instanceProgram.active();
                        }
                        if (gl3D) instanceProgram.putUniformSubroutine(GL43.GL_COMPUTE_SHADER, 0, 1);
                    } else instanceProgram.close();
                    instance.sysRefreshInstanceData(amount, isPaused);
                    if (glNotCustom) {
                        if (gl3D) instanceProgram.putUniformSubroutine(GL43.GL_COMPUTE_SHADER, 0, 0);
                        if (glF16) {
                            instanceProgram.close();
                            instanceProgram = ShaderCore.getInstanceMatrixF32Program();
                            instanceProgram.active();
                        }
                    } else instanceProgram.active();
                }
            }
            if (toRemove) {
                entity.delete();
                entitiesI.remove();
            }
        }
    }

    public void render(CampaignEngineLayers layer, ViewportAPI viewport, SectorEntityToken campaignEntity) {
        if (this.playerFleet == null || campaignEntity == null || this.playerFleet.getContainingLocation() != campaignEntity.getContainingLocation()) return;
        final boolean shaderEnable = BoxConfigs.isShaderEnable();
        if (layer == RenderingUtil.getLowestCampaignLayer()) {
            TransformUtil.createGameOrthoMatrix(viewport, _ORTHO_VIEWPORT);
            TransformUtil.createGamePerspectiveMatrix(30.0f, viewport, _PERSPECTIVE_VIEWPORT);
            {
                this.vanillaMatrix.put(0, _ORTHO_VIEWPORT.m00);
                this.vanillaMatrix.put(1, _ORTHO_VIEWPORT.m01);
                this.vanillaMatrix.put(2, _ORTHO_VIEWPORT.m02);
                this.vanillaMatrix.put(3, _ORTHO_VIEWPORT.m03);
                this.vanillaMatrix.put(4, _ORTHO_VIEWPORT.m10);
                this.vanillaMatrix.put(5, _ORTHO_VIEWPORT.m11);
                this.vanillaMatrix.put(6, _ORTHO_VIEWPORT.m12);
                this.vanillaMatrix.put(7, _ORTHO_VIEWPORT.m13);
                this.vanillaMatrix.put(8, _ORTHO_VIEWPORT.m20);
                this.vanillaMatrix.put(9, _ORTHO_VIEWPORT.m21);
                this.vanillaMatrix.put(10, _ORTHO_VIEWPORT.m22);
                this.vanillaMatrix.put(11, _ORTHO_VIEWPORT.m23);
                this.vanillaMatrix.put(12, _ORTHO_VIEWPORT.m30);
                this.vanillaMatrix.put(13, _ORTHO_VIEWPORT.m31);
                this.vanillaMatrix.put(14, _ORTHO_VIEWPORT.m32);
                this.vanillaMatrix.put(15, _ORTHO_VIEWPORT.m33);
            }
            {
                this.perspectiveMatrix.put(0, _PERSPECTIVE_VIEWPORT.m00);
                this.perspectiveMatrix.put(1, _PERSPECTIVE_VIEWPORT.m01);
                this.perspectiveMatrix.put(2, _PERSPECTIVE_VIEWPORT.m02);
                this.perspectiveMatrix.put(3, _PERSPECTIVE_VIEWPORT.m03);
                this.perspectiveMatrix.put(4, _PERSPECTIVE_VIEWPORT.m10);
                this.perspectiveMatrix.put(5, _PERSPECTIVE_VIEWPORT.m11);
                this.perspectiveMatrix.put(6, _PERSPECTIVE_VIEWPORT.m12);
                this.perspectiveMatrix.put(7, _PERSPECTIVE_VIEWPORT.m13);
                this.perspectiveMatrix.put(8, _PERSPECTIVE_VIEWPORT.m20);
                this.perspectiveMatrix.put(9, _PERSPECTIVE_VIEWPORT.m21);
                this.perspectiveMatrix.put(10, _PERSPECTIVE_VIEWPORT.m22);
                this.perspectiveMatrix.put(11, _PERSPECTIVE_VIEWPORT.m23);
                this.perspectiveMatrix.put(12, _PERSPECTIVE_VIEWPORT.m30);
                this.perspectiveMatrix.put(13, _PERSPECTIVE_VIEWPORT.m31);
                this.perspectiveMatrix.put(14, _PERSPECTIVE_VIEWPORT.m32);
                this.perspectiveMatrix.put(15, _PERSPECTIVE_VIEWPORT.m33);
            }
            this.vanillaMatrix.position(0);
            this.vanillaMatrix.limit(this.vanillaMatrix.capacity());
            this.perspectiveMatrix.position(0);
            this.perspectiveMatrix.limit(this.perspectiveMatrix.capacity());
            if (shaderEnable) {
                ShaderCore.refreshGameVanillaViewportUBOAll(this.vanillaMatrix, viewport);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, ShaderCore.getRenderingBuffer().getFBO(0));
                GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            }
            this.idle = true;
            this.needAA = false;
        }

        BaseShaderData program;
        RenderDataAPI entity;
        ControlDataAPI data;
        InstanceRenderAPI instance;
        MaterialData material;
        List<RenderDataAPI> commonList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_COMMON];
        List<RenderDataAPI> spriteList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_SPRITE];
        List<RenderDataAPI> curveList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_CURVE];
        List<RenderDataAPI> segmentList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_SEGMENT];
        List<RenderDataAPI> trailList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_TRAIL];
        List<RenderDataAPI> flareList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_FLARE];
        List<RenderDataAPI> customList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_CUSTOM];
        List<RenderDataAPI> textList = _ENTITIES_MAP.get(layer)[BoxEnum.ENTITY_TEXT];
        List<RenderDataAPI> distortionList = _DIRECT_MAP[BoxEnum.ENTITY_DISTORTION];
        List<RenderDataAPI> illuminantList = _DIRECT_MAP[BoxEnum.ENTITY_DISTORTION];
        boolean notMultiPass = shaderEnable;

        if (shaderEnable) ShaderCore.glBeginDraw();
        int[] vertexSub;
        int[] fragSub;
        int instanceBit;
        if (layer == RenderingUtil.getHighestCampaignLayer()) {
            if (!illuminantList.isEmpty()) {

            }

            notMultiPass &= BoxConfigs.isMultiPassBeauty();
            if (shaderEnable) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_BLEND);
                ShaderCore.glMultiPass();
                if (BoxConfigs.isMultiPassBeauty()) ShaderCore.glApplyAA();
                if (!this.idle && (BoxConfigs.isMultiPassBeauty() || BoxConfigs.isMultiPassBloom())) {
                    int bloomTex = ShaderCore.glDrawBloom();
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, bloomTex > 0 ? bloomTex : BoxDatabase.BUtil_ZERO.getTextureId());
                    ShaderCore.getDirectDrawProgram().active();
                    GL20.glUniform1f(ShaderCore.getDirectDrawProgram().location[0], 1.0f);
                    if (!BoxConfigs.isMultiPassBloom()) {
                        GL11.glEnable(GL11.GL_BLEND);
                        GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
                        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
                    }
                    ShaderCore.getDefaultQuadObject().glDraw();
                    ShaderCore.getDirectDrawProgram().close();
                }
            }

            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
            if (!distortionList.isEmpty()) {
                if (BoxConfigs.isDistortionEnable() && notMultiPass) {
                    program = ShaderCore.getDistortionProgram();
                    program.active();
                    ShaderUtil.copyFromScreen(ShaderCore.getRenderingBuffer().getFBO(1));
                    program.bindTexture2D(0, ShaderCore.getRenderingBuffer().getColorResult(1));
                    program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 1);
                    GL20.glUniform1f(program.location[2], viewport.getViewMult());
                    instanceBit = 1;
                    for (Iterator<RenderDataAPI> entitiesI = distortionList.iterator(); entitiesI.hasNext();) {
                        entity = entitiesI.next();
                        if (entity == null) {
                            entitiesI.remove();
                            continue;
                        }
                        if (entity.hasDelete()) continue;
                        data = entity.getControlData();
                        if (data != null) {
                            data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                            if (!data.controlCanRenderNow(entity)) continue;
                        }
                        instance = (InstanceRenderAPI) entity;
                        boolean notInstanceDefault = !instance.isInstanceData2D() && instance.haveValidInstanceData() || !instance.haveValidInstanceData() || instance.isCalledFixedSubmit();

                        if (!entity.isUseCustomDrawShader()) {
                            if (notInstanceDefault) {
                                if (!instance.isInstanceData2D()) instanceBit = 3; else if (!instance.haveValidInstanceData()) instanceBit = 0;
                                if (instance.isCalledFixedSubmit()) ++instanceBit;
                                program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, instanceBit);
                            }
                            if (instance.haveValidInstanceData()) {
                                instance.putShaderInstanceData();
                                GL20.glUniform1f(program.location[3], instance.getInstanceTimerOverride());
                            }
                            GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                            GL20.glUniform4(program.location[1], entity.pickDataPackage_vec4());
                        } else program.close();
                        this.matrixCheckA(entity);
                        entity.glDraw();
                        this.matrixCheckB(entity);
                        if (!entity.isUseCustomDrawShader()) {
                            if (notInstanceDefault) {
                                instanceBit = 1;
                                program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 1);
                            }
                        } else program.active();

                        this.removeCheck(entitiesI, data, entity);
                    }
                    program.close();
                } else glDisabledIterator(distortionList);
            }
        }
        if (commonList.isEmpty() && spriteList.isEmpty() && curveList.isEmpty() && segmentList.isEmpty() && trailList.isEmpty() && flareList.isEmpty() && customList.isEmpty() && textList.isEmpty()) {
            if (shaderEnable)ShaderCore.glEndDraw();
            return;
        }

        if (shaderEnable) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            if (layer != RenderingUtil.getHighestCampaignLayer()) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShaderCore.getRenderingBuffer().getColorResult(0));
                GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, ShaderCore.getScreenScaleWidth(), ShaderCore.getScreenScaleHeight());
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, ShaderCore.getRenderingBuffer().getFBO(0));
            }
        }
        if (!commonList.isEmpty()) {
            if (notMultiPass) {
                CommonEntity commonEntity;
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                program = ShaderCore.getCommonProgram();
                program.active();
                boolean globalColorMode = BoxConfigs.isShaderColorMode();
                int defaultColorMode = globalColorMode ? program.subroutineLocation[0][1] : program.subroutineLocation[0][0];
                vertexSub = new int[2];
                vertexSub[0] = defaultColorMode;
                vertexSub[1] = program.subroutineLocation[0][2];
                program.putUniformSubroutines(GL20.GL_VERTEX_SHADER, 0, vertexSub);
                program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, globalColorMode ? 1 : 0);
                for (Iterator<RenderDataAPI> entitiesI = commonList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (this.idle) this.idle = false;
                    commonEntity = (CommonEntity) entity;
                    material = ((MaterialRenderAPI) entity).getMaterialData();
                    instance = (InstanceRenderAPI) entity;
                    boolean entityColorMode = ((MaterialRenderAPI) entity).getDrawMode() == BoxEnum.MODE_COLOR || globalColorMode;

                    if (!entity.isUseCustomDrawShader()) {
                        if (entityColorMode) {
                            vertexSub[0] = program.subroutineLocation[0][1];
                            program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 1);
                        }
                        if (instance.haveValidInstanceData()) {
                            instanceBit = instance.isInstanceData2D() ? 3 : 5;
                            if (instance.isCalledFixedSubmit()) ++instanceBit;
                            vertexSub[1] = program.subroutineLocation[0][instanceBit];
                            instance.putShaderInstanceData();
                            GL20.glUniform1f(program.location[4], instance.getInstanceTimerOverride());
                        }
                        if (entityColorMode || instance.haveValidInstanceData())
                            program.putUniformSubroutines(GL20.GL_VERTEX_SHADER, 0, vertexSub);
                        GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                        GL20.glUniform4(program.location[1], entity.pickDataPackage_vec4());
                        GL20.glUniform3f(program.location[2], commonEntity.getBaseSizeX(), commonEntity.getBaseSizeY(), commonEntity.getBaseSizeZ());
                        GL20.glUniform1i(program.location[3], material.isAdditionEmissive() ? 1 : 0);
                        material.putShaderTexture();
                        commonEntity.getModel().putTBNShaderData();
                    } else program.close();
                    this.glMaterialEntityDraw(entity, material);
                    if (!entity.isUseCustomDrawShader()) {
                        if (entityColorMode) {
                            vertexSub[0] = defaultColorMode;
                            if (!globalColorMode) program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 0);
                        }
                        if (instance.haveValidInstanceData()) vertexSub[1] = program.subroutineLocation[0][2];
                        if (entityColorMode || instance.haveValidInstanceData())
                            program.putUniformSubroutines(GL20.GL_VERTEX_SHADER, 0, vertexSub);
                    } else program.active();

                    this.removeCheck(entitiesI, data, entity);
                }
                program.close();
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);
                if (!this.idle && !this.needAA) this.needAA = true;
            } else glDisabledIterator(commonList);
        }
        if (!spriteList.isEmpty()) {
            if (notMultiPass) {
                SpriteEntity spriteEntity;
                program = ShaderCore.getSpriteProgram();
                program.active();
                vertexSub = new int[2];
                vertexSub[0] = program.subroutineLocation[0][0];
                vertexSub[1] = program.subroutineLocation[0][4];
                program.putUniformSubroutines(GL20.GL_VERTEX_SHADER, 0, vertexSub);
                instanceBit = 4;
                for (Iterator<RenderDataAPI> entitiesI = spriteList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (this.idle) this.idle = false;
                    material = ((MaterialRenderAPI) entity).getMaterialData();
                    instance = (InstanceRenderAPI) entity;
                    spriteEntity = (SpriteEntity) entity;
                    boolean haveTiles = spriteEntity.isTilesRendering();
                    boolean notInstanceDefault = !instance.isInstanceData2D() && instance.haveValidInstanceData() || !instance.haveValidInstanceData() || instance.isCalledFixedSubmit();

                    if (!entity.isUseCustomDrawShader()) {
                        if (haveTiles)
                            vertexSub[0] = spriteEntity.isRandomTile() ? program.subroutineLocation[0][2] : program.subroutineLocation[0][1];
                        if (notInstanceDefault) {
                            if (!instance.isInstanceData2D()) instanceBit = 6; else if (!instance.haveValidInstanceData()) instanceBit = 3;
                            if (instance.isCalledFixedSubmit()) ++instanceBit;
                            vertexSub[1] = program.subroutineLocation[0][instanceBit];
                        }
                        if (instance.haveValidInstanceData()) {
                            instance.putShaderInstanceData();
                            GL20.glUniform1f(program.location[3], instance.getInstanceTimerOverride());
                        }
                        if (haveTiles || notInstanceDefault)
                            program.putUniformSubroutines(GL20.GL_VERTEX_SHADER, 0, vertexSub);
                        GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                        GL20.glUniform4(program.location[1], entity.pickDataPackage_vec4());
                        GL20.glUniform1i(program.location[2], material.isAdditionEmissive() ? 1 : 0);
                        material.putShaderTexture();
                    } else program.close();
                    this.glMaterialEntityFlatDraw(entity, material);
                    if (!entity.isUseCustomDrawShader()) {
                        if (haveTiles) vertexSub[0] = program.subroutineLocation[0][0];
                        if (notInstanceDefault) vertexSub[1] = program.subroutineLocation[0][4];
                        if (haveTiles || notInstanceDefault) {
                            instanceBit = 4;
                            vertexSub[1] = program.subroutineLocation[0][4];
                            program.putUniformSubroutines(GL20.GL_VERTEX_SHADER, 0, vertexSub);
                        }
                    } else program.active();

                    this.removeCheck(entitiesI, data, entity);
                }
                program.close();
            } else glDisabledIterator(spriteList);
        }
        if (!curveList.isEmpty()) {
            if (notMultiPass) {
                CurveEntity curveEntity;
                float uvFlow;
                program = ShaderCore.getCurveProgram();
                program.active();
                program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 1);
                GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES, 2);
                for (Iterator<RenderDataAPI> entitiesI = curveList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    curveEntity = (CurveEntity) entity;
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (!curveEntity.isHaveValidNodeCount()) continue;
                    if (this.idle) this.idle = false;
                    material = ((MaterialRenderAPI) entity).getMaterialData();
                    instance = (InstanceRenderAPI) entity;
                    boolean notInstanceDefault = instance.isInstanceData3D() && instance.haveValidInstanceData() || !instance.haveValidInstanceData() || instance.isCalledFixedSubmit();

                    if (!entity.isUseCustomDrawShader()) {
                        if (instance.haveValidInstanceData()) {
                            instanceBit = instance.isInstanceData2D() ? 1 : 3;
                            if (instance.isCalledFixedSubmit()) ++instanceBit;
                            program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, instanceBit);
                        }
                        if (instance.haveValidInstanceData()) {
                            instance.putShaderInstanceData();
                            GL20.glUniform1f(program.location[4], instance.getInstanceTimerOverride());
                        }
                        GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                        FloatBuffer buffer = entity.pickDataPackage_vec4();
                        uvFlow = (curveEntity.getTextureSpeed() == 0.0f || curveEntity.getTexturePixels() == 0.0f) ? 0.0f : curveEntity.getTextureSpeed() / curveEntity.getTexturePixels() * (curveEntity.isFlowWhenPaused() ? this.frameTime : this.frameTimePausedCheck);
                        buffer.put(15, CalculateUtil.fraction(uvFlow + curveEntity.getUVOffset()));
                        GL20.glUniform4(program.location[1], buffer);
                        GL20.glUniform1f(program.location[2], curveEntity.getValidNodeCount() - 1);
                        GL20.glUniform1i(program.location[3], material.isAdditionEmissive() ? 1 : 0);
                        material.putShaderTexture();
                    } else program.close();
                    this.glMaterialEntityFlatDraw(entity, material);
                    if (!entity.isUseCustomDrawShader()) {
                        if (notInstanceDefault) {
                            program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 1);
                        }
                    } else program.active();

                    this.removeCheck(entitiesI, data, entity);
                }
                program.close();
            } else glDisabledIterator(curveList);
        }
        if (!segmentList.isEmpty()) {
            if (notMultiPass) {
                SegmentEntity segmentEntity;
                float uvFlow;
                program = ShaderCore.getSegmentProgram();
                program.active();
                GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES, 2);
                for (Iterator<RenderDataAPI> entitiesI = segmentList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    segmentEntity = (SegmentEntity) entity;
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (!segmentEntity.isHaveValidNodeCount()) continue;
                    if (this.idle) this.idle = false;
                    material = ((MaterialRenderAPI) entity).getMaterialData();

                    if (!entity.isUseCustomDrawShader()) {
                        GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                        FloatBuffer buffer = entity.pickDataPackage_vec4();
                        uvFlow = (segmentEntity.getTextureSpeed() == 0.0f || segmentEntity.getTexturePixels() == 0.0f) ? 0.0f : segmentEntity.getTextureSpeed() / segmentEntity.getTexturePixels() * (segmentEntity.isFlowWhenPaused() ? this.frameTime : this.frameTimePausedCheck);
                        buffer.put(15, CalculateUtil.fraction(uvFlow + segmentEntity.getUVOffset()));
                        GL20.glUniform4(program.location[1], buffer);
                        GL20.glUniform1i(program.location[2], material.isAdditionEmissive() ? 1 : 0);
                        segmentEntity.putShaderSegmentData();
                        material.putShaderTexture();
                    } else program.close();
                    this.glMaterialEntityFlatDraw(entity, material);
                    if (entity.isUseCustomDrawShader()) program.active();

                    this.removeCheck(entitiesI, data, entity);
                }
                program.close();
            } else glDisabledIterator(segmentList);
        }
        if (!trailList.isEmpty()) {
            if (notMultiPass) {
                TrailEntity trailEntity;
                float uvFlow;
                program = ShaderCore.getTrailProgram();
                program.active();
                for (Iterator<RenderDataAPI> entitiesI = trailList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    trailEntity = (TrailEntity) entity;
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (!trailEntity.isHaveValidNodeCount()) continue;
                    if (this.idle) this.idle = false;
                    material = ((MaterialRenderAPI) entity).getMaterialData();

                    if (!entity.isUseCustomDrawShader()) {
                        GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                        FloatBuffer buffer = entity.pickDataPackage_vec4();
                        uvFlow = (trailEntity.getTextureSpeed() == 0.0f || trailEntity.getTexturePixels() == 0.0f) ? 0.0f : trailEntity.getTextureSpeed() / trailEntity.getTexturePixels() * (trailEntity.isFlowWhenPaused() ? this.frameTime : this.frameTimePausedCheck);
                        buffer.put(13, CalculateUtil.fraction(uvFlow + trailEntity.getUVOffset()));
                        GL20.glUniform4(program.location[1], buffer);
                        GL20.glUniform2f(program.location[2], trailEntity.getCurrentFlickerSyncValue(), trailEntity.getCurrentFlickerSyncValue() + (trailEntity.isFlickWhenPaused() ? this.frameTime : this.frameTimePausedCheck));
                        GL20.glUniform1i(program.location[3], material.isAdditionEmissive() ? 1 : 0);
                        trailEntity.putShaderTrailData();
                        material.putShaderTexture();
                    } else program.close();
                    this.glMaterialEntityFlatDraw(entity, material);
                    if (entity.isUseCustomDrawShader()) program.active();

                    this.removeCheck(entitiesI, data, entity);
                }
                program.close();
            } else glDisabledIterator(segmentList);
        }
        if (!flareList.isEmpty()) {
            if (notMultiPass) {
                FlareEntity flareEntity;
                program = ShaderCore.getFlareProgram();
                program.active();
                program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 1);
                program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 0);
                instanceBit = 1;
                for (Iterator<RenderDataAPI> entitiesI = flareList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (this.idle) this.idle = false;
                    instance = (InstanceRenderAPI) entity;
                    flareEntity = (FlareEntity) entity;
                    boolean notInstanceDefault = !instance.isInstanceData2D() && instance.haveValidInstanceData() || !instance.haveValidInstanceData() || instance.isCalledFixedSubmit();

                    if (!entity.isUseCustomDrawShader()) {
                        if (flareEntity.isSharp()) program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 1);
                        else if (flareEntity.isSmoothDisc())
                            program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 2);
                        else if (flareEntity.isSharpDisc())
                            program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 3);
                        if (notInstanceDefault) {
                            if (!instance.isInstanceData2D()) instanceBit = 3; else if (!instance.haveValidInstanceData()) instanceBit = 0;
                            if (instance.isCalledFixedSubmit()) ++instanceBit;
                            program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, instanceBit);
                        }
                        if (instance.haveValidInstanceData()) {
                            instance.putShaderInstanceData();
                            GL20.glUniform1f(program.location[2], instance.getInstanceTimerOverride());
                        }
                        GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                        FloatBuffer buffer = entity.pickDataPackage_vec4();
                        float flickerTime = flareEntity.isFlickWhenPaused() ? this.frameTime : this.frameTimePausedCheck;
                        flickerTime *= flareEntity.getFlickerAnimationRateMulti();
                        buffer.put(15, flickerTime);
                        GL20.glUniform4(program.location[1], buffer);
                    } else program.close();
                    this.glEntityDraw(entity);
                    if (!entity.isUseCustomDrawShader()) {
                        if (!flareEntity.isSmooth()) program.putUniformSubroutine(GL20.GL_FRAGMENT_SHADER, 1, 0);
                        if (notInstanceDefault) {
                            instanceBit = 1;
                            program.putUniformSubroutine(GL20.GL_VERTEX_SHADER, 0, 1);
                        }
                    } else program.active();

                    this.removeCheck(entitiesI, data, entity);
                }
                program.close();
            } else glDisabledIterator(flareList);
        }
        if (!customList.isEmpty()) {
            if (notMultiPass) {
                for (Iterator<RenderDataAPI> entitiesI = customList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (this.idle) this.idle = false;
                    boolean haveMaterial = entity instanceof MaterialRenderAPI;
                    material = haveMaterial ? ((MaterialRenderAPI) entity).getMaterialData() : null;

                    if (haveMaterial) this.cullCheckA(material);
                    this.glEntityDraw(entity);
                    if (haveMaterial) this.cullCheckB(material);

                    this.removeCheck(entitiesI, data, entity);
                }
            } else glDisabledIterator(customList);
        }
        if (!textList.isEmpty()) {
            if (notMultiPass) {
                TextFieldEntity textFieldEntity;
                program = ShaderCore.getTextProgram();
                program.active();
                for (Iterator<RenderDataAPI> entitiesI = textList.iterator(); entitiesI.hasNext();) {
                    entity = entitiesI.next();
                    if (entity == null) {
                        entitiesI.remove();
                        continue;
                    }
                    if (entity.hasDelete()) continue;
                    data = entity.getControlData();
                    textFieldEntity = (TextFieldEntity) entity;
                    if (data != null) {
                        data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                        if (!data.controlCanRenderNow(entity)) continue;
                    }
                    if (!textFieldEntity.isValidRenderingTextField()) continue;
                    if (this.idle) this.idle = false;

                    if (!entity.isUseCustomDrawShader()) {
                        GL20.glUniformMatrix4(program.location[0], false, entity.pickModelMatrixPackage_mat4());
                        GL20.glUniform1f(program.location[5], textFieldEntity.getCurrentItalicFactor());
                        float[] globalColor = textFieldEntity.getGlobalColorArray();
                        GL20.glUniform4f(program.location[6], globalColor[0], globalColor[1], globalColor[2], globalColor[3]);
                        for (int i = 0; i < textFieldEntity.getFontMapArray().length; i++) {
                            if (textFieldEntity.getFontMapArray()[i] != null && textFieldEntity.getFontMapArray()[i].isValid()) program.bindTexture2D(i, textFieldEntity.getFontMapArray()[i].getMap().getTextureId());
                        }
                    } else program.close();
                    this.glEntityDraw(entity);
                    if (entity.isUseCustomDrawShader()) program.active();
                    this.removeCheck(entitiesI, data, entity);
                }
                program.close();
            } else glDisabledIterator(textList);
        }

        if (shaderEnable) {
            if (layer != RenderingUtil.getHighestCampaignLayer()) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                ShaderUtil.blitToScreen(ShaderCore.getRenderingBuffer().getFBO(0));
            }
            ShaderCore.glEndDraw();
        }
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return true;
    }

    private void glMaterialEntityDraw(RenderDataAPI entity, MaterialData material) {
        this.cullCheckA(material);
        this.glEntityDraw(entity);
        this.cullCheckB(material);
    }

    private void glMaterialEntityFlatDraw(RenderDataAPI entity, MaterialData material) {
        this.cullCheckA_B(material);
        this.glEntityDraw(entity);
        this.cullCheckB_B(material);
    }

    private void glEntityDraw(RenderDataAPI entity) {
        this.matrixCheckA(entity);
        this.blendCheckA(entity);
        entity.glDraw();
        this.matrixCheckB(entity);
        this.blendCheckB(entity);
    }

    private void matrixCheckA(RenderDataAPI entity) {
        switch (entity.getPrimeMatrixState()) {
            case 0:
                break;
            case 1: {
                ShaderCore.refreshGameViewportMatrix(this.perspectiveMatrix);
                break;
            }
            case 2: {
                ShaderCore.refreshGameViewportMatrix(entity.pickPrimeMatrixPackage_mat4());
                break;
            }
            default: {
                ShaderCore.refreshGameViewportMatrixNone();
            }
        }
    }

    private void matrixCheckB(RenderDataAPI entity) {
        if (entity.getPrimeMatrixState() != 0) ShaderCore.refreshGameViewportMatrix(this.vanillaMatrix);
    }

    private void blendCheckA(RenderDataAPI entity) {
        switch (entity.getBlendState()) {
            case 0:
                break;
            case 1: {
                GL11.glBlendFunc(entity.getBlendColorSRC(), entity.getBlendColorDST());
                break;
            }
            case 2: {
                GL14.glBlendFuncSeparate(entity.getBlendColorSRC(), entity.getBlendAlphaSRC(), entity.getBlendColorDST(), entity.getBlendAlphaDST());
                GL14.glBlendEquation(entity.getBlendEquation());
                break;
            }
            default: {
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
    }

    private void blendCheckB(RenderDataAPI entity) {
        switch (entity.getBlendState()) {
            case 0:
                break;
            case 1: {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                break;
            }
            case 2: {
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
                GL14.glBlendEquation(GL14.GL_FUNC_ADD);
                break;
            }
            default: {
                GL11.glEnable(GL11.GL_BLEND);
            }
        }
    }

    private void cullCheckA(MaterialData material) {
        switch (material.getCullFace()) {
            case 0: break;
            case 1: {
                GL11.glCullFace(GL11.GL_FRONT);
                break;
            }
            case 2: {
                GL11.glCullFace(GL11.GL_FRONT_AND_BACK);
                break;
            }
            default: {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }
    }

    private void cullCheckB(MaterialData material) {
        switch (material.getCullFace()) {
            case 0: break;
            case 1:
            case 2: {
                GL11.glCullFace(GL11.GL_BACK);
                break;
            }
            default: {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        }
    }

    private void cullCheckA_B(MaterialData material) {
        if (material.getCullFace() != 3)  {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        switch (material.getCullFace()) {
            case 0:
                GL11.glCullFace(GL11.GL_BACK);
                break;
            case 1: {
                GL11.glCullFace(GL11.GL_FRONT);
                break;
            }
            case 2: {
                GL11.glCullFace(GL11.GL_FRONT_AND_BACK);
                break;
            }
        }
    }

    private void cullCheckB_B(MaterialData material) {
        if (material.getCullFace() != 3)  {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
    }

    private void removeCheck(Iterator<RenderDataAPI> iterator, ControlDataAPI data, RenderDataAPI entity) {
        boolean toRemove = false;
        if (data != null) {
            data.controlAfterRenderingAdvance(entity, this.lastFrameAmount);
            if (data.controlIsOnceRender(entity)) {
                entity.delete();
                toRemove = true;
            }
        } else if (entity.isGlobalTimerOnce()) {
            toRemove = true;
        }
        if (toRemove) iterator.remove();
    }

    private void glDisabledIterator(List<RenderDataAPI> list) {
        RenderDataAPI entity;
        for (Iterator<RenderDataAPI> entitiesI = list.iterator(); entitiesI.hasNext();) {
            entity = entitiesI.next();
            if (entity == null) {
                entitiesI.remove();
                continue;
            }
            if (entity.hasDelete()) continue;
            ControlDataAPI data = entity.getControlData();
            if (data != null) {
                data.controlBeforeRenderingAdvance(entity, this.lastFrameAmount);
                if (!data.controlCanRenderNow(entity)) continue;
            }
            this.removeCheck(entitiesI, data, entity);
        }
    }

    public static void clearRenderEntity() {
        for (List<RenderDataAPI>[] lists : _ENTITIES_MAP.values()) {
            for (int i = 0; i < BoxDatabase.ENTITY_TOTAL_TYPE_NORMAL; i++) {
                for (RenderDataAPI entity : lists[i]) {
                    if (entity == null || entity.hasDelete()) continue;
                    entity.delete();
                }
                lists[i].clear();
            }
        }
        for (int i = 0; i < BoxDatabase.ENTITY_TOTAL_TYPE_DIRECT; i++) {
            for (RenderDataAPI entity : _DIRECT_MAP[i]) {
                if (entity == null || entity.hasDelete()) continue;
                entity.delete();
            }
            _DIRECT_MAP[i].clear();
        }
    }

    public static Matrix4f getGameOrthoViewport() {
        return _ORTHO_VIEWPORT;
    }

    public static Matrix4f getGamePerspectiveViewport() {
        return _PERSPECTIVE_VIEWPORT;
    }

    /**
     * When player fleet goto new map, manager will delete all entity.
     *
     * @param target value only can be "ENTITY_xyz", see {@link BoxEnum}.
     * @return return {@link BoxEnum#STATE_SUCCESS} when entity added, return {@link BoxEnum#STATE_FAILED} when entity adding failed.
     */
    public static byte addEntity(byte target, @NotNull RenderDataAPI entity) {
        if (target >= BoxDatabase.ENTITY_TOTAL_TYPE_NORMAL) return BoxEnum.STATE_FAILED;
        Object layer = entity.getLayer();
        if (layer instanceof CampaignEngineLayers) {
            _ENTITIES_MAP.get(layer)[target].add(entity);
            return BoxEnum.STATE_SUCCESS;
        }
        if (entity instanceof DirectDrawEntity && (target == BoxEnum.ENTITY_DISTORTION || target == BoxEnum.ENTITY_ILLUMINANT)) {
            _DIRECT_MAP[target].add(entity);
            return BoxEnum.STATE_SUCCESS;
        }
        return BoxEnum.STATE_FAILED;
    }

    /**
     * @param target value only can be "ENTITY_xyz", see {@link BoxEnum}.
     */
    public static boolean containsEntity(CampaignEngineLayers layer, byte target, RenderDataAPI entity) {
        if (target >= BoxDatabase.ENTITY_TOTAL_TYPE_NORMAL) return false;
        return _ENTITIES_MAP.get(layer)[target].contains(entity);
    }

    public static float getTimeFraction() {
        return _TIMER_FRACTION[0];
    }

    public static float getTimeFractionIncludePaused() {
        return _TIMER_FRACTION[1];
    }
}
