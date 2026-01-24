# OneKeyMiner API 开发者文档

## 目录

- [简介](#简介)
- [快速入门](#快速入门)
  - [添加依赖](#添加依赖)
  - [检查模组是否加载](#检查模组是否加载)
- [核心 API](#核心-api)
  - [OneKeyMinerAPI 类](#onekeyminerapi-类)
  - [方块白名单/黑名单](#方块白名单黑名单)
  - [工具白名单/黑名单](#工具白名单黑名单)
  - [方块分组](#方块分组)
  - [查询方法](#查询方法)
- [链式操作系统](#链式操作系统)
  - [ChainActionType 枚举](#chainactiontype-枚举)
  - [ChainActionContext 上下文](#chainactioncontext-上下文)
  - [ChainActionResult 结果](#chainactionresult-结果)
  - [ChainActionLogic 核心逻辑](#chainactionlogic-核心逻辑)
- [事件系统](#事件系统)
  - [PreActionEvent 操作前事件](#preactionevent-操作前事件)
  - [PostActionEvent 操作后事件](#postactionevent-操作后事件)
  - [事件注册方法](#事件注册方法)
- [标签系统](#标签系统)
  - [TagResolver 类](#tagresolver-类)
  - [标签格式](#标签格式)
- [平台服务](#平台服务)
  - [PlatformServices 接口](#platformservices-接口)
  - [平台特定实现](#平台特定实现)
- [配置访问](#配置访问)
  - [MinerConfig 类](#minerconfig-类)
  - [运行时配置修改](#运行时配置修改)
- [代码示例](#代码示例)
  - [基础集成](#示例-1基础集成)
  - [区域保护集成](#示例-2区域保护集成)
  - [操作统计追踪](#示例-3操作统计追踪)
  - [自定义工具逻辑](#示例-4自定义工具逻辑)
  - [附属模组开发](#示例-5附属模组开发)
- [类参考](#类参考)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)
- [技术支持](#技术支持)

---

## 简介

OneKeyMiner 是一个支持 Fabric、NeoForge 和 Forge 的多平台连锁操作模组。本文档面向希望与 OneKeyMiner 集成或扩展其功能的模组开发者。

### 主要特性

- **三种链式操作类型**：挖矿（Mining）、交互（Interaction）、种植（Planting）
- **方块/物品白名单/黑名单 API**：注册自定义方块和工具
- **事件系统**：监听操作前后事件，实现自定义逻辑
- **标签系统**：支持 `#namespace:tag` 格式的标签匹配
- **平台抽象**：通过 SPI 机制支持多平台
- **BFS 算法**：使用广度优先搜索防止栈溢出
- **线程安全 API**：所有 API 方法都是线程安全的

### 支持的平台

| 平台 | 包名 | 最低版本 |
|------|------|----------|
| Fabric | `onekeyminer-fabric` | 0.15.0+ |
| NeoForge | `onekeyminer-neoforge` | 21.0+ |
| Forge | `onekeyminer-forge` | 59.0+ |

---

## 快速入门

### 添加依赖

#### Fabric (build.gradle)

```groovy
repositories {
    maven {
        name = "OneKeyMiner"
        url = "https://maven.example.com/releases"
    }
}

dependencies {
    // 完整实现（推荐）
    modImplementation "org.xiyu:onekeyminer-fabric:${onekeyminer_version}"
    
    // 仅 API（编译时依赖）
    modCompileOnly "org.xiyu:onekeyminer-api:${onekeyminer_version}"
}
```

#### NeoForge (build.gradle)

```groovy
repositories {
    maven {
        name = "OneKeyMiner"
        url = "https://maven.example.com/releases"
    }
}

dependencies {
    implementation "org.xiyu:onekeyminer-neoforge:${onekeyminer_version}"
}
```

#### Forge (build.gradle)

```groovy
repositories {
    maven {
        name = "OneKeyMiner"
        url = "https://maven.example.com/releases"
    }
}

dependencies {
    implementation fg.deobf("org.xiyu:onekeyminer-forge:${onekeyminer_version}")
}
```

### 检查模组是否加载

在调用 OneKeyMiner API 之前，请确保模组已加载：

```java
// Fabric
if (FabricLoader.getInstance().isModLoaded("onekeyminer")) {
    MyModIntegration.init();
}

// Forge/NeoForge
if (ModList.get().isLoaded("onekeyminer")) {
    MyModIntegration.init();
}
```

---

## 核心 API

### OneKeyMinerAPI 类

`org.xiyu.onekeyminer.api.OneKeyMinerAPI` 是主要的 API 入口点。所有方法都是静态的且线程安全。

```java
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
```

### 方块白名单/黑名单

#### 添加方块到白名单

```java
// 通过资源位置字符串注册
OneKeyMinerAPI.registerBlock("mymod:custom_ore");

// 通过 Block 实例注册
OneKeyMinerAPI.registerBlock(MyBlocks.CUSTOM_ORE);

// 注册标签内的所有方块
OneKeyMinerAPI.registerBlockTag("#mymod:custom_ores");
OneKeyMinerAPI.registerBlockTag("#c:ores");  // 通用标签

// 从白名单移除
OneKeyMinerAPI.unregisterBlock("mymod:custom_ore");
```

#### 添加方块到黑名单

黑名单优先级高于白名单。

```java
// 添加到黑名单
OneKeyMinerAPI.blacklistBlock("minecraft:bedrock");
OneKeyMinerAPI.blacklistBlock(Blocks.SPAWNER);

// 从黑名单移除
OneKeyMinerAPI.unblacklistBlock("minecraft:bedrock");
```

### 工具白名单/黑名单

#### 挖掘工具

```java
// 添加到挖掘工具白名单
OneKeyMinerAPI.whitelistTool("mymod:custom_pickaxe");
OneKeyMinerAPI.whitelistTool("#mymod:pickaxes");  // 使用标签

// 添加到挖掘工具黑名单
OneKeyMinerAPI.blacklistTool("minecraft:wooden_pickaxe");
```

#### 交互工具

```java
// 添加到交互工具白名单（剪刀、锄头、去皮用的斧头等）
OneKeyMinerAPI.whitelistInteractionTool("mymod:magic_shears");
OneKeyMinerAPI.whitelistInteractionTool("#c:shears");

// 添加到交互工具黑名单
OneKeyMinerAPI.blacklistInteractionTool("mymod:broken_shears");
```

#### 可种植物品

```java
// 添加到可种植白名单
OneKeyMinerAPI.whitelistPlantable("mymod:magic_seeds");
OneKeyMinerAPI.whitelistPlantable("#c:seeds");

// 添加到可种植黑名单
OneKeyMinerAPI.blacklistPlantable("mymod:cursed_seeds");
```

### 方块分组

方块分组允许不同类型的方块一起被连锁挖掘（用于"宽松匹配"模式）。

```java
// 将方块添加到同一组
OneKeyMinerAPI.addBlockToGroup("mymod:copper_ore", "copper");
OneKeyMinerAPI.addBlockToGroup("mymod:deepslate_copper_ore", "copper");
OneKeyMinerAPI.addBlockToGroup("minecraft:copper_ore", "copper");

// 检查两个方块是否在同一组
boolean sameGroup = OneKeyMinerAPI.areBlocksInSameGroup(block1, block2);

// 检查两个方块是否共享任何标签
boolean shareTag = OneKeyMinerAPI.blocksShareTag(state1, state2);
```

### 查询方法

```java
// 检查方块是否允许连锁挖矿
boolean allowed = OneKeyMinerAPI.isBlockAllowed(block);

// 检查方块是否在黑名单中
boolean blacklisted = OneKeyMinerAPI.isBlockBlacklisted(block);

// 检查工具是否允许连锁挖矿
boolean toolAllowed = OneKeyMinerAPI.isToolAllowed(itemStack);

// 检查工具是否允许连锁交互
boolean interactionAllowed = OneKeyMinerAPI.isInteractionToolAllowed(itemStack);

// 检查物品是否可种植
boolean plantable = OneKeyMinerAPI.isPlantableAllowed(itemStack);

// 访问只读集合
Set<String> whitelist = OneKeyMinerAPI.BLOCK_WHITELIST;
Set<String> blacklist = OneKeyMinerAPI.BLOCK_BLACKLIST;
Set<String> toolWhitelist = OneKeyMinerAPI.TOOL_WHITELIST;
Set<String> toolBlacklist = OneKeyMinerAPI.TOOL_BLACKLIST;
```

---

## 链式操作系统

### ChainActionType 枚举

定义三种链式操作类型：

```java
import org.xiyu.onekeyminer.chain.ChainActionType;

ChainActionType.MINING      // 连锁挖矿（破坏方块）
ChainActionType.INTERACTION // 连锁交互（剪羊毛、锄地、去皮等）
ChainActionType.PLANTING    // 连锁种植（在农田播种）
```

#### ChainActionType 方法

```java
ChainActionType type = ChainActionType.MINING;

// 获取显示名称（用于 UI）
String name = type.getDisplayName();  // "Mining"

// 获取 ID（用于序列化）
String id = type.getId();  // "mining"

// 从字符串解析
ChainActionType parsed = ChainActionType.fromId("mining");
```

### ChainActionContext 上下文

上下文对象包含执行链式操作所需的所有信息。使用 Builder 模式创建：

```java
import org.xiyu.onekeyminer.chain.ChainActionContext;

// 快速创建挖矿上下文
ChainActionContext miningContext = ChainActionContext.forMining(player, level, pos, state)
        .build();

// 快速创建交互上下文
ChainActionContext interactionContext = ChainActionContext.forInteraction(
        player, level, pos, state, heldItem, hand)
        .build();

// 快速创建种植上下文
ChainActionContext plantingContext = ChainActionContext.forPlanting(
        player, level, pos, heldItem, hand)
        .build();

// 完整 Builder 用法
ChainActionContext context = ChainActionContext.builder()
        .player(player)
        .level(level)
        .originPos(pos)
        .originState(state)
        .actionType(ChainActionType.MINING)
        .heldItem(player.getMainHandItem())
        .hand(InteractionHand.MAIN_HAND)
        .config(customConfig)  // 可选：使用自定义配置
        .build();
```

#### ChainActionContext 访问器

```java
ServerPlayer player = context.getPlayer();
Level level = context.getLevel();
BlockPos originPos = context.getOriginPos();
BlockState originState = context.getOriginState();
ChainActionType actionType = context.getActionType();
ItemStack heldItem = context.getHeldItem();
InteractionHand hand = context.getHand();
MinerConfig config = context.getConfig();
```

### ChainActionResult 结果

执行链式操作后返回的结果对象：

```java
import org.xiyu.onekeyminer.chain.ChainActionResult;

ChainActionResult result = ChainActionLogic.execute(context);

// 检查是否成功
boolean success = result.isSuccess();

// 获取统计信息
int totalCount = result.totalCount();         // 处理的总方块数
int durabilityUsed = result.durabilityUsed(); // 消耗的耐久度
int hungerUsed = result.hungerUsed();         // 消耗的饥饿值
Set<BlockPos> positions = result.successPositions(); // 所有处理的位置

// 获取停止原因
ChainActionResult.StopReason reason = result.stopReason();

// 获取摘要字符串（用于日志/显示）
String summary = result.getSummary();
// 示例: "Mining: 32 blocks, stopped: MAX_BLOCKS"
```

#### StopReason 枚举

| 枚举值 | 描述 |
|--------|------|
| `COMPLETED` | 正常完成（所有方块处理完毕） |
| `MAX_BLOCKS` | 达到最大方块数限制 |
| `MAX_DISTANCE` | 达到最大距离限制 |
| `LOW_DURABILITY` | 工具耐久度不足 |
| `TOOL_BROKEN` | 工具在操作中损坏 |
| `LOW_HUNGER` | 玩家饥饿值不足 |
| `TIMEOUT` | 操作超时（防卡顿保护） |
| `EVENT_CANCELLED` | 被事件监听器取消 |
| `NO_VALID_BLOCKS` | 没有找到有效方块 |
| `ERROR` | 发生错误 |

### ChainActionLogic 核心逻辑

执行链式操作的核心逻辑类：

```java
import org.xiyu.onekeyminer.chain.ChainActionLogic;

// 使用上下文执行
ChainActionResult result = ChainActionLogic.execute(context);

// 方块破坏事件的便捷方法
ChainActionResult miningResult = ChainActionLogic.onBlockBreak(
        player, level, pos, state);

// 检查是否应该触发连锁挖矿（不执行）
boolean shouldTrigger = ChainActionLogic.shouldTriggerChainMining(
        player, level, pos, state);
```

---

## 事件系统

OneKeyMiner 提供事件系统供外部模组监听和干预链式操作。

### PreActionEvent 操作前事件

在链式操作开始前触发。**可取消**。

```java
import org.xiyu.onekeyminer.api.event.PreActionEvent;

public class PreActionEvent {
    // 获取执行操作的玩家
    ServerPlayer getPlayer();
    
    // 获取世界实例
    Level getLevel();
    
    // 获取起始方块位置
    BlockPos getOriginPos();
    
    // 获取起始方块状态
    BlockState getOriginState();
    
    // 获取操作类型
    ChainActionType getActionType();
    
    // 获取手持物品
    ItemStack getHeldItem();
    
    // 获取目标位置（可修改 - 添加/移除位置）
    Set<BlockPos> getTargetPositions();
    
    // 检查是否已取消
    boolean isCancelled();
    
    // 取消操作
    void cancel();
    
    // 设置取消状态
    void setCancelled(boolean cancelled);
}
```

### PostActionEvent 操作后事件

在链式操作完成后触发。**不可取消** - 仅用于信息收集。

```java
import org.xiyu.onekeyminer.api.event.PostActionEvent;

public class PostActionEvent {
    // 获取执行操作的玩家
    ServerPlayer getPlayer();
    
    // 获取世界实例
    Level getLevel();
    
    // 获取起始方块位置
    BlockPos getOriginPos();
    
    // 获取操作类型
    ChainActionType getActionType();
    
    // 获取操作结果
    ChainActionResult getResult();
}
```

### 事件注册方法

```java
import org.xiyu.onekeyminer.api.event.ChainEvents;

// 注册操作前监听器（所有类型）
ChainEvents.registerPreActionListener(event -> {
    if (isProtectedArea(event.getOriginPos())) {
        event.cancel();
        event.getPlayer().sendSystemMessage(
            Component.literal("此区域禁止链式操作！"));
    }
});

// 仅注册特定操作类型
ChainEvents.registerPreActionListener(ChainActionType.MINING, event -> {
    // 只处理挖矿事件
});

// 带条件的注册
ChainEvents.registerPreActionListener(
    event -> event.getPlayer().hasPermissions(2),  // 条件：仅管理员
    event -> {
        // 只对管理员执行
    }
);

// 注册操作后监听器
ChainEvents.registerPostActionListener(event -> {
    LOGGER.info("玩家 {} 通过 {} 处理了 {} 个方块",
        event.getPlayer().getName().getString(),
        event.getActionType().getDisplayName(),
        event.getResult().totalCount());
});

// 注册特定类型
ChainEvents.registerPostActionListener(ChainActionType.PLANTING, event -> {
    // 只处理种植事件
});

// 注销监听器（保留引用）
var listener = ChainEvents.registerPreActionListener(event -> { /* ... */ });
ChainEvents.unregisterPreActionListener(listener);
```

---

## 标签系统

### TagResolver 类

OneKeyMiner 使用 `TagResolver` 进行方块和物品的模式匹配。

```java
import org.xiyu.onekeyminer.registry.TagResolver;

// 获取单例实例
TagResolver resolver = TagResolver.getInstance();

// 检查方块是否匹配模式
boolean matches = resolver.matchesBlock(blockState, "#minecraft:logs");

// 检查物品是否匹配模式
boolean matches = resolver.matchesItem(itemStack, "#c:shears");

// 检查两个方块是否共享任何标签
boolean shareTag = resolver.blocksShareTag(state1, state2);

// 验证模式格式
boolean valid = resolver.isValidPattern("#minecraft:logs");

// 清除缓存（配置更改后调用）
resolver.clearCache();
```

### 标签格式

| 格式 | 说明 | 示例 |
|------|------|------|
| `#namespace:tag` | 标签引用 | `#minecraft:logs`、`#c:ores` |
| `namespace:id` | 直接资源位置 | `minecraft:diamond_ore` |
| `*pattern*` | 包含匹配 | `*ore*` |
| `pattern*` | 前缀匹配 | `minecraft:*` |
| `*pattern` | 后缀匹配 | `*_ore` |

---

## 平台服务

### PlatformServices 接口

OneKeyMiner 使用平台抽象层实现跨平台兼容。

```java
import org.xiyu.onekeyminer.platform.PlatformServices;

// 获取平台实例
PlatformServices platform = PlatformServices.getInstance();

// 获取平台名称
String name = platform.getPlatformName();  // "Fabric"、"NeoForge" 或 "Forge"

// 检查模组是否加载
boolean loaded = platform.isModLoaded("mymod");

// 获取配置目录
Path configDir = platform.getConfigDirectory();

// 检查/设置玩家的连锁模式
boolean active = platform.isChainModeActive(player);
platform.setChainModeActive(player, true);
```

### 平台特定实现

每个平台都有自己的实现：

- `FabricPlatformServices` - Fabric 实现
- `NeoForgePlatformServices` - NeoForge 实现
- `ForgePlatformServices` - Forge 实现

正确的实现通过 SPI（服务提供者接口）自动加载。

---

## 配置访问

### MinerConfig 类

访问当前配置：

```java
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.config.ConfigManager;

// 获取当前配置
MinerConfig config = ConfigManager.getConfig();

// 访问配置值
boolean enabled = config.enabled;              // 是否启用
int maxBlocks = config.maxBlocks;              // 最大方块数
int maxDistance = config.maxDistance;          // 最大距离
boolean allowDiagonal = config.allowDiagonal;  // 允许对角线连接
boolean consumeDurability = config.consumeDurability;  // 消耗耐久
int preserveDurability = config.preserveDurability;    // 保留耐久
boolean consumeHunger = config.consumeHunger;  // 消耗饥饿值
int minHungerLevel = config.minHungerLevel;    // 最低饥饿值
boolean allowBareHand = config.allowBareHand;  // 允许空手
boolean teleportDrops = config.teleportDrops;  // 传送掉落物
boolean teleportExp = config.teleportExp;      // 传送经验
boolean showPreview = config.showPreview;      // 显示预览
boolean playSound = config.playSound;          // 播放音效
```

### 运行时配置修改

```java
// 从文件重新加载配置
ConfigManager.reload();

// 保存当前配置到文件
ConfigManager.save();
```

---

## 代码示例

### 示例 1：基础集成

```java
package com.example.mymod;

import org.xiyu.onekeyminer.api.OneKeyMinerAPI;

public class MyModIntegration {
    
    public static void init() {
        // 检查 OneKeyMiner 是否已加载
        if (!isModLoaded("onekeyminer")) {
            return;
        }
        
        registerContent();
    }
    
    private static void registerContent() {
        // 注册自定义矿石
        OneKeyMinerAPI.registerBlock("mymod:mythril_ore");
        OneKeyMinerAPI.registerBlock("mymod:deepslate_mythril_ore");
        
        // 或者使用标签（推荐）
        OneKeyMinerAPI.registerBlockTag("#mymod:mythril_ores");
        
        // 添加到同一组以支持宽松匹配
        OneKeyMinerAPI.addBlockToGroup("mymod:mythril_ore", "mythril");
        OneKeyMinerAPI.addBlockToGroup("mymod:deepslate_mythril_ore", "mythril");
        
        // 注册自定义工具
        OneKeyMinerAPI.whitelistTool("mymod:mythril_pickaxe");
        OneKeyMinerAPI.whitelistInteractionTool("mymod:mythril_shears");
        
        // 注册自定义种子
        OneKeyMinerAPI.whitelistPlantable("mymod:magic_seeds");
    }
}
```

### 示例 2：区域保护集成

```java
package com.example.protection;

import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.api.event.PreActionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class ProtectionIntegration {
    
    public static void init() {
        ChainEvents.registerPreActionListener(ProtectionIntegration::onPreAction);
    }
    
    private static void onPreAction(PreActionEvent event) {
        BlockPos origin = event.getOriginPos();
        var player = event.getPlayer();
        
        // 检查起始位置是否在保护区域
        if (ProtectionAPI.isProtected(origin, player)) {
            event.cancel();
            player.sendSystemMessage(
                Component.literal("§c此保护区域内禁止链式操作！"));
            return;
        }
        
        // 从目标列表中移除受保护的方块
        event.getTargetPositions().removeIf(pos -> 
            ProtectionAPI.isProtected(pos, player)
        );
        
        // 限制在领地边界内
        event.getTargetPositions().removeIf(pos ->
            !ProtectionAPI.isInSameClaim(origin, pos, player)
        );
    }
}
```

### 示例 3：操作统计追踪

```java
package com.example.stats;

import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.api.event.PostActionEvent;
import org.xiyu.onekeyminer.chain.ChainActionType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ActionStatsTracker {
    
    private static final Map<UUID, Map<ChainActionType, AtomicLong>> PLAYER_STATS = 
        new ConcurrentHashMap<>();
    
    public static void init() {
        ChainEvents.registerPostActionListener(event -> {
            UUID playerId = event.getPlayer().getUUID();
            ChainActionType actionType = event.getActionType();
            int count = event.getResult().totalCount();
            
            PLAYER_STATS
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(actionType, k -> new AtomicLong(0))
                .addAndGet(count);
            
            // 成就检查
            long total = getTotalActions(playerId, actionType);
            if (total >= 1000) {
                grantAchievement(event.getPlayer(), "chain_master_" + actionType.getId());
            }
        });
    }
    
    public static long getTotalActions(UUID playerId, ChainActionType type) {
        return PLAYER_STATS
            .getOrDefault(playerId, Map.of())
            .getOrDefault(type, new AtomicLong(0))
            .get();
    }
    
    public static Map<ChainActionType, Long> getPlayerStats(UUID playerId) {
        Map<ChainActionType, AtomicLong> stats = PLAYER_STATS.get(playerId);
        if (stats == null) return Map.of();
        
        Map<ChainActionType, Long> result = new HashMap<>();
        stats.forEach((type, count) -> result.put(type, count.get()));
        return result;
    }
}
```

### 示例 4：自定义工具逻辑

```java
package com.example.custom;

import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.chain.ChainActionType;

public class CustomToolHandler {
    
    public static void init() {
        // 修改自定义工具的行为
        ChainEvents.registerPreActionListener(ChainActionType.MINING, event -> {
            var player = event.getPlayer();
            var item = player.getMainHandItem();
            
            // 检查附魔或 NBT
            if (item.is(MyItems.MINING_LASER)) {
                // 采矿激光没有方块限制
                // （由自定义逻辑处理）
            }
            
            // 检查特殊修饰器
            if (item.getEnchantmentLevel(MyEnchantments.EXCAVATION) > 0) {
                // 双倍目标
                int level = item.getEnchantmentLevel(MyEnchantments.EXCAVATION);
                // 自定义扩展逻辑...
            }
        });
        
        ChainEvents.registerPostActionListener(ChainActionType.MINING, event -> {
            var item = event.getPlayer().getMainHandItem();
            
            // 触发特殊效果
            if (item.is(MyItems.FORTUNE_AMPLIFIER)) {
                // 应用额外掉落
                applyFortune(event.getResult().successPositions(), event.getPlayer());
            }
        });
    }
}
```

### 示例 5：附属模组开发

OneKeyMiner 附属模组的示例结构：

```java
package com.example.okm_addon;

import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.api.event.PreActionEvent;
import org.xiyu.onekeyminer.chain.ChainActionType;

public class OKMAddonMain {
    
    public static void init() {
        // 注册附属模组的方块/工具
        registerContent();
        
        // 设置事件监听器
        setupEventListeners();
        
        // 可选：设置客户端渲染
        if (isClientSide()) {
            OKMAddonClient.init();
        }
    }
    
    private static void registerContent() {
        // 注册附属模组的自定义矿石
        OneKeyMinerAPI.registerBlockTag("#okm_addon:custom_ores");
        
        // 注册附属模组的自定义工具
        OneKeyMinerAPI.whitelistTool("#okm_addon:enhanced_pickaxes");
    }
    
    private static void setupEventListeners() {
        // 操作前：添加可视化预览
        ChainEvents.registerPreActionListener(event -> {
            if (event.getActionType() == ChainActionType.MINING) {
                // 可以向客户端发送数据包用于预览渲染
                PreviewRenderer.showPreview(
                    event.getPlayer(), 
                    event.getTargetPositions()
                );
            }
        });
        
        // 操作后：自定义音效/粒子效果
        ChainEvents.registerPostActionListener(event -> {
            if (event.getResult().totalCount() > 10) {
                playChainMiningSound(event.getPlayer(), event.getOriginPos());
                spawnChainMiningParticles(event.getResult().successPositions());
            }
        });
    }
}
```

#### 预览渲染参考

如果你想为 OneKeyMiner 添加方块预览高亮功能，可以参考 [LiteMiner 的 BlockHighlightRenderer](https://github.com/iamkaf/liteminer/blob/1.21.9/common/src/main/java/com/iamkaf/liteminer/rendering/BlockHighlightRenderer.java)。

基本思路：
1. 监听 `PreActionEvent` 获取目标方块位置
2. 通过网络包发送位置数据到客户端
3. 在客户端使用 `WorldRenderEvents`（Fabric）或 `RenderLevelStageEvent`（Forge/NeoForge）渲染高亮

---

## 类参考

### OneKeyMinerAPI

| 方法 | 描述 |
|------|------|
| `registerBlock(String)` | 通过 ID 注册方块到白名单 |
| `registerBlock(Block)` | 通过实例注册方块到白名单 |
| `registerBlockTag(String)` | 注册方块标签到白名单 |
| `unregisterBlock(String)` | 从白名单移除方块 |
| `blacklistBlock(String)` | 将方块添加到黑名单 |
| `blacklistBlock(Block)` | 将方块实例添加到黑名单 |
| `unblacklistBlock(String)` | 从黑名单移除方块 |
| `whitelistTool(String)` | 将工具添加到挖掘白名单 |
| `blacklistTool(String)` | 将工具添加到挖掘黑名单 |
| `whitelistInteractionTool(String)` | 将工具添加到交互白名单 |
| `blacklistInteractionTool(String)` | 将工具添加到交互黑名单 |
| `whitelistPlantable(String)` | 将物品添加到可种植白名单 |
| `blacklistPlantable(String)` | 将物品添加到可种植黑名单 |
| `addBlockToGroup(String, String)` | 将方块添加到分组 |
| `areBlocksInSameGroup(Block, Block)` | 检查方块是否在同一组 |
| `blocksShareTag(BlockState, BlockState)` | 检查方块是否共享任何标签 |
| `isBlockAllowed(Block)` | 检查方块是否允许连锁 |
| `isBlockBlacklisted(Block)` | 检查方块是否在黑名单中 |
| `isToolAllowed(ItemStack)` | 检查工具是否允许挖掘 |
| `isInteractionToolAllowed(ItemStack)` | 检查工具是否允许交互 |
| `isPlantableAllowed(ItemStack)` | 检查物品是否可种植 |

### ChainActionContext.Builder

| 方法 | 描述 |
|------|------|
| `player(ServerPlayer)` | 设置玩家 |
| `level(Level)` | 设置世界 |
| `originPos(BlockPos)` | 设置起始位置 |
| `originState(BlockState)` | 设置起始方块状态 |
| `actionType(ChainActionType)` | 设置操作类型 |
| `heldItem(ItemStack)` | 设置手持物品 |
| `hand(InteractionHand)` | 设置交互手 |
| `config(MinerConfig)` | 设置自定义配置（可选） |
| `build()` | 构建上下文 |

### ChainActionResult

| 方法 | 描述 |
|------|------|
| `actionType()` | 获取操作类型 |
| `successPositions()` | 获取已处理位置的集合 |
| `totalCount()` | 获取处理的总方块数 |
| `durabilityUsed()` | 获取消耗的耐久度 |
| `hungerUsed()` | 获取消耗的饥饿值 |
| `stopReason()` | 获取停止原因 |
| `isSuccess()` | 检查是否至少处理了一个方块 |
| `getSummary()` | 获取摘要字符串 |

### PreActionEvent

| 方法 | 描述 |
|------|------|
| `getPlayer()` | 获取执行操作的玩家 |
| `getLevel()` | 获取世界实例 |
| `getOriginPos()` | 获取起始方块位置 |
| `getOriginState()` | 获取起始方块状态 |
| `getActionType()` | 获取操作类型 |
| `getHeldItem()` | 获取手持物品 |
| `getTargetPositions()` | 获取目标位置集合（可修改） |
| `isCancelled()` | 检查是否已取消 |
| `cancel()` | 取消操作 |
| `setCancelled(boolean)` | 设置取消状态 |

### PostActionEvent

| 方法 | 描述 |
|------|------|
| `getPlayer()` | 获取执行操作的玩家 |
| `getLevel()` | 获取世界实例 |
| `getOriginPos()` | 获取起始方块位置 |
| `getActionType()` | 获取操作类型 |
| `getResult()` | 获取操作结果 |

---

## 最佳实践

### 1. 检查模组是否存在

在调用 OneKeyMiner API 之前，始终检查模组是否已加载：

```java
if (Platform.isModLoaded("onekeyminer")) {
    // 安全调用 OneKeyMiner API
}
```

### 2. 优先使用标签而非单个方块

优先注册方块标签而非单个方块，以获得更好的兼容性：

```java
// 推荐 - 与模组兼容性好
OneKeyMinerAPI.registerBlockTag("#c:ores");

// 不太理想 - 只适用于原版
OneKeyMinerAPI.registerBlock("minecraft:diamond_ore");
```

### 3. 尊重事件取消

处理 PreActionEvent 时，尊重其他模组是否已取消：

```java
ChainEvents.registerPreActionListener(event -> {
    if (event.isCancelled()) return; // 尊重其他模组
    
    // 你的逻辑
});
```

### 4. 线程安全

API 方法是线程安全的，但要注意你自己的数据结构：

```java
// 使用 ConcurrentHashMap 进行线程安全存储
private static final Map<UUID, Data> playerData = new ConcurrentHashMap<>();
```

### 5. 最小化事件处理器工作

保持事件处理器轻量 - 延迟重型工作：

```java
ChainEvents.registerPostActionListener(event -> {
    // 不要在这里做重型工作
    // 而是将其排队稍后处理
    scheduler.schedule(() -> processStats(event));
});
```

### 6. 模组初始化时机

在模组的主初始化方法中注册内容，确保 OneKeyMiner 已加载：

```java
// Fabric
@Override
public void onInitialize() {
    if (FabricLoader.getInstance().isModLoaded("onekeyminer")) {
        MyOKMIntegration.init();
    }
}

// Forge/NeoForge
@SubscribeEvent
public void onCommonSetup(FMLCommonSetupEvent event) {
    if (ModList.get().isLoaded("onekeyminer")) {
        event.enqueueWork(MyOKMIntegration::init);
    }
}
```

---

## 常见问题

### Q: 如何让我的自定义矿石支持连锁挖矿？

A: 可以在你的 data 文件中将方块添加到支持的标签（如 `#c:ores`），或者在模组初始化时调用 `OneKeyMinerAPI.registerBlock()`。

### Q: 我可以在操作过程中修改哪些方块会被连锁挖掘吗？

A: 可以，使用 `PreActionEvent.getTargetPositions()` 来修改目标集合。

### Q: 为什么我的方块没有被连锁挖掘？

A: 检查以下几点：
1. 方块是否通过 API 或标签注册？
2. 方块是否在黑名单中？
3. 工具是否有效？
4. 玩家是否按住了激活键？

### Q: 我可以通过代码触发连锁挖矿吗？

A: 可以，创建一个 `ChainActionContext` 并调用 `ChainActionLogic.execute()`。

### Q: 如何为连锁挖矿添加预览高亮？

A: 监听 `PreActionEvent`，获取目标位置，然后发送到客户端渲染器。参见上面的示例 5。

### Q: 事件监听器的执行顺序是什么？

A: 按注册顺序执行。如果多个模组注册了监听器，它们按注册顺序依次执行。

### Q: API 是向后兼容的吗？

A: 是的，API 保持向后兼容。次版本更新不会破坏现有集成。

---

## 技术支持

- **GitHub Issues**: https://github.com/Mai-xiyu/OneKeyMiner/issues
- **源代码**: https://github.com/Mai-xiyu/OneKeyMiner

---

*文档版本: 2.0.0 | 最后更新: 2026年1月*

---

## 版权声明

本项目采用 **All Rights Reserved (ARR)** 协议。未经作者许可，不得复制、修改或分发本项目代码。
