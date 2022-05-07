package xyz.kubasz.personalspace;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraftforge.common.DimensionManager;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.kubasz.personalspace.block.PortalBlock;
import xyz.kubasz.personalspace.block.PortalTileEntity;
import xyz.kubasz.personalspace.world.DimensionConfig;

import java.io.File;
import java.util.Objects;

@Mod(modid = Tags.MODID, version = Tags.VERSION, name = Tags.MODNAME, acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "after:utilityworlds;after:appliedenergistics2-core")
public class PersonalSpaceMod {

    public static final String DIM_METADATA_FILE = "personalspace_metadata.nbt";

    @Mod.Instance(Tags.MODID)
    public static PersonalSpaceMod INSTANCE;

    public static Logger LOG = LogManager.getLogger(Tags.MODID);

    @SidedProxy(clientSide = Tags.GROUPNAME + ".ClientProxy", serverSide = Tags.GROUPNAME + ".CommonProxy")
    public static CommonProxy proxy;

    public static PortalBlock BLOCK_PORTAL;

    public static TIntObjectHashMap<DimensionConfig> registeredDimensions;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items,
    // etc, and register them with the GameRegistry."
    public void preInit(FMLPreInitializationEvent event) {
        if (Loader.isModLoaded("utilityworlds")) {
            throw new RuntimeException("Personal space mod cannot be loaded in the same instance with utilityworlds mod, because it replaces its functionality");
        }
        proxy.preInit(event);
        BLOCK_PORTAL = new PortalBlock();
        GameRegistry.registerBlock(BLOCK_PORTAL, "personalPortal");
        GameRegistry.registerTileEntityWithAlternatives(PortalTileEntity.class, "personalspace:personalPortal", "uw_portal_te");
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes."
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.serverAboutToStart(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
        loadDimensionConfigs();
    }

    void loadDimensionConfigs() {
        registeredDimensions.clear();
        try {
            File saveDir = DimensionManager.getCurrentSaveRootDirectory();
            if (saveDir == null || !saveDir.isDirectory()) {
                return;
            }
            for (File dir : Objects.requireNonNull(saveDir.listFiles())) {
                if (!dir.isDirectory()) {
                    continue;
                }
                File dimConfig = new File(dir, DIM_METADATA_FILE);
                if (dimConfig.exists() && dimConfig.isFile()) {
                    //
                    continue;
                }
                // Metadata not found, test for UW worlds.
                MutablePair<DimensionConfig, Integer> dc = DimensionConfig.fromUtilityWorldsWorld(dir.getName());
                if (dc != null) {

                }
            }
        } catch (Exception e) {
            LOG.error("Caught error while loading and registering personal space dimensions", e);
        }
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }

    @Mod.EventHandler
    public void missingMapping(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping mapping : event.getAll()) {
            if (mapping.type == GameRegistry.Type.BLOCK) {
                switch (mapping.name) {
                    case "utilityworlds:uw_portal_mining":
                    case "utilityworlds:uw_portal_void":
                    case "utilityworlds:uw_portal_garden":
                    case "utilityworlds:uw_portal_return":
                        mapping.remap(GameRegistry.findBlock(Tags.MODID, "personalPortal"));
                        break;
                    default:
                }
            } else if (mapping.type == GameRegistry.Type.ITEM) {
                switch (mapping.name) {
                    case "utilityworlds:uw_portal_mining":
                    case "utilityworlds:uw_portal_void":
                    case "utilityworlds:uw_portal_garden":
                    case "utilityworlds:uw_portal_return":
                        mapping.remap(GameRegistry.findItem(Tags.MODID, "personalPortal"));
                        break;
                    default:
                }
            }
        }
    }

    public static void debug(String message) {
        LOG.debug(message);
    }

    public static void info(String message) {
        LOG.info(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
    }

    public static void error(String message) {
        LOG.error(message);
    }
}
