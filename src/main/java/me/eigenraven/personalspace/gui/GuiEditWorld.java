package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.FlatLayerInfo;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import cpw.mods.fml.common.registry.GameRegistry;
import me.eigenraven.personalspace.CommonProxy;
import me.eigenraven.personalspace.PersonalSpaceMod;
import me.eigenraven.personalspace.block.PortalTileEntity;
import me.eigenraven.personalspace.config.AllowedBlock;
import me.eigenraven.personalspace.config.AllowedBlockRules;
import me.eigenraven.personalspace.config.Config;
import me.eigenraven.personalspace.net.Packets;
import me.eigenraven.personalspace.world.DimensionConfig;

public class GuiEditWorld extends GuiScreen {

    public PortalTileEntity tile;

    public int xSize, ySize, guiLeft, guiTop;

    public DimensionConfig desiredConfig = new DimensionConfig();
    public WSlider skyRed, skyGreen, skyBlue;
    public WSlider starBrightness;
    public WTextField biome;
    public int biomeCycle = 0;
    public WButton biomeEditButton;
    public WToggleButton enableWeather;
    public WCycleButton enableDaylightCycle;
    public WToggleButton enableClouds;
    public WButton skyType;
    public WToggleButton generateTrees;
    public WToggleButton generateVegetation;
    public WButton save;
    public WButton cancel;
    public WTextField presetEntry;
    public List<WButton> presetButtons = new ArrayList<>();

    public WBlockDropdown boundaryBlockADropdown;
    public WBlockDropdown boundaryBlockBDropdown;

    public WButton boundaryChunkXMinus;
    public WButton boundaryChunkXPlus;
    public WButton boundaryChunkZMinus;
    public WButton boundaryChunkZPlus;
    public WTextField boundaryChunkXField;
    public WTextField boundaryChunkZField;

    public WButton gapWidthMinus;
    public WButton gapWidthPlus;

    public WTextField gapWidthField;
    public WButton gapPresetButton;

    public WBlockDropdown gapBlockADropdown;
    public WBlockDropdown gapBlockBDropdown;
    public WBlockDropdown gapBlockCDropdown;

    public WToggleButton centerEnabledToggle;
    public WButton centerDirectionButton;
    public WBlockDropdown centerBlockDropdown;

    public int currentPage = 0;
    public Widget page1Container;
    public Widget page2Container;
    public WButton moreSettingsButton;
    public WButton presetScrollLeft;
    public WButton presetScrollRight;
    public int presetScrollOffset = 0;
    public static final int MAX_VISIBLE_PRESETS = 5;
    public WButton backToMainButton;
    public WPreviewPanel previewPanel;

    public Widget presetEditor;
    public Widget rootWidget = new Widget();
    public String voidPresetName = "gui.personalWorld.voidWorld";

    public WBlockDropdown layersBlockDropdown;
    public int layerListScrollOffset = 0;
    public boolean layerScrollbarDragging = false;
    public static final int MAX_VISIBLE_LAYERS = 6;

    public List<AllowedBlock> allowedBoundaryRules = new ArrayList<>();
    public List<String> allowedBoundaryBlockNames = new ArrayList<>();

    public List<AllowedBlock> allowedGapRules = new ArrayList<>();
    public List<String> allowedGapBlockNames = new ArrayList<>();

    public List<AllowedBlock> allowedCenterRules = new ArrayList<>();
    public List<String> allowedCenterBlockNames = new ArrayList<>();

    public List<AllowedBlock> allowedLayerRules = new ArrayList<>();

    public List<WBlockDropdown.BlockEntry> boundaryBlockEntries = new ArrayList<>();
    public List<WBlockDropdown.BlockEntry> gapBlockEntries = new ArrayList<>();
    public List<WBlockDropdown.BlockEntry> centerBlockEntries = new ArrayList<>();
    public List<WBlockDropdown.BlockEntry> layerBlockEntries = new ArrayList<>();

    public GuiEditWorld(PortalTileEntity tile) {
        super();
        this.tile = tile;
        int targetDimId = 0;
        DimensionConfig currentDimConfig = DimensionConfig
                .getForDimension(tile.getWorldObj().provider.dimensionId, true);
        if (currentDimConfig != null) {
            this.desiredConfig.copyFrom(currentDimConfig, false, true, true);
        } else if (tile.active && tile.targetDimId > 1) {
            DimensionConfig currentConfig = CommonProxy.getDimensionConfigObjects(true).get(tile.targetDimId);
            if (currentConfig != null) {
                this.desiredConfig.copyFrom(currentConfig, false, true, true);
            } else {
                this.desiredConfig.setAllowGenerationChanges(true);
            }
        } else {
            this.desiredConfig.setAllowGenerationChanges(true);
        }

        reloadBoundaryRules();

        this.desiredConfig.setBoundaryMetaA(
                clampBoundaryMeta(this.desiredConfig.getBoundaryMetaA(), this.desiredConfig.getBoundaryBlockA()));
        this.desiredConfig.setBoundaryMetaB(
                clampBoundaryMeta(this.desiredConfig.getBoundaryMetaB(), this.desiredConfig.getBoundaryBlockB()));
        this.desiredConfig
                .setBoundaryChunkIntervalX(clampBoundaryChunk(this.desiredConfig.getBoundaryChunkIntervalX()));
        this.desiredConfig
                .setBoundaryChunkIntervalZ(clampBoundaryChunk(this.desiredConfig.getBoundaryChunkIntervalZ()));

        this.desiredConfig.setGapWidth(clampGapWidth(this.desiredConfig.getGapWidth()));
        this.desiredConfig
                .setGapMetaA(clampGapMeta(this.desiredConfig.getGapMetaA(), this.desiredConfig.getGapBlockA()));
        this.desiredConfig
                .setGapMetaB(clampGapMeta(this.desiredConfig.getGapMetaB(), this.desiredConfig.getGapBlockB()));
        this.desiredConfig
                .setGapMetaC(clampGapMeta(this.desiredConfig.getGapMetaC(), this.desiredConfig.getGapBlockC()));
        this.desiredConfig.setCenterMeta(
                clampCenterMeta(this.desiredConfig.getCenterMeta(), this.desiredConfig.getCenterBlock()));
    }

    private int clampGapWidth(int v) {
        return MathHelper.clamp_int(v, 0, 5);
    }

