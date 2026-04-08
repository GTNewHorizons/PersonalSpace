package me.eigenraven.personalspace.world;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.commons.lang3.tuple.MutablePair;

import com.google.common.collect.Lists;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import cpw.mods.fml.common.registry.GameRegistry;
import me.eigenraven.personalspace.CommonProxy;
import me.eigenraven.personalspace.PersonalSpaceMod;
import me.eigenraven.personalspace.config.Config;

/**
 * Current world generation settings for a given dimension
 */
public class DimensionConfig {

    public enum SkyType {

        VANILLA(null, null),
        BARNADA_C("galaxyspace.BarnardsSystem.planets.barnardaC.dimension.sky.SkyProviderBarnardaC",
                "galaxyspace.BarnardsSystem.planets.barnardaC.dimension.sky.CloudProviderBarnardaC"),
        GARDEN_OF_GLASS("vazkii.botania.client.render.world.SkyblockSkyRenderer", null),;

        public final String skyProvider, cloudProvider;
        private Boolean isLoaded = null;

        SkyType(String skyProvider, String cloudProvider) {
            this.skyProvider = skyProvider;
            this.cloudProvider = cloudProvider;
        }

        public static SkyType fromOrdinal(int ord) {
            return (ord < 0 || ord >= values().length) ? SkyType.VANILLA : values()[ord];
        }

        public boolean isLoaded() {
            if (skyProvider == null && cloudProvider == null) {
                return true;
            }
            if (isLoaded == null) {
                try {
                    Class<?> skyClass;
                    if (skyProvider != null) skyClass = Class.forName(skyProvider);
                    Class<?> cloudClass;
                    if (cloudProvider != null) cloudClass = Class.forName(cloudProvider);
                    isLoaded = Boolean.TRUE;
                } catch (ClassNotFoundException e) {
                    isLoaded = Boolean.FALSE;
                }
            }
            return isLoaded;
        }

