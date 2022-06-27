package xyz.kubasz.personalspace.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;
import xyz.kubasz.personalspace.CommonProxy;
import xyz.kubasz.personalspace.PersonalSpaceMod;
import xyz.kubasz.personalspace.net.Packets;
import xyz.kubasz.personalspace.world.DimensionConfig;
import xyz.kubasz.personalspace.world.PersonalTeleporter;
import xyz.kubasz.personalspace.world.PersonalWorldProvider;

public class PortalTileEntity extends TileEntity {
    public static final ForgeDirection DEFAULT_FACING = ForgeDirection.NORTH;

    public boolean active = false;
    public int targetDimId = 0;
    public int targetX = 8;
    public int targetY = 8;
    public int targetZ = 8;
    public ForgeDirection facing = DEFAULT_FACING;

    public PortalTileEntity() {}

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("x") || tag.hasKey("y") || tag.hasKey("z")) {
            super.readFromNBT(tag);
        }
        boolean isLegacy = false;
        if (tag.hasKey("isReturnPortal")) {
            isLegacy = true;
            if (tag.getBoolean("isReturnPortal")) {
                this.targetDimId = 0;
            }
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
                    this.facing = ForgeDirection.NORTH;
                    break;
                case 1:
                    this.facing = ForgeDirection.EAST;
                    break;
                case 3:
                    this.facing = ForgeDirection.WEST;
                    break;
                default:
                    this.facing = ForgeDirection.SOUTH;
                    break;
            }
            this.targetX += 1;
            this.targetZ += 1;
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
        if (tag.hasKey("facing")) {
            facing = ForgeDirection.getOrientation(tag.getInteger("facing"));
        }

        if (isLegacy) {
            this.active = true;
            PersonalSpaceMod.LOG.info(
                    "Migrated old UW portal to dim {} : target {},{},{}", targetDimId, targetX, targetY, targetZ);
            markDirty();
        }
        if (facing == ForgeDirection.UNKNOWN) {
            facing = ForgeDirection.NORTH;
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
        tag.setIntArray("target", new int[] {this.targetDimId, this.targetX, this.targetY, this.targetZ});
        tag.setInteger("facing", this.facing.ordinal());
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
            readFromNBT(pkt.func_148857_g());
            PersonalSpaceMod.proxy.closePortalGui(this);
        }
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
            GameRegistry.generateWorld(
                    otherX >> 4, otherZ >> 4, world, world.getChunkProvider(), world.getChunkProvider());
        }
        PortalTileEntity otherPortal = null;

        if (world.getBlock(otherX, otherY, otherZ) == PersonalSpaceMod.BLOCK_PORTAL) {
            TileEntity wte = world.getTileEntity(otherX, otherY, otherZ);
            if (wte instanceof PortalTileEntity) {
                otherPortal = (PortalTileEntity) wte;
            }
        } else if (spawnNewPortal) {
            world.setBlock(otherX, otherY, otherZ, PersonalSpaceMod.BLOCK_PORTAL);
            otherPortal = (PortalTileEntity) world.getTileEntity(otherX, otherY, otherZ);
        }
        if (otherPortal != null) {
            otherPortal.active = true;
            otherPortal.targetDimId = worldObj.provider.dimensionId;
            otherPortal.targetX = xCoord + facing.offsetX;
            otherPortal.targetY = yCoord + facing.offsetY;
            otherPortal.targetZ = zCoord + facing.offsetZ;
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
        if (!DimensionConfig.canUseBiome(unsafeConfig.getBiomeId())) {
            unsafeConfig.setBiomeId("Plains");
        }
        DimensionConfig sanitized = new DimensionConfig();
        sanitized.copyFrom(unsafeConfig, false, true, true);
        boolean createdNewDim = false;
        int targetDimId = 0;
        if (this.worldObj.provider instanceof PersonalWorldProvider) {
            targetDimId = this.worldObj.provider.dimensionId;
        } else if (this.active) {
            targetDimId = this.targetDimId;
        }
        if (targetDimId > 0) {
            DimensionConfig realConfig =
                    CommonProxy.getDimensionConfigObjects(false).get(targetDimId);
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
            this.targetX = 8 + DEFAULT_FACING.offsetX;
            this.targetY = sanitized.getGroundLevel() + 1 + DEFAULT_FACING.offsetY;
            this.targetZ = 8 + DEFAULT_FACING.offsetZ;
            markDirty();
            createdNewDim = true;

            linkOtherPortal(true);
        }
        Packets.INSTANCE.sendWorldList().sendToClients();
        if (createdNewDim) {
            player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.created"));
        } else {
            player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.updated"));
        }
    }
}
