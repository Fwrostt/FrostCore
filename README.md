<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-brightgreen?style=for-the-badge&logo=mojang-studios&logoColor=white" alt="Minecraft 1.21+"/>
  <img src="https://img.shields.io/badge/Paper-API-blue?style=for-the-badge" alt="Paper"/>
  <img src="https://img.shields.io/badge/Java-21+-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21+"/>
  <img src="https://img.shields.io/badge/version-1.3.0-blueviolet?style=for-the-badge" alt="Version"/>
</p>

<h1 align="center">❄️ FrostCore</h1>

<p align="center">
  <b>The only core plugin your server will ever need.</b><br/>
  <sub>Teams · RTP · Bounties · Moderation · Teleportation · Chat Pipeline · Cosmetics — all in one.</sub>
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

FrostCore replaces **Essentials, LiteBans, BetterTeams, HuskHomes, DeluxeMenus, BetterRTP, and Bountiful** — with one lightweight, fully integrated, premium-quality plugin. Every system is built from scratch with performance, aesthetics, and configurability in mind.

### 🏴‍☠️ Team System
> A full-featured faction-style team system with roles, relations, and shared storage.

- **Create & Manage Teams** — `/team create`, `/team disband`, `/team rename`, `/team settag`
- **Role Hierarchy** — Owner → Admin → Member with configurable limits per role
- **Team Chat** — Private, toggleable chat channel for team members only
- **Allies & Enemies** — Invite-based alliance system with configurable max relations
- **Team PvP Toggle** — Friendly fire protection between teammates
- **Team Home & Warps** — Shared teleport points with per-team warp limits
- **Team Ender Chest** — DB-persisted shared storage inventory (configurable slots: 9–54)
- **Admin Override** — Staff can force-disband, force-join, and manage any team

### 🎯 Bounty Hunter System
> A robust, economy-powered bounty system with intelligent anti-abuse.

- **Place & Stack Bounties** — Players can place bounties on targets (`/bounty place`). Multiple players can fund the same target and it stacks seamlessly.
- **Dynamic Leaderboard** — Command (`/bounty top`) and a fully interactive GUI.
- **Premium GUI Engine** — Stunning in-game menus to browse, search (via Sign Input), and sort active bounties. 
- **Anti-Abuse Engine** — Sophisticated "anti-self-claim" mechanic prevents players from claiming bounties they placed or funded.
- **Economy Integration** — Withdraws and deposits automatically through Vault.
- **Admin Management** — Fast commands to inspect or wipe bounties cleanly.

### 🌌 High-Performance RTP (Random Teleport)
> The most robust and chunk-safe RTP engine available.

- **Zero Main-Thread Scanning** — Designed utilizing Paper's `ChunkSnapshot` API to completely eliminate lag spikes.
- **Pre-Generated Location Pools** — Computes and verifies locations asynchronously ahead of time for *instant* teleportation.
- **Uniform Circular Distribution** — A beautiful math-driven algorithm ensures players are evenly distributed, not clustered.
- **Premium Interfaces** — A beautifully crafted dynamic GUI (`rtp.yml`) utilizing MiniMessage gradients and icons.
- **Full Control** — Per-world toggles, dimensions checks, bounds, block blacklists, delay timers, and cooldown tracking.

### 🏠 Homes & Warps
> Personal homes and server-wide warps with animated GUIs.

- **Personal Homes** — Set, teleport, rename, and delete personal homes
- **Max Homes by Permission** — Default limit configurable, override with `frostcore.homes.limit.<n>`
- **Homes GUI** — Beautiful interactive menu with team home integration
- **Server Warps** — Admin-defined teleport points with custom icons
- **Warps GUI** — Paginated, configurable chest interface for browsing warps
- **Spawn Management** — `/setspawn`, `/spawn` with join and respawn teleport logic

### 🛡️ Enterprise Moderation Suite
> Command-rich moderation with off-line support, alt-tracking, and Discord alerts.

