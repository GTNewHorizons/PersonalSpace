package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.FlatLayerInfo;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import me.eigenraven.personalspace.world.DimensionConfig;

public class WPreviewPanel extends Widget {

    private static final int TEX_SIZE = 128;
    private DimensionConfig config;
    private DynamicTexture texture;
    private ResourceLocation textureLocation;
    private int[] pixelData;
    private int lastHash = 0;

    public WPreviewPanel(Rectangle position, DimensionConfig config) {
        this.position = position;
        this.config = config;
        this.texture = new DynamicTexture(TEX_SIZE, TEX_SIZE);
        this.pixelData = texture.getTextureData();
        this.textureLocation = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("personalspace_preview", texture);
    }

    public void setConfig(DimensionConfig config) {
        this.config = config;
        this.lastHash = 0;
    }

    @Override
    protected void drawImpl(int mouseX, int mouseY, float partialTicks) {
        int hash = computeConfigHash();
        if (hash != lastHash) {
            lastHash = hash;
            updatePreview();
        }

        // Draw border
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GuiDraw.drawRect(-1, -1, TEX_SIZE + 2, TEX_SIZE + 2, 0xFF000000);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Draw texture
        GL11.glColor4f(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(0, TEX_SIZE, 0, 0, 1);
        tess.addVertexWithUV(TEX_SIZE, TEX_SIZE, 0, 1, 1);
        tess.addVertexWithUV(TEX_SIZE, 0, 0, 1, 0);
        tess.addVertexWithUV(0, 0, 0, 0, 0);
        tess.draw();
    }

    private int computeConfigHash() {
        return Objects.hash(
                config.getBoundaryBlockA(),
                config.getBoundaryMetaA(),
                config.getBoundaryBlockB(),
                config.getBoundaryMetaB(),
                config.getBoundaryChunkIntervalX(),
                config.getBoundaryChunkIntervalZ(),
                config.getGapWidth(),
                config.getGapPreset(),
                config.getGapBlockA(),
                config.getGapMetaA(),
                config.getGapBlockB(),
                config.getGapMetaB(),
                config.getGapBlockC(),
                config.getGapMetaC(),
                config.isCenterEnabled(),
                config.getCenterDirection(),
                config.getCenterBlock(),
                config.getCenterMeta(),
                config.getLayersAsString());
    }

    private void updatePreview() {
        int intervalX = config.getBoundaryChunkIntervalX();
        int intervalZ = config.getBoundaryChunkIntervalZ();
        int gapWidth = config.getGapWidth();

        int mainColor = getTopLayerColor();

        if (intervalX <= 0 && intervalZ <= 0) {
            Arrays.fill(pixelData, mainColor);
            texture.updateDynamicTexture();
            return;
        }

        int periodX = intervalX + gapWidth;
        int periodZ = intervalZ + gapWidth;
        int periodXBlocks = periodX * 16;
        int periodZBlocks = periodZ * 16;

        int boundaryAColor = getBlockColor(config.getBoundaryBlockA(), config.getBoundaryMetaA());
        int boundaryBColor = getBlockColor(config.getBoundaryBlockB(), config.getBoundaryMetaB());
        int gapAColor = getBlockColor(config.getGapBlockA(), config.getGapMetaA());
        int gapBColor = getBlockColor(config.getGapBlockB(), config.getGapMetaB());
        int gapCColor = getBlockColor(config.getGapBlockC(), config.getGapMetaC());
        int centerColorRaw = config.isCenterEnabled() ? getBlockColor(config.getCenterBlock(), config.getCenterMeta())
                : 0;
        int centerColor = centerColorRaw != 0 ? centerColorRaw : mainColor;

        // Calculate uniform scale: show ~2 periods of the larger axis, keep chunks square
        int maxPeriodBlocks = Math.max(periodXBlocks > 0 ? periodXBlocks : 16, periodZBlocks > 0 ? periodZBlocks : 16);
        float scale = (maxPeriodBlocks * 2.0f) / TEX_SIZE;
        if (scale < 1.0f) scale = 1.0f;

        for (int pz = 0; pz < TEX_SIZE; pz++) {
            for (int px = 0; px < TEX_SIZE; px++) {
                int worldX = (int) (px * scale);
                int worldZ = (int) (pz * scale);
                int color = computeBlockColor(
                        worldX,
                        worldZ,
                        intervalX,
                        intervalZ,
                        gapWidth,
                        periodX,
                        periodZ,
                        periodXBlocks,
                        periodZBlocks,
                        mainColor,
                        boundaryAColor,
                        boundaryBColor,
                        gapAColor,
                        gapBColor,
                        gapCColor,
                        centerColor);
                pixelData[pz * TEX_SIZE + px] = color;
            }
        }

        texture.updateDynamicTexture();
    }

    private int computeBlockColor(int worldX, int worldZ, int intervalX, int intervalZ, int gapWidth, int periodX,
            int periodZ, int periodXBlocks, int periodZBlocks, int mainColor, int bAColor, int bBColor, int gapAColor,
            int gapBColor, int gapCColor, int centerColor) {
        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;
        int localX = worldX & 15;
        int localZ = worldZ & 15;

        boolean isGapChunkX = gapWidth > 0 && intervalX > 0 && periodX > 0 && mod(chunkX, periodX) >= intervalX;
        boolean isGapChunkZ = gapWidth > 0 && intervalZ > 0 && periodZ > 0 && mod(chunkZ, periodZ) >= intervalZ;

        // Gap
        if (isGapChunkX || isGapChunkZ) {
            return computeGapColor(
                    chunkX,
                    chunkZ,
                    localX,
                    localZ,
                    worldX,
                    worldZ,
                    isGapChunkX,
                    isGapChunkZ,
                    periodX,
                    periodZ,
                    gapWidth,
                    intervalX,
                    intervalZ,
                    gapAColor,
                    gapBColor,
                    gapCColor);
        }

        // Boundary
        boolean isBoundaryX, prevBoundaryX, isBoundaryZ, prevBoundaryZ;
        if (gapWidth > 0) {
            isBoundaryX = intervalX > 0 && periodX > 0 && mod(chunkX, periodX) == 0;
            prevBoundaryX = intervalX > 0 && periodX > 0 && mod(chunkX, periodX) == intervalX - 1;
            isBoundaryZ = intervalZ > 0 && periodZ > 0 && mod(chunkZ, periodZ) == 0;
            prevBoundaryZ = intervalZ > 0 && periodZ > 0 && mod(chunkZ, periodZ) == intervalZ - 1;
        } else {
            isBoundaryX = intervalX > 0 && mod(chunkX, intervalX) == 0;
            isBoundaryZ = intervalZ > 0 && mod(chunkZ, intervalZ) == 0;
            prevBoundaryX = intervalX > 0 && mod(chunkX + 1, intervalX) == 0;
            prevBoundaryZ = intervalZ > 0 && mod(chunkZ + 1, intervalZ) == 0;
        }

        if ((isBoundaryX && localX == 0) || (prevBoundaryX && localX == 15)
                || (isBoundaryZ && localZ == 0)
                || (prevBoundaryZ && localZ == 15)) {
            boolean useA = ((worldX + worldZ) & 1) == 0;
            if (useA) {
                return bAColor != 0 ? bAColor : (bBColor != 0 ? bBColor : mainColor);
            } else {
                return bBColor != 0 ? bBColor : (bAColor != 0 ? bAColor : mainColor);
            }
        }

        // Center
        if (config.isCenterEnabled() && intervalX > 0 && intervalZ > 0 && periodX > 0 && periodZ > 0) {
            DimensionConfig.CenterDirection dir = config.getCenterDirection();
            int dirOffX = (dir == DimensionConfig.CenterDirection.SW || dir == DimensionConfig.CenterDirection.NW) ? -1
                    : 0;
            int dirOffZ = (dir == DimensionConfig.CenterDirection.NE || dir == DimensionConfig.CenterDirection.NW) ? -1
                    : 0;
            int centerLocalX = intervalX * 8 + dirOffX;
            int centerLocalZ = intervalZ * 8 + dirOffZ;

            int modCX = mod(chunkX, periodX);
            int modCZ = mod(chunkZ, periodZ);

            if (modCX < intervalX && modCZ < intervalZ) {
                int areaBlockX = modCX * 16 + localX;
                int areaBlockZ = modCZ * 16 + localZ;
                // Make center marker 3x3 for visibility at any scale
                int dx = areaBlockX - centerLocalX;
                int dz = areaBlockZ - centerLocalZ;
                if (dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1) {
                    return centerColor;
                }
            }
        }

        return mainColor;
    }

    private int computeGapColor(int chunkX, int chunkZ, int localX, int localZ, int worldX, int worldZ, boolean isGapX,
            boolean isGapZ, int periodX, int periodZ, int gapWidth, int intervalX, int intervalZ, int gapAColor,
            int gapBColor, int gapCColor) {
        DimensionConfig.GapPreset preset = config.getGapPreset();
        int gapWidthBlocks = gapWidth * 16;

        if (preset == DimensionConfig.GapPreset.ROAD) {
            boolean isIntersection = isGapX && isGapZ;
            boolean hasStripe = gapBColor != 0;

            if (isIntersection) {
                if (hasStripe) {
                    int gapOffsetX = mod(chunkX, periodX) - intervalX;
                    int offsetX = gapOffsetX * 16 + localX;
                    int gapOffsetZ = mod(chunkZ, periodZ) - intervalZ;
                    int offsetZ = gapOffsetZ * 16 + localZ;
                    boolean onEdgeX = offsetX == 0 || offsetX == gapWidthBlocks - 1;
                    boolean onEdgeZ = offsetZ == 0 || offsetZ == gapWidthBlocks - 1;
                    if (onEdgeX && onEdgeZ) {
                        return gapBColor;
                    }
                }
            } else {
                if (isGapX && periodX > 0) {
                    int gapChunkOffset = mod(chunkX, periodX) - intervalX;
                    int offsetInGap = gapChunkOffset * 16 + localX;
                    return computeRoadColor(offsetInGap, worldZ, gapWidthBlocks, gapAColor, gapBColor, gapCColor);
                } else if (isGapZ && periodZ > 0) {
                    int gapChunkOffset = mod(chunkZ, periodZ) - intervalZ;
                    int offsetInGap = gapChunkOffset * 16 + localZ;
                    return computeRoadColor(offsetInGap, worldX, gapWidthBlocks, gapAColor, gapBColor, gapCColor);
                }
            }
        }

        return gapAColor;
    }

    private int computeRoadColor(int offsetInGap, int alongRoad, int gapWidthBlocks, int gapAColor, int gapBColor,
            int gapCColor) {
        boolean hasStripe = gapBColor != 0;
        boolean hasDash = gapCColor != 0;

        // Edge lines (B block)
        if (hasStripe && (offsetInGap == 0 || offsetInGap == gapWidthBlocks - 1)) {
            return gapBColor;
        }
        // Center dashed line (C block)
        if (hasDash && gapWidthBlocks >= 4) {
            int center = gapWidthBlocks / 2;
            if ((offsetInGap == center || offsetInGap == center - 1) && mod(alongRoad, 8) < 4) {
                return gapCColor;
            }
        }
        return gapAColor;
    }

    private int getTopLayerColor() {
        List<FlatLayerInfo> layers = config.getLayers();
        if (layers == null || layers.isEmpty()) {
            return 0xFF202020;
        }
        FlatLayerInfo topLayer = layers.get(layers.size() - 1);
        Block block = topLayer.func_151536_b();
        if (block == null || block == Blocks.air) {
            return 0xFF202020;
        }
        try {
            return 0xFF000000 | block.getMapColor(0).colorValue;
        } catch (Exception e) {
            return 0xFF808080;
        }
    }

    private int getBlockColor(String blockName, int meta) {
        if (blockName == null || blockName.isEmpty()) {
            return 0;
        }
        Block block = DimensionConfig.blockFromString(blockName);
        if (block == null || block == Blocks.air) {
            return 0;
        }
        try {
            return 0xFF000000 | block.getMapColor(meta).colorValue;
        } catch (Exception e) {
            return 0xFF808080;
        }
    }

    private static int mod(int a, int b) {
        if (b <= 0) return 0;
        int m = a % b;
        return m < 0 ? m + b : m;
    }
}
