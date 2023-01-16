package me.eigenraven.personalspace.world;

import me.eigenraven.personalspace.block.PortalTileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class PersonalTeleporter extends Teleporter {

    int x, y, z;
    private PortalTileEntity sourceTeleporter;

    public PersonalTeleporter(WorldServer world, int x, int y, int z) {
        super(world);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PersonalTeleporter(PortalTileEntity sourceTeleporter, WorldServer world) {
        super(world);
        this.sourceTeleporter = sourceTeleporter;
        this.x = sourceTeleporter.getTargetTeleportX();
        this.y = sourceTeleporter.getTargetTeleportY();
        this.z = sourceTeleporter.getTargetTeleportZ();
    }

    @Override
    public void placeInPortal(Entity entity, double entityPosX, double entityPosY, double entityPosZ, float yaw) {
        this.placeInExistingPortal(entity, entityPosX, entityPosY, entityPosZ, yaw);
    }

    @Override
    public boolean placeInExistingPortal(
            Entity entity, double entityPosX, double entityPosY, double entityPosZ, float yaw) {
        if (sourceTeleporter != null) {

            double dX = sourceTeleporter.targetPosX - sourceTeleporter.getTargetTeleportX();
            double dZ = sourceTeleporter.targetPosZ - sourceTeleporter.getTargetTeleportZ();
            double distanceXZ = Math.sqrt(dX * dX + dZ * dZ);
            double newYaw = Math.acos(dX / distanceXZ) * 180 / Math.PI - 90;

            if (dZ < 0) {
                newYaw = newYaw - 180;
            }

            yaw = (float) newYaw;
        }

        entity.setLocationAndAngles(x + 0.5, y + 0.1, z + 0.5, yaw, 0.0F);
        entity.motionX = 0.0F;
        entity.motionY = 0.0F;
        entity.motionZ = 0.0F;
        return true;
    }

    @Override
    public boolean makePortal(Entity player) {
        return true;
    }

    @Override
    public void removeStalePortalLocations(long p_85189_1_) {}
}
