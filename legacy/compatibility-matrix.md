# Compatibility and verification matrix

Statuses below describe the validation performed in this repository. A
server-start result proves loader/mod initialization and dedicated-server
startup; it is not a substitute for a manual client play session.

| Minecraft | Loader / version | Java | Build and package | Automated gameplay test | Startup / regression result | Manual in-game test | Known limitations |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1.18.1 | Forge 39.0.45 | 17 | `build` passed; reobfuscated JAR produced | Shared JUnit suite passed; no 1.18.1 GameTest class | Dedicated server reached `Done` and loaded `onekeyminer` | Not performed | Startup smoke only; no client play session |
| 1.18.2 | Forge 40.3.12 | 17 | `build` passed; reobfuscated JAR produced | `runGameTestServer`: 2 required tests passed, including real connected-block chain mining | GameTest server exited with code 0 after normal shutdown | Not performed | GameTest covers server-side destruction and traversal; full client display and manual drop inspection remain outstanding |
| 1.16.5 | Forge 36.2.40 | 17 | `build` passed; reobfuscated JAR produced | Shared JUnit suite passed; no practical Forge GameTest suite in this project | Dedicated server reached `Done` and loaded `onekeyminer` | Not performed | Startup smoke only; no client play session |
| 1.12.2 | Forge 14.23.5.2864 | 8 | `build` passed with `jar` and `reobfJar`; JAR produced | Shared JUnit suite passed; no Forge GameTest equivalent wired | Dedicated server reached `Done` and initialized the mod | Not performed | ForgeGradle/ASM emits legacy module-info and Maven-path warnings; see the shim note below |
| 1.20.1 (regression) | Historical root tag `1.20.1-1.6.9`, Fabric + Forge | 17 | Isolated tag worktree `check` passed | Root common/loader tests passed | `check` completed successfully | Not performed | This row validates the historical tag; the current 26.3 branch does not carry a 1.20.1 property file |
| 26.3 (regression) | Current root branch, Fabric + Forge + NeoForge | 25 | Quoted `-Pmc_ver=26.3 check` passed | All configured root tests passed | `check` completed successfully | Not performed | Gradle/JDK warning output is non-fatal |

## 1.12.2 runtime shim

The ForgeGradle 3 development merge artifact exposes a `Side.BUKKIT` marker,
while the Forge 1.12.2 `NetworkRegistry` path initializes only `CLIENT` and
`SERVER`. Iterating the merged enum caused a startup NPE before the mod could
load. `forge-1.12.2/src/forgeRuntimeFix/java/net/minecraftforge/fml/relauncher/Side.java`
provides the two-value development enum and is placed first in the dev run
classpath. The source set is not included in the published JAR; the artifact
was inspected to confirm that `Side.class` is absent.

