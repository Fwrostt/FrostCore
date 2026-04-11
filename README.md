<div align="center">

# ❄️ FrostCore

**A robust, feature-rich SMP Core Plugin for modern Minecraft servers.**

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![PaperMC](https://img.shields.io/badge/Paper-1.21-white.svg)](https://papermc.io/)
[![Version](https://img.shields.io/badge/Version-1.1-blue.svg)](#)

</div>

---

## 📖 Overview

**FrostCore** is an all-in-one survival multiplayer (SMP) core plugin built natively for Paper 1.21. Designed to replace multiple unoptimized plugins, FrostCore offers powerful performance while bringing essential SMP features together under a single, unified system. 

From fundamental utilities like teleportation and homes to an extensive and feature-packed Team management system, FrostCore is built to handle it all without sacrificing server performance.

## ✨ Key Features

### 🏕️ Essential Server Utilities
- **Universal Teleportation:** Robust handling of player teleports including `/tpa`, `/tpahere`, `/tpaccept`, and `/tpdecline`. Features highly interactive, clickable chat interfaces for teleport requests.
- **Homes, Warps & Spawn:** Set global `/spawn` and server warps (`/warp`, `/setwarp`, `/delwarp`). Supports beautifully formatted paginated warp grids.
- **Admin Teleports:** Fully equipped with overrides including `/tp`, `/tp2p`, `/tphere`, and `/otp` (for teleporting to offline players' last known coordinates).
- **Aesthetics & UI:** Teleportations process through configurable warmup delays and cooldowns, paired with perfectly smooth action-bar progress indicators, sound chimes, titles, and localized messages. Configurable `admin-bypass` allows for accurate testing.

### 🛡️ Comprehensive Team System
FrostCore includes a fully-fledged chunk of features solely dedicated to clans, parties, or team systems!
- **Role Hierarchy:** Structured roles with specific permissions including Owners (highest) and Admins.
- **Team Homes & Warps:** Teams can have a private team home and create their own custom warp points for internal use with manageable cooldowns and delays.
- **Shared Storage:** Shared Team Ender-Chests (`/team echest`) whose slot capacity can be configured.
- **Team Relations:** Establish deep alliances or bitter rivalries by setting team allies or enemies.
- **Team Chat & Toggles:** Dedicated private chat channels for teams (`/team chat`), and PvP toggling with friendly-fire safeguards.
- **Invite & Pagination System:** Time-limited team invites, ally requests, and paginated `/team list` formatting.

### ⚙️ Under The Hood
- **Performance First:** Written asynchronously where possible to keep the main thread ticking at 20 TPS.
- **Database Support:** Built-to-last generic SQL abstraction utilizing `HikariCP` connection pooling. Supports both **SQLite** (local, no setup required) and **MySQL** (cross-server syncing).
- **PlaceholderAPI Integration:** Expose your server to millions of other plugins via PAPI. Hook into FrostCore's stats and team data.

## 📦 Installation
1. Download the latest `FrostCore.jar` from the releases section.
2. Optional: Ensure [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) is installed if you want to use FrostCore's placeholders.
3. Drop the jar file into your server's `plugins/` folder.
4. Start the server (Requires **Java 21**, PaperMC **1.21**).
5. Open `plugins/FrostCore/config.yml` to set up your database provider (defaults to SQLite).
6. Configure `messages.yml` to your desired server theme and colors.
7. Restart the server!

## 📜 Configuration
Almost everything is customizable, including permissions, role limits, name filters, teleport delays, and every single plugin message.

### Database
If utilizing a MySQL database, update your credentials in `config.yml`:

```yaml
database:
  type: MYSQL # Change from SQLITE                    
  mysql:
    host: localhost
    port: 3306
    database: frostcore
    username: root
    password: "password"
    pool-size: 10
```
---
*Created by [Frost]()*
