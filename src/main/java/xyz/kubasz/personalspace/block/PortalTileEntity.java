package xyz.kubasz.personalspace.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import xyz.kubasz.personalspace.CommonProxy;
import xyz.kubasz.personalspace.PersonalSpaceMod;
import xyz.kubasz.personalspace.net.Packets;
import xyz.kubasz.personalspace.world.DimensionConfig;
import xyz.kubasz.personalspace.world.PersonalTeleporter;
import xyz.kubasz.personalspace.world.PersonalWorldProvider;

public class PortalTileEntity extends TileEntity {
    public boolean active = false;
    public int targetDimId = 0;
    public int targetX = 8;
    public int targetY = 8;
    public int targetZ = 8;

    public PortalTileEntity() {
        PersonalSpaceMod.info("Made new PS Portal tile entity");
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        boolean isLegacy = false;
        if (tag.hasKey("isReturnPortal")) {
            isLegacy = true;
            targetDimId = 0;
        }
        if (tag.hasKey("remoteDimensionId")) {
            isLegacy = true;
            this.targetDimId = tag.getInteger("remoteDimensionId");
        }
        if (tag.hasKey("remotePos")) {
            isLegacy = true;
            int[] pos_array = tag.getIntArray("remotePos");
            this.targetX = pos_array[0];
            this.targetY = pos_array[1];
            this.targetZ = pos_array[2];
        }
        if (tag.hasKey("remoteDir")) {
            isLegacy = true;
            byte remoteDir = tag.getByte("remoteDir");
            switch (remoteDir) {
                case 0:
                    this.targetX += 1;
                    this.targetZ -= 1;
                    break;
                case 1:
                    this.targetX += 2;
                    this.targetZ += 1;
                    break;
                case 3:
                    this.targetX -= 1;
                    this.targetZ += 1;
                    break;
                default:
                    this.targetX += 1;
                    this.targetZ += 2;
                    break;
            }
        }
        if (tag.hasKey("localDimensionId")) {
            isLegacy = true;
        }
        if (tag.hasKey("localDir")) {
            isLegacy = true;
        }

        if (tag.hasKey("active")) {
            this.active = tag.getBoolean("active");
        }
        if (tag.hasKey("target")) {
            int[] t_array = tag.getIntArray("target");
            this.targetDimId = t_array[0];
            this.targetX = t_array[1];
            this.targetY = t_array[2];
            this.targetZ = t_array[3];
        }

        if (isLegacy) {
            markDirty();
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("active", this.active);
        tag.setIntArray("target", new int[]{this.targetDimId, this.targetX, this.targetY, this.targetZ});
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        if (worldObj.isRemote) {
            boolean wasActive = this.active;
            readFromNBT(pkt.func_148857_g());
            if (wasActive != this.active) {
                PersonalSpaceMod.proxy.closePortalGui(this);
            }
        }
    }

    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        if (worldObj == null) {
            return true;
        }
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        if (worldObj != entityplayer.worldObj) {
            return false;
        }
        return entityplayer.getDistanceSq((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D) <= 64.0D;
    }

    public void transport(EntityPlayerMP player) {
        if (worldObj.isRemote || !this.active) {
            return;
        }

        PersonalTeleporter tp = new PersonalTeleporter((WorldServer) worldObj, targetX, targetY, targetZ);
        player.mcServer.getConfigurationManager().transferPlayerToDimension(player, this.targetDimId, tp);
    }

    public void linkOtherPortal(boolean spawnNewPortal) {
        if (!this.active || this.targetDimId < 2) {
            return;
        }
        DimensionConfig config = CommonProxy.getDimensionConfigObjects(false).get(this.targetDimId);
        if (config == null) {
            return;
        }
        WorldServer world = DimensionManager.getWorld(this.targetDimId);
        if (world == null) {
            DimensionManager.initDimension(this.targetDimId);
            world = DimensionManager.getWorld(this.targetDimId);
        }
        if (world == null) {
            PersonalSpaceMod.LOG.fatal("Couldn't initialize personal space world " + this.targetDimId);
            return;
        }
        int otherX = 8;
        int otherY = config.getGroundLevel() + 1;
        int otherZ = 8;
        if (!world.blockExists(otherX, otherY, otherZ)) {
            GameRegistry.generateWorld(otherX >> 4, otherZ >> 4, world, world.getChunkProvider(), world.getChunkProvider());
        }
        if (world.getBlock(otherX, otherY, otherZ) == PersonalSpaceMod.BLOCK_PORTAL) {
            TileEntity wte = world.getTileEntity(otherX, otherY, otherZ);
            if (wte instanceof PortalTileEntity) {
                PortalTileEntity otherPortal = (PortalTileEntity) wte;
                otherPortal.active = true;
                otherPortal.targetDimId = worldObj.provider.dimensionId;
                otherPortal.targetX = xCoord;
                otherPortal.targetY = yCoord + 1;
                otherPortal.targetZ = zCoord;
                otherPortal.markDirty();
            }
        } else if (spawnNewPortal) {
            GameRegistry.generateWorld(otherX >> 4, otherZ >> 4, world, world.getChunkProvider(), world.getChunkProvider());
            world.setBlock(otherX, otherY, otherZ, PersonalSpaceMod.BLOCK_PORTAL);
            PortalTileEntity otherPortal = new PortalTileEntity();
            otherPortal.worldObj = world;
            otherPortal.xCoord = otherX;
            otherPortal.yCoord = otherY;
            otherPortal.zCoord = otherZ;
            otherPortal.active = true;
            otherPortal.targetDimId = worldObj.provider.dimensionId;
            otherPortal.targetX = xCoord;
            otherPortal.targetY = yCoord + 1;
            otherPortal.targetZ = zCoord;
            world.setTileEntity(otherX, otherY, otherZ, otherPortal);
            otherPortal.markDirty();
        }
    }

    public void updateSettings(EntityPlayerMP player, DimensionConfig unsafeConfig) {
        if (worldObj.isRemote) {
            return;
        }
        if (!worldObj.canMineBlock(player, xCoord, yCoord, zCoord)) {
            // permissions check
            return;
        }
        if (!DimensionConfig.canUseLayers(unsafeConfig.getLayersAsString())) {
            return;
        }
        DimensionConfig sanitized = new DimensionConfig();
        sanitized.copyFrom(unsafeConfig, false, true, true);
        int targetDimId = 0;
        if (this.worldObj.provider instanceof PersonalWorldProvider) {
            targetDimId = this.worldObj.provider.dimensionId;
        } else if (this.active) {
            targetDimId = this.targetDimId;
        }
        if (targetDimId > 0) {
            DimensionConfig realConfig = CommonProxy.getDimensionConfigObjects(false).get(targetDimId);
            if (realConfig == null) {
                return;
            }
            realConfig.copyFrom(sanitized, false, true, realConfig.getAllowGenerationChanges());
            realConfig.setAllowGenerationChanges(false);
        } else {
            if (this.worldObj.provider.dimensionId != 0) {
                return;
            }
            // create new dimension
            targetDimId = DimensionConfig.nextFreeDimId();
            sanitized.setAllowGenerationChanges(false);
            sanitized.registerWithDimensionManager(targetDimId, false);
            this.active = true;
            this.targetDimId = targetDimId;
            this.targetX = 8;
            this.targetY = sanitized.getGroundLevel() + 2;
            this.targetZ = 8;
            markDirty();

            linkOtherPortal(true);
        }
        Packets.INSTANCE.sendWorldList().sendToClients();
    }
}
