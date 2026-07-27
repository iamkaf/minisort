![Pixel-art chest sorting loose item stacks into the word minisort](docs/assets/minisort-banner.png)

# minisort

minisort adds a compact sorting button to normal Minecraft storage screens.
Sorting is performed by the server, keeps item components intact, and fails
without changing the container if a slot cannot safely accept the result.

## Supported storage

- Chests and barrels
- Shulker boxes
- Dispensers and droppers
- Hoppers

Special-purpose menus such as crafting tables, anvils, grindstones, merchants,
enchanting tables, looms, and stonecutters are intentionally left untouched.
Player-inventory sorting is not part of the initial release.

## Requirements

minisort requires [Amber](https://modrinth.com/mod/amber).

The mod supports Fabric, Forge, and NeoForge on Minecraft 1.21.1, 1.21.11,
26.1.2, and 26.2.

## Artifacts

Loader-specific jars are the default publication format. One merged jar per
Minecraft version can also be built with `just horizontal-jars`.

Merged jars for 26.1.2 and 26.2 preserve stable common class names. Merged jars
for 1.21.1 and 1.21.11 are experimental: they can relocate common classes or
loader metadata, which may break addons and mixins that target minisort
internals. Use loader-specific jars on those versions when compatibility is
important.
