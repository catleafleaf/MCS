package org.boxutil.units.builtin.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.boxutil.config.BoxConfigGUI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.standard.misc.UIBorderObject;
import org.boxutil.util.CalculateUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class BUtil_BaseConfigPanel implements CustomUIPanelPlugin {
    private final static float _SCREEN_SCALE = Global.getSettings().getScreenScaleMult();
    private final static float _SPACE_BOTTOM = 30.0f;
    private final static float _BORDER_WIDTH = 8.0f;
    private final static float _ITEM_SPACE = 5.0f;
    private final static float _ITEM_SPLIT_WIDTH = 2.0f;
    private final static float _MAIN_BUTTON_SPACE = 5.0f;
    private final static float _TIPS_WIDTH_FACTOR = 0.33333f;
    private final static byte _BUTTON_CANCEL = 0;
    private final static byte _BUTTON_SAVE = 1;
    private final static byte _BUTTON_DEFAULTS = 2;
    private final static byte _BUTTON_UNDO = 3;
    private final static byte _BUTTON_TIPS_EXIT = 0;
    private final static byte _BUTTON_TIPS_RETURN = 1;
    private final static float _ITEM_HEIGHT = 64.0f;
    private final static float _TRACKBAR_NUM_SPACES = 84.0f;
    private final static float _TRACKBAR_HEIGHT = 50.0f;
    private final static float _ITEM_BUTTON_WIDTH = 64.0f;
    private final static float _ITEM_BUTTON_HEIGHT = 32.0f;
    private final static Color _BUTTON_COLOR_BG = Global.getSettings().getColor("buttonBgDark");
    private final static float[] _ITEM_COLOR_BG = new float[4];
    private final static float[] _ITEM_SPLIT_COLOR = new float[4];
    private final static float _EVENT_CD = 0.1f;
    private CustomPanelAPI panel = null;
    private final BoxConfigGUI host;
    private final UIBorderObject backgroundObject;
    private CloseTips tipsPlugin = null;
    private CustomPanelAPI tipsPanel = null;
    private byte panelFlag = 0;
    private TooltipMakerAPI[] mainConfigMakers = new TooltipMakerAPI[3]; // title, button, configBody
    private final float[] state = new float[9];
    private LabelAPI _heading = null;
    private ButtonAPI _cancelButton = null;
    private ButtonAPI _saveButton = null;
    private ButtonAPI _defaultButton = null;
    private ButtonAPI _undoButton = null;
    private boolean _configChanged = false;
    private List<Item> _items = new ArrayList<>(12);
    private LabelAPI[] _itemTitle = new LabelAPI[3];
    private boolean panelOn = false;
    private int backgroundTex = 0;
    private float[] backgroundTexUV = new float[2];

    static {
        final float[] _itemSplitColorTmp = Global.getSettings().getColor("widgetBorderColorDark").getComponents(new float[4]);
        _ITEM_SPLIT_COLOR[0] = _itemSplitColorTmp[0];
        _ITEM_SPLIT_COLOR[1] = _itemSplitColorTmp[1];
        _ITEM_SPLIT_COLOR[2] = _itemSplitColorTmp[2];
        _ITEM_SPLIT_COLOR[3] = _itemSplitColorTmp[3] * 0.42f;
        final float[] _itemColorTmp = _BUTTON_COLOR_BG.getComponents(new float[4]);
        _ITEM_COLOR_BG[0] = _itemColorTmp[0];
        _ITEM_COLOR_BG[1] = _itemColorTmp[1];
        _ITEM_COLOR_BG[2] = _itemColorTmp[2];
        _ITEM_COLOR_BG[3] = _itemColorTmp[3] * 0.42f;
    }

    public BUtil_BaseConfigPanel(BoxConfigGUI host, float width, float height, float BLx, float BLy) {
        this.host = host;
        this.backgroundObject = new UIBorderObject(false, true);
        this.state[0] = width;
        this.state[1] = height;
        this.state[2] = BLx;
        this.state[3] = BLy;
        this.state[4] = this.state[2] - _BORDER_WIDTH;
        this.state[5] = this.state[3] - _BORDER_WIDTH;
        this.state[6] = width + _BORDER_WIDTH + _BORDER_WIDTH;
        this.state[7] = height + _BORDER_WIDTH + _BORDER_WIDTH;
        this.backgroundObject.setSize(this.state[6], this.state[7]);
    }

    private void addMainConfigComponent() {
        this.panel.addUIElement(this.mainConfigMakers[0]);
        this.panel.addUIElement(this.mainConfigMakers[1]);
        this.panel.addUIElement(this.mainConfigMakers[2]);
    }

    private void removeMainConfigComponent() {
        this.panel.removeComponent(this.mainConfigMakers[0]);
        this.panel.removeComponent(this.mainConfigMakers[1]);
        this.panel.removeComponent(this.mainConfigMakers[2]);
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;
        final float buttonWidth = Math.max(this.state[0] * 0.15f, 180.0f);
        this.mainConfigMakers[0] = this.panel.createUIElement(this.state[0], this.state[1] + _SPACE_BOTTOM + _BORDER_WIDTH, false);
        this.mainConfigMakers[0].getPosition().inTMid(0.0f);
        this._heading = this.mainConfigMakers[0].addSectionHeading(getString("BUtil_ConfigPanel_Title"), Misc.getTextColor(), Misc.getDarkPlayerColor(), Alignment.MID, 0.0f);
        this._heading.getPosition().inTMid(0.0f);
        this.mainConfigMakers[1] = this.panel.createUIElement(this.state[0], _SPACE_BOTTOM, false);
        this.mainConfigMakers[1].getPosition().inBMid(-_SPACE_BOTTOM - _BORDER_WIDTH);
        this.mainConfigMakers[1].setButtonFontOrbitron20();
        final float halfWidth = this.state[0] * 0.5f;
        this._cancelButton = mainButton(this.mainConfigMakers[1], "BUtil_ConfigPanel_Cancel", _BUTTON_CANCEL, buttonWidth, Keyboard.KEY_ESCAPE);
        this._cancelButton.getPosition().inBR(halfWidth - buttonWidth * 2.0f - _MAIN_BUTTON_SPACE * 1.5f, 0.0f);
        this._saveButton = mainButton(this.mainConfigMakers[1], "BUtil_ConfigPanel_Save", _BUTTON_SAVE, buttonWidth, Keyboard.KEY_RETURN);
        this._saveButton.getPosition().inBR(halfWidth - buttonWidth - _MAIN_BUTTON_SPACE * 0.5f, 0.0f);
        this._saveButton.setEnabled(false);
        this._defaultButton = mainButton(this.mainConfigMakers[1], "BUtil_ConfigPanel_Default", _BUTTON_DEFAULTS, buttonWidth, Keyboard.KEY_D);
        this._defaultButton.getPosition().inBL(halfWidth - buttonWidth - _MAIN_BUTTON_SPACE * 0.5f, 0.0f);
        this._undoButton = mainButton(this.mainConfigMakers[1], "BUtil_ConfigPanel_Undo", _BUTTON_UNDO, buttonWidth, Keyboard.KEY_U);
        this._undoButton.getPosition().inBL(halfWidth - buttonWidth * 2.0f - _MAIN_BUTTON_SPACE * 1.5f, 0.0f);
        this._undoButton.setEnabled(false);

        this.mainConfigMakers[2] = this.panel.createUIElement(this.state[0], this.state[1] - this._heading.getPosition().getHeight(), true);
        this.mainConfigMakers[2].getPosition().inBMid(0.0f);
        this.mainConfigMakers[2].setParaInsigniaVeryLarge();
        this._itemTitle[0] = this.initItemGlobal(this.mainConfigMakers[2]);
        this.mainConfigMakers[2].addSpacer(_SPACE_BOTTOM * 1.5f);
        this._itemTitle[1] = this.initItemCommon(this.mainConfigMakers[2]);
        this.mainConfigMakers[2].addSpacer(_SPACE_BOTTOM * 1.5f);
        this._itemTitle[2] = this.initItemMisc(this.mainConfigMakers[2]);
        this.addMainConfigComponent();

        if (this.tipsPlugin == null) {
            final float[] mainPanelSize = new float[]{ShaderCore.getScreenWidth() * _TIPS_WIDTH_FACTOR, 160.0f * _SCREEN_SCALE, 0.0f, 0.0f};
            mainPanelSize[2] = (this.state[0] - mainPanelSize[0]) * 0.5f;
            mainPanelSize[3] = (this.state[1] - mainPanelSize[1]) * 0.5f;
            this.tipsPlugin = new CloseTips(this, mainPanelSize[0], mainPanelSize[1], mainPanelSize[2], mainPanelSize[3]);
            this.tipsPanel = Global.getSettings().createCustom(mainPanelSize[0], mainPanelSize[1], this.tipsPlugin);
            this.tipsPanel.getPosition().inBL(mainPanelSize[2], mainPanelSize[3]);
            this.tipsPlugin.init(this.tipsPanel);
        }
    }

    private static String indexFill(byte index) {
        return index < 10 ? ("0" + index) : Byte.toString(index);
    }

    private LabelAPI initItemGlobal(TooltipMakerAPI maker) {
        LabelAPI titleGlobal = maker.addPara(getString("BUtil_ConfigPanel_Global"), Misc.getButtonTextColor(), _ITEM_SPACE);
        titleGlobal.setAlignment(Alignment.MID);
        maker.addSpacer(_ITEM_SPACE * 2.0f);
        for (byte i = 0; i < 2; i++) {
            Item item = new Item(this, this.state[0], i == 1, (byte) 0, i);
            item.add("BUtil_ConfigPanel_Global_" + indexFill(i), maker, true, false);
            this._items.add(item);
        }
        return titleGlobal;
    }

    private LabelAPI initItemCommon(TooltipMakerAPI maker) {
        LabelAPI titleCommon = maker.addPara(getString("BUtil_ConfigPanel_Common"), Misc.getButtonTextColor(), _ITEM_SPACE);
        titleCommon.setAlignment(Alignment.MID);
        maker.addSpacer(_ITEM_SPACE * 2.0f);
        for (byte i = 0; i < 8; i++) {
            boolean isBar = i == 4 || i == 5 || i == 6;
            Item item = new Item(this, this.state[0], i == 7, (byte) 1, i);
            item.add("BUtil_ConfigPanel_Common_" + indexFill(i), maker, false, isBar);
            this._items.add(item);
        }
        return titleCommon;
    }

    private LabelAPI initItemMisc(TooltipMakerAPI maker) {
        LabelAPI titleCommon = maker.addPara(getString("BUtil_ConfigPanel_Misc"), Misc.getButtonTextColor(), _ITEM_SPACE);
        titleCommon.setAlignment(Alignment.MID);
        maker.addSpacer(_ITEM_SPACE * 2.0f);
        boolean isGlobal;
        for (byte i = 0; i < 2; i++) {
            Item item = new Item(this, this.state[0], i == 1, (byte) 2, i);
            if (i == 0) isGlobal = false; else isGlobal = true;
            item.add("BUtil_ConfigPanel_Misc_" + indexFill(i), maker, isGlobal, false);
            this._items.add(item);
        }
        return titleCommon;
    }

    private void refreshItems() {
        for (Item item : this._items) item.refresh(true);
    }

    private void refreshItemsLanguage() {
        for (Item item : this._items) item.refreshLanguage();
        this.tipsPlugin.refresh();
        if (this._heading != null) this._heading.setText(getString("BUtil_ConfigPanel_Title"));
        if (this._cancelButton != null) {
            this._cancelButton.setText(getString("BUtil_ConfigPanel_Cancel"));
            this._cancelButton.setShortcut(Keyboard.KEY_ESCAPE, false);
        }
        if (this._saveButton != null) {
            this._saveButton.setText(getString("BUtil_ConfigPanel_Save"));
            this._saveButton.setShortcut(Keyboard.KEY_RETURN, false);
        }
        if (this._defaultButton != null) {
            this._defaultButton.setText(getString("BUtil_ConfigPanel_Default"));
            this._defaultButton.setShortcut(Keyboard.KEY_D, false);
        }
        if (this._undoButton != null) {
            this._undoButton.setText(getString("BUtil_ConfigPanel_Undo"));
            this._undoButton.setShortcut(Keyboard.KEY_U, false);
        }
        if (this._itemTitle[0] != null) this._itemTitle[0].setText(getString("BUtil_ConfigPanel_Global"));
        if (this._itemTitle[1] != null) this._itemTitle[1].setText(getString("BUtil_ConfigPanel_Common"));
        if (this._itemTitle[2] != null) this._itemTitle[2].setText(getString("BUtil_ConfigPanel_Misc"));
    }

    private static ButtonAPI mainButton(TooltipMakerAPI host, String id, byte type, float width, int hotkey) {
        ButtonAPI button = host.addButton(getString(id), type, Misc.getButtonTextColor(), _BUTTON_COLOR_BG, Alignment.MID, CutStyle.BOTTOM, width, _SPACE_BOTTOM, 0);
        button.setMouseOverSound("BUtil_button_in");
        button.setButtonPressedSound("BUtil_button_down");
        button.setShortcut(hotkey, false);
        return button;
    }

    private final static class Tooltip extends BaseTooltipCreator {
        public String textID;
        public final boolean global;
        public final byte[] _ID = new byte[2];
        public LabelAPI _label = null;

        public Tooltip(String textID, boolean isGlobal, byte master, byte item) {
            this.textID = textID;
            this.global = isGlobal;
            this._ID[0] = master;
            this._ID[1] = item;
        }

        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            String mainText = getString(this.textID);
            if (this.global) {
                String tips = mainText + getString("BUtil_ConfigPanel_RebootTips_Value");
                this._label = tooltip.addPara(tips, 0.0f, Misc.getButtonTextColor(), this.getRebootValue(), getString("BUtil_ConfigPanel_RebootTips"));
                this._label.setHighlightColors(Misc.getButtonTextColor(), Misc.getNegativeHighlightColor());
            }
            else this._label = tooltip.addPara(mainText, 0);
        }

        private String getRebootValue() {
            return getString(BoxConfigs.getRebootValueRealString(this._ID[0], this._ID[1]));
        }
    }

    private final static class Item extends BaseCustomUIPanelPlugin {
        private final BUtil_BaseConfigPanel _itemHost;
        private String id = null;
        private PositionAPI _pos = null;
        private LabelAPI _info = null;
        private LabelAPI _title = null;
        private BUtil_BaseTrackbar _trackbar = null;
        private final byte[] _ID = new byte[2];
        private final boolean _bottom;
        private boolean _pickSound = false;
        private final float[] state = new float[7];

        public Item(BUtil_BaseConfigPanel itemHost, float width, boolean bottom, byte master, byte item) {
            this._itemHost = itemHost;
            this.state[5] = width - _ITEM_SPACE - _ITEM_SPACE;
            this._bottom = bottom;
            this._ID[0] = master;
            this._ID[1] = item;
        }

        public UIComponentAPI add(String id, TooltipMakerAPI maker, boolean isGlobal, boolean isTrackbar) {
            this.id = id;
            CustomPanelAPI itemPanel = Global.getSettings().createCustom(this.state[5], _ITEM_HEIGHT, this);
            itemPanel.getPosition().inTL(_ITEM_SPACE, 0.0f);

            TooltipMakerAPI makerFixed = itemPanel.createUIElement(this.state[5], _ITEM_HEIGHT, false);
            makerFixed.getPosition().inTL(0.0f, 0.0f);
            makerFixed.setParaOrbitronLarge();
            makerFixed.setTitleOrbitronLarge();
            makerFixed.setButtonFontOrbitron24Bold();
            makerFixed.setParaFontColor(Misc.getTextColor());
            makerFixed.setTitleFontColor(Misc.getTextColor());
            this._title = makerFixed.addTitle(getString(this.id));
            this._title.getPosition().inTL(_ITEM_SPACE * 4.0f, (_ITEM_HEIGHT - this._title.getPosition().getHeight()) * 0.5f);

            Pair<String, Boolean> value = BoxConfigs.getValueString(this._ID[0], this._ID[1]);
            if (isTrackbar) {
                int valueI = Integer.parseInt(value.one);
                if (valueI == -1) {
                    value.one = getString("BUtil_ConfigPanel_ValueLimit");
                }
                float trackbarWidth = this.state[5] * 0.42f - _ITEM_SPACE * 3.0f - _TRACKBAR_NUM_SPACES + _ITEM_BUTTON_WIDTH;
                byte masStep = 10; // index 4
                if (this._ID[1] == 5) masStep = 5;
                if (this._ID[1] == 6) masStep = 7;
                this._trackbar = new BUtil_BaseTrackbar(makerFixed, value.one, trackbarWidth, _TRACKBAR_HEIGHT, _TRACKBAR_NUM_SPACES, masStep, 0.0f);
                if (valueI > 0) {
                    byte exponent = CalculateUtil.getExponentPOTMin(valueI);
                    if (this._ID[1] == 4) exponent -= 7;
                    if (this._ID[1] == 5) exponent -= 2;
                    if (this._ID[1] == 6) exponent -= 3;
                    this._trackbar.setCurrStep(exponent);
                } else if (valueI == -1) this._trackbar.setCurrStep(this._trackbar.getMaxStep());
                else this._trackbar.setCurrStep(0);
                this._trackbar.getPosition().inTR(_ITEM_SPACE * 5.0f, (_ITEM_HEIGHT - _TRACKBAR_HEIGHT) * 0.5f);
                this._info = this._trackbar.getValue();

                this.state[2] = _TRACKBAR_NUM_SPACES;
            } else {
                ButtonAPI buttonLeft = makerFixed.addButton("<", false, Misc.getButtonTextColor(), _BUTTON_COLOR_BG, Alignment.MID, CutStyle.ALL, _ITEM_BUTTON_WIDTH, _ITEM_BUTTON_HEIGHT, 0);
                buttonLeft.setMouseOverSound("BUtil_button_in");
                buttonLeft.setButtonPressedSound("BUtil_button_down");
                buttonLeft.getPosition().inTR(_ITEM_SPACE * 2.0f + this.state[5] * 0.42f, (_ITEM_HEIGHT - _ITEM_BUTTON_HEIGHT) * 0.5f);
                ButtonAPI buttonRight = makerFixed.addButton(">", true, Misc.getButtonTextColor(), _BUTTON_COLOR_BG, Alignment.MID, CutStyle.ALL, _ITEM_BUTTON_WIDTH, _ITEM_BUTTON_HEIGHT, 0);
                buttonRight.setMouseOverSound("BUtil_button_in");
                buttonRight.setButtonPressedSound("BUtil_button_down");
                buttonRight.getPosition().inTR(_ITEM_SPACE * 2.0f, (_ITEM_HEIGHT - _ITEM_BUTTON_HEIGHT) * 0.5f);
                this.state[2] = _ITEM_BUTTON_WIDTH;

                this._info = makerFixed.addPara(value.one, 0.0f);
                this._info.setColor(value.two ? Misc.getTextColor() : Misc.getNegativeHighlightColor());
                this._info.setAlignment(Alignment.MID);
                this._info.getPosition().inTR(_ITEM_SPACE * 2.0f + _ITEM_BUTTON_WIDTH, (_ITEM_HEIGHT - this._info.getPosition().getHeight()) * 0.5f);
                this._info.getPosition().setSize(this.state[5] * 0.42f - _ITEM_BUTTON_WIDTH, this._info.getPosition().getHeight());
            }
            this.state[0] = this.state[5] * 0.58f - _ITEM_SPACE * 2.0f;
            this.state[2] = this.state[5] - _ITEM_SPACE * 2.0f - this.state[2];
            this.state[4] = (this.state[0] + this.state[2]) * 0.5f;
            this.state[1] = (_ITEM_HEIGHT - _ITEM_BUTTON_HEIGHT) * 0.5f;
            this.state[3] = this.state[1] + _ITEM_BUTTON_HEIGHT;

            makerFixed.addTooltipTo(new Tooltip(this.id + "P", isGlobal, this._ID[0], this._ID[1]), makerFixed, TooltipMakerAPI.TooltipLocation.BELOW);
            makerFixed.setHeightSoFar(_ITEM_HEIGHT);
            itemPanel.addUIElement(makerFixed);
            this._pos = itemPanel.getPosition();
            if (isTrackbar) this._trackbar.setInputCheck(this._pos);
            return maker.addCustom(itemPanel, _ITEM_SPACE);
        }

        public void advance(float amount) {
            float mouseX = Mouse.getX();
            float mouseY = Mouse.getY();
            boolean within = mouseX >= this._pos.getX() && mouseY >= this._pos.getY() && mouseX <= this._pos.getX() + this._pos.getWidth() && mouseY <= this._pos.getY() + this._pos.getHeight();
            within &= !this._itemHost.tipsPlugin.panelOn;
            boolean barActive = this._trackbar != null && this._trackbar.isActive();
            float a5 = amount * 5.0f;
            if ((within || barActive) && this.state[6] < 1.0f) {
                this.state[6] = Math.min(this.state[6] + a5 + a5, 1.0f);
                if (!this._pickSound) {
                    this._pickSound = true;
                    Global.getSoundPlayer().playUISound("BUtil_ui_pick", 1.0f, 1.0f);
                }
            } else if (!within && !barActive && this.state[6] > 0.0f) {
                if (this._pickSound) this._pickSound = false;
                this.state[6] -= a5;
            }
            if (this._trackbar != null) {
                if (this._trackbar.haveChanged()) {
                    BoxConfigs.setValue(this._ID[0], this._ID[1], false, this._trackbar);
                    this.refresh(false);
                    this.callHostRefresh();
                }
                this._trackbar.setBreakAnimation(this._itemHost.tipsPlugin.panelOn);
            }
        }

        public void renderBelow(float alphaMult) {
            final float lineRaw = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
            GL11.glLineWidth(_ITEM_SPLIT_WIDTH);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(_ITEM_SPLIT_COLOR[0], _ITEM_SPLIT_COLOR[1], _ITEM_SPLIT_COLOR[2], 0.0f);
            final float widthPos = this._pos.getX() + this._pos.getWidth();
            final float heightPos = this._pos.getY() + this._pos.getHeight();
            final float splitHalf = this._pos.getWidth() * 0.5f;
            final float splitBottom = this._pos.getY() - _ITEM_SPACE * 0.5f;
            if (this._bottom) {
                GL11.glBegin(GL11.GL_LINE_STRIP);
                GL11.glVertex2f(this._pos.getX(), splitBottom);
                GL11.glColor4f(_ITEM_SPLIT_COLOR[0], _ITEM_SPLIT_COLOR[1], _ITEM_SPLIT_COLOR[2], _ITEM_SPLIT_COLOR[3] * alphaMult);
                GL11.glVertex2f(this._pos.getX() + splitHalf, splitBottom);
                GL11.glColor4f(_ITEM_SPLIT_COLOR[0], _ITEM_SPLIT_COLOR[1], _ITEM_SPLIT_COLOR[2], 0.0f);
                GL11.glVertex2f(widthPos, splitBottom);
                GL11.glEnd();
            }
            final float splitTop = heightPos + _ITEM_SPACE * 0.5f;
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex2f(this._pos.getX(), splitTop);
            GL11.glColor4f(_ITEM_SPLIT_COLOR[0], _ITEM_SPLIT_COLOR[1], _ITEM_SPLIT_COLOR[2], _ITEM_SPLIT_COLOR[3] * alphaMult);
            GL11.glVertex2f(this._pos.getX() + splitHalf, splitTop);
            GL11.glColor4f(_ITEM_SPLIT_COLOR[0], _ITEM_SPLIT_COLOR[1], _ITEM_SPLIT_COLOR[2], 0.0f);
            GL11.glVertex2f(widthPos, splitTop);
            GL11.glEnd();
            GL11.glLineWidth(lineRaw);

            if (this.state[6] > 0.0f) {
                float alpha = (float) Math.sqrt(this.state[6]);
                float heightSpace = 2.0f + this._pos.getHeight() * 0.1f;
                float rectX = this._pos.getX() + 3.0f;
                float rectY = this._pos.getY() + heightSpace;
                float rectYZero = this._pos.getY() + this._pos.getHeight() * 0.5f;
                float rectWidthPos = widthPos - 3.0f;
                float rectHeightPos = heightPos - heightSpace;
                GL11.glColor4f(_ITEM_COLOR_BG[0], _ITEM_COLOR_BG[1], _ITEM_COLOR_BG[2], alpha * alphaMult * 0.35f);
                GL11.glRectf(CalculateUtil.mix(rectX + 16.0f, rectX, this.state[6]), CalculateUtil.mix(rectYZero, rectY, this.state[6]), CalculateUtil.mix(rectWidthPos - 16.0f, rectWidthPos, this.state[6]), CalculateUtil.mix(rectYZero, rectHeightPos, this.state[6]));
            }

            float bgAlpha = (_ITEM_COLOR_BG[3] + this.state[6] * 0.25f) * alphaMult;
            if (this._trackbar != null) bgAlpha = CalculateUtil.mix(bgAlpha, 0.0f, this._trackbar.getAnimationProgress());
            if (bgAlpha > 0.0f) {
                float bgXA = this._pos.getX() + this.state[0];
                float bgXB = this._pos.getX() + this.state[4];
                float bgXC = this._pos.getX() + this.state[2];
                float bgYA = this._pos.getY() + this.state[1];
                float bgYB = this._pos.getY() + this.state[3];
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                GL11.glColor4f(_ITEM_COLOR_BG[0], _ITEM_COLOR_BG[1], _ITEM_COLOR_BG[2], 0.0f);
                GL11.glVertex2f(bgXA, bgYA);
                GL11.glVertex2f(bgXA, bgYB);
                GL11.glColor4f(_ITEM_COLOR_BG[0], _ITEM_COLOR_BG[1], _ITEM_COLOR_BG[2], bgAlpha);
                GL11.glVertex2f(bgXB, bgYA);
                GL11.glVertex2f(bgXB, bgYB);
                GL11.glColor4f(_ITEM_COLOR_BG[0], _ITEM_COLOR_BG[1], _ITEM_COLOR_BG[2], 0.0f);
                GL11.glVertex2f(bgXC, bgYA);
                GL11.glVertex2f(bgXC, bgYB);
                GL11.glEnd();
            }
        }

        public void buttonPressed(Object buttonId) {
            if (this._itemHost.state[8] >= _EVENT_CD) this._itemHost.state[8] = 0.0f; else return;
            if (this._itemHost.tipsPlugin.panelOn || !this._itemHost.panelOn || !(buttonId instanceof Boolean)) return;
            BoxConfigs.setValue(this._ID[0], this._ID[1], (boolean) buttonId, this._trackbar);
            this.refresh(false);
            this.callHostRefresh();
        }

        public void callHostRefresh() {
            this._itemHost._configChanged = true;
            if (!this._itemHost._saveButton.isEnabled()) {
                this._itemHost._saveButton.setEnabled(true);
                this._itemHost._saveButton.flash(false, 0.0f, 0.5f);
            }
            if (!this._itemHost._undoButton.isEnabled()) {
                this._itemHost._undoButton.setEnabled(true);
            }
        }

        public void refresh(boolean setTrackbar) {
            if (this._info == null) return;
            Pair<String, Boolean> value = BoxConfigs.getValueString(this._ID[0], this._ID[1]);
            if (this._trackbar != null) {
                int valueI = Integer.parseInt(value.one);
                if (valueI == -1) value.one = getString("BUtil_ConfigPanel_ValueLimit");
                if (setTrackbar) {
                    if (valueI > 0) {
                        byte exponent = CalculateUtil.getExponentPOTMin(valueI);
                        if (this._ID[1] == 4) exponent -= 7;
                        if (this._ID[1] == 5) exponent -= 2;
                        if (this._ID[1] == 6) exponent -= 3;
                        this._trackbar.setCurrStep(exponent);
                    } else if (valueI == -1) this._trackbar.setCurrStep(this._trackbar.getMaxStep());
                    else this._trackbar.setCurrStep(0);
                }
            }
            this._info.setText(value.one);
            this._info.setColor(value.two ? Misc.getTextColor() : Misc.getNegativeHighlightColor());
        }

        public void refreshLanguage() {
            if (this._title == null) return;
            this._title.setText(getString(this.id));
        }

        public void processInput(List<InputEventAPI> events) {
            float mouseX = Mouse.getX();
            float mouseY = Mouse.getY();
            boolean within = mouseX >= this._pos.getX() && mouseY >= this._pos.getY() && mouseX <= this._pos.getX() + this._pos.getWidth() && mouseY <= this._pos.getY() + this._pos.getHeight();

            if (!within || this._itemHost.tipsPlugin.panelOn || this._itemHost.state[8] < _EVENT_CD) return;
            boolean rightKey;
            for (InputEventAPI event : events) {
                if (event.isKeyDownEvent()) {
                    rightKey = event.getEventValue() == Keyboard.KEY_RIGHT;
                    if (event.getEventValue() == Keyboard.KEY_LEFT || rightKey) {
                        if (this._trackbar != null) {
                            if (this._trackbar.isEnabled()) {
                                this._trackbar.setCurrStep(this._trackbar.getCurrStep() + (rightKey ? this._trackbar.getKeyStep() : -this._trackbar.getKeyStep()));
                                Global.getSoundPlayer().playUISound(this._trackbar.getDraggingSoundID(), 1.0f, 1.0f);
                            } else Global.getSoundPlayer().playUISound(this._trackbar.getDisabledDraggingSoundID(), 1.0f, 0.5f);
                        } else Global.getSoundPlayer().playUISound("BUtil_button_down", 1.0f, 1.0f);
                        BoxConfigs.setValue(this._ID[0], this._ID[1], rightKey, this._trackbar);
                        this.refresh(false);
                        this.callHostRefresh();
                        this._itemHost.state[8] = 0.0f;
                        event.consume();
                        break;
                    }
                }
            }
        }
    }

    public void initBackgroundTex(int texture, float[] uvs) {
        this.backgroundTex = texture;
        this.backgroundTexUV = uvs;
    }

    public void renderBelow(float alphaMult) {
        if (this.backgroundTex != 0) {
            int currMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glViewport(0, 0, ShaderCore.getScreenScaleWidth(), ShaderCore.getScreenScaleHeight());
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.backgroundTex);
            GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(-1.0f, -1.0f);
            GL11.glTexCoord2f(0.0f, this.backgroundTexUV[1]);
            GL11.glVertex2f(-1.0f, 1.0f);
            GL11.glTexCoord2f(this.backgroundTexUV[0], 0.0f);
            GL11.glVertex2f(1.0f, -1.0f);
            GL11.glTexCoord2f(this.backgroundTexUV[0], this.backgroundTexUV[1]);
            GL11.glVertex2f(1.0f, 1.0f);
            GL11.glEnd();
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glMatrixMode(currMatrixMode);
        }
        this.backgroundObject.setAlpha(alphaMult);
        this.backgroundObject.render(this.state[4], this.state[5]);
    }

    public void processInput(List<InputEventAPI> events) {
        if (!this.panelOn || events == null || events.isEmpty() || this.tipsPlugin.panelOn) return;
        boolean shouldBreak = false;
        float mouseX = Mouse.getX() / _SCREEN_SCALE;
        float mouseY = Mouse.getY() / _SCREEN_SCALE;
        boolean inWidget = mouseX < this.state[4] || mouseY < this.state[5] || mouseX > this.state[4] + this.state[6] || mouseY > this.state[5] + this.state[7];
        for (InputEventAPI event : events) {
            if (event.isRMBDownEvent() && inWidget) {
                if (this._configChanged) {
                    this.panel.addComponent(this.tipsPanel);
                    this.tipsPlugin.panelOn = true;
                } else {
                    this.host.closePanel();
                    this.panelOn = false;
                }
                this.state[8] = 0.0f;
                shouldBreak = true;
            }
            event.consume();
            if (shouldBreak) break;
        }
    }

    public void buttonPressed(Object buttonId) {
        if (this.state[8] >= _EVENT_CD) this.state[8] = 0.0f; else return;
        if (this.tipsPlugin.panelOn || !this.panelOn) return;
        byte id = (byte) buttonId;
        switch (id) {
            case _BUTTON_CANCEL: {
                if (this._configChanged) {
                    this.panel.addComponent(this.tipsPanel);
                    this.tipsPlugin.panelOn = true;
                } else {
                    this.host.closePanel();
                    this.panelOn = false;
                }
                break;
            }
            case _BUTTON_SAVE: {
                this._saveButton.setEnabled(false);
                this._undoButton.setEnabled(false);
                this._configChanged = false;
                BoxConfigs.check();
                BoxConfigs.save();
                this.refreshItems();
                this.refreshItemsLanguage();
                break;
            }
            case _BUTTON_DEFAULTS: {
                this._configChanged = true;
                if (!this._saveButton.isEnabled()) {
                    this._saveButton.setEnabled(true);
                    this._saveButton.flash(false, 0.0f, 0.5f);
                }
                if (!this._undoButton.isEnabled()) {
                    this._undoButton.setEnabled(true);
                }
                BoxConfigs.setDefault();
                BoxConfigs.check();
                this.refreshItems();
                break;
            }
            case _BUTTON_UNDO: {
                this._saveButton.setEnabled(false);
                this._undoButton.setEnabled(false);
                this._configChanged = false;
                BoxConfigs.load();
                BoxConfigs.check();
                this.refreshItems();
            }
        }
    }

    public boolean isOn() {
        return this.panelOn;
    }

    public void stateSwitch(boolean on) {
        this.panelOn = on;
    }

    public void advance(float amount) {
        if (this.state[8] < _EVENT_CD) this.state[8] += amount;
    }

    public void render(float alphaMult) {}

    public void positionChanged(PositionAPI position) {}

    private static ButtonAPI tipsButton(TooltipMakerAPI host, String id, byte type, float width) {
        ButtonAPI button = host.addButton(getString(id), type, Misc.getButtonTextColor(), _BUTTON_COLOR_BG, Alignment.MID, CutStyle.TL_BR, width, _SPACE_BOTTOM, 0);
        button.setMouseOverSound("BUtil_button_in");
        button.setButtonPressedSound("BUtil_button_down");
        return button;
    }

    private final static class CloseTips implements CustomUIPanelPlugin {
        private CustomPanelAPI tipsPanel = null;
        private final UIBorderObject backgroundObject;
        private final BUtil_BaseConfigPanel tipsHost;
        private LabelAPI _text = null;
        private final ButtonAPI[] _button = new ButtonAPI[2];
        private final float[] state = new float[9];
        private boolean panelOn = false;

        public CloseTips(BUtil_BaseConfigPanel tipsHost, float width, float height, float BLx, float BLy) {
            this.tipsHost = tipsHost;
            this.backgroundObject = new UIBorderObject(false, true);
            this.state[0] = width;
            this.state[1] = height;
            this.state[2] = BLx;
            this.state[3] = BLy;
            this.state[4] = ShaderCore.getScreenWidth() * (1.0f - _TIPS_WIDTH_FACTOR) * 0.5f - _BORDER_WIDTH;
            this.state[5] = (ShaderCore.getScreenHeight() - height) * 0.5f - _BORDER_WIDTH;
            this.state[6] = width + _BORDER_WIDTH + _BORDER_WIDTH;
            this.state[7] = height + _BORDER_WIDTH + _BORDER_WIDTH;
            this.backgroundObject.setSize(this.state[6], this.state[7]);
        }

        public void init(CustomPanelAPI panel) {
            final float buttonWidth = Math.max(this.state[0] * 0.2f, 120.0f);
            final float buttonOffset = buttonWidth * 0.2f;
            final float textMakerHeight = this.state[1] - _SPACE_BOTTOM - _BORDER_WIDTH - _BORDER_WIDTH;
            this.tipsPanel = panel;
            TooltipMakerAPI makerFixed = this.tipsPanel.createUIElement(this.state[0], textMakerHeight, false);
            makerFixed.setParaOrbitronVeryLarge();
            makerFixed.setParaFontColor(Misc.getNegativeHighlightColor());
            this._text = makerFixed.addPara(getString("BUtil_ConfigPanel_CancelTips"), 0.0f);
            this._text.setAlignment(Alignment.MID);
            makerFixed.getPosition().inTMid((textMakerHeight - this._text.getPosition().getHeight()) * 0.5f);
            TooltipMakerAPI makerButton = this.tipsPanel.createUIElement(this.state[0], _SPACE_BOTTOM, false);
            makerButton.getPosition().inBMid(_BORDER_WIDTH);
            makerButton.setButtonFontOrbitron20();
            this._button[0] = tipsButton(makerButton, "BUtil_ConfigPanel_CancelTipsY", _BUTTON_TIPS_EXIT, buttonWidth);
            this._button[0].getPosition().inBL(buttonOffset, _BORDER_WIDTH);
            this._button[1] = tipsButton(makerButton, "BUtil_ConfigPanel_CancelTipsN", _BUTTON_TIPS_RETURN, buttonWidth);
            this._button[1].getPosition().inBR(buttonOffset, _BORDER_WIDTH);
            this.tipsPanel.addUIElement(makerFixed);
            this.tipsPanel.addUIElement(makerButton);
        }

        public void renderBelow(float alphaMult) {
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
            GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
            GL11.glRectf(-1.0f, -1.0f, 1.0f, 1.0f);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            this.backgroundObject.setAlpha(alphaMult);
            this.backgroundObject.render(this.state[4], this.state[5]);
        }

        public void processInput(List<InputEventAPI> events) {
            if (!this.panelOn || events == null || events.isEmpty()) return;
            boolean shouldBreak = false;
            float mouseX = Mouse.getX() / _SCREEN_SCALE;
            float mouseY = Mouse.getY() / _SCREEN_SCALE;
            boolean inWidget = mouseX < this.state[4] || mouseY < this.state[5] || mouseX > this.state[4] + this.state[6] || mouseY > this.state[5] + this.state[7];
            for (InputEventAPI event : events) {
                if ((event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE && this.state[8] >= _EVENT_CD) ||
                        (event.isRMBDownEvent() && inWidget)
                ) {
                    this.tipsHost.panel.removeComponent(this.tipsHost.tipsPanel);
                    this.state[8] = 0.0f;
                    this.panelOn = false;
                    shouldBreak = true;
                }
                event.consume();
                if (shouldBreak) break;
            }
        }

        public void buttonPressed(Object buttonId) {
            if (this.state[8] >= _EVENT_CD) this.state[8] = 0.0f; else return;
            if (!this.panelOn) return;
            byte id = (byte) buttonId;
            switch (id) {
                case _BUTTON_TIPS_EXIT: {
                    BoxConfigs.load();
                    BoxConfigs.check();
                    this.tipsHost.refreshItems();
                    this.tipsHost._saveButton.setEnabled(false);
                    this.tipsHost._undoButton.setEnabled(false);
                    this.tipsHost.panel.removeComponent(this.tipsPanel);
                    this.panelOn = false;
                    this.tipsHost.host.closePanel();
                    this.state[8] = 0.0f;
                    this.tipsHost.panelOn = false;
                    this.tipsHost._configChanged = false;
                    break;
                }
                case _BUTTON_TIPS_RETURN: {
                    this.tipsHost.panel.removeComponent(this.tipsPanel);
                    this.panelOn = false;
                }
            }
        }

        public void advance(float amount) {
            if (this.state[8] < _EVENT_CD) this.state[8] += amount;
        }

        public void render(float alphaMult) {}

        public void positionChanged(PositionAPI position) {}

        public void refresh() {
            if (this._text != null) this._text.setText(getString("BUtil_ConfigPanel_CancelTips"));
            if (this._button[0] != null) this._button[0].setText(getString("BUtil_ConfigPanel_CancelTipsY"));
            if (this._button[1] != null) this._button[1].setText(getString("BUtil_ConfigPanel_CancelTipsN"));
        }
    }

    private static String getString(String id) {
        String current = id;
        if (!id.endsWith(BoxDatabase.NONE_LANG)) current += BoxConfigs.getLanguage();
        return Global.getSettings().getString("ui", current);
    }
}
