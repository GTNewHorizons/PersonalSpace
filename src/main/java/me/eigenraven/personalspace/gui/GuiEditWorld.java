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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.FlatLayerInfo;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import cpw.mods.fml.common.registry.GameRegistry;
import me.eigenraven.personalspace.CommonProxy;
import me.eigenraven.personalspace.PersonalSpaceMod;
import me.eigenraven.personalspace.block.PortalTileEntity;
import me.eigenraven.personalspace.config.AllowedBoundaryBlock;
import me.eigenraven.personalspace.config.BoundaryBlockRules;
import me.eigenraven.personalspace.config.Config;
import me.eigenraven.personalspace.net.Packets;
import me.eigenraven.personalspace.world.DimensionConfig;

public class GuiEditWorld extends GuiScreen {

    public PortalTileEntity tile;

    int xSize, ySize, guiLeft, guiTop;

    DimensionConfig desiredConfig = new DimensionConfig();
    WSlider skyRed, skyGreen, skyBlue;
    WSlider starBrightness;
    WTextField biome;
    int biomeCycle = 0;
    WButton biomeEditButton;
    WToggleButton enableWeather;
    WCycleButton enableDaylightCycle;
    WToggleButton enableClouds;
    WButton skyType;
    WToggleButton generateTrees;
    WToggleButton generateVegetation;
    WButton save;
    WTextField presetEntry;
    List<WButton> presetButtons = new ArrayList<>();

    WButton boundaryBlockAButton;
    WButton boundaryBlockBButton;

    WButton boundaryMetaAMinus;
    WButton boundaryMetaAPlus;
    WButton boundaryMetaBMinus;
    WButton boundaryMetaBPlus;

    WTextField boundaryMetaAField;
    WTextField boundaryMetaBField;

    WButton boundaryChunkXMinus;
    WButton boundaryChunkXPlus;
    WButton boundaryChunkZMinus;
    WButton boundaryChunkZPlus;
    WTextField boundaryChunkXField;
    WTextField boundaryChunkZField;

    int boundaryBlockACycle = 0;
    int boundaryBlockBCycle = 0;

    Widget presetEditor;
    Widget rootWidget = new Widget();
    String voidPresetName = "gui.personalWorld.voidWorld";

