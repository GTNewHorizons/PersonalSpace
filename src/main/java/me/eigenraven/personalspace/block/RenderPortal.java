package me.eigenraven.personalspace.block;

import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderPortal extends TileEntitySpecialRenderer {

    private static final ResourceLocation field_147540_b = new ResourceLocation(
            "textures/entity/enchanting_table_book.png");
    private ModelBook bookModel = new ModelBook();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTickTime) {
        if (!(tile instanceof PortalTileEntity portal)) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + 0.5F, (float) y + 0.75F, (float) z + 0.5F);

        float f1 = (float) portal.tickCount + partialTickTime;
        GL11.glTranslatef(0.0F, 0.1F + MathHelper.sin(f1 * 0.1F) * 0.01F, 0.0F);

        float f3 = (float) Math.atan2(portal.facing.offsetZ, portal.facing.offsetX);
        GL11.glRotatef(-f3 * 180.0F / (float) Math.PI, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(80.0F, 0.0F, 0.0F, 1.0F);
        this.bindTexture(field_147540_b);

        float interpPage = portal.prevPagePosition + (portal.pagePosition - portal.prevPagePosition) * partialTickTime;
        float f4 = interpPage + 0.25F;
        float f5 = interpPage + 0.75F;
        f4 = (f4 - (float) MathHelper.truncateDoubleToInt(f4)) * 1.6F - 0.3F;
        f5 = (f5 - (float) MathHelper.truncateDoubleToInt(f5)) * 1.6F - 0.3F;
        f4 = MathHelper.clamp_float(f4, 0.0F, 1.0F);
        f5 = MathHelper.clamp_float(f5, 0.0F, 1.0F);

        GL11.glEnable(GL11.GL_CULL_FACE);
        this.bookModel.render(null, f1, f4, f5, 1.0F, 0.0F, 0.0625F);
        GL11.glPopMatrix();
    }
}
