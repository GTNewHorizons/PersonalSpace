package xyz.kubasz.personalspace.block;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class PortalEntityItem extends EntityItem {

    public PortalEntityItem(World p_i1711_1_) {
        super(p_i1711_1_);
    }

    public PortalEntityItem(World p_i1709_1_, double p_i1709_2_, double p_i1709_4_, double p_i1709_6_) {
        super(p_i1709_1_, p_i1709_2_, p_i1709_4_, p_i1709_6_);
    }

    public PortalEntityItem(
            World p_i1710_1_, double p_i1710_2_, double p_i1710_4_, double p_i1710_6_, ItemStack p_i1710_8_) {
        super(p_i1710_1_, p_i1710_2_, p_i1710_4_, p_i1710_6_, p_i1710_8_);
    }

    public PortalEntityItem(World world, Entity location, ItemStack itemstack) {
        super(world, location.posX, location.posY, location.posZ, itemstack);
        this.motionX = location.motionX;
        this.motionY = location.motionY;
        this.motionZ = location.motionZ;
        this.rotationPitch = location.rotationPitch;
        this.rotationYaw = location.rotationYaw;
        this.delayBeforeCanPickup = 10;
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float dmg) {
        if (src.equals(DamageSource.outOfWorld)) {
            return super.attackEntityFrom(src, dmg);
        }
        return false;
    }
}
