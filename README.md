<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-brightgreen?style=for-the-badge&logo=mojang-studios&logoColor=white" alt="Minecraft 1.21+"/>
  <img src="https://img.shields.io/badge/Paper-API-blue?style=for-the-badge" alt="Paper"/>
  <img src="https://img.shields.io/badge/Java-21+-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21+"/>
  <img src="https://img.shields.io/badge/version-1.2.1-blueviolet?style=for-the-badge" alt="Version"/>
</p>

<h1 align="center">❄️ FrostCore</h1>

<p align="center">
  <b>The only core plugin your server will ever need.</b><br/>
  <sub>Teams · Moderation · Teleportation · Homes · Warps · Cosmetics · Administration — all in one.</sub>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#%EF%B8%8F-installation">Installation</a> •
  <a href="#-commands">Commands</a> •
  <a href="#-permissions">Permissions</a> •
  <a href="#-configuration">Configuration</a> •
  <a href="#-database">Database</a> •
  <a href="#-placeholderapi">PlaceholderAPI</a>
</p>

---

## ✨ Features

FrostCore replaces **Essentials, LiteBans, BetterTeams, HuskHomes, DeluxeMenus**, and more — with one lightweight, fully integrated, premium-quality plugin. Every system is built from scratch with performance, aesthetics, and configurability in mind.

### 🏴‍☠️ Team System
> A full-featured faction-style team system with roles, relations, and shared storage.

- **Create & Manage Teams** — `/team create`, `/team disband`, `/team rename`, `/team settag`, `/team color`
- **Role Hierarchy** — Owner → Admin → Member with configurable limits per role
- **Team Chat** — Private, toggleable chat channel for team members only
- **Allies & Enemies** — Invite-based alliance system with configurable max relations
- **Team PvP Toggle** — Friendly fire protection between teammates
- **Team Home & Warps** — Shared teleport points with per-team warp limits
- **Team Ender Chest** — Shared storage inventory (configurable slots: 9–54) persisted to database
- **Team List** — Paginated list of all teams on the server
- **Admin Override** — Staff can force-disband, force-join, and manage any team

### 🏠 Homes & Warps
> Personal homes and server-wide warps with animated GUIs.

- **Personal Homes** — Set, teleport, rename, and delete personal homes
- **Max Homes by Permission** — Default limit configurable, override with `frostcore.homes.limit.<n>`
- **Homes GUI** — Beautiful interactive menu with team home integration
- **Server Warps** — Admin-defined teleport points with custom icons
- **Warps GUI** — Paginated, configurable chest interface for browsing warps
- **Spawn Management** — `/setspawn`, `/spawn` with join and respawn teleport options

### 🔀 Teleportation Engine
> One unified teleport system powering every teleport in the plugin.

- **TPA / TPAHere** — Request-based player teleportation with expiry timers
- **TPA Toggle** — Players can disable incoming requests
- **Admin TP** — `/tp`, `/tphere`, `/tp2p`, `/tpall`, `/otp` (offline TP)
- **Smart Warmups** — Configurable countdown with animated action bar (●●●○○ style or text)
- **Cooldowns** — Per-command cooldowns with bypass permissions
- **Async Chunk Loading** — Chunks are loaded asynchronously before teleporting for zero-lag warps
- **Back Command** — `/back` returns you to your previous location (death, teleport, etc.)
- **Movement Cancellation** — Teleports cancel if the player moves during the warmup
- **Particles, Sounds & Titles** — Configurable arrival effects

### 🛡️ Moderation Suite
> Enterprise-grade moderation with 35+ commands, offline support, and Discord integration.

**Punishments:**
| Command | Description |
|---|---|
| `/ban` `/tempban` `/unban` | Permanent & temporary bans with reason tracking |
| `/mute` `/tempmute` `/unmute` | Chat muting with blocked command list |
| `/warn` `/unwarn` `/warnings` | Warning system with template support |
| `/kick` | Instant kick with reason |
| `/ipban` `/ipmute` | IP-based punishments that auto-catch alt accounts |
| `/jail` `/unjail` `/setjail` `/deljail` | Jail system with named jail locations |
| `/freeze` | Freeze players in place for screenshare sessions |
| `/screenshare` | Full screenshare mode with inventory lock and isolation |

