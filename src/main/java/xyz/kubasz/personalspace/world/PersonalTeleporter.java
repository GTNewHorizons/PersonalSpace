package xyz.kubasz.personalspace.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import xyz.kubasz.personalspace.block.PortalTileEntity;

public class PersonalTeleporter extends Teleporter {

    int x, y, z;
    private WorldServer targetDimension;
    private PortalTileEntity sourceTeleporter;

    public PersonalTeleporter(WorldServer world, int x, int y, int z) {
        super(world);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PersonalTeleporter(PortalTileEntity sourceTeleporter, WorldServer targetDimension, WorldServer world) {
        super(world);
        this.targetDimension = targetDimension;
        this.sourceTeleporter = sourceTeleporter;
        this.x = sourceTeleporter.getTargetTeleportX();
        this.y = sourceTeleporter.getTargetTeleportY();
        this.z = sourceTeleporter.getTargetTeleportZ();
    }

    @Override
    public void placeInPortal(Entity entity, double entityPosX, double entityPosY, double entityPosZ, float yaw) {
        if (!this.placeInExistingPortal(entity, entityPosX, entityPosY, entityPosZ, yaw)) {
            this.makePortal(entity);
            this.placeInExistingPortal(entity, entityPosX, entityPosY, entityPosZ, yaw);
        }
    }

    @Override
    public boolean placeInExistingPortal(
            Entity entity, double entityPosX, double entityPosY, double entityPosZ, float yaw) {
        if (sourceTeleporter != null) {

            if (!targetPortalExist()) {
                return false;
            }

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

    private boolean targetPortalExist() {
        for (int x = sourceTeleporter.targetPosX - 1; x <= sourceTeleporter.targetPosX + 1; x++) {
            for (int y = sourceTeleporter.targetPosY - 1; y <= sourceTeleporter.targetPosY + 1; y++) {
                if (y < 0 || y > targetDimension.getHeight()) continue;
                for (int z = sourceTeleporter.targetPosZ - 1; z <= sourceTeleporter.targetPosZ + 1; z++) {
                    if (targetDimension.getTileEntity(x, y, z) instanceof PortalTileEntity) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean makePortal(Entity player) {
        sourceTeleporter.linkOtherPortal(true, (EntityPlayerMP) player);
        return true;
    }

    @Override
    public void removeStalePortalLocations(long p_85189_1_) {}
}