        private Object makeClass(String name) {
            if (name == null || !isLoaded()) {
                return null;
            }
            try {
                Class<?> klass = Class.forName(name);
                return klass.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public Object makeSkyProvider() {
            return makeClass(skyProvider);
        }

        public Object makeCloudProvider() {
            return makeClass(cloudProvider);
        }

        public String getButtonText() {
            return this.toString().substring(0, 1);
        }

        public String getButtonTooltip() {
            return StatCollector.translateToLocal("gui.personalWorld.skyType." + this);
        }
    }

    public enum DaylightCycle {

        SUN,
        MOON,
        CYCLE;

        DaylightCycle() {

        }

        public static DaylightCycle fromOrdinal(int ordinal) {
            return (ordinal < 0 || ordinal >= values().length) ? DaylightCycle.CYCLE : values()[ordinal];
        }
    }

    public enum GapPreset {

        ROAD,
        SOLID;

        GapPreset() {}

        public static GapPreset fromOrdinal(int ordinal) {
            return (ordinal < 0 || ordinal >= values().length) ? GapPreset.ROAD : values()[ordinal];
        }
    }

    public enum CenterDirection {

        SE, // x+ z+ (default)
        SW, // x- z+
        NE, // x+ z-
        NW; // x- z-

        CenterDirection() {}

        public static CenterDirection fromOrdinal(int ordinal) {
            return (ordinal < 0 || ordinal >= values().length) ? CenterDirection.SE : values()[ordinal];
        }
    }

    private String saveDirOverride = "";
    private int skyColor = 0xc0d8ff;
    private float starBrightness = 1.0F;
    private boolean weatherEnabled = false;
    private DaylightCycle daylightCycle = DaylightCycle.CYCLE;
    private boolean cloudsEnabled = true;
    private SkyType skyType = SkyType.VANILLA;
    private boolean generatingVegetation = false;
    private boolean generatingTrees = false;
    private boolean allowGenerationChanges = false;
    private String biomeId = "Plains";
    private ArrayList<FlatLayerInfo> layers = Lists.newArrayList();

    private String boundaryBlockA = "minecraft:wool";
    private int boundaryMetaA = 4;
    private String boundaryBlockB = "minecraft:wool";
    private int boundaryMetaB = 15;
    private int boundaryChunkIntervalX = 0;
    private int boundaryChunkIntervalZ = 0;

    private int gapWidth = 0;
    private GapPreset gapPreset = GapPreset.ROAD;
    private String gapBlockA = "minecraft:wool";
    private int gapMetaA = 15;
    private String gapBlockB = "minecraft:wool";
    private int gapMetaB = 0;

    private boolean centerEnabled = false;
    private CenterDirection centerDirection = CenterDirection.SE;
    private String centerBlock = "minecraft:wool";
    private int centerMeta = 14;

    private boolean needsSaving = true;

    public static final String PRESET_UW_VOID = "";
    public static final String PRESET_UW_GARDEN = "minecraft:bedrock;minecraft:dirt*3;minecraft:grass";
    public static final String PRESET_UW_MINING = "minecraft:bedrock*4;minecraft:stone*58;minecraft:dirt;minecraft:grass";
    public static final Pattern PRESET_VALIDATION_PATTERN = Pattern
            .compile("^([^:\\*;]+:[^:\\*;]+(\\*\\d+)?;)*([^:\\*;]+:[^:\\*;]+(\\*\\d+)?)?$");

    public DimensionConfig() {}

    public void writeToPacket(MCDataOutput pkt) {
        pkt.writeString(saveDirOverride);
        pkt.writeInt(skyColor);
        pkt.writeFloat(starBrightness);
        pkt.writeVarInt(getRawBiomeId());
        pkt.writeVarInt(daylightCycle.ordinal());
        pkt.writeBoolean(cloudsEnabled);
        pkt.writeVarInt(skyType.ordinal());
        pkt.writeBoolean(weatherEnabled);
        pkt.writeBoolean(generatingVegetation);
        pkt.writeBoolean(generatingTrees);
        pkt.writeBoolean(allowGenerationChanges);
        pkt.writeVarInt(layers.size());
        for (FlatLayerInfo info : layers) {
            pkt.writeVarInt(Block.getIdFromBlock(info.func_151536_b()));
            pkt.writeVarInt(info.getLayerCount());
        }

        pkt.writeString(boundaryBlockA == null ? "" : boundaryBlockA);
        pkt.writeVarInt(boundaryMetaA);
        pkt.writeString(boundaryBlockB == null ? "" : boundaryBlockB);
        pkt.writeVarInt(boundaryMetaB);
        pkt.writeVarInt(boundaryChunkIntervalX);
        pkt.writeVarInt(boundaryChunkIntervalZ);

        pkt.writeVarInt(gapWidth);
        pkt.writeVarInt(gapPreset.ordinal());
        pkt.writeString(gapBlockA == null ? "" : gapBlockA);
        pkt.writeVarInt(gapMetaA);
        pkt.writeString(gapBlockB == null ? "" : gapBlockB);
        pkt.writeVarInt(gapMetaB);

        pkt.writeBoolean(centerEnabled);
        pkt.writeVarInt(centerDirection.ordinal());
        pkt.writeString(centerBlock == null ? "" : centerBlock);
        pkt.writeVarInt(centerMeta);
    }

    public void readFromPacket(MCDataInput pkt) {
        this.saveDirOverride = pkt.readString();
        this.needsSaving = true;
        this.setSkyColor(pkt.readInt());
        this.setStarBrightness(pkt.readFloat());
        this.setBiomeId(BiomeGenBase.getBiomeGenArray()[pkt.readVarInt()].biomeName);
        this.setDaylightCycle(DaylightCycle.fromOrdinal(pkt.readVarInt()));
        this.setCloudsEnabled(pkt.readBoolean());
        this.setSkyType(SkyType.fromOrdinal(pkt.readVarInt()));
        this.setWeatherEnabled(pkt.readBoolean());
        this.setGeneratingVegetation(pkt.readBoolean());
        this.setGeneratingTrees(pkt.readBoolean());
        this.setAllowGenerationChanges(pkt.readBoolean());
        int layerCount = pkt.readVarInt();
        ArrayList<FlatLayerInfo> layers = new ArrayList<>(layerCount);
        int y = 0;
        for (int layerI = 0; layerI < layerCount; ++layerI) {
            int blockId = pkt.readVarInt();
            int count = pkt.readVarInt();
            FlatLayerInfo info = new FlatLayerInfo(count, Block.getBlockById(blockId));
            info.setMinY(y);
            layers.add(info);
            y += count;
        }
        this.layers = layers;

        this.setBoundaryBlockA(pkt.readString());
        this.setBoundaryMetaA(pkt.readVarInt());
        this.setBoundaryBlockB(pkt.readString());
        this.setBoundaryMetaB(pkt.readVarInt());
        this.setBoundaryChunkIntervalX(pkt.readVarInt());
        this.setBoundaryChunkIntervalZ(pkt.readVarInt());

        this.setGapWidth(pkt.readVarInt());
        this.setGapPreset(GapPreset.fromOrdinal(pkt.readVarInt()));
        this.setGapBlockA(pkt.readString());
        this.setGapMetaA(pkt.readVarInt());
        this.setGapBlockB(pkt.readString());
        this.setGapMetaB(pkt.readVarInt());

        this.setCenterEnabled(pkt.readBoolean());
        this.setCenterDirection(CenterDirection.fromOrdinal(pkt.readVarInt()));
        this.setCenterBlock(pkt.readString());
        this.setCenterMeta(pkt.readVarInt());
    }

    /**
     * @return Loaded dimension ID
     */
    public int syncWithFile(File file, boolean write, int dimId) {
        final String VISUAL = "visual";
        final String WORLDGEN = "worldgen";
        final String BOUNDARY = "boundary";
        Configuration cfg = new Configuration(file);
        Property cur;

        cur = cfg.get(VISUAL, "skyColor", skyColor, "", 0, 0xFFFFFF);
        if (write) {
            cur.set(skyColor);
        } else {
            setSkyColor(cur.getInt());
        }

        cur = cfg.get(VISUAL, "starBrightness", starBrightness, "", 0.0F, 1.0F);
        if (write) {
            cur.set(starBrightness);
        } else {
            setStarBrightness((float) cur.getDouble());
        }

        cur = cfg.get(WORLDGEN, "biomeId", getBiomeId());
        if (write) {
            cur.set(getBiomeId());
        } else {
            setBiomeId(cur.getString());
        }

        cur = cfg.get(VISUAL, "weatherEnabled", weatherEnabled, "");
        if (write) {
            cur.set(weatherEnabled);
        } else {
            setWeatherEnabled(cur.getBoolean());
        }

        cur = cfg.get(VISUAL, "daylightCycle", daylightCycle.ordinal(), "");
        if (write) {
            cur.set(daylightCycle.ordinal());
        } else {
            setDaylightCycle(DaylightCycle.fromOrdinal(cur.getInt()));
        }

        // handle old nightTime config
        if (cfg.hasKey(VISUAL, "nightTime")) {
            if (write) {
                cfg.getCategory(VISUAL).remove("nightTime");
            } else {
                boolean isNight = cfg.getCategory(VISUAL).get("nightTime").getBoolean();
                setDaylightCycle(isNight ? DaylightCycle.MOON : DaylightCycle.SUN);
                needsSaving = true;
            }
        }

        cur = cfg.get(VISUAL, "cloudsEnabled", cloudsEnabled, "");
        if (write) {
            cur.set(cloudsEnabled);
        } else {
            setCloudsEnabled(cur.getBoolean());
        }

        cur = cfg.get(VISUAL, "skyType", skyType.ordinal(), "");
        if (write) {
            cur.set(skyType.ordinal());
        } else {
            setSkyType(SkyType.fromOrdinal(cur.getInt()));
        }

        cur = cfg.get(WORLDGEN, "generatingTrees", generatingTrees, "");
        if (write) {
            cur.set(generatingTrees);
        } else {
            setGeneratingTrees(cur.getBoolean());
        }

        cur = cfg.get(WORLDGEN, "generatingVegetation", generatingVegetation, "");
        if (write) {
            cur.set(generatingVegetation);
        } else {
            setGeneratingVegetation(cur.getBoolean());
        }

        cur = cfg.get(
                WORLDGEN,
                "allowGenerationChanges",
                allowGenerationChanges,
                "One-time-use permission to change generation settings on the world");
        if (write) {
            cur.set(allowGenerationChanges);
        } else {
            setAllowGenerationChanges(cur.getBoolean());
        }

        cur = cfg.get(WORLDGEN, "layers", getLayersAsString());
        if (write) {
            cur.set(getLayersAsString());
        } else {
            setLayers(cur.getString());
        }

        cur = cfg.get(BOUNDARY, "blockA", getBoundaryBlockA());
        if (write) {
            cur.set(getBoundaryBlockA());
        } else {
            setBoundaryBlockA(cur.getString());
        }

        cur = cfg.get(BOUNDARY, "metaA", getBoundaryMetaA(), "", 0, 15);
        if (write) {
            cur.set(getBoundaryMetaA());
        } else {
            setBoundaryMetaA(cur.getInt());
        }

        cur = cfg.get(BOUNDARY, "blockB", getBoundaryBlockB());
        if (write) {
            cur.set(getBoundaryBlockB());
        } else {
            setBoundaryBlockB(cur.getString());
        }

        cur = cfg.get(BOUNDARY, "metaB", getBoundaryMetaB(), "", 0, 15);
        if (write) {
            cur.set(getBoundaryMetaB());
        } else {
            setBoundaryMetaB(cur.getInt());
        }

        cur = cfg.get(BOUNDARY, "chunkIntervalX", getBoundaryChunkIntervalX(), "", 0, 20);
        if (write) {
            cur.set(getBoundaryChunkIntervalX());
        } else {
            setBoundaryChunkIntervalX(cur.getInt());
        }

        cur = cfg.get(BOUNDARY, "chunkIntervalZ", getBoundaryChunkIntervalZ(), "", 0, 20);
        if (write) {
            cur.set(getBoundaryChunkIntervalZ());
        } else {
            setBoundaryChunkIntervalZ(cur.getInt());
        }

        final String GAP = "gap";

        cur = cfg.get(GAP, "width", getGapWidth(), "", 0, 5);
        if (write) {
            cur.set(getGapWidth());
        } else {
            setGapWidth(cur.getInt());
        }

        cur = cfg.get(GAP, "preset", getGapPreset().ordinal(), "");
        if (write) {
            cur.set(getGapPreset().ordinal());
        } else {
            setGapPreset(GapPreset.fromOrdinal(cur.getInt()));
        }

        cur = cfg.get(GAP, "blockA", getGapBlockA());
        if (write) {
            cur.set(getGapBlockA());
        } else {
            setGapBlockA(cur.getString());
        }

        cur = cfg.get(GAP, "metaA", getGapMetaA(), "", 0, 15);
        if (write) {
            cur.set(getGapMetaA());
        } else {
            setGapMetaA(cur.getInt());
        }

        cur = cfg.get(GAP, "blockB", getGapBlockB());
        if (write) {
            cur.set(getGapBlockB());
        } else {
            setGapBlockB(cur.getString());
        }

        cur = cfg.get(GAP, "metaB", getGapMetaB(), "", 0, 15);
        if (write) {
            cur.set(getGapMetaB());
        } else {
            setGapMetaB(cur.getInt());
        }

        final String CENTER = "center";

        cur = cfg.get(CENTER, "enabled", isCenterEnabled(), "");
        if (write) {
            cur.set(isCenterEnabled());
        } else {
            setCenterEnabled(cur.getBoolean());
        }

        cur = cfg.get(CENTER, "direction", getCenterDirection().ordinal(), "");
        if (write) {
            cur.set(getCenterDirection().ordinal());
        } else {
            setCenterDirection(CenterDirection.fromOrdinal(cur.getInt()));
        }

        cur = cfg.get(CENTER, "block", getCenterBlock());
        if (write) {
            cur.set(getCenterBlock());
        } else {
            setCenterBlock(cur.getString());
        }

        cur = cfg.get(CENTER, "meta", getCenterMeta(), "", 0, 15);
        if (write) {
            cur.set(getCenterMeta());
        } else {
            setCenterMeta(cur.getInt());
        }

        cur = cfg.get(WORLDGEN, "dimId", dimId);
        if (write) {
            cur.set(dimId);
        } else {
            dimId = cur.getInt();
        }

        if (write) {
            cfg.save();
            needsSaving = false;
        }
        return dimId;
    }

    public static DimensionConfig fromPacket(MCDataInput pkt) {
        DimensionConfig cfg = new DimensionConfig();
        cfg.readFromPacket(pkt);
        return cfg;
    }

    public boolean copyFrom(DimensionConfig source, boolean copySaveInfo, boolean copyVisualInfo,
            boolean copyGenerationInfo) {
        this.needsSaving = false;
        if (copySaveInfo) {
            this.saveDirOverride = source.saveDirOverride;
        }
        if (copyVisualInfo) {
            this.setSkyColor(source.getSkyColor());
            this.setStarBrightness(source.getStarBrightness());
            this.setDaylightCycle(source.getDaylightCycle());
            this.setCloudsEnabled(source.isCloudsEnabled());
            this.setSkyType(source.getSkyType());
            this.setWeatherEnabled(source.isWeatherEnabled());
        }
        if (copyGenerationInfo) {
            this.setAllowGenerationChanges(source.getAllowGenerationChanges());
            this.setBiomeId(source.getBiomeId());
            this.setGeneratingTrees(source.isGeneratingTrees());
            this.setGeneratingVegetation(source.isGeneratingVegetation());
            this.layers = source.layers;

            this.setBoundaryBlockA(source.getBoundaryBlockA());
            this.setBoundaryMetaA(source.getBoundaryMetaA());
            this.setBoundaryBlockB(source.getBoundaryBlockB());
            this.setBoundaryMetaB(source.getBoundaryMetaB());
            this.setBoundaryChunkIntervalX(source.getBoundaryChunkIntervalX());
            this.setBoundaryChunkIntervalZ(source.getBoundaryChunkIntervalZ());

            this.setGapWidth(source.getGapWidth());
            this.setGapPreset(source.getGapPreset());
            this.setGapBlockA(source.getGapBlockA());
            this.setGapMetaA(source.getGapMetaA());
            this.setGapBlockB(source.getGapBlockB());
            this.setGapMetaB(source.getGapMetaB());

            this.setCenterEnabled(source.isCenterEnabled());
            this.setCenterDirection(source.getCenterDirection());
            this.setCenterBlock(source.getCenterBlock());
            this.setCenterMeta(source.getCenterMeta());

            this.needsSaving = true;
        }
        boolean modified = this.needsSaving;
        this.needsSaving = true;
        return modified;
    }

    @SuppressWarnings("unchecked")
    public void registerWithDimensionManager(int dimId, boolean isClient) {
        if (!DimensionManager.isDimensionRegistered(dimId)) {
            DimensionManager.registerProviderType(dimId, PersonalWorldProvider.class, false);
            // Work around bad thermos logic
            if (PersonalSpaceMod.isInThermos()) {
                try {
                    Class<?> bukkitWorldEnv = Class.forName("org.bukkit.World$Environment");
                    Field lookupField = bukkitWorldEnv.getDeclaredField("lookup");
                    lookupField.setAccessible(true);
                    Map<Integer, ?> lookup = (Map<Integer, ?>) lookupField.get(null);
                    if (lookup.remove(dimId) != null) {
                        PersonalSpaceMod.LOG
                                .info("Removed bad thermos environment lookup entry for dimension {}", dimId);
                    }
                } catch (Exception e) {
                    PersonalSpaceMod.LOG.error("Couldn't adjust thermos environment lookup table", e);
                }
            }
            DimensionManager.registerDimension(dimId, dimId);
            if (Config.debugLogging) {
                PersonalSpaceMod.LOG
                        .info("DimensionConfig registered for dim {}, client {}", dimId, isClient, new Throwable());
            }
        }
        synchronized (CommonProxy.getDimensionConfigObjects(isClient)) {
            if (!CommonProxy.getDimensionConfigObjects(isClient).containsKey(dimId)) {
                CommonProxy.getDimensionConfigObjects(isClient).put(dimId, this);
            } else {
                CommonProxy.getDimensionConfigObjects(isClient).get(dimId).copyFrom(this, true, true, true);
            }
        }
    }

    public static int nextFreeDimId() {
        AtomicInteger nextIdV = new AtomicInteger(Config.firstDimensionId);
        CommonProxy.getDimensionConfigObjects(false).forEachKey(id -> {
            if (nextIdV.get() <= id) {
                nextIdV.set(id + 1);
            }
            return true;
        });
        int nextId = nextIdV.get();
        while (DimensionManager.isDimensionRegistered(nextId) && nextId < Integer.MAX_VALUE - 1) {
            ++nextId;
        }
        assert !DimensionManager.isDimensionRegistered(nextId);
        return nextId;
    }

    public static DimensionConfig getForDimension(int dimId, boolean isClient) {
        synchronized (CommonProxy.getDimensionConfigObjects(isClient)) {
            return CommonProxy.getDimensionConfigObjects(isClient).get(dimId);
        }
    }

    public void setSaveDirOverride(String override) {
        if (override == null) {
            override = "";
        }
        this.saveDirOverride = override;
    }

    public String getSaveDir(int dimId) {
        return (saveDirOverride != null && !saveDirOverride.isEmpty()) ? saveDirOverride
                : String.format("PERSONAL_DIM_%d", dimId);
    }

    public float getStarBrightness() {
        return starBrightness;
    }

    public void setStarBrightness(float starBrightness) {
        if (starBrightness != this.starBrightness) {
            this.needsSaving = true;
            this.starBrightness = MathHelper.clamp_float(starBrightness, 0.0F, 1.0F);
        }
    }

    public int getSkyColor() {
        return skyColor;
    }

    public void setSkyColor(int skyColor) {
        if (skyColor != this.skyColor) {
            this.needsSaving = true;
            this.skyColor = MathHelper.clamp_int(skyColor, 0, 0xFFFFFF);
        }
    }

    public boolean isWeatherEnabled() {
        return weatherEnabled;
    }

    public void setWeatherEnabled(boolean weatherEnabled) {
        if (this.weatherEnabled != weatherEnabled) {
            this.needsSaving = true;
            this.weatherEnabled = weatherEnabled;
        }
    }

    public DaylightCycle getDaylightCycle() {
        return daylightCycle;
    }

    public void setDaylightCycle(DaylightCycle cycle) {
        if (this.daylightCycle != cycle) {
            this.needsSaving = true;
            this.daylightCycle = cycle;
        }
    }

    public boolean isNightTime() {
        return daylightCycle == DaylightCycle.MOON;
    }

    public boolean isCloudsEnabled() {
        return cloudsEnabled;
    }

    public void setCloudsEnabled(boolean cloudsEnabled) {
        if (this.cloudsEnabled != cloudsEnabled) {
            this.needsSaving = true;
            this.cloudsEnabled = cloudsEnabled;
        }
    }

    public SkyType getSkyType() {
        return skyType;
    }

    public void setSkyType(SkyType skyType) {
        if (this.skyType != skyType) {
            this.needsSaving = true;
            this.skyType = skyType;
        }
    }

    public boolean isGeneratingVegetation() {
        return generatingVegetation;
    }

    public void setGeneratingVegetation(boolean generatingVegetation) {
        if (this.generatingVegetation != generatingVegetation) {
            this.needsSaving = true;
            this.generatingVegetation = generatingVegetation;
        }
    }

    public boolean isGeneratingTrees() {
        return generatingTrees;
    }

    public void setGeneratingTrees(boolean generatingTrees) {
        if (this.generatingTrees != generatingTrees) {
            this.needsSaving = true;
            this.generatingTrees = generatingTrees;
        }
    }

    public String getBiomeId() {
        return biomeId;
    }

    public int getRawBiomeId() {
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        for (int i = 0; i < biomes.length; i++) {
            if (biomes[i] != null && biomes[i].biomeName != null && biomes[i].biomeName.equalsIgnoreCase(biomeId)) {
                return i;
            }
        }
        return 1;
    }

    public void setBiomeId(String biomeId) {
        if (biomeId == null) {
            biomeId = "Plains";
        }
        if (!this.biomeId.equalsIgnoreCase(biomeId)) {
            this.needsSaving = true;
            this.biomeId = biomeId;
        }
    }

    public boolean getAllowGenerationChanges() {
        return allowGenerationChanges;
    }

    public void setAllowGenerationChanges(boolean allowGenerationChanges) {
        if (this.allowGenerationChanges != allowGenerationChanges) {
            this.needsSaving = true;
            this.allowGenerationChanges = allowGenerationChanges;
        }
    }

    public boolean needsSaving() {
        return needsSaving;
    }

    public static ArrayList<FlatLayerInfo> parseLayers(String preset) {
        if (preset == null) {
            return Lists.newArrayList();
        }
        preset = preset.replaceAll("\\s+", "");
        if (preset.isEmpty() || !PRESET_VALIDATION_PATTERN.matcher(preset).matches()) {
            return Lists.newArrayList();
        }
        ArrayList<FlatLayerInfo> infos = new ArrayList<>();
        int currY = 0;
        for (String layerStr : preset.split(";")) {
            if (layerStr.isEmpty()) {
                continue;
            }
            String[] components = layerStr.split("\\*", 2);
            String[] blockName = components[0].split(":");
            if (blockName.length != 2) {
                return Lists.newArrayList();
            }
            int blockCount = 1;
            if (components.length > 1) {
                try {
                    blockCount = Integer.parseInt(components[1]);
                } catch (NumberFormatException nfe) {
                    return Lists.newArrayList();
                }
            }
            blockCount = MathHelper.clamp_int(blockCount, 1, 255);
            Block block = GameRegistry.findBlock(blockName[0], blockName[1]);
            if (block == null) {
                return Lists.newArrayList();
            }
            FlatLayerInfo info = new FlatLayerInfo(blockCount, block, 0);
            info.setMinY(currY);
            infos.add(info);
            currY += blockCount;
            if (currY > 255) {
                break;
            }
        }
        return infos;
    }

    public List<FlatLayerInfo> getLayers() {
        return Collections.unmodifiableList(this.layers);
    }

    public List<FlatLayerInfo> getMutableLayers() {
        return this.layers;
    }

    public static String layersToString(List<FlatLayerInfo> layers) {
        StringBuilder b = new StringBuilder();
        for (FlatLayerInfo info : layers) {
            int count = info.getLayerCount();
            if (count < 1) {
                continue;
            }
            GameRegistry.UniqueIdentifier block = GameRegistry.findUniqueIdentifierFor(info.func_151536_b());
            if (block == null) {
                block = new GameRegistry.UniqueIdentifier("minecraft:air");
            }
            b.append(block.modId);
            b.append(':');
            b.append(block.name);
            if (count > 1) {
                b.append('*');
                b.append(count);
            }
            b.append(';');
        }
        if (b.length() > 0) {
            b.deleteCharAt(b.length() - 1);
        }
        return b.toString();
    }

    public String getLayersAsString() {
        return layersToString(this.layers);
    }

    public static boolean canUseLayers(String preset, boolean onClient) {
        if (preset == null) {
            preset = "";
        }
        if (preset.equals(PRESET_UW_GARDEN) || preset.equals(PRESET_UW_VOID) || preset.equals(PRESET_UW_MINING)) {
            return true;
        }
        List<FlatLayerInfo> infos = parseLayers(preset);
        if (infos.isEmpty() && !preset.trim().isEmpty()) {
            return false;
        }
        for (FlatLayerInfo info : infos) {
            String block = GameRegistry.findUniqueIdentifierFor(info.func_151536_b()).toString();
            if (!(onClient ? PersonalSpaceMod.clientAllowedBlocks.contains(block)
                    : Config.allowedBlocks.contains(block))) {
                return false;
            }
        }
        return true;
    }

    public static boolean canUseBiome(String biome, boolean onClient) {
        if (biome.equalsIgnoreCase("Plains")) {
            return true;
        }
        return onClient ? PersonalSpaceMod.clientAllowedBiomes.contains(biome.toLowerCase())
                : Config.allowedBiomes.contains(biome.toLowerCase());
    }

    /**
     * Doesn't check if the layers are valid, make sure to call canUseLayers on user-provided input
     */
    public void setLayers(String preset) {
        this.layers = parseLayers(preset);
    }

    /**
     * @param name Original UW save folder name
     * @return Dimension config generating a UW-compatible world, and the dimension ID of the original world; or null if
     *         it's not a UW world
     */
    public static MutablePair<DimensionConfig, Integer> fromUtilityWorldsWorld(String name) {
        boolean isMining = name.startsWith("UW_MINING_");
        boolean isGarden = name.startsWith("UW_GARDEN_");
        boolean isVoid = name.startsWith("UW_VOID_");
        if (!(isMining || isGarden || isVoid)) {
            return null;
        }
        String dimIdStr = name.split("_", 3)[2];
        int dimId;
        try {
            dimId = Integer.parseInt(dimIdStr);
        } catch (NumberFormatException nfe) {
            PersonalSpaceMod.LOG.warn("Couldn't parse dimension ID from folder name " + name, nfe);
            return null;
        }
        DimensionConfig cfg = new DimensionConfig();
        cfg.saveDirOverride = name;
        cfg.skyColor = isVoid ? 0x000000 : 0xc0d8ff;
        cfg.starBrightness = isVoid ? 0.0F : 1.0F;
        if (isMining) {
            cfg.setLayers(PRESET_UW_MINING);
        } else if (isGarden) {
            cfg.setLayers(PRESET_UW_GARDEN);
            cfg.generatingVegetation = true;
        } else /* isVoid */ {
            cfg.setLayers(PRESET_UW_VOID);
        }
        return MutablePair.of(cfg, dimId);
    }

    public int getGroundLevel() {
        if (layers.isEmpty()) {
            return 128;
        }
        int y = 0;
        for (FlatLayerInfo info : layers) {
            y += info.getLayerCount();
        }
        return MathHelper.clamp_int(y, 0, 255);
    }

    public static String blockToString(Block block) {
        if (block == null) {
            return "";
        }
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
        if (uid == null) {
            return "";
        }
        return uid.modId + ":" + uid.name;
    }

    public static Block blockFromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String[] sp = name.split(":");
        if (sp.length != 2) {
            return null;
        }
        return GameRegistry.findBlock(sp[0], sp[1]);
    }

