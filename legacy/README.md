# Legacy multi-loader builds

The root build selects Minecraft with `mc_ver`, just like modern branches.
Each legacy branch defaults to its own Minecraft version.

| Minecraft | Loaders | Runtime Java |
| --- | --- | --- |
| 1.12.2 | Forge | 8 |
| 1.16.5 | Fabric, Forge | 8 |
| 1.18.1 | Fabric, Forge | 17 |
| 1.18.2 | Fabric, Forge | 17 |

Run the root wrapper on JDK 21. Install JDK 17 for the child builds/shared
tests and JDK 8 for Forge 1.12.2 and the 1.16.5 MCP pipeline. Gradle discovers
local toolchains; CI also exposes `JAVA_HOME_8_X64` and `JAVA_HOME_17_X64`.
Explicit overrides are `-Plegacy_java8_home=<path>` and
`-Plegacy_java17_home=<path>`. Shared compilation/tests use Gradle's JDK 17
toolchain discovery (`org.gradle.java.installations.paths` if needed).

```powershell
# From repository root: both loaders, including checks.
.\gradlew.bat '-Pmc_ver=1.18.2' build
# Select just one loader.
.\gradlew.bat '-Pmc_ver=1.16.5' :fabric:build
.\gradlew.bat '-Pmc_ver=1.18.1' :forge:build
# 1.12.2 supports Forge only.
.\gradlew.bat '-Pmc_ver=1.12.2' build
# Existing lifecycle/development tasks.
.\gradlew.bat '-Pmc_ver=1.18.2' check
.\gradlew.bat '-Pmc_ver=1.18.2' :fabric:runClient
```

Production artifacts have the same root layout as modern builds:
`fabric/build/libs/onekeyminer-fabric-<mod_version>-<mc_ver>.jar` and
`forge/build/libs/onekeyminer-forge-<mod_version>-<mc_ver>.jar`.
Only the selected version is collected. `-Pmod_version=...` is propagated to
both loaders and embedded metadata. CI and tag releases use this root entry.

Old ForgeGradle and Loom versions cannot run together inside the modern Gradle
daemon. Root loader tasks therefore invoke each adapter's pinned wrapper and
compatible JDK in a separate process. A shared build service serializes these
processes to avoid races in game download caches. Gradle offline and explicit
HTTP/S proxy options propagate to child builds. Internal source directories
remain `legacy/forge-<version>` and `legacy/fabric-<version>`; shared Java 8
code is under `legacy/shared`. This preserves runtime compatibility at the
cost of additional Gradle startup time.

## HUD and API

Press K to edit HUD visibility, position, scale, background alpha and the
shape/count/limit lines. Done saves; Cancel/Escape discards the draft.
Forge settings persist in `config/onekeyminer-hud.properties`; Fabric stores
them in the local `config/onekeyminer.json`.

Forge exposes `org.xiyu.yee.onekeyminer.api.ChainAPI.getPreviewHudSettings()`,
`setPreviewHudSettings(LegacyHudSettings)` and `resetPreviewHudSettings()`;
the immutable value type is
`org.xiyu.onekeyminer.legacy.core.LegacyHudSettings`.
Existing `ChainAPI` chain operations and `ChainPositionStore` remain available.
Forge's server-synchronized lists and Fabric's connected search already
include the starting block; HUD count therefore uses their existing size.

Fabric exposes the immutable HUD/preview API in
[`fabric-shared/README.md`](fabric-shared/README.md), including preview listener
registration/removal. Its baseline adapter implements connected mining,
basic axe/hoe/shovel/shears block interactions and seed placement; it does not
provide binary compatibility with modern custom shape, harvesting or teleport
registration APIs. Compile against the correct version/loader production JAR.

See [`compatibility-matrix.md`](compatibility-matrix.md) for earlier Forge
runtime checks. Fabric production builds and tests pass; dedicated server
launches reach the EULA check. Client rendering, multiplayer gameplay and
third-party protection integrations still require in-game verification.
