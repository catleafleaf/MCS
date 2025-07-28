package org.boxutil.units.standard.entity;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.boxutil.units.builtin.legacy.array.Stack2f;
import org.jetbrains.annotations.NotNull;
import org.boxutil.base.BaseRenderData;
import org.boxutil.base.BaseShaderData;
import org.boxutil.base.api.ControlDataAPI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CampaignRenderingManager;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.builtin.legacy.array.Stack3f;
import org.boxutil.units.standard.attribute.FontMapData;
import org.boxutil.util.CommonUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Put in to rendering manage is not required, can direct creating and use it.<p>
 * Loading font info or submit string will both cost some time, usual should to preload it.<p>
 * Anchor at top-left of text field.<p>
 * <strong>Remember to register your font texture at settings.json !</strong><p>
 * Use {@link TextFieldEntity#RESERVED_SYMBOL} for create empty character (will not display anything), if you will display a dynamic value or text such as flux value.
 */
public class TextFieldEntity extends BaseRenderData {
    public final static char RESERVED_SYMBOL = '\0';
    public final static char LINE_FEED_SYMBOL = '\n';
    public final static char NOT_FOUND_SYMBOL = '?';
    protected final static byte _DEFAULT_PAD = 10;
    protected final static byte _VBO_COUNT = 14;
    protected final int _textFieldID;
    // for each char: vec4(uvBL, uvTR), vec4(x, y, topStyleUV, bottomStyleUV), float(handelIndex + (invert + channel) + italic + underline + strikeout)), vec4(color), vec4(size, edge)
    protected int _textFieldVBO = 0;
    protected FontMapData[] fontMapList = null;
    protected final int[] textDataRefreshState = new int[4]; // lastSize, index, size, drawMode
    protected final float[] textStateAfterSubmit = new float[]{0.0f, 0.0f, 0.2f}; // width, height, italicValue
    protected List<TextData> _lastTextDataList = null;
    protected List<Stack3f> _lastTextDataState = null;
    protected final float[] state = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f}; // widthSpace, HeightSpace, fieldWidth, fieldHeight, vec4(globalColor)
    protected Alignment alignment = Alignment.LEFT;

    /**
     * Default value {@link Alignment#LEFT}.
     */
    public enum Alignment {
        LEFT,
        MID,
        RIGHT
    }

    public TextFieldEntity() {
        this._textFieldID = GL30.glGenVertexArrays();
        this.textDataRefreshState[3] = GL15.GL_STATIC_DRAW;
    }

    public TextFieldEntity(boolean dynamicRefresh) {
        this._textFieldID = GL30.glGenVertexArrays();
        this.textDataRefreshState[3] = dynamicRefresh ? GL15.GL_STREAM_DRAW : GL15.GL_STATIC_DRAW;
    }

    public TextFieldEntity(@NotNull FontMapData map) {
        this();
        this.setFontMap(map);
    }

    public TextFieldEntity(@NotNull FontMapData map, boolean dynamicRefresh) {
        this(dynamicRefresh);
        this.setFontMap(map);
    }

    public TextFieldEntity(@NotNull String fontPath) {
        this(new FontMapData(fontPath));
    }

    public TextFieldEntity(@NotNull String fontPath, boolean dynamicRefresh) {
        this(new FontMapData(fontPath), dynamicRefresh);
    }

    public int getFontFieldID() {
        return this._textFieldID;
    }

    public FontMapData[] getFontMapArray() {
        return this.fontMapList;
    }

    /**
     * Apply for the first map.
     * @param map all the font height (or font size) should be near or equal.
     */
    public void setFontMap(@NotNull FontMapData map) {
        if (this.fontMapList == null) this.fontMapList = new FontMapData[4];
        this.fontMapList[0] = map;
    }

    /**
     * Can use at most four maps.
     * @param map all the font height (or font size) should be near or equal.
     */
    public void setFontMap(@NotNull FontMapData map, byte index) {
        if (index > 3) return;
        if (this.fontMapList == null) this.fontMapList = new FontMapData[4];
        this.fontMapList[index] = map;
    }

    /**
     * Can use at most four maps.
     * @param fontPath all the font height (or font size) should be near or equal.
     */
    public void setFontMap(@NotNull String fontPath, byte index) {
        this.setFontMap(new FontMapData(fontPath), index);
    }

    public void delete() {
        super.delete();
        this.textDataRefreshState[0] = 0;
        this._lastTextDataList = null;
        this.fontMapList = null;
        if (BoxConfigs.isVAOSupported()) {
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            if (this._textFieldVBO != 0) GL15.glDeleteBuffers(this._textFieldVBO);
            if (this._textFieldID != 0) GL30.glDeleteVertexArrays(this._textFieldID);
        }
    }

    public void glDraw() {
        if (this.textDataRefreshState[0] < 1) return;
        GL30.glBindVertexArray(this._textFieldID);
        GL31.glDrawArraysInstanced(GL11.GL_POINTS, 0, 1, this.textDataRefreshState[0]);
    }

    /**
     * You can directly call it when need display the text.
     *
     * @param inCampaignOrCombat ignored when use custom prime matrix or none prime.
     */
    public void directDraw(boolean inCampaignOrCombat) {
        BaseShaderData program = ShaderCore.getTextProgram();
        if (this.textDataRefreshState[0] < 1 || program == null || !program.isValid()) return;
        int fboNow = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (this.getBlendState() == 3) GL11.glDisable(GL11.GL_BLEND); else GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(this.blendConfig[0], this.blendConfig[1], this.blendConfig[2], this.blendConfig[3]);
        GL14.glBlendEquation(this.blendConfig[4]);
        program.active();
        switch (this.getPrimeMatrixState()) {
            case 0:
                break;
            case 1: {
                Matrix4f matrix = inCampaignOrCombat ? CampaignRenderingManager.getGamePerspectiveViewport() : CombatRenderingManager.getGamePerspectiveViewport();
                ShaderCore.refreshGameViewportMatrix(CommonUtil.createFloatBuffer(matrix));
                break;
            }
            case 2: {
                ShaderCore.refreshGameViewportMatrix(this.pickPrimeMatrixPackage_mat4());
                break;
            }
            default: {
                ShaderCore.refreshGameViewportMatrixNone();
            }
        }
        GL20.glUniformMatrix4(program.location[0], false, this.pickModelMatrixPackage_mat4());
        GL20.glUniform1f(program.location[5], this.getCurrentItalicFactor());
        GL20.glUniform4f(program.location[6], this.state[4], this.state[5], this.state[6], this.state[7]);
        for (int i = 0; i < this.fontMapList.length; i++) {
            if (this.fontMapList[i] != null && this.fontMapList[i].isValid()) program.bindTexture2D(i, this.fontMapList[i].getMap().getTextureId());
        }
        GL30.glBindVertexArray(this._textFieldID);
        GL11.glDrawArrays(GL11.GL_POINTS, 0, this.textDataRefreshState[0]);
        GL30.glBindVertexArray(0);
        program.close();
        if (this.getPrimeMatrixState() != 0) {
            Matrix4f matrix = inCampaignOrCombat ? CampaignRenderingManager.getGameOrthoViewport() : CombatRenderingManager.getGameOrthoViewport();
            ShaderCore.refreshGameViewportMatrix(CommonUtil.createFloatBuffer(matrix));
        }
        GL11.glPopAttrib();
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboNow);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    /**
     * Excludes text data.
     */
    public void reset() {
        super.reset();
        this.state[0] = 0.0f;
        this.state[1] = 0.0f;
        this.state[2] = 0.0f;
        this.state[3] = 0.0f;
        this.state[4] = 1.0f;
        this.state[5] = 1.0f;
        this.state[6] = 1.0f;
        this.state[7] = 1.0f;
        this.textStateAfterSubmit[0] = 0.0f;
        this.textStateAfterSubmit[1] = 0.0f;
        this.textStateAfterSubmit[2] = 0.2f;
    }

    public void resetText() {
        this.fontMapList = null;
        this.textDataRefreshState[0] = 0;
        this.textDataRefreshState[1] = 0;
        this.textDataRefreshState[2] = 0;
        this._lastTextDataList = null;
        this._lastTextDataState = null;
    }

    protected TextData createTextData(String text, float padding, Color color, boolean invert, boolean italic, boolean underline, boolean strikeout, int fontMapIndex) {
        TextData para = new TextData();
        para.text = text == null ? "null" : text;
        para.byteState[0] = (byte) Math.max(0, Math.min(fontMapIndex, 3));
        para.pad = padding;
        byte[] colorArray = CommonUtil.colorToByteArray(color);
        if (invert) para.byteState[1] = 0b1000;
        if (italic) para.byteState[1] |= 0b0100;
        if (underline) para.byteState[1] |= 0b0010;
        if (strikeout) para.byteState[1] |= 0b0001;
        para.byteState[2] = colorArray[0];
        para.byteState[3] = colorArray[1];
        para.byteState[4] = colorArray[2];
        para.byteState[5] = colorArray[3];
        return para;
    }

    public TextData addText(String text, int fontMapIndex) {
        return this.addText(text, _DEFAULT_PAD, Misc.getTextColor(), false, false, false, false, fontMapIndex);
    }

    /**
     * @param padding offset when line feed.
     */
    public TextData addText(String text, float padding, int fontMapIndex) {
        return this.addText(text, padding, Misc.getTextColor(), false, false, false, false, fontMapIndex);
    }

    /**
     * @param padding offset when line feed.
     */
    public TextData addText(String text, float padding, Color color, boolean invert, boolean italic, boolean underline, boolean strikeout, int fontMapIndex) {
        if (fontMapIndex < 0) return null;
        if (this._lastTextDataList == null) this._lastTextDataList = new ArrayList<>();
        TextData para = createTextData(text, padding, color, invert, italic, underline, strikeout, fontMapIndex);
        this._lastTextDataList.add(para);
        return para;
    }

    public TextData replaceTextAtParagraph(@NotNull String text, int fontMapIndex, int paragraphIndex) {
        return this.replaceTextAtParagraph(text, _DEFAULT_PAD, Misc.getTextColor(), false, false, false, false, fontMapIndex, paragraphIndex);
    }

    /**
     * @param padding offset when line feed.
     */
    public TextData replaceTextAtParagraph(@NotNull String text, float padding, int fontMapIndex, int paragraphIndex) {
        return this.replaceTextAtParagraph(text, padding, Misc.getTextColor(), false, false, false, false, fontMapIndex, paragraphIndex);
    }

    /**
     * @param padding offset when line feed.
     */
    public TextData replaceTextAtParagraph(@NotNull String text, float padding, Color color, boolean invert, boolean italic, boolean underline, boolean strikeout, int fontMapIndex, int paragraphIndex) {
        if (fontMapIndex < 0) return null;
        if (this._lastTextDataList == null || paragraphIndex >= this._lastTextDataList.size()) return null;
        TextData para = createTextData(text, padding, color, invert, italic, underline, strikeout, fontMapIndex);
        this._lastTextDataList.set(paragraphIndex, para);
        return para;
    }

    public static class TextData {
        public String text = "";
        public float pad = 0.0f; // mapIndex, pad
        public final byte[] byteState = new byte[]{0, 0, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR, BoxEnum.ONE_COLOR}; // mapIndex, bit4(invert, italic, underline, strikeout), vec4(color)

        public TextData() {}

        public byte getMapIndex() {
            return this.byteState[0];
        }

        public float getPadding() {
            return this.pad;
        }

        public int pickFontStylePart() {
            return this.byteState[1] << 5 & 0b111100000 | this.byteState[0];
        }

        public byte[][] pickColorPackage_vec4() {
            return new byte[][]{new byte[]{this.byteState[3], this.byteState[5], 0, 0}, new byte[]{this.byteState[2], this.byteState[4], 0, 0}};
        }
    }

    /**
     * Cost <strong>28Byte</strong> of vRAM each character, that if it had <strong>32768</strong> characters will cost <strong>917.5KB</strong> of vRAM.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when an empty text data list or refresh count is zero.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte submitText() {
        if (this._lastTextDataList == null || this._lastTextDataList.isEmpty()) return BoxEnum.STATE_FAILED;
        if (!BoxConfigs.isVAOSupported()) return BoxEnum.STATE_FAILED_OTHER;
        final int textDataRefreshIndex = this.textDataRefreshState[1];
        final int textDataRefreshCount = this.textDataRefreshState[2];
        final int textDataRefreshLimit = textDataRefreshIndex + textDataRefreshCount;
        if (textDataRefreshCount <= 0) return BoxEnum.STATE_FAILED;
        if (this._lastTextDataState == null) this._lastTextDataState = new ArrayList<>(textDataRefreshCount + 8);

        List<ShortBuffer> tmpBufferList = new ArrayList<>();
        ShortBuffer vboBuffer;
        int tmpBufferAddIndex, charLength, lastCharLength;
        tmpBufferAddIndex = charLength = lastCharLength = 0;
        final int preDataIndex = textDataRefreshIndex - 1;
        float currentLine, currentStep, lastLineVisualWidth = 0.0f, maxLineVisualWidth = 0.0f;
        for (int i = 0; i < Math.min(textDataRefreshIndex, this._lastTextDataState.size()); i++) {
            lastCharLength += (int) this._lastTextDataState.get(i).getZ();
        }
        if (textDataRefreshIndex >= this._lastTextDataState.size()) {
            currentLine = currentStep = 0.0f;
        } else {
            if (preDataIndex > -1) {
                Stack3f tmp = this._lastTextDataState.get(preDataIndex);
                currentLine = tmp.getX();
                currentStep = tmp.getY();
            } else currentLine = currentStep = 0.0f;
        }
        char[] charArray;
        byte offset = (byte) (this.getAlignment() == Alignment.MID ? 2 : (this.getAlignment() == Alignment.RIGHT ? 1 : 0));
        int charLimit, currentTextSize, lineValidChar = 0;
        char lastCharacter;
        boolean putWidthDataCheck = false, hasCharSubmit = false, notDefaultAlignment = offset > 0;
        ShortBuffer tmpBuffer;
        Pair<List<Float>, Float> lastLineAlignmentData = new Pair<List<Float>, Float>(new ArrayList<Float>(32), 0.0f);
        List<Float> lineStepData = new ArrayList<>(32);
        List<Pair<List<Float>, Float>> currLineAlignmentData = new ArrayList<>(8); // lineCharCount, currWidth
        for (int i = textDataRefreshIndex; i < textDataRefreshLimit; i++) {
            TextData textData = this._lastTextDataList.get(i);
            if (textData == null) continue;
            final byte mapIndex = textData.getMapIndex();
            FontMapData currentFontMapData = this.fontMapList[mapIndex];
            FontMapData.FontData fontData;
            HashMap<Character, Byte> kerningMap;
            String text = textData.text;
            final byte[][] textDataArray = textData.pickColorPackage_vec4();
            final int style = textData.pickFontStylePart();
            final float currentLineHeight = currentFontMapData.getLineHeight();
            lastCharacter = 0;
            float charFill = 0;
            char character;
            byte kerningValue;
            boolean isLineFeed, currNotFound, haveNotFoundSymbol = currentFontMapData.containsFont(NOT_FOUND_SYMBOL);
            if (Math.abs(currentLine - currentLineHeight) > this.getTextFieldHeight()) break;
            charArray = text.toCharArray();
            currentTextSize = charArray.length;
            charLimit = currentTextSize - 1;
            tmpBuffer = BufferUtils.createShortBuffer(currentTextSize * _VBO_COUNT);
            charLength += currentTextSize;
            currentLine -= textData.getPadding();
            for (int j = 0; j < charArray.length; j++) {
                character = charArray[j];
                isLineFeed = character == LINE_FEED_SYMBOL;
                currNotFound = !currentFontMapData.containsFont(character);
                if (currNotFound && haveNotFoundSymbol) {
                    currNotFound = false;
                    character = NOT_FOUND_SYMBOL;
                }
                if (currNotFound || isLineFeed) {
                    if (isLineFeed) currentLine -= currentLineHeight + this.getFontHeightSpace();
                    charLength--;
                    currentTextSize--;
                    lastCharacter = character;
                    currentStep = 0.0f;
                    charFill = 0.0f;
                    if (j >= charLimit) {
                        Stack3f currentTMP = new Stack3f(currentLine, currentStep, currentTextSize);
                        if (i >= this._lastTextDataState.size()) this._lastTextDataState.add(currentTMP); else this._lastTextDataState.set(i, currentTMP);
                    }
                    if (notDefaultAlignment && lineValidChar > 0) {
                        putWidthDataCheck = false;
                        currLineAlignmentData.add(new Pair<>(lineStepData, lastLineVisualWidth));
                        lineStepData = new ArrayList<>(32);
                        lineValidChar = 0;
                    }
                    continue;
                }

                fontData = currentFontMapData.getFont(character);
                currentStep += fontData.getXOffset();
                if (currentFontMapData.haveKerning() && currentFontMapData.containsKerning(lastCharacter)) {
                    kerningMap = currentFontMapData.getKerningMap(lastCharacter);
                    if (kerningMap.containsKey(character)) {
                        kerningValue = kerningMap.get(character);
                        currentStep += kerningValue;
                        charFill += Math.max(kerningValue, 0);
                    }
                }

                if (currentStep + fontData.getSize()[0] > this.getTextFieldWidth()) {
                    currentStep = fontData.getXOffset();
                    currentLine -= currentLineHeight + this.getFontHeightSpace();
                    charFill = 0.0f;

                    putWidthDataCheck = false;
                    currLineAlignmentData.add(new Pair<>(lineStepData, lastLineVisualWidth));
                    lineStepData = new ArrayList<>(32);
                    lineValidChar = 0;
                }
                lastLineVisualWidth = currentStep + fontData.getSize()[0];
                maxLineVisualWidth = Math.max(maxLineVisualWidth, lastLineVisualWidth);

                if (Math.abs(currentLine - currentLineHeight) > this.getTextFieldHeight()) break;
                charFill = Math.max(charFill + fontData.getXOffset(), 0.0f);

                CommonUtil.putFloat16(tmpBuffer, fontData.getUVs());
                CommonUtil.putFloat16(tmpBuffer, currentStep);
                CommonUtil.putFloat16(tmpBuffer, currentLine - fontData.getYOffset());
                CommonUtil.putFloat16(tmpBuffer, 1.0f - (float) fontData.getYOffset() / currentLineHeight); // topStyleUV
                CommonUtil.putFloat16(tmpBuffer, 1.0f - (float) (fontData.getSize()[1] + fontData.getYOffset()) / currentLineHeight); // bottomStyleUV
                tmpBuffer.put((short) (style | fontData.getChannel()));
                textDataArray[1][2] = fontData.getSize()[0];
                textDataArray[0][2] = fontData.getSize()[1];
                textDataArray[1][3] = fontData.getYOffset();
                textDataArray[0][3] = (byte) Math.max(currentLineHeight - fontData.getYOffset() - fontData.getSize()[1], 0);
                CommonUtil.putPackingBytes(tmpBuffer, textDataArray[0], textDataArray[1]);
                CommonUtil.putFloat16(tmpBuffer, charFill);

                if (notDefaultAlignment) {
                    if (!putWidthDataCheck) putWidthDataCheck = true;
                    lineStepData.add(currentStep);
                    lineValidChar++;
                }
                lastCharacter = character;
                currentStep += fontData.getXAdvance() + this.getFontWidthSpace();
                charFill = fontData.getXAdvance() - fontData.getSize()[0] + this.getFontWidthSpace();
                if (!hasCharSubmit) {
                    hasCharSubmit = true;
                    this.textStateAfterSubmit[0] = 0.0f;
                }
            }
            if (notDefaultAlignment) {
                lastLineAlignmentData.one = lineStepData;
                lastLineAlignmentData.two = lastLineVisualWidth;
            }
            tmpBuffer.position(0);
            tmpBuffer.limit(currentTextSize * _VBO_COUNT);
            tmpBufferList.add(tmpBuffer);
            if (i >= this._lastTextDataState.size()) {
                this._lastTextDataState.add(new Stack3f(currentLine, currentStep, currentTextSize));
            } else {
                this._lastTextDataState.set(i, new Stack3f(currentLine, currentStep, currentTextSize));
            }
            if (hasCharSubmit) this.textStateAfterSubmit[0] = Math.max(this.textStateAfterSubmit[0], maxLineVisualWidth);
            this.textStateAfterSubmit[1] = Math.abs(currentLine - currentLineHeight);
        }
        if (putWidthDataCheck && !lastLineAlignmentData.one.isEmpty()) currLineAlignmentData.add(lastLineAlignmentData);

        if (charLength < 1) return BoxEnum.STATE_FAILED;
        vboBuffer = BufferUtils.createShortBuffer(charLength * _VBO_COUNT);
        for (ShortBuffer shortBuffer : tmpBufferList) {
            vboBuffer.put(shortBuffer);
            tmpBufferAddIndex += shortBuffer.limit();
            vboBuffer.position(tmpBufferAddIndex);
        }
        if (offset > 0) {
            int lineBufferIndex = 0;
            float currOffsetStep;
            for (Pair<List<Float>, Float> line : currLineAlignmentData) {
                currOffsetStep = Math.max(this.getTextFieldWidth() - line.two, 0.0f) / offset;
                for (float step : line.one) {
                    vboBuffer.put(lineBufferIndex * _VBO_COUNT + 4, CommonUtil.float16ToShort(step + currOffsetStep));
                    lineBufferIndex++;
                }
            }
        }
        vboBuffer.position(0);
        vboBuffer.limit(vboBuffer.capacity());
        charLength += lastCharLength;
        final boolean newBuffer = charLength > this.textDataRefreshState[0];
        this.textDataRefreshState[0] = charLength;

        if (this._textFieldVBO == 0) {
            final int sizeH = BoxDatabase.HALF_FLOAT_SIZE * _VBO_COUNT;
            this._textFieldVBO = GL15.glGenBuffers();
            if (this._textFieldID == 0 || this._textFieldVBO == 0) return BoxEnum.STATE_FAILED_OTHER;
            GL30.glBindVertexArray(this._textFieldID);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this._textFieldVBO);
            GL20.glVertexAttribPointer(0, 4, GL30.GL_HALF_FLOAT, false, sizeH, 0);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(1, 4, GL30.GL_HALF_FLOAT, false, sizeH, 4 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(2, 1, GL11.GL_UNSIGNED_SHORT, false, sizeH, 8 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, sizeH, 9 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(4, 2, GL11.GL_UNSIGNED_BYTE, false, sizeH, 11 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(5, 2, GL11.GL_BYTE, false, sizeH, 12 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(6, 1, GL30.GL_HALF_FLOAT, false, sizeH, 13 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(6);
            GL30.glBindVertexArray(0);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this._textFieldVBO);
        if (newBuffer) GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboBuffer, this.textDataRefreshState[3]);
        else GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, (long) lastCharLength * BoxDatabase.HALF_FLOAT_SIZE * _VBO_COUNT, vboBuffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        return BoxEnum.STATE_SUCCESS;
    }

    /**
     * Optional.<p>
     * Just <strong>malloc()</strong> without any submit call.
     *
     * @param charNum must be positive integer.
     *
     * @return return {@link BoxEnum#STATE_SUCCESS} when success.<p> return {@link BoxEnum#STATE_FAILED} when parameter error.<p> return {@link BoxEnum#STATE_FAILED_OTHER} when happened another error.
     */
    public byte mallocTextData(int charNum) {
        if (charNum < 1) return BoxEnum.STATE_FAILED;

        if (this._textFieldVBO == 0) {
            final int sizeH = BoxDatabase.HALF_FLOAT_SIZE * _VBO_COUNT;
            this._textFieldVBO = GL15.glGenBuffers();
            if (this._textFieldID == 0 || this._textFieldVBO == 0) return BoxEnum.STATE_FAILED_OTHER;
            GL30.glBindVertexArray(this._textFieldID);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this._textFieldVBO);
            GL20.glVertexAttribPointer(0, 4, GL30.GL_HALF_FLOAT, false, sizeH, 0);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(1, 4, GL30.GL_HALF_FLOAT, false, sizeH, 4 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(2, 1, GL30.GL_HALF_FLOAT, false, sizeH, 8 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, sizeH, 9 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(4, 2, GL11.GL_UNSIGNED_BYTE, false, sizeH, 11 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(5, 2, GL11.GL_BYTE, false, sizeH, 12 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(6, 1, GL30.GL_HALF_FLOAT, false, sizeH, 13 * BoxDatabase.HALF_FLOAT_SIZE);
            GL20.glEnableVertexAttribArray(6);
            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this._textFieldVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) charNum * BoxDatabase.HALF_FLOAT_SIZE * _VBO_COUNT, this.textDataRefreshState[3]);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        this.textDataRefreshState[0] = charNum;
        return BoxEnum.STATE_SUCCESS;
    }

    public int getTextDataRefreshIndex() {
        return this.textDataRefreshState[1];
    }

    /**
     * @param index Will refresh text data start from the index.
     */
    public void setTextDataRefreshIndex(int index) {
        if (this._lastTextDataList == null) return;
        this.textDataRefreshState[1] = Math.min(Math.max(index, 0), Math.max((short) this._lastTextDataList.size() - 1, 0));
    }

    /**
     * @param size Will refresh text data count.
     */
    public void setTextDataRefreshSize(int size) {
        if (this._lastTextDataList == null) return;
        int total = this._lastTextDataList.size();
        this.textDataRefreshState[2] = this.textDataRefreshState[1] + size > total ? total - this.textDataRefreshState[1] : size;
    }

    public void setTextDataRefreshAllFromCurrentIndex() {
        if (this._lastTextDataList == null) return;
        this.textDataRefreshState[2] = this._lastTextDataList.size() - this.textDataRefreshState[1];
    }
    public List<TextData> getTextDataList() {
        return this._lastTextDataList;
    }

    public int getValidCharLength() {
        return this.textDataRefreshState[0];
    }

    public boolean isValidRenderingTextField() {
        return this.textDataRefreshState[0] > 0;
    }

    public Alignment getAlignment() {
        return this.alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public float getFontWidthSpace() {
        return this.state[0];
    }

    public float getFontHeightSpace() {
        return this.state[1];
    }

    public float getTextFieldWidth() {
        return this.state[2];
    }

    public float getTextFieldHeight() {
        return this.state[3];
    }

    /**
     * @param space interval to previous font.
     */
    public void setFontWidthSpace(float space) {
        this.state[0] = space;
    }

    /**
     * @param space line interval to previous line of same(only) text data.
     */
    public void setFontHeightSpace(float space) {
        this.state[1] = space;
    }

    public void setFontSpace(float width, float height) {
        this.setFontWidthSpace(width);
        this.setFontHeightSpace(height);
    }

    public void setFieldWidth(float width) {
        this.state[2] = width;
    }

    public void setFieldHeight(float height) {
        this.state[3] = height;
    }

    public void setFieldSize(float width, float height) {
        this.setFieldWidth(width);
        this.setFieldHeight(height);
    }

    /**
     * After called {@link TextFieldEntity#submitText()}.<p>
     * But only counts for last submit call, not for the entity.
     */
    public float getCurrentVisualWidth() {
        return this.textStateAfterSubmit[0];
    }

    /**
     * After called {@link TextFieldEntity#submitText()}.
     */
    public float getCurrentVisualHeight() {
        return this.textStateAfterSubmit[1];
    }

    public float getCurrentItalicFactor() {
        return this.textStateAfterSubmit[2];
    }

    /**
     * @param angle range at <strong>[-90, 90]</strong>.
     */
    public void setItalicFactor(float angle) {
        this.textStateAfterSubmit[2] = (float) Math.sin(Math.toRadians(angle));
    }

    /**
     * @param value value of sin, and angle range at <strong>[-90, 90]</strong>.
     */
    public void setItalicFactorDirect(float value) {
        this.textStateAfterSubmit[2] = value;
    }

    public void setDefaultItalicFactor() {
        this.textStateAfterSubmit[2] = 0.2f;
    }

    public float[] getGlobalColorArray() {
        return new float[]{this.state[4], this.state[5], this.state[6], this.state[7]};
    }

    public Color getGlobalColorC() {
        return CommonUtil.toCommonColor(this.getGlobalColor());
    }

    public Vector4f getGlobalColor() {
        return new Vector4f(this.state[4], this.state[5], this.state[6], this.state[7]);
    }

    public float getGlobalColorAlpha() {
        return this.state[7];
    }

    public int getGlobalColorAlphaI() {
        return Math.max(Math.min(Math.round(this.state[7] * 255.0f), 255), 0);
    }

    public void setGlobalColor(@NotNull Vector4f color) {
        this.state[4] = color.x;
        this.state[5] = color.y;
        this.state[6] = color.z;
        this.state[7] = color.w;
    }

    public void setGlobalColor(float r, float g, float b, float a) {
        this.state[4] = r;
        this.state[5] = g;
        this.state[6] = b;
        this.state[7] = a;
    }

    public void setGlobalColor(Color color) {
        this.state[4] = color.getRed() / 255.0f;
        this.state[5] = color.getGreen() / 255.0f;
        this.state[6] = color.getBlue() / 255.0f;
        this.state[7] = color.getAlpha() / 255.0f;
    }

    public void setGlobalColorAlpha(float alpha) {
        this.state[7] = alpha;
    }
}
