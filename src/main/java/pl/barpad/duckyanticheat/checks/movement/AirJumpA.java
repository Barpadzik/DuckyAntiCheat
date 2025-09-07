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
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AirJumpA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    private final ConcurrentHashMap<UUID, Boolean> lastOnGround = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastGroundTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastPressurePlateTime = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Long> lastWindChargeUse = new ConcurrentHashMap<>();

    private static final class VelocityMark {
        final long time;
        final Vector vec;
        VelocityMark(long time, Vector vec) {
            this.time = time;
            this.vec = vec;
        }
    }
    private final ConcurrentHashMap<UUID, VelocityMark> lastExternalVelocity = new ConcurrentHashMap<>();

    private static final long DEFAULT_DAMAGE_GRACE_MS = 300L;
    private static final long DEFAULT_TELEPORT_GRACE_MS = 500L;
    private static final long DEFAULT_GROUND_GRACE_MS = 200L;

    public AirJumpA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent ev) {
                if (!(ev.getEntity() instanceof Player p)) return;
                lastDamageTime.put(p.getUniqueId(), System.currentTimeMillis());
            }
        }, plugin);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onTeleport(PlayerTeleportEvent ev) {
                Player p = ev.getPlayer();
                lastTeleportTime.put(p.getUniqueId(), System.currentTimeMillis());
                lastOnGround.remove(p.getUniqueId());
                lastGroundTime.remove(p.getUniqueId());
            }
        }, plugin);
    }

    private boolean isPressurePlate(Material m) {
        if (m == null) return false;
        String name = m.name();
        return name.endsWith("_PRESSURE_PLATE") || name.equals("STONE_PLATE") || name.equals("WOOD_PLATE");
    }
    private boolean isSlimeBlock(Material m) { return m != null && "SLIME_BLOCK".equals(m.name()); }
    private boolean isBubbleColumn(Material m) { return m != null && "BUBBLE_COLUMN".equals(m.name()); }
    private boolean isWindCharge(Material m) { return m != null && "WIND_CHARGE".equals(m.name()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent ev) {
        if (!config.isAirJumpAEnabled()) return;
        if (!config.isIgnoreExternalVelocity()) return;

        final Player p = ev.getPlayer();
        final Vector v = ev.getVelocity();
        if (v == null) return;

        lastExternalVelocity.put(p.getUniqueId(), new VelocityMark(System.currentTimeMillis(), v));

        if (config.isAirJumpADebugMode() && v.getY() > 0) {
            Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) VelocityEvent for "
                    + p.getName() + " vY=" + String.format("%.3f", v.getY()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPhysical(PlayerInteractEvent ev) {
        if (!config.isAirJumpAEnabled()) return;
        if (ev.getAction() == Action.PHYSICAL && ev.getClickedBlock() != null) {
            Material m = ev.getClickedBlock().getType();
            if (config.isDetectPressurePlates() && isPressurePlate(m)) {
                lastPressurePlateTime.put(ev.getPlayer().getUniqueId(), System.currentTimeMillis());
                if (config.isAirJumpADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Plate activated for "
                            + ev.getPlayer().getName() + " on " + m.name());
                }
            }
            return;
        }

        if (!config.isAirJumpAADetectWindCharge()) return;
        Action a = ev.getAction();
        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            if (ev.getItem() != null && isWindCharge(ev.getItem().getType())) {
                lastWindChargeUse.put(ev.getPlayer().getUniqueId(), System.currentTimeMillis());
                if (config.isAirJumpADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Wind Charge used by " + ev.getPlayer().getName());
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent ev) {
        UUID id = ev.getPlayer().getUniqueId();
        lastOnGround.remove(id);
        lastGroundTime.remove(id);
        lastDamageTime.remove(id);
        lastTeleportTime.remove(id);
        lastExternalVelocity.remove(id);
        lastPressurePlateTime.remove(id);
        lastWindChargeUse.remove(id);
    }

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

        try { if (player.isRiptiding()) return; } catch (NoSuchMethodError ignored) {}

        long now = System.currentTimeMillis();

        boolean prevGround = lastOnGround.getOrDefault(uuid, player.isOnGround());
        boolean currGround = player.isOnGround();

        if (currGround) {
            lastGroundTime.put(uuid, now);
        }

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

        long lastGround = lastGroundTime.getOrDefault(uuid, 0L);
        long sinceGround = (lastGround == 0L) ? Long.MAX_VALUE : (now - lastGround);

        boolean basicCondition = !prevGround && !currGround && deltaY > config.getAirJumpAVerticalThreshold();

        boolean recentGroundContact = sinceGround <= DEFAULT_GROUND_GRACE_MS;
        double minHorizontal = config.getAirJumpAMinHorizontalMovement();
        boolean sufficientHorizontalMovement = horizontalDelta >= minHorizontal;

        if (config.isAirJumpAIgnorePotionBoost()) {
            PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP);
            if (jump != null) {
                if (config.isAirJumpADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - JUMP_BOOST.");
                }
                lastOnGround.put(uuid, currGround);
                return;
            }
        }
        PotionEffect lev = player.getPotionEffect(PotionEffectType.LEVITATION);
        if (lev != null) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - LEVITATION.");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        Material feet = player.getLocation().getBlock().getType();
        Material below = player.getLocation().clone().subtract(0, 1, 0).getBlock().getType();
        if (isBubbleColumn(feet) || isSlimeBlock(below)) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName()
                        + " - environment (bubble/slime).");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        long lastDam = lastDamageTime.getOrDefault(uuid, 0L);
        long damageWindow = config.getAirJumpADamageIgnoreMillis();
        if (damageWindow <= 0) damageWindow = DEFAULT_DAMAGE_GRACE_MS;
        if (now - lastDam <= damageWindow) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - recent damage.");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        long lastTp = lastTeleportTime.getOrDefault(uuid, 0L);
        if (now - lastTp <= DEFAULT_TELEPORT_GRACE_MS) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName() + " - recent teleport.");
            }
            lastOnGround.put(uuid, currGround);
            return;
        }

        if (config.isAirJumpAADetectWindCharge()) {
            long lastWC = lastWindChargeUse.getOrDefault(uuid, 0L);
            if (lastWC > 0 && now - lastWC <= config.getAirJumpAWindChargeGraceMs()) {
                if (config.isAirJumpADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName()
                            + " - recent Wind Charge use (" + (now - lastWC) + "ms).");
                }
                lastOnGround.put(uuid, currGround);
                return;
            }
        }

        if (config.isIgnoreExternalVelocity()) {
            VelocityMark vm = lastExternalVelocity.get(uuid);
            if (vm != null) {
                long dt = now - vm.time;
                if (dt <= config.getExternalVelocityGraceMs()
                        && vm.vec != null && vm.vec.getY() >= config.getMinUpwardVelocityY()) {
                    if (config.isAirJumpADebugMode()) {
                        Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) Ignoring " + player.getName()
                                + " - recent external velocity (dt=" + dt + "ms, vY=" + String.format("%.3f", vm.vec.getY()) + ").");
                    }
                    lastOnGround.put(uuid, currGround);
                    return;
                }
            }
        }

        boolean startedAscendingInAir = basicCondition && !recentGroundContact && sufficientHorizontalMovement;

        if (config.isAirJumpADebugMode()) {
            Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) " + player.getName()
                    + " deltaY=" + String.format("%.3f", deltaY)
                    + " threshold=" + String.format("%.3f", config.getAirJumpAVerticalThreshold())
                    + " horizontalDelta=" + String.format("%.4f", horizontalDelta)
                    + " minHorizontal=" + String.format("%.4f", minHorizontal)
                    + " prevGround=" + prevGround + " currGround=" + currGround
                    + " sinceGround=" + (sinceGround == Long.MAX_VALUE ? "never" : sinceGround + "ms")
                    + " groundGrace=" + DEFAULT_GROUND_GRACE_MS + "ms");
        }

        if (startedAscendingInAir) {
            if (config.isAirJumpADebugMode()) {
                Bukkit.getLogger().info("[DuckyAC] (AirJumpA Debug) " + player.getName()
                        + " possible air-jump: deltaY=" + String.format("%.3f", deltaY)
                        + " horizontalDelta=" + String.format("%.4f", horizontalDelta));
            }

            if (config.isAirJumpACancelEvent()) {
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

        lastOnGround.put(uuid, currGround);
    }
}