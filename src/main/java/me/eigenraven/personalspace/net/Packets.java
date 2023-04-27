package me.eigenraven.personalspace.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;

import codechicken.lib.packet.PacketCustom;
import me.eigenraven.personalspace.CommonProxy;
import me.eigenraven.personalspace.Config;
import me.eigenraven.personalspace.PersonalSpaceMod;
import me.eigenraven.personalspace.block.PortalTileEntity;
import me.eigenraven.personalspace.world.DimensionConfig;

public enum Packets {

    INSTANCE;

    public enum PacketIds {

        DUMMY,
        UPDATE_WORLDLIST,
        CHANGE_WORLD_SETTINGS;

        static final PacketIds[] VALUES = values();
    }

    public void handleClientPacket(PacketCustom packetCustom, Minecraft mc, INetHandlerPlayClient handler) {
        int id = packetCustom.getType();
        if (id >= PacketIds.VALUES.length || id < 0) {
            return;
        }
        switch (PacketIds.VALUES[id]) {
            case UPDATE_WORLDLIST -> {
                handleWorldList(packetCustom);
            }
            case CHANGE_WORLD_SETTINGS -> {}
        }
    }

    public void handleServerPacket(PacketCustom pkt, EntityPlayerMP sender, INetHandlerPlayServer handler) {
        int id = pkt.getType();
        if (id >= PacketIds.VALUES.length || id < 0) {
            return;
        }
        switch (PacketIds.VALUES[id]) {
            case UPDATE_WORLDLIST -> {}
            case CHANGE_WORLD_SETTINGS -> {
                int dim = pkt.readVarInt();
                int x = pkt.readVarInt();
                int y = pkt.readVarInt();
                int z = pkt.readVarInt();
                DimensionConfig desired = DimensionConfig.fromPacket(pkt);
                if (sender != null && sender.worldObj != null && sender.worldObj.provider.dimensionId == dim) {
                    TileEntity te = sender.worldObj.getTileEntity(x, y, z);
                    if (te instanceof PortalTileEntity) {
                        ((PortalTileEntity) te).updateSettings(sender, desired);
                    }
                }
            }
        }
    }

    public PacketCustom sendWorldList() {
        PacketCustom pkt = new PacketCustom(PersonalSpaceMod.CHANNEL, PacketIds.UPDATE_WORLDLIST.ordinal());
        pkt.writeVarInt(Config.allowedBiomes.size());
        for (String biome : Config.allowedBiomes) {
            pkt.writeString(biome);
        }
        pkt.writeVarInt(Config.allowedBlocks.size());
        for (String block : Config.allowedBlocks) {
            pkt.writeString(block);
        }
        // Send all dimconfigs
        synchronized (CommonProxy.getDimensionConfigObjects(false)) {
            pkt.writeVarInt(CommonProxy.getDimensionConfigObjects(false).size());
            CommonProxy.getDimensionConfigObjects(false).forEachEntry((dimId, dimCfg) -> {
                pkt.writeVarInt(dimId);
                dimCfg.writeToPacket(pkt);
                return true;
            });
        }
        return pkt;
    }

    public PacketCustom sendChangeWorldSettings(PortalTileEntity tile, DimensionConfig config) {
        PacketCustom pkt = new PacketCustom(PersonalSpaceMod.CHANNEL, PacketIds.CHANGE_WORLD_SETTINGS.ordinal());
        pkt.writeVarInt(tile.getWorldObj().provider.dimensionId);
        pkt.writeVarInt(tile.xCoord);
        pkt.writeVarInt(tile.yCoord);
        pkt.writeVarInt(tile.zCoord);
        config.writeToPacket(pkt);
        return pkt;
    }

    private static void handleWorldList(PacketCustom pkt) {
        int allowedBiomes = pkt.readVarInt();
        // Use tmpList to only atomically swap references after the list is populated
        // to prevent concurrent modification by the network thread
        List<String> tmpList = new ArrayList<>(allowedBiomes);
        for (int i = 0; i < allowedBiomes; ++i) {
            tmpList.add(pkt.readString());
        }
        PersonalSpaceMod.clientAllowedBiomes = tmpList;
        int allowedBlocks = pkt.readVarInt();
        tmpList = new ArrayList<>(allowedBlocks);
        for (int i = 0; i < allowedBlocks; ++i) {
            tmpList.add(pkt.readString());
        }
        PersonalSpaceMod.clientAllowedBlocks = tmpList;
        int dimConfigs = pkt.readVarInt();
        for (int i = 0; i < dimConfigs; i++) {
            int dimId = pkt.readVarInt();
            DimensionConfig cfg = DimensionConfig.fromPacket(pkt);
            cfg.registerWithDimensionManager(dimId, true);
        }
    }
}