**Investigation:**
| Command | Description |
|---|---|
| `/history` | Full punishment history for any player |
| `/staffhistory` | View all punishments issued by a specific staff member |
| `/checkban` `/checkmute` `/checkwarn` | Inspect active punishment details by player or ID |
| `/banlist` `/mutelist` `/warnlist` | Paginated lists of all active punishments |
| `/alts` `/dupeip` | Detect alt accounts sharing the same IP |
| `/iphistory` | View all IPs a player has connected from |
| `/namehistory` | View previous usernames |
| `/whois` `/seen` | Detailed player information & last login |

**Staff Tools:**
| Command | Description |
|---|---|
| `/staffchat` `/sc` | Private staff-only chat channel |
| `/lockchat` `/unlockchat` | Lock global chat for all non-staff |
| `/lockdown` | Server lockdown — block all non-whitelisted joins |
| `/prunehistory` | Bulk-delete old punishment records |
| `/staffrollback` | Undo all punishments by a rogue staff member |
| `/reports` `/report` | Player report system with staff review |

**Key Features:**
- 📋 **Silent Punishments** — Append `-s` to any punishment to hide it from broadcast
- 📝 **Punishment Templates** — Use `-t <name>` to apply predefined reasons instantly
- ⚖️ **Group Weight System** — Lower-ranked staff cannot punish higher-ranked staff
- 🔔 **Staff Notifications** — All actions broadcast to online staff in real-time
- 🪝 **Discord Webhooks** — Punishments, reports, staff chat, and admin actions post to Discord with rich embeds
- 📊 **Audit Logging** — Every moderation action is logged to `plugins/FrostCore/logs/audit.log`

### 🛠️ Administration
> Everything you need to manage your server efficiently.

| Command | Description |
|---|---|
| `/gm` `/gms` `/gmc` `/gma` `/gmsp` | Quick gamemode switching (self & others) |
| `/fly` | Toggle flight with optional speed argument |
| `/heal` `/feed` | Heal and feed yourself or others |
| `/god` | Toggle invincibility |
| `/speed` | Change walk/fly speed (1–10) |
| `/clear` | Clear inventory |
| `/vanish` `/v` | Full vanish with join/leave message suppression |
| `/invsee` | Live view & edit of another player's inventory |
| `/enderchest` `/ec` | Open your own or another player's ender chest |
| `/hat` | Wear the held item on your head |
| `/nick` `/unnick` | Set MiniMessage-formatted nicknames |
| `/sudo` | Force a player to chat or run a command |
| `/broadcast` | Server-wide formatted announcements |
| `/smite` | Strike a player with lightning |
| `/skull` | Get any player's head as an item |
| `/socialspy` | See all private messages server-wide |
| `/ram` | Live server performance dashboard (TPS, memory, player count) |
| `/top` `/bottom` | Instant vertical teleportation |
| `/near` | Radar — list nearby players with distances |
| `/coords` | View your or another player's coordinates |
| `/day` `/night` `/time` `/weather` | World environment controls |
| `/frostcore reload` | Hot-reload all configuration files |

### 💬 Private Messaging
> Full PM system with social features.

- `/msg` `/tell` `/w` `/whisper` `/pm` — Send private messages (works from console too!)
- `/r` `/reply` — Quick reply to your last conversation
- `/ignore` — Block messages from specific players
- `/socialspy` — Staff can monitor all private conversations
- **Sound Notifications** — Recipients hear a subtle ping sound

### ⚔️ Mace Limiter
> The most advanced Mace item control system for survival servers.

- **Global & Per-Player Limits** — Restrict the total number of Maces on the entire server
- **UUID Tracking** — Every Mace gets a persistent `PersistentDataContainer` ID tag for lifecycle tracking
- **Automatic Crafting Lock** — Crafting recipe disables when the global limit is reached
- **Enchantment Caps** — Set max levels for Density, Breach, and Wind Burst
- **PvP Cooldown** — Configurable cooldown between Mace hits in combat
- **Damage Cap** — Limit maximum damage per Mace hit
- **Destroy on Death** — Optionally remove Maces from drops on player death
- **Admin GUI** — `/mace` opens a sleek settings interface for live configuration changes
- **Mace Registry GUI** — View all tracked Maces, see current holders, teleport to locations, or destroy remotely
- **Pending Removal Queue** — If a Mace is destroyed while the holder is offline, it's queued and automatically removed when they log in
- **Periodic Scanner** — Scans all online inventories, ender chests, and team echests every 60 seconds
- **Staff Notifications** — Real-time alerts when Maces are crafted, transferred, or destroyed
- **Untracked Mace Handling** — Auto-track, confiscate, or ignore unregistered Maces (configurable)

