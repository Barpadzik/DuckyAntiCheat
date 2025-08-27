package pl.barpad.duckyanticheat.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;

/**
 * ReachB check - detects when a player strikes another living entity (player or mob)
 * from a distance greater than what's considered possible.
 * <p>
 * Behavior change: players with ping above the configured threshold no longer get ignored.
 * Instead, their allowed reach is increased by 0.5 blocks to reduce false positives for high ping.
 */
public class ReachB implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    public ReachB(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Safely obtain player's ping.
     * Returns the player's ping in milliseconds if available, or a reasonable default (50ms)
     * if any reflection / API mismatch throws an exception (keeps check robust across versions).
     */
    private int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception ex) {
            return 50;
        }
    }

    /**
     * Main event listener for EntityDamageByEntityEvent.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Ensure attacker is a player and target is a living entity (player or mob)
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // Config toggle: skip if ReachB disabled
        if (!config.isReachBEnabled()) return;

        // bypass & perms: explicit bypass list or permission
        if (PermissionBypass.hasBypass(Objects.requireNonNull(attacker.getPlayer()))) return;
        if (attacker.hasPermission("duckyac.bypass.reach-b")) return;

        // world & movement state checks: ensure valid scenario for reach detection
        if (!attacker.getWorld().equals(victim.getWorld())) return;
        if (attacker.isGliding() || attacker.isFlying() || attacker.isInsideVehicle()) return;
        String gm = attacker.getGameMode().name();
        if (gm.equals("CREATIVE") || gm.equals("SPECTATOR")) return;

        // obtain ping
        int ping = getPlayerPing(attacker);
        int pingThreshold = config.getReachBPingThreshold();

        // compute distance from attacker's eye to victim's eye (or body center for mobs)
        double distance = getDistance(attacker, victim);

        // configured allowed distance + tolerance
        double maxDistance = config.getReachBBaseRange();
        double tolerance = config.getReachBTolerance(); // small configurable extra tolerance to avoid false positives
        double allowed = maxDistance + tolerance;

        // If ping exceeds threshold, add 0.5 block to allowed reach (instead of ignoring player).
        if (ping > pingThreshold) {
            allowed += 0.5;
            if (config.isReachBDebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (ReachB Debug) Adjusting allowed reach for "
                        + attacker.getName() + " due to high ping (" + ping + "ms > " + pingThreshold
                        + "ms). +0.5 block applied. New allowed=" + String.format("%.3f", allowed));
            }
        }

        if (config.isReachBDebugMode()) {
            // Detailed debug logging to help tune thresholds and understand flagged cases:
            float yaw = attacker.getLocation().getYaw();
            float pitch = attacker.getLocation().getPitch();
            Bukkit.getLogger().info("[DuckyAC] (ReachB Debug) " + attacker.getName()
                    + " hit " + (victim instanceof Player ? victim.getName() : victim.getType().name())
                    + " | Dist=" + String.format("%.3f", distance)
                    + " (baseMax=" + String.format("%.3f", maxDistance) + " tol=" + String.format("%.3f", tolerance) + ")"
                    + " | allowed(final)=" + String.format("%.3f", allowed)
                    + " | yaw=" + String.format("%.1f", yaw) + " pitch=" + String.format("%.1f", pitch)
                    + " | ping=" + ping);
        }

        // If the actual distance exceeds allowed, flag as suspicious
        if (distance > allowed) {
            // optionally cancel the damage event to prevent the hit from applying
            if (config.isReachBCancelEvent()) {
                event.setCancelled(true);
            }

            // report violation: returns the current VL for the player for this check
            int vl = violationAlerts.reportViolation(attacker.getName(), "ReachB");

            if (config.isReachBDebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (ReachB Debug) " + attacker.getName() + " suspected reach (VL:" + vl + ").");
            }

            // apply punishment when player accumulated enough violations
            if (vl >= config.getMaxReachBAlerts()) {
                String cmd = config.getReachBCommand();
                violationAlerts.executePunishment(attacker.getName(), "ReachB", cmd);
                discordHook.sendPunishmentCommand(attacker.getName(), cmd);
                violationAlerts.clearPlayerViolations(attacker.getName());

                if (config.isReachBDebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (ReachB Debug) Punishment executed for " + attacker.getName());
                }
            }
        }
    }

    private static double getDistance(Player attacker, LivingEntity victim) {
        Location attackerEye = attacker.getEyeLocation();
        Location victimEye;
        try {
            // LivingEntity#getEyeLocation exists for all LivingEntity; preferred because it represents the visual hit point for players and many mobs.
            victimEye = victim.getEyeLocation();
        } catch (Throwable t) {
            // fallback to entity location if something goes wrong (defensive; should rarely happen)
            victimEye = victim.getLocation();
        }

        // measured 3D Euclidean distance between attacker's eye and victim's eye
        return attackerEye.distance(victimEye);
    }
}