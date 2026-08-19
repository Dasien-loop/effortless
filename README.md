![Logo](assets/logo.png)

# Effortless Structure

Unofficial maintenance port of [Effortless Structure](https://github.com/huskuraft/effortless), a Minecraft building mod for placing, breaking, copying, and transforming block structures.

[中文说明](README_zh-CN.md)

## Supported Targets

Install the matching jar on both the client and the server. Each Minecraft and loader target has its own jar.

| Artifact | Loader | Minecraft | Java |
|---|---|---|---|
| `effortless-forge-1.20.1-3.4.0.jar` | Forge | 1.20.1 | 17 |
| `effortless-neoforge-1.21.1-3.4.0.jar` | NeoForge | 1.21.1 | 21 |
| `effortless-neoforge-26.1.2-3.4.0.jar` | NeoForge | 26.1.2 | 25 |

This port intentionally supports only the targets listed above. It uses standard ForgeGradle and NeoForge ModDevGradle modules and includes the required loader-neutral API source in this repository; it does not require the upstream multi-version Gradle plugin or artifact repository.

## Features

- Building modes for single blocks, lines, walls, floors, cubes, circles, cylinders, spheres, pyramids, cones, and diagonal variants.
- Block placement, bulk breaking, replace modes, patterns, clipboard copy/paste, undo, and redo.
- Server-authoritative material checks and operations.
- Optional network-material integrations for AE2, Beyond Dimensions, Refined Storage, Sophisticated Backpacks, and ProjectE where supported. Materials can be sourced from a connected network or container as well as the player's inventory.
- Wireless AE2 and Refined Storage terminals can be read from supported Curios accessory slots.
- Stable network-material preview: when the server confirms sufficient network materials, the preview and material panel no longer report a local-inventory shortage.
- Cached previews and throttled server preview checks to reduce stutter for large selections.

## Controls

- Hold `Left Alt` to open the build-mode radial menu.
- Use `Attack/Destroy` to start a bulk-break selection.
- Use `Use Item/Place Block` to start a placement or interaction selection.
- Press `[` for undo and `]` for redo.

All supported targets use the same server-side `use proper tools only` setting for bulk breaking.

## Optional Integrations

All integrations are optional. Effortless Structure launches without them, but matching mods must be installed on both client and server when their network or container materials are used.

| Integration | Supported targets | Additional notes |
|---|---|---|
| Applied Energistics 2 | All three | AE2WTLib is needed for AE2 wireless terminals; Curios is needed when a terminal is stored in an accessory slot. |
| Beyond Dimensions | All three | Provides network-material lookup and consumption. |
| Refined Storage | All three | Use the matching Refined Storage wireless-terminal/addon modules for the target version; Curios Integration enables accessory-slot terminals. |
| Sophisticated Backpacks | All three | Reads materials from accessible backpacks. |
| ProjectE | Forge 1.20.1 and NeoForge 1.21.1 | EMC conversion is optional and is not enabled for NeoForge 26.1.2. |

AE2 versions may also require their own dependencies, such as GuideME. Optional integrations never become hard dependencies of Effortless Structure.

## Credits And Attribution

This project is an unofficial maintenance port. It is not affiliated with or endorsed by the upstream authors.

- **Huskcasaca**: original Effortless Structure author and upstream project maintainer.
- **Requioss**: author of [Effortless Building](https://www.curseforge.com/minecraft/mc-mods/effortless-building), the original inspiration credited by upstream.
- **loehnertj**: credited by upstream for the 1.20.2 port.
- **Dasien**: maintainer of this Forge 1.20.1 / NeoForge 1.21.1 / NeoForge 26.1.2 port.

All upstream attribution notices are retained. See [CHANGELOG.md](CHANGELOG.md) or the [Chinese changelog](CHANGELOG_zh-CN.md) for maintenance-port changes.

## License

The project remains licensed under [LGPLv3](LICENSE). Upstream copyright notices, attribution, and license obligations continue to apply to upstream-derived code and assets.