### ✨ Glow Cosmetics
> Permission-based colored glow outlines for players.

- **16 Colors** — White, Light Gray, Gray, Black, Red, Dark Red, Orange, Yellow, Lime, Green, Aqua, Cyan, Blue, Dark Blue, Pink, Purple
- **Color Picker GUI** — Beautiful interactive menu showing locked/unlocked colors
- **Permission-Based** — Each color has its own permission node (`frostcore.glow.<color>`)
- **Admin Force-Set** — Staff can set glow on any player: `/glow <player> <color>`
- **Automatic Cleanup** — Glows are removed on disconnect and plugin disable

### 🖥️ GUI Framework
> A custom-built, reusable GUI engine powering every interactive menu.

- **Simple, Paged & Switcher GUIs** — Different GUI types for different needs
- **Auto-Pagination** — Content automatically paginates with nav arrows
- **Confirmation Dialogs** — Built-in confirm/cancel prompts for destructive actions
- **Borders & Templates** — Consistent glass-pane borders across all menus
- **Click Safety** — All clicks are cancelled by default to prevent item theft

### 🎨 Design & Aesthetics
> Every message, GUI, and notification uses a carefully curated visual identity.

FrostCore uses **MiniMessage** formatting throughout with a consistent premium color palette:

| Role | Color | Hex |
|---|---|---|
| Primary / Accents | Steel Blue | `#6B8DAE` |
| Body Text | Soft Lavender | `#8FA3BF` |
| Success / Enabled | Sage Green | `#7ECFA0` |
| Error / Disabled | Muted Rose | `#D4727A` |
| Warnings / Enchants | Warm Gold | `#D4A76A` / `#C8A87C` |
| Muted / Expired | Dim Gray | `#707880` |

Every message is **fully customizable** via `messages.yml` with placeholder support and nested prefix references.

---

## ⚙️ Installation

1. Download the latest `FrostCore-1.2.1.jar`
2. Place it in your server's `plugins/` folder
3. Start (or restart) your server
4. Edit `plugins/FrostCore/config.yml` to your liking
5. Use `/frostcore reload` or `/fc reload` to apply changes

**Requirements:**
- Paper 1.21+ (or any Paper fork like Purpur)
- Java 21+
- No other dependencies required — all libraries are bundled automatically

**Optional:**
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — For team placeholders in chat/tab

---

## 📋 Commands

FrostCore registers **90+ commands** across these categories:

| Category | Count | Examples |
|---|---|---|
| Team Management | 25+ subcommands | `/team create`, `/team invite`, `/team echest` |
| Teleportation | 18 commands | `/tpa`, `/home`, `/warp`, `/spawn`, `/back` |
| Moderation | 36 commands | `/ban`, `/mute`, `/jail`, `/freeze`, `/history` |
| Administration | 22 commands | `/vanish`, `/invsee`, `/sudo`, `/ram`, `/whois` |
| Player Utilities | 10 commands | `/fly`, `/heal`, `/god`, `/glow`, `/ping` |
| Messaging | 5 commands | `/msg`, `/r`, `/ignore`, `/socialspy`, `/staffchat` |
| Item Editing | 3 commands | `/itemrename`, `/lore`, `/repair` |
| Mace System | 1 command (8 subs) | `/mace info`, `/mace settings`, `/mace destroy` |

---

## 🔑 Permissions

All permissions follow the `frostcore.<category>.<action>` pattern.

<details>
<summary><b>Click to expand full permissions list</b></summary>

### Player Permissions (default: true)
| Permission | Description |
|---|---|
| `frostcore.team.use` | Access to /team commands |
| `frostcore.tpa` | TPA commands |
| `frostcore.warp` | Use server warps |
| `frostcore.spawn` | Teleport to spawn |
| `frostcore.sethome` | Set personal homes |
| `frostcore.home` | Teleport to homes |
| `frostcore.homes` | Open homes GUI |
| `frostcore.delhome` | Delete homes |
| `frostcore.renamehome` | Rename homes |
| `frostcore.ping` | Check your ping |
| `frostcore.message` | Private messaging |
| `frostcore.report` | Report players |
| `frostcore.utility.back` | Use /back |
| `frostcore.glow.use` | Use /glow |

