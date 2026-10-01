# OneKeyMiner Forge 1.12.2

- Minecraft: 1.12.2
- Forge: 14.23.5.2864
- Java: 8
- Gradle wrapper: 4.9

Build with a JDK 8 `JAVA_HOME`:

```powershell
.\gradlew.bat build --no-daemon
```

The development-only `forgeRuntimeFix` source set works around the
ForgeGradle 3 `Side.BUKKIT` merge issue. It is not included in the published
JAR. See [`../architecture.md`](../architecture.md) and the
[compatibility matrix](../compatibility-matrix.md).

