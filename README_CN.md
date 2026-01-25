# OneKeyMiner / 一键连锁

<p align="center">
  <img src="img/logo.png" alt="OneKeyMiner Logo" width="720" height="393">
</p>

<p align="center">
  <strong>连锁挖矿、交互、种植 - 一个模组全搞定！</strong>
</p>

<p align="center">
  <a href="https://github.com/Mai-xiyu/OneKeyMiner/releases"><img src="https://img.shields.io/github/v/release/Mai-xiyu/OneKeyMiner?style=flat-square" alt="Release"></a>
  <a href="https://github.com/Mai-xiyu/OneKeyMiner/blob/main/LICENSE_CN.md"><img src="https://img.shields.io/github/license/Mai-xiyu/OneKeyMiner?style=flat-square" alt="License"></a>
  <img src="https://img.shields.io/badge/Minecraft-1.21.9-green?style=flat-square" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=flat-square" alt="Java Version">
</p>

<p align="center">
  <a href="README.md">🇺🇸 English</a> | 
  <a href="USER_GUIDE.md">📖 使用指南</a> | 
  <a href="API_DOCS_CN.md">🔧 API 文档</a>
</p>

---

## ✨ 功能特性

- ⛏️ **连锁挖矿** - 一次性破坏相连的同类型方块
- ✂️ **连锁交互** - 批量剪羊毛、锄地、去皮、铲路径
- 🌱 **连锁种植** - 在相邻农田自动播种
- 🎮 **多平台支持** - 支持 Fabric、NeoForge、Forge
- ⚙️ **高度可配置** - 自定义最大方块数、距离、激活模式
- 🏷️ **标签支持** - 使用标签如 `#minecraft:logs`、`#c:ores`
- 🛡️ **保护机制** - 工具耐久或饥饿过低时自动停止
- 🔌 **提供 API** - 方便其他模组集成

---

## 📥 安装方法

### 环境要求

| 组件 | 版本要求 |
|------|----------|
| Minecraft | 1.21.9 |
| Java | 21+ |
| Fabric Loader | 0.15.0+ |
| NeoForge | 21.0+ |
| Forge | 59.0+ |

### 下载

从 [GitHub Releases](https://github.com/Mai-xiyu/OneKeyMiner/releases) 下载最新版本。

根据你的平台选择正确的版本：
- `onekeyminer-fabric-x.x.x-1.21.9.jar` - Fabric 版
- `onekeyminer-neoforge-x.x.x-1.21.9.jar` - NeoForge 版
- `onekeyminer-forge-x.x.x-1.21.9.jar` - Forge 版
- `onekeyminer-x.x.x-1.21.9.jar` - 通用版（自动检测）

---

## 🎮 快速开始

### 连锁挖矿
1. 手持镐子或斧头
2. **按住激活键**（默认：`` ` `` 反引号键）
3. 破坏一个矿石或原木
4. 看着相连的方块自动破坏！

### 连锁交互
1. 手持锄头、斧头、铲子或剪刀
2. **按住激活键**
3. 右键点击方块进行交互
4. 相邻的可交互方块也会被处理！

### 连锁种植
1. 手持种子或作物
2. **按住激活键**
3. 右键点击农田
4. 相邻的空农田自动播种！

---

## ⚙️ 配置说明

配置文件位置：`config/onekeyminer.json`

### 主要设置

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | `true` | 启用/禁用模组 |
| `maxBlocks` | `64` | 每次连锁操作的最大方块数 |
| `maxDistance` | `16` | 最大搜索距离 |
| `allowDiagonal` | `true` | 允许对角线方向连接 |
| `consumeDurability` | `true` | 消耗工具耐久 |
| `preserveDurability` | `1` | 当耐久达到此值时停止 |
| `consumeHunger` | `true` | 每破坏一个方块消耗饥饿值 |
| `minHungerLevel` | `1` | 当饥饿值达到此值时停止 |
| `allowBareHand` | `true` | 允许空手连锁挖矿 |
| `teleportDrops` | `false` | 将掉落物传送到玩家背包 |
| `teleportExp` | `false` | 将经验传送到玩家 |

### 方块/工具列表

```json
{
  "customWhitelist": ["mymod:custom_ore"],
  "blacklist": ["minecraft:bedrock"],
  "toolWhitelist": [],
  "toolBlacklist": ["minecraft:wooden_pickaxe"]
}
```

---

## 🔧 开发者指南

OneKeyMiner 为模组开发者提供了完整的 API。

### 添加依赖

```groovy
// Fabric
modImplementation "org.xiyu:onekeyminer-fabric:2.0.0"

// NeoForge/Forge
implementation "org.xiyu:onekeyminer-neoforge:2.0.0"
```

### 基本 API 用法

```java
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;

// 注册自定义方块
OneKeyMinerAPI.registerBlock("mymod:custom_ore");
OneKeyMinerAPI.registerBlockTag("#mymod:ores");

// 注册自定义工具
OneKeyMinerAPI.registerTool("mymod:super_pickaxe");

// 监听事件
ChainEvents.registerPreActionListener(event -> {
    // 连锁操作前的自定义逻辑
});
```

完整 API 参考请查看 [API 文档](API_DOCS_CN.md)。

---

## 🌟 附属模组开发

想要添加预览高亮或其他功能？你可以使用我们的 API 创建附属模组！

### 示例：方块预览渲染器

参考 [LiteMiner 的 BlockHighlightRenderer](https://github.com/iamkaf/liteminer/blob/1.21.9/common/src/main/java/com/iamkaf/liteminer/rendering/BlockHighlightRenderer.java) 了解如何实现方块预览高亮功能。

---

## 📋 兼容性

### 支持的模组加载器
- ✅ Fabric（需要 Fabric API）
- ✅ NeoForge
- ✅ Forge

### 已测试模组
- Mod Menu（Fabric）
- 大多数矿石/工具模组

### 保护插件支持
使用 `ServerPlayerGameMode#destroyBlock()` 以正确兼容：
- FTB Chunks
- 领地插件
- 其他保护类模组

---

## 🐛 问题反馈

发现 Bug 或有建议？

- [提交 Issue](https://github.com/Mai-xiyu/OneKeyMiner/issues)
- [提交 Pull Request](https://github.com/Mai-xiyu/OneKeyMiner/pulls)

---

## 📜 版权声明

本项目采用 [All Rights Reserved (ARR)](LICENSE) 协议。未经作者许可，不得复制、修改或分发本项目代码。

---

## 💖 鸣谢

- **作者**：[Mai_xiyu](https://github.com/Mai-xiyu)
- **项目起源**：原版 OneKeyMiner 针对每个模组加载器和 Minecraft 版本都有独立的项目和分支，为了方便管理和维护，决定将所有平台统一到一个代码库中，并进行了完全的代码重构。
- **特别感谢**：所有贡献者和测试者

---

<p align="center">
  用 ❤️ 为 Minecraft 社区制作
</p>
