package pl.barpad.duckyanticheat.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;

/**
 * ReachA - checks whether a player placed or broke a block from too far away.
 * Uses a base range and tolerance from ConfigManager and applies a small ping compensation.
 */
public class ReachA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    public ReachA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Returns the squared distance from the player's eyes to the block center.
     * Using squared distances avoids a sqrt when only comparing.
     */
    private double eyeToBlockCenterSquared(Player player, Block block) {
        Location eye = player.getEyeLocation();
        Location blockCenter = block.getLocation().clone().add(0.5, 0.5, 0.5);
        double dx = eye.getX() - blockCenter.getX();
        double dy = eye.getY() - blockCenter.getY();
        double dz = eye.getZ() - blockCenter.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Core check: given a player, a block and the player's ping, determine if the interaction is too far.
     * If player's ping is above configured threshold, increase allowed distance by 0.5 blocks.
     */
    private boolean isInteractionTooFar(Player player, Block block, int ping) {
        double baseRange = config.getReachABaseRange();
        double tolerance = config.getReachATolerance();

        double allowed = baseRange + tolerance;

        int pingThreshold = config.getReachAPingThreshold();
        if (ping > pingThreshold) {
            allowed += 0.5;
            if (config.isReachADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (ReachA Debug) Increasing allowed reach for " + player.getName()
                        + " due to ping (" + ping + "ms > " + pingThreshold + "ms). +0.5 block applied.");
            }
        }

        double allowedSq = allowed * allowed;
        double distSq = eyeToBlockCenterSquared(player, block);
        return distSq > allowedSq;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isReachAEnabled()) return;

        Player player = event.getPlayer();
        if (PermissionBypass.hasBypass(Objects.requireNonNull(player))) return;
        if (player.hasPermission("duckyac.bypass.reach-a")) return;

        // ignore spectator/creative as usual
        String gm = player.getGameMode().name();
        if (gm.equals("SPECTATOR") || gm.equals("CREATIVE")) return;

        int ping = 0;
        try { ping = player.getPing(); } catch (Throwable ignored) {}

        Block placed = event.getBlockPlaced();
        if (isInteractionTooFar(player, placed, ping)) {
            if (config.isReachACancelEvent()) event.setCancelled(true);

            int vl = violationAlerts.reportViolation(player.getName(), "ReachA");

            if (config.isReachADebugMode()) {
                double actual = Math.sqrt(eyeToBlockCenterSquared(player, placed));
                double base = config.getReachABaseRange();
                double allowed = base + config.getReachATolerance();
                if (ping > config.getReachAPingThreshold()) allowed += 0.5;
                Bukkit.getLogger().info("[DuckyAC] (ReachA Debug) " + player.getName()
                        + " placed block too far. Dist=" + String.format("%.3f", actual)
                        + " | baseRange=" + String.format("%.3f", base)
                        + " | tol=" + String.format("%.3f", config.getReachATolerance())
                        + " | allowed(final)=" + String.format("%.3f", allowed)
                        + " | ping=" + ping
                        + " | vl=" + vl);
            }

            if (vl >= config.getMaxReachAAlerts()) {
                String cmd = config.getReachACommand();
                violationAlerts.executePunishment(player.getName(), "ReachA", cmd);
                discordHook.sendPunishmentCommand(player.getName(), cmd);
                violationAlerts.clearPlayerViolations(player.getName());
                if (config.isReachADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (ReachA Debug) Punishment executed for " + player.getName());
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.isReachAEnabled()) return;

        Player player = event.getPlayer();
        if (PermissionBypass.hasBypass(Objects.requireNonNull(player))) return;
        if (player.hasPermission("duckyac.bypass.reach-a")) return;

        String gm = player.getGameMode().name();
        if (gm.equals("SPECTATOR") || gm.equals("CREATIVE")) return;

        int ping = 0;
        try { ping = player.getPing(); } catch (Throwable ignored) {}

        Block broken = event.getBlock();
        if (isInteractionTooFar(player, broken, ping)) {
            if (config.isReachACancelEvent()) event.setCancelled(true);

            int vl = violationAlerts.reportViolation(player.getName(), "ReachA");

            if (config.isReachADebugMode()) {
                double actual = Math.sqrt(eyeToBlockCenterSquared(player, broken));
                double base = config.getReachABaseRange();
                double allowed = base + config.getReachATolerance();
                if (ping > config.getReachAPingThreshold()) allowed += 0.5;
                Bukkit.getLogger().info("[DuckyAC] (ReachA Debug) " + player.getName()
                        + " broke block too far. Dist=" + String.format("%.3f", actual)
                        + " | baseRange=" + String.format("%.3f", base)
                        + " | tol=" + String.format("%.3f", config.getReachATolerance())
                        + " | allowed(final)=" + String.format("%.3f", allowed)
                        + " | ping=" + ping
                        + " | vl=" + vl);
            }

            if (vl >= config.getMaxReachAAlerts()) {
                String cmd = config.getReachACommand();
                violationAlerts.executePunishment(player.getName(), "ReachA", cmd);
                discordHook.sendPunishmentCommand(player.getName(), cmd);
                violationAlerts.clearPlayerViolations(player.getName());
                if (config.isReachADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (ReachA Debug) Punishment executed for " + player.getName());
                }
            }
        }
    }
}