    public String getBoundaryBlockA() {
        return boundaryBlockA == null ? "" : boundaryBlockA;
    }

    public void setBoundaryBlockA(String boundaryBlockA) {
        if (boundaryBlockA == null) {
            boundaryBlockA = "";
        }
        if (!this.getBoundaryBlockA().equals(boundaryBlockA)) {
            this.needsSaving = true;
            this.boundaryBlockA = boundaryBlockA;
        }
    }

    public int getBoundaryMetaA() {
        return boundaryMetaA;
    }

    public void setBoundaryMetaA(int boundaryMetaA) {
        int v = Math.max(boundaryMetaA, 0);
        if (this.boundaryMetaA != v) {
            this.needsSaving = true;
            this.boundaryMetaA = v;
        }
    }

    public String getBoundaryBlockB() {
        return boundaryBlockB == null ? "" : boundaryBlockB;
    }

    public void setBoundaryBlockB(String boundaryBlockB) {
        if (boundaryBlockB == null) {
            boundaryBlockB = "";
        }
        if (!this.getBoundaryBlockB().equals(boundaryBlockB)) {
            this.needsSaving = true;
            this.boundaryBlockB = boundaryBlockB;
        }
    }

    public int getBoundaryMetaB() {
        return boundaryMetaB;
    }

