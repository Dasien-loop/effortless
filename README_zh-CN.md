![Logo](assets/logo.png)

# Effortless Structure

这是 [Effortless Structure](https://github.com/huskuraft/effortless) 的非官方维护移植版。模组用于在 Minecraft 中批量放置、破坏、复制和变换方块结构。

[English README](README.md)

## 支持版本

客户端和服务端都必须安装对应版本的 jar。每个 Minecraft/加载器目标均使用独立 jar。

| 文件 | 加载器 | Minecraft | Java |
|---|---|---|---|
| `effortless-forge-1.20.1-3.4.0.jar` | Forge | 1.20.1 | 17 |
| `effortless-neoforge-1.21.1-3.4.0.jar` | NeoForge | 1.21.1 | 21 |
| `effortless-neoforge-26.1.2-3.4.0.jar` | NeoForge | 26.1.2 | 25 |

本维护版仅支持上表中的三个目标版本。项目已改用标准 ForgeGradle 与 NeoForge ModDevGradle 模块，并在仓库中包含所需的跨加载器 API 源码；构建不再依赖上游作者的多版本 Gradle 插件或制品仓库。

## 功能

- 单方块、直线、墙面、地板、立方体、圆形、圆柱、球体、金字塔、圆锥及斜向结构等建造模式。
- 批量放置、批量破坏、替换模式、图案、剪贴板复制粘贴、撤销与重做。
- 服务端权威的材料检查和实际操作。
- 可选兼容 AE2、超越维度、精致存储、精妙背包以及等价交换（ProjectE，部分版本）。材料可从玩家背包、背包容器或已连接的网络中补充。
- AE2 与精致存储的无线终端支持从 Curios 饰品槽读取；终端仍通过其自身绑定信息访问网络。
- 稳定的网络材料预览：服务端确认网络材料足够后，预览和材料面板不会再因本地背包数量不足而错误提示。
- 预览缓存与服务端预览限频，降低大范围选区时的渲染和网络开销。

## 操作

- 长按 `左 Alt` 打开建造模式轮盘。
- 使用 `攻击/破坏` 开始批量破坏选区。
- 使用 `使用物品/放置方块` 开始放置或交互选区。
- 按 `[` 撤销，按 `]` 重做。

三个支持目标的批量破坏均遵从同一个服务端“仅使用正确工具”设置。

## 可选兼容

所有联动均为可选依赖。未安装时 Effortless Structure 仍可启动；使用对应功能时，客户端和服务端都必须安装匹配版本的模组及其依赖。

| 联动模组 | 支持目标 | 说明 |
|---|---|---|
| Applied Energistics 2（AE2） | 三个版本 | 无线终端需要 AE2WTLib；终端放入饰品槽时还需要 Curios。 |
| 超越维度（Beyond Dimensions） | 三个版本 | 支持从维度网络读取和消耗材料。 |
| 精致存储（Refined Storage） | 三个版本 | 按目标版本安装对应的无线终端/附属模组；饰品槽终端需要 Curios Integration。 |
| 精妙背包（Sophisticated Backpacks） | 三个版本 | 支持读取可访问背包中的材料。 |
| 等价交换（ProjectE） | Forge 1.20.1、NeoForge 1.21.1 | 支持使用 EMC 转化材料；NeoForge 26.1.2 不启用此联动。 |

AE2 的部分版本还需要按照 AE2 自身要求安装 GuideME。所有联动都不会成为 Effortless Structure 的强制依赖。

## 致谢与署名

本项目是非官方维护移植版，与上游作者不存在隶属关系，也未获得其官方背书。

- **Huskcasaca**：Effortless Structure 原作者与上游项目维护者。
- **Requioss**：上游项目致谢的 [Effortless Building](https://www.curseforge.com/minecraft/mc-mods/effortless-building) 作者与原始灵感来源。
- **loehnertj**：上游项目致谢的 1.20.2 移植贡献者。
- **Dasien**：本 Forge 1.20.1 / NeoForge 1.21.1 / NeoForge 26.1.2 维护移植版作者。

所有上游署名均已保留。维护移植版的改动请查看 [CHANGELOG.md](CHANGELOG.md) 或 [中文更新日志](CHANGELOG_zh-CN.md)。

## 许可证

项目继续使用 [LGPLv3](LICENSE)。上游代码与资源的原有版权声明、署名以及许可证义务继续有效。