- **Punishments** — Bans, mutes, warns, kicks, and jails. Support for temp/permanent, IPs, and stealth (`-s`).
- **History & Auditing** — `/history`, `/staffhistory`, `/alts`, `/namehistory`, `/iphistory`.
- **Chat Pipeline** — Weighted violation decay model, aggressive string normalization to neutralize obfuscation bypasses, and **shadow mutes**.
- **Utility Tools** — `/freeze` (with auto-quit punish), `/screenshare`, `/lockdown`, `/staffchat`.
- **Group Weights** — Built-in hierarchy so lower staff cannot punish higher staff.
- **Discord Integrations** — Pre-built webhook routing for bans, reports, and staff-chat.

### 🔀 Teleportation Engine
> One unified teleport system powering every teleport in the plugin.

- **TPA / TPAHere** — Request-based player teleportation with expiry timers and toggles (`/tpatoggle`).
- **Admin TP** — `/tp`, `/tphere`, `/tp2p`, `/tpall`, `/otp` (offline TP).
- **Smart Warmups** — Cancel-on-move logic checking block vectors (head-rotation safe).
- **Return System** — highly intelligent `/back` command preserving last states.

### 💬 Advanced Chat & Messaging
> Full control over your communication layers.

- **/chattoggle** — Allows players to silence the global chat entirely to focus on their adventure.
- **/msgtoggle** — Disable incoming private messages (admins bypass).
- **/msg, /reply, /ignore** — Full PM networking suite, cross-compatible with console.
- **MiniMessage Formatting** — Chat prefixes, gradients, hover events, and beautiful colors.

### ✨ Glow Cosmetics
> Complex visual cosmetics integrated perfectly.

- **16 Available Colors** — Pick from all vanilla team colors via a fast, permission-gated GUI.
- **Per-Viewer Scoreboard Architecture** — Custom written packet-level scoreboard systems designed specifically so Glow colors **do not conflict** with LuckPerms rank prefixes. Enjoy your prefix and your glow simultaneously!
- **Admin Management** — Force set glows for events or VIPs.

### ⚔️ Mace Limiter
> The ultimate modern balance tool to reign in 1.21's strongest weapon.

- **Global & Per-Player Limits** — Physical caps on how many maces can exist.
- **Life-cycle Tracking** — Tracks through PDC to disable maces crafted past the limit without deleting them instantly.
- **Deep Balances** — Caps Density/Breach to safe levels, implements hit-cooldowns.
- **Live Registry** — Sleek GUI to track every single Mace on the server in real-time.

---

## ⚙️ Installation

1. Download the latest `FrostCore.jar`
2. Place it in your server's `plugins/` folder
3. Start (or restart) your server
4. Edit `plugins/FrostCore/config.yml` and other config files to your liking
5. Use `/frostcore reload` or `/fc reload` to apply changes instantly without restarts.

**Requirements:**
- Paper 1.21+ (or forks like Purpur, Folia compatibility pending)
- Java 21+

