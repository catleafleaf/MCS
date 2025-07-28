package org.boxutil.units.builtin.buffer;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.boxutil.manager.ShaderCore;
import org.boxutil.util.CommonUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class BUtil_RenderingBuffer {
    private final static byte _SCALE_LAYERS = 4;
    private final static byte _BUFFER_COUNT = 2;
    private final static byte _BUFFER_TEX_COUNT = 4;
    private final int[] FBO = new int[_BUFFER_COUNT];
    private int RBO = 0;
    private final int[][] texID = new int[_BUFFER_COUNT][_BUFFER_TEX_COUNT];
    private final boolean[] finished = new boolean[_BUFFER_COUNT];
    private final int[][] scaleSize = new int[_SCALE_LAYERS][2];
    private final float[] scaleFactor = new float[_SCALE_LAYERS];

    public BUtil_RenderingBuffer() {
        final int width = ShaderCore.getScreenScaleWidth();
        final int height = ShaderCore.getScreenScaleHeight();
        IntBuffer ids;
        int state;

        for (byte i = 0; i < _SCALE_LAYERS; i++) {
            this.scaleFactor[i] = 1 << i;
            this.scaleSize[i][0] = (int) Math.ceil((float) width / this.scaleFactor[i]);
            this.scaleSize[i][1] = (int) Math.ceil((float) height / this.scaleFactor[i]);
        }

        if (!GLContext.getCapabilities().OpenGL30 || !GLContext.getCapabilities().GL_ARB_framebuffer_object) {
            Global.getLogger(ShaderCore.class).log(Level.ERROR, "'BoxUtil' rendering framebuffers create failed: OpenGL Context unsupported.");
            return;
        }

        for (byte f = 0; f < _BUFFER_COUNT; ++f) {
            this.FBO[f] = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.FBO[f]);

            ids = BufferUtils.createIntBuffer(_BUFFER_TEX_COUNT);
            GL11.glGenTextures(ids);
            for (byte i = 0; i < (f == 0 ? _BUFFER_TEX_COUNT : 1); ++i) {
                this.texID[f][i] = ids.get(i);
                int att = GL30.GL_COLOR_ATTACHMENT0 + i;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texID[f][i]);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, att, GL11.GL_TEXTURE_2D, this.texID[f][i], 0);
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            if (f == 0) {
                this.RBO = GL30.glGenRenderbuffers();
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.RBO);
                GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT16, width, height);
                GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, this.RBO);
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
            }

            if (f == 0) {
                GL20.glDrawBuffers(CommonUtil.createIntBuffer(GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_COLOR_ATTACHMENT2, GL30.GL_COLOR_ATTACHMENT3));
            } else {
                GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
            }

            state = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (state == GL30.GL_FRAMEBUFFER_COMPLETE) {
                Global.getLogger(ShaderCore.class).info("'BoxUtil' rendering framebuffer-" + f + " has created.");
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                this.finished[f] = true;
            } else {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                this.delete(f);
                Global.getLogger(ShaderCore.class).log(Level.ERROR, "'BoxUtil' rendering framebuffer-" + f + " create failed: " + state);
            }
        }
    }

    public static byte getBufferCount() {
        return _BUFFER_COUNT;
    }

    public static byte getLayerCount() {
        return _SCALE_LAYERS;
    }

    public void delete(int index) {
        if (!GLContext.getCapabilities().OpenGL30 || !GLContext.getCapabilities().GL_ARB_framebuffer_object) {
            return;
        }
        GL11.glDeleteTextures(CommonUtil.createIntBuffer(this.texID[index]));
        if (index == 0 && this.RBO > 0) GL30.glDeleteRenderbuffers(this.RBO);
        GL30.glDeleteFramebuffers(this.FBO[index]);
        this.finished[index] = false;
    }

    public boolean[] isFinished() {
        return this.finished;
    }

    public boolean isFinished(int index) {
        return this.finished[index];
    }

    public int[] getFBOs() {
        return this.FBO;
    }

    public int getFBO(int index) {
        return this.FBO[index];
    }

    public int getRBO() {
        return this.RBO;
    }

    public int[] getScaleSize(int level) {
        return this.scaleSize[level];
    }

    public float getScaleFactor(int level) {
        return this.scaleFactor[level];
    }

    public int[][] getResultTex() {
        return this.texID;
    }

    public int[] getResultTex(int index) {
        return this.texID[index];
    }

    public int getColorResult(int index) {
        return this.texID[index][0];
    }

    public int getDataResult(int index) {
        return this.texID[index][1];
    }

    public int getEmissiveResult(int index) {
        return this.texID[index][2];
    }

    public int getNormalResult(int index) {
        return this.texID[index][3];
    }

    public void setScaleViewport(int level) {
        GL11.glViewport(0, 0, this.scaleSize[level][0], this.scaleSize[level][1]);
    }
}