    private void reloadBoundaryRules() {
        this.allowedBoundaryRules = AllowedBlockRules.parseAll(PersonalSpaceMod.clientAllowedBoundaryBlocks);
        this.allowedBoundaryBlockNames = AllowedBlockRules.extractBlockNames(this.allowedBoundaryRules);
        if (this.allowedBoundaryBlockNames.isEmpty()) {
            this.allowedBoundaryBlockNames = new ArrayList<>();
            this.allowedBoundaryBlockNames.add("");
        }

        this.allowedGapRules = AllowedBlockRules.parseAll(PersonalSpaceMod.clientAllowedGapBlocks);
        this.allowedGapBlockNames = AllowedBlockRules.extractBlockNames(this.allowedGapRules);
        if (this.allowedGapBlockNames.isEmpty()) {
            this.allowedGapBlockNames = new ArrayList<>();
            this.allowedGapBlockNames.add("");
        }

        this.allowedCenterRules = AllowedBlockRules.parseAll(PersonalSpaceMod.clientAllowedCenterBlocks);
        this.allowedCenterBlockNames = AllowedBlockRules.extractBlockNames(this.allowedCenterRules);
        if (this.allowedCenterBlockNames.isEmpty()) {
            this.allowedCenterBlockNames = new ArrayList<>();
            this.allowedCenterBlockNames.add("");
        }

        this.allowedLayerRules = AllowedBlockRules.parseAll(PersonalSpaceMod.clientAllowedBlocks);

        // Build dropdown entries
        this.boundaryBlockEntries = WBlockDropdown.buildEntriesFromRules(this.allowedBoundaryRules, true);
        this.gapBlockEntries = WBlockDropdown.buildEntriesFromRules(this.allowedGapRules, true);
        this.centerBlockEntries = WBlockDropdown.buildEntriesFromRules(this.allowedCenterRules, true);
        this.layerBlockEntries = WBlockDropdown.buildEntriesFromRules(this.allowedLayerRules, false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        rootWidget.update();
        WBlockDropdown.updateOpenDropdown();
        if (!this.mc.thePlayer.isEntityAlive() || this.mc.thePlayer.isDead) {
            this.mc.thePlayer.closeScreen();
        }
    }

    private void addButton(GuiButton btn) {
        this.buttonList.add(btn);
        this.ySize += btn.height + 6;
    }

    private void addWidget(Widget w) {
        this.rootWidget.addChild(w);
        this.ySize += w.position.height + 1;
    }

    private void updateSkyTypeButton() {
        DimensionConfig.SkyType currentType = desiredConfig.getSkyType();
        skyType.text = currentType.getButtonText();
        skyType.tooltip = currentType.getButtonTooltip();
    }

    private AllowedBlock getBoundaryRule(String blockName) {
        return AllowedBlockRules.findByBlockName(this.allowedBoundaryRules, blockName);
    }

    private AllowedBlock getGapRule(String blockName) {
        return AllowedBlockRules.findByBlockName(this.allowedGapRules, blockName);
    }

    private int clampGapMeta(int meta, String blockName) {
        if (blockName == null || blockName.isEmpty()) {
            return 0;
        }
        AllowedBlock rule = getGapRule(blockName);
        if (rule == null) {
            return 0;
        }
        return rule.clampMeta(meta);
    }

    private AllowedBlock getCenterRule(String blockName) {
        return AllowedBlockRules.findByBlockName(this.allowedCenterRules, blockName);
    }

    private int clampCenterMeta(int meta, String blockName) {
        if (blockName == null || blockName.isEmpty()) {
            return 0;
        }
        AllowedBlock rule = getCenterRule(blockName);
        if (rule == null) {
            return 0;
        }
        return rule.clampMeta(meta);
    }

    private String getCenterDirectionText() {
        DimensionConfig.CenterDirection dir = desiredConfig.getCenterDirection();
        return I18n.format("gui.personalWorld.center.dir." + dir.name());
    }

    private void updateCenterButtons() {
        String block = desiredConfig.getCenterBlock();
        int meta = clampCenterMeta(desiredConfig.getCenterMeta(), block);
        desiredConfig.setCenterMeta(meta);

        boolean centerVisible = desiredConfig.isCenterEnabled();

        if (centerDirectionButton != null) {
            centerDirectionButton.visible = centerVisible;
            centerDirectionButton.text = getCenterDirectionText();
        }
        if (centerBlockDropdown != null) {
            centerBlockDropdown.visible = centerVisible;
            centerBlockDropdown.setSelectedIndex(WBlockDropdown.findEntryIndex(centerBlockEntries, block, meta));
        }
    }

    private int clampBoundaryMeta(int meta, String blockName) {
        if (blockName == null || blockName.isEmpty()) {
            return 0;
        }
        AllowedBlock rule = getBoundaryRule(blockName);
        if (rule == null) {
            return 0;
        }
        return rule.clampMeta(meta);
    }

    private int clampBoundaryChunk(int v) {
        return MathHelper.clamp_int(v, 0, 20);
    }

    private void updateBoundaryButtons() {
        String a = desiredConfig.getBoundaryBlockA();
        String b = desiredConfig.getBoundaryBlockB();

        int metaA = clampBoundaryMeta(desiredConfig.getBoundaryMetaA(), a);
        int metaB = clampBoundaryMeta(desiredConfig.getBoundaryMetaB(), b);
        desiredConfig.setBoundaryMetaA(metaA);
        desiredConfig.setBoundaryMetaB(metaB);

        if (boundaryBlockADropdown != null) {
            boundaryBlockADropdown.setSelectedIndex(WBlockDropdown.findEntryIndex(boundaryBlockEntries, a, metaA));
        }

        if (boundaryBlockBDropdown != null) {
            boundaryBlockBDropdown.setSelectedIndex(WBlockDropdown.findEntryIndex(boundaryBlockEntries, b, metaB));
        }
    }

    private String getGapPresetText() {
        DimensionConfig.GapPreset preset = desiredConfig.getGapPreset();
        return I18n.format("gui.personalWorld.gap.preset." + preset.name());
    }

    private void updateGapButtons() {
        String a = desiredConfig.getGapBlockA();
        String b = desiredConfig.getGapBlockB();
        String c = desiredConfig.getGapBlockC();

        int metaA = clampGapMeta(desiredConfig.getGapMetaA(), a);
        int metaB = clampGapMeta(desiredConfig.getGapMetaB(), b);
        int metaC = clampGapMeta(desiredConfig.getGapMetaC(), c);
        desiredConfig.setGapMetaA(metaA);
        desiredConfig.setGapMetaB(metaB);
        desiredConfig.setGapMetaC(metaC);

        if (gapBlockADropdown != null) {
            gapBlockADropdown.setSelectedIndex(WBlockDropdown.findEntryIndex(gapBlockEntries, a, metaA));
        }

        boolean isSolid = desiredConfig.getGapPreset() == DimensionConfig.GapPreset.SOLID;

        if (gapBlockBDropdown != null) {
            gapBlockBDropdown.visible = !isSolid;
            gapBlockBDropdown.setSelectedIndex(WBlockDropdown.findEntryIndex(gapBlockEntries, b, metaB));
        }

        if (gapBlockCDropdown != null) {
            gapBlockCDropdown.visible = !isSolid;
            gapBlockCDropdown.setSelectedIndex(WBlockDropdown.findEntryIndex(gapBlockEntries, c, metaC));
        }

        if (gapPresetButton != null) {
            gapPresetButton.text = getGapPresetText();
        }
    }

    private int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private boolean isTextFieldFocused(WTextField field) {
        return field != null && field.textField != null && field.textField.isFocused();
    }

    private void syncExtendedInputsFromDesiredConfig() {
        if (!isTextFieldFocused(this.boundaryChunkXField)) {
            this.boundaryChunkXField.textField
                    .setText(Integer.toString(this.desiredConfig.getBoundaryChunkIntervalX()));
        }
        if (!isTextFieldFocused(this.boundaryChunkZField)) {
            this.boundaryChunkZField.textField
                    .setText(Integer.toString(this.desiredConfig.getBoundaryChunkIntervalZ()));
        }
        if (!isTextFieldFocused(this.gapWidthField)) {
            this.gapWidthField.textField.setText(Integer.toString(this.desiredConfig.getGapWidth()));
        }
    }

    private void syncPresetFromExtendedInputsIfChanged(int prevBoundaryX, int prevBoundaryZ, int prevGapWidth) {
        if (isTextFieldFocused(this.presetEntry)) {
            return;
        }
        if (prevBoundaryX != this.desiredConfig.getBoundaryChunkIntervalX()
                || prevBoundaryZ != this.desiredConfig.getBoundaryChunkIntervalZ()
                || prevGapWidth != this.desiredConfig.getGapWidth()) {
            this.configToPreset();
        }
    }

    private void updatePresetButtonPositions() {
        int total = presetButtons.size();
        int maxOffset = Math.max(0, total - MAX_VISIBLE_PRESETS);
        presetScrollOffset = MathHelper.clamp_int(presetScrollOffset, 0, maxOffset);
        boolean needsScroll = total > MAX_VISIBLE_PRESETS;

        int px = 0;
        for (int i = 0; i < total; i++) {
            WButton btn = presetButtons.get(i);
            boolean vis = i >= presetScrollOffset && i < presetScrollOffset + MAX_VISIBLE_PRESETS;
            btn.visible = vis;
            if (vis) {
                btn.position = new Rectangle(px, btn.position.y, 24, 18);
                px += 26;
            }
        }

        // Position scroll buttons
        if (needsScroll) {
            presetScrollLeft.visible = true;
            presetScrollLeft.position = new Rectangle(px, presetScrollLeft.position.y, 12, 18);
            presetScrollLeft.enabled = presetScrollOffset > 0;
            px += 14;
            presetScrollRight.visible = true;
            presetScrollRight.position = new Rectangle(px, presetScrollRight.position.y, 12, 18);
            presetScrollRight.enabled = presetScrollOffset < maxOffset;
            px += 14;
        } else {
            presetScrollLeft.visible = false;
            presetScrollRight.visible = false;
        }

        // "More Settings" right edge aligned with Cancel button (130+128=258)
        int moreW = 80;
        int moreX = 258 - moreW;
        moreSettingsButton.position = new Rectangle(moreX, moreSettingsButton.position.y, moreW, 18);
    }

    private void switchPage(int page) {
        this.currentPage = page;
        if (page1Container != null) page1Container.visible = (page == 0);
        if (page2Container != null) page2Container.visible = (page == 1);
        if (save != null) save.visible = (page == 0);
        if (cancel != null) cancel.visible = (page == 0);
    }

    @Override
    public void initGui() {
        reloadBoundaryRules();

        // Create page containers
        Widget realRoot = new Widget();
        this.page1Container = new Widget();
        this.page1Container.position = new Rectangle(0, 0, 1, 1);
        this.page2Container = new Widget();
        this.page2Container.position = new Rectangle(0, 0, 1, 1);

        // Build page 1 widgets
        this.rootWidget = page1Container;
        this.ySize = 0;
        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.skyColor"), false));
        this.skyRed = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                I18n.format("gui.personalWorld.skyColor.red") + "%.0f",
                0.0,
                255.0,
                ((desiredConfig.getSkyColor() >> 16) & 0xFF),
                1.0,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(skyRed);
        this.skyGreen = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                I18n.format("gui.personalWorld.skyColor.green") + "%.0f",
                0.0,
                255.0,
                ((desiredConfig.getSkyColor() >> 8) & 0xFF),
                1.0,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(skyGreen);
        this.skyBlue = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                I18n.format("gui.personalWorld.skyColor.blue") + "%.0f",
                0.0,
                255.0,
                ((desiredConfig.getSkyColor()) & 0xFF),
                1.0,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(skyBlue);

        this.ySize += 4;

        this.enableDaylightCycle = new WCycleButton(
                new Rectangle(130, this.ySize, 18, 18),
                "",
                false,
                0,
                Arrays.asList(
                        new WCycleButton.ButtonState(DimensionConfig.DaylightCycle.SUN, Icons.SUN),
                        new WCycleButton.ButtonState(DimensionConfig.DaylightCycle.MOON, Icons.MOON),
                        new WCycleButton.ButtonState(DimensionConfig.DaylightCycle.CYCLE, Icons.SUN_MOON)),
                desiredConfig.getDaylightCycle().ordinal(),
                () -> desiredConfig.setDaylightCycle(enableDaylightCycle.getState()));
        this.rootWidget.addChild(this.enableDaylightCycle);
        this.skyType = new WButton(
                new Rectangle(150, this.ySize, 18, 18),
                "?",
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    final int skyTypes = DimensionConfig.SkyType.values().length;
                    int skyType = (desiredConfig.getSkyType().ordinal() + 1) % skyTypes;
                    while (!DimensionConfig.SkyType.fromOrdinal(skyType).isLoaded()) {
                        skyType = (skyType + 1) % skyTypes;
                    }
                    desiredConfig.setSkyType(DimensionConfig.SkyType.fromOrdinal(skyType));
                    updateSkyTypeButton();
                });
        this.rootWidget.addChild(this.skyType);
        updateSkyTypeButton();

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.starBrightness"), false));
        this.starBrightness = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                "%.2f",
                0.0,
                1.0,
                desiredConfig.getStarBrightness(),
                0.01,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(starBrightness);

        this.ySize += 4;
        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.biome"), false));
        this.biome = new WTextField(new Rectangle(0, this.ySize, 142, 18), desiredConfig.getBiomeId());
        this.biomeEditButton = new WButton(new Rectangle(144, 0, 18, 18), "", false, 0, Icons.PENCIL, () -> {
            this.biomeCycle = (this.biomeEditButton.lastButton == 0) ? (this.biomeCycle + 1)
                    : (this.biomeCycle + PersonalSpaceMod.clientAllowedBiomes.size() - 1);
            this.biomeCycle = this.biomeCycle % PersonalSpaceMod.clientAllowedBiomes.size();
            this.biome.textField.setText(PersonalSpaceMod.clientAllowedBiomes.get(this.biomeCycle));
        });
        this.biome.addChild(biomeEditButton);
        addWidget(this.biome);
        this.ySize += 4;
        this.generateTrees = new WToggleButton(
                new Rectangle(0, this.ySize, 18, 18),
                "",
                false,
                0,
                desiredConfig.isGeneratingTrees(),
                () -> desiredConfig.setGeneratingTrees(generateTrees.getValue()));
        this.generateTrees.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.trees"), false));
        addWidget(generateTrees);
        this.enableWeather = new WToggleButton(
                new Rectangle(90, this.generateTrees.position.y, 18, 18),
                "",
                false,
                0,
                desiredConfig.isWeatherEnabled(),
                () -> desiredConfig.setWeatherEnabled(enableWeather.getValue()));
        this.enableWeather.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.weather"), false));
        rootWidget.addChild(this.enableWeather);

        this.generateVegetation = new WToggleButton(
                new Rectangle(0, this.ySize, 18, 18),
                "",
                false,
                0,
                desiredConfig.isGeneratingVegetation(),
                () -> desiredConfig.setGeneratingVegetation(generateVegetation.getValue()));
        this.generateVegetation.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.vegetation"), false));
        addWidget(generateVegetation);

        this.enableClouds = new WToggleButton(
                new Rectangle(90, this.generateVegetation.position.y, 18, 18),
                "",
                false,
                0,
                desiredConfig.isCloudsEnabled(),
                () -> desiredConfig.setCloudsEnabled(enableClouds.getValue()));
        this.enableClouds.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.clouds"), false));
        rootWidget.addChild(this.enableClouds);

        voidPresetName = I18n.format("gui.personalWorld.voidWorld");

        this.ySize += 6;
        this.presetEntry = new WTextField(new Rectangle(0, this.ySize, 168, 20), desiredConfig.getFullPresetString());
        if (this.presetEntry.textField.getText().isEmpty()) {
            this.presetEntry.textField.setText(voidPresetName);
        }
        addWidget(presetEntry);
        this.ySize += 2;

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.presets"), false));

        // Build all preset buttons but only show MAX_VISIBLE_PRESETS at a time
        int pi = 1;
        for (String preset : Config.defaultPresets) {
            if (preset.isEmpty()) {
                preset = voidPresetName;
            }
            String finalPreset = preset;
            WButton btn = new WButton(
                    new Rectangle(0, this.ySize, 24, 18),
                    Integer.toString(pi),
                    true,
                    WButton.DEFAULT_COLOR,
                    null,
                    () -> this.presetEntry.textField.setText(finalPreset));
            presetButtons.add(btn);
            rootWidget.addChild(btn);
            ++pi;
        }

        // Scroll left/right buttons for presets
        this.presetScrollLeft = new WButton(
                new Rectangle(0, this.ySize, 12, 18),
                "<",
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    if (presetScrollOffset > 0) presetScrollOffset--;
                    updatePresetButtonPositions();
                });
        rootWidget.addChild(this.presetScrollLeft);

