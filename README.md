![Pixel-art chest sorting loose item stacks into the word minisort](docs/assets/minisort-banner.webp)

# minisort

minisort adds a compact sorting button to normal Minecraft storage screens and
refills an emptied hand from matching inventory stacks. Both features run on
the logical server and preserve item components.

## Supported storage

- Chests and barrels
- Shulker boxes
- Dispensers and droppers
- Hoppers

Special-purpose menus such as crafting tables, anvils, grindstones, merchants,
enchanting tables, looms, and stonecutters are intentionally left untouched.
Player-inventory sorting is not part of the initial release.

## Refill behavior

- Exact item-component matching for blocks and consumables.
- Tool replacement ignores durability damage but preserves every other
  component.
- Main-hand and off-hand refill support.
- Configurable feature categories and inventory search order.
- No shulker-box, bundle, armor-slot, cursor-stack, or open-container scans.

Generic right-click items are disabled by default. Refill only reacts to
item-use and durability events; dropping or otherwise clearing a hand does not
trigger a refill.

## Requirements

minisort requires [Amber](https://modrinth.com/mod/amber) and
[Konfig](https://modrinth.com/mod/konfig).

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
