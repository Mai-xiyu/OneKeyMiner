# OneKeyMiner 1.21.9 API Reference

This document describes the public API shipped by OneKeyMiner `1.6.8` for
Minecraft `1.21.9`.

## Supported environment

| Component | Version |
|---|---|
| Java | 21 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.134.1+1.21.9 |
| Forge | 59.0.5 |
| NeoForge | 21.9.16-beta |

OneKeyMiner does not currently publish a documented Maven repository or stable
Maven coordinate. Do not copy coordinates from old examples. Compile an add-on
against the platform JAR that it will run with.

## Local development dependency

Copy one production JAR into your add-on project's `libs/` directory:

- `onekeyminer-fabric-1.6.8-1.21.9.jar`
- `onekeyminer-forge-1.6.8-1.21.9.jar`
- `onekeyminer-neoforge-1.6.8-1.21.9.jar`

There is intentionally no Forgix/universal API artifact. Public signatures
contain Minecraft types whose runtime mappings differ by loader, so add-ons
and their users must install the matching platform JAR.

Use a compile-only dependency because the player or server installs
OneKeyMiner separately.

Fabric Loom:

```groovy
dependencies {
    modCompileOnly files("libs/onekeyminer-fabric-1.6.8-1.21.9.jar")
}
```

ForgeGradle:

```groovy
dependencies {
    compileOnly fg.deobf(files("libs/onekeyminer-forge-1.6.8-1.21.9.jar"))
}
```

NeoGradle or ModDevGradle:

```groovy
dependencies {
    compileOnly files("libs/onekeyminer-neoforge-1.6.8-1.21.9.jar")
}
```

Declare `onekeyminer` as an optional or required runtime dependency in your
loader metadata according to whether your add-on can operate without it. Never
embed the JAR as a second copy; keep it as a separate runtime mod.

## Execution and security model

The server is authoritative. It validates the player, world, loaded chunks,
tool, target, distance, count, protection result, durability, hunger and item
use before applying a chained operation. A client preference packet is only a
request; it cannot override server policy.

Call mutation and execution APIs from the logical server thread. Registration
methods use concurrent or snapshot-backed collections, but Minecraft world and
entity objects are not generally safe to access from arbitrary threads.

`ChainActionContext.Builder#skipPermissionCheck(boolean)` and
`ChainActionContext#isSkipPermissionCheck()` are deprecated compatibility
members. The flag is ignored and cannot bypass authoritative permission checks.

## Core imports

```java
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI.InteractionRule;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI.ToolActionRule;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI.ToolTargetType;
import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.api.event.PostActionEvent;
import org.xiyu.onekeyminer.api.event.PreActionEvent;
import org.xiyu.onekeyminer.chain.ChainActionContext;
import org.xiyu.onekeyminer.chain.ChainActionLogic;
import org.xiyu.onekeyminer.chain.ChainActionResult;
import org.xiyu.onekeyminer.chain.ChainActionType;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
```

## Block and tool registration

Registration methods return `true` when they changed the runtime registration.
IDs use `namespace:path`. Item and block tag selectors may use a leading `#`.
Invalid IDs return `false` rather than being registered.

```java
OneKeyMinerAPI.registerBlock("examplemod:ruby_ore");
OneKeyMinerAPI.registerBlockTag("#c:ores/ruby");
OneKeyMinerAPI.blacklistBlock("examplemod:unstable_ore");
OneKeyMinerAPI.blacklistBlockTag("#examplemod:no_chain_mining");

OneKeyMinerAPI.whitelistTool("examplemod:ruby_pickaxe");
OneKeyMinerAPI.whitelistTool("#c:tools/pickaxe");
OneKeyMinerAPI.blacklistTool("examplemod:fragile_pickaxe");

OneKeyMinerAPI.registerInteractionTool("examplemod:precision_shears");
OneKeyMinerAPI.registerInteractiveItem("examplemod:fertilizer");
OneKeyMinerAPI.registerPlantableItem("examplemod:ruby_seed");
OneKeyMinerAPI.blacklistPlantable("#examplemod:no_chain_planting");
```

Relevant removal and query methods include:

```java
OneKeyMinerAPI.unregisterBlock("examplemod:ruby_ore");
OneKeyMinerAPI.unregisterBlockTag("#c:ores/ruby");
OneKeyMinerAPI.unblacklistBlock("examplemod:unstable_ore");
OneKeyMinerAPI.unblacklistBlockTag("#examplemod:no_chain_mining");
OneKeyMinerAPI.unwhitelistTool("examplemod:ruby_pickaxe");
OneKeyMinerAPI.unblacklistTool("examplemod:fragile_pickaxe");

boolean allowedBlock = OneKeyMinerAPI.isBlockAllowed(block);
boolean deniedBlock = OneKeyMinerAPI.isBlockBlacklisted(block);
boolean allowedTool = OneKeyMinerAPI.isToolAllowed(stack);
```

