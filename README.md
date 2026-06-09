# GrimnatorAC - Custom Fork

A customized fork of GrimnatorAC with enhanced anti-cheat capabilities and custom features.

[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat&logo=github)](https://github.com/GrimnatorAC/GrimnatorAC)
[![Minecraft](https://img.shields.io/badge/minecraft-1.8--1.21+-blue?style=flat&logo=minecraft)](https://github.com/GrimnatorAC/GrimnatorAC)
[![License](https://img.shields.io/badge/license-GPL--3.0-orange?style=flat)](LICENSE)

## 🚀 Custom Features

This fork includes several custom enhancements on top of the original GrimnatorAC:

### 🔍 Translation Key Mod Detection
- **Multi-batch sign-based detection system**
- Detects 8+ different hacked clients including:
  - Meteor Client
  - Wurst Client
  - Freecam
  - Item Scroller
  - Xaero's World Map
  - Mace Attack Assistance
  - Pathmind
  - Elytra Assistant
- **OpSec bypass logic** - defeats anti-detection mods
- **Strike system** with persistent storage
  - Strike 1: Warning kick
  - Strike 2: Permanent ban
- **Turkish language support** for kick/ban messages
- Automatic checking on player join
- Exempts creative/spectator mode players

### 📦 Automatic Update System
- **GitHub integration** for automatic update checking
- Downloads new versions automatically
- Notifies admins in console and in-game
- Safe update process (manual installation required)
- Supports semantic versioning (MAJOR.MINOR.PATCH)

### 🎯 Simplified Build
- **Bukkit-only build** - no common/fabric modules
- Faster compilation times
- Easier to maintain and customize
- Direct compilation with `gradlew.bat obfuscate`

## 📥 Installation

### Requirements
- **Java 17 or higher**
- **Spigot/Paper/Folia server** (1.8-1.21+)
- Server must support Bukkit plugins

### Quick Start

1. Download the latest release from [Releases](../../releases)
2. Place the `.jar` file in your server's `plugins/` folder
3. Restart your server
4. Configure in `plugins/GrimnatorAC/config.yml`

## 🔧 Configuration

### Mod Detection

The mod detection system runs automatically on player join. To configure:

```yaml
# Enable/Disable mod detection
ModDetection:
  enabled: true
  # Delay before checking (seconds)
  check-delay: 5
```

**Manual check command:**
```
/modcheck - Run mod detection on yourself (requires grimnatorac.modcheck permission)
```

### Strike System

Strikes persist across server restarts in `plugins/GrimnatorAC/mod_strikes.txt`

- **Strike 1**: Player is kicked with a warning
- **Strike 2+**: Player is permanently banned

### Update Checker

The update checker runs automatically on server startup. To configure it for your fork:

1. Edit `bukkit/src/main/java/com/grimnatorac/platform/bukkit/utils/UpdateChecker.java`
2. Set your GitHub details:
   ```java
   private static final String GITHUB_USER = "your-username";
   private static final String GITHUB_REPO = "your-repo-name";
   private static final String JAR_NAME = "GrimnatorAC.jar";
   ```
3. Rebuild and create GitHub releases

**See [UPDATE_CHECKER_SETUP.md](UPDATE_CHECKER_SETUP.md) for detailed setup instructions.**

## 🛠️ Building From Source

### Prerequisites
- Java 17 JDK or higher
- Git

### Compilation

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
cd YOUR_REPO_NAME

# Build with ProGuard obfuscation
gradlew.bat obfuscate

# Or on Linux/Mac
./gradlew obfuscate
```

The compiled `.jar` will be in `bukkit/build/libs/`

## 📚 Documentation

- **Original GrimnatorAC Wiki**: [GitHub Wiki](https://github.com/GrimnatorAC/GrimnatorAC/wiki)
- **Update Checker Setup**: [UPDATE_CHECKER_SETUP.md](UPDATE_CHECKER_SETUP.md)
- **FAQ**: [Original FAQ](https://github.com/GrimnatorAC/GrimnatorAC/wiki/FAQ)

## 🎮 Supported Minecraft Versions

| Version Range | Support Status |
|---------------|----------------|
| 1.8 - 1.12.2  | ✅ Full Support |
| 1.13 - 1.16.5 | ✅ Full Support |
| 1.17 - 1.20.6 | ✅ Full Support |
| 1.21+         | ✅ Full Support |

**Note**: Geyser/Bedrock players are automatically exempted to prevent false positives.

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `grimnatorac.bypass` | Bypass all anticheat checks | OP |
| `grimnatorac.alerts` | Receive anticheat alerts | OP |
| `grimnatorac.admin` | Admin permissions + update notifications | OP |
| `grimnatorac.modcheck` | Use /modcheck command | OP |

## 🎯 Key Features (Original + Custom)

### ✨ Original GrimnatorAC Features
- **Movement Simulation Engine** - 1:1 replication of player movements
- **Fully Asynchronous** - Multi-threaded design for optimal performance
- **World Replication** - Per-player world cache for accurate checks
- **Latency Compensation** - No false positives from lag
- **Inventory Tracking** - Ghost block prevention
- **Secure by Design** - Mathematically impossible to bypass

### 🎨 Custom Fork Features
- **Translation Key Probing** - Detects mod clients via language file resolution
- **OpSec Bypass** - Defeats anti-detection mods with retry logic
- **Strike System** - Persistent punishment tracking
- **Auto-Updater** - GitHub integration for automatic updates
- **Streamlined Build** - Bukkit-only, faster compilation
- **Turkish Support** - Localized kick/ban messages

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

## 📜 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Original GrimnatorAC**: [GrimnatorAC/GrimnatorAC](https://github.com/GrimnatorAC/GrimnatorAC)
- **Translation Key Detection**: Inspired by CheatDetector methodology
- **Custom Features**: Developed by the fork maintainer

## 📞 Support

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)
- **Original Discord**: [discord.grim.ac](https://discord.grim.ac) *(for upstream issues only)*

## ⚠️ Disclaimer

This is a custom fork and is not officially supported by the GrimnatorAC team. For official support, please refer to the [original GrimnatorAC repository](https://github.com/GrimnatorAC/GrimnatorAC).

---

<div align="center">
  <b>Made with ❤️ by the community</b>
  <br>
  <sub>Based on GrimnatorAC - The open-source Minecraft anticheat</sub>
</div>
