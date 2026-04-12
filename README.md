<div align="center">

# ❄️ FrostCore

**A high-performance, premium SMP Core for modern Minecraft servers.**

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![PaperMC](https://img.shields.io/badge/Paper-1.21-white.svg)](https://papermc.io/)
[![Version](https://img.shields.io/badge/Version-1.1.5-blue.svg)](#)

</div>

---

## 📖 Overview

**FrostCore** is a professional-grade survival multiplayer (SMP) core plugin built natively for **Paper 1.21**. It unifies essential server functionalities—**Enterprise Moderation**, Teams, Homes, Warps, and Teleportation—into a single, high-performance system. 

Designed with a focus on **Premium Aesthetics** and **Thread-Safe Performance**, FrostCore ensures your server stays lag-free while providing a top-tier user experience. It uses asynchronous SQL database tasks extensively, assuring your main thread is never blocked by IO tasks.

---

## ✨ Key Modules

### 🚨 Enterprise Moderation Suite
A moderation system built directly into the core, removing the need for external, bloated, paid plugins. 
- **100% Async Database & Cross-Server Sync:** Punishments, limits, and alt IP-tracking are fully database-persistent (MySQL/SQLite).
- **Punishment IDs & Templates:** Punishments get generated Hex IDs (`#8A5F73`) for aesthetic consistency. Integrated `punishments.yml` templates allow automatic escalation strings (e.g., *1st Warning -> 30d Tempban -> Perm Ban*).
- **Group Hierarchy & Exemptions:** Strict staff role-weight checks ensure Junior Mods cannot punish Admins. 
- **Smart IP Tracking:** The `/ipban` and `/ipmute` commands automatically blacklist all linked alt accounts natively via asynchronous Join listeners mapping `player_ips`.
- **Advanced Paginated GUIs:**
  - `/history <player>`: Interactive paginated GUI to instantly review a player’s full history and natively pardon right from the menu. 
  - `/banlist`, `/mutelist`, `/warnlist`: Dynamic GUIs to view all universally active punishments with pardon integrations.
  - `/reports`: Live-updating GUI menu grouping player reports, enabling 1-click teleporting to the reported player and fast resolution handling. 
- **Discord Webhook Integrity:** Automated log separation for *Moderation actions*, *Player Reports*, and *Admin Actions* with visually beautiful, rich Discord embeds.

### 🏠 Personal Homes System
A robust, GUI-driven home management system.
- **Dynamic GUI:** Manage homes with `/homes`. Features smooth blue gradients, intuitive controls (Left-Click to TP, Right-Click to Delete, Middle-Click to Rename).
- **Flexible Limits:** Configure global home limits or use permissions for VIP ranks.
- **Auto-Naming:** Simple `/sethome` automatically handles naming (Home, Home 2, etc.) if no name is provided.

### 📍 Professional Warp System
Universal server navigation made beautiful.
- **Warp Browser:** `/warps` opens a sleek, paginated GUI.
- **Smart Centering:** Items are mathematically centered if only a few warps exist, ensuring a premium feel.
- **Configurable Items:** Customize material, lore, and glow for every warp.

### 🛡️ Feature-Rich Team System
The ultimate clan management module.
- **Role Hierarchy:** Owners, Admins, and Members with distinct powers.
- **Visual Team Info:** `/team info <name>` shows a professional dashboard with player heads, stats, and relations.
- **Team Relations:** Declare Allies and Enemies to define your server's diplomacy.
- **Shared Assets:** Private Team Homes, shared Ender-Chests (`/team echest`), and Team Chat.

---

## 🎨 UI & Customization

FrostCore is built to be "Eye-Candy" for your players.

### 📱 GUI Customization
- **Ocean-Blue Gradients:** Basic UIs use a soothing, non-italicized blue color scheme designed for readability and elegance.
- **Premium Muted Moderation:** Moderation menus are driven by a stunning reddish gradient (`<gradient:#D4727A:#A35560>`).
- **Vertical Separators:** Structural glass panes keep different UI sections clearly defined.

### ⚡ Teleportation Engine
- **Action Bar Styles:** Switch between a smooth filling `BAR` (●●●○○) or minimalist `TEXT` ("Teleporting in 3s...") style.
- **Movement Safeguards:** Teleports are cancelled if the player moves, preventing combat escapes.
- **Asynchronous Loading:** Chunks are pre-loaded at the destination to ensure zero-lag arrivals.

---

## 📋 Commands & Permissions

### Core Player & Team Commands
| Command | Description | Permission |
|:---|:---|:---|
| `/home [name]` | Teleport to a personal home | `frostcore.home` |
| `/sethome [name]` | Create a new home | `frostcore.sethome` |
| `/homes` | Open the homes GUI | `frostcore.homes` |
| `/warp <name>` | Teleport to a server warp | `frostcore.warp` |
| `/team create <name>` | Create a new team | `frostcore.team.create` |

### Moderation System Commands
| Command | Description | Permission |
|:---|:---|:---|
| `/ban`, `/mute`, `/warn` | Core punishment functions | `frostcore.moderation.<type>` |
| `/tempmute`, `/tempban` | Core duration punishments | `frostcore.moderation.<type>` |
| `/unban`, `/unmute`, `/unwarn` | Remove active punishments | `frostcore.moderation.<type>` |
| `/ipban`, `/ipmute` | Network ban across alt accounts | `frostcore.moderation.ipban` |
| `/history`, `/staffhistory` | View punishment GUI history | `frostcore.moderation.history` |
| `/banlist`, `/mutelist`, `/warnlist` | Paginated lists of active punishments | `frostcore.moderation.check` |
| `/checkban`, `/checkmute`, `/checkwarn` | Look up a specific user | `frostcore.moderation.check` |
| `/alts` (Alias: `/dupeip`) | See all alternate IPs | `frostcore.moderation.alts` |
| `/reports` | Open the live Reports GUI | `frostcore.moderation.reports` |
| `/lockdown` | Activate server whitelist | `frostcore.moderation.lockdown` |
| `/staffrollback` | Roll back bad staff actions | `frostcore.moderation.rollback` |

### Admin Commands
| Command | Description | Permission |
|:---|:---|:---|
| `/setwarp <name>` | Create a server warp | `frostcore.admin` |
| `/setspawn` | Set global spawn point | `frostcore.admin` |
| `/fc reload` | Reload configurations | `frostcore.admin` |
| `/socialspy` | Read player private messages | `frostcore.admin.socialspy` |
| `/top`, `/bottom` | Intelligent teleport utilities | `frostcore.admin.top` |

---

## ⚙️ Configuration

### Database Support
FrostCore supports high-performance database backends via **HikariCP**.
- **SQLite:** Default (no setup required).
- **MySQL:** Recommended for cross-server synchronization.

```yaml
database:
  type: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: frostcore
    username: root
    password: "password"
```

---

## 📦 Developer Info
FrostCore includes an internal **GUI API** used to build all menus. All Java source code is meticulously cleaned of inline comments for a professional look, while preserving essential **Javadoc** for developer reference.

*Built for quality by [Frost]()*
