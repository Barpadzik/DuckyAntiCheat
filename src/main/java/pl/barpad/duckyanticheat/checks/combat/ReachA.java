package pl.barpad.duckyanticheat.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
 * It considers a per-player interaction range attribute if present, falls back to a configured base range,
 * and applies a configurable tolerance. Also adjusts allowed distance slightly for players above a ping threshold.
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
     * Attempts to obtain a per-player interaction/reach range by scanning available Attribute enums
     * and reading the first attribute whose name suggests reach/interaction/block. Returns NaN if none found.
     */
    private double getPlayerAttributeInteractionRange(Player player) {
        try {
            for (Attribute attr : Attribute.values()) {
                String name = attr.name().toLowerCase();
                if (name.contains("reach") || name.contains("interaction") || name.contains("block")) {
                    AttributeInstance inst = player.getAttribute(attr);
                    if (inst != null) {
                        double val = inst.getValue();
                        if (!Double.isNaN(val) && val > 0) return val;
                    }
                }
            }
        } catch (Throwable t) {
            // ignore and fall back to config
        }
        return Double.NaN;
    }

    /**
     * Computes the effective allowed interaction range for the player:
     *  - if player has an attribute (found by getPlayerAttributeInteractionRange), use that
     *  - otherwise use the configured base range from ConfigManager
     * The returned value is the value BEFORE applying tolerance and ping compensation.
     */
    private double getEffectivePlayerRange(Player player) {
        double attr = getPlayerAttributeInteractionRange(player);
        if (!Double.isNaN(attr)) return attr;
        return config.getReachABaseRange();
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
        double effectiveRange = getEffectivePlayerRange(player);
        double tolerance = config.getReachATolerance();

        // base allowed (before ping compensation)
        double allowed = effectiveRange + tolerance;

        // if player's ping exceeds threshold, increase allowed range by 0.5 blocks
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
                double eff = getEffectivePlayerRange(player);
                double allowed = eff + config.getReachATolerance();
                if (ping > config.getReachAPingThreshold()) allowed += 0.5;
                Bukkit.getLogger().info("[DuckyAC] (ReachA Debug) " + player.getName()
                        + " placed block too far. Dist=" + String.format("%.3f", actual)
                        + " | allowed(base)=" + String.format("%.3f", eff)
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
                double eff = getEffectivePlayerRange(player);
                double allowed = eff + config.getReachATolerance();
                if (ping > config.getReachAPingThreshold()) allowed += 0.5;
                Bukkit.getLogger().info("[DuckyAC] (ReachA Debug) " + player.getName()
                        + " broke block too far. Dist=" + String.format("%.3f", actual)
                        + " | allowed(base)=" + String.format("%.3f", eff)
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