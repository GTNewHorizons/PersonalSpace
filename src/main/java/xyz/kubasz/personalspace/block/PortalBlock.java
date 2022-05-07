package xyz.kubasz.personalspace.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

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
}
