# OneKeyMiner API Developer Documentation

## Table of Contents

- [Introduction](#introduction)
- [Getting Started](#getting-started)
  - [Adding Dependency](#adding-dependency)
  - [Checking Mod Presence](#checking-mod-presence)
- [Core API](#core-api)
  - [OneKeyMinerAPI Class](#onekeyminerapi-class)
  - [Block Whitelist/Blacklist](#block-whitelistblacklist)
  - [Tool Whitelist/Blacklist](#tool-whitelistblacklist)
  - [Block Grouping](#block-grouping)
  - [Query Methods](#query-methods)
- [Chain Action System](#chain-action-system)
  - [ChainActionType Enum](#chainactiontype-enum)
  - [ChainActionContext](#chainactioncontext)
  - [ChainActionResult](#chainactionresult)
  - [ChainActionLogic](#chainactionlogic)
- [Event System](#event-system)
  - [PreActionEvent](#preactionevent)
  - [PostActionEvent](#postactionevent)
  - [Event Registration](#event-registration)
- [Tag System](#tag-system)
  - [TagResolver Class](#tagresolver-class)
  - [Tag Formats](#tag-formats)
- [Platform Services](#platform-services)
  - [PlatformServices Interface](#platformservices-interface)
  - [Platform-Specific Implementations](#platform-specific-implementations)
- [Configuration Access](#configuration-access)
  - [MinerConfig Class](#minerconfig-class)
  - [Runtime Config Modification](#runtime-config-modification)
- [Code Examples](#code-examples)
  - [Basic Integration](#example-1-basic-integration)
  - [Region Protection](#example-2-region-protection-integration)
  - [Statistics Tracking](#example-3-statistics-tracking)
  - [Custom Tool Logic](#example-4-custom-tool-logic)
  - [Addon Development](#example-5-addon-development)
- [Class Reference](#class-reference)
- [Best Practices](#best-practices)
- [FAQ](#faq)
- [Support](#support)

---

## Introduction

OneKeyMiner is a multi-platform chain operation mod supporting Fabric, NeoForge, and Forge. This documentation is intended for mod developers who wish to integrate with or extend OneKeyMiner's functionality.

### Key Features

- **Three Chain Operation Types**: Mining, Interaction, and Planting
- **Block/Item Whitelist/Blacklist API**: Register custom blocks and tools
- **Event System**: Listen to pre/post operation events for custom logic
- **Tag System**: Support for `#namespace:tag` format tag matching
- **Platform Abstraction**: Multi-platform support via SPI mechanism
- **BFS Algorithm**: Breadth-first search prevents stack overflow
- **Thread-Safe API**: All API methods are thread-safe

### Supported Platforms

| Platform | Package Name | Min Version |
|----------|--------------|-------------|
| Fabric | `onekeyminer-fabric` | 0.15.0+ |
| NeoForge | `onekeyminer-neoforge` | 21.0+ |
| Forge | `onekeyminer-forge` | 59.0+ |

---

## Getting Started

### Adding Dependency

#### Fabric (build.gradle)

```groovy
repositories {
    maven {
        name = "OneKeyMiner"
        url = "https://maven.example.com/releases"
    }
}

dependencies {
    // Full implementation (recommended)
    modImplementation "org.xiyu:onekeyminer-fabric:${onekeyminer_version}"
    
    // API only (compile-time dependency)
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

### Checking Mod Presence

Before calling OneKeyMiner API, ensure the mod is loaded:

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

## Core API

### OneKeyMinerAPI Class

`org.xiyu.onekeyminer.api.OneKeyMinerAPI` is the main API entry point. All methods are static and thread-safe.

```java
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
```

### Block Whitelist/Blacklist

#### Adding Blocks to Whitelist

```java
// Register by resource location string
OneKeyMinerAPI.registerBlock("mymod:custom_ore");

// Register by Block instance
OneKeyMinerAPI.registerBlock(MyBlocks.CUSTOM_ORE);

// Register all blocks in a tag
OneKeyMinerAPI.registerBlockTag("#mymod:custom_ores");
OneKeyMinerAPI.registerBlockTag("#c:ores");  // Common tag

// Remove from whitelist
OneKeyMinerAPI.unregisterBlock("mymod:custom_ore");
```

#### Adding Blocks to Blacklist

Blacklist has higher priority than whitelist.

```java
// Add to blacklist
OneKeyMinerAPI.blacklistBlock("minecraft:bedrock");
OneKeyMinerAPI.blacklistBlock(Blocks.SPAWNER);

// Remove from blacklist
OneKeyMinerAPI.unblacklistBlock("minecraft:bedrock");
```

### Tool Whitelist/Blacklist

#### Mining Tools

```java
// Add to mining tool whitelist
OneKeyMinerAPI.whitelistTool("mymod:custom_pickaxe");
OneKeyMinerAPI.whitelistTool("#mymod:pickaxes");  // Using tag

// Add to mining tool blacklist
OneKeyMinerAPI.blacklistTool("minecraft:wooden_pickaxe");
```

#### Interaction Tools

```java
// Add to interaction tool whitelist (shears, hoes, axes for stripping, etc.)
OneKeyMinerAPI.whitelistInteractionTool("mymod:magic_shears");
OneKeyMinerAPI.whitelistInteractionTool("#c:shears");

// Add to interaction tool blacklist
OneKeyMinerAPI.blacklistInteractionTool("mymod:broken_shears");
```

#### Plantable Items

```java
// Add to plantable whitelist
OneKeyMinerAPI.whitelistPlantable("mymod:magic_seeds");
OneKeyMinerAPI.whitelistPlantable("#c:seeds");

// Add to plantable blacklist
OneKeyMinerAPI.blacklistPlantable("mymod:cursed_seeds");
```

### Block Grouping

Block groups allow different block types to be chain-mined together (useful for "loose matching" mode).

```java
// Add blocks to the same group
OneKeyMinerAPI.addBlockToGroup("mymod:copper_ore", "copper");
OneKeyMinerAPI.addBlockToGroup("mymod:deepslate_copper_ore", "copper");
OneKeyMinerAPI.addBlockToGroup("minecraft:copper_ore", "copper");

// Check if two blocks are in the same group
boolean sameGroup = OneKeyMinerAPI.areBlocksInSameGroup(block1, block2);

// Check if two blocks share any tag
boolean shareTag = OneKeyMinerAPI.blocksShareTag(state1, state2);
```

### Query Methods

```java
// Check if a block is allowed for chain mining
boolean allowed = OneKeyMinerAPI.isBlockAllowed(block);

// Check if a block is blacklisted
boolean blacklisted = OneKeyMinerAPI.isBlockBlacklisted(block);

// Check if a tool is allowed for chain mining
boolean toolAllowed = OneKeyMinerAPI.isToolAllowed(itemStack);

// Check if a tool is allowed for chain interaction
boolean interactionAllowed = OneKeyMinerAPI.isInteractionToolAllowed(itemStack);

// Check if an item is plantable
boolean plantable = OneKeyMinerAPI.isPlantableAllowed(itemStack);

// Access read-only sets
Set<String> whitelist = OneKeyMinerAPI.BLOCK_WHITELIST;
Set<String> blacklist = OneKeyMinerAPI.BLOCK_BLACKLIST;
Set<String> toolWhitelist = OneKeyMinerAPI.TOOL_WHITELIST;
Set<String> toolBlacklist = OneKeyMinerAPI.TOOL_BLACKLIST;
```

---

## Chain Action System

### ChainActionType Enum

Defines the three types of chain operations:

```java
import org.xiyu.onekeyminer.chain.ChainActionType;

ChainActionType.MINING      // Chain mining (breaking blocks)
ChainActionType.INTERACTION // Chain interaction (shearing, hoeing, stripping, etc.)
ChainActionType.PLANTING    // Chain planting (planting seeds on farmland)
```

#### ChainActionType Methods

```java
ChainActionType type = ChainActionType.MINING;

// Get display name (for UI)
String name = type.getDisplayName();  // "Mining"

// Get ID (for serialization)
String id = type.getId();  // "mining"

// Parse from string
ChainActionType parsed = ChainActionType.fromId("mining");
```

### ChainActionContext

The context object contains all information needed to execute a chain operation. Use the Builder pattern to create:

```java
import org.xiyu.onekeyminer.chain.ChainActionContext;

// Quick creation for mining
ChainActionContext miningContext = ChainActionContext.forMining(player, level, pos, state)
        .build();

// Quick creation for interaction
ChainActionContext interactionContext = ChainActionContext.forInteraction(
        player, level, pos, state, heldItem, hand)
        .build();

// Quick creation for planting
ChainActionContext plantingContext = ChainActionContext.forPlanting(
        player, level, pos, heldItem, hand)
        .build();

// Full Builder usage
ChainActionContext context = ChainActionContext.builder()
        .player(player)
        .level(level)
        .originPos(pos)
        .originState(state)
        .actionType(ChainActionType.MINING)
        .heldItem(player.getMainHandItem())
        .hand(InteractionHand.MAIN_HAND)
        .config(customConfig)  // Optional: use custom config
        .build();
```

#### ChainActionContext Accessors

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

### ChainActionResult

The result object returned after executing a chain operation:

```java
import org.xiyu.onekeyminer.chain.ChainActionResult;

ChainActionResult result = ChainActionLogic.execute(context);

// Check success
boolean success = result.isSuccess();

// Get statistics
int totalCount = result.totalCount();         // Total blocks processed
int durabilityUsed = result.durabilityUsed(); // Durability consumed
int hungerUsed = result.hungerUsed();         // Hunger consumed
Set<BlockPos> positions = result.successPositions(); // All processed positions

// Get stop reason
ChainActionResult.StopReason reason = result.stopReason();

// Get summary string (for logging/display)
String summary = result.getSummary();
// Example: "Mining: 32 blocks, stopped: MAX_BLOCKS"
```

#### StopReason Enum

| Value | Description |
|-------|-------------|
| `COMPLETED` | Normal completion (all blocks processed) |
| `MAX_BLOCKS` | Reached maximum block limit |
| `MAX_DISTANCE` | Reached maximum distance limit |
| `LOW_DURABILITY` | Tool durability too low |
| `TOOL_BROKEN` | Tool broke during operation |
| `LOW_HUNGER` | Player hunger too low |
| `TIMEOUT` | Operation timeout (anti-lag protection) |
| `EVENT_CANCELLED` | Cancelled by event listener |
| `NO_VALID_BLOCKS` | No valid blocks found |
| `ERROR` | An error occurred |

### ChainActionLogic

The core logic class for executing chain operations:

```java
import org.xiyu.onekeyminer.chain.ChainActionLogic;

// Execute with context
ChainActionResult result = ChainActionLogic.execute(context);

// Convenience method for block break events
ChainActionResult miningResult = ChainActionLogic.onBlockBreak(
        player, level, pos, state);

// Check if chain mining should trigger (without executing)
boolean shouldTrigger = ChainActionLogic.shouldTriggerChainMining(
        player, level, pos, state);
```

---

## Event System

OneKeyMiner provides an event system for external mods to listen to and intervene in chain operations.

### PreActionEvent

Triggered before a chain operation starts. **Cancellable**.

```java
import org.xiyu.onekeyminer.api.event.PreActionEvent;

public class PreActionEvent {
    // Get player performing the action
    ServerPlayer getPlayer();
    
    // Get world instance
    Level getLevel();
    
    // Get origin block position
    BlockPos getOriginPos();
    
    // Get origin block state
    BlockState getOriginState();
    
    // Get action type
    ChainActionType getActionType();
    
    // Get held item
    ItemStack getHeldItem();
    
    // Get target positions (MODIFIABLE - add/remove positions)
    Set<BlockPos> getTargetPositions();
    
    // Check if cancelled
    boolean isCancelled();
    
    // Cancel the operation
    void cancel();
    
    // Set cancelled state
    void setCancelled(boolean cancelled);
}
```

### PostActionEvent

Triggered after a chain operation completes. **NOT cancellable** - for information only.

```java
import org.xiyu.onekeyminer.api.event.PostActionEvent;

public class PostActionEvent {
    // Get player who performed the action
    ServerPlayer getPlayer();
    
    // Get world instance
    Level getLevel();
    
    // Get origin block position
    BlockPos getOriginPos();
    
    // Get action type
    ChainActionType getActionType();
    
    // Get operation result
    ChainActionResult getResult();
}
```

### Event Registration

```java
import org.xiyu.onekeyminer.api.event.ChainEvents;

// Register pre-action listener (all types)
ChainEvents.registerPreActionListener(event -> {
    if (isProtectedArea(event.getOriginPos())) {
        event.cancel();
        event.getPlayer().sendSystemMessage(
            Component.literal("Chain operations disabled in this area!"));
    }
});

// Register for specific action type only
ChainEvents.registerPreActionListener(ChainActionType.MINING, event -> {
    // Only handles mining events
});

// Register with condition
ChainEvents.registerPreActionListener(
    event -> event.getPlayer().hasPermissions(2),  // Condition: OP only
    event -> {
        // Only runs for operators
    }
);

// Register post-action listener
ChainEvents.registerPostActionListener(event -> {
    LOGGER.info("Player {} processed {} blocks via {}",
        event.getPlayer().getName().getString(),
        event.getResult().totalCount(),
        event.getActionType().getDisplayName());
});

// Register for specific type
ChainEvents.registerPostActionListener(ChainActionType.PLANTING, event -> {
    // Only handles planting events
});

// Unregister listener (keep reference)
var listener = ChainEvents.registerPreActionListener(event -> { /* ... */ });
ChainEvents.unregisterPreActionListener(listener);
```

---

## Tag System

### TagResolver Class

OneKeyMiner uses `TagResolver` for pattern matching on blocks and items.

```java
import org.xiyu.onekeyminer.registry.TagResolver;

// Get singleton instance
TagResolver resolver = TagResolver.getInstance();

// Check if block matches pattern
boolean matches = resolver.matchesBlock(blockState, "#minecraft:logs");

// Check if item matches pattern
boolean matches = resolver.matchesItem(itemStack, "#c:shears");

// Check if two blocks share any tag
boolean shareTag = resolver.blocksShareTag(state1, state2);

// Validate pattern format
boolean valid = resolver.isValidPattern("#minecraft:logs");

// Clear cache (call after config changes)
resolver.clearCache();
```

### Tag Formats

| Format | Description | Example |
|--------|-------------|---------|
| `#namespace:tag` | Tag reference | `#minecraft:logs`, `#c:ores` |
| `namespace:id` | Direct resource location | `minecraft:diamond_ore` |
| `*pattern*` | Contains match | `*ore*` |
| `pattern*` | Prefix match | `minecraft:*` |
| `*pattern` | Suffix match | `*_ore` |

---

## Platform Services

### PlatformServices Interface

OneKeyMiner uses a platform abstraction layer for cross-platform compatibility.

```java
import org.xiyu.onekeyminer.platform.PlatformServices;

// Get platform instance
PlatformServices platform = PlatformServices.getInstance();

// Get platform name
String name = platform.getPlatformName();  // "Fabric", "NeoForge", or "Forge"

// Check if mod is loaded
boolean loaded = platform.isModLoaded("mymod");

// Get config directory
Path configDir = platform.getConfigDirectory();

// Check/set chain mode for player
boolean active = platform.isChainModeActive(player);
platform.setChainModeActive(player, true);
```

### Platform-Specific Implementations

Each platform has its own implementation:

- `FabricPlatformServices` - Fabric implementation
- `NeoForgePlatformServices` - NeoForge implementation  
- `ForgePlatformServices` - Forge implementation

The correct implementation is loaded automatically via SPI (Service Provider Interface).

---

## Configuration Access

### MinerConfig Class

Access the current configuration:

```java
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.config.ConfigManager;

// Get current config
MinerConfig config = ConfigManager.getConfig();

// Access config values
boolean enabled = config.enabled;
int maxBlocks = config.maxBlocks;
int maxDistance = config.maxDistance;
boolean allowDiagonal = config.allowDiagonal;
boolean consumeDurability = config.consumeDurability;
int preserveDurability = config.preserveDurability;
boolean consumeHunger = config.consumeHunger;
int minHungerLevel = config.minHungerLevel;
boolean allowBareHand = config.allowBareHand;
boolean teleportDrops = config.teleportDrops;
boolean teleportExp = config.teleportExp;
boolean showPreview = config.showPreview;
boolean playSound = config.playSound;
```

### Runtime Config Modification

```java
// Reload config from file
ConfigManager.reload();

// Save current config to file
ConfigManager.save();
```

---

## Code Examples

### Example 1: Basic Integration

```java
package com.example.mymod;

import org.xiyu.onekeyminer.api.OneKeyMinerAPI;

public class MyModIntegration {
    
    public static void init() {
        // Check if OneKeyMiner is loaded
        if (!isModLoaded("onekeyminer")) {
            return;
        }
        
        registerContent();
    }
    
    private static void registerContent() {
        // Register custom ores
        OneKeyMinerAPI.registerBlock("mymod:mythril_ore");
        OneKeyMinerAPI.registerBlock("mymod:deepslate_mythril_ore");
        
        // Or use tag (recommended)
        OneKeyMinerAPI.registerBlockTag("#mymod:mythril_ores");
        
        // Add to same group for loose matching
        OneKeyMinerAPI.addBlockToGroup("mymod:mythril_ore", "mythril");
        OneKeyMinerAPI.addBlockToGroup("mymod:deepslate_mythril_ore", "mythril");
        
        // Register custom tools
        OneKeyMinerAPI.whitelistTool("mymod:mythril_pickaxe");
        OneKeyMinerAPI.whitelistInteractionTool("mymod:mythril_shears");
        
        // Register custom seeds
        OneKeyMinerAPI.whitelistPlantable("mymod:magic_seeds");
    }
}
```

### Example 2: Region Protection Integration

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
        
        // Check if origin is in protected area
        if (ProtectionAPI.isProtected(origin, player)) {
            event.cancel();
            player.sendSystemMessage(
                Component.literal("§cChain operations are disabled in protected areas!"));
            return;
        }
        
        // Remove protected blocks from target list
        event.getTargetPositions().removeIf(pos -> 
            ProtectionAPI.isProtected(pos, player)
        );
        
        // Limit to claim boundaries
        event.getTargetPositions().removeIf(pos ->
            !ProtectionAPI.isInSameClaim(origin, pos, player)
        );
    }
}
```

### Example 3: Statistics Tracking

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
            
            // Achievement check
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

### Example 4: Custom Tool Logic

```java
package com.example.custom;

import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.chain.ChainActionType;

public class CustomToolHandler {
    
    public static void init() {
        // Modify behavior for custom tools
        ChainEvents.registerPreActionListener(ChainActionType.MINING, event -> {
            var player = event.getPlayer();
            var item = player.getMainHandItem();
            
            // Check for enchantment or NBT
            if (item.is(MyItems.MINING_LASER)) {
                // Mining laser has no block limit
                // (handled by custom logic)
            }
            
            // Check for special modifier
            if (item.getEnchantmentLevel(MyEnchantments.EXCAVATION) > 0) {
                // Double the targets
                int level = item.getEnchantmentLevel(MyEnchantments.EXCAVATION);
                // Custom expansion logic...
            }
        });
        
        ChainEvents.registerPostActionListener(ChainActionType.MINING, event -> {
            var item = event.getPlayer().getMainHandItem();
            
            // Trigger special effects
            if (item.is(MyItems.FORTUNE_AMPLIFIER)) {
                // Apply bonus drops
                applyFortune(event.getResult().successPositions(), event.getPlayer());
            }
        });
    }
}
```

### Example 5: Addon Development

Example structure for a OneKeyMiner addon mod:

```java
package com.example.okm_addon;

import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.api.event.PreActionEvent;
import org.xiyu.onekeyminer.chain.ChainActionType;

public class OKMAddonMain {
    
    public static void init() {
        // Register addon blocks/tools
        registerContent();
        
        // Setup event listeners
        setupEventListeners();
        
        // Optional: Setup client-side rendering
        if (isClientSide()) {
            OKMAddonClient.init();
        }
    }
    
    private static void registerContent() {
        // Register addon's custom ores
        OneKeyMinerAPI.registerBlockTag("#okm_addon:custom_ores");
        
        // Register addon's custom tools
        OneKeyMinerAPI.whitelistTool("#okm_addon:enhanced_pickaxes");
    }
    
    private static void setupEventListeners() {
        // Pre-action: Add visual preview
        ChainEvents.registerPreActionListener(event -> {
            if (event.getActionType() == ChainActionType.MINING) {
                // Could send packet to client for preview rendering
                PreviewRenderer.showPreview(
                    event.getPlayer(), 
                    event.getTargetPositions()
                );
            }
        });
        
        // Post-action: Custom sound/particle effects
        ChainEvents.registerPostActionListener(event -> {
            if (event.getResult().totalCount() > 10) {
                playChainMiningSound(event.getPlayer(), event.getOriginPos());
                spawnChainMiningParticles(event.getResult().successPositions());
            }
        });
    }
}
```

---

## Class Reference

### OneKeyMinerAPI

| Method | Description |
|--------|-------------|
| `registerBlock(String)` | Register block to whitelist by ID |
| `registerBlock(Block)` | Register block instance to whitelist |
| `registerBlockTag(String)` | Register block tag to whitelist |
| `unregisterBlock(String)` | Remove block from whitelist |
| `blacklistBlock(String)` | Add block to blacklist |
| `blacklistBlock(Block)` | Add block instance to blacklist |
| `unblacklistBlock(String)` | Remove block from blacklist |
| `whitelistTool(String)` | Add tool to mining whitelist |
| `blacklistTool(String)` | Add tool to mining blacklist |
| `whitelistInteractionTool(String)` | Add tool to interaction whitelist |
| `blacklistInteractionTool(String)` | Add tool to interaction blacklist |
| `whitelistPlantable(String)` | Add item to plantable whitelist |
| `blacklistPlantable(String)` | Add item to plantable blacklist |
| `addBlockToGroup(String, String)` | Add block to group |
| `areBlocksInSameGroup(Block, Block)` | Check if blocks are in same group |
| `blocksShareTag(BlockState, BlockState)` | Check if blocks share any tag |
| `isBlockAllowed(Block)` | Check if block is allowed |
| `isBlockBlacklisted(Block)` | Check if block is blacklisted |
| `isToolAllowed(ItemStack)` | Check if tool is allowed for mining |
| `isInteractionToolAllowed(ItemStack)` | Check if tool is allowed for interaction |
| `isPlantableAllowed(ItemStack)` | Check if item is plantable |

### ChainActionContext.Builder

| Method | Description |
|--------|-------------|
| `player(ServerPlayer)` | Set the player |
| `level(Level)` | Set the world |
| `originPos(BlockPos)` | Set origin position |
| `originState(BlockState)` | Set origin block state |
| `actionType(ChainActionType)` | Set action type |
| `heldItem(ItemStack)` | Set held item |
| `hand(InteractionHand)` | Set interaction hand |
| `config(MinerConfig)` | Set custom config (optional) |
| `build()` | Build the context |

### ChainActionResult

| Method | Description |
|--------|-------------|
| `actionType()` | Get action type |
| `successPositions()` | Get set of processed positions |
| `totalCount()` | Get total blocks processed |
| `durabilityUsed()` | Get durability consumed |
| `hungerUsed()` | Get hunger consumed |
| `stopReason()` | Get stop reason |
| `isSuccess()` | Check if at least one block was processed |
| `getSummary()` | Get summary string |

---

## Best Practices

### 1. Check Mod Presence

Always check if OneKeyMiner is loaded before calling its API:

```java
if (Platform.isModLoaded("onekeyminer")) {
    // Safe to call OneKeyMiner API
}
```

### 2. Use Tags Over Individual Blocks

Prefer registering block tags over individual blocks for better compatibility:

```java
// Good - works with mod compat
OneKeyMinerAPI.registerBlockTag("#c:ores");

// Less ideal - only works for vanilla
OneKeyMinerAPI.registerBlock("minecraft:diamond_ore");
```

### 3. Respect Event Cancellation

When handling PreActionEvent, respect if another mod has already cancelled:

```java
ChainEvents.registerPreActionListener(event -> {
    if (event.isCancelled()) return; // Respect other mods
    
    // Your logic here
});
```

### 4. Thread Safety

API methods are thread-safe, but be careful with your own data structures:

```java
// Use ConcurrentHashMap for thread-safe storage
private static final Map<UUID, Data> playerData = new ConcurrentHashMap<>();
```

### 5. Minimize Event Handler Work

Keep event handlers lightweight - defer heavy work:

```java
ChainEvents.registerPostActionListener(event -> {
    // Don't do heavy work here
    // Instead, queue it for later
    scheduler.schedule(() -> processStats(event));
});
```

---

## FAQ

### Q: How do I make my custom ore work with chain mining?

A: Either add your block to a supported tag (`#c:ores`) in your data files, or call `OneKeyMinerAPI.registerBlock()` during mod initialization.

### Q: Can I modify which blocks get chain-mined during an operation?

A: Yes, use `PreActionEvent.getTargetPositions()` to modify the target set.

### Q: Why isn't my block being chain-mined?

A: Check:
1. Is the block registered via API or tag?
2. Is the block in the blacklist?
3. Is the tool valid for mining?
4. Is the player holding the activation key?

### Q: Can I trigger chain mining programmatically?

A: Yes, create a `ChainActionContext` and call `ChainActionLogic.execute()`.

### Q: How do I add preview highlighting for chain mining?

A: Listen to `PreActionEvent`, get the target positions, and send them to your client-side renderer. See Example 5 above.

---

## Support

- **GitHub Issues**: https://github.com/Mai-xiyu/OneKeyMiner/issues
- **Source Code**: https://github.com/Mai-xiyu/OneKeyMiner

---

*Documentation Version: 2.0.0 | Last Updated: January 2026*

---

## License

This project is licensed under **All Rights Reserved (ARR)**. You may not copy, modify, or distribute the code without permission from the author.
