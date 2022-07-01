package xyz.kubasz.personalspace;

import appeng.api.AEApi;
import appeng.api.IAppEngApi;
import appeng.api.features.IWorldGen;
import codechicken.lib.packet.PacketCustom;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Optional;
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
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkHandshakeEstablished;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.kubasz.personalspace.block.PortalBlock;
import xyz.kubasz.personalspace.block.PortalItem;
import xyz.kubasz.personalspace.block.PortalTileEntity;
import xyz.kubasz.personalspace.net.Packets;
import xyz.kubasz.personalspace.world.DimensionConfig;
import xyz.kubasz.personalspace.world.PersonalWorldProvider;

@Mod(
        modid = "personalspace",
        version = Tags.VERSION,
        name = Tags.MODNAME,
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "after:utilityworlds;after:appliedenergistics2-core")
public class PersonalSpaceMod {

    public static final String DIM_METADATA_FILE = "personalspace_metadata.cfg";

    @Mod.Instance
    public static PersonalSpaceMod INSTANCE;

    public static Logger LOG = LogManager.getLogger(Tags.MODID);

    @SidedProxy(
            clientSide = "xyz.kubasz.personalspace.ClientProxy",
            serverSide = "xyz.kubasz.personalspace.CommonProxy")
    public static CommonProxy proxy;

    public static PortalBlock BLOCK_PORTAL, BP_MIGRATION_2, BP_MIGRATION_3, BP_MIGRATION_4;

    public static final String CHANNEL = Tags.MODID;

