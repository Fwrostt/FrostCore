<div align="center">

# ❄️ FrostCore

**A high-performance, premium SMP Core for modern Minecraft servers.**

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![PaperMC](https://img.shields.io/badge/Paper-1.21-white.svg)](https://papermc.io/)
[![Version](https://img.shields.io/badge/Version-1.1.5-blue.svg)](#)

</div>

---

## 📖 Overview

**FrostCore** is a professional-grade survival multiplayer (SMP) core plugin built natively for **Paper 1.21**. It unifies essential server functionalities—Teams, Homes, Warps, and Teleportation—into a single, high-performance system. 

Designed with a focus on **Premium Aesthetics** and **Thread-Safe Performance**, FrostCore ensures your server stays lag-free while providing a top-tier user experience.

---

## ✨ Key Modules

### 🏠 Personal Homes System
A robust, GUI-driven home management system.
- **Dynamic GUI:** Manage homes with `/homes`. Features smooth blue gradients, intuitive controls (Left-Click to TP, Right-Click to Delete, Middle-Click to Rename).
- **Flexible Limits:** Configure global home limits or use permissions (`frostcore.homes.limit.X`) for VIP ranks.
- **Auto-Naming:** Simple `/sethome` automatically handles naming (Home, Home 2, etc.) if no name is provided.

### 📍 Professional Warp System
Universal server navigation made beautiful.
- **Warp Browser:** `/warps` opens a sleek, paginated GUI.
- **Smart Centering:** Items are mathematically centered if only a few warps exist, ensuring a premium feel.
- **Configurable Items:** Customize material, lore, and glow for every warp in `warps.yml`.

### 🛡️ Feature-Rich Team System
The ultimate clan management module.
- **Role Hierarchy:** Owners, Admins, and Members with distinct powers.
- **Visual Team Info:** `/team info <name>` shows a professional dashboard with player heads, stats, and relations.
- **Team Relations:** Declare Allies and Enemies to define your server's diplomacy.
- **Shared Assets:** Private Team Homes, shared Ender-Chests (`/team echest`), and Team Chat.
- **Confirmation GUIs:** Safeguards for disbanding, leaving, or joining teams.

---

## 🎨 UI & Customization

FrostCore is built to be "Eye-Candy" for your players.

### 📱 GUI Customization
- **Borders Toggle:** Enable or disable the black-glass frame borders globally in `config.yml`.
- **Ocean-Blue Gradients:** All UIs use a soothing, non-italicized blue color scheme designed for readability and elegance.
- **Vertical Separators:** Structural glass panes keep different UI sections (like Team vs Personal Homes) clearly defined.

### ⚡ Teleportation Engine
- **Action Bar Styles:** Switch between a smooth filling `BAR` (●●●○○) or minimalist `TEXT` ("Teleporting in 3s...") style.
- **Movement Safeguards:** Teleports are cancelled if the player moves, preventing combat escapes.
- **Asynchronous Loading:** Chunks are pre-loaded at the destination to ensure zero-lag arrivals.

---

## 📋 Commands & Permissions

### Player Commands
| Command | Description | Permission |
|:---|:---|:---|
| `/home [name]` | Teleport to a personal home | `frostcore.home` |
| `/sethome [name]` | Create a new home | `frostcore.sethome` |
| `/delhome <name>` | Delete a home | `frostcore.delhome` |
| `/homes` | Open the homes GUI | `frostcore.homes` |
| `/warp <name>` | Teleport to a server warp | `frostcore.warp` |
| `/warps` | Open the warp browser | `frostcore.warps` |
| `/team create <name>` | Create a new team | `frostcore.team.create` |
| `/team info [name]` | View team information | `frostcore.team.info` |
| `/team list` | List all server teams | `frostcore.team.list` |
| `/spawn` | Teleport to global spawn | `frostcore.spawn` |

### Admin Commands
| Command | Description | Permission |
|:---|:---|:---|
| `/setwarp <name>` | Create a server warp | `frostcore.admin` |
| `/delwarp <name>` | Delete a server warp | `frostcore.admin` |
| `/setspawn` | Set global spawn point | `frostcore.admin` |
| `/fc reload` | Reload configurations | `frostcore.admin` |

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

