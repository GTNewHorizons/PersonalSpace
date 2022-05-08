package xyz.kubasz.personalspace.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class PersonalTeleporter extends Teleporter {

    int x, y, z;

    public PersonalTeleporter(WorldServer world, int x, int y, int z) {
        super(world);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
        entity.setLocationAndAngles(this.x + 0.2, this.y + 0.1, this.z + 0.2, yaw, 0.0F);
        entity.motionX = 0.0F;
        entity.motionY = 0.0F;
        entity.motionZ = 0.0F;
    }

    @Override
    public boolean placeInExistingPortal(Entity p_77184_1_, double p_77184_2_, double p_77184_4_, double p_77184_6_, float p_77184_8_) {
        return true;
    }

    @Override
    public boolean makePortal(Entity p_85188_1_) {
        return true;
    }

    @Override
    public void removeStalePortalLocations(long p_85189_1_) {
    }
}