    public static List<String> clientAllowedBlocks = Lists.newArrayList(), clientAllowedBiomes = Lists.newArrayList();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (Loader.isModLoaded("utilityworlds")) {
            throw new RuntimeException(
                    "Personal space mod cannot be loaded in the same instance with utilityworlds mod, because it replaces its functionality");
        }
        if (event.getSide().isClient()) {
            PacketCustom.assignHandler(
                    CHANNEL, (PacketCustom.IClientPacketHandler) Packets.INSTANCE::handleClientPacket);
        }
        PacketCustom.assignHandler(CHANNEL, (PacketCustom.IServerPacketHandler) Packets.INSTANCE::handleServerPacket);
        NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL + "_event").register(this);
        proxy.preInit(event);
        BLOCK_PORTAL = new PortalBlock(false);
        BP_MIGRATION_2 = new PortalBlock(true);
        BP_MIGRATION_3 = new PortalBlock(true);
        BP_MIGRATION_4 = new PortalBlock(true);
        GameRegistry.registerBlock(BLOCK_PORTAL, PortalItem.class, "personalPortal");
        GameRegistry.registerBlock(BP_MIGRATION_2, PortalItem.class, "personalPortal_migration2");
        GameRegistry.registerBlock(BP_MIGRATION_3, PortalItem.class, "personalPortal_migration3");
        GameRegistry.registerBlock(BP_MIGRATION_4, PortalItem.class, "personalPortal_migration4");
        GameRegistry.registerTileEntityWithAlternatives(
                PortalTileEntity.class, "personalspace:personalPortal", "uw_portal_te");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        if (Loader.isModLoaded("appliedenergistics2-core")) {
            removeAe2Meteors();
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Optional.Method(modid = "appliedenergistics2-core")
    private void removeAe2Meteors() {
        IAppEngApi aeApi = AEApi.instance();
        if (aeApi == null) {
            return;
        }
        aeApi.registries()
                .worldgen()
                .disableWorldGenForProviderID(IWorldGen.WorldGenType.Meteorites, PersonalWorldProvider.class);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this."
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.serverAboutToStart(event);
        loadDimensionConfigs();
    }

    @Mod.EventHandler
    // register server commands in this event handler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
        event.registerServerCommand(new PersonalSpaceCommand());
    }

    void loadDimensionConfigs() {
        try {
            deregisterServerDimensions();
            File saveDir = DimensionManager.getCurrentSaveRootDirectory();
            LOG.info("Searching for PS worlds at {}", saveDir.getPath());
            if (saveDir == null || !saveDir.isDirectory()) {
                return;
            }
            for (File dir : Objects.requireNonNull(saveDir.listFiles())) {
                if (!dir.isDirectory()) {
                    continue;
                }
                File dimConfig = new File(dir, DIM_METADATA_FILE);
                if (dimConfig.exists() && dimConfig.isFile()) {
                    try {
                        DimensionConfig dimCfg = new DimensionConfig();
                        int dimId = dimCfg.syncWithFile(dimConfig, false, 0);
                        dimCfg.setSaveDirOverride(dir.getName());
                        dimCfg.registerWithDimensionManager(dimId, false);
                        LOG.info("Loaded PersonalSpace world {} (at {})", dimId, dir.getName());
                    } catch (Exception e) {
                        LOG.error("Couldn't load personal dimension data from " + dimConfig.getPath(), e);
                    }
                    continue;
                }
                // Metadata not found, test for UW worlds.
                MutablePair<DimensionConfig, Integer> dc = DimensionConfig.fromUtilityWorldsWorld(dir.getName());
                if (dc != null) {
                    dc.getLeft().registerWithDimensionManager(dc.getRight(), false);
                    saveConfig(dc.getRight(), dc.getLeft());
                    LOG.info("Migrated world {} (at {}) from utilityworlds", dc.getRight(), dir.getName());
                }
            }
        } catch (Exception e) {
            LOG.error("Caught error while loading and registering personal space dimensions", e);
        }
    }

    private void saveConfig(int dimId, DimensionConfig config) throws IOException {
        File saveDir = DimensionManager.getCurrentSaveRootDirectory();
        if (saveDir == null || !saveDir.isDirectory()) {
            return;
        }
        saveDir = new File(saveDir, config.getSaveDir(dimId));
        if (!(saveDir.exists() && saveDir.isDirectory())) {
            if (!saveDir.mkdirs()) {
                throw new IOException(
                        "Couldn't create save directory for personal dimension " + config.getSaveDir(dimId));
            }
        }
        File dataFile = new File(saveDir, DIM_METADATA_FILE);
        config.syncWithFile(dataFile, true, dimId);
    }

    @SubscribeEvent
    public void worldSave(WorldEvent.Save event) {
        try {
            if (!(event.world.provider instanceof PersonalWorldProvider)) {
                return;
            }
            PersonalWorldProvider provider = (PersonalWorldProvider) event.world.provider;
            DimensionConfig config = provider.getConfig();
            if (config == null || !config.needsSaving()) {
                return;
            }
            saveConfig(provider.dimensionId, config);
        } catch (Exception e) {
            LOG.fatal("Couldn't save personal dimension data for " + event.world.provider.getDimensionName(), e);
        }
    }

    private static boolean thermosLogged = false;

    public static boolean isInThermos() {
        try {
            Class.forName("thermos.Thermos");
            if (!thermosLogged) {
                thermosLogged = true;
                LOG.warn("Thermos detected, applying workarounds");
            }
            return true;
        } catch (ClassNotFoundException e) {
            if (!thermosLogged) {
                thermosLogged = true;
                LOG.info("Thermos not detected");
            }
            return false;
        }
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
        TIntObjectHashMap<DimensionConfig> configs = CommonProxy.getDimensionConfigObjects(true);
        configs.forEachEntry((dimId, dimCfg) -> {
            if (dimCfg == null || !dimCfg.needsSaving()) {
                return true;
            }
            try {
                saveConfig(dimId, dimCfg);
            } catch (IOException e) {
                LOG.error("Couldn't save dimension " + dimId, e);
            }
            return true;
        });
    }

    private void deregisterServerDimensions() {
        synchronized (CommonProxy.getDimensionConfigObjects(false)) {
            CommonProxy.getDimensionConfigObjects(false).forEachEntry((dimId, dimCfg) -> {
                if (DimensionManager.isDimensionRegistered(dimId)) {
                    FMLLog.info("Deregistering PersonalSpace dimension %d", dimId);
                    DimensionManager.unregisterDimension(dimId);
                    if (DimensionManager.unregisterProviderType(dimId).length > 0) {
                        FMLLog.severe(
                                "PersonalSpace dimension id %d has other dimension ids registered for the same provider",
                                dimId);
                    }
                }
                return true;
            });
            CommonProxy.getDimensionConfigObjects(false).clear();
        }
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
        deregisterServerDimensions();
        synchronized (CommonProxy.getDimensionConfigObjects(true)) {
            CommonProxy.getDimensionConfigObjects(true).clear();
        }
    }

    @SubscribeEvent
    public void netEventHandler(FMLNetworkEvent.CustomNetworkEvent event) {
        if (event.wrappedEvent instanceof NetworkHandshakeEstablished) {
            NetworkHandshakeEstablished hs = (NetworkHandshakeEstablished) event.wrappedEvent;
            if (hs.netHandler instanceof NetHandlerPlayServer) {
                NetHandlerPlayServer netHandler = (NetHandlerPlayServer) hs.netHandler;
                PacketCustom pkt = Packets.INSTANCE.sendWorldList();
                netHandler.sendPacket(pkt.toPacket());
            }
        }
    }

    @Mod.EventHandler
    public void missingMapping(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping mapping : event.getAll()) {
            if (mapping.type == GameRegistry.Type.BLOCK) {
                switch (mapping.name) {
                    case "utilityworlds:uw_portal_mining":
                        mapping.remap(GameRegistry.findBlock(Tags.MODID, "personalPortal"));
                        break;
                    case "utilityworlds:uw_portal_void":
                        mapping.remap(GameRegistry.findBlock(Tags.MODID, "personalPortal_migration2"));
                        break;
                    case "utilityworlds:uw_portal_garden":
                        mapping.remap(GameRegistry.findBlock(Tags.MODID, "personalPortal_migration3"));
                        break;
                    case "utilityworlds:uw_portal_return":
                        mapping.remap(GameRegistry.findBlock(Tags.MODID, "personalPortal_migration4"));
                        break;
                    default:
                }
            } else if (mapping.type == GameRegistry.Type.ITEM) {
                switch (mapping.name) {
                    case "utilityworlds:uw_portal_mining":
                        mapping.remap(GameRegistry.findItem(Tags.MODID, "personalPortal"));
                        break;
                    case "utilityworlds:uw_portal_void":
                        mapping.remap(GameRegistry.findItem(Tags.MODID, "personalPortal_migration2"));
                        break;
                    case "utilityworlds:uw_portal_garden":
                        mapping.remap(GameRegistry.findItem(Tags.MODID, "personalPortal_migration3"));
                        break;
                    case "utilityworlds:uw_portal_return":
                        mapping.remap(GameRegistry.findItem(Tags.MODID, "personalPortal_migration4"));
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
