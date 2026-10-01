# OneKeyMiner legacy Forge builds

This directory contains isolated ForgeGradle projects for the required legacy
Minecraft versions. The projects intentionally do not share a Gradle build:
each target has its own mappings, ForgeGradle generation tasks, run directory,
and Java requirement. The version-neutral traversal code is shared by adding
`legacy/shared/src/main/java` to each project's main source set.

## Targets

| Minecraft | Forge | Project | Java | Gradle wrapper |
| --- | --- | --- | --- | --- |
| 1.18.1 | 39.0.45 | `forge-1.18.1` | 17 | 8.8 |
| 1.18.2 | 40.3.12 | `forge-1.18.2` | 17 | 8.8 |
| 1.16.5 | 36.2.40 | `forge-1.16.5` | 17 | 8.8 |
| 1.12.2 | 14.23.5.2864 | `forge-1.12.2` | 8 | 4.9 |

The Java 17 requirement for the 1.16.5 project is a build-toolchain choice in
this repository; the generated mod targets the legacy Forge runtime. The
1.12.2 project must use Java 8 because ForgeGradle 3 and Gradle 4.9 are not
compatible with newer JDKs.

## Build

Run each command from the corresponding project directory:

```powershell
Set-Location legacy/shared
.\gradlew.bat test --no-daemon

Set-Location ../forge-1.18.1
.\gradlew.bat build --no-daemon

Set-Location ../forge-1.18.2
.\gradlew.bat build --no-daemon
.\gradlew.bat runGameTestServer --no-daemon

Set-Location ../forge-1.16.5
.\gradlew.bat build --no-daemon

Set-Location ../forge-1.12.2
# Set JAVA_HOME to a JDK 8 installation before this command.
.\gradlew.bat build --no-daemon
```

The reobfuscated artifacts are written to each project's `build/libs`:

```text
legacy/forge-1.18.1/build/libs/onekeyminer-1.5.8-1.18.1.jar
legacy/forge-1.18.2/build/libs/onekeyminer-1.5.8-1.18.2.jar
legacy/forge-1.16.5/build/libs/onekeyminer-1.5.8-1.16.5.jar
legacy/forge-1.12.2/build/libs/onekeyminer-1.5.8.jar
```

`run/`, `.gradle/`, and `build/` are development output and are ignored by
the repository. Do not copy those directories into a mod distribution.

## Verification

The shared JUnit suite covers bounded BFS traversal, diagonal connectivity,
search-distance bounds, negative coordinates, duplicate suppression, and invalid
limits. Forge 1.18.2 has a dedicated GameTest server; its suite passes two
required tests, including a FakePlayer chain-mining test that removes two
connected blocks in a real server world. The other legacy targets have automated compilation/package checks
and dedicated-server startup checks. See
[`compatibility-matrix.md`](compatibility-matrix.md) for the exact scope and
remaining manual-test gap.

The root project regression checks are separate from this directory. From the
repository root, use a quoted Gradle property in PowerShell, for example:

```powershell
.\gradlew.bat '-Pmc_ver=26.3' check --no-daemon
```

The historical `1.20.1-1.6.9` tag was also checked in an isolated worktree
with its own JDK 17 toolchain.