    public void setBoundaryMetaB(int boundaryMetaB) {
        int v = Math.max(boundaryMetaB, 0);
        if (this.boundaryMetaB != v) {
            this.needsSaving = true;
            this.boundaryMetaB = v;
        }
    }

    public int getBoundaryChunkIntervalX() {
        return boundaryChunkIntervalX;
    }

    public void setBoundaryChunkIntervalX(int boundaryChunkIntervalX) {
        int v = MathHelper.clamp_int(boundaryChunkIntervalX, 0, 20);
        if (this.boundaryChunkIntervalX != v) {
            this.needsSaving = true;
            this.boundaryChunkIntervalX = v;
        }
    }

    public int getBoundaryChunkIntervalZ() {
        return boundaryChunkIntervalZ;
    }

    public void setBoundaryChunkIntervalZ(int boundaryChunkIntervalZ) {
        int v = MathHelper.clamp_int(boundaryChunkIntervalZ, 0, 20);
        if (this.boundaryChunkIntervalZ != v) {
            this.needsSaving = true;
            this.boundaryChunkIntervalZ = v;
        }
    }

    public Block getBoundaryBlockAResolved() {
        return blockFromString(boundaryBlockA);
    }

    public Block getBoundaryBlockBResolved() {
        return blockFromString(boundaryBlockB);
    }

