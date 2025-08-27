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
 * FlyA - basic vanilla fly detection.
 *
 * Detects two common vanilla-fly signatures:
 *  - Hovering: player stays effectively at same Y (within hoverDelta) while airborne for N ticks.
 *  - Ascending: player continuously gains Y above expected ascendDelta for M ticks.
 *
 * Heuristics / false-positive mitigation included:
 *  - ignores players with high ping (configurable)
 *  - ignores players with recent damage/teleport (knockback/teleport)
 *  - optionally ignores players with JUMP_BOOST / LEVITATION (config)
 *  - ignores creative/spectator, gliding, flying, vehicles
 *  - bypass permission support
 *
 * Notes:
 *  - Uses config methods from ConfigManager (must be implemented there).
 *  - Conservative by default; tune thresholds in config.
 */
public class FlyA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    // per-player counters/state
    private final ConcurrentHashMap<UUID, Integer> hoverTicks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> ascendTicks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    public FlyA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // record when players take damage (to ignore knockback-caused motion)
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent ev) {
                if (!(ev.getEntity() instanceof Player p)) return;
                lastDamageTime.put(p.getUniqueId(), System.currentTimeMillis());
            }
        }, plugin);

        // record teleports to ignore immediate false positives
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onTeleport(PlayerTeleportEvent ev) {
                Player p = ev.getPlayer();
                lastTeleportTime.put(p.getUniqueId(), System.currentTimeMillis());
                // reset counters to avoid immediate flag
                hoverTicks.remove(p.getUniqueId());
                ascendTicks.remove(p.getUniqueId());
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // config toggle
        if (!config.isFlyAEnabled()) return;

        // bypasses and permissions
        if (PermissionBypass.hasBypass(Objects.requireNonNull(player))) return;
        if (player.hasPermission("duckyac.bypass.fly-a")) return;

        // ignore gamemodes & states that allow flight/vertical movement
        String gm = player.getGameMode().name();
        if (gm.equals("CREATIVE") || gm.equals("SPECTATOR")) return;
        if (player.isGliding() || player.isFlying() || player.isInsideVehicle()) return;

        // movement locations
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null) return;

        // ignore if player is on ground (we only care when airborne)
        boolean currGround = player.isOnGround();
        if (currGround) {
            // reset counters on ground contact
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
            return;
        }

        // ignore if player has jump/levitation (based on config)
        if (config.isFlyAIgnoreJumpBoost()) {
            PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP);
            if (jump != null) {
                if (config.isFlyADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - has JUMP_BOOST potion.");
                }
                hoverTicks.remove(uuid);
                ascendTicks.remove(uuid);
                return;
            }
        }
        PotionEffect lev = player.getPotionEffect(PotionEffectType.LEVITATION);
        if (lev != null) {
            if (config.isFlyADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - LEVITATION present.");
            }
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
            return;
        }

        // ping check
        int ping = getPlayerPing(player);
        int pingThreshold = config.getFlyAPingThreshold();
        if (ping > pingThreshold) {
            if (config.isFlyADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - high ping (" + ping + "ms).");
            }
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
            return;
        }

        // ignore recent damage/teleport
        long now = System.currentTimeMillis();
        long lastDam = lastDamageTime.getOrDefault(uuid, 0L);
        long dmgIgnore = config.getFlyADamageIgnoreMillis();
        if (dmgIgnore <= 0) dmgIgnore = 500L;
        if (now - lastDam <= dmgIgnore) {
            if (config.isFlyADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - recent damage.");
            }
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
            return;
        }

        long lastTp = lastTeleportTime.getOrDefault(uuid, 0L);
        long tpIgnore = config.getFlyATeleportIgnoreMillis();
        if (tpIgnore <= 0) tpIgnore = 1000L;
        if (now - lastTp <= tpIgnore) {
            if (config.isFlyADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - recent teleport.");
            }
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
            return;
        }

        // compute vertical/horizontal deltas
        double deltaY = to.getY() - from.getY();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.hypot(dx, dz);

        // thresholds from config
        double hoverDelta = config.getFlyAHoverDelta();    // small Y movement treated as hover
        int hoverThreshold = config.getFlyAHoverTicks();  // ticks to consider hover suspicious
        double ascendDelta = config.getFlyAAscendDelta(); // Y increment threshold for ascend detection
        int ascendThreshold = config.getFlyAAscendTicks(); // consecutive ticks of ascend -> suspicious

        // HOVER DETECTION: player nearly stable vertically but airborne
        if (Math.abs(deltaY) <= hoverDelta) {
            int h = hoverTicks.getOrDefault(uuid, 0) + 1;
            hoverTicks.put(uuid, h);
        } else {
            hoverTicks.remove(uuid);
        }

        // ASCEND DETECTION: continuous positive vertical motion above ascendDelta
        if (deltaY > ascendDelta) {
            int a = ascendTicks.getOrDefault(uuid, 0) + 1;
            ascendTicks.put(uuid, a);
        } else {
            ascendTicks.remove(uuid);
        }

        // Debug logging
        if (config.isFlyADebugMode()) {
            Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) " + player.getName()
                    + " deltaY=" + String.format("%.4f", deltaY)
                    + " horiz=" + String.format("%.4f", horizontal)
                    + " hoverTicks=" + hoverTicks.getOrDefault(uuid, 0)
                    + " ascendTicks=" + ascendTicks.getOrDefault(uuid, 0)
                    + " ping=" + ping);
        }

        boolean flagged = false;
        String reason = "";

        // decide flagging
        if (hoverTicks.getOrDefault(uuid, 0) >= hoverThreshold) {
            flagged = true;
            reason = "hover";
        } else if (ascendTicks.getOrDefault(uuid, 0) >= ascendThreshold) {
            flagged = true;
            reason = "ascend";
        }

        if (flagged) {
            if (config.isFlyACancelEvent()) {
                // optional: cancel movement (rarely used)
                event.setCancelled(true);
            }

            int vl = violationAlerts.reportViolation(player.getName(), "FlyA");
            if (config.isFlyADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) " + player.getName() + " suspected FlyA (" + reason + ") VL:" + vl);
            }

            if (vl >= config.getMaxFlyAAlerts()) {
                String cmd = config.getFlyACommand();
                violationAlerts.executePunishment(player.getName(), "FlyA", cmd);
                discordHook.sendPunishmentCommand(player.getName(), cmd);
                violationAlerts.clearPlayerViolations(player.getName());
                if (config.isFlyADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Punishment executed for " + player.getName());
                }
            }

            // after flagging, reset counters to avoid repeated immediate flags
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
        }
    }
}