# 🦆 DuckyAC

**DuckyAC** is a lightweight, modular, and fully configurable anti-cheat plugin for Spigot/Paper Minecraft servers.  
It detects unnatural player behavior in block placing, movement, and other interactions to effectively prevent cheating.

> ✅ **Recommended**: Use **DuckyAC** in combination with **[Vulcan](https://www.spigotmc.org/resources/vulcan-anti-cheat-advanced-cheat-detection-1-8-1-21-5.83626/)** and **[GrimAC](https://modrinth.com/plugin/lightning-grim-anticheat)** for maximum cheat detection coverage.

---

## ⚙️ Features

- 🔍 **Checks List**:
  - `AirJumpA`: Detects Jumping in air
  - `AirPlaceA`: Detects when player are building mid air  
  - `AutoTotemA`: Detects suspicious totem planting (beta check)
  - `ElytraAimA`: Player was detected to be hitting too fast while flying an elytra
  - `ElytraCriticalsA`: Detects when a player deals too much critical damage while flying an elytra in too short a time
  - `GroundSpoofA`: Detects when player is sending false informations about his position
  - `FastClimbA`: Detects to fast player climbing on ladder, vines etc.
  - `FastPlaceA`: Detects when a player places too many blocks at a time
  - `FlyA`: Detects when player is flying *basic fly*
  - `InvalidPlaceA`: Detects when a player has placed a block at the wrong angle
  - `NoSlowDownA-G`: Many features of the player walking too fast during certain activities
  - `NoWebA`: Detects player movement that is too fast while in a web
  - `ReachA-B`: Detects when player is interacting with entities/blocks to far away his distance
  - `ThruBlocksA-B`: Detects when a player hits another player through a wall
  - `TimerA-D`: Detects when a player sends too many packets
- 📉 Violation Level (VL) system for tracking repeated offenses
- 🔧 Fully configurable thresholds, punishments, and enabled checks
- 🛡 Permission-based bypass support (e.g., for admins)
- 🔔 Alerts in chat and console to authorized staff (`duckyac.alerts`)
- 📩 Discord webhook support for sending logs and punishments
- 🧠 Config setting caching for performance optimization
- 🔄 Optional command to reload config (`/duckyac reload`)

---

## 🧪 Sample Configuration (`config.yml`)

```yaml
# === DUCKY ANTICHEAT CONFIGURATION ===
# All checks are listed alphabetically. Each check has a short English description above it.

# Time after which a player's violations reset (in seconds).
# Example: if a player gets 2 reports for a check, after this timeout their counter resets to 0.
alert-timeout: 300

# === VIOLATION ALERT MESSAGE TEMPLATE ===
# Message format used when broadcasting alerts to staff / console.
alert-message: "&d&lDuckyAC &8» &fPlayer &7»&f %player% &7»&6 %check% &7(&c%vl%VL&7)"

# === CHECKS (alphabetically ordered) ===

# AIR JUMP A
# Detects jumping in the air
air-jump-a:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  ignore-external-velocity: true
  min-upward-velocity-y: 0.25
  detect-pressure-plates: true
  jumppad-plate-grace-ms: 450
  min-horizontal: 0.02
  external-velocity-grace-ms: 700
  vertical-threshold: 0.3
  damage-ignore-millis: 500
  ignore-potion-boost: false # Keep it false; it's a disabling player checking when player has Jump Boost effect
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Unnatural air jump detected (AirJumpA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# AIR PLACE A
# Detects block placements in midair (no supporting blocks nearby).
air-place-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-alerts: 3 # Number of reports after which the command is executed
  command: "kick %player% Invalid block placement (AirPlaceA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# AUTO TOTEM A
# Detects extremely fast totem swaps (auto totem macros).
auto-totem-a:
  enabled: true # Function to enable/disable check
  min-delay: 150 # Minimum allowed swap interval in ms
  tick-interval: 3
  max-ping: -1
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Too fast totem swap (AutoTotemA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# ELYTRA AIM A
# Detects abnormal aiming/targeting while using an Elytra (aim assistance).
elytra-aim-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-firework-delay: 200 # Time in milliseconds when a player hits another player a second time within the set time will be reported
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Cheating with Elytra (ElytraAimA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# ELYTRA CRITICALS A
# Detects repeated "critical" hits while using Elytra that may indicate cheats.
elytra-criticals-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  critical-hits-required: 3 # The number of critical hits a player can land in a time frame to report the player.
  timeframe: 700 # Timeframe in milliseconds to count critical hits
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Cheating with Elytra (ElytraCriticalsA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# FAST CLIMB A
# Detects unnatural fast ladder/climb movement.
fast-climb-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-climb-speed: 0.15
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% You climbed too fast (FastClimbA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# FAST PLACE A
# Detects placing blocks too quickly (anti-fast-place).
fast-place-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-per-tick: 4
  max-alerts: 3 # Number of reports after which the command is executed
  command: "kick %player% Too fast block placement (FastPlaceA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# FLY A
fly-a:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  ignore-jump-boost: true
  ping-threshold: 250
  hover-delta: 0.01
  hover-ticks: 15
  ascend-delta: 0.42
  ascend-ticks: 8
  damage-ignore-millis: 1500
  teleport-ignore-millis: 2000
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Flying (FlyA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# === GROUND SPOOF A ===
# Heuristic check for clients that lie about being "on ground".
# Conservative defaults chosen to minimise false positives.
ground-spoof-a:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  ping-threshold: 300
  check-depth: 3.0
  min-ground-distance: 0.50
  vertical-tolerance: 0.02
  damage-ignore-millis: 300
  teleport-ignore-millis: 500
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Suspected ground spoof (GroundSpoofA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# HITBOX A
# Detects attackers that appear to hit targets outside reasonable hitbox angles/distances.
hitbox-a:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-angle: 40.0
  max-distance: 3.5
  max-tolerance: 0.2
  ping-threshold: 300
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Too large hitboxes (HitboxA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# INVALID PLACE A
# Detects block placements with an invalid looking angle (player not plausibly looking at placement).
invalid-place-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-angle: 50             # Maximum angle allowed between a look direction and placed block
  max-alerts: 3 # Number of reports after which the command is executed
  command: "kick %player% Invalid block placement (InvalidPlaceA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# NO WEB A
# Detects suspicious movement while in cobwebs (NoWeb cheats).
no-web-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-alerts: 3 # Number of reports after which the command is executed
  command: "kick %player% Suspicious movement in cobwebs (NoWebA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# NO SLOWDOWN B / C / D
# NoSlowDown checks for different situations (bow, crossbow, shield).
no-slowdown-max-distance: 1.0 # ignore distance used by multiple NoSlowDown checks

no-slowdown-a:
  # Detects walking too fast while eating.
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-eating-speed: 0.20
  max-distance: 1.0
  ignored-speeds:
    - 0.5072
    - 0.3024
    - 0.2933
    - 0.4822
    - 0.5013
    - 0.5014
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% Player was walking too fast while eating (NoSlowDownA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

no-slowdown-b:
  # Detects walking too fast with a drawn bow.
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-bow-speed: 0.20
  ignored-speeds:
    - 0.5072
    - 0.3024
    - 0.2933
    - 0.4822
    - 0.5013
    - 0.5014
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% Player was walking too fast with a drawn bow (NoSlowDownB)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

no-slowdown-c:
  # Detects walking too fast while drawing a crossbow.
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-speed: 0.20
  ignored-speeds:
    - 0.5072
    - 0.3024
    - 0.2933
    - 0.4822
    - 0.5013
    - 0.5014
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% Player walked too fast while drawing the crossbow (NoSlowDownC)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

no-slowdown-d:
  # Detects walking too fast while holding a shield.
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-speed: 0.20
  ignored-speeds:
    - 0.5072
    - 0.3024
    - 0.2933
    - 0.4822
    - 0.5013
    - 0.5014
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% Player was walking too fast while holding a shield (NoSlowDownD)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

no-slowdown-e:
  # Detects walking too fast on honey blocks.
  enabled: true # Function to enable/disable check
  max-speed: 0.170
  max-alerts: 5
  command: "kick %player% Player was walking too fast on honey block (NoSlowDownE)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

no-slowdown-f:
  # Detects walking too fast on soul sand.
  enabled: true # Function to enable/disable check
  max-speed: 0.170
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Player was walking too fast on soul sand (NoSlowDownF)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

no-slowdown-g:
  # Detects walking too fast while sneaking.
  enabled: true # Function to enable/disable check
  max-speed: 0.135
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% Player was walking too fast while sneaking (NoSlowDownG)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

no-slowdown-h:
  # Detects walking too fast while drinking; includes an "ignore-after-speed" threshold.
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-speed: 0.20
  ignore-after-speed: 1.0 # The player is unable to go faster than a value of 1, so any value above that will be false, this feature prevents false reports
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% Player was walking too fast while drinking (NoSlowDownH)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# REACH A
# Detects interacting with (placing or breaking) a block from an impossible distance.
# Takes into account per-player block_interaction_range attribute where available, plus tolerance and ping adjustments.
reach-a:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  base-range: 3.0 # Default base reach (used when player attribute is not available)
  tolerance: 0.2 # Extra tolerance added to reach checks
  ping-threshold: 300 # If player's ping exceeds this, the check will grant an extra 0.5 block tolerance for player
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Interacting with distant block (ReachA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# REACH B
# Detects attacking players/mobs from an impossible (too far) distance.
# Uses configurable base range, tolerance, and ping threshold.
reach-b:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  base-range: 3.0 # Default base reach (used when player attribute is not available)
  tolerance: 0.2 # Extra tolerance added to reach checks
  ping-threshold: 300 # If player's ping exceeds this, the check will grant an extra 0.5 block tolerance for player
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Interacting with distant player (ReachB)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# THRU BLOCKS A
# Detects hitting entities through blocks (A variant).
thru-blocks-a:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Hitting through blocks (ThruBlocksA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# THRU BLOCKS B
# Detects hitting entities through blocks (B variant).
thru-blocks-b:
  enabled: true # Function to enable/disable check
  cancel-event: false # Enabling this feature will take you back to the position before reporting
  max-alerts: 5 # Number of reports after which the command is executed
  command: "kick %player% Hitting through blocks (ThruBlocksB)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# TIMER checks (A.D)
# Detects abnormal packet timing / fast-clicking / timer manipulation.
timer-a:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-packets-per-second: 24
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% You send too many packets (TimerA)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

timer-b:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-packets-per-second: 24
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% You send too many packets (TimerB)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

timer-c:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-packets-per-second: 24
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% You send too many packets (TimerC)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

timer-d:
  enabled: true # Function to enable/disable check
  cancel-event: true # Enabling this feature will take you back to the position before reporting
  max-packets-per-second: 30
  max-alerts: 10 # Number of reports after which the command is executed
  command: "kick %player% You send too many packets (TimerD)" # The command that will be executed when the maximum number of reports is reached
  debug: false # Enable verbose debugging (may cause log spam / lag)

# === DISCORD / MESSAGES / MISC ===

# Discord integration settings (optional).
discord:
  enabled: false
  discord-webhook-url: "https://discord.com/api/webhooks/your-webhook-id"
  username: "DuckyAntiCheat"
  avatar-url: "https://i.imgur.com/ahbEPVO.png"
  violation-message-template: "**AntiCheatSystem**\nPlayer: **%player%**\nCheck: **%check%**\nViolation: **%vl%**"
  punishment-message-template: "**Punishment Executed**\nPlayer: **%player%**\nCommand: `%command%`"

# Misc messages and labels used by plugin commands.
no-permission: "&d&lDuckyAC &8» &cNo Permission!"
incorrect-usage: '&d&lDuckyAC &8» &cUsage: /duckyac reload'
update-available: "&d&lDuckyAC &8» &eA new version is available: &c%version%"
update-download: "&d&lDuckyAC &8» &eDownload: &a%url%"
update-check-failed: "&d&lDuckyAC &8» &cCould not check for updates."
player-only: "&d&lDuckyAC &8» &cOnly Players can use this command."
config-reloaded: '&d&lDuckyAC &8» &aConfiguration reloaded.'
plugin-reloaded: '&d&lDuckyAC &8» &aPlugin successfully reloaded.'
```

---

## 🔐 Permissions

| Permission | Description |
|------------|-------------|
| `duckyac.bypass` | Completely disables checks for the player |
| `duckyac.bypass.<checkname>-<check-letter example- a>` | Disables only defined check for the player |
| `duckyac.*` | Full access (bypass + admin permissions) |
| `duckyac.alerts` | Receive alert messages in chat |
| `duckyac.update` | Receive messages in chat about an available update |

---

## 🚀 Installation

1. Place `DuckyAC-x.x.x.jar` in your server’s `plugins/` folder.
2. Start your server.
3. Configure the plugin in `plugins/DuckyAC/config.yml`.
4. Reload plugin with `/duckyac reload`.
5. Done!

---

## 🛠 Planned Features

- Add more combat checks!

---

## 🤝 Contributing / Support

Found a bug or have a suggestion?  
Reach out via Discord or open an issue in the GitHub repository!

---

## 📜 License

DuckyAC is licensed under the GPL-3.0 License.  
You are free to use, modify, and redistribute it under the terms of the license.

---

## 💡 Recommendation

For the best protection against modern cheat clients, it is **strongly recommended** to run **DuckyAC alongside [Vulcan](https://www.spigotmc.org/resources/vulcan-anti-cheat-advanced-cheat-detection-1-8-1-21-5.83626/)** and **[GrimAC](https://modrinth.com/plugin/lightning-grim-anticheat)**.  
Each plugin covers different types of exploits, and together they form a powerful defense.

---
