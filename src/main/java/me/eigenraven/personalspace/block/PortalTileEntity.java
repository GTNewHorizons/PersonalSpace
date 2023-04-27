package me.eigenraven.personalspace.block;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import me.eigenraven.personalspace.Config;
import me.eigenraven.personalspace.PersonalSpaceMod;
import me.eigenraven.personalspace.net.Packets;
import me.eigenraven.personalspace.world.DimensionConfig;
import me.eigenraven.personalspace.world.PersonalTeleporter;
import me.eigenraven.personalspace.world.PersonalWorldProvider;

public class PortalTileEntity extends TileEntity {

    public static final ForgeDirection DEFAULT_FACING = ForgeDirection.NORTH;

    public boolean active = false;
    public int targetDimId = 0;
    public int targetPosX = 8;
    public int targetPosY = 8;
    public int targetPosZ = 8;
    public ForgeDirection targetFacing = DEFAULT_FACING;
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
            this.targetPosX = pos_array[0];
            this.targetPosY = pos_array[1];
            this.targetPosZ = pos_array[2];
        }
        if (tag.hasKey("remoteDir")) {
            isLegacy = true;
            byte remoteDir = tag.getByte("remoteDir");
            this.facing = switch (remoteDir) {
                case 0 -> ForgeDirection.NORTH;
                case 1 -> ForgeDirection.EAST;
                case 3 -> ForgeDirection.WEST;
                default -> ForgeDirection.SOUTH;
            };
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
            this.targetPosX = t_array[1];
            this.targetPosY = t_array[2];
            this.targetPosZ = t_array[3];
        }
        if (tag.hasKey("facing")) {
            facing = ForgeDirection.getOrientation(tag.getInteger("facing"));
        }
        if (tag.hasKey("targetFacing")) {
            targetFacing = ForgeDirection.getOrientation(tag.getInteger("targetFacing"));
        }

        if (isLegacy) {
            this.active = true;
            PersonalSpaceMod.LOG.info(
                    "Migrated old UW portal to dim {} : target {},{},{}",
                    targetDimId,
                    targetPosX,
                    targetPosY,
                    targetPosZ);
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
        tag.setIntArray("target", new int[] { this.targetDimId, this.targetPosX, this.targetPosY, this.targetPosZ });
        tag.setInteger("facing", this.facing.ordinal());
        tag.setInteger("targetFacing", this.targetFacing.ordinal());
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

        PersonalTeleporter tp = new PersonalTeleporter(this, (WorldServer) worldObj);

        player.mcServer.getConfigurationManager().transferPlayerToDimension(player, this.targetDimId, tp);
    }

    public int getTargetTeleportX() {
        return targetPosX + targetFacing.offsetX;
    }

    public int getTargetTeleportY() {
        return targetPosY + targetFacing.offsetY;
    }

    public int getTargetTeleportZ() {
        return targetPosZ + targetFacing.offsetZ;
    }

    public void linkOtherPortal(boolean spawnNewPortal, EntityPlayerMP player) {
        if (!this.active) {
            return;
        }
        if (worldObj.isRemote) {
            return;
        }
        WorldServer otherWorld = DimensionManager.getWorld(this.targetDimId);
        if (otherWorld == null) {
            DimensionManager.initDimension(this.targetDimId);
            otherWorld = DimensionManager.getWorld(this.targetDimId);
        }
        if (otherWorld == null) {
            PersonalSpaceMod.LOG.fatal("Couldn't initialize world {}", this.targetDimId);
            return;
        }
        int otherX = targetPosX, otherY = targetPosY, otherZ = targetPosZ;
        searchloop: for (otherX = targetPosX - 1; otherX <= targetPosX + 1; otherX++) {
            for (otherY = targetPosY - 1; otherY <= targetPosY + 1; otherY++) {
                if (otherY < 0 || otherY > otherWorld.getHeight()) continue;
                for (otherZ = targetPosZ - 1; otherZ <= targetPosZ + 1; otherZ++) {
                    if (!otherWorld.blockExists(otherX, otherY, otherZ)) {
                        otherWorld.theChunkProviderServer.loadChunk(otherX >> 4, otherZ >> 4);
                    }
                    if (otherWorld.getBlock(otherX, otherY, otherZ) instanceof PortalBlock) {
                        break searchloop;
                    }
                }
            }
        }
        PortalTileEntity otherPortal = null;

        if (otherWorld.getBlock(otherX, otherY, otherZ) == PersonalSpaceMod.BLOCK_PORTAL) {
            TileEntity wte = otherWorld.getTileEntity(otherX, otherY, otherZ);
            if (wte instanceof PortalTileEntity) {
                otherPortal = (PortalTileEntity) wte;
            }
        } else if (spawnNewPortal) {
            otherX = targetPosX;
            otherY = targetPosY;
            otherZ = targetPosZ;
            otherWorld.setBlock(otherX, otherY, otherZ, PersonalSpaceMod.BLOCK_PORTAL, facing.ordinal(), 3);
            otherPortal = (PortalTileEntity) otherWorld.getTileEntity(otherX, otherY, otherZ);
        }
        if (otherPortal != null) {
            otherPortal.active = true;
            DimensionConfig otherPortalDimCfg = DimensionConfig.getForDimension(otherPortal.targetDimId, false);
            if (otherPortal.targetDimId != worldObj.provider.dimensionId && otherPortalDimCfg != null) {
                if (player != null) {
                    player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.relinked.error"));
                }
                return;
            }
            otherPortal.targetDimId = worldObj.provider.dimensionId;
            otherPortal.targetPosX = xCoord;
            otherPortal.targetPosY = yCoord;
            otherPortal.targetPosZ = zCoord;
            otherPortal.targetFacing = facing;
            otherPortal.markDirty();
            if (player != null) {
                player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.relinked", targetDimId));
            }
            PersonalSpaceMod.LOG.info(
                    "Linked portal at {}:{},{},{} to {}:{},{},{}",
                    targetDimId,
                    otherX,
                    otherY,
                    otherZ,
                    worldObj.provider.dimensionId,
                    xCoord,
                    yCoord,
                    zCoord);
        }
    }

    public void updateSettings(EntityPlayerMP player, DimensionConfig unsafeConfig) {
        if (worldObj.isRemote || player == null) {
            return;
        }
        if (!worldObj.canMineBlock(player, xCoord, yCoord, zCoord)) {
            // permissions check
            player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.denied"));
            PersonalSpaceMod.LOG.warn(
                    "Player {} tried to modify settings for portal block @{},{},{} (dim {}, target dim {}), denied - spawn protection.",
                    player,
                    xCoord,
                    yCoord,
                    zCoord,
                    worldObj.provider.dimensionId,
                    targetDimId);
            return;
        }
        // Send a fake block break event to test permissions further
        if (Config.useBlockEventChecks) {
            BlockEvent.BreakEvent fakeBreakEvent = new BlockEvent.BreakEvent(
                    xCoord,
                    yCoord,
                    zCoord,
                    worldObj,
                    PersonalSpaceMod.BLOCK_PORTAL,
                    0,
                    player);
            if (MinecraftForge.EVENT_BUS.post(fakeBreakEvent)) {
                player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.denied"));
                PersonalSpaceMod.LOG.warn(
                        "Player {} tried to modify settings for portal block @{},{},{} (dim {}, target dim {}), denied - block permission.",
                        player,
                        xCoord,
                        yCoord,
                        zCoord,
                        worldObj.provider.dimensionId,
                        targetDimId);
                return;
            }
        }
        if (!DimensionConfig.canUseLayers(unsafeConfig.getLayersAsString(), false)) {
            player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.badLayers"));
            PersonalSpaceMod.LOG.warn(
                    "Player {} tried to modify settings for portal block @{},{},{} (dim {}, target dim {}), denied - using forbidden layers.",
                    player,
                    xCoord,
                    yCoord,
                    zCoord,
                    worldObj.provider.dimensionId,
                    targetDimId);
            return;
        }
        if (!DimensionConfig.canUseBiome(unsafeConfig.getBiomeId(), false)) {
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
        boolean changed = true;
        if (targetDimId > 0) {
            DimensionConfig realConfig = DimensionConfig.getForDimension(targetDimId, false);
            if (realConfig == null) {
                return;
            }
            changed = realConfig.copyFrom(sanitized, false, true, realConfig.getAllowGenerationChanges());
            realConfig.setAllowGenerationChanges(false);
            PersonalSpaceMod.INSTANCE.onDimSettingsChangeServer(targetDimId);
        } else {
            if (this.worldObj.provider.dimensionId != 0) {
                return;
            }
            // create new dimension
            targetDimId = DimensionConfig.nextFreeDimId();
            sanitized.setAllowGenerationChanges(false);
            sanitized.registerWithDimensionManager(targetDimId, false);
            PersonalSpaceMod.INSTANCE.onDimSettingsChangeServer(targetDimId);
            this.active = true;
            this.targetDimId = targetDimId;
            this.targetPosY = sanitized.getGroundLevel() + 1;
            markDirty();
            createdNewDim = true;

            linkOtherPortal(true, player);
        }
        Packets.INSTANCE.sendWorldList().sendToClients();
        if (createdNewDim) {
            player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.created"));
        } else if (changed) {
            player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.updated"));
        }
    }
}
