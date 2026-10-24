# OneKeyMiner — Minecraft 1.12.2

本分支默认构建 Minecraft **1.12.2**。旧版与现代版使用同一套根目录命令：
`mc_ver` 选择游戏版本，`build` 编译对应加载器，也可以单独指定加载器。

| Minecraft | 加载器 | 运行 Java |
| --- | --- | --- |
| 1.12.2 | Forge | 8 |
| 1.16.5 | Fabric、Forge | 8 |
| 1.18.1 | Fabric、Forge | 17 |
| 1.18.2 | Fabric、Forge | 17 |

## 构建

根 Gradle 使用 JDK 21；安装 JDK 17 和 JDK 8 供旧版子构建使用。
Gradle 自动查找已安装的工具链；路径覆盖和 CI 配置见
[旧版构建文档](legacy/README.md)。无需切换到单独工程目录。

```powershell
# 本分支默认版本及其全部加载器
.\gradlew.bat build
# 选择版本及加载器
.\gradlew.bat '-Pmc_ver=1.18.2' build
.\gradlew.bat '-Pmc_ver=1.16.5' :fabric:build
.\gradlew.bat '-Pmc_ver=1.18.1' :forge:build
```

生产 JAR 输出到 `fabric/build/libs`、`forge/build/libs`，文件名为
`onekeyminer-<loader>-<mod_version>-<mc_ver>.jar`。
`-Pmod_version=...` 同时传递给两个加载器及模组元数据。
1.12.2 仅支持 Forge；上述 Fabric 目标不适用于 1.12.2。

旧版插件依赖的 Gradle/JDK 版本不同，因此根任务通过各自固定的工具链执行，
共享算法位于 `legacy/shared`，版本适配器源码位于 `legacy/`。
构建和发布工作流均使用根入口。仓库保留的 26.3 源码可用
`-Pmc_ver=26.3`（JDK 25）显式选择，不是本分支默认构建目标。

## 功能与预览 HUD

支持连锁挖掘、工具方块交互、播种和预览。按住连锁键激活；K 打开 HUD 设置。
可配置显示、位置、缩放、背景透明度及形状/数量/上限信息行。
完成时保存，取消或 Escape 放弃修改；HUD 设置仅在本地生效。
预览数量包含准心方块；旧版搜索结果已经包含起点，使用结果数量即可。

旧版 Fabric 支持基础连锁挖掘、斧/锄/锹/剪刀方块交互和播种；
完整适配范围及依赖见 [Fabric 文档](legacy/fabric-shared/README.md)。

## API

- Forge：既有 `org.xiyu.yee.onekeyminer.api.ChainAPI` 和 `ChainPositionStore`，
  新增 HUD 设置的读取、更新、重置接口。
- Fabric：`org.xiyu.onekeyminer.api.OneKeyMinerAPI` 提供不可变 HUD 设置快照、
  预览列表及监听器注册/移除。

用法见 [旧版 API 文档](legacy/README.md)；请依赖对应版本和加载器的生产 JAR。
现代扩展 API 参考保留在 [api-reference.md](api-reference.md)，其注册接口并非
旧版 Fabric 的二进制兼容承诺。

编译及自动化测试不能替代实际游戏验证；客户端 HUD、多人行为和第三方领地
保护模组兼容性尚需游戏内验证。现有运行检查范围见
[兼容性矩阵](legacy/compatibility-matrix.md)。

## 许可

使用 Code Repository General License Agreement v1.2。
详见 [中文许可](LICENSE_CN.md) 和 [英文许可](LICENSE_EN.md)。