    public int getGapWidth() {
        return gapWidth;
    }

    public void setGapWidth(int gapWidth) {
        int v = MathHelper.clamp_int(gapWidth, 0, 5);
        if (this.gapWidth != v) {
            this.needsSaving = true;
            this.gapWidth = v;
        }
    }

    public GapPreset getGapPreset() {
        return gapPreset;
    }

    public void setGapPreset(GapPreset gapPreset) {
        if (this.gapPreset != gapPreset) {
            this.needsSaving = true;
            this.gapPreset = gapPreset;
        }
    }

    public String getGapBlockA() {
        return gapBlockA == null ? "" : gapBlockA;
    }

    public void setGapBlockA(String gapBlockA) {
        if (gapBlockA == null) {
            gapBlockA = "";
        }
        if (!this.getGapBlockA().equals(gapBlockA)) {
            this.needsSaving = true;
            this.gapBlockA = gapBlockA;
        }
    }

    public int getGapMetaA() {
        return gapMetaA;
    }

    public void setGapMetaA(int gapMetaA) {
        int v = Math.max(gapMetaA, 0);
        if (this.gapMetaA != v) {
            this.needsSaving = true;
            this.gapMetaA = v;
        }
    }

    public String getGapBlockB() {
        return gapBlockB == null ? "" : gapBlockB;
    }

