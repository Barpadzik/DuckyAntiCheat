package pl.barpad.duckyanticheat.checks.place;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.barpad.duckyanticheat.Main;
import pl.barpad.duckyanticheat.utils.DiscordHook;
import pl.barpad.duckyanticheat.utils.PermissionBypass;
import pl.barpad.duckyanticheat.utils.ViolationAlerts;
import pl.barpad.duckyanticheat.utils.managers.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AutoTrapA implements Listener {

    private final Main plugin;
    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    private static final class BlockPos {
        final int x, y, z;
        final String worldName;

        BlockPos(Location loc) {
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.worldName = Objects.requireNonNull(loc.getWorld()).getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPos that)) return false;
            return x == that.x && y == that.y && z == that.z && worldName.equals(that.worldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, worldName);
        }
    }

    private static final class TrapData {
        final Set<BlockPos> placed = new HashSet<>();
        final long startTime;
        long lastPlacedTime;
        final BlockPos targetPos;

        TrapData(BlockPos targetPos) {
            this.startTime = System.currentTimeMillis();
            this.lastPlacedTime = this.startTime;
            this.targetPos = targetPos;
        }
    }

    private final ConcurrentHashMap<UUID, TrapData> active = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Long> lastDebugTime = new ConcurrentHashMap<>();

    public AutoTrapA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.plugin = plugin;
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOldData, 100L, 100L * 5);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
        lastDebugTime.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isAutoTrapAEnabled()) return;

        Player player = event.getPlayer();
        if (PermissionBypass.hasBypass(player)) return;
        if (player.hasPermission("duckyac.bypass.autotrap-a")) return;

        Block placed = event.getBlockPlaced();
        Location blockLoc = placed.getLocation();
        World world = blockLoc.getWorld();

        double maxDist = config.getAutoTrapAMaxDistance();
        Player nearest = findNearestPlayerViaNearbyEntities(blockLoc, maxDist);
        if (nearest == null) return;

        UUID uuid = player.getUniqueId();
        BlockPos blockPos = new BlockPos(blockLoc);

        BlockPos targetPos = new BlockPos(nearest.getLocation());
        TrapData td = active.compute(uuid, (k, old) -> {
            if (old == null) return new TrapData(targetPos);
            if (!old.targetPos.equals(targetPos)) {
                return new TrapData(targetPos);
            }
            old.lastPlacedTime = System.currentTimeMillis();
            return old;
        });

        int maxStoredBlocks = Math.max(64, config.getAutoTrapAMinBlocks() * 3);
        if (td.placed.size() < maxStoredBlocks) {
            td.placed.add(blockPos);
        }

        long elapsed = System.currentTimeMillis() - td.startTime;
        long maxTime = config.getAutoTrapAMaxTime();
        if (elapsed > maxTime) {
            active.remove(uuid);
            return;
        }

        int blockCount = td.placed.size();
        int minBlocks = config.getAutoTrapAMinBlocks();

        if (blockCount >= minBlocks && elapsed < maxTime) {
            assert world != null;
            PatternResult res = isEnclosingPatternFast(td, nearest.getLocation().getBlockX(),
                    nearest.getLocation().getBlockY(), nearest.getLocation().getBlockZ(), world.getName());

            if (res.enclosed) {
                if (config.isAutoTrapADebugMode()) {
                    maybeDebugLog(uuid, () -> "[DuckyAC] (AutoTrapA Debug) " + player.getName() +
                            " suspicious trap pattern detected (VL will be reported)\n" +
                            "[DuckyAC] (AutoTrapA Debug) Blocks placed: " + blockCount +
                            ", Time: " + elapsed + "ms\n" +
                            "[DuckyAC] (AutoTrapA Debug) Target location: " +
                            nearest.getLocation().getBlockX() + " " +
                            nearest.getLocation().getBlockY() + " " +
                            nearest.getLocation().getBlockZ() + "\n" +
                            "[DuckyAC] (AutoTrapA Debug) Pattern details: " + res.debugSummary() + "\n");
                }

                if (config.isAutoTrapACancelEvent()) {
                    event.setCancelled(true);
                }

                int vl = violationAlerts.reportViolation(player.getName(), "AutoTrapA");

                if (config.isAutoTrapADebugMode()) {
                    final int fvl = vl;
                    maybeDebugLog(uuid, () -> "[DuckyAC] (AutoTrapA Debug) VL for " + player.getName() + ": " + fvl);
                }

                if (vl >= config.getMaxAutoTrapAAlerts()) {
                    String cmd = config.getAutoTrapACommand();
                    violationAlerts.executePunishment(player.getName(), "AutoTrapA", cmd);
                    discordHook.sendPunishmentCommand(player.getName(), cmd);
                    violationAlerts.clearPlayerViolations(player.getName());

                    if (config.isAutoTrapADebugMode()) {
                        maybeDebugLog(uuid, () -> "[DuckyAC] (AutoTrapA Debug) Punishment executed for " + player.getName()
                                + " (cmd: " + config.getAutoTrapACommand() + ")");
                    }
                }

                active.remove(uuid);
            }
        }
    }

    private Player findNearestPlayerViaNearbyEntities(Location loc, double maxDist) {
        World world = loc.getWorld();
        if (world == null) return null;

        Collection<Entity> nearby = world.getNearbyEntities(loc, maxDist, maxDist, maxDist);
        Player nearest = null;
        double best = maxDist;

        for (Entity e : nearby) {
            if (!(e instanceof Player p)) continue;
            if (p.getGameMode().name().equals("SPECTATOR")) continue;

            double d;
            try {
                d = p.getLocation().distance(loc);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private record PatternResult(boolean enclosed, boolean north, boolean south, boolean east, boolean west,
                                 boolean above, boolean below, int surrounding) {

        String debugSummary() {
                int sides = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
                return "Sides: " + sides +
                        " (N:" + north + ", S:" + south + ", E:" + east + ", W:" + west + ")" +
                        ", Vertical: (Above:" + above + ", Below:" + below + ")" +
                        ", Blocks around target: " + surrounding;
            }
        }

    private PatternResult isEnclosingPatternFast(TrapData trapData, int targetX, int targetY, int targetZ, String worldName) {
        boolean hasNorth = false, hasSouth = false, hasEast = false, hasWest = false;
        boolean hasAbove = false, hasBelow = false;
        int surrounding = 0;

        if (trapData.placed.isEmpty()) {
            return new PatternResult(false, false, false, false, false, false, false, 0);
        }

        for (BlockPos bp : trapData.placed) {
            if (!bp.worldName.equals(worldName)) continue;

            int dx = bp.x - targetX;
            int dy = bp.y - targetY;
            int dz = bp.z - targetZ;

            if (Math.abs(dx) <= 2 && Math.abs(dy) <= 2 && Math.abs(dz) <= 2) {
                surrounding++;

                if (dz > 0 && Math.abs(dx) <= 1) hasNorth = true;
                if (dz < 0 && Math.abs(dx) <= 1) hasSouth = true;
                if (dx > 0 && Math.abs(dz) <= 1) hasEast = true;
                if (dx < 0 && Math.abs(dz) <= 1) hasWest = true;
                if (dy > 0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) hasAbove = true;
                if (dy < 0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) hasBelow = true;

                int sides = (hasNorth ? 1 : 0) + (hasSouth ? 1 : 0) + (hasEast ? 1 : 0) + (hasWest ? 1 : 0);
                boolean vertical = hasAbove || hasBelow;

                if (sides >= 3 && vertical) {
                    return new PatternResult(true, hasNorth, hasSouth, hasEast, hasWest, hasAbove, hasBelow, surrounding);
                }
            }
        }

        return new PatternResult(false, hasNorth, hasSouth, hasEast, hasWest, hasAbove, hasBelow, surrounding);
    }

    private void cleanupOldData() {
        long now = System.currentTimeMillis();
        long maxTime = config.getAutoTrapAMaxTime();
        long expire = maxTime + TimeUnit.SECONDS.toMillis(5);

        active.entrySet().removeIf(entry -> {
            TrapData td = entry.getValue();
            long elapsed = now - td.lastPlacedTime;
            return elapsed > expire;
        });
    }

    private void maybeDebugLog(UUID playerUuid, DebugMessageSupplier supplier) {
        long now = System.currentTimeMillis();
        Long last = lastDebugTime.get(playerUuid);
        long debugThrottleMs = 1000L;
        if (last != null && (now - last) < debugThrottleMs) return;
        lastDebugTime.put(playerUuid, now);

        String msg = supplier.get();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> Bukkit.getLogger().info(msg));
    }

    @FunctionalInterface
    private interface DebugMessageSupplier {
        String get();
    }
}