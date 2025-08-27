package pl.barpad.duckyanticheat.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HitboxA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    /**
     * Constructor - stores references and registers this listener.
     *
     * @param plugin          main plugin instance (used for registering the listener)
     * @param violationAlerts helper for reporting violations and executing punishments
     * @param discordHook     helper for sending punishment info to Discord
     * @param config          configuration manager with thresholds and toggles
     */
    public HitboxA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Get the player's current network ping in ms.
     * Returns a safe default (50ms) if the API call fails for compatibility reasons.
     *
     * @param player the player to query
     * @return ping in milliseconds (int)
     */
    private int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception ex) {
            return 50;
        }
    }

    /**
     * Determine an approximate hitbox height for the victim depending on their state.
     * This attempts to account for swimming/riding/sneaking poses that change the player's height.
     *
     * @param player the victim player
     * @return estimated hitbox height in blocks/meters
     */
    private double getHitboxHeight(Player player) {
        if (player.isSwimming() || player.isSleeping() || player.isRiptiding() ||
                player.getPose().name().equals("SWIMMING") || player.getPose().name().equals("SPIN_ATTACK")) {
            return 0.6;
        } else if (player.isSneaking()) {
            return 1.5;
        } else {
            return 1.8;
        }
    }

    /**
     * Return a constant approximate player hitbox width.
     *
     * @return hitbox width (meters/blocks)
     */
    private double getHitboxWidth() {
        return 0.6;
    }

    /**
     * Generate a small set of sample locations (points) around the victim's center that represent
     * "important" parts of the player's bounding box — head, mid, feet, and some side/corner points.
     *
     * Using multiple points makes the angle-based visibility test more robust against slight offsets.
     *
     * @param victimCenter center location of victim (feet + half height in caller)
     * @param height       estimated hitbox height
     * @param width        estimated hitbox width
     * @return list of sample Locations (not world-teleporting; clones are safe to manipulate)
     */
    private List<Location> generateHitboxPoints(Location victimCenter, double height, double width) {
        List<Location> points = new ArrayList<>(9);
        points.add(victimCenter.clone().add(0, height / 2, 0));
        points.add(victimCenter.clone().add(0, height * 0.9, 0));
        points.add(victimCenter.clone().add(0, height * 0.1, 0));
        points.add(victimCenter.clone().add(width * 0.25, height / 2, 0));
        points.add(victimCenter.clone().add(-width * 0.25, height / 2, 0));
        points.add(victimCenter.clone().add(0, height / 2, width * 0.25));
        points.add(victimCenter.clone().add(0, height / 2, -width * 0.25));
        points.add(victimCenter.clone().add(width * 0.2, height * 0.3, width * 0.2));
        points.add(victimCenter.clone().add(-width * 0.2, height * 0.3, -width * 0.2));
        return points;
    }

    /**
     * Compute the angle in degrees between two vectors.
     * This function is robust to degenerate vectors and clamps the cosine to [-1,1] to avoid NaNs.
     *
     * @param look    normalized direction vector representing attacker's view
     * @param toPoint vector from attacker to sample point
     * @return angle in degrees between the two vectors (0..180)
     */
    private double angleBetweenVectors(Vector look, Vector toPoint) {
        double dot = look.dot(toPoint);
        double denom = look.length() * toPoint.length();
        if (denom == 0) return 180.0;
        double cos = dot / denom;
        if (cos > 1.0) cos = 1.0;
        if (cos < -1.0) cos = -1.0;
        return Math.toDegrees(Math.acos(cos));
    }

    /**
     * Main event handler: checks whether a player attack should be considered suspicious because
     * the victim's hitbox appears to be larger than expected or the attacker hit from an impossible angle/distance.
     *
     * Steps:
     *  - Basic type checks (attacker & victim are players).
     *  - Feature toggle and bypass/permission checks.
     *  - World and movement state checks (ignore flying/gliding/spectator/vehicle).
     *  - Ping threshold: ignore players with too-high ping to reduce false positives.
     *  - Create a victim-centered hitbox sample and compute attacker eye-to-victim-center distance.
     *  - Compute angle-based visibility on several sample points.
     *  - Combine distance + angle heuristics with conservative rules to decide whether to flag.
     *  - If flagged: optionally cancel the event, report violation, debug log, and punish when VL threshold reached.
     *
     * The logic is intentionally conservative (multiple gates) to avoid flagging legitimate edge cases.
     *
     * @param event the damage event triggered by an entity attacking another
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (!config.isHitboxAEnabled()) return;

        if (PermissionBypass.hasBypass(Objects.requireNonNull(attacker.getPlayer()))) return;
        if (attacker.hasPermission("duckyac.bypass.hitbox-a")) return;

        if (!attacker.getWorld().equals(victim.getWorld())) return;

        // ignore flying/creative/gliding/spectator/vehicles
        if (attacker.isGliding() || attacker.isFlying() ||
                attacker.getGameMode().name().equals("SPECTATOR") ||
                attacker.isInsideVehicle()) return;

        int ping = getPlayerPing(attacker);
        int pingThreshold = config.getHitboxAPingThreshold();
        if (ping > pingThreshold) {
            if (config.isHitboxADebugMode()) {
                // Debug: skipping check for this attacker due to high ping which may cause unreliable data
                Bukkit.getLogger().info("[DuckyAC] (HitboxA Debug) Ignoring " + attacker.getName() + " - high ping (" + ping + "ms).");
            }
            return;
        }

        // generate hitbox sample points around victim center (more accurate)
        double hitboxHeight = getHitboxHeight(victim);
        double hitboxWidth = getHitboxWidth();
        Location victimCenter = victim.getLocation().clone().add(0, hitboxHeight / 2.0, 0);
        List<Location> points = generateHitboxPoints(victimCenter, hitboxHeight, hitboxWidth);

        // measure distance from attacker's eye to victim center (more realistic)
        Location attackerEye = attacker.getEyeLocation();
        double distance = attackerEye.distance(victimCenter);

        double maxDistance = config.getMaxHitboxADistance();
        double angleThreshold = config.getMaxHitboxAAngle();

        // tolerance from config + small ping-based factor
        double tolerance = config.getHitboxATolerance(); // add this method in ConfigManager (e.g. default 0.2)
        double pingFactor = Math.max(0.0, (ping - 50) / 1000.0); // small extra tolerance for high ping
        double effectiveTolerance = tolerance + pingFactor;

        // angle checks: count how many sample points are within the attacker's view cone
        Vector look = attackerEye.getDirection().normalize();
        int visiblePoints = 0;
        for (Location p : points) {
            Vector toPoint = p.toVector().subtract(attackerEye.toVector());
            double angle = angleBetweenVectors(look, toPoint);
            if (angle <= angleThreshold) {
                visiblePoints++;
            }
        }

        boolean suspiciousByAngle = (visiblePoints == 0);
        boolean suspiciousByDistance = (distance > (maxDistance + effectiveTolerance));

        if (config.isHitboxADebugMode()) {
            // Debug logging: measured values and intermediate info for tuning thresholds
            Bukkit.getLogger().info("[DuckyAC] (HitboxA Debug) " + attacker.getName()
                    + " hit " + victim.getName()
                    + " | Dist=" + String.format("%.3f", distance)
                    + " (max=" + String.format("%.3f", maxDistance) + " tol=" + String.format("%.3f", effectiveTolerance) + ")"
                    + " | visiblePoints=" + visiblePoints
                    + " | angleThresh=" + angleThreshold
                    + " | ping=" + ping);
            Bukkit.getLogger().info("[DuckyAC] (HitboxA Debug) AttackerEye=" + formatLoc(attackerEye) + " VictimCenter=" + formatLoc(victimCenter));
        }

        // Check whether the victim is standing close to a block edge — edges can cause legitimate strange offsets
        boolean victimOnBlockEdge = (victim.getLocation().getX() % 1.0 < 0.15 || victim.getLocation().getX() % 1.0 > 0.85) ||
                (victim.getLocation().getZ() % 1.0 < 0.15 || victim.getLocation().getZ() % 1.0 > 0.85);

        // Conservative flagging rules (mixed heuristics to reduce false positives):
        //  - strongDistance: a clear exceedance of distance by a larger margin -> flag
        //  - suspiciousByDistance + very few visible points -> flag
        //  - suspiciousByAngle (no visible points) and distance is near the threshold -> flag
        double strongDistanceThreshold = maxDistance + 0.5;
        boolean strongDistance = distance > strongDistanceThreshold;

        boolean shouldFlag = false;
        if (!victimOnBlockEdge) {
            if (strongDistance) {
                // obvious distance exceedance
                shouldFlag = true;
            } else if (suspiciousByDistance && visiblePoints < 3) {
                // distance slightly exceeded + attacker cannot see many hitbox points
                shouldFlag = true;
            } else if (suspiciousByAngle && distance > maxDistance * 0.95) {
                // attacker sees none of the sample points and distance is close to threshold
                shouldFlag = true;
            }
        }

        if (shouldFlag) {
            if (config.isHitboxACancelEvent()) {
                // Optionally cancel the damage event to prevent suspicious hit
                event.setCancelled(true);
            }

            // Report violation and obtain current violation level (VL)
            int vl = violationAlerts.reportViolation(attacker.getName(), "HitboxA");

            if (config.isHitboxADebugMode()) {
                // Additional debug output showing yaw/pitch for manual analysis
                Location attackerLoc = attacker.getLocation();
                float yaw = attackerLoc.getYaw();
                float pitch = attackerLoc.getPitch();
                Bukkit.getLogger().info("[DuckyAC] (HitboxA Debug) " + attacker.getName() + " hit suspiciously (VL:" + vl + ").");
                Bukkit.getLogger().info("[DuckyAC] (HitboxA Debug) Attacker yaw=" + String.format("%.1f", yaw)
                        + " pitch=" + String.format("%.1f", pitch) + " | visiblePoints=" + visiblePoints);
            }

            // If the player reached configured VL threshold, execute punishment and notify Discord
            if (vl >= config.getMaxHitboxAAlerts()) {
                String cmd = config.getHitboxACommand();
                violationAlerts.executePunishment(attacker.getName(), "HitboxA", cmd);
                discordHook.sendPunishmentCommand(attacker.getName(), cmd);
                violationAlerts.clearPlayerViolations(attacker.getName());

                if (config.isHitboxADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (HitboxA Debug) Punishment executed for " + attacker.getName());
                }
            }
        }
    }

    /**
     * Format a Location as a short string for logging: "world [X=.., Y=.., Z=..]".
     *
     * @param loc location to format (may be null)
     * @return formatted string (never null)
     */
    private String formatLoc(Location loc) {
        if (loc == null) return "null";
        return (loc.getWorld() != null ? loc.getWorld().getName() : "world") + " [" +
                "X=" + String.format("%.2f", loc.getX()) + ", " +
                "Y=" + String.format("%.2f", loc.getY()) + ", " +
                "Z=" + String.format("%.2f", loc.getZ()) + "]";
    }
}