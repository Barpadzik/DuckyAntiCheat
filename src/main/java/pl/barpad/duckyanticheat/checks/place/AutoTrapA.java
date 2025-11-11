package pl.barpad.duckyanticheat.checks.place;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public class AutoTrapA implements Listener {

    private final ViolationAlerts violationAlerts;
    private final DiscordHook discordHook;
    private final ConfigManager config;

    private static class TrapData {
        final Set<Location> woodBlocks = new HashSet<>();
        final Set<Location> webBlocks = new HashSet<>();
        final long startTime;
        final Location targetLocation;

        TrapData(Location targetLocation) {
            this.startTime = System.currentTimeMillis();
            this.targetLocation = targetLocation;
        }
    }

    private final ConcurrentHashMap<UUID, TrapData> activeTrapBuilding = new ConcurrentHashMap<>();

    public AutoTrapA(Main plugin, ViolationAlerts violationAlerts, DiscordHook discordHook, ConfigManager config) {
        this.violationAlerts = violationAlerts;
        this.discordHook = discordHook;
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOldData, 100L, 100L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeTrapBuilding.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isAutoTrapAEnabled()) return;

        Player player = event.getPlayer();
        if (PermissionBypass.hasBypass(player)) return;
        if (player.hasPermission("duckyac.bypass.autotrap-a")) return;

        Block placedBlock = event.getBlockPlaced();
        Material type = placedBlock.getType();

        boolean isWood = isWoodMaterial(type);
        boolean isWeb = type == Material.COBWEB;

        if (!isWood && !isWeb) return;

        UUID uuid = player.getUniqueId();
        Location blockLoc = placedBlock.getLocation();

        Player nearestPlayer = findNearestPlayer(player, blockLoc);
        if (nearestPlayer == null) {
            return;
        }

        TrapData trapData = activeTrapBuilding.computeIfAbsent(uuid, k -> new TrapData(nearestPlayer.getLocation()));

        if (isWood) {
            trapData.woodBlocks.add(blockLoc);
        } else {
            trapData.webBlocks.add(blockLoc);
        }

        long elapsed = System.currentTimeMillis() - trapData.startTime;
        long maxTime = config.getAutoTrapAMaxTime();

        if (elapsed > maxTime) {
            activeTrapBuilding.remove(uuid);
            return;
        }

        int woodCount = trapData.woodBlocks.size();
        int webCount = trapData.webBlocks.size();

        int minWoodBlocks = config.getAutoTrapAMinWoodBlocks();
        int minWebBlocks = config.getAutoTrapAMinWebBlocks();

        boolean hasEnoughWood = woodCount >= minWoodBlocks;
        boolean hasEnoughWebs = webCount >= minWebBlocks;
        boolean isSuspiciouslyFast = elapsed < maxTime;

        if (hasEnoughWood && hasEnoughWebs && isSuspiciouslyFast) {
            if (isEnclosingPattern(trapData, nearestPlayer.getLocation())) {
                if (config.isAutoTrapACancelEvent()) {
                    event.setCancelled(true);
                }

                int vl = violationAlerts.reportViolation(player.getName(), "AutoTrapA");

                if (config.isAutoTrapADebugMode()) {
                    Bukkit.getLogger().info("[DuckyAC] (AutoTrapA Debug) " + player.getName()
                            + " suspicious trap pattern detected (VL: " + vl + ")");
                    Bukkit.getLogger().info("[DuckyAC] (AutoTrapA Debug) Wood blocks: " + woodCount
                            + ", Web blocks: " + webCount + ", Time: " + elapsed + "ms");
                }

                if (vl >= config.getMaxAutoTrapAAlerts()) {
                    String cmd = config.getAutoTrapACommand();
                    violationAlerts.executePunishment(player.getName(), "AutoTrapA", cmd);
                    discordHook.sendPunishmentCommand(player.getName(), cmd);
                    violationAlerts.clearPlayerViolations(player.getName());

                    if (config.isAutoTrapADebugMode()) {
                        Bukkit.getLogger().info("[DuckyAC] (AutoTrapA Debug) Punishment executed for " + player.getName());
                    }
                }

                activeTrapBuilding.remove(uuid);
            }
        }
    }

    private boolean isWoodMaterial(Material type) {
        String name = type.name();
        return name.contains("PLANKS") ||
                name.contains("LOG") ||
                name.contains("WOOD") && !name.contains("WOODEN");
    }

    private Player findNearestPlayer(Player placer, Location blockLoc) {
        double minDist = config.getAutoTrapAMaxDistance();
        Player nearest = null;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(placer)) continue;
            if (other.getWorld() != placer.getWorld()) continue;
            if (other.getGameMode().name().equals("SPECTATOR")) continue;

            double dist = other.getLocation().distance(blockLoc);
            if (dist < minDist) {
                minDist = dist;
                nearest = other;
            }
        }

        return nearest;
    }

    private boolean isEnclosingPattern(TrapData trapData, Location targetLoc) {
        Set<Location> allBlocks = new HashSet<>();
        allBlocks.addAll(trapData.woodBlocks);
        allBlocks.addAll(trapData.webBlocks);

        int targetX = targetLoc.getBlockX();
        int targetY = targetLoc.getBlockY();
        int targetZ = targetLoc.getBlockZ();

        boolean hasNorth = false, hasSouth = false, hasEast = false, hasWest = false;
        boolean hasAbove = false, hasBelow = false;
        boolean hasCenterWeb = false;

        for (Location loc : allBlocks) {
            int dx = loc.getBlockX() - targetX;
            int dy = loc.getBlockY() - targetY;
            int dz = loc.getBlockZ() - targetZ;

            if (Math.abs(dx) <= 2 && Math.abs(dy) <= 2 && Math.abs(dz) <= 2) {
                if (dx == 0 && dy >= 0 && dy <= 1 && dz == 0) {
                    if (trapData.webBlocks.contains(loc)) {
                        hasCenterWeb = true;
                    }
                }

                if (dz > 0 && Math.abs(dx) <= 1) hasNorth = true;
                if (dz < 0 && Math.abs(dx) <= 1) hasSouth = true;
                if (dx > 0 && Math.abs(dz) <= 1) hasEast = true;
                if (dx < 0 && Math.abs(dz) <= 1) hasWest = true;
                if (dy > 0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) hasAbove = true;
                if (dy < 0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) hasBelow = true;
            }
        }

        int sidesEnclosed = (hasNorth ? 1 : 0) + (hasSouth ? 1 : 0) + (hasEast ? 1 : 0) + (hasWest ? 1 : 0);
        boolean verticallyEnclosed = hasAbove || hasBelow;

        return sidesEnclosed >= 3 && verticallyEnclosed && hasCenterWeb;
    }

    private void cleanupOldData() {
        long now = System.currentTimeMillis();
        long maxTime = config.getAutoTrapAMaxTime();

        activeTrapBuilding.entrySet().removeIf(entry -> {
            long elapsed = now - entry.getValue().startTime;
            return elapsed > maxTime + 5000L;
        });
    }
}
