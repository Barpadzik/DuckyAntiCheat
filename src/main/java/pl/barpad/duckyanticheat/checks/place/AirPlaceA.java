package pl.barpad.duckyanticheat.checks.place;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;

public class AirPlaceA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    /**
     * Constructor
     * Registers this listener and stores references to utility managers.
     */
    public AirPlaceA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Determines if a block should be considered obstructing/solid for the "air place" check.
     * Returns true for blocks that block placement line-of-sight / count as solid neighbors.
     */
    private boolean isBlockObstructing(Block block) {
        if (block == null) return false;
        Material type = block.getType();

        if (type == Material.AIR || type == Material.VOID_AIR || type == Material.CAVE_AIR) {
            return false;
        }

        if (isPassableMaterial(type)) {
            return false;
        }

        if (type.isSolid()) return true;

        String blockData = block.getBlockData().getAsString();
        if (type.name().endsWith("TRAPDOOR")) {
            return !blockData.contains("open=true");
        }

        if (type.name().endsWith("DOOR") && !type.name().contains("TRAPDOOR")) {
            return !blockData.contains("open=true");
        }

        if (type.name().contains("FENCE_GATE")) {
            return !blockData.contains("open=true");
        }

        return type == Material.PISTON_HEAD ||
                type == Material.MOVING_PISTON ||
                type == Material.COBWEB ||
                type.name().contains("GLASS") ||
                type.name().contains("BARRIER");
    }

    /**
     * Checks whether a material is "passable" (non-obstructing) for placement purposes.
     * This method lists plants, tiny decorations, carpets, torches, rails, etc. that do not count
     * as a solid neighbour that would make placing a block in air valid.
     */
    private boolean isPassableMaterial(Material material) {
        return material == Material.FERN ||
                material.name().contains("GRASS") ||
                material.name().contains("BUSH") ||
                material == Material.DEAD_BUSH ||
                material == Material.DANDELION ||
                material == Material.POPPY ||
                material == Material.BLUE_ORCHID ||
                material == Material.ALLIUM ||
                material == Material.AZURE_BLUET ||
                material == Material.RED_TULIP ||
                material == Material.ORANGE_TULIP ||
                material == Material.WHITE_TULIP ||
                material == Material.PINK_TULIP ||
                material == Material.OXEYE_DAISY ||
                material == Material.LILY_OF_THE_VALLEY ||
                material == Material.LILY_PAD ||
                material.name().contains("TORCHFLOWER") ||
                material.name().contains("CLOSED_EYEBLOSSOM") ||
                material == Material.WITHER_ROSE ||
                material == Material.LILAC ||
                material == Material.ROSE_BUSH ||
                material == Material.PEONY ||
                material.name().contains("PITCHER_PLANT") ||
                material.name().contains("SPORE_BLOSSOM") ||
                material == Material.SEAGRASS ||
                material == Material.TALL_SEAGRASS ||
                material == Material.KELP ||
                material == Material.KELP_PLANT ||
                material == Material.BROWN_MUSHROOM ||
                material == Material.RED_MUSHROOM ||
                material == Material.CRIMSON_FUNGUS ||
                material == Material.WARPED_FUNGUS ||
                material.name().contains("AZALEA") ||
                material.name().contains("SAPLING") ||
                material.name().contains("FLOWER") ||
                material == Material.SUGAR_CANE ||
                material == Material.WHEAT ||
                material == Material.CARROTS ||
                material == Material.POTATOES ||
                material == Material.BEETROOTS ||
                material.name().contains("CARPET") ||
                material == Material.SNOW ||
                material == Material.TORCH ||
                material == Material.REDSTONE_TORCH ||
                material == Material.SOUL_TORCH ||
                material.name().contains("WALL_TORCH") ||
                material.name().contains("BANNER") ||
                material.name().contains("SIGN") ||
                material == Material.LADDER ||
                material.name().contains("RAIL") ||
                material == Material.TRIPWIRE_HOOK ||
                material == Material.TRIPWIRE ||
                material == Material.STRING ||
                material.name().contains("BUTTON") ||
                material.name().contains("PRESSURE_PLATE") ||
                material == Material.LEVER;
    }

    /**
     * Determines if a block is "supportive" — a small block that can support placement
     * logically (like torches, rails, signs, small plants). These are treated as valid neighbors
     * for the AirPlace check (placing next to them should not be considered placing in the air).
     */
    private boolean isSupportiveBlock(Block block) {
        if (block == null) return false;
        Material material = block.getType();

        return material == Material.TORCH ||
                material == Material.WALL_TORCH ||
                material == Material.REDSTONE_TORCH ||
                material == Material.SOUL_TORCH ||
                material == Material.LADDER ||
                material.name().contains("SIGN") ||
                material == Material.LEVER ||
                material.name().contains("BUTTON") ||
                material.name().contains("PRESSURE_PLATE") ||
                material.name().contains("RAIL") ||
                material == Material.TALL_GRASS ||
                material.name().contains("BUSH") ||
                material.name().contains("GRASS") ||
                material == Material.FERN ||
                material == Material.DEAD_BUSH ||
                material == Material.DANDELION ||
                material == Material.POPPY ||
                material == Material.BLUE_ORCHID ||
                material == Material.ALLIUM ||
                material == Material.AZURE_BLUET ||
                material == Material.RED_TULIP ||
                material == Material.ORANGE_TULIP ||
                material == Material.WHITE_TULIP ||
                material == Material.PINK_TULIP ||
                material == Material.OXEYE_DAISY ||
                material == Material.LILY_OF_THE_VALLEY ||
                material == Material.LILY_PAD ||
                material.name().contains("TORCHFLOWER") ||
                material.name().contains("CLOSED_EYEBLOSSOM") ||
                material == Material.WITHER_ROSE ||
                material == Material.LILAC ||
                material == Material.ROSE_BUSH ||
                material == Material.PEONY ||
                material.name().contains("PITCHER_PLANT") ||
                material.name().contains("SPORE_BLOSSOM") ||
                material == Material.SEAGRASS ||
                material == Material.TALL_SEAGRASS ||
                material == Material.KELP ||
                material == Material.KELP_PLANT ||
                material == Material.BROWN_MUSHROOM ||
                material == Material.RED_MUSHROOM ||
                material == Material.CRIMSON_FUNGUS ||
                material == Material.WARPED_FUNGUS ||
                material.name().contains("AZALEA") ||
                material.name().contains("SAPLING") ||
                material.name().contains("FLOWER") ||
                material == Material.SUGAR_CANE ||
                material == Material.WHEAT ||
                material == Material.CARROTS ||
                material == Material.POTATOES ||
                material == Material.BEETROOTS ||
                material.name().contains("CARPET") ||
                material == Material.SNOW ||
                material.name().contains("WALL_TORCH") ||
                material.name().contains("BANNER") ||
                material == Material.TRIPWIRE ||
                material == Material.STRING ||
                material.name().contains("OPEN_EYEBLOSSOM") ||
                material == Material.TRIPWIRE_HOOK;
    }

    /**
     * Event handler for BlockPlaceEvent.
     * Checks neighbouring blocks (above, below, north, south, east, west) and determine
     * whether the placed block is effectively "in the air". If no obstructing or supportive
     * neighbour is present, the player is reported/optionally cancelled/punished.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isAirPlaceAEnabled()) return;

        Player player = event.getPlayer();

        if (PermissionBypass.hasBypass(Objects.requireNonNull(player))) return;
        if (player.hasPermission("duckyac.bypass.airplace-a")) return;

        if (player.getGameMode().name().equals("SPECTATOR"))
            return;

        Block placed = event.getBlockPlaced();

        Block above = placed.getRelative(BlockFace.UP);
        Block below = placed.getRelative(BlockFace.DOWN);
        Block north = placed.getRelative(BlockFace.NORTH);
        Block south = placed.getRelative(BlockFace.SOUTH);
        Block east = placed.getRelative(BlockFace.EAST);
        Block west = placed.getRelative(BlockFace.WEST);

        boolean anyNearbySolid =
                isBlockObstructing(above) || isSupportiveBlock(above) ||
                        isBlockObstructing(below) || isSupportiveBlock(below) ||
                        isBlockObstructing(north) || isSupportiveBlock(north) ||
                        isBlockObstructing(south) || isSupportiveBlock(south) ||
                        isBlockObstructing(east) || isSupportiveBlock(east) ||
                        isBlockObstructing(west) || isSupportiveBlock(west);

        if (!anyNearbySolid) {
            if (config.isAirPlaceACancelEvent()) {
                event.setCancelled(true);
            }

            int vl = violationAlerts.reportViolation(player.getName(), "AirPlaceA");

            if (config.isAirPlaceADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirPlaceA Debug) " + player.getName()
                        + " placed a block in the air (VL: " + vl + ")");
                Bukkit.getLogger().info("[DuckyAC] (AirPlaceA Debug) Placed block: "
                        + placed.getType() + " at " + placed.getLocation().getBlockX() + ", "
                        + placed.getLocation().getBlockY() + ", " + placed.getLocation().getBlockZ());
            }

            if (vl >= config.getMaxAirPlaceAAlerts()) {
                String cmd = config.getAirPlaceACommand();
                violationAlerts.executePunishment(player.getName(), "AirPlaceA", cmd);
                discordHook.sendPunishmentCommand(player.getName(), cmd);
                violationAlerts.clearPlayerViolations(player.getName());

                if (config.isAirPlaceADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AirPlaceA Debug) Punishment executed for " + player.getName());
                }
            }
        }
    }
}