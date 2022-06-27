package xyz.kubasz.personalspace.block;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

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
}
