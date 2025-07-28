package org.boxutil.base;

import com.fs.starfarer.api.Global;
import org.boxutil.base.api.InstanceDataAPI;
import org.boxutil.base.api.InstanceRenderAPI;
import org.boxutil.base.api.RenderDataAPI;
import org.boxutil.units.standard.attribute.Instance2Data;
import org.boxutil.units.standard.attribute.Instance3Data;
import org.boxutil.util.TrigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <strong>Very^32</strong> simple particle dynamic resource manager.<p>
 * Only for one entity.<p>
 * For reference.
 */
public class SimpleParticleControlData extends BaseControlData {
    protected InstanceRenderAPI entity = null;
    protected final int[] state = new int[5]; // currIndex, lastIndex, renderingCount, maxCount, lastIndexSub
    protected final float[] stateF = new float[3]; // timer, dataTimer, maxDur
    protected final boolean[] stateB = new boolean[6]; // indexReset, fullRefresh, invalid, ignorePaused, use3D, forceDelete

    protected boolean toAddBefore() {
        if (this.stateB[2] || this.entity == null || this.stateB[5]) return true;
        this.stateF[0] = this.stateF[2];
        this.entity.setAlwaysRefreshInstanceData(true);
        if (this.state[0] >= this.state[3]) this.state[0] = 0;
        return false;
    }

    protected void toAddAfter(InstanceDataAPI instanceData) {
        if (this.state[0] >= this.entity.getInstanceData().size()) this.entity.getInstanceData().add(instanceData);
        else this.entity.getInstanceData().set(this.state[0], instanceData);
        if (this.state[2] < this.state[3]) this.state[2]++;

        this.state[0]++;
        if (this.stateB[0] && this.state[0] >= this.state[1]) this.stateB[1] = true;
        if (this.state[0] <= this.state[1]) {
            this.state[4] = 0;
            this.stateB[0] = true;
        }
    }

    /**
     * @return null if it cannot add.
     */
    public @Nullable Instance2Data addParticle() {
        if (this.stateB[4] || this.toAddBefore()) return null;
        Instance2Data result;
        result = new Instance2Data();
        this.toAddAfter(result);
        return result;
    }

    /**
     * @return null if it cannot add.
     */
    public @Nullable Instance3Data addParticle3D() {
        if (!this.stateB[4] || this.toAddBefore()) return null;
        Instance3Data result;
        result = new Instance3Data();
        this.toAddAfter(result);
        return result;
    }

    public Instance2Data addParticle(Vector2f location, float facing, float turnRate, Vector2f velocity, Vector2f scale, Vector2f scaleRate, Color basecolor, Color emissive, float in, float full, float out) {
        if (this.toAddBefore()) return null;
        Instance2Data data = new Instance2Data();
        data.setLocation(location);
        data.setFacing(facing);
        data.setTurnRate(turnRate);
        data.setVelocity(velocity);
        data.setScale(scale);
        data.setScaleRate(scaleRate);
        data.setColor(basecolor);
        data.setEmissiveColor(emissive);
        data.setTimer(in, full, out);
        this.toAddAfter(data);
        return data;
    }

    public Instance2Data addParticle(float spawnStartRadius, float velAngRad, float velocity, float scale, Color basecolor, Color emissive, float in, float full, float out) {
        if (this.toAddBefore()) return null;
        float c = (float) Math.cos(velAngRad), s = TrigUtil.sinFormCosRadiansF(c, velAngRad);
        Instance2Data data = new Instance2Data();
        data.setLocation(c * spawnStartRadius, s * spawnStartRadius);
        data.setVelocity(c * velocity, s * velocity);
        data.setScale(scale, scale);
        data.setColor(basecolor);
        data.setEmissiveColor(emissive);
        data.setTimer(in, full, out);
        this.toAddAfter(data);
        return data;
    }

    public Instance2Data addParticle(float spawnStartRadius, float velocity, float scale, Color basecolor, Color emissive, float in, float full, float out) {
        return this.addParticle(spawnStartRadius, (float) Math.random() * TrigUtil.PI2_F, velocity, scale, basecolor, emissive, in, full, out);
    }

    public Instance2Data addParticle(float spawnStartRadius, float velocity, float scale, float in, float full, float out) {
        return this.addParticle(spawnStartRadius, velocity, scale, Color.WHITE, Color.WHITE, in, full, out);
    }

