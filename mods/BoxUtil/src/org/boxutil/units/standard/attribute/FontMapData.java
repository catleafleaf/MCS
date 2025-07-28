package org.boxutil.units.standard.attribute;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.apache.log4j.Level;
import org.boxutil.define.BoxEnum;
import org.boxutil.units.standard.entity.TextFieldEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

/**
 * Only supported one page loading now.
 * Do not use large texture font-map;
 * The font height is 255 the maximum, and 0 is the minimum.
 */
public class FontMapData {
    protected final static FontData _RESERVED_FONT = new FontData(BoxEnum.ONE, BoxEnum.ONE, BoxEnum.ZERO, BoxEnum.ZERO, BoxEnum.ZERO, BoxEnum.ZERO, BoxEnum.ZERO, BoxEnum.ZERO, BoxEnum.ZERO, (byte) 8);
    static {
        _RESERVED_FONT.uv[0] = -512.0f;
        _RESERVED_FONT.uv[1] = -512.0f;
        _RESERVED_FONT.uv[2] = -512.0f;
        _RESERVED_FONT.uv[3] = -512.0f;
    }
    protected String name = "";
    protected SpriteAPI fontMap = null;
    protected int fontMapID = 0;
    protected HashMap<Character, FontData> fonts = new HashMap<>();
    protected HashMap<Character, HashMap<Character, Byte>> kerning = null;

    protected final int[] size = new int[4]; // heightPerLine, fontOffsetOnLine, vec2(mapSize)
    protected boolean isValid = true;

