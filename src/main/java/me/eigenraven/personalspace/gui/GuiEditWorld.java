package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
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
import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import cpw.mods.fml.common.registry.GameRegistry;
import me.eigenraven.personalspace.CommonProxy;
import me.eigenraven.personalspace.Config;
import me.eigenraven.personalspace.PersonalSpaceMod;
import me.eigenraven.personalspace.block.PortalTileEntity;
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
    Widget presetEditor;
    Widget rootWidget = new Widget();
    String voidPresetName = "gui.personalWorld.voidWorld";

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

    @Override
    public void initGui() {
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
                new ArrayList<>() {

                    {
                        add(new WCycleButton.ButtonState(DimensionConfig.DaylightCycle.SUN, Icons.SUN));
                        add(new WCycleButton.ButtonState(DimensionConfig.DaylightCycle.MOON, Icons.MOON));
                        add(new WCycleButton.ButtonState(DimensionConfig.DaylightCycle.CYCLE, Icons.SUN_MOON));
                    }
                },
                0,
                () -> desiredConfig.setDaylightCycle(enableDaylightCycle.getState()));
        this.rootWidget.addChild(this.enableDaylightCycle);
        // this.enableNightTime = new WToggleButton(
        // new Rectangle(130, this.ySize, 18, 18),
        // "",
        // false,
        // 0,
        // desiredConfig.isNightTime(),
        // () -> desiredConfig.setNightTime(enableNightTime.value));
        // this.enableNightTime.noIcon = Icons.SUN;
        // this.enableNightTime.yesIcon = Icons.MOON;
        // this.enableNightTime.setValue(this.enableNightTime.value);
        // this.rootWidget.addChild(this.enableNightTime);
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

        int px = 8, pi = 1;
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
        this.ySize = 240 - 16;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    private void regeneratePresetEditor() {
        final boolean generationEnabled = desiredConfig.getAllowGenerationChanges();
        this.presetEditor.children.clear();
        // Palette
        int curX = 0;
        int curY = 0;
        for (String bl : PersonalSpaceMod.clientAllowedBlocks) {
            String[] blName = bl.split(":");
            if (blName.length != 2) continue;
            Block block = GameRegistry.findBlock(blName[0], blName[1]);
            ItemStack is = new ItemStack(block);
            WButton addBtn = new WButton(new Rectangle(curX, curY, 20, 20), "", false, 0, null, () -> {
                FlatLayerInfo fli = new FlatLayerInfo(1, block);
                this.desiredConfig.getMutableLayers().add(fli);
                this.desiredConfig.setLayers(this.desiredConfig.getLayersAsString());
                this.configToPreset();
            });
            addBtn.itemStack = is;
            addBtn.itemStackText = "+";
            addBtn.tooltip = (is.getItem() != null) ? is.getDisplayName() : block.getLocalizedName();
            addBtn.enabled = generationEnabled;
            this.presetEditor.addChild(addBtn);
            curY += 21;
            if (curY > 188) {
                curY = 0;
                curX += 21;
            }
        }
        // Layers
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

            // up
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
        } else if (!this.desiredConfig.getBiomeId()
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
