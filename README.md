# Client Commands for NeoForge (Minecraft 26.3)

An unofficial NeoForge port of [ClientCommands](https://github.com/Earthcomputer/clientcommands)
by Earthcomputer, for **Minecraft 26.3** and **NeoForge 26.3**.

Adds several useful client-side commands to Minecraft. All commands and features are the work of the
original project; this repository only contains the port to the NeoForge loader.

**This is a private, fan-made project.** It exists for hobbyists who want to use ClientCommands on
NeoForge, and for no other purpose.

## Social

- Original project: https://github.com/Earthcomputer/clientcommands
- Upstream Discord: https://discord.gg/Jg7Bun7
- Upstream Patreon: https://www.patreon.com/earthcomputer

## Installation

1. Install [NeoForge for Minecraft 26.3](https://neoforged.net/) (developed against
   `26.3.0.51-beta`, tested on `26.3.0.46-beta`).
2. Download the mod jar from this repository's
   [releases](https://github.com/clature76-source/clientcommands-mod-26.3-NeoForge-version/releases)
   or build it yourself, and move it to the mods folder (`.minecraft/mods`).

Nothing else needs to be installed. **Fabric API is not required**: the Forgified Fabric API
modules and the Fabric-only libraries this mod uses are jar-in-jar bundled inside the mod jar.

## Requirements

- Java 25
- Minecraft 26.3 with NeoForge 26.3

## Building

```
./gradlew clean jar
```

`clean` is not optional — see "Rebuilding after a build.gradle change" below.

The mod jar is written to `build/libs/clientcommands-<version>.jar`.

## Disclaimer

- **Private project, hobby use only.** This port is made by a fan, for fans. It is not affiliated
  with, endorsed by, or supported by Earthcomputer or the ClientCommands project.
- **No commercial use is intended or supported by this port.** This port is published for personal
  and hobby use. The author of this port does not grant, and cannot grant, any commercial licence
  to the original work.
- **The author of this port accepts no liability** for any damage, data loss, ban, or legal
  consequence arising from the use of this port. Use it at your own risk.
- **No warranty** of any kind is provided. See the licence below.

## Licence

Licensed under **LGPL-3.0-or-later**, the same licence as the original project (see `LICENSE`).

The original work is Copyright (c) 2021 Earthcomputer. This port is a derivative work and is
distributed under the same terms. If you redistribute this port, keep the licence and copyright
notice intact.

> On the disclaimer above: LGPL-3.0-or-later permits commercial use of the licensed work, and a
> derivative work cannot add a restriction the licence forbids. The "no commercial use" statement
> therefore describes the intent and support scope of *this port*, not a legal term of the licence —
> the licence itself governs what you may do with the code.

## How the port works

This is not a rewrite: the original sources are kept as close to upstream as possible, and the
loader-specific layers are adapted around them.

- A NeoForge `@Mod` entry point (`ClientCommands`) mirrors Fabric's `ClientModInitializer`, and
  invokes the Fabric entry points of the embedded Fabric-only libraries, in the order the Fabric
  loader would use.
- Fabric API surface that has a working NeoForge port is used as-is and bundled with `jarJar`
  (Forgified Fabric API: `fabric-api-base`, `fabric-command-api-v2`, plus the Forgified Fabric
  Loader). Keeping them as nested jars is required: each is itself a NeoForge mod with its own
  `META-INF/neoforge.mods.toml`, so unpacking their classes while dropping that metadata makes
  NeoForge abort startup with "contains mod entrypoint class ... which does not exist".
- The parts of the Fabric API that cannot run on 26.3 (client lifecycle events, level rendering
  events, render state data) are implemented natively on top of the corresponding NeoForge events.
  Those implementations live under `net.fabricmc.fabric.api.client.*` so the original sources keep
  compiling unchanged.
- Command registration is bridged: the original commands are built on a Brigadier tree rooted at
  `FabricClientCommandSource`, and `compat/CommandTreeBridge` rebuilds that tree onto NeoForge's
  vanilla `CommandSourceStack` dispatcher, adapting the source at execution time. The parsed
  argument map is carried across the rebuild, otherwise every command that reads an argument fails
  with "No such argument ... exists on this command".
- The embedded libraries' entry points are invoked exactly once. `RegisterClientCommandsEvent`
  fires again for every command tree the server sends, and `ModConfigBuilder.build` throws if a
  config id is registered twice, so re-entering would disconnect the player.
- Access widening is done with a native NeoForge access transformer
  (`META-INF/accesstransformer.cfg`), converted from the original access widener.
- Mixin configs are declared explicitly in `META-INF/neoforge.mods.toml`; NeoForge does not
  discover them from `fabric.mod.json` the way Fabric does.

### Rebuilding after a build.gradle change

`unpackGameContent` is a Gradle `Copy` task, which only adds files. Classes unpacked by a previous
build stay in `build/classes/java/main` and are packaged into the next jar even after the source of
that unpacking is removed. Always run `clean` after changing which libraries are unpacked.

## Known limitations

- **Runtime verification is incomplete.** The build succeeds, the jar structure is correct, and the
  failures recorded in the commit history have been fixed, but not every command has been exercised
  in a live game. Please report problems together with a log.
- **`src/test` is not part of the build.** The test sources need test-only dependencies (ASM, Guava,
  jspecify) that the NeoForge build does not declare yet. `src/main` compiles cleanly.
- **`fabric-rendering-v1` is not bundled.** A few Fabric API types from that module are provided by
  hand-written sources under `src/main/java/net/fabricmc/`, because bundling the module would add
  mixins with more risk than benefit. Functionally equivalent, but not the upstream implementation.
- **Merging upstream is manual.** When the upstream `fabric` branch moves, the compatibility layer
  under `compat/` has to be re-merged. The command bodies themselves are almost untouched, so the
  cost is contained.

## Contributing

Bug reports and fixes for the port itself are welcome. Changes to the mod's features or commands
belong upstream: https://github.com/Earthcomputer/clientcommands

To contribute translations, see the upstream
[translation contribution guidelines](docs/TRANSLATING.md).
