<div align="center">

<h1>⚔️ GrimnatorAC</h1>

<p><strong>A powerful, customized fork of GrimAC — the most accurate open-source Minecraft anticheat.</strong></p>
<p><strong>Fully async, multithreaded, predictive, open source, 3.01 reach, 1.005 timer, 0.01% speed, 99.99% antikb, 0.010 hitbox, "bypassable" 1.8-1.21 anticheat.</strong></p>


[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square&logo=github)](https://github.com/baybimposeti-crypto/grimnatorAC)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21+-blue?style=flat-square&logo=minecraft)](https://github.com/baybimposeti-crypto/grimnatorAC)
[![License](https://img.shields.io/badge/license-GPL--3.0-orange?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/java-17+-red?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![Releases](https://img.shields.io/github/v/release/baybimposeti-crypto/grimnatorAC?style=flat-square&color=purple)](https://github.com/baybimposeti-crypto/grimnatorAC/releases)

</div>

---

## 📖 What is GrimnatorAC?

GrimnatorAC is a **customized fork of [GrimAC](https://github.com/GrimAnticheat/Grim)** — the industry-leading open-source Minecraft anticheat. This fork extends the original with **mod client detection**, an **automatic update system**, a **persistent strike system**, and **Turkish language support**, while maintaining the accuracy and performance GrimAC is known for.

> Designed for **Spigot / Paper / Folia** servers running **Minecraft 1.21+**

---

## ✨ Features

### 🔍 Translation Key Mod Detection *(Custom)*
Detects hacked clients by probing Minecraft's translation key resolution system — a technique that defeats most anti-detection countermeasures.

**Detected clients include:**
| Client | Client |
|---|---|
| Meteor Client | Wurst Client |
| Freecam | Item Scroller |
| Xaero's World Map | Mace Attack Assistance |
| Pathmind | Elytra Assistant |

- **OpSec bypass logic** — defeats anti-detection mods via multi-batch sign probing
- **Automatic check on player join** (configurable delay)
- **Creative/Spectator mode exemption** — no false positives
- **Bedrock/Geyser player exemption**

---

### ⚖️ Strike System *(Custom)*
Persistent punishment tracking that survives server restarts.

| Strike | Action |
|--------|--------|
| 1st | Warning kick with explanation |
| 2nd+ | Permanent ban |

Strikes are stored in `plugins/GrimnatorAC/mod_strikes.txt` and persist across restarts.

---

### 🔄 Automatic Update System *(Custom)*
- Checks GitHub Releases for new versions on startup
- Notifies admins both in-console and in-game
- Supports semantic versioning (`MAJOR.MINOR.PATCH`)
- Safe process — new `.jar` is downloaded but requires manual swap

---

### 🧠 Original GrimAC Engine
All of GrimAC's battle-tested features are included:

- **Movement Simulation** — 1:1 replication of Minecraft's physics engine
- **Fully Asynchronous** — multi-threaded, minimal TPS impact
- **Latency Compensation** — no false positives from lag spikes
- **Per-Player World Replication** — accurate block state tracking
- **Ghost Block Prevention** — via inventory/interaction tracking
- **Mathematically bypass-resistant** checks

---

## 📥 Installation

### Requirements
- Java **17** or higher
- **Spigot / Paper / Folia** 1.21+

### Steps

```bash
# 1. Download the latest release
# https://github.com/baybimposeti-crypto/grimnatorAC/releases

# 2. Drop the .jar into your plugins folder
cp GrimnatorAC-*.jar /your-server/plugins/

# 3. Restart your server
# Config will be generated at: plugins/GrimnatorAC/config.yml
```

---

## ⚙️ Configuration

```yaml
ModDetection:
  enabled: true
  check-delay: 5        # Seconds after join before checking

UpdateChecker:
  enabled: true
  notify-admins: true   # In-game notifications for ops
```

### Manual Mod Check

```
/modcheck   — Run mod detection on yourself
```
> Requires `grimnatorac.modcheck` permission.

---

## 🔐 Permissions

| Permission | Description | Default |
|---|---|---|
| `grimnatorac.bypass` | Bypass all anticheat checks | OP |
| `grimnatorac.alerts` | Receive anticheat violation alerts | OP |
| `grimnatorac.admin` | Admin access + update notifications | OP |
| `grimnatorac.modcheck` | Use `/modcheck` command | OP |

---

## 🛠️ Building from Source

```bash
# Clone
git clone https://github.com/baybimposeti-crypto/grimnatorAC
cd grimnatorAC

# Build (Windows)
gradlew.bat obfuscate

# Build (Linux / macOS)
./gradlew obfuscate
```

Output: `bukkit/build/libs/GrimnatorAC-*.jar`

---

## 🎮 Compatibility

| Minecraft Version | Status |
|---|---|
| 1.8 – 1.20.6 | ❌ Not Supported |
| 1.21+ | ✅ Fully Supported |

> Bedrock players connecting via **Geyser** are automatically exempted.

---

## 🤝 Contributing

1. Fork this repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push and open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines.

---

## 📜 License

Licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for details.

---

## 🙏 Credits

- **[GrimAC](https://github.com/GrimAnticheat/Grim)** — the original anticheat this fork is based on
- **Translation Key Detection** — methodology inspired by CheatDetector research
- **GrimnatorAC custom features** — developed and maintained by [@baybimposeti-crypto](https://github.com/baybimposeti-crypto)

---

> ⚠️ **Disclaimer:** This is an independent fork and is not officially affiliated with or supported by the GrimAC team. For official GrimAC support, visit the [original repository](https://github.com/GrimAnticheat/Grim).

<div align="center">
  <sub>Built on top of GrimAC — the open-source Minecraft anticheat</sub>
</div>




-ukkacukka..
