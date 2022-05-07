package xyz.kubasz.personalspace.net;

import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import xyz.kubasz.personalspace.CommonProxy;
import xyz.kubasz.personalspace.PersonalSpaceMod;
import xyz.kubasz.personalspace.block.PortalTileEntity;
import xyz.kubasz.personalspace.world.DimensionConfig;

public enum Packets {
    INSTANCE;

    public enum PacketIds {
        DUMMY,
        UPDATE_WORLDLIST,
        CHANGE_WORLD_SETTINGS;
        final static PacketIds[] VALUES = values();
    }

    public void handleClientPacket(PacketCustom packetCustom, Minecraft mc, INetHandlerPlayClient handler) {
        int id = packetCustom.getType();
        if (id >= PacketIds.VALUES.length || id < 0) {
            return;
        }
        switch (PacketIds.VALUES[id]) {
            case UPDATE_WORLDLIST: {
                handleWorldList(packetCustom);
                break;
            }
            case CHANGE_WORLD_SETTINGS: {
                break;
            }
        }
    }

    public void handleServerPacket(PacketCustom pkt, EntityPlayerMP sender, INetHandlerPlayServer handler) {
        int id = pkt.getType();
        if (id >= PacketIds.VALUES.length || id < 0) {
            return;
        }
        switch (PacketIds.VALUES[id]) {
            case UPDATE_WORLDLIST: {
                break;
            }
            case CHANGE_WORLD_SETTINGS: {
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
                break;
            }
        }
    }

    public PacketCustom sendWorldList() {
        PacketCustom pkt = new PacketCustom(PersonalSpaceMod.CHANNEL, PacketIds.UPDATE_WORLDLIST.ordinal());
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
        int dimConfigs = pkt.readVarInt();
        for (int i = 0; i < dimConfigs; i++) {
            int dimId = pkt.readVarInt();
            DimensionConfig cfg = DimensionConfig.fromPacket(pkt);
            cfg.registerWithDimensionManager(dimId, true);
        }
    }
}
