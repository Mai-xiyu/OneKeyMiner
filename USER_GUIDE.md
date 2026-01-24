# OneKeyMiner 用户指南

<p align="center">
  <img src="img/logo.png" alt="OneKeyMiner Logo" width="720" height="393">
</p>

<p align="center">
  <strong>一键连锁操作</strong> - 让挖矿、耕作、种植变得轻松高效！
</p>

---

## 📖 目录

- [简介](#简介)
- [安装说明](#安装说明)
- [快速开始](#快速开始)
- [三大功能模块](#三大功能模块)
- [操作说明](#操作说明)
- [配置选项](#配置选项)
- [标签系统](#标签系统)
- [兼容性说明](#兼容性说明)
- [常见问题](#常见问题)

---

## 简介

**OneKeyMiner（一键挖矿）** 是一款强大的 Minecraft 模组，不仅仅是连锁挖矿！它提供三种核心功能：

### ✨ 主要特性

- ⛏️ **连锁挖矿**：一次性挖掘大量相连的同类方块（矿石、原木等）
- ✂️ **连锁交互**：批量剪羊毛、锄地、剥树皮、制作草径等
- 🌱 **连锁种植**：自动在相邻耕地上种植作物
- ⚙️ **高度可配置**：自定义最大方块数、搜索距离、激活方式等
- 🏷️ **标签支持**：使用 `#minecraft:logs`、`#c:shears` 等标签配置
- 🛡️ **安全保护**：自动保护工具耐久度和饥饿值
- 🎮 **多平台支持**：同时支持 Fabric、NeoForge 和 Forge
- 🔧 **模组兼容**：与其他模组的物品和方块完美配合

---

## 安装说明

### 前置要求

- Minecraft 1.21.9
- Java 21 或更高版本
- 对应平台的加载器：
  - **Fabric**：Fabric Loader 0.15.0+ 和 Fabric API
  - **NeoForge**：NeoForge 21.0+
  - **Forge**：Forge 51.0+

### 安装步骤

1. 下载对应平台的模组文件
2. 将 `.jar` 文件放入 `.minecraft/mods` 文件夹
3. 启动游戏

### 可选依赖

- **Mod Menu**（Fabric）：在模组列表中显示配置按钮
- **Cloth Config**（Fabric）：提供更好的配置界面

---

## 快速开始

### 连锁挖矿
1. 手持镐子或斧头
2. **按住潜行键（Shift）**
3. 挖掘一个矿石或原木
4. 观察相连的同类方块被一起挖掘！

### 连锁交互
1. 手持锄头、斧头、铲子或剪刀
2. **按住潜行键（Shift）**
3. 右键点击方块进行交互
4. 相邻的可交互方块也会被处理！

### 连锁种植
1. 手持种子或作物
2. **按住潜行键（Shift）**
3. 右键点击耕地
4. 相邻的空耕地也会被种植！

> 💡 **提示**：默认情况下需要按住潜行键才能激活连锁操作，你可以在配置中修改激活方式。

---

## 三大功能模块

### ⛏️ 连锁挖矿（Chain Mining）

破坏一个方块时，自动破坏相邻的同类方块。

**支持的方块**：
- 所有矿石（包括深层变种和下界矿石）
- 所有原木和木头
- 所有树叶
- 自定义白名单方块

**工具要求**：
- 镐子：矿石类
- 斧头：原木/木头类
- 任何工具：白名单中的方块

### ✂️ 连锁交互（Chain Interaction）

对一个方块使用工具时，自动对相邻方块执行相同操作。

| 工具 | 交互类型 | 目标方块 | 结果 |
|------|----------|----------|------|
| 🪓 斧头 | 剥皮 | 原木/木头 | 去皮原木/木头 |
| 🌾 锄头 | 耕地 | 草方块/泥土 | 耕地 |
| ⛏️ 铲子 | 铺路 | 草方块/泥土 | 草径 |
| ✂️ 剪刀 | 剪羊毛 | 羊/藤蔓等 | 掉落羊毛/藤蔓 |
| 🖌️ 刷子 | 刷洗 | 可疑沙/砾石 | 考古发掘 |

### 🌱 连锁种植（Chain Planting）

种植作物时，自动在相邻的空耕地上种植。

**支持的物品**：
- 小麦种子
- 甜菜根种子
- 胡萝卜、马铃薯
- 瓜种子、南瓜种子
- 模组添加的种子（通过标签配置）

**目标方块**：
- 空的耕地方块
- 与种子类型匹配的可种植方块

---

## 操作说明

### 激活方式

模组支持多种激活方式，可在配置中选择：

| 模式 | 说明 |
|------|------|
| **按住潜行键** | 按住 Shift 时激活（默认） |
| **不按潜行键** | 不按 Shift 时激活 |
| **始终开启** | 随时都会触发 |
| **按键切换** | 使用专用按键切换开关状态 |

### 按键绑定

| 按键 | 功能 | 默认键位 |
|------|------|----------|
| 切换连锁模式 | 开启/关闭所有连锁功能 | ` （波浪键） |
| 打开配置 | 打开模组配置界面 | 未绑定 |

> 📝 在游戏中按 `Esc` → `选项` → `控制` → `按键绑定` 可以修改按键

---

## 配置选项

### 打开配置界面

**Fabric（需要 Mod Menu）**：
1. 按 `Esc` → `模组`
2. 找到 OneKeyMiner → 点击齿轮图标

**NeoForge / Forge**：
1. 按 `Esc` → `模组`
2. 找到 OneKeyMiner → 点击 `配置`

**手动编辑**：
编辑 `.minecraft/config/onekeyminer.json`

### 配置项详解

#### 基础设置

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | ✅ true | 是否启用模组 |
| `enableInteraction` | ✅ true | 是否启用连锁交互 |
| `enablePlanting` | ✅ true | 是否启用连锁种植 |
| `activationMode` | SNEAK | 激活模式 |
| `requireSneak` | ✅ true | 是否需要潜行激活 |

#### 挖矿限制

| 选项 | 默认值 | 范围 | 说明 |
|------|--------|------|------|
| `maxBlocks` | 64 | 1-1000 | 单次连锁操作的最大方块数量 |
| `maxBlocksCreative` | 256 | 1-10000 | 创造模式下的最大方块数量 |
| `maxDistance` | 16 | 1-64 | 连锁搜索的最大距离 |
| `allowDiagonal` | ✅ true | - | 是否包括斜向相邻的方块 |
| `strictBlockMatching` | ❌ false | - | 是否要求方块完全相同 |

#### 保护设置

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `toolProtection` | ✅ true | 工具低耐久时自动停止 |
| `toolProtectionThreshold` | 1 | 保留的最低耐久值 |
| `hungerProtection` | ✅ true | 饥饿值过低时自动停止 |
| `hungerProtectionThreshold` | 2 | 保留的最低饥饿值 |
| `consumeDurability` | ✅ true | 是否消耗工具耐久 |
| `consumeHunger` | ✅ true | 是否消耗饥饿值 |
| `hungerMultiplier` | 1.0 | 饥饿消耗倍率（0.0-10.0） |

#### 白名单与黑名单

| 选项 | 说明 |
|------|------|
| `blockWhitelist` | 方块白名单（支持标签） |
| `blockBlacklist` | 方块黑名单（支持标签） |
| `toolWhitelist` | 挖掘工具白名单 |
| `toolBlacklist` | 挖掘工具黑名单 |
| `interactionToolWhitelist` | 交互工具白名单 |
| `interactionToolBlacklist` | 交互工具黑名单 |
| `plantableWhitelist` | 可种植物品白名单 |
| `plantableBlacklist` | 可种植物品黑名单 |

### 配置文件示例

```json
{
  "enabled": true,
  "enableInteraction": true,
  "enablePlanting": true,
  "activationMode": "SNEAK",
  "requireSneak": true,
  "maxBlocks": 64,
  "maxBlocksCreative": 256,
  "maxDistance": 16,
  "allowDiagonal": true,
  "strictBlockMatching": false,
  "toolProtection": true,
  "toolProtectionThreshold": 1,
  "hungerProtection": true,
  "hungerProtectionThreshold": 2,
  "consumeDurability": true,
  "consumeHunger": true,
  "hungerMultiplier": 1.0,
  "blockWhitelist": [
    "#minecraft:coal_ores",
    "#minecraft:iron_ores",
    "#minecraft:logs"
  ],
  "blockBlacklist": [],
  "toolWhitelist": [],
  "toolBlacklist": [],
  "interactionToolWhitelist": [
    "#c:shears",
    "#minecraft:hoes",
    "#minecraft:axes"
  ],
  "interactionToolBlacklist": [],
  "plantableWhitelist": [
    "#c:seeds"
  ],
  "plantableBlacklist": []
}
```

---

## 标签系统

OneKeyMiner 支持使用 Minecraft 和模组的标签系统来配置方块和物品。

### 标签格式

| 格式 | 说明 | 示例 |
|------|------|------|
| `#namespace:tag` | 标签引用 | `#minecraft:logs` |
| `namespace:id` | 直接 ID | `minecraft:diamond_ore` |
| `*pattern*` | 通配符匹配 | `*_ore` |

### 常用标签

#### 方块标签

| 标签 | 说明 |
|------|------|
| `#minecraft:logs` | 所有原木 |
| `#minecraft:coal_ores` | 煤矿石 |
| `#minecraft:iron_ores` | 铁矿石 |
| `#minecraft:gold_ores` | 金矿石 |
| `#minecraft:diamond_ores` | 钻石矿石 |
| `#minecraft:emerald_ores` | 绿宝石矿石 |
| `#minecraft:lapis_ores` | 青金石矿石 |
| `#minecraft:redstone_ores` | 红石矿石 |
| `#minecraft:copper_ores` | 铜矿石 |
| `#c:ores` | 通用矿石标签（Fabric/Forge） |

#### 物品标签

| 标签 | 说明 |
|------|------|
| `#minecraft:pickaxes` | 所有镐子 |
| `#minecraft:axes` | 所有斧头 |
| `#minecraft:shovels` | 所有铲子 |
| `#minecraft:hoes` | 所有锄头 |
| `#c:shears` | 所有剪刀 |
| `#c:seeds` | 所有种子 |

### 添加模组物品

```json
{
  "blockWhitelist": [
    "#mymod:custom_ores",
    "mymod:magic_crystal_ore"
  ],
  "interactionToolWhitelist": [
    "mymod:magical_shears"
  ]
}
```

---

## 兼容性说明

### 工具与附魔

- ✅ **效率**：加速挖掘
- ✅ **耐久**：减少耐久消耗
- ✅ **时运/精准采集**：正常应用于所有方块
- ✅ **经验修补**：正常获取经验

### 保护插件兼容

模组与以下保护系统兼容：
- ✅ FTB Chunks
- ✅ Claim Chunk
- ✅ 领地插件（Residence）
- ✅ 其他使用标准事件的保护系统

> 受保护区域的方块不会被连锁操作

### 模组兼容

OneKeyMiner 使用通用的物品交互事件，自动兼容：
- ✅ 来自其他模组的矿石和原木
- ✅ 来自其他模组的工具（镐子、斧头、剪刀等）
- ✅ 来自其他模组的种子和作物

### 服务器支持

- ✅ 完全支持多人服务器
- ✅ 配置可由服务器管理员统一设置
- ✅ 玩家的个人设置由客户端保存

---

## 常见问题

### Q: 连锁操作不起作用？

**检查以下几点**：
1. 确保模组已启用（`enabled: true`）
2. 确保使用正确的激活方式（默认是按住 Shift）
3. 确保对应功能已启用（`enableInteraction`、`enablePlanting`）
4. 确保方块/工具在白名单中且不在黑名单中

### Q: 如何添加模组方块到白名单？

编辑 `.minecraft/config/onekeyminer.json`：
```json
{
  "blockWhitelist": [
    "#mymod:custom_ores",
    "mymod:special_ore"
  ]
}
```

### Q: 如何减少饥饿消耗？

调整配置：
- 将 `consumeHunger` 设为 `false` 关闭饥饿消耗
- 或降低 `hungerMultiplier` 的值（如 0.5）

### Q: 如何保护我的工具？

默认已启用保护功能：
- `toolProtection: true`
- `toolProtectionThreshold: 1`

工具会在剩余 1 点耐久时停止操作。

### Q: 连锁种植为什么没有种植所有耕地？

可能的原因：
1. 耕地不在搜索范围内（超过 `maxDistance`）
2. 已达到最大方块数（`maxBlocks`）
3. 手中的种子数量不足
4. 耕地已经有作物

### Q: 如何完全禁用某个功能？

在配置中设置：
- 禁用连锁交互：`"enableInteraction": false`
- 禁用连锁种植：`"enablePlanting": false`
- 禁用整个模组：`"enabled": false`

---

## 反馈与支持

遇到问题或有建议？

- **GitHub Issues**: 报告 Bug 或提出功能请求
- **Discord**: 加入社区讨论

---

<p align="center">
  <strong>享受挖矿、耕作、种植的乐趣！ ⛏️🌾🌱</strong>
</p>

*用户指南版本: 2.0.0 | 适用于 Minecraft 1.21.9*