    public Instance2Data addParticle(float spawnStartRadius, float velocity, float scale, float out) {
        return this.addParticle(spawnStartRadius, velocity, scale, 0.0f, 0.0f, out);
    }

    public void clearParticles() {
        if (this.entity == null) return;
        this.stateF[0] = 0.0f;
        this.entity.setAlwaysRefreshInstanceData(false);
        this.entity.setRenderingCount(0);
        this.entity.getInstanceData().clear();
        this.state[0] = 0;
        this.state[1] = 0;
        this.state[2] = 0;
    }

    public void forceDelete() {
        this.stateB[5] = true;
        this.clearParticles();
    }

    /**
     * @param dataDur rendering once if at <strong>(-3000.0f, -1000.0f)</strong>, always active if less than <strong>-3000.0f</strong>.
     */
    public SimpleParticleControlData(int maxParticles, float maxDur, float dataDur, boolean ignorePaused, boolean use3D) {
        this.state[3] = maxParticles;
        this.stateF[1] = dataDur;
        this.stateF[2] = maxDur;
        this.stateB[3] = ignorePaused;
        this.stateB[4] = use3D;
    }

    /**
     * @param dataDur rendering once if at <strong>(-3000.0f, -1000.0f)</strong>, always active if less than <strong>-3000.0f</strong>.
     */
    public SimpleParticleControlData(int maxParticles, float maxDur, float dataDur, boolean ignorePaused) {
        this(maxParticles, maxDur, dataDur, ignorePaused, false);
    }

    public void controlInit(@NotNull RenderDataAPI renderEntity) {
        this.stateB[2] = !(renderEntity instanceof InstanceRenderAPI);
        if (this.stateB[2]) return;
        this.entity = (InstanceRenderAPI) renderEntity;
        this.entity.setInstanceData(new ArrayList<InstanceDataAPI>(this.state[3]), 0.0f, this.stateF[2], 0.0f);
        if (this.stateB[4]) this.entity.setUseInstanceData3D(); else this.entity.setUseInstanceData2D();
        this.entity.mallocInstanceData(this.state[3]);
    }

    public void controlAdvance(@NotNull RenderDataAPI renderEntity, float amount) {
        if (this.stateB[2] || !this.controlCanRenderNow(renderEntity) || this.stateB[5]) return;
        if (this.state[0] != this.state[4] || this.stateB[1]) {
            if (this.stateB[1]) {
                this.entity.setInstanceDataRefreshIndex(0);
                this.entity.setInstanceDataRefreshAllFromCurrentIndex();
                this.entity.submitInstanceData();
            } else {
                if (this.stateB[0]) {
                    this.entity.setInstanceDataRefreshIndex(this.state[1]);
                    this.entity.setInstanceDataRefreshAllFromCurrentIndex();
                    this.entity.submitInstanceData();
                }
                this.entity.setInstanceDataRefreshIndex(this.state[4]);
                this.entity.setInstanceDataRefreshSize(this.state[0] - this.state[4]);
                this.entity.submitInstanceData();
            }
            if (this.state[0] >= this.state[3]) this.state[0] = 0;
            this.stateB[1] = false;
            this.stateB[0] = false;
            this.state[4] = this.state[0];
            this.state[1] = this.state[0];
            this.entity.setRenderingCount(this.state[2]);
        }
        if (this.stateF[0] > 0.0f) this.stateF[0] -= amount;
        else if (this.state[2] > 0) this.clearParticles();
        if (this.stateF[1] > 0.0f) this.stateF[1] -= amount;
    }

    public boolean controlAlphaBasedTimer(@NotNull RenderDataAPI renderEntity) {
        return false;
    }

    public boolean controlRemoveBasedTimer(@NotNull RenderDataAPI renderEntity) {
        return false;
    }

    public boolean controlRunWhilePaused(@NotNull RenderDataAPI renderEntity) {
        return this.stateB[3];
    }

    public boolean controlIsOnceRender(@NotNull RenderDataAPI renderEntity) {
        return this.stateF[1] > -3000.0f && this.stateF[1] < -1000.0f;
    }

    public boolean controlIsDone(@NotNull RenderDataAPI renderEntity) {
        return this.stateB[2] || (this.stateF[1] > -1000.0f && this.stateF[1] < 0.0f) || this.stateB[5];
    }

    public boolean controlCanRenderNow(@NotNull RenderDataAPI renderEntity) {
        return this.state[2] > 0;
    }
}
