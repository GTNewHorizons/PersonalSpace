package xyz.kubasz.personalspace.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import xyz.kubasz.personalspace.PersonalSpaceMod;

import java.util.ArrayList;

public class PortalBlock extends Block implements ITileEntityProvider {
    public PortalBlock() {
        super(Material.rock);
        this.setBlockName("personalSpacePortal");
        this.setHardness(5.0F);
        this.setResistance(2000.0F);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
        this.setLightOpacity(0);
        this.setBlockTextureName("obsidian");
        this.setCreativeTab(CreativeTabs.tabTransport);
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int _meta) {
        switch (side) {
            // top
            case 1:
                return Blocks.portal.getIcon(0, 0);
            // bottom
            case 0:
                return Blocks.enchanting_table.getIcon(side, 0);
            // sides
            default:
                return Blocks.obsidian.getIcon(side, 0);
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new PortalTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            PersonalSpaceMod.proxy.openPortalGui(world, x, y, z);
            return true;
        } else {
            TileEntity wte = world.getTileEntity(x, y, z);
            if (!(wte instanceof PortalTileEntity)) {
                return false;
            }
            PortalTileEntity te = (PortalTileEntity) wte;
            if (te.active && !player.isSneaking() && player instanceof EntityPlayerMP) {
                te.transport((EntityPlayerMP) player);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack fromItem) {
        TileEntity wte = world.getTileEntity(x, y, z);
        if (!(wte instanceof PortalTileEntity)) {
            return;
        }
        PortalTileEntity te = (PortalTileEntity) wte;
        if (fromItem.hasTagCompound()) {
            te.readFromNBT(fromItem.getTagCompound());
            te.markDirty();
            te.linkOtherPortal(false);
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ItemStack drop = new ItemStack(this);
        TileEntity wte = world.getTileEntity(x, y, z);
        if (wte instanceof PortalTileEntity) {
            PortalTileEntity te = (PortalTileEntity) wte;
            NBTTagCompound tag = new NBTTagCompound();
            te.writeToNBT(tag);
            tag.removeTag("x");
            tag.removeTag("y");
            tag.removeTag("z");
            drop.setTagCompound(tag);
        }
        ArrayList<ItemStack> drops = new ArrayList<>(1);
        drops.add(drop);
        return drops;
    }
}
