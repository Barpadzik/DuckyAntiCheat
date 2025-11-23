package pl.barpad.duckyanticheat.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.BlockIterator;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;

public class ThruBlocksB implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    /**
     * Constructor - initializes references and registers event listener.
     */
    public ThruBlocksB(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isIgnoredBlock(Material mat) {
        return mat == Material.OAK_SIGN ||
                mat.name().contains("SIGN");
    }


    /**
     * Listens for player-vs.-player damage events.
     * Detects if the attacker hits through blocks illegally.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if damager and victim are players
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        // Check if feature is enabled and attacker is not gliding (elytra)
        if (!config.isThruBlocksBEnabled()) return;
        if (attacker.isGliding()) return;

        // Permission bypass check
        if (PermissionBypass.hasBypass(Objects.requireNonNull(attacker.getPlayer()))) return;
        if (attacker.hasPermission("duckyac.bypass.thrublocks-b")) return;

        if (victim.isSwimming() || victim.isRiptiding() || victim.isSleeping() ||
                victim.getPose().name().equals("SWIMMING") || victim.getPose().name().equals("SPIN_ATTACK")) {
            return;
        }

        // Ignore very close hits to reduce false positives
        double distance = attacker.getEyeLocation().distance(victim.getLocation());
        if (distance < 2.2) return;

        // Define multiple target points on the victim for ray tracing
        Location[] targetPoints = {
                victim.getLocation().add(0, 0.1, 0),    // Near feet
                victim.getLocation().add(0, 0.9, 0),    // Near the chest
                victim.getEyeLocation()                  // Head height
        };

        boolean hasClearPath = false;

        // Check line of sight for each target point
        for (Location target : targetPoints) {
            BlockIterator iterator = new BlockIterator(
                    attacker.getWorld(),
                    attacker.getEyeLocation().toVector(),
                    target.toVector().subtract(attacker.getEyeLocation().toVector()).normalize(),
                    0.0,
                    (int) attacker.getEyeLocation().distance(target)
            );

            boolean pathBlocked = false;

            // Iterate through blocks on a path and check if any are solid or cobweb
            while (iterator.hasNext()) {
                Block block = iterator.next();
                // Ignore sign blocks completely
                if (isIgnoredBlock(block.getType())) {
                    continue;
                }

                if (block.getType().isSolid() || block.getType() == Material.COBWEB) {
                    pathBlocked = true;
                    break;
                }

            }

            if (!pathBlocked) {
                hasClearPath = true;
                break;  // No need to check other points if one clear path found
            }
        }

        // If no clear path was found, this is potentially hitting through blocks
        if (!hasClearPath) {
            // Cancel event if configured
            if (config.isThruBlocksBCancelEvent()) {
                event.setCancelled(true);
            }

            // Report a violation and retrieve the updated violation level (VL)
            int vl = violationAlerts.reportViolation(attacker.getName(), "ThruBlocksB"); // <- Returns the new VL after incrementing

            // If debug mode is enabled, log detailed info to the console
            if (config.isThruBlocksBDebugMode()) {
                Location loc = attacker.getLocation();
                float yaw = loc.getYaw();
                float pitch = loc.getPitch();

                Block targetBlock = attacker.getTargetBlockExact(5); // Get the block the player is looking at, up to 5 blocks away
                String targetBlockInfo = (targetBlock != null)
                        ? targetBlock.getType() + " at " + targetBlock.getLocation().getBlockX() + ", "
                        + targetBlock.getLocation().getBlockY() + ", " + targetBlock.getLocation().getBlockZ()
                        : "None";

                Bukkit.getLogger().info("[DuckyAC] (ThruBlocksB Debug) " + attacker.getName()
                        + " hit through a block (VL: " + vl + ")");
                Bukkit.getLogger().info("[DuckyAC] (ThruBlocksB Debug) Location: X=" + loc.getX()
                        + " Y=" + loc.getY() + " Z=" + loc.getZ() + " | Yaw=" + yaw + " Pitch=" + pitch);
                Bukkit.getLogger().info("[DuckyAC] (ThruBlocksB Debug) Looking at block: " + targetBlockInfo);
            }

            // If the violation level has reached or exceeded the configured maximum, execute punishment
            violationAlerts.getViolationCount(attacker.getName(), "ThruBlocksB");
            if (vl >= config.getMaxThruBlocksBAlerts()) {
                String cmd = config.getThruBlocksBCommand(); // Get the punishment command from config

                // Execute the punishment command on the player
                violationAlerts.executePunishment(attacker.getName(), "ThruBlocksB", cmd);

                // Send the punishment info to Discord (if applicable)
                discordHook.sendPunishmentCommand(attacker.getName(), cmd);

                // Clear user Violations after command has been executed
                violationAlerts.clearPlayerViolations(attacker.getName());

                // Log punishment execution if debug mode is enabled
                if (config.isThruBlocksBDebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (ThruBlocksB Debug) Punishment executed for " + attacker.getName());
                }
            }
        }
    }
}