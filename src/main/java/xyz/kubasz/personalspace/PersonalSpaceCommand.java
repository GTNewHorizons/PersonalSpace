package xyz.kubasz.personalspace;

import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import xyz.kubasz.personalspace.net.Packets;
import xyz.kubasz.personalspace.world.DimensionConfig;
import xyz.kubasz.personalspace.world.PersonalTeleporter;

public class PersonalSpaceCommand extends CommandBase {

    public String getCommandName() {
        return "pspace";
    }

    /**
     * Return the required permission level for this command.
     */
    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "commands.pspace.usage";
    }

    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1 || sender == null) {
            throw new WrongUsageException("commands.pspace.usage");
        }
        if (args[0].equalsIgnoreCase("ls")) {
            CommonProxy.getDimensionConfigObjects(false).forEachEntry((dimId, dimCfg) -> {
                if (dimCfg == null) {
                    return true;
                }
                sender.addChatMessage(new ChatComponentText(String.format("%d: %s", dimId, dimCfg.getSaveDir(dimId))));
                return true;
            });
            return;
        }
        if (args[0].equalsIgnoreCase("where")) {
            if (args.length < 2) {
                throw new WrongUsageException("commands.pspace.usage");
            }
            EntityPlayerMP player = getPlayer(sender, args[1]);
            sender.addChatMessage(new ChatComponentTranslation("commands.pspace.where", sender.getCommandSenderName(), player.worldObj.provider.dimensionId));
            return;
        }
        if (args[0].equalsIgnoreCase("tpx")) {
            if (args.length < 3) {
                throw new WrongUsageException("commands.pspace.usage");
            }
            EntityPlayerMP player = getPlayer(sender, args[1]);
            int dim = parseInt(sender, args[2]);
            if (!DimensionManager.isDimensionRegistered(dim)) {
                throw new CommandException("commands.pspace.badDimension");
            }
            WorldServer dimWorld = DimensionManager.getWorld(dim);
            if (dimWorld == null) {
                DimensionManager.initDimension(dim);
                dimWorld = DimensionManager.getWorld(dim);
                if (dimWorld == null) {
                    throw new CommandException("commands.pspace.badDimension");
                }
            }
            ChunkCoordinates target = dimWorld.getSpawnPoint();
            target.posY = dimWorld.getTopSolidOrLiquidBlock(target.posX, target.posZ) + 1;
            double x = target.posX, y = target.posY, z = target.posZ;
            if (args.length >= 6) {
                ChunkCoordinates coords = sender.getPlayerCoordinates();
                x = func_110666_a(sender, x, args[3]);
                y = func_110666_a(sender, y, args[4]);
                z = func_110666_a(sender, z, args[5]);
            }
            PersonalTeleporter tp = new PersonalTeleporter(dimWorld, (int) x, (int) y, (int) z);
            player.mcServer.getConfigurationManager().transferPlayerToDimension(player, dim, tp);
            sender.addChatMessage(new ChatComponentTranslation("commands.pspace.tpx", sender.getCommandSenderName(), dim, x, y, z));
            return;
        }
        if (args[0].equalsIgnoreCase("give-portal")) {
            if (args.length < 3) {
                throw new WrongUsageException("commands.pspace.usage");
            }
            EntityPlayerMP player = getPlayer(sender, args[1]);
            int dim = parseInt(sender, args[2]);
            if (!DimensionManager.isDimensionRegistered(dim)) {
                throw new CommandException("commands.pspace.badDimension");
            }
            WorldServer dimWorld = DimensionManager.getWorld(dim);
            if (dimWorld == null) {
                DimensionManager.initDimension(dim);
                dimWorld = DimensionManager.getWorld(dim);
                if (dimWorld == null) {
                    throw new CommandException("commands.pspace.badDimension");
                }
            }
            ChunkCoordinates target = dimWorld.getSpawnPoint();
            target.posY = dimWorld.getTopSolidOrLiquidBlock(target.posX, target.posZ) + 1;
            double x = target.posX, y = target.posY, z = target.posZ;
            if (args.length >= 6) {
                ChunkCoordinates coords = sender.getPlayerCoordinates();
                x = func_110666_a(sender, x, args[3]);
                y = func_110666_a(sender, y, args[4]);
                z = func_110666_a(sender, z, args[5]);
            }
            ItemStack itemstack = new ItemStack(PersonalSpaceMod.BLOCK_PORTAL, 1, 0);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("active", true);
            tag.setIntArray("target", new int[] {dim, (int) x, (int) y, (int) z});
            itemstack.setTagCompound(tag);
            EntityItem entityitem = player.dropPlayerItemWithRandomChoice(itemstack, false);
            entityitem.delayBeforeCanPickup = 0;
            entityitem.func_145797_a(player.getCommandSenderName());
            return;
        }
        if (args[0].equalsIgnoreCase("allow-worldgen-change")) {
            if (args.length < 2) {
                throw new WrongUsageException("commands.pspace.usage");
            }
            int dim = parseInt(sender, args[1]);
            DimensionConfig cfg = DimensionConfig.getForDimension(dim, false);
            if (cfg == null) {
                throw new CommandException("commands.pspace.badDimension");
            }
            cfg.setAllowGenerationChanges(true);
            Packets.INSTANCE.sendWorldList().sendToClients();
            sender.addChatMessage(new ChatComponentTranslation("commands.pspace.allow-worldgen-change", sender.getCommandSenderName(), dim));
            return;
        }

        throw new WrongUsageException("commands.pspace.usage");
    }

    /**
     * Adds the strings available in this command to the given list of tab completion options.
     */
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        switch (args.length) {
            case 0:
            case 1:
                return getListOfStringsMatchingLastWord(
                        args, "ls", "where", "tpx", "give-portal", "allow-worldgen-change");
            case 2:
                return getListOfStringsMatchingLastWord(args, this.getPlayers());
        }
        return null;
    }

    protected String[] getPlayers() {
        return MinecraftServer.getServer().getAllUsernames();
    }

    /**
     * Return whether the specified command parameter index is a username parameter.
     */
    public boolean isUsernameIndex(String[] args, int idx) {
        return idx == 1;
    }
}
