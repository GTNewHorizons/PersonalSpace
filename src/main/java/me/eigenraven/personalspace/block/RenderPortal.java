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
        if (!(tile instanceof PortalTileEntity)) {
            return;
        }
        PortalTileEntity portal = (PortalTileEntity) tile;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + 0.5F, (float) y + 0.75F, (float) z + 0.5F);
        float time = ((float) tile.getWorldObj().getWorldInfo().getWorldTotalTime() + partialTickTime) / 100.0F;
        GL11.glTranslatef(0.0F, 0.1F + MathHelper.sin(time * 0.1F) * 0.01F, 0.0F);

        float f3 = (float) Math.atan2(portal.facing.offsetZ, portal.facing.offsetX);
        GL11.glRotatef(-f3 * 180.0F / (float) Math.PI, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(80.0F, 0.0F, 0.0F, 1.0F);
        this.bindTexture(field_147540_b);
        float pageRightAngle = 0.3F;
        float pageLeftAngle = 0.9F;
        pageRightAngle = (pageRightAngle - (float) MathHelper.truncateDoubleToInt(pageRightAngle)) * 1.6F - 0.3F;
        pageLeftAngle = (pageLeftAngle - (float) MathHelper.truncateDoubleToInt(pageLeftAngle)) * 1.6F - 0.3F;
        pageRightAngle = MathHelper.clamp_float(pageRightAngle, 0.0F, 1.0F);
        pageLeftAngle = MathHelper.clamp_float(pageLeftAngle, 0.0F, 1.0F);

        float f6 = 0.75F + 0.1F * (float) Math.sin(time);
        GL11.glEnable(GL11.GL_CULL_FACE);
        this.bookModel.render(null, time, pageRightAngle, pageLeftAngle, f6, 0.0F, 0.0625F);
        GL11.glPopMatrix();
    }
}
