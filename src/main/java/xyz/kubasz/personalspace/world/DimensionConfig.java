package xyz.kubasz.personalspace.world;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraftforge.common.DimensionManager;
import org.apache.commons.lang3.tuple.MutablePair;
import xyz.kubasz.personalspace.CommonProxy;
import xyz.kubasz.personalspace.Config;
import xyz.kubasz.personalspace.PersonalSpaceMod;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Current world generation settings for a given dimension
 */
public class DimensionConfig {

    private String saveDirOverride = "";
    private int skyColor = 0xc0d8ff;
    private float starBrightness = 1.0F;
    private boolean generatingVegetation = false;
    private boolean generatingTrees = false;
    private boolean allowGenerationChanges = false;
    private List<FlatLayerInfo> layers = Collections.emptyList();

    private boolean needsSaving = true;

    public static final String PRESET_UW_VOID = "";
    public static final String PRESET_UW_GARDEN = "minecraft:bedrock;minecraft:dirt*3;minecraft:grass";
    public static final String PRESET_UW_MINING = "minecraft:bedrock*4;minecraft:stone*58;minecraft:dirt;minecraft:grass";
    public static final Pattern PRESET_VALIDATION_PATTERN = Pattern.compile("^([^:\\*;]+:[^:\\*;]+(\\*\\d+)?;)+([^:\\*;]+:[^:\\*;]+(\\*\\d+)?)$");

    public DimensionConfig() {
    }

    public void writeToPacket(MCDataOutput pkt) {
        pkt.writeString(saveDirOverride);
        pkt.writeInt(skyColor);
        pkt.writeFloat(starBrightness);
        pkt.writeBoolean(generatingVegetation);
        pkt.writeBoolean(generatingTrees);
        pkt.writeBoolean(allowGenerationChanges);
        pkt.writeVarInt(layers.size());
        for (FlatLayerInfo info : layers) {
            pkt.writeVarInt(Block.getIdFromBlock(info.func_151536_b()));
            pkt.writeVarInt(info.getLayerCount());
        }
    }

    public void readFromPacket(MCDataInput pkt) {
        this.saveDirOverride = pkt.readString();
        this.needsSaving = true;
        this.setSkyColor(pkt.readInt());
        this.setStarBrightness(pkt.readFloat());
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
    }

    public void writeToDataStream(DataOutputStream stream) throws IOException {
        PacketCustom pc = new PacketCustom(PersonalSpaceMod.CHANNEL, 1);
        writeToPacket(pc);
        ByteBuf buf = pc.getByteBuf();
        stream.writeInt(buf.writerIndex());
        stream.write(buf.array(), 0, buf.writerIndex());
        needsSaving = false;
    }

    public void readFromDataStream(DataInputStream stream) throws IOException {
        int len = stream.readInt();
        byte[] bytes = new byte[len];
        if (stream.read(bytes) != len) {
            throw new IOException("Couldn't read all bytes of DimensionConfig");
        }
        PacketCustom pc = new PacketCustom(Unpooled.wrappedBuffer(bytes));
        readFromPacket(pc);
    }

    public static DimensionConfig fromPacket(MCDataInput pkt) {
        DimensionConfig cfg = new DimensionConfig();
        cfg.readFromPacket(pkt);
        return cfg;
    }

    public static DimensionConfig fromDataStream(DataInputStream stream) throws IOException {
        DimensionConfig cfg = new DimensionConfig();
        cfg.readFromDataStream(stream);
        cfg.needsSaving = false;
        return cfg;
    }

    public void copyFrom(DimensionConfig source, boolean copySaveInfo, boolean copyVisualInfo, boolean copyGenerationInfo) {
        this.needsSaving = true;
        if (copySaveInfo) {
            this.saveDirOverride = source.saveDirOverride;
        }
        if (copyVisualInfo) {
            this.setSkyColor(source.getSkyColor());
            this.setStarBrightness(source.getStarBrightness());
        }
        if (copyGenerationInfo) {
            this.setAllowGenerationChanges(source.getAllowGenerationChanges());
            this.setGeneratingTrees(source.isGeneratingTrees());
            this.setGeneratingVegetation(source.isGeneratingVegetation());
            this.layers = source.layers;
        }
    }

    public void registerWithDimensionManager(int dimId, boolean isClient) {
        if (!DimensionManager.isDimensionRegistered(dimId)) {
            DimensionManager.registerProviderType(dimId, PersonalWorldProvider.class, false);
            DimensionManager.registerDimension(dimId, dimId);
        }
        synchronized (CommonProxy.getDimensionConfigObjects(isClient)) {
            if (!CommonProxy.getDimensionConfigObjects(isClient).containsKey(dimId)) {
                CommonProxy.getDimensionConfigObjects(isClient).put(dimId, this);
            } else {
                CommonProxy.getDimensionConfigObjects(isClient).get(dimId).copyFrom(this, false, true, true);
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

    public String getSaveDir(int dimId) {
        return (saveDirOverride != null && saveDirOverride.length() > 0) ? saveDirOverride : String.format("PERSONAL_DIM_%d", dimId);
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

    public static List<FlatLayerInfo> parseLayers(String preset) {
        if (preset == null) {
            return Collections.emptyList();
        }
        preset = preset.replaceAll("\\s+", "");
        if (preset.length() < 1 || !PRESET_VALIDATION_PATTERN.matcher(preset).matches()) {
            return Collections.emptyList();
        }
        List<FlatLayerInfo> infos = new ArrayList<>();
        int currY = 0;
        for (String layerStr : preset.split(";")) {
            if (layerStr.isEmpty()) {
                continue;
            }
            String[] components = layerStr.split("\\*", 2);
            String[] blockName = components[0].split(":");
            if (blockName.length != 2) {
                return Collections.emptyList();
            }
            int blockCount = 1;
            if (components.length > 1) {
                try {
                    blockCount = Integer.parseInt(components[1]);
                } catch (NumberFormatException nfe) {
                    return Collections.emptyList();
                }
            }
            blockCount = MathHelper.clamp_int(blockCount, 1, 255);
            Block block = GameRegistry.findBlock(blockName[0], blockName[1]);
            if (block == null) {
                block = Blocks.dirt;
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

    public static boolean canUseLayers(String preset) {
        if (preset.equals(PRESET_UW_GARDEN) || preset.equals(PRESET_UW_VOID) || preset.equals(PRESET_UW_MINING)) {
            return true;
        }
        List<FlatLayerInfo> infos = parseLayers(preset);
        for (FlatLayerInfo info : infos) {
            String block = GameRegistry.findUniqueIdentifierFor(info.func_151536_b()).toString();
            if (!Config.allowedBlocks.contains(block)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Doesn't check if the layers are valid, make sure to call canUseLayers on user-provided input
     */
    public void setLayers(String preset) {
        this.layers = parseLayers(preset);
    }

    /**
     * @param name Original UW save folder name
     * @return Dimension config generating a UW-compatible world, and the dimension ID of the original world; or null if it's not a UW world
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
        int y = 0;
        for (FlatLayerInfo info : layers) {
            y += info.getLayerCount();
        }
        return MathHelper.clamp_int(y, 0, 255);
    }
}