    /**
     * @param fontPath should register the texture of font in <strong>settings.json</strong>, or set handel after by manual.
     */
    public FontMapData(String fontPath) {
        this.fonts.put(TextFieldEntity.RESERVED_SYMBOL, _RESERVED_FONT);
        StringBuilder mapPath = new StringBuilder();
        String[] fontFileFound = fontPath.split("/");
        for (int i = 0; i < fontFileFound.length - 1; i++) {
            mapPath.append(fontFileFound[i]).append("/");
        }
        String fontFile;
        try {
            fontFile = Global.getSettings().loadText(fontPath);
            BufferedReader reader = new BufferedReader(new StringReader(fontFile));
            String line;
            String lineLow;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                lineLow = line.toLowerCase();
                if (lineLow.startsWith("info")) {
                    String[] fieldFound = lineLow.split(" ");
                    for (String check : fieldFound) {
                        if (check.startsWith("face=")) this.name = check.replace("face=", "").replace("\"", "");
                    }
                }
                if (lineLow.startsWith("common")) {
                    String[] fieldFound = lineLow.split(" ");
                    int lineHeight = 0;
                    int base = 0;
                    for (String check : fieldFound) {
                        if (check.startsWith("lineheight=")) lineHeight = Integer.parseInt(check.replace("lineheight=", ""));
                        if (check.startsWith("base=")) base = Integer.parseInt(check.replace("base=", ""));
                        if (check.startsWith("scalew=")) this.size[2] = Integer.parseInt(check.replace("scalew=", ""));
                        if (check.startsWith("scaleh=")) this.size[3] = Integer.parseInt(check.replace("scaleh=", ""));
                    }
                    this.size[0] = lineHeight;
                    this.size[1] = base;
                }
                if (this.isValid && lineLow.startsWith("page") && this.fontMap == null) {
                    String[] fieldFound = line.split(" ");
                    String imageFile;
                    for (String check : fieldFound) {
                        if (check.toLowerCase().startsWith("file=")) {
                            imageFile = check.substring(5).replace("\"", "");
                            String format = imageFile.toLowerCase();
                            if (format.endsWith(".bmp") || format.endsWith(".jpg") || format.endsWith(".jpeg") || format.endsWith(".png") || imageFile.endsWith(".gif")) {
                                this.fontMap = Global.getSettings().getSprite(mapPath + imageFile);
                                this.fontMapID = this.fontMap.getTextureId();
                            }
                        }
                    }
                }
                if (this.isValid && lineLow.startsWith("char")) {
                    String[] fieldFound = lineLow.split(" ");
                    char id;
                    int x, y;
                    byte width, height, xOffset, yOffset, xAdvance, page, channel;
                    id = 0;
                    page = 0;
                    x = y = id;
                    width = height = xOffset = yOffset = xAdvance = page;
                    // R(4u), G(8u), B(12u), A(16u), RGBA(0u)
                    channel = 0b000_000_00;
                    for (String check : fieldFound) {
                        if (check.startsWith("id=")) id = (char) Integer.parseInt(check.replace("id=", ""));
                        if (check.startsWith("x=")) x = Integer.parseInt(check.replace("x=", ""));
                        if (check.startsWith("y=")) y = Integer.parseInt(check.replace("y=", ""));
                        if (check.startsWith("width=")) width = Byte.parseByte(check.replace("width=", ""));
                        if (check.startsWith("height=")) height = Byte.parseByte(check.replace("height=", ""));
                        if (check.startsWith("xoffset=")) xOffset = Byte.parseByte(check.replace("xoffset=", ""));
                        if (check.startsWith("yoffset=")) yOffset = Byte.parseByte(check.replace("yoffset=", ""));
                        if (check.startsWith("xadvance=")) xAdvance = Byte.parseByte(check.replace("xadvance=", ""));
                        if (check.startsWith("chnl=")) {
                            byte channelTmp = Byte.parseByte(check.replace("chnl=", ""));
                            if (channelTmp == 4) channel = 0b000_001_00;
                            if (channelTmp == 2) channel = 0b000_010_00;
                            if (channelTmp == 1) channel = 0b000_011_00;
                            if (channelTmp == 8) channel = 0b000_100_00;
                        }
                    }
                    this.fonts.put(id, new FontData(this.size[2], this.size[3], x, y, width, height, xOffset, yOffset, xAdvance, channel));
                }
                if (this.isValid && lineLow.startsWith("kerning")) {
                    if (this.kerning == null) this.kerning = new HashMap<>();
                    String[] fieldFound = lineLow.split(" ");
                    char first = 0, second = 0;
                    byte amount = 0;
                    for (String check : fieldFound) {
                        if (check.startsWith("first=")) first = (char) Integer.parseInt(check.replace("first=", ""));
                        if (check.startsWith("second=")) second = (char) Integer.parseInt(check.replace("second=", ""));
                        if (check.startsWith("amount=")) amount = Byte.parseByte(check.replace("amount=", ""));
                    }
                    if (!this.fonts.containsKey(first)) {
                        this.isValid = false;
                        break;
                    }
                    if (!this.kerning.containsKey(first)) this.kerning.put(first, new HashMap<Character, Byte>());
                    HashMap<Character, Byte> thisMap = this.kerning.get(first);
                    thisMap.put(second, amount);
                }
            }
            this.checkValidWhenChangedFontMap();
        } catch (IOException e) {
            this.isValid = false;
            Global.getLogger(FontMapData.class).log(Level.ERROR, "'Box Util' loading font file failed at path: '" + fontPath + "'. " + e.getMessage());
        }
    }

    protected void checkValidWhenChangedFontMap() {
        this.isValid = this.fontMapID != 0 && !this.fonts.isEmpty();
    }

    public boolean isValid() {
        return this.isValid;
    }

    public String getName() {
        return this.name;
    }

    public SpriteAPI getMap() {
        return this.fontMap;
    }

    public void setMap(SpriteAPI fontMap) {
        this.fontMap = fontMap;
        if (this.fontMap == null) {
            this.fontMapID = 0;
        } else this.fontMapID = this.fontMap.getTextureId();
        this.checkValidWhenChangedFontMap();
    }

    public int getMapID() {
        return this.fontMapID;
    }

    public void setMapID(int fontMapID) {
        this.fontMapID = fontMapID;
        this.checkValidWhenChangedFontMap();
    }

    public int getLineHeight() {
        return this.size[0];
    }

    public int getLineBase() {
        return this.size[1];
    }

    public int getMapWidth() {
        return this.size[2];
    }

    public int getMapHeight() {
        return this.size[3];
    }

    public boolean containsFont(char character) {
        return this.fonts.containsKey(character);
    }

    public FontData getFont(char character) {
        return this.fonts.get(character);
    }

    public boolean haveKerning() {
        return this.kerning != null && !this.kerning.isEmpty();
    }

    public boolean containsKerning(char character) {
        return this.kerning.containsKey(character);
    }

    public HashMap<Character, Byte> getKerningMap(char character) {
        return this.kerning.get(character);
    }

    private FontMapData() {}

    public static class FontData {
        public final float[] uv = new float[]{0.0f, 1.0f, 0.0f, 1.0f}; // uvBLx, uvBLy, uvTRx, uvTRy
        public final byte[] byteState = new byte[6]; // vec2(size), xOffset, yOffset, xAdvance, page, channel

        public FontData(int rawX, int rawY, int x, int y, byte width, byte height, byte xOffset, byte yOffset, byte xAdvance, byte channel) {
            this.uv[0] = (float) x / (float) rawX;
            this.uv[1] -= (float) y / (float) rawY;
            this.uv[2] = this.uv[0] + (float) width / (float) rawX;
            this.uv[3] = this.uv[1] - (float) height / (float) rawY;
            this.byteState[0] = width;
            this.byteState[1] = height;
            this.byteState[2] = xOffset;
            this.byteState[3] = yOffset;
            this.byteState[4] = xAdvance;
            this.byteState[5] = channel;
        }

        public float[] getUVs() {
            return this.uv;
        }

        public byte[] getSize() {
            return new byte[]{this.byteState[0], this.byteState[1]};
        }

        public byte getXOffset() {
            return this.byteState[2];
        }

        public byte getYOffset() {
            return this.byteState[3];
        }

        public byte getXAdvance() {
            return this.byteState[4];
        }

        /**
         * R(4u), G(8u), B(12u), A(16u), RGBA(0u)
         */
        public byte getChannel() {
            return this.byteState[5];
        }
    }
}