**Recommended:**
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — For team and bounty placeholders in chat/tab.
- [LuckPerms](https://luckperms.net/) — For permissions and chat prefixes.
- [Vault](https://dev.bukkit.org/projects/vault) — Required for the Economy / Bounty / RTP features.

---

## 📋 Commands

FrostCore registers **100+ commands** across these categories:

| Category | Examples |
|---|---|
| **Bounties** | `/bounty place <player> <amount>`, `/bounty list`, `/bounty top` |
| **RTP** | `/rtp`, `/rtp [world]`, `/rtp reload` |
| **Teams** | `/team create`, `/team invite`, `/team echest`, `/team warps` |
| **Teleportation** | `/tpa`, `/home`, `/warp`, `/spawn`, `/back` |
| **Moderation** | `/ban`, `/mute`, `/jail`, `/freeze`, `/history`, `/alts` |
| **Administration** | `/vanish`, `/invsee`, `/sudo`, `/ram`, `/whois` |
| **Utilities** | `/fly`, `/heal`, `/god`, `/glow`, `/ping` |
| **Chat/Messaging**| `/msg`, `/r`, `/ignore`, `/chattoggle`, `/msgtoggle` |
| **Item Editing** | `/itemrename`, `/lore`, `/repair` |
| **Maces** | `/mace info`, `/mace settings`, `/mace destroy` |

---

## 🔑 Permissions

All permissions follow the `frostcore.<category>.<action>` pattern.

<details>
<summary><b>Click to expand full permissions list</b></summary>

### Player Permissions (default: true)
| Permission | Description |
|---|---|
| `frostcore.team.use` | Access to /team commands |
| `frostcore.bounty.list` | Access to bounty GUI and list |
| `frostcore.bounty.place` | Place bounties |
| `frostcore.rtp` | Perform random teleports |
| `frostcore.tpa` | TPA commands |
| `frostcore.warp` | Use server warps |
| `frostcore.spawn` | Teleport to spawn |
| `frostcore.sethome` | Set personal homes |
| `frostcore.home` | Teleport to homes |
| `frostcore.homes` | Open homes GUI |
| `frostcore.message` | Private messaging |
| `frostcore.msgtoggle` | Toggle incoming PMs |
| `frostcore.chattoggle`| Toggle global chat |
| `frostcore.glow.use` | Access glow GUI |

### Admin & Staff Permissions (default: op)
| Permission | Description |
|---|---|
| `frostcore.admin` | Access admin utilities |
| `frostcore.moderation.*` | Full moderation access |
| `frostcore.rtp.admin` | Force RTP and Reload |
| `frostcore.bounty.admin` | Wipe/remove any bounty |
| `frostcore.admin.vanish` | Ghost completely |
| `frostcore.glow.admin` | Set glows on other players |
| `frostcore.glow.*` | Access all 16 glow colors |

### Bypass Permissions
| Permission | Description |
|---|---|
| `frostcore.bypass.cooldown` | Bypass teleport cooldowns |
| `frostcore.bypass.delay` | Bypass warmup timers |
| `frostcore.rtp.bypass.cost` | Free random teleports |
| `frostcore.rtp.bypass.cooldown` | Rapid RTPs |
| `frostcore.moderation.exempt` | Cannot be punished |

</details>

---

## 🏷️ PlaceholderAPI

FrostCore provides a dense suite of placeholders out-of-the-box:

**Team System:**
- `%frostcore_team_name%` — Player's team name
- `%frostcore_team_tag%` — Team's display tag
- `%frostcore_team_role%` — Player's role inside the team
- `%frostcore_team_members%` — Total team member count

**Bounty System:**
- `%frostcore_bounty_has%` — `true`/`false`
- `%frostcore_bounty_amount%` — Value of bounty on this player
- `%frostcore_bounty_contributors%` — Amount of people who funded this bounty
- `%frostcore_bounty_top_name%` — Overall server #1 target
- `%frostcore_bounty_top_amount%` — Overall server #1 bounty reward value

---

## 📈 Performance & Architecture

FrostCore was engineered to handle high concurrent player counts without dropping ticks.

- **Zero-Block Async Pools** — Teleports preload and verify destination chunks asynchronously.
- **Robust Database Transactions** — HikariCP connection pooling pushes network wait times completely off thread.
- **Polished Thread Models** — Bounties, logs, timers, and heavy computations never touch the primary `tick` loop.
- **Smart GUI Caching** — Reusable, stateful GUI engines recycle inventories rather than needlessly destroying them. 

---

<p align="center">
  <sub>Built with ❄️ by <b>Frost</b></sub><br/>
  <sub>Paper 1.21+ · Java 21+ · MiniMessage · HikariCP</sub>
</p>
