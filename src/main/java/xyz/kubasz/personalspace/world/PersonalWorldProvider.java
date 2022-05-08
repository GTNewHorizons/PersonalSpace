package xyz.kubasz.personalspace.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Based on WorldProviderEnd
 */
public class PersonalWorldProvider extends WorldProvider {
    DimensionConfig config;

    public PersonalWorldProvider() {
        // Called by Forge, followed by setDimension
    }

    @Override
    public void setDimension(int dim) {
        // Called by Forge/Vanilla immediately after the constructor
        super.setDimension(dim);
    }

    public DimensionConfig getConfig() {
        return this.config;
    }

    @Override
    public String getDimensionName() {
        return "Personal World " + this.dimensionId;
    }

    public void registerWorldChunkManager() {
        this.config = DimensionConfig.getForDimension(this.dimensionId, this.worldObj.isRemote);
        this.worldChunkMgr = new WorldChunkManagerHell(BiomeGenBase.plains, 0.0F);
    }

    public IChunkProvider createChunkGenerator() {
        return new PersonalChunkProvider(this, this.worldObj.getSeed());
    }

    @SideOnly(Side.CLIENT)
    public Vec3 getFogColor(float sunAngle, float timeSinceLastTick) {
        int baseColor = this.config.getSkyColor();

        float red = (float) (baseColor >> 16 & 255) / 255.0F;
        float green = (float) (baseColor >> 8 & 255) / 255.0F;
        float blue = (float) (baseColor & 255) / 255.0F;
        return Vec3.createVectorHelper(red, green, blue);
    }

    @Override
    public ChunkCoordinates getSpawnPoint() {
        return new ChunkCoordinates(8, getConfig().getGroundLevel() + 2, 8);
    }

    @Override
    public ChunkCoordinates getRandomizedSpawnPoint() {
        return getSpawnPoint();
    }

    @Override
    public Vec3 getSkyColor(Entity cameraEntity, float partialTicks) {
        return getFogColor(0.0f, partialTicks);
    }

    public boolean canRespawnHere() {
        return true;
    }

    public boolean isSurfaceWorld() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    public float getCloudHeight() {
        return 256.0F;
    }

    public boolean canCoordinateBeSpawn(int x, int z) {
        return this.worldObj.getTopBlock(x, z).getMaterial().blocksMovement();
    }

    public int getAverageGroundLevel() {
        return this.config.getGroundLevel();
    }

    @SideOnly(Side.CLIENT)
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }

    @Override
    public boolean getWorldHasVoidParticles() {
        return false;
    }

    @Override
    public double getVoidFogYFactor() {
        return 1.0F;
    }

    @Override
    public String getSaveFolder() {
        return config.getSaveDir(this.dimensionId);
    }

    @Override
    public boolean isDaytime() {
        return true;
    }

    @Override
    public float getSunBrightnessFactor(float par1) {
        return 1.0F;
    }

    @Override
    public float getSunBrightness(float par1) {
        return 1.0F;
    }

    @Override
    public float getStarBrightness(float par1) {
        return config.getStarBrightness();
    }

    @Override
    public float calculateCelestialAngle(long p_76563_1_, float p_76563_3_) {
        return 0.0f;
    }

    @Override
    public void calculateInitialWeather() {
        this.worldObj.rainingStrength = 0.0F;
        this.worldObj.thunderingStrength = 0.0F;
    }

    @Override
    public void updateWeather() {
        if (!this.worldObj.isRemote) {
            this.resetRainAndThunder();
        }
    }

    @Override
    public boolean canDoLightning(Chunk chunk) {
        return false;
    }

    @Override
    public boolean canDoRainSnowIce(Chunk chunk) {
        return false;
    }

    @Override
    public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
        return false;
    }

    @Override
    public boolean canSnowAt(int x, int y, int z, boolean checkLight) {
        return false;
    }

    @Override
    public ChunkCoordinates getEntrancePortalLocation() {
        return new ChunkCoordinates(8, getAverageGroundLevel(), 8);
    }
}
