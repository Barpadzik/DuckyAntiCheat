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
 *
 * Detects cases where the player appears to be considered "on ground" while there is no solid block
 * sufficiently close beneath the player's feet. This is a heuristic that attempts to catch common
 * "ground spoof" cheats where the client lies about ground state.
 *
 * Conservative measures to reduce false positives:
 *  - ignores players with high ping (configurable),
 *  - ignores players who recently took damage (knockback),
 *  - ignores players who recently teleported,
 *  - ignores creative / spectator / flying / gliding / in-vehicle states,
 *  - ignores small vertical velocity noise,
 *  - uses a configurable scan depth and tolerances.
 *
 * Edge tolerance:
 *  When standing on the edge of a block, the feet location may be over air even though part of the
 *  hitbox is supported. We therefore probe several offsets around the feet (±~0.31 in X/Z) and take
 *  the minimum distance-to-ground found among those probes.
 */
public class GroundSpoofA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    // track last damage / teleport times to ignore knockback / teleport cases
    private final ConcurrentHashMap<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    // Edge tolerance params (chosen to roughly match player half-width ~0.3)
    private static final double EDGE_PROBE_RADIUS = 0.31;   // how far from center to probe (X/Z)
    private static final double EDGE_PROBE_DIAGONAL = 0.31; // same for diagonal
    private static final double PROBE_Y_STEP = 0.05;        // vertical scan increment

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

    /** Safe wrapper for Player.getPing() - returns 50 on failure. */
    private int getPlayerPing(Player player) {
        try { return player.getPing(); } catch (Throwable t) { return 50; }
    }

    /**
     * Scan downward from a given location (treated as "feet") to the first non-air block top surface.
     * Returns vertical distance (feetY - topY). If none found within depth, returns +INF.
     */
    private double scanDistanceToGroundAt(Location feetLoc, double checkDepth) {
        final double feetY = feetLoc.getY();
        double scanned = 0.0;
        while (scanned <= checkDepth) {
            Location probe = feetLoc.clone().subtract(0.0, scanned, 0.0);
            Block block = probe.getBlock();
            if (!block.getType().isAir()) {
                double topY = block.getY() + 1.0;
                return feetY - topY;
            }
            scanned += PROBE_Y_STEP;
        }
        return Double.POSITIVE_INFINITY;
    }

    /** Backwards-compat wrapper using player's current feet position. */
    private double scanDistanceToGround(Player player, double checkDepth) {
        return scanDistanceToGroundAt(player.getLocation(), checkDepth);
    }

    /**
     * Edge-tolerant ground distance: probe center + 8 surrounding offsets and take the MIN distance.
     * This reduces false positives when only part of the hitbox is supported.
     */
    private double scanMinDistanceAround(Player player, double checkDepth) {
        Location feet = player.getLocation();

        // Offsets to probe: center, 4-axis, 4-diagonals
        double r = EDGE_PROBE_RADIUS;
        double d = EDGE_PROBE_DIAGONAL;
        double[][] offsets = new double[][]{
                { 0.0,  0.0},
                { r,    0.0}, {-r,   0.0}, { 0.0,  r}, { 0.0, -r},
                { d,    d  }, { d,   -d  }, { -d,  d }, { -d,  -d}
        };

        double min = Double.POSITIVE_INFINITY;
        for (double[] off : offsets) {
            Location at = feet.clone().add(off[0], 0.0, off[1]);
            double dist = scanDistanceToGroundAt(at, checkDepth);
            if (dist < min) min = dist;
        }
        return min;
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
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) Ignoring " + player.getName()
                        + " - high ping (" + ping + "ms).");
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
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) Ignoring " + player.getName()
                        + " - recent damage.");
            }
            return;
        }

        long lastTp = lastTeleportTime.getOrDefault(player.getUniqueId(), 0L);
        long tpIgnore = config.getGroundSpoofATeleportIgnoreMillis();
        if (tpIgnore <= 0) tpIgnore = 500L;
        if (now - lastTp <= tpIgnore) {
            if (config.isGroundSpoofADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) Ignoring " + player.getName()
                        + " - recent teleport.");
            }
            return;
        }

        // only proceed when the server believes the player is on ground
        if (!player.isOnGround()) return;

        // edge-tolerant distance to the nearest solid block under/around the player
        double checkDepth = config.getGroundSpoofACheckDepth();
        double distanceToGround = scanMinDistanceAround(player, checkDepth);

        double minGroundDistance = config.getGroundSpoofAMinGroundDistance();
        double verticalTolerance = config.getGroundSpoofAVerticalTolerance();

        // small vertical velocity check to avoid flagging legitimate falling/step/jump-in-place
        double vy = player.getVelocity().getY();
        boolean smallVerticalMotion = Math.abs(vy) <= verticalTolerance;

        boolean suspicious;
        if (Double.isInfinite(distanceToGround)) {
            // no block within configured depth -> very suspicious
            suspicious = true;
        } else {
            // Allow min distance + tolerance; with edge probes we already give lateral slack.
            suspicious = (distanceToGround > (minGroundDistance + verticalTolerance)) && smallVerticalMotion;
        }

        if (config.isGroundSpoofADebugMode()) {
            String distStr = Double.isInfinite(distanceToGround) ? "none" : String.format("%.3f", distanceToGround);
            Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) " + player.getName()
                    + " isOnGround=true | distToGround(min-probed)=" + distStr
                    + " (min=" + String.format("%.3f", minGroundDistance)
                    + " tol=" + String.format("%.3f", verticalTolerance) + ")"
                    + " vy=" + String.format("%.3f", vy)
                    + " ping=" + ping);
        }

        if (suspicious) {
            if (config.isGroundSpoofACancelEvent()) {
                event.setCancelled(true);
            }

            int vl = violationAlerts.reportViolation(player.getName(), "GroundSpoofA");

            if (config.isGroundSpoofADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (GroundSpoofA Debug) " + player.getName()
                        + " flagged GroundSpoofA (VL:" + vl + ").");
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