package xyz.kubasz.personalspace.block;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

public class PortalItem extends ItemBlock {
    public PortalItem(Block block) {
        super(block);
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List list, boolean displayMoreInfo) {
        if (itemStack.getTagCompound() != null) {
            int[] target = itemStack.getTagCompound().getIntArray("target");
            if (target.length >= 4) {
                list.add(String.format("DIM%d: %d, %d, %d", target[0], target[1], target[2], target[3]));
            }
        }
    }

    @Override
    public boolean hasEffect(ItemStack itemStack, int pass) {
        return itemStack != null
                && itemStack.getTagCompound() != null
                && itemStack.getTagCompound().hasKey("target");
    }

    @Override
    public boolean onItemUse(
            ItemStack itemStack,
            EntityPlayer player,
            World world,
            int x,
            int y,
            int z,
            int clickSide,
            float p_77648_8_,
            float p_77648_9_,
            float p_77648_10_) {
        for (int xc = x - 2; xc <= x + 2; xc++) {
            for (int yc = y - 2; yc <= y + 2; yc++) {
                for (int zc = z - 2; zc <= z + 2; zc++) {
                    if (world.blockExists(xc, yc, zc)) {
                        if (world.getBlock(xc, yc, zc) instanceof PortalBlock) {
                            if (!world.isRemote && player != null) {
                                player.addChatMessage(new ChatComponentTranslation("chat.personalWorld.proximity"));
                            }
                            return false;
                        }
                    }
                }
            }
        }
        return super.onItemUse(itemStack, player, world, x, y, z, clickSide, p_77648_8_, p_77648_9_, p_77648_10_);
    }

    /**
     * Prevents despawning of portals on the ground
     */
    @Override
    public int getEntityLifespan(ItemStack itemStack, World world) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    /**
     * Override the portal item entity to be indestructible by fire, lava, cactus, explosion etc. damage
     */
    @Override
    public Entity createEntity(World world, Entity location, ItemStack itemstack) {
        return new PortalEntityItem(world, location, itemstack);
    }
}