    private List<AllowedBoundaryBlock> allowedBoundaryRules = new ArrayList<>();
    private List<String> allowedBoundaryBlockNames = new ArrayList<>();

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
    }

    private void reloadBoundaryRules() {
        this.allowedBoundaryRules = BoundaryBlockRules.parseAll(PersonalSpaceMod.clientAllowedBoundaryBlocks);
        this.allowedBoundaryBlockNames = BoundaryBlockRules.extractBlockNames(this.allowedBoundaryRules);
        if (this.allowedBoundaryBlockNames.isEmpty()) {
            this.allowedBoundaryBlockNames = new ArrayList<>();
            this.allowedBoundaryBlockNames.add("");
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        rootWidget.update();
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

    private AllowedBoundaryBlock getBoundaryRule(String blockName) {
        return BoundaryBlockRules.findByBlockName(this.allowedBoundaryRules, blockName);
    }

    private String getBoundaryButtonText(String s) {
        if (s == null || s.isEmpty()) {
            return I18n.format("gui.personalWorld.boundary.none.short");
        }
        String[] sp = s.split(":");
        return sp.length == 2 ? sp[1].substring(0, Math.min(1, sp[1].length())).toUpperCase()
                : I18n.format("gui.personalWorld.boundary.unknown.short");
    }

    private Block getBoundaryBlock(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return DimensionConfig.blockFromString(s);
    }

    private ItemStack getBoundaryPreviewStack(String s, int meta) {
        Block b = getBoundaryBlock(s);
        if (b == null) {
            return null;
        }
        try {
            Item item = Item.getItemFromBlock(b);
            if (item == null) {
                return null;
            }
            return new ItemStack(item, 1, clampBoundaryMeta(meta, s));
        } catch (Throwable t) {
            return null;
        }
    }

    private int getBoundaryMetaMin(String blockName) {
        AllowedBoundaryBlock rule = getBoundaryRule(blockName);
        return rule != null ? rule.getMinAllowedMeta() : 0;
    }

    private int getBoundaryMetaMax(String blockName) {
        AllowedBoundaryBlock rule = getBoundaryRule(blockName);
        return rule != null ? rule.getMaxAllowedMeta() : 0;
    }

    private boolean isBoundaryMetaAllowed(String blockName, int meta) {
        if (blockName == null || blockName.isEmpty()) {
            return meta == 0;
        }
        AllowedBoundaryBlock rule = getBoundaryRule(blockName);
        return rule != null && rule.isMetaAllowed(meta);
    }

    private int clampBoundaryMeta(int meta, String blockName) {
        if (blockName == null || blockName.isEmpty()) {
            return 0;
        }
        AllowedBoundaryBlock rule = getBoundaryRule(blockName);
        if (rule == null) {
            return 0;
        }
        return rule.clampMeta(meta);
    }

    private int clampBoundaryChunk(int v) {
        return MathHelper.clamp_int(v, 0, 20);
    }

    private String getBoundaryButtonTooltip(String s, int meta, String labelKey) {
        if (s == null || s.isEmpty()) {
            return I18n.format(labelKey) + ": "
                    + I18n.format("gui.personalWorld.boundary.none")
                    + ", "
                    + I18n.format("gui.personalWorld.boundary.metaValue", meta);
        }
        AllowedBoundaryBlock rule = getBoundaryRule(s);
        Block b = getBoundaryBlock(s);
        ItemStack is = getBoundaryPreviewStack(s, meta);
        String dn = (is != null && is.getItem() != null) ? is.getDisplayName() : s;
        String rangeText = rule != null ? rule.getMetaDescription() : I18n.format("gui.personalWorld.boundary.none");
        if (b == null) {
            return I18n.format(labelKey) + ": "
                    + s
                    + ", "
                    + I18n.format("gui.personalWorld.boundary.metaValue", meta)
                    + ", "
                    + I18n.format("gui.personalWorld.boundary.allowedValue", rangeText);
        }
        return I18n.format(labelKey) + ": "
                + dn
                + " ("
                + s
                + "), "
                + I18n.format("gui.personalWorld.boundary.metaValue", meta)
                + ", "
                + I18n.format("gui.personalWorld.boundary.allowedValue", rangeText);
    }

    private void updateBoundaryButtons() {
        String a = desiredConfig.getBoundaryBlockA();
        String b = desiredConfig.getBoundaryBlockB();

        int metaA = clampBoundaryMeta(desiredConfig.getBoundaryMetaA(), a);
        int metaB = clampBoundaryMeta(desiredConfig.getBoundaryMetaB(), b);
        desiredConfig.setBoundaryMetaA(metaA);
        desiredConfig.setBoundaryMetaB(metaB);

        boundaryBlockAButton.text = getBoundaryButtonText(a);
        boundaryBlockAButton.tooltip = getBoundaryButtonTooltip(a, metaA, "gui.personalWorld.boundary.a");
        boundaryBlockAButton.itemStack = getBoundaryPreviewStack(a, metaA);

        boundaryBlockBButton.text = getBoundaryButtonText(b);
        boundaryBlockBButton.tooltip = getBoundaryButtonTooltip(b, metaB, "gui.personalWorld.boundary.b");
        boundaryBlockBButton.itemStack = getBoundaryPreviewStack(b, metaB);

        if (boundaryMetaAField != null && !boundaryMetaAField.textField.isFocused()) {
            boundaryMetaAField.textField.setText(Integer.toString(metaA));
        }
        if (boundaryMetaBField != null && !boundaryMetaBField.textField.isFocused()) {
            boundaryMetaBField.textField.setText(Integer.toString(metaB));
        }
    }

    private int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public void initGui() {
        reloadBoundaryRules();
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

        this.ySize += 2;
        this.presetEntry = new WTextField(new Rectangle(0, this.ySize, 160, 20), desiredConfig.getLayersAsString());
        if (this.presetEntry.textField.getText().isEmpty()) {
            this.presetEntry.textField.setText(voidPresetName);
        }
        addWidget(presetEntry);
        this.ySize += 2;

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.presets"), false));

        int px = 0, pi = 1;
        for (String preset : Config.defaultPresets) {
            if (preset.isEmpty()) {
                preset = voidPresetName;
            }
            String finalPreset = preset;
            presetButtons.add(
                    new WButton(
                            new Rectangle(px, this.ySize, 24, 18),
                            Integer.toString(pi),
                            true,
                            WButton.DEFAULT_COLOR,
                            null,
                            () -> this.presetEntry.textField.setText(finalPreset)));
            rootWidget.addChild(presetButtons.get(presetButtons.size() - 1));
            ++pi;
            px += 26;
        }
        this.ySize += 20;

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.boundary"), false));

        this.boundaryBlockAButton = new WButton(
                new Rectangle(0, this.ySize, 20, 20),
                "?",
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    boundaryBlockACycle = (boundaryBlockAButton.lastButton == 0) ? (boundaryBlockACycle + 1)
                            : (boundaryBlockACycle + allowedBoundaryBlockNames.size() - 1);
                    boundaryBlockACycle %= allowedBoundaryBlockNames.size();
                    String newBlock = allowedBoundaryBlockNames.get(boundaryBlockACycle);
                    desiredConfig.setBoundaryBlockA(newBlock);
                    desiredConfig.setBoundaryMetaA(clampBoundaryMeta(desiredConfig.getBoundaryMetaA(), newBlock));
                    updateBoundaryButtons();
                });
        this.boundaryBlockAButton.addChild(new WLabel(26, 6, I18n.format("gui.personalWorld.boundary.a.short"), false));
        addWidget(this.boundaryBlockAButton);

        this.boundaryMetaAMinus = new WButton(
                new Rectangle(40, this.boundaryBlockAButton.position.y + 1, 18, 18),
                I18n.format("gui.personalWorld.button.minus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = desiredConfig.getBoundaryMetaA() - 1;
                    v = clampBoundaryMeta(v, desiredConfig.getBoundaryBlockA());
                    desiredConfig.setBoundaryMetaA(v);
                    updateBoundaryButtons();
                });
        rootWidget.addChild(this.boundaryMetaAMinus);

        this.boundaryMetaAField = new WTextField(
                new Rectangle(60, this.boundaryBlockAButton.position.y + 1, 28, 18),
                Integer.toString(desiredConfig.getBoundaryMetaA()));
        rootWidget.addChild(this.boundaryMetaAField);

        this.boundaryMetaAPlus = new WButton(
                new Rectangle(90, this.boundaryBlockAButton.position.y + 1, 18, 18),
                I18n.format("gui.personalWorld.button.plus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = desiredConfig.getBoundaryMetaA() + 1;
                    v = clampBoundaryMeta(v, desiredConfig.getBoundaryBlockA());
                    desiredConfig.setBoundaryMetaA(v);
                    updateBoundaryButtons();
                });
        rootWidget.addChild(this.boundaryMetaAPlus);

        this.boundaryBlockBButton = new WButton(
                new Rectangle(120, this.boundaryBlockAButton.position.y, 20, 20),
                "?",
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    boundaryBlockBCycle = (boundaryBlockBButton.lastButton == 0) ? (boundaryBlockBCycle + 1)
                            : (boundaryBlockBCycle + allowedBoundaryBlockNames.size() - 1);
                    boundaryBlockBCycle %= allowedBoundaryBlockNames.size();
                    String newBlock = allowedBoundaryBlockNames.get(boundaryBlockBCycle);
                    desiredConfig.setBoundaryBlockB(newBlock);
                    desiredConfig.setBoundaryMetaB(clampBoundaryMeta(desiredConfig.getBoundaryMetaB(), newBlock));
                    updateBoundaryButtons();
                });
        this.boundaryBlockBButton.addChild(new WLabel(26, 6, I18n.format("gui.personalWorld.boundary.b.short"), false));
        rootWidget.addChild(this.boundaryBlockBButton);

        this.boundaryMetaBMinus = new WButton(
                new Rectangle(160, this.boundaryBlockAButton.position.y + 1, 18, 18),
                I18n.format("gui.personalWorld.button.minus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = desiredConfig.getBoundaryMetaB() - 1;
                    v = clampBoundaryMeta(v, desiredConfig.getBoundaryBlockB());
                    desiredConfig.setBoundaryMetaB(v);
                    updateBoundaryButtons();
                });
        rootWidget.addChild(this.boundaryMetaBMinus);

        this.boundaryMetaBField = new WTextField(
                new Rectangle(180, this.boundaryBlockAButton.position.y + 1, 28, 18),
                Integer.toString(desiredConfig.getBoundaryMetaB()));
        rootWidget.addChild(this.boundaryMetaBField);

        this.boundaryMetaBPlus = new WButton(
                new Rectangle(210, this.boundaryBlockAButton.position.y + 1, 18, 18),
                I18n.format("gui.personalWorld.button.plus"),
                true,
                WButton.DEFAULT_COLOR,
                null,
                () -> {
                    int v = desiredConfig.getBoundaryMetaB() + 1;
                    v = clampBoundaryMeta(v, desiredConfig.getBoundaryBlockB());
                    desiredConfig.setBoundaryMetaB(v);
                    updateBoundaryButtons();
                });
        rootWidget.addChild(this.boundaryMetaBPlus);

        this.ySize += 1;

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
                });
        rootWidget.addChild(this.boundaryChunkZPlus);

        this.ySize += 24;

        updateBoundaryButtons();

        this.save = new WButton(
                new Rectangle(0, ySize, 128, 20),
                I18n.format("gui.done"),
                true,
                WButton.DEFAULT_COLOR,
                Icons.CHECKMARK,
                () -> {
                    Packets.INSTANCE.sendChangeWorldSettings(this.tile, desiredConfig).sendToServer();
                    Minecraft.getMinecraft().displayGuiScreen(null);
                });
        rootWidget.addChild(
                new WButton(
                        new Rectangle(130, ySize, 128, 20),
                        I18n.format("gui.cancel"),
                        true,
                        WButton.DEFAULT_COLOR,
                        Icons.CROSS,
                        () -> Minecraft.getMinecraft().displayGuiScreen(null)));
        addWidget(save);

        this.presetEditor = new Widget();
        this.presetEditor.position = new Rectangle(172, 0, 1, 1);
        this.rootWidget.addChild(this.presetEditor);

        regeneratePresetEditor();

        this.xSize = 320 - 16;
        this.ySize = 310 - 16;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    private void regeneratePresetEditor() {
        final boolean generationEnabled = desiredConfig.getAllowGenerationChanges();
        this.presetEditor.children.clear();
        int curX = 0;
        int curY = 0;
        for (String bl : PersonalSpaceMod.clientAllowedBlocks) {
            String[] blName = bl.split(":");
            if (blName.length != 2) continue;
            Block block = GameRegistry.findBlock(blName[0], blName[1]);
            if (block == null) continue;
            ItemStack is = new ItemStack(block);
            WButton addBtn = new WButton(new Rectangle(curX, curY, 20, 20), "", false, 0, null, () -> {
                FlatLayerInfo fli = new FlatLayerInfo(1, block);
                this.desiredConfig.getMutableLayers().add(fli);
                this.desiredConfig.setLayers(this.desiredConfig.getLayersAsString());
                this.configToPreset();
            });
            addBtn.itemStack = is;
            addBtn.itemStackText = I18n.format("gui.personalWorld.button.plus");
            addBtn.tooltip = (is.getItem() != null) ? is.getDisplayName() : block.getLocalizedName();
            addBtn.enabled = generationEnabled;
            this.presetEditor.addChild(addBtn);
            curY += 21;
            if (curY > 188) {
                curY = 0;
                curX += 21;
            }
        }

        curY = 0;
        curX += 22;
        this.presetEditor.addChild(new WLabel(curX, curY, I18n.format("gui.personalWorld.layers"), false));
        curY += 10;
        List<FlatLayerInfo> fli = this.desiredConfig.getLayers();
        for (int i = fli.size() - 1; i >= 0; i--) {
            FlatLayerInfo info = fli.get(i);
            final int finalI = i;
            WButton block = new WButton(new Rectangle(curX + 12, curY, 20, 28), "", false, 0, null, null);
            Block gameBlock = info.func_151536_b();
            block.enabled = false;
            block.itemStack = new ItemStack(gameBlock);
            block.itemStackText = Integer.toString(info.getLayerCount());
            block.tooltip = gameBlock.getLocalizedName();
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
                this.desiredConfig.getMutableLayers().set(finalI, new FlatLayerInfo(newCnt, orig.func_151536_b()));
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
            if (curY > 188) {
                curY = 10;
                curX += 21;
            }
        }
    }

    private void configToPreset() {
        String preset = this.desiredConfig.getLayersAsString();
        if (preset == null || preset.isEmpty()) {
            preset = voidPresetName;
        }
        this.presetEntry.textField.setText(preset);
        this.presetEntry.textField.setCursorPositionZero();
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
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.biome.enabled = generationEnabled;
        this.biomeEditButton.enabled = generationEnabled;
        this.biomeEditButton.buttonIcon = generationEnabled ? Icons.PENCIL : Icons.LOCK;
        this.presetEntry.enabled = generationEnabled;

        this.boundaryBlockAButton.enabled = generationEnabled;
        this.boundaryBlockBButton.enabled = generationEnabled;
        this.boundaryMetaAMinus.enabled = generationEnabled;
        this.boundaryMetaAPlus.enabled = generationEnabled;
        this.boundaryMetaBMinus.enabled = generationEnabled;
        this.boundaryMetaBPlus.enabled = generationEnabled;
        this.boundaryMetaAField.enabled = generationEnabled;
        this.boundaryMetaBField.enabled = generationEnabled;

        this.boundaryChunkXMinus.enabled = generationEnabled;
        this.boundaryChunkXPlus.enabled = generationEnabled;
        this.boundaryChunkZMinus.enabled = generationEnabled;
        this.boundaryChunkZPlus.enabled = generationEnabled;
        this.boundaryChunkXField.enabled = generationEnabled;
        this.boundaryChunkZField.enabled = generationEnabled;

        String actualText = this.presetEntry.textField.getText();
        if (voidPresetName.equals(actualText)) {
            actualText = "";
        }
        if (!generationEnabled) {
            this.presetEntry.textField.setTextColor(0x909090);
        } else if (!DimensionConfig.PRESET_VALIDATION_PATTERN.matcher(actualText).matches()) {
            this.presetEntry.textField.setTextColor(0xFF0000);
            this.presetEntry.tooltip = I18n.format("gui.personalWorld.invalidSyntax");
            inputsValid = false;
        } else if (!DimensionConfig.canUseLayers(actualText, true)) {
            this.presetEntry.textField.setTextColor(0xFFFF00);
            this.presetEntry.tooltip = I18n.format("gui.personalWorld.notAllowed");
            inputsValid = false;
        } else {
            this.presetEntry.textField.setTextColor(0xA0FFA0);
            this.presetEntry.tooltip = null;
            this.desiredConfig.setLayers(actualText);
            this.regeneratePresetEditor();
        }

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
            int rawMetaA = parseIntOrDefault(boundaryMetaAField.textField.getText(), desiredConfig.getBoundaryMetaA());
            if (!isBoundaryMetaAllowed(desiredConfig.getBoundaryBlockA(), rawMetaA)) {
                this.boundaryMetaAField.textField.setTextColor(0xFF0000);
                AllowedBoundaryBlock rule = getBoundaryRule(desiredConfig.getBoundaryBlockA());
                this.boundaryMetaAField.tooltip = rule != null
                        ? I18n.format("gui.personalWorld.boundary.allowed", rule.getMetaDescription())
                        : I18n.format("gui.personalWorld.boundary.invalidMeta");
                inputsValid = false;
            } else {
                this.boundaryMetaAField.textField.setTextColor(0xA0FFA0);
                this.boundaryMetaAField.tooltip = null;
                desiredConfig.setBoundaryMetaA(rawMetaA);
            }

            int rawMetaB = parseIntOrDefault(boundaryMetaBField.textField.getText(), desiredConfig.getBoundaryMetaB());
            if (!isBoundaryMetaAllowed(desiredConfig.getBoundaryBlockB(), rawMetaB)) {
                this.boundaryMetaBField.textField.setTextColor(0xFF0000);
                AllowedBoundaryBlock rule = getBoundaryRule(desiredConfig.getBoundaryBlockB());
                this.boundaryMetaBField.tooltip = rule != null
                        ? I18n.format("gui.personalWorld.boundary.allowed", rule.getMetaDescription())
                        : I18n.format("gui.personalWorld.boundary.invalidMeta");
                inputsValid = false;
            } else {
                this.boundaryMetaBField.textField.setTextColor(0xA0FFA0);
                this.boundaryMetaBField.tooltip = null;
                desiredConfig.setBoundaryMetaB(rawMetaB);
            }

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
                this.boundaryBlockAButton.tooltip = I18n.format("gui.personalWorld.boundary.blockNotAllowed.a");
            }

            if (!desiredConfig.getBoundaryBlockB().isEmpty()
                    && getBoundaryRule(desiredConfig.getBoundaryBlockB()) == null) {
                inputsValid = false;
                this.boundaryBlockBButton.tooltip = I18n.format("gui.personalWorld.boundary.blockNotAllowed.b");
            }

            updateBoundaryButtons();
        } else {
            this.boundaryMetaAField.textField.setTextColor(0x909090);
            this.boundaryMetaBField.textField.setTextColor(0x909090);
            this.boundaryChunkXField.textField.setTextColor(0x909090);
            this.boundaryChunkZField.textField.setTextColor(0x909090);
        }

        this.save.enabled = inputsValid;

        rootWidget.draw(mouseX, mouseY, partialTicks);

        GuiDraw.gui.setZLevel(0.f);
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

        rootWidget.drawForeground(mouseX, mouseY, partialTicks);

        GL11.glPopMatrix();
    }

    @Override
    protected void keyTyped(char character, int key) {
        super.keyTyped(character, key);
        rootWidget.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        x -= guiLeft;
        y -= guiTop;
        super.mouseClicked(x, y, button);
        rootWidget.mouseClicked(x, y, button);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        x -= guiLeft;
        y -= guiTop;
        super.mouseMovedOrUp(x, y, button);
        rootWidget.mouseMovedOrUp(x, y, button);
    }

    @Override
    protected void mouseClickMove(int x, int y, int lastBtn, long timeDragged) {
        x -= guiLeft;
        y -= guiTop;
        super.mouseClickMove(x, y, lastBtn, timeDragged);
        rootWidget.mouseClickMove(x, y, lastBtn, timeDragged);
    }

    @Override
    protected void actionPerformed(GuiButton button) {}

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
