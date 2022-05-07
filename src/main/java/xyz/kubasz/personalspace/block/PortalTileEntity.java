package xyz.kubasz.personalspace.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import xyz.kubasz.personalspace.PersonalSpaceMod;

public class PortalTileEntity extends TileEntity {
    PortalTileEntity() {
        PersonalSpaceMod.info("Made new PS Portal tile entity");
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
    }

    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        if (worldObj == null) {
            return true;
        }
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return entityplayer.getDistanceSq((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D) <= 64D;
    }
}
