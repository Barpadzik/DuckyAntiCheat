package pl.barpad.duckyanticheat.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AirJump check.
 * <p>
 * Detects when a player appears to "jump" (gain upward motion typical for a jump)
 * while not having contact with the ground (i.e. starting the upward motion while already airborne).
 * <p>
 * This implementation is conservative to avoid false positives:
 * - ignores players with high ping (configurable),
 * - ignores players who recently took damage (knockback),
 * - ignores players who recently teleported,
 * - ignores players with certain potion effects if configured (e.g. JUMP_BOOST, LEVITATION),
 * - ignores creative / spectator / flying / gliding / in-vehicle states,
 * - ignores ascents that occur shortly after last ground contact (ground-grace window),
 * - ignores ascents that have negligible horizontal movement (typical "jump in place").
 * <p>
 * Configuration keys read via ConfigManager#getString(...) (safe defaults used):
 * - air-jump.vertical-threshold      (default "0.3")
 * - air-jump.min-horizontal          (default "0.02")
 */
public class AirJumpA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    // track whether the player was on the ground during the previous move tick
    private final ConcurrentHashMap<UUID, Boolean> lastOnGround = new ConcurrentHashMap<>();

    // time (ms) of the last known ground contact for each player
    private final ConcurrentHashMap<UUID, Long> lastGroundTime = new ConcurrentHashMap<>();

    // times for ignoring due to external events
    private final ConcurrentHashMap<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    // small grace windows (ms)
    private static final long DEFAULT_DAMAGE_GRACE_MS = 300L;
    private static final long DEFAULT_TELEPORT_GRACE_MS = 500L;
    private static final long DEFAULT_GROUND_GRACE_MS = 200L; // don't flag if player was on ground within this window

    public AirJumpA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // simple listener to record when a player takes damage (to ignore knockback-jumps)
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent ev) {
                if (!(ev.getEntity() instanceof Player p)) return;
                lastDamageTime.put(p.getUniqueId(), System.currentTimeMillis());
            }
        }, plugin);

        // record teleports so we can ignore jumps immediately after teleport
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onTeleport(PlayerTeleportEvent ev) {
                Player p = ev.getPlayer();
                lastTeleportTime.put(p.getUniqueId(), System.currentTimeMillis());
                // clear lastOnGround to avoid weird state
                lastOnGround.remove(p.getUniqueId());
                // also update lastGroundTime to far past so that immediate teleport won't be considered ground contact
                lastGroundTime.remove(p.getUniqueId());
            }
        }, plugin);
    }

    /**
     * Safe wrapper for Player.getPing() - returns 50 on failure.
     */
    private int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception ex) {
            return 50;
        }
    }

    /**
     * Reads the vertical threshold for AirJump from the configuration.
     * Uses ConfigManager#getString(path, def) with default 0.3 blocks/tick.
     *
     * @return configured vertical threshold (double)
     */
    private double getAirJumpAVerticalThreshold() {
        String s = config.getString("air-jump.vertical-threshold", "0.3");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return 0.3;
        }
    }

    /**
     * Minimum horizontal movement (XZ) required to consider this ascent suspicious.
     * If horizontal movement is below this threshold we treat it as a "jump in place" and ignore.
     * <p>
     * Default: 0.02 blocks per tick (conservative).
     */
    private double getAirJumpAMinHorizontalMovement() {
        String s = config.getString("air-jump.min-horizontal", "0.02");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return 0.02;
        }
    }

    /**
     * Returns the configured ground grace (ms).
     * For now we keep it local; you can expose it through ConfigManager later.
     */
    private long getGroundGraceMillis() {
        return DEFAULT_GROUND_GRACE_MS;
    }

    /**
     * Main movement handler.
     * <p>
     * Logic:
     * - If a player transitions in-air and exhibits an upward Y delta larger than a threshold
     *   while they were not on the ground in previous tick -> suspect air-jump.
     * <p>
     * - Several heuristics and ignores are applied to reduce false positives (potion effects,
     *   recent damage, teleport, ping threshold, gamemode, gliding/flying, vehicles).
     * <p>
     * Improvements:
     * - do not flag if the player had ground contact recently (within ground-grace window)
     * - do not flag if horizontalDelta < minHorizontalMovement (jumping in place)
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!config.isAirJumpAEnabled()) return;

        if (PermissionBypass.hasBypass(Objects.requireNonNull(player))) return;
        if (player.hasPermission("duckyac.bypass.airjump-a")) return;

        String gm = player.getGameMode().name();
        if (gm.equals("CREATIVE") || gm.equals("SPECTATOR")) return;
        if (player.isGliding() || player.isFlying() || player.isInsideVehicle()) return;

        long now = System.currentTimeMillis();

        boolean prevGround = lastOnGround.getOrDefault(uuid, player.isOnGround());
        boolean currGround = player.isOnGround();

        // update lastGroundTime when we see the player on ground
        if (currGround) {
            lastGroundTime.put(uuid, now);
        }

        // compute vertical delta between from->to; PlayerMoveEvent sometimes fires with null to/from
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from == null) {
            lastOnGround.put(uuid, currGround);
            return;
        }

        double deltaY = to.getY() - from.getY();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDelta = Math.hypot(dx, dz);

        // how recently was the player on ground?
        long lastGround = lastGroundTime.getOrDefault(uuid, 0L);
        long sinceGround = (lastGround == 0L) ? Long.MAX_VALUE : (now - lastGround);

        // basic airborne ascent condition
        boolean basicCondition = !prevGround && !currGround && deltaY > getAirJumpAVerticalThreshold();

        // additional guard: if player was on ground within the ground-grace window, consider it a normal jump and ignore
        boolean recentGroundContact = sinceGround <= getGroundGraceMillis();

        // horizontal guard: ignore if player is essentially not moving horizontally (jump in place)
        double minHorizontal = getAirJumpAMinHorizontalMovement();
        boolean sufficientHorizontalMovement = horizontalDelta >= minHorizontal;

        // ignore if player has jump-boost potion and config allows ignoring
        if (config.isAirJumpAIgnorePotionBoost()) {
            PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP);
            if (jump != null) {
                if (config.isAirJumpADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - has JUMP_BOOST potion.");
                }
                lastOnGround.put(uuid, currGround);
                return;
            }
        }

        // ignore levitation (player may move up)
        PotionEffect lev = player.getPotionEffect(PotionEffectType.LEVITATION);
        if (lev != null) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - LEVITATION effect present.");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        // ping check
        int ping = getPlayerPing(player);
        int pingThreshold = config.getAirJumpAPingThreshold();
        if (ping > pingThreshold) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - high ping (" + ping + "ms).");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        // recent damage/teleport ignores
        long lastDam = lastDamageTime.getOrDefault(uuid, 0L);
        long damageWindow = config.getAirJumpADamageIgnoreMillis(); // how long after damage to ignore
        if (damageWindow <= 0) damageWindow = DEFAULT_DAMAGE_GRACE_MS;
        if (now - lastDam <= damageWindow) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - recent damage (knockback) detected.");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        long lastTp = lastTeleportTime.getOrDefault(uuid, 0L);
        if (now - lastTp <= DEFAULT_TELEPORT_GRACE_MS) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - recent teleport detected.");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        // final decision: require basicCondition, not recent ground contact, and sufficient horizontal movement
        boolean startedAscendingInAir = basicCondition && !recentGroundContact && sufficientHorizontalMovement;

        if (config.isAirJumpADebugMode()) {
            Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) " + player.getName()
                    + " deltaY=" + String.format("%.3f", deltaY)
                    + " threshold=" + String.format("%.3f", getAirJumpAVerticalThreshold())
                    + " horizontalDelta=" + String.format("%.4f", horizontalDelta)
                    + " minHorizontal=" + String.format("%.4f", minHorizontal)
                    + " prevGround=" + prevGround + " currGround=" + currGround
                    + " sinceGround=" + (sinceGround == Long.MAX_VALUE ? "never" : sinceGround + "ms")
                    + " groundGrace=" + getGroundGraceMillis() + "ms"
                    + " ping=" + ping);
        }

        if (startedAscendingInAir) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) " + player.getName()
                        + " possible air-jump: deltaY=" + String.format("%.3f", deltaY)
                        + " horizontalDelta=" + String.format("%.4f", horizontalDelta));
            }

            if (config.isAirJumpACancelEvent()) {
                // optionally cancel movement event (most plugins don't cancel move events; keep configurable)
                event.setCancelled(true);
            }

            int vl = violationAlerts.reportViolation(player.getName(), "AirJumpA");
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) " + player.getName() + " flagged AirJumpA (VL:" + vl + ").");
            }

            if (vl >= config.getMaxAirJumpAAlerts()) {
                String cmd = config.getAirJumpACommand();
                violationAlerts.executePunishment(player.getName(), "AirJumpA", cmd);
                discordHook.sendPunishmentCommand(player.getName(), cmd);
                violationAlerts.clearPlayerViolations(player.getName());
                if (config.isAirJumpADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Punishment executed for " + player.getName());
                }
            }
        }

        // update ground state (store last seen onGround)
        lastOnGround.put(uuid, currGround);
    }
}