    public void setGapBlockB(String gapBlockB) {
        if (gapBlockB == null) {
            gapBlockB = "";
        }
        if (!this.getGapBlockB().equals(gapBlockB)) {
            this.needsSaving = true;
            this.gapBlockB = gapBlockB;
        }
    }

    public int getGapMetaB() {
        return gapMetaB;
    }

    public void setGapMetaB(int gapMetaB) {
        int v = Math.max(gapMetaB, 0);
        if (this.gapMetaB != v) {
            this.needsSaving = true;
            this.gapMetaB = v;
        }
    }

    public Block getGapBlockAResolved() {
        return blockFromString(gapBlockA);
    }

    public Block getGapBlockBResolved() {
        return blockFromString(gapBlockB);
    }

    public boolean isCenterEnabled() {
        return centerEnabled;
    }

    public void setCenterEnabled(boolean centerEnabled) {
        if (this.centerEnabled != centerEnabled) {
            this.needsSaving = true;
            this.centerEnabled = centerEnabled;
        }
    }

    public CenterDirection getCenterDirection() {
        return centerDirection;
    }

    public void setCenterDirection(CenterDirection centerDirection) {
        if (this.centerDirection != centerDirection) {
            this.needsSaving = true;
            this.centerDirection = centerDirection;
        }
    }

    public String getCenterBlock() {
        return centerBlock == null ? "" : centerBlock;
    }

    public void setCenterBlock(String centerBlock) {
        if (centerBlock == null) {
            centerBlock = "";
        }
        if (!this.getCenterBlock().equals(centerBlock)) {
            this.needsSaving = true;
            this.centerBlock = centerBlock;
        }
    }

    public int getCenterMeta() {
        return centerMeta;
    }

    public void setCenterMeta(int centerMeta) {
        int v = Math.max(centerMeta, 0);
        if (this.centerMeta != v) {
            this.needsSaving = true;
            this.centerMeta = v;
        }
    }

    public Block getCenterBlockResolved() {
        return blockFromString(centerBlock);
    }
}
