# OneKeyMiner

<p align="center">
  <img src="img/logo.png" alt="OneKeyMiner Logo" width="720" height="393">
</p>

<p align="center">
  <strong>Chain Mining, Interaction & Planting - All in One!</strong>
</p>

<p align="center">
  <a href="https://github.com/Mai-xiyu/OneKeyMiner/releases"><img src="https://img.shields.io/github/v/release/Mai-xiyu/OneKeyMiner?style=flat-square" alt="Release"></a>
  <a href="https://github.com/Mai-xiyu/OneKeyMiner/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Mai-xiyu/OneKeyMiner?style=flat-square" alt="License"></a>
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-green?style=flat-square" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java Version">
</p>

<p align="center">
  <a href="README_CN.md">🇨🇳 中文文档</a> | 
  <a href="USER_GUIDE.md">📖 User Guide</a> | 
  <a href="API_DOCS_EN.md">🔧 API Documentation</a>
</p>

---

## ✨ Features

- ⛏️ **Chain Mining** - Break connected blocks of the same type at once
- ✂️ **Chain Interaction** - Batch shearing, hoeing, stripping, path making
- 🌱 **Chain Planting** - Auto-plant crops on adjacent farmland
- 🎮 **Multi-Platform** - Supports Fabric and Forge
- ⚙️ **Highly Configurable** - Customize max blocks, distance, activation mode
- 🏷️ **Tag Support** - Use tags like `#minecraft:logs`, `#c:ores`
- 🛡️ **Protection** - Auto-stop when tool durability or hunger is low
- 🔌 **API Available** - Easy integration for other mods

---

## 📥 Installation

### Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.20.1 |
| Java | 17+ |
| Fabric Loader | 0.15.0+ |
| Forge | 47.2.0+ |

### Download

Download the latest release from [GitHub Releases](https://github.com/Mai-xiyu/OneKeyMiner/releases).

Choose the correct version for your platform:
- `onekeyminer-fabric-x.x.x-1.20.1.jar` for Fabric
- `onekeyminer-forge-x.x.x-1.20.1.jar` for Forge

Install the matching loader artifact and the same OneKeyMiner version on both
the dedicated server and every connecting client. Version 1.6.7 uses one
atomic, versioned preference packet plus a server acknowledgement on both
loaders. A missing or incompatible optional channel never sends partial state;
the client keeps retrying and uses local preview policy until an ACK arrives.

---

## 🎮 Quick Start

### Chain Mining
1. Hold a pickaxe or axe
2. **Hold the activation key** (default: `` ` `` backtick)
3. Break an ore or log
4. Watch connected blocks break automatically!

### Chain Interaction  
1. Hold a hoe, axe, shovel, or shears
2. **Hold the activation key**
3. Right-click to interact with blocks
4. Adjacent interactable blocks are also processed!

### Chain Planting
1. Hold seeds or crops
2. **Hold the activation key**
3. Right-click on farmland
4. Adjacent empty farmland is planted automatically!

---

## ⚙️ Configuration

Configuration file location: `config/onekeyminer.json`

On a dedicated server, global limits and feature switches are authoritative in
the server's config file. The in-game client screen synchronizes only the
selected shape and the per-player drop/experience teleport preferences.

### Key Settings

| Option | Default | Description |
|--------|---------|-------------|
| `enabled` | `true` | Enable/disable the mod |
| `maxBlocks` | `64` | Maximum blocks per chain operation |
| `maxDistance` | `16` | Maximum search distance |
| `allowDiagonal` | `true` | Allow diagonal block connections |
| `consumeDurability` | `true` | Consume tool durability |
| `preserveDurability` | `1` | Stop when durability reaches this value |
| `consumeHunger` | `true` | Consume hunger for each block |
| `minHungerLevel` | `1` | Stop when hunger reaches this value |
| `allowBareHand` | `true` | Allow chain mining without tools |
| `teleportDrops` | `false` | Teleport drops to player inventory |
| `teleportExp` | `false` | Teleport experience to player |

### Block/Tool Lists

```json
{
  "customWhitelist": ["mymod:custom_ore"],
  "blacklist": ["minecraft:bedrock"],
  "toolWhitelist": [],
  "toolBlacklist": ["minecraft:wooden_pickaxe"]
}
```

---

## 🔧 For Developers

OneKeyMiner provides a comprehensive API for mod developers.

### Adding Dependency

```groovy
// Fabric
modImplementation "org.xiyu:onekeyminer-fabric:1.6.7"

// Forge
implementation "org.xiyu:onekeyminer-forge:1.6.7"
```

### Basic API Usage

```java
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;

// Register custom blocks
OneKeyMinerAPI.registerBlock("mymod:custom_ore");
OneKeyMinerAPI.registerBlockTag("#mymod:ores");

// Register custom tools
OneKeyMinerAPI.whitelistTool("mymod:super_pickaxe");

// Listen to events
ChainEvents.registerPreActionListener(event -> {
    // Custom logic before chain operation
});
```

See [API Documentation](API_DOCS_EN.md) for complete API reference.

---

## 🌟 Addon Development

Want to build addon mods or integrations? You can use our API to register blocks/tools and listen to chain events.

---

## 📋 Compatibility

### Supported Mod Loaders
- ✅ Fabric (with Fabric API)
- ✅ Forge

### Tested Mods
- Mod Menu (Fabric)
- Most ore/tool mods

### Protection Plugin Support
Uses `ServerPlayerGameMode#destroyBlock()` for proper integration with:
- FTB Chunks
- Claim plugins
- Other protection mods

---

## 🌿 Branching & Releases

- **Branching**: Each Minecraft version uses its own branch (e.g., `1.20.1`).
- **Latest**: The latest Minecraft version is maintained on `master`.
- **Tag format**: `<branch>-<mod_version>` (example: `1.20.1-1.6.7`).


## 🐛 Issues & Contributions

Found a bug or have a suggestion?

- [Open an Issue](https://github.com/Mai-xiyu/OneKeyMiner/issues)
- [Submit a Pull Request](https://github.com/Mai-xiyu/OneKeyMiner/pulls)

---

## 📜 License

This project uses the **Code Repository General License Agreement v1.2**
(`LicenseRef-Code-Repository-General-License-v1.2`). See
[LICENSE_EN.md](LICENSE_EN.md) or [LICENSE_CN.md](LICENSE_CN.md) for the exact terms.

---

## 💖 Credits

- **Author**: [Mai_xiyu](https://github.com/Mai-xiyu)
- **Project Origin**: The original OneKeyMiner had separate projects and branches for each mod loader and Minecraft version. This unified version was created to consolidate all platforms into a single codebase with completely refactored code.
- **Special Thanks**: All contributors and testers

---

<p align="center">
  Made with ❤️ for the Minecraft community
</p>