Blacklists take precedence over whitelists. `getBlockWhitelist()` and
`getBlockBlacklist()` return immutable snapshots, not live mutable sets.

`addBlockToGroup(blockId, groupId)` can group different blocks for loose
matching. `areBlocksInSameGroup` and `blocksShareTag` provide the corresponding
queries.

## Exact tool-action rules

Tool-action rules bind one item ID or tag to an action and an exact block/entity
target set. A target may also be a tag. An empty target list means any target of
the declared target type.

```java
boolean registered = OneKeyMinerAPI.registerToolAction(
        "examplemod:precision_shears",
        ToolTargetType.ENTITY,
        ChainActionType.INTERACTION,
        InteractionRule.SHEARING,
        List.of("minecraft:sheep")
);
```

Convenience forms:

```java
OneKeyMinerAPI.registerMiningToolRule(
        "examplemod:vein_drill",
        "#c:ores"
);

OneKeyMinerAPI.registerInteractionToolRule(
        "examplemod:bark_knife",
        ToolTargetType.BLOCK,
        InteractionRule.STRIPPING,
        "#minecraft:logs"
);

OneKeyMinerAPI.registerEntityShearingRule(
        "examplemod:precision_shears",
        "minecraft:sheep"
);
```

The complete signature is:

```java
boolean registerToolAction(
        String toolSelector,
        ToolTargetType targetType,
        ChainActionType actionType,
        InteractionRule interactionRule,
        List<String> targets
)
```

Rules with `ToolTargetType.ENTITY` only accept
`ChainActionType.INTERACTION`. `interactionRule` is required for interaction
rules and ignored for non-interaction rules.

Use the action-filtered lookups when dispatching an operation:

```java
Optional<ToolActionRule> miningRule =
        OneKeyMinerAPI.findToolActionForBlock(
                stack,
                state,
                ChainActionType.MINING
        );

Optional<ToolActionRule> rightClickRule =
        OneKeyMinerAPI.findToolActionForBlock(
                stack,
                state,
                Set.of(
                        ChainActionType.INTERACTION,
                        ChainActionType.PLANTING,
                        ChainActionType.HARVESTING
                )
        );

Optional<ToolActionRule> entityRule =
        OneKeyMinerAPI.findToolActionForEntity(
                stack,
                entity,
                ChainActionType.INTERACTION
        );
```

The unfiltered lookup overloads remain binary/source compatible but are
deprecated because an earlier rule for another action can otherwise shade the
intended rule. `getToolActionRules()` returns an immutable snapshot.
`unregisterToolAction` removes a rule.

Planting rules match the clicked support block; harvesting rules match the
actual mature crop. Once a rule is selected on the origin, every derived target
must match that same exact rule (including its target IDs/tags).

Vanilla block/item and entity interactions are observed after their loader
event permits the action. Forge and NeoForge cancellation results therefore
cannot authorize derived targets. Exact-position entity interaction is covered
on all three loaders.

Fabric callbacks that return a non-`PASS` result stop vanilla before that
observation point. If an add-on implements the original action inside such a
consuming callback, wrap that action explicitly:

```java
InteractionResult result = OneKeyMinerAPI.executeBlockUseWithChain(
        serverPlayer,
        hand,
        hitResult,
        () -> performCustomBlockAction(serverPlayer, hand, hitResult)
);

InteractionResult entityResult = OneKeyMinerAPI.executeEntityUseWithChain(
        serverPlayer,
        target,
        hand,
        () -> performCustomEntityAction(serverPlayer, target, hand)
);
```

The supplier is executed exactly once. It must return a consuming result only
when its original action actually succeeded. Do not use these wrappers in a
callback that returns `PASS` and lets vanilla continue, because that would run
the original action twice.

Native hoe, axe and shovel actions, every planting rule, and shearing on a
ready `Shearable` also verify the expected world/entity transition. Exact
custom interaction rules that do not describe one of those stateful actions,
plus generic API whitelist entries, use the consuming result as their explicit
completion contract. The live item in the hand must still be the same item
type as the captured action.

For native shearing, the same entity must remain present and transition from
ready to not ready. Vanilla mooshroom-to-cow conversion is the only built-in
replacement exception, and it must produce a new live cow at the original
position. Arbitrary removal, death, dimension transfer, or replacement does
not authorize derived targets.

