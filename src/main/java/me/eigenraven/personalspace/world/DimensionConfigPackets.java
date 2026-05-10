package me.eigenraven.personalspace.world;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.FlatLayerInfo;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;

public class DimensionConfigPackets {

    private DimensionConfigPackets() {}

    static void write(DimensionConfig config, MCDataOutput pkt) {
        pkt.writeString(config.getSaveDirOverrideForPacket());
        pkt.writeInt(config.getSkyColor());
        pkt.writeFloat(config.getStarBrightness());
        pkt.writeVarInt(config.getRawBiomeId());
        pkt.writeVarInt(config.getDaylightCycle().ordinal());
        pkt.writeBoolean(config.isCloudsEnabled());
        pkt.writeVarInt(config.getSkyType().ordinal());
        pkt.writeBoolean(config.isWeatherEnabled());
        pkt.writeBoolean(config.isGeneratingVegetation());
        pkt.writeBoolean(config.isGeneratingTrees());
        pkt.writeBoolean(config.getAllowGenerationChanges());
        pkt.writeVarInt(config.getLayers().size());
        for (FlatLayerInfo info : config.getLayers()) {
            pkt.writeVarInt(Block.getIdFromBlock(info.func_151536_b()));
            pkt.writeVarInt(info.getFillBlockMeta());
            pkt.writeVarInt(info.getLayerCount());
        }

        pkt.writeString(config.getBoundaryBlockA());
        pkt.writeVarInt(config.getBoundaryMetaA());
        pkt.writeString(config.getBoundaryBlockB());
        pkt.writeVarInt(config.getBoundaryMetaB());
        pkt.writeVarInt(config.getBoundaryChunkIntervalX());
        pkt.writeVarInt(config.getBoundaryChunkIntervalZ());

        pkt.writeVarInt(config.getGapWidth());
        pkt.writeVarInt(config.getGapPreset().ordinal());
        pkt.writeString(config.getGapBlockA());
        pkt.writeVarInt(config.getGapMetaA());
        pkt.writeString(config.getGapBlockB());
        pkt.writeVarInt(config.getGapMetaB());
        pkt.writeString(config.getGapBlockC());
        pkt.writeVarInt(config.getGapMetaC());

        pkt.writeBoolean(config.isCenterEnabled());
        pkt.writeVarInt(config.getCenterDirection().ordinal());
        pkt.writeString(config.getCenterBlock());
        pkt.writeVarInt(config.getCenterMeta());
    }

    static void readInto(DimensionConfig config, MCDataInput pkt) {
        config.setSaveDirOverride(pkt.readString());
        config.setNeedsSavingForPacket(true);
        config.setSkyColor(pkt.readInt());
        config.setStarBrightness(pkt.readFloat());
        config.setBiomeId(BiomeGenBase.getBiomeGenArray()[pkt.readVarInt()].biomeName);
        config.setDaylightCycle(DimensionConfig.DaylightCycle.fromOrdinal(pkt.readVarInt()));
        config.setCloudsEnabled(pkt.readBoolean());
        config.setSkyType(DimensionConfig.SkyType.fromOrdinal(pkt.readVarInt()));
        config.setWeatherEnabled(pkt.readBoolean());
        config.setGeneratingVegetation(pkt.readBoolean());
        config.setGeneratingTrees(pkt.readBoolean());
        config.setAllowGenerationChanges(pkt.readBoolean());
        config.setLayersFromPacket(readLayers(pkt));

        config.setBoundaryBlockA(pkt.readString());
        config.setBoundaryMetaA(pkt.readVarInt());
        config.setBoundaryBlockB(pkt.readString());
        config.setBoundaryMetaB(pkt.readVarInt());
        config.setBoundaryChunkIntervalX(pkt.readVarInt());
        config.setBoundaryChunkIntervalZ(pkt.readVarInt());

        config.setGapWidth(pkt.readVarInt());
        config.setGapPreset(DimensionConfig.GapPreset.fromOrdinal(pkt.readVarInt()));
        config.setGapBlockA(pkt.readString());
        config.setGapMetaA(pkt.readVarInt());
        config.setGapBlockB(pkt.readString());
        config.setGapMetaB(pkt.readVarInt());
        config.setGapBlockC(pkt.readString());
        config.setGapMetaC(pkt.readVarInt());

        config.setCenterEnabled(pkt.readBoolean());
        config.setCenterDirection(DimensionConfig.CenterDirection.fromOrdinal(pkt.readVarInt()));
        config.setCenterBlock(pkt.readString());
        config.setCenterMeta(pkt.readVarInt());
    }

    static DimensionConfig fromPacket(MCDataInput pkt) {
        DimensionConfig config = new DimensionConfig();
        readInto(config, pkt);
        return config;
    }

    private static ArrayList<FlatLayerInfo> readLayers(MCDataInput pkt) {
        int layerCount = pkt.readVarInt();
        ArrayList<FlatLayerInfo> layers = new ArrayList<>(layerCount);
        int y = 0;
        for (int layerI = 0; layerI < layerCount; ++layerI) {
            int blockId = pkt.readVarInt();
            int meta = pkt.readVarInt();
            int count = pkt.readVarInt();
            FlatLayerInfo info = new FlatLayerInfo(count, Block.getBlockById(blockId), meta);
            info.setMinY(y);
            layers.add(info);
            y += count;
        }
        return layers;
    }
}
