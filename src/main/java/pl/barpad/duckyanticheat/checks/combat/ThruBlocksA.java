package pl.barpad.duckyanticheat.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;

public class ThruBlocksA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    /**
     * Constructor - initializes references and registers event listener.
     */
    public ThruBlocksA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isBlockObstructing(Block block) {
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

    private boolean isPassableMaterial(Material material) {
        return material == Material.GRASS ||
                material == Material.TALL_GRASS ||
                material == Material.SEAGRASS ||
                material == Material.TALL_SEAGRASS ||
                material == Material.KELP ||
                material == Material.KELP_PLANT ||
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

    private int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception e) {
            return 50;
        }
    }

    private double getHitboxHeight(Player player) {
        if (player.isSwimming() || player.isRiptiding() || player.isSleeping() ||
                player.getPose().name().equals("SWIMMING") || player.getPose().name().equals("SPIN_ATTACK")) {
            return 0.6;
        } else if (player.isSneaking()) {
            return 1.5;
        } else {
            return 1.8;
        }
    }

    private double getHitboxWidth() {
        return 0.6;
    }

    /**
     * Performs precise ray tracing with sub-pixel accuracy
     */
    private boolean hasLineOfSight(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);
        double step = 0.05;

        for (double d = 0; d < distance; d += step) {
            Location checkLoc = from.clone().add(direction.clone().multiply(d));
            Block block = checkLoc.getBlock();

            if (isBlockObstructing(block)) {
                return false;
            }
        }
        return true;
    }

    private boolean canHitFromAngle(Location attackerEye, Location victimLoc, double hitboxHeight, double hitboxWidth) {
        Location[] hitboxPoints = {
                victimLoc.clone().add(0, hitboxHeight / 2, 0),
                victimLoc.clone().add(0, hitboxHeight * 0.9, 0),
                victimLoc.clone().add(0, hitboxHeight * 0.1, 0),
                victimLoc.clone().add(hitboxWidth * 0.25, hitboxHeight / 2, 0),
                victimLoc.clone().add(-hitboxWidth * 0.25, hitboxHeight / 2, 0),
                victimLoc.clone().add(0, hitboxHeight / 2, hitboxWidth * 0.25),
                victimLoc.clone().add(0, hitboxHeight / 2, -hitboxWidth * 0.25),
                victimLoc.clone().add(hitboxWidth * 0.2, hitboxHeight * 0.3, hitboxWidth * 0.2),
                victimLoc.clone().add(-hitboxWidth * 0.2, hitboxHeight * 0.3, -hitboxWidth * 0.2)
        };

        int clearPaths = 0;
        for (Location point : hitboxPoints) {
            if (hasLineOfSight(attackerEye, point)) {
                clearPaths++;
            }
        }

        return clearPaths > 0;
    }

    private boolean checkSideHitPossibility(Location attackerEye, Location victimCenter, double distance) {
        Vector toVictim = victimCenter.toVector().subtract(attackerEye.toVector()).normalize();

        for (int angle = -45; angle <= 45; angle += 15) {
            Vector rotated = rotateVector(toVictim, angle);
            Location sideTarget = attackerEye.clone().add(rotated.multiply(distance * 0.9));

            if (hasLineOfSight(attackerEye, sideTarget)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rotates a vector by given angle in degrees (Y-axis rotation)
     */
    private Vector rotateVector(Vector vector, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;

        return new Vector(x, vector.getY(), z);
    }

    /**
     * Listens for player-vs.-player damage events.
     * Detects if the attacker hits through blocks illegally.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (!config.isThruBlocksAEnabled()) return;

        if (PermissionBypass.hasBypass(Objects.requireNonNull(attacker.getPlayer()))) return;
        if (attacker.hasPermission("duckyac.bypass.thrublocks-a")) return;

        if (!attacker.getWorld().equals(victim.getWorld())) return;

        if (attacker.isGliding() || attacker.isFlying() ||
                attacker.getGameMode().name().equals("CREATIVE") ||
                attacker.getGameMode().name().equals("SPECTATOR")) return;

        double distance = attacker.getEyeLocation().distance(victim.getLocation());
        if (distance > 4.8) return;
        if (distance < 1.2) return;

        int attackerPing = getPlayerPing(attacker);

        if (attackerPing == 0) {
            if (distance < 3.5) return;
        }

        Location victimLoc = victim.getLocation();
        boolean victimOnBlockEdge = (victimLoc.getX() % 1.0 < 0.15 || victimLoc.getX() % 1.0 > 0.85) ||
                (victimLoc.getZ() % 1.0 < 0.15 || victimLoc.getZ() % 1.0 > 0.85);

        double hitboxHeight = getHitboxHeight(victim);
        double hitboxWidth = getHitboxWidth();

        Location attackerEye = attacker.getEyeLocation();
        Location victimCenter = victimLoc.clone().add(0, hitboxHeight / 2, 0);

        boolean canHit = canHitFromAngle(attackerEye, victimLoc, hitboxHeight, hitboxWidth);

        if (!canHit) {
            canHit = checkSideHitPossibility(attackerEye, victimCenter, distance);
        }

        if (!canHit && (victim.isSwimming() || victim.getPose().name().equals("SWIMMING"))) {
            Location[] swimmingPoints = {
                    victimLoc.clone().add(0, 0.3, 0),
                    victimLoc.clone().add(0.3, 0.3, 0),
                    victimLoc.clone().add(-0.3, 0.3, 0),
                    victimLoc.clone().add(0, 0.3, 0.3),
                    victimLoc.clone().add(0, 0.3, -0.3)
            };

            for (Location point : swimmingPoints) {
                if (hasLineOfSight(attackerEye, point)) {
                    canHit = true;
                    break;
                }
            }
        }

        if (!canHit) {
            if (victimOnBlockEdge || attackerPing > 100) {
                return;
            }

            if (config.isThruBlocksACancelEvent()) {
                event.setCancelled(true);
            }

            int vl = violationAlerts.reportViolation(attacker.getName(), "ThruBlocksA");

            if (config.isThruBlocksADebugMode()) {
                Location attackerLoc = attacker.getLocation();
                float yaw = attackerLoc.getYaw();
                float pitch = attackerLoc.getPitch();

                Block targetBlock = attacker.getTargetBlockExact(5);
                String targetBlockInfo = (targetBlock != null)
                        ? targetBlock.getType() + " at " + targetBlock.getLocation().getBlockX() + ", "
                        + targetBlock.getLocation().getBlockY() + ", " + targetBlock.getLocation().getBlockZ()
                        : "None";

                Bukkit.getLogger().info("[DuckyAntiCheat] (ThruBlocksA Debug) " + attacker.getName()
                        + " hit through blocks (VL: " + vl + ")");
                Bukkit.getLogger().info("[DuckyAntiCheat] (ThruBlocksA Debug) Distance: " + String.format("%.2f", distance)
                        + " | Victim hitbox: " + String.format("%.1f", hitboxHeight) + "x" + String.format("%.1f", hitboxWidth));
                Bukkit.getLogger().info("[DuckyAntiCheat] (ThruBlocksA Debug) Location: X=" + String.format("%.2f", victimLoc.getX())
                        + " Y=" + String.format("%.2f", victimLoc.getY()) + " Z=" + String.format("%.2f", victimLoc.getZ())
                        + " | Yaw=" + String.format("%.1f", yaw) + " Pitch=" + String.format("%.1f", pitch));
                Bukkit.getLogger().info("[DuckyAntiCheat] (ThruBlocksA Debug) Ping: " + attackerPing + "ms | Looking at: " + targetBlockInfo);
                Bukkit.getLogger().info("[DuckyAntiCheat] (ThruBlocksA Debug) Victim pose: " + victim.getPose().name()
                        + " | On edge: " + victimOnBlockEdge + " | Can hit: " + canHit);
            }

            if (vl >= config.getMaxThruBlocksAAlerts()) {
                String cmd = config.getThruBlocksACommand();

                violationAlerts.executePunishment(attacker.getName(), "ThruBlocksA", cmd);

                // Send the punishment info to Discord (if applicable)
                discordHook.sendPunishmentCommand(attacker.getName(), cmd);

                // Clear user Violations after command has been executed
                violationAlerts.clearPlayerViolations(attacker.getName());

                // Log punishment execution if debug mode is enabled
                if (config.isThruBlocksADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAntiCheat] (ThruBlocksA Debug) Punishment executed for " + attacker.getName());
                }
            }
        }
    }
}