`InteractionRule.BRUSHING` remains available for atomic custom item rules.
Vanilla brushing is a sustained-use action rather than one completed
`ItemStack.useOn` call, so it is not treated as a native atomic chain action.

## Events

Listeners are registered as `Consumer<PreActionEvent>` or
`Consumer<PostActionEvent>`. Registration returns `void`, so retain the same
consumer instance if it must later be unregistered.

```java
Consumer<PreActionEvent> limiter = event -> {
    if (event.getActionType() == ChainActionType.MINING
            && event.getTargetCount() > 32) {
        event.setTargetPositions(
                List.copyOf(event.getTargetPositions().subList(0, 32))
        );
    }
};

ChainEvents.registerPreActionListener(limiter);
// Later:
ChainEvents.unregisterPreActionListener(limiter);
```

Type-filtered and predicate-filtered overloads are also available:

```java
ChainEvents.registerPreActionListener(
        ChainActionType.INTERACTION,
        event -> {
            if (!event.getPlayer().isCreative()) {
                event.cancel("examplemod policy");
            }
        }
);

ChainEvents.registerPostActionListener(
        event -> event.getActionType() == ChainActionType.MINING,
        event -> {
            int minedCount = event.getTotalCount();
            // Record minedCount in the add-on's own state.
        }
);
```

`PreActionEvent` exposes a mutable target list and can be cancelled. Targets
added by a listener are not trusted: the server revalidates them before use.
Do not remove the original target if derived targets should proceed: it is the
authorization token for the entire operation.

For block interaction, planting, and entity shearing, the server first lets the
original loader/vanilla action complete. `PreActionEvent` then controls only
the derived targets and cannot roll back that completed original action.
Harvesting is different because ordinary right-click usually performs no
vanilla harvest: its pre-event still runs before the origin is harvested.
Every physical loader event is emitted once per actual target.

Pre-listener dispatch is fail-closed. If a filter or pre-listener throws, the
exception is logged, the event is cancelled and remaining pre-listeners are
not invoked. Post-listener exceptions are logged and do not roll back an
already completed operation.

`PreActionEvent#getTool()` returns a copy. `PostActionEvent` and
`ChainActionResult` expose immutable/copy-backed result data.

## Custom shapes

A shape supplies neighboring candidate positions. It must not mutate the
world. The executor still performs authoritative validation on every returned
position.

```java
public final class UpwardColumnShape implements ChainShape {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("examplemod", "upward_column");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "shape.examplemod.upward_column";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        for (int offset = 1;
                offset <= context.getMaxDistance()
                        && result.size() < context.getMaxBlocks();
                offset++) {
            BlockPos pos = context.getOriginPos().above(offset);
            if (!context.getLevel().hasChunkAt(pos)) {
                break;
            }
            if (!context.isMatchingBlock(context.getLevel().getBlockState(pos))) {
                break;
            }
            result.add(pos.immutable());
        }
        return List.copyOf(result);
    }
}
```

Register the shape during common add-on initialization:

```java
OneKeyMinerAPI.registerShape(new UpwardColumnShape());
```

The required interface is:

```java
public interface ChainShape {
    ResourceLocation getId();
    String getTranslationKey();
    List<BlockPos> collectBlocks(ShapeContext context);

    default List<BlockPos> getPreviewPositions(ShapeContext context);
    default boolean requiresDirection();
}
```

Shape IDs are limited to 128 characters, including the namespace and colon.
Registration replaces an existing shape with the same ID. The built-in default
shape cannot be unregistered, and the currently selected shape cannot be
unregistered through `OneKeyMinerAPI`.

Useful methods:

```java
OneKeyMinerAPI.getShape(id);
OneKeyMinerAPI.getRegisteredShapes();
OneKeyMinerAPI.isShapeRegistered("examplemod:upward_column");
OneKeyMinerAPI.setSelectedShape("examplemod:upward_column");
OneKeyMinerAPI.unregisterShape(UpwardColumnShape.ID);
```

`ShapeContext` clamps `maxBlocks` to `1..10240` and `maxDistance` to `1..128`.
Respect both limits and use `Level#hasChunkAt` before reading a candidate so a
shape does not synchronously load chunks.

## Constructing and executing a context

The factory methods directly return a finished `ChainActionContext`; do not
call builder methods or `build()` on their result.

```java
ChainActionContext context =
        ChainActionContext.forMining(player, level, pos, state);
ChainActionResult result = ChainActionLogic.execute(context);
```

For overrides, construct the context with the builder itself:

```java
ChainActionContext context = ChainActionContext.builder()
        .player(player)
        .level(level)
        .originPos(pos)
        .originState(level.getBlockState(pos))
        .actionType(ChainActionType.MINING)
        .heldItem(player.getMainHandItem())
        .hand(InteractionHand.MAIN_HAND)
        .maxCount(32)
        .maxDistance(12)
        .allowDiagonal(false)
        .build();

ChainActionResult result = ChainActionLogic.execute(context);
```

The player must be a `ServerPlayer`, and `level` must be that player's current
server level. The context copies the held item and origin position. Positive
context overrides are clamped to 10240 targets and 128 blocks; non-positive
values request configured limits.

Available factories:

```java
ChainActionContext.forMining(player, level, pos, state);
ChainActionContext.forInteraction(player, level, pos, state, hand);
ChainActionContext.forPlanting(player, level, pos, hand);
ChainActionContext.forHarvesting(player, level, pos, hand);
```

Loader event integrations should preserve the original target identity and
click geometry using `originEntityId(...)` and `blockHitResult(...)` where
applicable. Add-ons should normally register rules/events instead of replacing
the built-in loader handlers or invoking another operation from inside a chain
event.

## Configuration API

`ConfigManager#getConfig()` and `getConfigSnapshot()` return defensive copies.
Changing fields on those objects does not change active configuration.

Networking and UI code that only needs the selected shape and teleport
preferences should use `ConfigManager#getClientPreferencesSnapshot()`. It
returns an immutable, constant-size snapshot without copying the complete
configuration.

Remote-client config screens must persist only those preferences through
`ConfigManager#updateClientPreferences(MinerConfig)`. The method copies the
selected shape and the two teleport requests while preserving every
server-authoritative gameplay and policy field.

The built-in receivers accept a bounded four-packet burst per player per server
tick and sample invalid-input warnings. Clients retry one immutable snapshot
until the server acknowledges its sequence, then refresh policy every 600
ticks. Add-ons should update configuration through the API rather than sending
additional protocol packets.

For an atomic update, use:

```java
ConfigManager.editConfig("maxBlocks", config -> config.maxBlocks = 96);
```

This edits the latest snapshot, validates and saves it, notifies listeners and
triggers synchronization. It avoids a lost update caused by modifying an old
copy and passing it to `updateConfig`.

For a complete replacement:

```java
MinerConfig copy = ConfigManager.getConfig();
copy.maxBlocks = 96;
ConfigManager.updateConfig(copy, "maxBlocks");
```

Prefer `editConfig` for one or a few fields. `OneKeyMinerAPI` also provides
validated convenience methods:

```java
OneKeyMinerAPI.setSelectedShape("examplemod:upward_column");
OneKeyMinerAPI.setLocalDropTeleportRequested(true);
OneKeyMinerAPI.setLocalExperienceTeleportRequested(true);

// Run these only in an authoritative server configuration context.
OneKeyMinerAPI.setClientDropTeleportAllowed(true);
OneKeyMinerAPI.setClientExperienceTeleportAllowed(true);
```

On a dedicated server, these calls must run in the correct server-side policy
context. Do not present a client-only setting mutation as a way to change server
limits. `MinerConfig#allowClientTeleportDrops` and
`MinerConfig#allowClientTeleportExp` are authoritative server policy gates; a
client request is effective only when the corresponding gate is enabled.
Use `isDropTeleportEffective(player)` or
`isExperienceTeleportEffective(player)` when an add-on needs the resolved
per-player result. The legacy `setTeleport*Enabled` methods now explicitly
delegate to local-preference setters and are deprecated.

On a physical client,
`OneKeyMinerAPI.getAcknowledgedServerPreferences()` returns the latest applied
shape, preview bounds, teleport results, and capability mask. It returns empty
before acknowledgement, after disconnect, and on a dedicated server.

## Compatibility notes

- API examples in this document target only Minecraft 1.21.9 / OneKeyMiner
  1.6.8. Other branches must be compiled against their own platform JAR.
- A production add-on must declare its supported OneKeyMiner and Minecraft
  versions in loader metadata.
- Do not depend on `org.xiyu.onekeyminer.platform.*` implementations. They are
  internal loader bridges, not stable add-on API.
- Runtime registrations last for the life of the game process unless explicitly
  removed. `OneKeyMinerAPI.clearAll()` is primarily useful for tests and removes
  add-on registrations too.
- Algorithms in custom shapes should be bounded by `maxBlocks`, `maxDistance`
  and loaded chunks. A breadth-first shape is normally `O(V + E)` time and
  `O(V)` memory for visited candidates.