### Staff Permissions (default: op)
| Permission | Description |
|---|---|
| `frostcore.admin` | Admin teleport commands |
| `frostcore.admin.vanish` | Toggle vanish |
| `frostcore.admin.invsee` | View player inventories |
| `frostcore.admin.sudo` | Force player actions |
| `frostcore.admin.broadcast` | Server announcements |
| `frostcore.admin.socialspy` | Monitor PMs |
| `frostcore.admin.ram` | Server performance |
| `frostcore.moderation.*` | All moderation commands |
| `frostcore.mace.admin` | Mace limiter management |
| `frostcore.glow.admin` | Set glow on others |
| `frostcore.glow.*` | Access all glow colors |

### Bypass Permissions
| Permission | Description |
|---|---|
| `frostcore.bypass.cooldown` | Skip teleport cooldowns |
| `frostcore.bypass.delay` | Skip teleport warmups |
| `frostcore.mace.bypass` | Bypass mace limits |
| `frostcore.mace.bypass.enchant` | Bypass enchant caps |
| `frostcore.moderation.exempt` | Cannot be punished |
| `frostcore.moderation.bypass.lockchat` | Chat while locked |

### Glow Color Permissions
| Permission | Color |
|---|---|
| `frostcore.glow.white` | White |
| `frostcore.glow.red` | Red |
| `frostcore.glow.blue` | Blue |
| `frostcore.glow.green` | Green |
| `frostcore.glow.purple` | Purple |
| ... | *16 colors total* |

</details>

---

## 🗃️ Configuration

FrostCore is **deeply configurable** through three files:

| File | Purpose |
|---|---|
| `config.yml` | All system settings, limits, toggles, and database config |
| `messages.yml` | Every player-facing message with MiniMessage + placeholder support |
| `plugin.yml` | Permission defaults |

### Example Config Highlights

```yaml
# Database — swap between SQLite and MySQL with one line
database:
  type: SQLITE  # or MYSQL

# Teams — full control over limits and features
teams:
  player-limit: 10
  max-owners: 2
  max-admins: 4
  echest:
    enabled: true
    slots: 27

# Teleportation — universal settings for all teleports
teleport:
  action-bar: true
  action-bar-style: BAR  # Animated ●●●○○ progress bar
  sounds: true
  particles: true

# Moderation — group weights, webhooks, and more
moderation:
  broadcast-to-staff: true
  use-group-weights: true
  webhooks:
    punishments: "https://discord.com/api/webhooks/..."
    reports: "https://discord.com/api/webhooks/..."
    staffchat: "https://discord.com/api/webhooks/..."

# Mace Limiter — total control over Mace items
mace-limiter:
  enabled: true
  max-maces-overall: 3
  max-maces-per-player: 1
  enchantment-limits:
    DENSITY: 5
    BREACH: 4
    WIND_BURST: 3
```

---

## 🗄️ Database

FrostCore supports **SQLite** (zero-config, file-based) and **MySQL** (for networks and performance).

All data is stored and managed automatically:
- Teams, members, roles, relations, warps, homes
- Team ender chest inventories (Base64 serialized)
- All punishment records (bans, mutes, warns, jails)
- IP tracking and login history
- Player name history
- Mace registry and pending removal queue
- Server warps and spawn location

Switch databases at any time — data migration is handled on restart.

---

## 🏷️ PlaceholderAPI

FrostCore provides team-related placeholders when [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed:

| Placeholder | Returns |
|---|---|
| `%frostcore_team_name%` | Player's team name |
| `%frostcore_team_tag%` | Team's display tag |
| `%frostcore_team_role%` | Player's role in their team |
| `%frostcore_team_members%` | Total member count |

---

## 📈 Performance

FrostCore is built with performance as a core priority:

- **Async Database Operations** — All reads/writes run off the main thread via HikariCP connection pooling
- **Async Chunk Loading** — Teleports preload destination chunks asynchronously
- **ConcurrentHashMap Caching** — Teams, maces, and managers use thread-safe in-memory caches
- **Lazy Echest Loading** — Team ender chests load from DB only when accessed
- **Minimal Event Overhead** — Listeners exit early when features are disabled
- **Efficient Serialization** — Base64 item serialization for compact storage

---

<p align="center">
  <sub>Built with ❄️ by <b>Frost</b></sub><br/>
  <sub>Paper 1.21+ · Java 21+ · MiniMessage · HikariCP</sub>
</p>
