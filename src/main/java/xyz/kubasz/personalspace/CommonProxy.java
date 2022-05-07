package xyz.kubasz.personalspace;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.world.World;
import xyz.kubasz.personalspace.block.PortalTileEntity;
import xyz.kubasz.personalspace.world.DimensionConfig;

public class CommonProxy {

    protected final TIntObjectHashMap<DimensionConfig> clientDimensionConfigObjects = new TIntObjectHashMap<>();
    protected final TIntObjectHashMap<DimensionConfig> serverDimensionConfigObjects = new TIntObjectHashMap<>();

    public static TIntObjectHashMap<DimensionConfig> getDimensionConfigObjects(boolean isClient) {
        if (isClient) {
            return PersonalSpaceMod.proxy.clientDimensionConfigObjects;
        } else {
            return PersonalSpaceMod.proxy.serverDimensionConfigObjects;
        }
    }

    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {

    }

    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {

    }

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {

    }

    // register server commands in this event handler
    public void serverStarting(FMLServerStartingEvent event) {

    }

    public void serverStarted(FMLServerStartedEvent event) {

    }

    public void serverStopping(FMLServerStoppingEvent event) {

    }

    public void serverStopped(FMLServerStoppedEvent event) {
        getDimensionConfigObjects(false).clear();
    }

    public void openPortalGui(World world, int x, int y, int z) {
    }

    public void closePortalGui(PortalTileEntity owner) {
    }
}
