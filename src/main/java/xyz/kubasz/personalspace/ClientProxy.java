package xyz.kubasz.personalspace;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import xyz.kubasz.personalspace.block.PortalTileEntity;
import xyz.kubasz.personalspace.gui.GuiEditWorld;

public class ClientProxy extends CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        super.serverAboutToStart(event);
    }

    // register server commands in this event handler
    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event) {
        super.serverStarted(event);
    }

    @Override
    public void serverStopping(FMLServerStoppingEvent event) {
        super.serverStopping(event);
    }

    @Override
    public void serverStopped(FMLServerStoppedEvent event) {
        super.serverStopped(event);
    }

    @Override
    public void openPortalGui(World world, int x, int y, int z) {
        TileEntity wte = world.getTileEntity(x, y, z);
        if (!(wte instanceof PortalTileEntity)) {
            return;
        }
        PortalTileEntity te = (PortalTileEntity) wte;
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (!te.active && player.worldObj.provider.dimensionId != 0) {
            player.addChatMessage(new ChatComponentText(I18n.format("chat.overworldPersonalDimension")));
            return;
        }
        if (!te.active || player.isSneaking()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiEditWorld(te));
        }
    }

    @Override
    public void closePortalGui(PortalTileEntity owner) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiEditWorld) {
            GuiEditWorld gui = (GuiEditWorld) screen;
            if (gui.tile == owner) {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
        }
    }
}