        this.presetScrollRight = new WButton(
                new Rectangle(0, this.ySize, 12, 18),
                ">",
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int maxOffset = Math.max(0, presetButtons.size() - MAX_VISIBLE_PRESETS);
                    if (presetScrollOffset < maxOffset) presetScrollOffset++;
                    updatePresetButtonPositions();
                });
        rootWidget.addChild(this.presetScrollRight);

        // "More Settings" button - right edge aligned with skyType button (right edge = 168)
        this.moreSettingsButton = new WButton(
                new Rectangle(0, this.ySize, 80, 18),
                I18n.format("gui.personalWorld.moreSettings"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> switchPage(1));
        moreSettingsButton.tooltip = I18n.format("gui.personalWorld.moreSettings.tooltip");
        rootWidget.addChild(moreSettingsButton);

        updatePresetButtonPositions();
        this.ySize += 20;

        // Preset editor on page 1
        this.presetEditor = new Widget();
        this.presetEditor.position = new Rectangle(192, 0, 1, 1);
        this.rootWidget.addChild(this.presetEditor);

        // Layer block dropdown (ADD mode)
        this.layersBlockDropdown = new WBlockDropdown(
                new Rectangle(-20, 0, 20, 20),
                true,
                layerBlockEntries,
                0,
                (entry) -> {
                    String[] blName = entry.blockName().split(":");
                    if (blName.length != 2) return;
                    Block blk = GameRegistry.findBlock(blName[0], blName[1]);
                    if (blk == null) return;
                    FlatLayerInfo fli = new FlatLayerInfo(1, blk, entry.meta());
                    this.desiredConfig.getMutableLayers().add(fli);
                    this.desiredConfig.setLayers(this.desiredConfig.getLayersAsString());
                    this.configToPreset();
                });
        this.layersBlockDropdown.setLabel(I18n.format("gui.personalWorld.button.plus"));
        this.layersBlockDropdown.setGuiRelativePos(172, 0);
        this.presetEditor.addChild(this.layersBlockDropdown);

        regeneratePresetEditor();

        // Switch to Page 2
        this.rootWidget = page2Container;
        this.ySize = 0;

        // "Back to Main" button
        this.backToMainButton = new WButton(
                new Rectangle(0, this.ySize, 150, 18),
                I18n.format("gui.personalWorld.backToMain"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> switchPage(0));
        backToMainButton.tooltip = I18n.format("gui.personalWorld.backToMain.tooltip");
        rootWidget.addChild(backToMainButton);
        this.ySize += 20;

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.boundary.chunks"), false));

        this.boundaryChunkXMinus = new WButton(
                new Rectangle(0, this.ySize, 18, 18),
                I18n.format("gui.personalWorld.button.minus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = clampBoundaryChunk(
                            parseIntOrDefault(
                                    boundaryChunkXField.textField.getText(),
                                    desiredConfig.getBoundaryChunkIntervalX()) - 1);
                    desiredConfig.setBoundaryChunkIntervalX(v);
                    boundaryChunkXField.textField.setText(Integer.toString(v));
                    configToPreset();
                });
        rootWidget.addChild(this.boundaryChunkXMinus);

        this.boundaryChunkXField = new WTextField(
                new Rectangle(20, this.ySize, 32, 18),
                Integer.toString(desiredConfig.getBoundaryChunkIntervalX()));
        rootWidget.addChild(this.boundaryChunkXField);

        this.boundaryChunkXPlus = new WButton(
                new Rectangle(54, this.ySize, 18, 18),
                I18n.format("gui.personalWorld.button.plus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = clampBoundaryChunk(
                            parseIntOrDefault(
                                    boundaryChunkXField.textField.getText(),
                                    desiredConfig.getBoundaryChunkIntervalX()) + 1);
                    desiredConfig.setBoundaryChunkIntervalX(v);
                    boundaryChunkXField.textField.setText(Integer.toString(v));
                    configToPreset();
                });
        rootWidget.addChild(this.boundaryChunkXPlus);

        this.rootWidget.addChild(new WLabel(78, this.ySize + 4, I18n.format("gui.personalWorld.multiply"), false));

        this.boundaryChunkZMinus = new WButton(
                new Rectangle(90, this.ySize, 18, 18),
                I18n.format("gui.personalWorld.button.minus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = clampBoundaryChunk(
                            parseIntOrDefault(
                                    boundaryChunkZField.textField.getText(),
                                    desiredConfig.getBoundaryChunkIntervalZ()) - 1);
                    desiredConfig.setBoundaryChunkIntervalZ(v);
                    boundaryChunkZField.textField.setText(Integer.toString(v));
                    configToPreset();
                });
        rootWidget.addChild(this.boundaryChunkZMinus);

        this.boundaryChunkZField = new WTextField(
                new Rectangle(110, this.ySize, 32, 18),
                Integer.toString(desiredConfig.getBoundaryChunkIntervalZ()));
        rootWidget.addChild(this.boundaryChunkZField);

        this.boundaryChunkZPlus = new WButton(
                new Rectangle(144, this.ySize, 18, 18),
                I18n.format("gui.personalWorld.button.plus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = clampBoundaryChunk(
                            parseIntOrDefault(
                                    boundaryChunkZField.textField.getText(),
                                    desiredConfig.getBoundaryChunkIntervalZ()) + 1);
                    desiredConfig.setBoundaryChunkIntervalZ(v);
                    boundaryChunkZField.textField.setText(Integer.toString(v));
                    configToPreset();
                });
        rootWidget.addChild(this.boundaryChunkZPlus);

        this.ySize += 21;

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.boundary"), false));

        int boundaryRowY = this.ySize;
        this.boundaryBlockADropdown = new WBlockDropdown(
                new Rectangle(0, boundaryRowY, 20, 20),
                false,
                boundaryBlockEntries,
                WBlockDropdown.findEntryIndex(
                        boundaryBlockEntries,
                        desiredConfig.getBoundaryBlockA(),
                        desiredConfig.getBoundaryMetaA()),
                (entry) -> {
                    desiredConfig.setBoundaryBlockA(entry.blockName());
                    desiredConfig.setBoundaryMetaA(entry.meta());
                    updateBoundaryButtons();
                    configToPreset();
                });
        this.boundaryBlockADropdown.setLabel(I18n.format("gui.personalWorld.boundary.a.short"));
        this.boundaryBlockADropdown.setGuiRelativePos(0, boundaryRowY);
        rootWidget.addChild(this.boundaryBlockADropdown);

        this.boundaryBlockBDropdown = new WBlockDropdown(
                new Rectangle(42, boundaryRowY, 20, 20),
                false,
                boundaryBlockEntries,
                WBlockDropdown.findEntryIndex(
                        boundaryBlockEntries,
                        desiredConfig.getBoundaryBlockB(),
                        desiredConfig.getBoundaryMetaB()),
                (entry) -> {
                    desiredConfig.setBoundaryBlockB(entry.blockName());
                    desiredConfig.setBoundaryMetaB(entry.meta());
                    updateBoundaryButtons();
                    configToPreset();
                });
        this.boundaryBlockBDropdown.setLabel(I18n.format("gui.personalWorld.boundary.b.short"));
        this.boundaryBlockBDropdown.setGuiRelativePos(42, boundaryRowY);
        rootWidget.addChild(this.boundaryBlockBDropdown);

        this.ySize += 24;

        updateBoundaryButtons();

        // Gap section
        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.gap"), false));

        this.gapWidthMinus = new WButton(
                new Rectangle(0, this.ySize, 18, 18),
                I18n.format("gui.personalWorld.button.minus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = clampGapWidth(
                            parseIntOrDefault(gapWidthField.textField.getText(), desiredConfig.getGapWidth()) - 1);
                    desiredConfig.setGapWidth(v);
                    gapWidthField.textField.setText(Integer.toString(v));
                    configToPreset();
                });
        rootWidget.addChild(this.gapWidthMinus);

        this.gapWidthField = new WTextField(
                new Rectangle(20, this.ySize, 32, 18),
                Integer.toString(desiredConfig.getGapWidth()));
        rootWidget.addChild(this.gapWidthField);

        this.gapWidthPlus = new WButton(
                new Rectangle(54, this.ySize, 18, 18),
                I18n.format("gui.personalWorld.button.plus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = clampGapWidth(
                            parseIntOrDefault(gapWidthField.textField.getText(), desiredConfig.getGapWidth()) + 1);
                    desiredConfig.setGapWidth(v);
                    gapWidthField.textField.setText(Integer.toString(v));
                    configToPreset();
                });
        rootWidget.addChild(this.gapWidthPlus);

        this.gapPresetButton = new WButton(
                new Rectangle(76, this.ySize, 74, 18),
                getGapPresetText(),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int next = (desiredConfig.getGapPreset().ordinal() + 1) % DimensionConfig.GapPreset.values().length;
                    desiredConfig.setGapPreset(DimensionConfig.GapPreset.fromOrdinal(next));
                    gapPresetButton.text = getGapPresetText();
                    updateGapButtons();
                    configToPreset();
                });
        rootWidget.addChild(this.gapPresetButton);

        this.ySize += 21;

        int gapRowY = this.ySize;
        this.gapBlockADropdown = new WBlockDropdown(
                new Rectangle(0, gapRowY, 20, 20),
                false,
                gapBlockEntries,
                WBlockDropdown
                        .findEntryIndex(gapBlockEntries, desiredConfig.getGapBlockA(), desiredConfig.getGapMetaA()),
                (entry) -> {
                    desiredConfig.setGapBlockA(entry.blockName());
                    desiredConfig.setGapMetaA(entry.meta());
                    updateGapButtons();
                    configToPreset();
                });
        this.gapBlockADropdown.setLabel(I18n.format("gui.personalWorld.gap.a.short"));
        this.gapBlockADropdown.setGuiRelativePos(0, gapRowY);
        rootWidget.addChild(this.gapBlockADropdown);

        this.gapBlockBDropdown = new WBlockDropdown(
                new Rectangle(42, gapRowY, 20, 20),
                false,
                gapBlockEntries,
                WBlockDropdown
                        .findEntryIndex(gapBlockEntries, desiredConfig.getGapBlockB(), desiredConfig.getGapMetaB()),
                (entry) -> {
                    desiredConfig.setGapBlockB(entry.blockName());
                    desiredConfig.setGapMetaB(entry.meta());
                    updateGapButtons();
                    configToPreset();
                });
        this.gapBlockBDropdown.setLabel(I18n.format("gui.personalWorld.gap.b.short"));
        this.gapBlockBDropdown.setGuiRelativePos(42, gapRowY);
        rootWidget.addChild(this.gapBlockBDropdown);

        this.gapBlockCDropdown = new WBlockDropdown(
                new Rectangle(84, gapRowY, 20, 20),
                false,
                gapBlockEntries,
                WBlockDropdown
                        .findEntryIndex(gapBlockEntries, desiredConfig.getGapBlockC(), desiredConfig.getGapMetaC()),
                (entry) -> {
                    desiredConfig.setGapBlockC(entry.blockName());
                    desiredConfig.setGapMetaC(entry.meta());
                    updateGapButtons();
                    configToPreset();
                });
        this.gapBlockCDropdown.setLabel(I18n.format("gui.personalWorld.gap.c.short"));
        this.gapBlockCDropdown.setGuiRelativePos(84, gapRowY);
        rootWidget.addChild(this.gapBlockCDropdown);

        this.ySize += 24;
        updateGapButtons();

        // Center marker section
        this.centerEnabledToggle = new WToggleButton(
                new Rectangle(0, this.ySize, 18, 18),
                "",
                false,
                0,
                desiredConfig.isCenterEnabled(),
                () -> {
                    desiredConfig.setCenterEnabled(centerEnabledToggle.getValue());
                    updateCenterButtons();
                    configToPreset();
                });
        this.centerEnabledToggle.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.center.enable"), false));
        addWidget(this.centerEnabledToggle);

        this.centerBlockDropdown = new WBlockDropdown(
                new Rectangle(0, this.ySize, 20, 20),
                false,
                centerBlockEntries,
                WBlockDropdown.findEntryIndex(
                        centerBlockEntries,
                        desiredConfig.getCenterBlock(),
                        desiredConfig.getCenterMeta()),
                (entry) -> {
                    desiredConfig.setCenterBlock(entry.blockName());
                    desiredConfig.setCenterMeta(entry.meta());
                    updateCenterButtons();
                    configToPreset();
                });
        this.centerBlockDropdown.setLabel(I18n.format("gui.personalWorld.center.block.short"));
        this.centerBlockDropdown.setGuiRelativePos(0, this.ySize);
        addWidget(this.centerBlockDropdown);

        this.centerDirectionButton = new WButton(
                new Rectangle(42, this.centerBlockDropdown.position.y, 80, 18),
                getCenterDirectionText(),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int next = (desiredConfig.getCenterDirection().ordinal() + 1)
                            % DimensionConfig.CenterDirection.values().length;
                    desiredConfig.setCenterDirection(DimensionConfig.CenterDirection.fromOrdinal(next));
                    updateCenterButtons();
                    configToPreset();
                });
        rootWidget.addChild(this.centerDirectionButton);

        this.ySize += 2;
        updateCenterButtons();

        // Preview panel on page 2
        this.previewPanel = new WPreviewPanel(new Rectangle(166, 20, 130, 130), desiredConfig);
        rootWidget.addChild(this.previewPanel);

        // Restore real root
        this.rootWidget = realRoot;
        realRoot.addChild(page1Container);
        realRoot.addChild(page2Container);

        // Save/Cancel always visible at fixed position
        int saveY = 244 - 16 - 22 + 5;
        this.save = new WButton(
                new Rectangle(0, saveY, 128, 20),
                I18n.format("gui.done"),
                true,
                WButton.DEFAULT_COLOR,
                Icons.CHECKMARK,
                () -> {
                    Packets.INSTANCE.sendChangeWorldSettings(this.tile, desiredConfig).sendToServer();
                    Minecraft.getMinecraft().displayGuiScreen(null);
                });
        rootWidget.addChild(save);
        this.cancel = new WButton(
                new Rectangle(130, saveY, 128, 20),
                I18n.format("gui.cancel"),
                true,
                WButton.DEFAULT_COLOR,
                Icons.CROSS,
                () -> Minecraft.getMinecraft().displayGuiScreen(null));
        rootWidget.addChild(cancel);

        switchPage(0);

        this.xSize = 320 - 16;
        this.ySize = 244 - 16;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    private void regeneratePresetEditor() {
        final boolean generationEnabled = desiredConfig.getAllowGenerationChanges();
        // Remove all children except the layers dropdown
        List<Widget> toKeep = new ArrayList<>();
        for (Widget child : this.presetEditor.children) {
            if (child == this.layersBlockDropdown) {
                toKeep.add(child);
            }
        }
        this.presetEditor.children.clear();
        this.presetEditor.children.addAll(toKeep);

        if (this.layersBlockDropdown != null) {
            this.layersBlockDropdown.enabled = generationEnabled;
        }

        int curX = 24;
        int curY = 0;
        this.presetEditor.addChild(new WLabel(curX, curY, I18n.format("gui.personalWorld.layers"), false));
        curY += 10;

        List<FlatLayerInfo> fli = this.desiredConfig.getLayers();
        int startIdx = layerListScrollOffset;
        int endIdx = Math.min(fli.size(), startIdx + MAX_VISIBLE_LAYERS);

        for (int vi = startIdx; vi < endIdx; vi++) {
            // Display layers top-to-bottom (highest to lowest in the stack)
            int i = fli.size() - 1 - vi;
            if (i < 0) break;
            FlatLayerInfo info = fli.get(i);
            final int finalI = i;
            WButton block = new WButton(new Rectangle(curX + 12, curY, 20, 28), "", false, 0, null, null);
            Block gameBlock = info.func_151536_b();
            int blockMeta = info.getFillBlockMeta();
            block.enabled = false;
            block.itemStack = new ItemStack(gameBlock, 1, blockMeta);
            block.itemStackText = Integer.toString(info.getLayerCount());
            try {
                String displayName = block.itemStack.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    block.tooltip = displayName;
                } else {
                    block.tooltip = gameBlock.getLocalizedName();
                }
            } catch (Throwable ignored) {
                block.tooltip = gameBlock.getLocalizedName();
            }
            this.presetEditor.addChild(block);

            if (i < fli.size() - 1) {
                block.addChild(new WButton(new Rectangle(-12, 0, 10, 10), "", false, 0, Icons.SMALL_UP, () -> {
                    Collections.swap(this.desiredConfig.getMutableLayers(), finalI, finalI + 1);
                    this.configToPreset();
                }));
            }
            block.addChild(new WButton(new Rectangle(-12, 9, 10, 10), "", false, 0, Icons.SMALL_CROSS, () -> {
                this.desiredConfig.getMutableLayers().remove(finalI);
                this.configToPreset();
            }));
            if (i > 0) {
                block.addChild(new WButton(new Rectangle(-12, 18, 10, 10), "", false, 0, Icons.SMALL_DOWN, () -> {
                    Collections.swap(this.desiredConfig.getMutableLayers(), finalI, finalI - 1);
                    this.configToPreset();
                }));
            }
            IntConsumer plusMinus = (mul) -> {
                FlatLayerInfo orig = this.desiredConfig.getMutableLayers().get(finalI);
                boolean shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
                boolean ctrlHeld = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);
                int newCnt = ctrlHeld ? 64 : (shiftHeld ? 10 : 1);
                newCnt *= mul;
                newCnt = MathHelper.clamp_int(orig.getLayerCount() + newCnt, 1, 255);
                this.desiredConfig.getMutableLayers()
                        .set(finalI, new FlatLayerInfo(newCnt, orig.func_151536_b(), orig.getFillBlockMeta()));
                this.desiredConfig.setLayers(this.desiredConfig.getLayersAsString());
                this.configToPreset();
            };
            block.addChild(
                    new WButton(
                            new Rectangle(21, 5, 18, 18),
                            "",
                            false,
                            0,
                            generationEnabled ? Icons.PLUS : Icons.LOCK,
                            () -> plusMinus.accept(1)));
            block.addChild(
                    new WButton(
                            new Rectangle(40, 5, 18, 18),
                            "",
                            false,
                            0,
                            generationEnabled ? Icons.MINUS : Icons.LOCK,
                            () -> plusMinus.accept(-1)));

            for (Widget child : block.children) {
                child.enabled = generationEnabled;
            }

            curY += 30;
        }

        // Clamp scroll offset
        if (fli.size() <= MAX_VISIBLE_LAYERS) {
            layerListScrollOffset = 0;
        } else {
            layerListScrollOffset = MathHelper.clamp_int(layerListScrollOffset, 0, fli.size() - MAX_VISIBLE_LAYERS);
        }
    }

    private void configToPreset() {
        String preset = this.desiredConfig.getFullPresetString();
        if (preset == null || preset.isEmpty()) {
            preset = voidPresetName;
        }
        if (!preset.equals(this.presetEntry.textField.getText())) {
            this.presetEntry.textField.setText(preset);
            this.presetEntry.textField.setCursorPositionZero();
        }
    }

    private void updateLayerScrollFromMouse(int mouseY) {
        List<FlatLayerInfo> layers = desiredConfig.getLayers();
        if (layers.size() <= MAX_VISIBLE_LAYERS) return;
        int trackY = 10;
        int trackH = MAX_VISIBLE_LAYERS * 30;
        int maxScroll = layers.size() - MAX_VISIBLE_LAYERS;
        int thumbH = Math.max(20, trackH * MAX_VISIBLE_LAYERS / layers.size());
        int scrollableRange = trackH - thumbH;
        if (scrollableRange <= 0) return;
        int relY = mouseY - trackY - thumbH / 2;
        float ratio = MathHelper.clamp_float((float) relY / scrollableRange, 0, 1);
        layerListScrollOffset = Math.round(ratio * maxScroll);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        boolean inputsValid = true;
        this.drawDefaultBackground();
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(this.guiLeft, this.guiTop, 0.0f);
        mouseX -= guiLeft;
        mouseY -= guiTop;
        Icons.bindTexture();
        Icons.GUI_BG.draw9Patch(-8, -8, xSize + 16, ySize + 16);

        int skyR = MathHelper.clamp_int(skyRed.getValueInt(), 0, 255);
        int skyG = MathHelper.clamp_int(skyGreen.getValueInt(), 0, 255);
        int skyB = MathHelper.clamp_int(skyBlue.getValueInt(), 0, 255);
        desiredConfig.setSkyColor((skyR << 16) | (skyG << 8) | skyB);
        desiredConfig.setStarBrightness((float) this.starBrightness.getValue());
        boolean generationEnabled = desiredConfig.getAllowGenerationChanges();
        this.generateTrees.enabled = generationEnabled;
        this.generateVegetation.enabled = generationEnabled;
        for (WButton presetBtn : presetButtons) {
            presetBtn.enabled = generationEnabled;
        }
        if (presetScrollLeft != null) {
            presetScrollLeft.enabled = generationEnabled && presetScrollOffset > 0;
        }
        if (presetScrollRight != null) {
            int maxOffset = Math.max(0, presetButtons.size() - MAX_VISIBLE_PRESETS);
            presetScrollRight.enabled = generationEnabled && presetScrollOffset < maxOffset;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.biome.enabled = generationEnabled;
        this.biomeEditButton.enabled = generationEnabled;
        this.biomeEditButton.buttonIcon = generationEnabled ? Icons.PENCIL : Icons.LOCK;
        this.presetEntry.enabled = generationEnabled;

        this.boundaryBlockADropdown.enabled = generationEnabled;
        this.boundaryBlockBDropdown.enabled = generationEnabled;

        this.boundaryChunkXMinus.enabled = generationEnabled;
        this.boundaryChunkXPlus.enabled = generationEnabled;
        this.boundaryChunkZMinus.enabled = generationEnabled;
        this.boundaryChunkZPlus.enabled = generationEnabled;
        this.boundaryChunkXField.enabled = generationEnabled;
        this.boundaryChunkZField.enabled = generationEnabled;

        this.gapWidthMinus.enabled = generationEnabled;
        this.gapWidthPlus.enabled = generationEnabled;
        this.gapWidthField.enabled = generationEnabled;
        this.gapPresetButton.enabled = generationEnabled;
        boolean gapIsSolid = desiredConfig.getGapPreset() == DimensionConfig.GapPreset.SOLID;
        boolean boundaryIsZero = desiredConfig.getBoundaryChunkIntervalX() == 0
                && desiredConfig.getBoundaryChunkIntervalZ() == 0;
        boolean gapEnabled = generationEnabled && !boundaryIsZero;
        this.gapWidthMinus.enabled = gapEnabled;
        this.gapWidthPlus.enabled = gapEnabled;
        this.gapWidthField.enabled = gapEnabled;
        this.gapPresetButton.enabled = gapEnabled;
        this.gapBlockADropdown.enabled = gapEnabled && !gapIsSolid;
        this.gapBlockBDropdown.enabled = gapEnabled && !gapIsSolid;
        this.gapBlockCDropdown.enabled = gapEnabled && !gapIsSolid;

        boolean centerCanEnable = generationEnabled && !boundaryIsZero;
        this.centerEnabledToggle.enabled = centerCanEnable;
        boolean centerActive = centerCanEnable && desiredConfig.isCenterEnabled();
        this.centerDirectionButton.enabled = centerActive;
        this.centerBlockDropdown.enabled = centerActive;

        String actualText = this.presetEntry.textField.getText();
        if (voidPresetName.equals(actualText)) {
            actualText = "";
        }
        String layersPart = DimensionConfig.extractLayersPart(actualText);
        if (!generationEnabled) {
            this.presetEntry.textField.setTextColor(0x909090);
        } else if (!DimensionConfig.PRESET_VALIDATION_PATTERN.matcher(layersPart).matches()) {
            this.presetEntry.textField.setTextColor(0xFF0000);
            this.presetEntry.tooltip = I18n.format("gui.personalWorld.invalidSyntax");
            inputsValid = false;
        } else if (!DimensionConfig.canUseLayers(layersPart, true)) {
            this.presetEntry.textField.setTextColor(0xFFFF00);
            this.presetEntry.tooltip = I18n.format("gui.personalWorld.notAllowed");
            inputsValid = false;
        } else {
            this.presetEntry.textField.setTextColor(0xA0FFA0);
            this.presetEntry.tooltip = null;
            this.desiredConfig.setLayers(layersPart);
            if (DimensionConfig.hasExtendedSettings(actualText)) {
                this.desiredConfig.applyExtendedSettings(actualText);
                // Only overwrite UI fields that the user is not actively editing.
                this.syncExtendedInputsFromDesiredConfig();
                updateBoundaryButtons();
                updateGapButtons();
                updateCenterButtons();
            }
            this.regeneratePresetEditor();
        }

        int prevBoundaryChunkX = this.desiredConfig.getBoundaryChunkIntervalX();
        int prevBoundaryChunkZ = this.desiredConfig.getBoundaryChunkIntervalZ();
        int prevGapWidth = this.desiredConfig.getGapWidth();

        this.desiredConfig.setBiomeId(this.biome.textField.getText());
        if (!generationEnabled) {
            this.biome.textField.setTextColor(0x909090);
        } else if (BiomeGenBase.getBiome(this.desiredConfig.getRawBiomeId()) == null || !this.desiredConfig.getBiomeId()
                .equalsIgnoreCase(BiomeGenBase.getBiome(this.desiredConfig.getRawBiomeId()).biomeName)) {
                    this.biome.textField.setTextColor(0xFF0000);
                    this.biome.tooltip = I18n.format("gui.personalWorld.invalidSyntax");
                    inputsValid = false;
                } else
            if (!DimensionConfig.canUseBiome(this.desiredConfig.getBiomeId(), true)) {
                this.biome.textField.setTextColor(0xFFFF00);
                this.biome.tooltip = I18n.format("gui.personalWorld.notAllowed");
                inputsValid = false;
            } else {
                this.biome.textField.setTextColor(0xA0FFA0);
                this.biome.tooltip = null;
            }

        if (generationEnabled) {
            int bx = parseIntOrDefault(this.boundaryChunkXField.textField.getText(), 0);
            boolean bxOk = bx >= 0 && bx <= 20;
            if (!bxOk) {
                this.boundaryChunkXField.textField.setTextColor(0xFF0000);
                this.boundaryChunkXField.tooltip = I18n.format("gui.personalWorld.boundary.range");
                inputsValid = false;
            } else {
                this.boundaryChunkXField.textField.setTextColor(0xA0FFA0);
                this.boundaryChunkXField.tooltip = I18n.format("gui.personalWorld.boundary.range");
                this.desiredConfig.setBoundaryChunkIntervalX(bx);
            }

            int bz = parseIntOrDefault(this.boundaryChunkZField.textField.getText(), 0);
            boolean bzOk = bz >= 0 && bz <= 20;
            if (!bzOk) {
                this.boundaryChunkZField.textField.setTextColor(0xFF0000);
                this.boundaryChunkZField.tooltip = I18n.format("gui.personalWorld.boundary.range");
                inputsValid = false;
            } else {
                this.boundaryChunkZField.textField.setTextColor(0xA0FFA0);
                this.boundaryChunkZField.tooltip = I18n.format("gui.personalWorld.boundary.range");
                this.desiredConfig.setBoundaryChunkIntervalZ(bz);
            }

            if (!desiredConfig.getBoundaryBlockA().isEmpty()
                    && getBoundaryRule(desiredConfig.getBoundaryBlockA()) == null) {
                inputsValid = false;
            }

            if (!desiredConfig.getBoundaryBlockB().isEmpty()
                    && getBoundaryRule(desiredConfig.getBoundaryBlockB()) == null) {
                inputsValid = false;
            }

            updateBoundaryButtons();

            // Gap validation - skip when boundary is 0x0
            boolean gapBoundaryZero = desiredConfig.getBoundaryChunkIntervalX() == 0
                    && desiredConfig.getBoundaryChunkIntervalZ() == 0;
            if (!gapBoundaryZero) {
                int gw = parseIntOrDefault(this.gapWidthField.textField.getText(), 0);
                boolean gwOk = gw >= 0 && gw <= 5;
                if (!gwOk) {
                    this.gapWidthField.textField.setTextColor(0xFF0000);
                    this.gapWidthField.tooltip = I18n.format("gui.personalWorld.gap.widthRange");
                    inputsValid = false;
                } else {
                    this.gapWidthField.textField.setTextColor(0xA0FFA0);
                    this.gapWidthField.tooltip = I18n.format("gui.personalWorld.gap.widthRange");
                    this.desiredConfig.setGapWidth(gw);
                }

                if (!desiredConfig.getGapBlockA().isEmpty() && getGapRule(desiredConfig.getGapBlockA()) == null) {
                    inputsValid = false;
                }

                if (!gapIsSolid && !desiredConfig.getGapBlockB().isEmpty()
                        && getGapRule(desiredConfig.getGapBlockB()) == null) {
                    inputsValid = false;
                }

                updateGapButtons();
            } else {
                // Boundary is 0x0, gray out gap fields
                this.gapWidthField.textField.setTextColor(0x909090);
                updateGapButtons();
            }

            // Center validation
            if (!boundaryIsZero && desiredConfig.isCenterEnabled()) {
                if (!desiredConfig.getCenterBlock().isEmpty()
                        && getCenterRule(desiredConfig.getCenterBlock()) == null) {
                    inputsValid = false;
                }

                updateCenterButtons();
            } else {
                updateCenterButtons();
            }

            this.syncPresetFromExtendedInputsIfChanged(prevBoundaryChunkX, prevBoundaryChunkZ, prevGapWidth);
        } else {
            this.boundaryChunkXField.textField.setTextColor(0x909090);
            this.boundaryChunkZField.textField.setTextColor(0x909090);
            this.gapWidthField.textField.setTextColor(0x909090);
        }

        this.save.enabled = inputsValid;

        rootWidget.draw(mouseX, mouseY, partialTicks);

        if (currentPage == 0) {
            GuiDraw.gui.setZLevel(0.f);
            // Draw layer scrollbar
            List<FlatLayerInfo> layers = desiredConfig.getLayers();
            if (layers.size() > MAX_VISIBLE_LAYERS) {
                int trackX = 288;
                int trackY = 10;
                int trackW = 6;
                int trackH = MAX_VISIBLE_LAYERS * 30;
                int maxScroll = layers.size() - MAX_VISIBLE_LAYERS;
                int thumbH = Math.max(20, trackH * MAX_VISIBLE_LAYERS / layers.size());
                int thumbY = trackY + (int) ((float) (trackH - thumbH) * layerListScrollOffset / maxScroll);
                GuiDraw.drawRect(trackX, trackY, trackW, trackH, 0xFF404040);
                GuiDraw.drawRect(trackX, thumbY, trackW, thumbH, 0xFFA0A0A0);
            }
            GuiDraw.drawRect(130, skyRed.position.y, 32, 3 * (skyRed.position.height + 1), 0xFF000000);
            GuiDraw.drawRect(
                    131,
                    skyRed.position.y + 1,
                    30,
                    3 * (skyRed.position.height + 1) - 2,
                    0xFF000000 | desiredConfig.getSkyColor());
            Icons.bindTexture();
            GL11.glColor4f(1, 1, 1, desiredConfig.getStarBrightness());
            Icons.STAR.drawAt(132, this.skyRed.position.y + 2);
            Icons.STAR.drawAt(145, this.skyRed.position.y + 12);
            Icons.STAR.drawAt(134, this.skyRed.position.y + 21);
            GL11.glColor4f(1, 1, 1, 1);
        }

        // Draw open dropdown overlay on top
        WBlockDropdown.setScreenDimensions(this.width, this.height, this.guiLeft, this.guiTop);
        WBlockDropdown.drawOpenDropdown(mouseX, mouseY, partialTicks);

        rootWidget.drawForeground(mouseX, mouseY, partialTicks);

        GL11.glPopMatrix();
    }

    @Override
    protected void keyTyped(char character, int key) {
        // If dropdown is open, forward key events to its search box
        if (WBlockDropdown.isAnyDropdownOpen()) {
            if (key == Keyboard.KEY_ESCAPE) {
                WBlockDropdown.closeDropdown();
                return;
            }
            WBlockDropdown.handleOpenDropdownKeyTyped(character, key);
            return;
        }
        super.keyTyped(character, key);
        rootWidget.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        x -= guiLeft;
        y -= guiTop;
        // Handle dropdown click first (overlay is on top)
        if (WBlockDropdown.handleOpenDropdownClick(x, y, button)) {
            return;
        }
        // Handle layer scrollbar click
        if (currentPage == 0 && button == 0) {
            List<FlatLayerInfo> layers = desiredConfig.getLayers();
            if (layers.size() > MAX_VISIBLE_LAYERS) {
                int trackX = 288, trackY = 10, trackW = 6, trackH = MAX_VISIBLE_LAYERS * 30;
                if (x >= trackX && x < trackX + trackW && y >= trackY && y < trackY + trackH) {
                    layerScrollbarDragging = true;
                    updateLayerScrollFromMouse(y);
                    return;
                }
            }
        }
        super.mouseClicked(x, y, button);
        rootWidget.mouseClicked(x, y, button);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        x -= guiLeft;
        y -= guiTop;
        WBlockDropdown.handleOpenDropdownMouseUp();
        if (button >= 0) {
            layerScrollbarDragging = false;
        }
        super.mouseMovedOrUp(x, y, button);
        rootWidget.mouseMovedOrUp(x, y, button);
    }

    @Override
    protected void mouseClickMove(int x, int y, int lastBtn, long timeDragged) {
        x -= guiLeft;
        y -= guiTop;
        if (WBlockDropdown.handleOpenDropdownDrag(x, y)) {
            return;
        }
        if (layerScrollbarDragging) {
            updateLayerScrollFromMouse(y);
            return;
        }
        super.mouseClickMove(x, y, lastBtn, timeDragged);
        rootWidget.mouseClickMove(x, y, lastBtn, timeDragged);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            int mx = Mouse.getEventX() * this.width / this.mc.displayWidth - guiLeft;
            int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1 - guiTop;
            // Try dropdown scroll first
            if (!WBlockDropdown.handleOpenDropdownScroll(mx, my, delta)) {
                // Handle preset scroll on page 0
                if (currentPage == 0 && !presetButtons.isEmpty() && presetButtons.size() > MAX_VISIBLE_PRESETS) {
                    int presetY = presetButtons.get(0).position.y;
                    if (my >= presetY && my < presetY + 18 && mx >= 0 && mx < 168) {
                        int maxOffset = Math.max(0, presetButtons.size() - MAX_VISIBLE_PRESETS);
                        if (delta > 0) {
                            presetScrollOffset = Math.max(0, presetScrollOffset - 1);
                        } else {
                            presetScrollOffset = Math.min(maxOffset, presetScrollOffset + 1);
                        }
                        updatePresetButtonPositions();
                    }
                }
                // Handle layer list scroll on page 1 (preset editor area)
                if (currentPage == 0 && presetEditor != null) {
                    int peX = presetEditor.position.x;
                    int peY = presetEditor.position.y;
                    if (mx >= peX && mx < peX + 200 && my >= peY && my < peY + 220) {
                        int maxScroll = Math.max(0, desiredConfig.getLayers().size() - MAX_VISIBLE_LAYERS);
                        if (delta > 0) {
                            layerListScrollOffset = Math.max(0, layerListScrollOffset - 1);
                        } else {
                            layerListScrollOffset = Math.min(maxScroll, layerListScrollOffset + 1);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {}

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
