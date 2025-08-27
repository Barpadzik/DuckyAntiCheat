package pl.barpad.duckyanticheat.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GroundSpoofA check.
 * <p>
 * Detects cases where the player appears to be considered "on ground" while there is no solid block
 * sufficiently close beneath the player's feet. This is a heuristic that attempts to catch common
 * "ground spoof" cheats where the client lies about ground state.
 * <p>
 * Conservative measures to reduce false positives:
 *  - ignores players with high ping (configurable),
 *  - ignores players who recently took damage (knockback),
 *  - ignores players who recently teleported,
 *  - ignores creative / spectator / flying / gliding / in-vehicle states,
 *  - ignores small vertical velocity noise,
 *  - uses a configurable scan depth and tolerances.
 * <p>
 * Note: this implementation purposely avoids ProtocolLib / PacketEvents and operates purely using
 * Bukkit events & world queries (blocks).
 */
public class GroundSpoofA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    // track last damage / teleport times to ignore knockback / teleport cases
    private final ConcurrentHashMap<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    public GroundSpoofA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // record damage times (to ignore knockback)
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent ev) {
                if (!(ev.getEntity() instanceof Player p)) return;
                lastDamageTime.put(p.getUniqueId(), System.currentTimeMillis());
            }
        }, plugin);

        // record teleports (to ignore immediate post-teleport anomalies)
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onTeleport(PlayerTeleportEvent ev) {
                Player p = ev.getPlayer();
                lastTeleportTime.put(p.getUniqueId(), System.currentTimeMillis());
            }
        }, plugin);
    }

    /**
     * Safe wrapper for Player.getPing() - returns 50 on failure.
     */
    private int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Throwable t) {
            return 50;
        }
    }

    /**
     * Scan downward from player's feet in small increments up to checkDepth to find the first non-air block.
     * Returns the vertical distance (in blocks) between player's feet (player.getLocation().getY())
     * and the top surface of the found block (block.getY() + 1). If no block is found within depth,
     * returns Double.POSITIVE_INFINITY.
     * <p>
     * This uses small increments (0.05) to be reasonably precise without heavy cost.
     */
    private double scanDistanceToGround(Player player, double checkDepth) {
        Location feet = player.getLocation(); // feet location
        double feetY = feet.getY();
        // step is small to handle partial blocks (slabs, etc.)
        final double step = 0.05;
        double scanned = 0.0;
        while (scanned <= checkDepth) {
            Location probe = feet.clone().subtract(0.0, scanned, 0.0);
            Block block = probe.getBlock();
            if (!block.getType().isAir()) {
                // top of block is block.getY() + 1.0
                double topY = block.getY() + 1.0;
                return feetY - topY;
            }
            scanned += step;
        }
        // no solid block found within checkDepth
        return Double.POSITIVE_INFINITY;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!config.isGroundSpoofAEnabled()) return;

        if (PermissionBypass.hasBypass(Objects.requireNonNull(player))) return;
        if (player.hasPermission("duckyac.bypass.ground-spoof-a")) return;

        // ignore certain gamemodes / states
        String gm = player.getGameMode().name();
        if (gm.equals("CREATIVE") || gm.equals("SPECTATOR")) return;
        if (player.isGliding() || player.isFlying() || player.isInsideVehicle()) return;

        // ping ignore
        int ping = getPlayerPing(player);
        int pingThreshold = config.getGroundSpoofAPingThreshold();
        if (ping > pingThreshold) {
            if (config.isGroundSpoofADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) Ignoring " + player.getName() + " - high ping (" + ping + "ms).");
            }
            return;
        }

        long now = System.currentTimeMillis();

        // recent damage / teleport ignores
        long lastDam = lastDamageTime.getOrDefault(player.getUniqueId(), 0L);
        long dmgIgnore = config.getGroundSpoofADamageIgnoreMillis();
        if (dmgIgnore <= 0) dmgIgnore = 300L;
        if (now - lastDam <= dmgIgnore) {
            if (config.isGroundSpoofADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) Ignoring " + player.getName() + " - recent damage.");
            }
            return;
        }

        long lastTp = lastTeleportTime.getOrDefault(player.getUniqueId(), 0L);
        long tpIgnore = config.getGroundSpoofATeleportIgnoreMillis();
        if (tpIgnore <= 0) tpIgnore = 500L;
        if (now - lastTp <= tpIgnore) {
            if (config.isGroundSpoofADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) Ignoring " + player.getName() + " - recent teleport.");
            }
            return;
        }

        // only proceed when the server believes the player is on ground; ground-spoof commonly lies about being on/off-ground
        if (!player.isOnGround()) return;

        // measure vertical distance to the nearest solid block under the player
        double checkDepth = config.getGroundSpoofACheckDepth();
        double distanceToGround = scanDistanceToGround(player, checkDepth);

        double minGroundDistance = config.getGroundSpoofAMinGroundDistance();
        double verticalTolerance = config.getGroundSpoofAVerticalTolerance();

        // small vertical velocity check to avoid flagging legitimate falling/step/jump-in-place
        player.getVelocity();
        double vy = player.getVelocity().getY();
        boolean smallVerticalMotion = Math.abs(vy) <= verticalTolerance;

        boolean suspicious;
        if (Double.isInfinite(distanceToGround)) {
            // no block within configured depth -> very suspicious
            suspicious = true;
        } else {
            // if distance to top of block is larger than allowed minimum + tolerance AND vertical motion small -> suspicious
            suspicious = (distanceToGround > (minGroundDistance + verticalTolerance)) && smallVerticalMotion;
        }

        if (config.isGroundSpoofADebugMode()) {
            String distStr = Double.isInfinite(distanceToGround) ? "none" : String.format("%.3f", distanceToGround);
            Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) " + player.getName()
                    + " isOnGround=true | distToGround=" + distStr
                    + " (min=" + String.format("%.3f", minGroundDistance) + " tol=" + String.format("%.3f", verticalTolerance) + ")"
                    + " vy=" + String.format("%.3f", vy)
                    + " ping=" + ping);
        }

        if (suspicious) {
            if (config.isGroundSpoofACancelEvent()) {
                event.setCancelled(true);
            }

            int vl = violationAlerts.reportViolation(player.getName(), "GroundSpoofA");

            if (config.isGroundSpoofADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) " + player.getName() + " flagged GroundSpoofA (VL:" + vl + ").");
            }

            if (vl >= config.getMaxGroundSpoofAAlerts()) {
                String cmd = config.getGroundSpoofACommand();
                violationAlerts.executePunishment(player.getName(), "GroundSpoofA", cmd);
                discordHook.sendPunishmentCommand(player.getName(), cmd);
                violationAlerts.clearPlayerViolations(player.getName());

                if (config.isGroundSpoofADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) Punishment executed for " + player.getName());
                }
            }
        }
    }
}