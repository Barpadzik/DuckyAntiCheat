package pl.barpad.duckyanticheat.utils.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // === GETTERS (lexicographically ordered) ===

    /** Returns the command configured for AirPlaceA. */
    public String getAirPlaceACommand() {
        return plugin.getConfig().getString("air-place-a.command", "kick %player% Invalid block placement (InvalidPlaceA)");
    }

    /** Returns the command for AirJump (punishment command). */
    public String getAirJumpACommand() {
        return plugin.getConfig().getString("air-jump-a.command", "kick %player% Unnatural air jump detected (AirJump)");
    }

    /** Returns the maximum number of alerts before punishment for AirJump. */
    public int getMaxAirJumpAAlerts() {
        return plugin.getConfig().getInt("air-jump-a.max-alerts", 5);
    }

    /** Returns the main alert message format. */
    public String getAlertMessage() {
        return config.getString("alert-message", "&d&lDuckyAC &8» &fPlayer &7»&f %player% &7»&6 %check% &7(&c%vl%VL&7)");
    }

    /** Returns alert timeout in seconds. */
    public int getAlertTimeoutSeconds() {
        return config.getInt("alert-timeout", 300);
    }

    /** Returns the command for AutoTotemA. */
    public String getAutoTotemACommand() {
        return config.getString("auto-totem-a.command", "kick %player% Too fast totem swap (AutoTotemA)");
    }

    /** Returns minimal delay for AutoTotemA (ms). */
    public long getAutoTotemAMinDelay() {
        return config.getLong("auto-totem-a.min-delay", 150);
    }

    /** Returns tick interval for AutoTotemA. */
    public int getAutoTotemATickInterval() {
        return config.getInt("auto-totem-a.tick-interval", 3);
    }

    /** Returns configured Discord avatar URL. */
    public String getDiscordAvatarUrl() {
        return config.getString("discord.avatar-url", "https://i.imgur.com/wPfoYdI.png");
    }

    /** Returns the Discord punishment message template. */
    public String getDiscordPunishmentMessageTemplate() {
        return config.getString("discord.punishment-message-template",
                "**Punishment Executed**\nPlayer: **%player%**\nCommand: `%command%`");
    }

    /** Returns the Discord violation message template. */
    public String getDiscordViolationMessageTemplate() {
        return config.getString("discord.violation-message-template",
                "**AntiCheatSystem**\nPlayer: **%player%**\nCheck: **%check%**\nViolation: **%vl%**");
    }

    /** Returns the configured Discord username. */
    public String getDiscordUsername() {
        return config.getString("discord.username", "DuckyAntiCheat");
    }

    /** Returns the Discord webhook URL from config. */
    public String getDiscordWebhookUrl() {
        return config.getString("discord.discord-webhook-url", "");
    }

    /** Returns the command configured for Elytra Aim A. */
    public String getElytraAimACommand() {
        return config.getString("elytra-aim-a.command", "kick %player% Cheating with Elytra (ElytraAimA)");
    }

    /** Returns the max alerts for Elytra Aim A. */
    public int getElytraAimAMaxAlerts() {
        return config.getInt("elytra-aim-a.max-alerts", 5);
    }

    /** Returns max firework delay (ms) for Elytra Aim A. */
    public int getElytraAimAMaxFireworkDelay() {
        return config.getInt("elytra-aim-a.max-firework-delay", 200);
    }

    /** Returns the command for Elytra Criticals A. */
    public String getElytraCriticalsACommand() {
        return config.getString("elytra-criticals-a.command", "kick %player% Cheating with Elytra (ElytraCriticalsA)");
    }

    /** Returns how many critical hits are required for Elytra Criticals A. */
    public int getElytraCriticalsACriticalHitsRequired() {
        return config.getInt("elytra-criticals-a.critical-hits-required", 2);
    }

    /** Returns timeframe (ms) for Elytra Criticals A. */
    public int getElytraCriticalsATimeframe() {
        return config.getInt("elytra-criticals-a.timeframe", 500);
    }

    /** Returns the command for FastClimbA. */
    public String getFastClimbACommand() {
        return config.getString("fast-climb-a.command", "kick %player% You send too many packets (TimerD)");
    }

    /** Returns max climb speed for FastClimbA. */
    public double getFastClimbAMaxSpeed() {
        return plugin.getConfig().getDouble("fast-climb-a.max-climb-speed", 0.15);
    }

    /** Returns the command for FastPlaceA. */
    public String getFastPlaceACommand() {
        return plugin.getConfig().getString("fast-place-a.command", "kick %player% Too fast block placement (FastPlaceA)");
    }

    /**
     * Ping threshold (ms). Players with ping above this value will be ignored by the check.
     * Default: 300
     */
    public int getFlyAPingThreshold() {
        return config.getInt("fly-a.ping-threshold", 300);
    }

    /**
     * Small vertical delta (blocks per tick) considered "hover" (near-zero vertical movement).
     * If |deltaY| <= hoverDelta while airborne for N ticks the player may be hovering/flying.
     * Default: 0.02
     */
    public double getFlyAHoverDelta() {
        return config.getDouble("fly-a.hover-delta", 0.02);
    }

    /**
     * Number of consecutive ticks with near-zero vertical movement (hover) required to trigger a hover alert.
     * Default: 12
     */
    public int getFlyAHoverTicks() {
        return config.getInt("fly-a.hover-ticks", 12);
    }

    /**
     * Vertical delta threshold (blocks per tick) used to detect suspicious continuous ascending.
     * If deltaY > ascendDelta for M consecutive ticks, it may indicate flying.
     * Default: 0.06
     */
    public double getFlyAAscendDelta() {
        return config.getDouble("fly-a.ascend-delta", 0.06);
    }

    /**
     * Number of consecutive ticks with deltaY > ascendDelta required to trigger an ascend alert.
     * Default: 6
     */
    public int getFlyAAscendTicks() {
        return config.getInt("fly-a.ascend-ticks", 6);
    }

    /**
     * Milliseconds to ignore detections after the player took damage (knockback).
     * Default: 500
     */
    public long getFlyADamageIgnoreMillis() {
        return config.getLong("fly-a.damage-ignore-millis", 500L);
    }

    /**
     * Milliseconds to ignore detections after the player teleported.
     * Default: 1000
     */
    public long getFlyATeleportIgnoreMillis() {
        return config.getLong("fly-a.teleport-ignore-millis", 1000L);
    }

    public boolean isFlyADetectWindCharge() {
        return plugin.getConfig().getBoolean("fly-a.detect-wind-charge", true);
    }

    public long getFlyAWindChargeGraceMs() {
        return config.getLong("fly-a.wind-charge-grace-ms", 900);
    }

    /**
     * Maximum number of alerts (violations) before punishment is executed.
     * Default: 10
     */
    public int getMaxFlyAAlerts() {
        return config.getInt("fly-a.max-alerts", 10);
    }

    /**
     * Command to execute when a player is punished by FlyA.
     * Use %player% placeholder for the player's name.
     * Default: kick %player% Flying (FlyA)
     */
    public String getFlyACommand() {
        return config.getString("fly-a.command", "kick %player% Flying (FlyA)");
    }

    /** Returns max allowed placements per tick for FastPlaceA. */
    public int getFastPlaceAMaxPerSecond() {
        return plugin.getConfig().getInt("fast-place-a.max-per-tick", 4);
    }

    /** Returns ping threshold (ms) above which players are ignored by GroundSpoofA. */
    public int getGroundSpoofAPingThreshold() {
        return plugin.getConfig().getInt("ground-spoof-a.ping-threshold", 300);
    }

    /** Returns how deep (in blocks) to scan below the player for a solid block. */
    public double getGroundSpoofACheckDepth() {
        return plugin.getConfig().getDouble("ground-spoof-a.check-depth", 0.8);
    }

    /** Returns maximum allowed distance (blocks) from player's feet to the top of the block below. */
    public double getGroundSpoofAMinGroundDistance() {
        return plugin.getConfig().getDouble("ground-spoof-a.min-ground-distance", 0.15);
    }

    /** Returns vertical movement tolerance (blocks) to ignore small vertical noise/landings. */
    public double getGroundSpoofAVerticalTolerance() {
        return plugin.getConfig().getDouble("ground-spoof-a.vertical-tolerance", 0.02);
    }

    /** Returns how long (ms) after taking damage to ignore the player for GroundSpoofA. */
    public long getGroundSpoofADamageIgnoreMillis() {
        return plugin.getConfig().getLong("ground-spoof-a.damage-ignore-millis", 300L);
    }

    /** Returns how long (ms) after teleport to ignore the player for GroundSpoofA. */
    public long getGroundSpoofATeleportIgnoreMillis() {
        return plugin.getConfig().getLong("ground-spoof-a.teleport-ignore-millis", 500L);
    }

    /** Returns maximum alerts (violations) before punishment is executed for GroundSpoofA. */
    public int getMaxGroundSpoofAAlerts() {
        return plugin.getConfig().getInt("ground-spoof-a.max-alerts", 5);
    }

    /** Returns the punishment command for GroundSpoofA. */
    public String getGroundSpoofACommand() {
        return plugin.getConfig().getString("ground-spoof-a.command", "kick %player% Suspected ground spoof (GroundSpoofA)");
    }

    /** Returns the command for HitboxA. */
    public String getHitboxACommand() { return config.getString("hitbox-a.command", "kick %player% Too large hitboxes (HitboxA)"); }

    /** Returns ping threshold for HitboxA. */
    public int getHitboxAPingThreshold() { return config.getInt("hitbox-a.ping-threshold", 300); }

    /** Returns tolerance used in HitboxA check. */
    public double getHitboxATolerance() { return config.getDouble("hitbox-a.max-tolerance", 0.2); }

    /** Returns the command for InvalidPlaceA. */
    public String getInvalidPlaceACommand() {
        return plugin.getConfig().getString("invalid-place-a.command", "kick %player% Invalid block placement (AirPlaceA)");
    }

    /** Returns threshold (angle) used by InvalidPlaceA. */
    public double getInvalidPlaceAThreshold() {
        return plugin.getConfig().getDouble("invalid-place-a.max-angle", 50.0);
    }

    /** Returns max alerts for AirPlaceA. */
    public int getMaxAirPlaceAAlerts() {
        return plugin.getConfig().getInt("air-place-a.max-alerts", 5);
    }

    /** Returns configured maximum alerts for AutoTotemA. */
    public int getMaxAutoTotemAAlerts() {
        return config.getInt("auto-totem-a.max-alerts", 5);
    }

    /** Returns configured maximum alerts for ElytraCriticalsA. */
    public int getMaxElytraCriticalsAAlerts() {
        return config.getInt("elytra-criticals-a.max-alerts", 5);
    }

    /** Returns configured maximum alerts for FastClimbA. */
    public int getMaxFastClimbAAlerts() {
        return config.getInt("fast-climb-a.max-alerts", 5);
    }

    /** Returns configured maximum alerts for FastPlaceA. */
    public int getMaxFastPlaceAAlerts() {
        return plugin.getConfig().getInt("fast-place-a.max-alerts", 3);
    }

    /** Returns maximum alerts for HitboxA. */
    public int getMaxHitboxAAlerts() { return config.getInt("hitbox-a.max-alerts", 5); }

    /** Returns maximum angle allowed for HitboxA. */
    public double getMaxHitboxAAngle() { return config.getDouble("hitbox-a.max-angle", 40.0); }

    /** Returns maximum distance allowed for HitboxA. */
    public double getMaxHitboxADistance() { return config.getDouble("hitbox-a.max-distance", 3.5); }

    /** Returns configured max alerts for InvalidPlaceA. */
    public int getMaxInvalidPlaceAAlerts() {
        return plugin.getConfig().getInt("invalid-place-a.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownA. */
    public int getMaxNoSlowDownAAlerts() {
        return plugin.getConfig().getInt("no-slowdown-a.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownB. */
    public int getMaxNoSlowDownBAlerts() {
        return plugin.getConfig().getInt("no-slowdown-b.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownC. */
    public int getMaxNoSlowDownCAlerts() {
        return plugin.getConfig().getInt("no-slowdown-c.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownD. */
    public int getMaxNoSlowDownDAlerts() {
        return plugin.getConfig().getInt("no-slowdown-d.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownE. */
    public int getMaxNoSlowDownEAlerts() {
        return plugin.getConfig().getInt("no-slowdown-e.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownF. */
    public int getMaxNoSlowDownFAlerts() {
        return plugin.getConfig().getInt("no-slowdown-f.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownG. */
    public int getMaxNoSlowDownGAlerts() {
        return plugin.getConfig().getInt("no-slowdown-g.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownH. */
    public int getMaxNoSlowDownHAlerts() {
        return plugin.getConfig().getInt("no-slowdown-h.max-alerts", 5);
    }

    /** Returns configured max alerts for NoSlowDownH. */
    public int getMaxNoWebAAlerts() {
        return plugin.getConfig().getInt("no-web-a.max-alerts", 5);
    }

    /** Returns maximum packets per second allowed for TimerA. */
    public int getMaxPacketsPerSecondA() {
        return plugin.getConfig().getInt("timer-a.max-packets-per-second", 24);
    }

    /** Returns maximum packets per second allowed for TimerB. */
    public int getMaxPacketsPerSecondB() {
        return plugin.getConfig().getInt("timer-b.max-packets-per-second", 20);
    }

    /** Returns maximum packets per second allowed for TimerC. */
    public int getMaxPacketsPerSecondC() {
        return plugin.getConfig().getInt("timer-c.max-packets-per-second", 20);
    }

    /** Returns maximum packets per second allowed for TimerD. */
    public int getMaxPacketsPerSecondD() {
        return plugin.getConfig().getInt("timer-d.max-packets-per-second", 30);
    }

    /** Returns configured max ping used by AutoTotemA. */
    public double getMaxPing() {
        return config.getDouble("auto-totem-a.max-ping", -1.0);
    }

    /** Returns configured max alerts for TimerA. */
    public int getMaxTimerAAlerts() {
        return config.getInt("timer-a.max-alerts", 10);
    }

    /** Returns configured max alerts for TimerB. */
    public int getMaxTimerBAlerts() {
        return config.getInt("timer-b.max-alerts", 10);
    }

    /** Returns configured max alerts for TimerC. */
    public int getMaxTimerCAlerts() {
        return config.getInt("timer-c.max-alerts", 10);
    }

    /** Returns configured max alerts for TimerD. */
    public int getMaxTimerDAlerts() {
        return config.getInt("timer-d.max-alerts", 10);
    }

    /** Returns the command for NoSlowDownA. */
    public String getNoSlowDownACommand() {
        return plugin.getConfig().getString("no-slowdown-a.command", "kick %player% Player was walking too fast while eating (NoSlowDownA)");
    }

    /** Returns max eating speed for NoSlowDownA. */
    public double getNoSlowDownAMaxEatingSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-a.max-eating-speed", 0.20);
    }

    /** Returns max ignore speed for NoSlowDownA. */
    public double getNoSlowDownAMaxIgnoreSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-a.max-distance", 1.0);
    }

    /** Returns list of ignored speeds for NoSlowDownA. */
    public List<Double> getNoSlowDownAIgnoredSpeedValues() {
        return config.getDoubleList("no-slowdown-a.ignored-speeds");
    }

    /** Returns the command for NoSlowDownB. */
    public String getNoSlowDownBCommand() {
        return plugin.getConfig().getString("no-slowdown-b.command", "kick %player% Player was walking too fast with a drawn bow (NoSlowDownB)");
    }

    /** Returns max bow speed for NoSlowDownB. */
    public double getNoSlowDownBMaxBowSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-b.max-bow-speed", 0.20);
    }

    /** Returns max ignore speed for NoSlowDownB. */
    public double getNoSlowDownBMaxIgnoreSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-b.no-slowdown-max-distance", 1.0);
    }

    /** Returns list of ignored speeds for NoSlowDownB. */
    public List<Double> getNoSlowDownBIgnoredSpeedValues() {
        return config.getDoubleList("no-slowdown-b.ignored-speeds");
    }

    /** Returns the command for NoSlowDownC. */
    public String getNoSlowDownCCommand() {
        return plugin.getConfig().getString("no-slowdown-c.command", "kick %player% Player walked too fast while drawing the crossbow (NoSlowDownC)");
    }

    /** Returns max ignore speed for NoSlowDownC. */
    public double getNoSlowDownCMaxIgnoreSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-max-distance", 1.0);
    }

    /** Returns max speed for NoSlowDownC. */
    public double getNoSlowDownCMaxSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-c.max-speed", 0.20);
    }

    /** Returns list of ignored speeds for NoSlowDownC. */
    public List<Double> getNoSlowDownCIgnoredSpeedValues() {
        return config.getDoubleList("no-slowdown-c.ignored-speeds");
    }

    /** Returns the command for NoSlowDownD. */
    public String getNoSlowDownDCommand() {
        return plugin.getConfig().getString("no-slowdown-d.command", "kick %player% Player was walking too fast while holding a shield (NoSlowDownD)");
    }

    /** Returns max ignore speed for NoSlowDownD. */
    public double getNoSlowDownDMaxIgnoreSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-max-distance", 1.0);
    }

    /** Returns max speed for NoSlowDownD. */
    public double getNoSlowDownDMaxSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-d.max-speed", 0.20);
    }

    /** Returns list of ignored speeds for NoSlowDownD. */
    public List<Double> getNoSlowDownDIgnoredSpeedValues() {
        return config.getDoubleList("no-slowdown-d.ignored-speeds");
    }

    /** Returns the command for NoSlowDownE. */
    public String getNoSlowDownECommand() {
        return plugin.getConfig().getString("no-slowdown-e.command", "kick %player% Player was walking too fast on honey block (NoSlowDownE)");
    }

    /** Returns max speed for NoSlowDownE. */
    public double getNoSlowDownEMaxSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-e.max-speed", 0.170);
    }

    /** Returns the command for NoSlowDownF. */
    public String getNoSlowDownFCommand() {
        return plugin.getConfig().getString("no-slowdown-f.command", "kick %player% Player was walking too fast on soul sand (NoSlowDownF)");
    }

    /** Returns max speed for NoSlowDownF. */
    public double getNoSlowDownFMaxSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-f.max-speed", 0.170);
    }

    /** Returns the command for NoSlowDownG. */
    public String getNoSlowDownGCommand() {
        return plugin.getConfig().getString("no-slowdown-g.command", "kick %player% Player was walking too fast while sneaking (NoSlowDownG)");
    }

    /** Returns max speed for NoSlowDownG. */
    public double getNoSlowDownGMaxSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-g.max-speed", 0.135);
    }

    /** Returns the command for NoSlowDownH. */
    public String getNoSlowDownHCommand() {
        return plugin.getConfig().getString("no-slowdown-h.command", "kick %player% Player was walking too fast while drinking (NoSlowDownH)");
    }

    /** Returns max ignore speed for NoSlowDownH. */
    public double getNoSlowDownHMaxIgnoreSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-h.ignore-after-speed", 1.0);
    }

    /** Returns max speed for NoSlowDownH. */
    public double getNoSlowDownHMaxSpeed() {
        return plugin.getConfig().getDouble("no-slowdown-h.max-speed", 0.0);
    }

    /** Returns list of ignored speeds for NoSlowDownH. */
    public List<Double> getNoSlowDownHIgnoredSpeedValues() {
        return config.getDoubleList("no-slowdown-h.ignored-speeds");
    }

    /** Returns the command for NoSlowDownH. */
    public String getNoWebACommand() {
        return plugin.getConfig().getString("no-web-a.command", "kick %player% Suspicious movement in cobwebs (NoWebA)");
    }

    /** Returns ReachA max alerts. */
    public int getMaxReachAAlerts() {
        return plugin.getConfig().getInt("reach-a.max-alerts", 5);
    }

    /** Returns the command for ReachA. */
    public String getReachACommand() {
        return plugin.getConfig().getString("reach-a.command", "kick %player% Interacting with distant block (ReachA)");
    }

    /** Returns base range for ReachA (used when no per-player attribute is present). */
    public double getReachABaseRange() {
        return plugin.getConfig().getDouble("reach-a.base-range", 5.0);
    }

    /** Returns ping threshold for ReachA. */
    public int getReachAPingThreshold() {
        return plugin.getConfig().getInt("reach-a.ping-threshold", 300);
    }

    /** Returns tolerance added to reach check for ReachA. */
    public double getReachATolerance() {
        return plugin.getConfig().getDouble("reach-a.tolerance", 0.2);
    }

    /** Returns ReachB max alerts. */
    public int getMaxReachBAlerts() {
        return plugin.getConfig().getInt("reach-b.max-alerts", 5);
    }

    /** Returns the command for ReachB. */
    public String getReachBCommand() {
        return plugin.getConfig().getString("reach-b.command", "kick %player% Interacting with distant player (ReachB)");
    }

    /** Returns base range for ReachB (used when no per-player attribute is present). */
    public double getReachBBaseRange() {
        return plugin.getConfig().getDouble("reach-b.base-range", 3.0);
    }

    /** Returns ping threshold for ReachB. */
    public int getReachBPingThreshold() {
        return plugin.getConfig().getInt("reach-b.ping-threshold", 300);
    }

    /** Returns tolerance added to reach check for ReachB. */
    public double getReachBTolerance() {
        return plugin.getConfig().getDouble("reach-b.tolerance", 0.3);
    }

    /** Generic: returns the configured string at path or default. */
    public String getString(String path, String def) {
        return config.contains(path) ? config.getString(path) : def;
    }

    /** Returns the command for TimerA. */
    public String getTimerACommand() {
        return config.getString("timer-a.command", "kick %player% You send too many packets (TimerA)");
    }

    /** Returns the command for TimerB. */
    public String getTimerBCommand() {
        return config.getString("timer-b.command", "kick %player% You send too many packets (TimerB)");
    }

    /** Returns the command for TimerC. */
    public String getTimerCCommand() {
        return config.getString("timer-c.command", "kick %player% You send too many packets (TimerC)");
    }

    /** Returns the command for TimerD. */
    public String getTimerDCommand() {
        return config.getString("timer-d.command", "kick %player% You send too many packets (TimerD)");
    }

    /** Returns ThruBlocksA max alerts. */
    public int getMaxThruBlocksAAlerts() {
        return plugin.getConfig().getInt("thru-blocks-a.max-alerts", 5);
    }

    /** Returns the command for ThruBlocksA. */
    public String getThruBlocksACommand() {
        return plugin.getConfig().getString("thru-blocks-a.command", "kick %player% Hitting through blocks (ThruBlocksA)");
    }

    /** Returns ThruBlocksb max alerts. */
    public int getMaxThruBlocksBAlerts() {
        return plugin.getConfig().getInt("thru-blocks-b.max-alerts", 5);
    }

    /** Returns the command for ThruBlocksB. */
    public String getThruBlocksBCommand() {
        return plugin.getConfig().getString("thru-blocks-b.command", "kick %player% Hitting through blocks (ThruBlocksB)");
    }

    // === IS* methods (lexicographically ordered) ===

    /** Returns whether AirPlaceA is enabled. */
    public boolean isAirPlaceAEnabled() {
        return plugin.getConfig().getBoolean("air-place-a.enabled", true);
    }

    /** Returns whether AirPlaceA cancel-event is enabled. */
    public boolean isAirPlaceACancelEvent() {
        return plugin.getConfig().getBoolean("air-place-a.cancel-event", false);
    }

    /** Returns whether AirPlaceA debug is enabled. */
    public boolean isAirPlaceADebugMode() {
        return plugin.getConfig().getBoolean("air-place-a.debug", false);
    }

    /** Returns whether AirJump cancel-event is enabled. */
    public boolean isAirJumpACancelEvent() {
        return plugin.getConfig().getBoolean("air-jump-a.cancel-event", false);
    }

    public boolean isIgnoreExternalVelocity() {
        return plugin.getConfig().getBoolean("air-jump-a.ignore-external-velocity", true);
    }

    public long getExternalVelocityGraceMs() {
        String s = config.getString("air-jump-a.external-velocity-grace-ms", "700");
        try { return Long.parseLong(s); } catch (Exception ignored) { return 700L; }
    }

    public double getMinUpwardVelocityY() {
        String s = config.getString("air-jump-a.min-upward-velocity-y", "0.25");
        try { return Double.parseDouble(s); } catch (Exception ignored) { return 0.25; }
    }

    public boolean isDetectPressurePlates() {
        return plugin.getConfig().getBoolean("air-jump-a.detect-pressure-plates", true);
    }

    public boolean isAirJumpAADetectWindCharge() {
        return plugin.getConfig().getBoolean("air-jump-a.detect-wind-charge", true);
    }

    public long getAirJumpAWindChargeGraceMs() {
        return config.getLong("air-jump-a.wind-charge-grace-ms", 900);
    }

    /** Returns vertical delta threshold (Y) above which an in-air upward movement is considered suspicious. */

    public double getAirJumpAVerticalThreshold() {
        return config.getDouble("air-jump-a.vertical-threshold", 0.3);
    }

    public double getAirJumpAMinHorizontalMovement() {
        return config.getDouble("air-jump-a.min-horizontal", 0.02);
    }

    /** Returns whether AirJump debug mode is enabled. */
    public boolean isAirJumpADebugMode() {
        return plugin.getConfig().getBoolean("air-jump-a.debug", false);
    }

    /** Returns whether AirJump is enabled. */
    public boolean isAirJumpAEnabled() {
        return plugin.getConfig().getBoolean("air-jump-a.enabled", true);
    }

    /** Is there a Speed/Jump Boost effect (avoid false positives) */
    public boolean isAirJumpAIgnorePotionBoost() {
        return config.getBoolean("air-jump-a.ignore-potion-boost", false);
    }

    /** Minimum time delay (in ms) from receiving damage to avoid a false positive knockback */
    public long getAirJumpADamageIgnoreMillis() {
        return config.getLong("air-jump-a.damage-ignore-millis", 500);
    }

    /** Returns whether AutoTotemA is enabled. */
    public boolean isAutoTotemAEnabled() {
        return config.getBoolean("auto-totem-a.enabled", true);
    }

    /** Returns whether AutoTotemA debug mode is enabled. */
    public boolean isAutoTotemADebugMode() {
        return config.getBoolean("auto-totem-a.debug", false);
    }

    /** Returns whether AutoTrapA is enabled. */
    public boolean isAutoTrapAEnabled() {
        return config.getBoolean("auto-trap-a.enabled", true);
    }

    /** Returns whether AutoTrapA cancel-event is enabled. */
    public boolean isAutoTrapACancelEvent() {
        return config.getBoolean("auto-trap-a.cancel-event", true);
    }

    /** Returns the max time (ms) to detect trap pattern for AutoTrapA. */
    public long getAutoTrapAMaxTime() {
        return config.getLong("auto-trap-a.max-time", 2000);
    }

    /** Returns minimum wood blocks required for AutoTrapA. */
    public int getAutoTrapAMinBlocks() {
        return config.getInt("auto-trap-a.min-blocks", 6);
    }

    /** Returns max distance from placed block to nearest player for AutoTrapA. */
    public double getAutoTrapAMaxDistance() {
        return config.getDouble("auto-trap-a.max-distance", 5.0);
    }

    /** Returns configured maximum alerts for AutoTrapA. */
    public int getMaxAutoTrapAAlerts() {
        return config.getInt("auto-trap-a.max-alerts", 3);
    }

    /** Returns the command for AutoTrapA. */
    public String getAutoTrapACommand() {
        return config.getString("auto-trap-a.command", "kick %player% Suspicious auto-trap detected (AutoTrapA)");
    }

    /** Returns whether AutoTrapA debug mode is enabled. */
    public boolean isAutoTrapADebugMode() {
        return config.getBoolean("auto-trap-a.debug", false);
    }

    /** Returns if Discord integration is enabled. */
    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled", false);
    }

    /** Returns whether Elytra Aim A is enabled. */
    public boolean isElytraAimAEnabled() {
        return config.getBoolean("elytra-aim-a.enabled", true);
    }

    /** Returns whether Elytra Aim A cancel-event is enabled. */
    public boolean isElytraAimACancelEvent() {
        return plugin.getConfig().getBoolean("elytra-aim-a.cancel-event", true);
    }

    /** Returns whether Elytra Aim A debug mode is enabled. */
    public boolean isElytraAimADebugMode() {
        return config.getBoolean("elytra-aim-a.debug-mode", false);
    }

    /** Returns whether Elytra Criticals A is enabled. */
    public boolean isElytraCriticalsAEnabled() {
        return config.getBoolean("elytra-criticals-a.enabled", true);
    }

    /** Returns whether Elytra Criticals A cancel-event is enabled. */
    public boolean isElytraCriticalsACancelEvent() {
        return plugin.getConfig().getBoolean("elytra-criticals-a.cancel-event", true);
    }

    /** Returns whether Elytra Criticals A debug mode is enabled. */
    public boolean isElytraCriticalsADebugMode() {
        return config.getBoolean("elytra-criticals-a.debug", false);
    }

    /** Returns whether FastClimbA is enabled. */
    public boolean isFastClimbAEnabled() {
        return config.getBoolean("fast-climb-a.enabled", true);
    }

    /** Returns whether FastClimbA cancel-event is enabled. */
    public boolean isFastClimbACancelEvents() {
        return plugin.getConfig().getBoolean("fast-climb-a.cancel-event", true);
    }

    /** Returns whether FastClimbA debug mode is enabled. */
    public boolean isFastClimbADebugMode() {
        return plugin.getConfig().getBoolean("fast-climb-a.debug", false);
    }

    /** Returns whether FastPlaceA is enabled. */
    public boolean isFastPlaceAEnabled() {
        return plugin.getConfig().getBoolean("fast-place-a.enabled", true);
    }

    /** Returns whether FastPlaceA cancel-event is enabled. */
    public boolean isFastPlaceACancelEvent() {
        return plugin.getConfig().getBoolean("fast-place-a.cancel-event", true);
    }

    /** Returns whether FastPlaceA debug mode is enabled. */
    public boolean isFastPlaceADebugMode() {
        return plugin.getConfig().getBoolean("fast-place-a.debug", false);
    }

    /**
     * Whether the FlyA check is enabled.
     * Default: true
     */
    public boolean isFlyAEnabled() {
        return config.getBoolean("fly-a.enabled", true);
    }

    /**
     * Whether FlyA debug logging is enabled.
     * When true the check will log diagnostic information to the server console.
     * Default: false
     */
    public boolean isFlyADebugMode() {
        return config.getBoolean("fly-a.debug", false);
    }

    /**
     * If true, the check will cancel the movement event when a violation is detected.
     * Be careful: cancelling PlayerMoveEvent can have side-effects with other plugins.
     * Default: false
     */
    public boolean isFlyACancelEvent() {
        return config.getBoolean("fly-a.cancel-event", false);
    }

    /**
     * Whether FlyA should ignore players who have a jump boost potion.
     * Many servers allow Jump Boost; enabling this reduces false positives.
     * Default: true
     */
    public boolean isFlyAIgnoreJumpBoost() {
        return config.getBoolean("fly-a.ignore-jump-boost", true);
    }

    /** Returns whether GroundSpoofA is enabled. */
    public boolean isGroundSpoofAEnabled() {
        return plugin.getConfig().getBoolean("ground-spoof-a.enabled", true);
    }

    /** Returns whether GroundSpoofA debug mode is enabled. */
    public boolean isGroundSpoofADebugMode() {
        return plugin.getConfig().getBoolean("ground-spoof-a.debug", false);
    }

    /** Returns whether GroundSpoofA should cancel the event when detected. */
    public boolean isGroundSpoofACancelEvent() {
        return plugin.getConfig().getBoolean("ground-spoof-a.cancel-event", false);
    }

    /** Returns whether HitboxA is enabled. */
    public boolean isHitboxAEnabled() { return config.getBoolean("hitbox-a.enable", true); }

    /** Returns whether HitboxA debug mode is enabled. */
    public boolean isHitboxADebugMode() { return config.getBoolean("hitbox-a.debug", false); }

    /** Returns whether FastPlaceA cancel-event is enabled. */
    public boolean isHitboxACancelEvent() { return plugin.getConfig().getBoolean("hitbox-a.cancel-event", true); }

    /** Returns whether InvalidPlaceA is enabled. */
    public boolean isInvalidPlaceAEnabled() {
        return plugin.getConfig().getBoolean("invalid-place-a.enabled", true);
    }

    /** Returns whether InvalidPlaceA cancel-event is enabled. */
    public boolean isInvalidPlaceACancelEvent() {
        return plugin.getConfig().getBoolean("invalid-place-a.cancel-event", false);
    }

    /** Returns whether NoWebA is enabled. */
    public boolean isNoWebAEnabled() {
        return plugin.getConfig().getBoolean("no-web-a.enabled", true);
    }

    /** Returns whether NoWebA cancel-event is enabled. */
    public boolean isNoWebACancelEvent() {
        return plugin.getConfig().getBoolean("no-web-a.cancel-event", true);
    }

    /** Returns whether NoWebA debug mode is enabled. */
    public boolean isNoWebADebugMode() {
        return config.getBoolean("no-web-a.debug", false);
    }

    /** Returns whether NoSlowDownA is enabled. */
    public boolean isNoSlowDownAEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-a.enabled", true);
    }

    /** Returns whether NoSlowDownA debug is enabled. */
    public boolean isNoSlowDownADebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-a.debug", false);
    }

    /** Returns whether NoSlowDownB is enabled. */
    public boolean isNoSlowDownBEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-b.enabled", true);
    }

    /** Returns whether NoSlowDownB debug is enabled. */
    public boolean isNoSlowDownBDebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-b.debug", false);
    }

    /** Returns whether NoSlowDownC is enabled. */
    public boolean isNoSlowDownCEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-c.enabled", true);
    }

    /** Returns whether NoSlowDownC debug is enabled. */
    public boolean isNoSlowDownCDebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-c.debug", false);
    }

    /** Returns whether NoSlowDownD is enabled. */
    public boolean isNoSlowDownDEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-d.enabled", true);
    }

    /** Returns whether NoSlowDownD debug is enabled. */
    public boolean isNoSlowDownDDebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-d.debug", false);
    }

    /** Returns whether NoSlowDownE is enabled. */
    public boolean isNoSlowDownEEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-e.enabled", true);
    }

    /** Returns whether NoSlowDownE debug is enabled. */
    public boolean isNoSlowDownEDebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-e.debug", false);
    }

    /** Returns whether NoSlowDownF is enabled. */
    public boolean isNoSlowDownFEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-f.enabled", true);
    }

    /** Returns whether NoSlowDownF debug is enabled. */
    public boolean isNoSlowDownFDebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-f.debug", false);
    }

    /** Returns whether NoSlowDownG is enabled. */
    public boolean isNoSlowDownGEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-g.enabled", true);
    }

    /** Returns whether NoSlowDownG debug is enabled. */
    public boolean isNoSlowDownGDebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-g.debug", false);
    }

    /** Returns whether NoSlowDownH is enabled. */
    public boolean isNoSlowDownHEnabled() {
        return plugin.getConfig().getBoolean("no-slowdown-h.enabled", true);
    }

    /** Returns whether NoSlowDownH debug is enabled. */
    public boolean isNoSlowDownHDebugMode() {
        return plugin.getConfig().getBoolean("no-slowdown-h.debug", false);
    }

    /** Returns whether ReachA is enabled. */
    public boolean isReachAEnabled() {
        return plugin.getConfig().getBoolean("reach-a.enabled", true);
    }

    /** Returns whether ReachA cancel-event is enabled. */
    public boolean isReachACancelEvent() {
        return plugin.getConfig().getBoolean("reach-a.cancel-event", false);
    }

    /** Returns whether ReachA debug mode is enabled. */
    public boolean isReachADebugMode() {
        return plugin.getConfig().getBoolean("reach-a.debug", false);
    }

    /** Returns whether ReachB is enabled. */
    public boolean isReachBEnabled() {
        return plugin.getConfig().getBoolean("reach-b.enabled", true);
    }

    /** Returns whether ReachB cancel-event is enabled. */
    public boolean isReachBCancelEvent() {
        return plugin.getConfig().getBoolean("reach-b.cancel-event", false);
    }

    /** Returns whether ReachB debug mode is enabled. */
    public boolean isReachBDebugMode() {
        return plugin.getConfig().getBoolean("reach-b.debug", false);
    }

    /** Returns whether ThruBlocksA is enabled. */
    public boolean isThruBlocksAEnabled() {
        return plugin.getConfig().getBoolean("thru-blocks-a.enabled", true);
    }

    /** Returns whether ThruBlocksA cancel-event is enabled. */
    public boolean isThruBlocksACancelEvent() {
        return plugin.getConfig().getBoolean("thru-blocks-a.cancel-event", false);
    }

    /** Returns whether ThruBlocksA debug is enabled. */
    public boolean isThruBlocksADebugMode() {
        return plugin.getConfig().getBoolean("thru-blocks-a.debug", false);
    }

    /** Returns whether ThruBlocksB is enabled. */
    public boolean isThruBlocksBEnabled() {
        return plugin.getConfig().getBoolean("thru-blocks-b.enabled", true);
    }

    /** Returns whether ThruBlocksB cancel-event is enabled. */
    public boolean isThruBlocksBCancelEvent() {
        return plugin.getConfig().getBoolean("thru-blocks-b.cancel-event", false);
    }

    /** Returns whether ThruBlocksB debug is enabled. */
    public boolean isThruBlocksBDebugMode() {
        return plugin.getConfig().getBoolean("thru-blocks-b.debug", false);
    }

    /** Returns whether TimerA is enabled. */
    public boolean isTimerAEnabled() {
        return config.getBoolean("timer-a.enabled", true);
    }

    /** Returns whether TimerA cancel events are enabled. */
    public boolean isTimerACancelEvents() {
        return plugin.getConfig().getBoolean("timer-a.cancel-event", true);
    }

    /** Returns whether TimerA debug is enabled. */
    public boolean isTimerADebugMode() {
        return plugin.getConfig().getBoolean("timer-a.debug", false);
    }

    /** Returns whether TimerB is enabled. */
    public boolean isTimerBEnabled() {
        return config.getBoolean("timer-b.enabled", true);
    }

    /** Returns whether TimerB cancel events are enabled. */
    public boolean isTimerBCancelEvents() {
        return plugin.getConfig().getBoolean("timer-b.cancel-event", true);
    }

    /** Returns whether TimerB debug is enabled. */
    public boolean isTimerBDebugMode() {
        return plugin.getConfig().getBoolean("timer-b.debug", false);
    }

    /** Returns whether TimerC is enabled. */
    public boolean isTimerCEnabled() {
        return config.getBoolean("timer-c.enabled", true);
    }

    /** Returns whether TimerC cancel events are enabled. */
    public boolean isTimerCCancelEvents() {
        return plugin.getConfig().getBoolean("timer-c.cancel-event", true);
    }

    /** Returns whether TimerC debug is enabled. */
    public boolean isTimerCDebugMode() {
        return plugin.getConfig().getBoolean("timer-c.debug", false);
    }

    /** Returns whether TimerD is enabled. */
    public boolean isTimerDEnabled() {
        return config.getBoolean("timer-d.enabled", true);
    }

    /** Returns whether TimerD cancel events are enabled. */
    public boolean isTimerDCancelEvents() {
        return plugin.getConfig().getBoolean("timer-d.cancel-event", true);
    }

    /** Returns whether TimerD debug is enabled. */
    public boolean isTimerDDebugMode() {
        return plugin.getConfig().getBoolean("timer-d.debug", false);
    }

    // === SHOULD* methods (lexicographically ordered) ===

    /** Returns whether NoSlowDownA should cancel event. */
    public boolean shouldNoSlowDownACancelEvent() {
        return plugin.getConfig().getBoolean("no-slowdown-a.cancel-event", true);
    }

    /** Returns whether NoSlowDownB should cancel event. */
    public boolean shouldNoSlowDownBCancelEvent() {
        return plugin.getConfig().getBoolean("no-slowdown-b.cancel-event", true);
    }

    /** Returns whether NoSlowDownC should cancel event. */
    public boolean shouldNoSlowDownCCancelEvent() {
        return plugin.getConfig().getBoolean("no-slowdown-c.cancel-event", true);
    }

    /** Returns whether NoSlowDownD should cancel event. */
    public boolean shouldNoSlowDownDCancelEvent() {
        return plugin.getConfig().getBoolean("no-slowdown-d.cancel-event", true);
    }

    /** Returns whether NoSlowDownH should cancel event. */
    public boolean shouldNoSlowDownHCancelEvent() {
        return plugin.getConfig().getBoolean("no-slowdown-h.cancel-event", false);
    }
}