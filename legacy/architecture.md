# Legacy architecture

## Boundary between pure logic and Minecraft APIs

```text
Forge event/config/network/client adapters
        |
        v
version-specific ChainHandler and handlers
        |
        |  convert BlockPos/IBlockState and read config/world state
        v
legacy/shared LegacyPosition + LegacyChainSearch
        |
        v
bounded, deterministic connected-component traversal
```

`legacy/shared` contains no Minecraft or Forge classes. `LegacyPosition` is an
immutable integer coordinate, and `LegacyChainSearch` accepts a version-owned
matcher callback. The matcher is evaluated for a candidate coordinate only;
non-matching blocks terminate traversal in that direction. Positions are
marked when queued, so diagonal neighborhoods cannot enqueue duplicates.

The traversal supports cardinal or all-26-neighbor connectivity, a
breadth-first search-distance bound, a maximum result count, and an explicit
visited-node budget. Its time and space complexity are O(V), where V is the
visited-node bound. The default visited budget is bounded relative to the
configured block limit (`max(64, maxBlocks * 8)`); adapters can use the
overload with an explicit budget when a stricter server tick budget is needed.

## Version adapters

The four Forge projects retain their own event, networking, configuration, and
client code because the APIs and mappings are materially different:

- 1.18.1 and 1.18.2 use the Mojang/Parchment-style `BlockPos`, `BlockState`,
  `ServerPlayer`, and `ServerLevel` APIs. Their source trees are intentionally
  separate so a patch for one Forge line cannot silently change the other.
- 1.16.5 uses the corresponding Forge 36 APIs and `ServerPlayerEntity` /
  `ServerWorld` names in its selected mappings.
- 1.12.2 uses MCP snapshot mappings and the older `EntityPlayerMP`,
  `WorldServer`, and `IBlockState` APIs. Its network and configuration code is
  isolated rather than adding version checks to the shared traversal.

Each adapter converts its native coordinates to `LegacyPosition`, invokes the
shared search with a state matcher, then converts the result back before doing
world mutation, loot, experience, durability, and packet work. This keeps the
algorithm testable without a Minecraft runtime while leaving side effects in
the loader-specific layer.

When a chain action succeeds, the Forge `BlockEvent.BreakEvent` is cancelled so
the origin is not processed again by the normal break pipeline. The 1.18.x and
1.16.5 adapters explicitly reproduce loot, experience, and tool durability;
1.12.2 uses its older `World#destroyBlock` API and therefore supplies the
corresponding experience and durability work explicitly.

## 1.18.2 GameTest resource handling

Forge 40.3.12's userdev GameTest run reads the legacy template from the run
directory in this setup. `prepareGameTestStructures` copies
`src/main/resources/gameteststructures/legacygametests.empty3x3x3.snbt` to
`run/gameteststructures` before `runGameTestServer`. The resource remains in
the mod JAR as well.

## 1.12.2 ForgeGradle shim

ForgeGradle 3 merges a development Forge artifact whose `Side` enum includes
`BUKKIT`, but the universal Forge 1.12.2 runtime only has `CLIENT` and
`SERVER`. Forge's `NetworkRegistry` assumes the latter shape. The
development-only `forgeRuntimeFix` source set shadows the enum for run tasks,
without shipping Forge classes in the mod artifact. This is a build/runtime
compatibility workaround, not a gameplay dependency.

## 1.7.10 feasibility decision

1.7.10 is not implemented. Supporting it would require a separate
ForgeGradle 1.x build and MCP mapping pipeline, Java/Gradle constraints,
different event and networking APIs, and a separate client/server test setup.
The current adapters and shared source would provide little usable
infrastructure beyond the small BFS class, while adding another independently
maintained toolchain. That cost is disproportionate to the optional target and
would increase risk for the four required versions. The required versions do
not depend on a 1.7.10 compatibility branch.

## Test boundary

The shared tests provide repeatable traversal coverage. The 1.18.2 GameTest
proves template loading and runs both the shared traversal and a real
server-side chain-mining operation with a Forge `FakePlayer`. Dedicated-server
startup checks cover Forge discovery, config registration, network registration,
and mod initialization for the remaining targets. A real client session
exercising chained drops, tool durability, planting, and interaction remains a
manual test item and is therefore marked as not performed in the matrix.

