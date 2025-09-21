package pl.barpad.duckyanticheat.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
 * FlyA - basic vanilla fly detection with Wind Charge grace.
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
    private final ConcurrentHashMap<UUID, Long> lastWindChargeUse = new ConcurrentHashMap<>();

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

    /** Safe wrapper for Player.getPing() - returns 50 on failure. */
    private int getPlayerPing(Player player) {
        try { return player.getPing(); } catch (Exception ex) { return 50; }
    }

    private boolean isWindCharge(Material m) {
        return m != null && "WIND_CHARGE".equals(m.name());
    }

    private boolean isInLiquidOrSwimming(Player player) {
        try {
            if (player.isSwimming()) return true;
            if (player.getLocation().getBlock().isLiquid()) return true;
            if (player.getEyeLocation().getBlock().isLiquid()) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent ev) {
        if (!config.isFlyAEnabled()) return;
        if (!config.isFlyADetectWindCharge()) return;
        Action a = ev.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (ev.getItem() == null) return;
        if (!isWindCharge(ev.getItem().getType())) return;

        lastWindChargeUse.put(ev.getPlayer().getUniqueId(), System.currentTimeMillis());
        if (config.isFlyADebugMode()) {
            Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Wind Charge used by " + ev.getPlayer().getName());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!config.isFlyAEnabled()) return;

        // bypasses and permissions
        if (PermissionBypass.hasBypass(Objects.requireNonNull(player))) return;
        if (player.hasPermission("duckyac.bypass.fly-a")) return;

        // ignore gamemodes & states that allow flight/vertical movement
        String gm = player.getGameMode().name();
        if (gm.equals("CREATIVE") || gm.equals("SPECTATOR")) return;
        if (player.isGliding() || player.isFlying() || player.isInsideVehicle()) return;

        if (isInLiquidOrSwimming(player)) {
            if (config.isFlyADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - swimming/in liquid.");
            }
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
            return;
        }

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

        // potions
        if (config.isFlyAIgnoreJumpBoost()) {
            PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP);
            if (jump != null) {
                if (config.isFlyADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - JUMP_BOOST.");
                }
                hoverTicks.remove(uuid);
                ascendTicks.remove(uuid);
                return;
            }
        }
        PotionEffect lev = player.getPotionEffect(PotionEffectType.LEVITATION);
        if (lev != null) {
            if (config.isFlyADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName() + " - LEVITATION.");
            }
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
            return;
        }

        // ping
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

        // recent damage/teleport
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

        if (config.isFlyADetectWindCharge()) {
            long wc = lastWindChargeUse.getOrDefault(uuid, 0L);
            if (wc > 0 && now - wc <= config.getFlyAWindChargeGraceMs()) {
                if (config.isFlyADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (FlyA Debug) Ignoring " + player.getName()
                            + " - recent Wind Charge use (" + (now - wc) + "ms).");
                }
                hoverTicks.remove(uuid);
                ascendTicks.remove(uuid);
                return;
            }
        }

        // compute deltas
        double deltaY = to.getY() - from.getY();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.hypot(dx, dz);

        // thresholds
        double hoverDelta = config.getFlyAHoverDelta();
        int hoverThreshold = config.getFlyAHoverTicks();
        double ascendDelta = config.getFlyAAscendDelta();
        int ascendThreshold = config.getFlyAAscendTicks();

        // HOVER
        if (Math.abs(deltaY) <= hoverDelta) {
            hoverTicks.put(uuid, hoverTicks.getOrDefault(uuid, 0) + 1);
        } else {
            hoverTicks.remove(uuid);
        }

        // ASCEND
        if (deltaY > ascendDelta) {
            ascendTicks.put(uuid, ascendTicks.getOrDefault(uuid, 0) + 1);
        } else {
            ascendTicks.remove(uuid);
        }

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

        if (hoverTicks.getOrDefault(uuid, 0) >= hoverThreshold) { flagged = true; reason = "hover"; }
        else if (ascendTicks.getOrDefault(uuid, 0) >= ascendThreshold) { flagged = true; reason = "ascend"; }

        if (flagged) {
            if (config.isFlyACancelEvent()) {
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
            hoverTicks.remove(uuid);
            ascendTicks.remove(uuid);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent ev) {
        UUID id = ev.getPlayer().getUniqueId();
        hoverTicks.remove(id);
        ascendTicks.remove(id);
        lastDamageTime.remove(id);
        lastTeleportTime.remove(id);
        lastWindChargeUse.remove(id);
    }
}