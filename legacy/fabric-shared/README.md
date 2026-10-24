# Legacy Fabric adapters

These Fabric Loom adapters target Minecraft 1.16.5, 1.18.1 and
1.18.2. They use Yarn mappings and the official Fabric artifacts verified from
`https://meta.fabricmc.net/v2/versions/yarn/` and Fabric's Maven repository.

| Minecraft | Fabric API | Yarn | Runtime Java |
| --- | --- | --- | --- |
| 1.16.5 | 0.42.0+1.16 | 1.16.5+build.10 | 8+ |
| 1.18.1 | 0.46.6+1.18 | 1.18.1+build.22 | 17+ |
| 1.18.2 | 0.77.0+1.18.2 | 1.18.2+build.4 | 17+ |

All use Fabric Loader 0.15.11+, Loom 1.7.4 and Gradle 8.8. Build all targets
with JDK 17; the 1.16.5 compiler emits Java 8 bytecode. Install the matching
Fabric JAR and Fabric API on both client and server.

From the repository root run `./gradlew -Pmc_ver=1.18.2 :fabric:build` (or
`build` for both Fabric and Forge). Production remapped JARs and sources are
collected in `fabric/build/libs`. The internal target projects remain directly
buildable with JDK 17 for debugging. The projects share
Java 8 source and bounded BFS with legacy Forge. The build substitutes only
the renamed screen/widget methods for 1.18; handwritten sources remain shared.

Hold backtick to activate connected mining. Server execution uses the vanilla
interaction manager and Fabric break callbacks, checks tool survival, hunger,
loaded chunks and the block blacklist, and guards against recursion. Successful
axe/hoe/shovel/shears block interactions and seed placement on farmland or soul
sand can chain horizontally. Configuration is in `config/onekeyminer.json`.
The server supplies preview search limits; the client supplies only key state.
Key state expires after 100 server ticks without refresh (about five seconds
at 20 TPS) and clears on disconnect.

Press K to open the HUD settings screen. Done persists; Cancel/Escape discards
the draft. The panel supports visibility, X/Y, scale, background alpha, three
information lines and reset. Position buttons move in eight-pixel steps;
arbitrary coordinates can be set with JSON or the API. Zero alpha removes the
background, and hiding all three information lines hides the panel.

## API

Compile against the production JAR for the exact Minecraft version. The legacy
Fabric adapter exposes this HUD/preview API; the broader modern registration,
shape, harvesting and teleport APIs are not binary-compatible with it.

```java
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;

OneKeyMinerAPI.setPreviewHudSettings(new OneKeyMinerAPI.PreviewHudSettings(
    true, 6, 6, 1.0f, 128, true, true, true));
OneKeyMinerAPI.PreviewHudSettings settings = OneKeyMinerAPI.getPreviewHudSettings();
OneKeyMinerAPI.resetPreviewHudSettings();
OneKeyMinerAPI.addPreviewListener(blocks -> System.out.println(blocks.size()));
```

Settings and preview lists are immutable snapshots. Keep the listener reference
to unregister with `removePreviewListener`. Call setters on the client thread;
dedicated-server calls only modify that process's local settings. Search is
O(V) time and space with a visited bound; HUD rendering is O(1) per frame.

All three production JARs compile and package, and their JUnit suites pass.
Dedicated-server launches reach the EULA check without mod-loading errors;
1.16.5 launches on Java 8, and 1.18.x on Java 17. These fresh test directories
do not accept the EULA automatically, so world startup and gameplay are not
claimed as verified.

Compilation and JUnit coverage do not establish client rendering correctness
or compatibility with every protection mod. Manual client interaction and
third-party protection tests remain necessary before publishing a release.
