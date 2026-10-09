<p align="center">
  <img src=".github/minisort-banner.webp" alt="Minisort sorting a chest with its Sort button" width="100%" />
</p>

<p align="center">
  <picture><source media="(prefers-color-scheme: dark)" srcset="https://shieldcn.dev/badge/Minecraft-1.21.11%20%C2%B7%2026.1.2%20%C2%B7%2026.2%20%C2%B7%2026.3-4ade80.svg?logo=lu:Pickaxe&amp;variant=secondary&amp;mode=dark" /><img alt="Minecraft 1.21.11, 26.1.2, 26.2, and 26.3" src="https://shieldcn.dev/badge/Minecraft-1.21.11%20%C2%B7%2026.1.2%20%C2%B7%2026.2%20%C2%B7%2026.3-4ade80.svg?logo=lu:Pickaxe&amp;variant=secondary&amp;mode=light" /></picture>
  <picture><source media="(prefers-color-scheme: dark)" srcset="https://shieldcn.dev/badge/Loaders-Fabric%20%C2%B7%20Forge%20%C2%B7%20NeoForge-5cd2ff.svg?logo=lu:Layers&amp;variant=secondary&amp;mode=dark" /><img alt="Fabric, Forge, and NeoForge" src="https://shieldcn.dev/badge/Loaders-Fabric%20%C2%B7%20Forge%20%C2%B7%20NeoForge-5cd2ff.svg?logo=lu:Layers&amp;variant=secondary&amp;mode=light" /></picture>
  <a href="https://modrinth.com/mod/amber"><picture><source media="(prefers-color-scheme: dark)" srcset="https://shieldcn.dev/badge/Requires-Amber-ebb134.svg?logo=lu:Gem&amp;variant=secondary&amp;mode=dark" /><img alt="Requires Amber" src="https://shieldcn.dev/badge/Requires-Amber-ebb134.svg?logo=lu:Gem&amp;variant=secondary&amp;mode=light" /></picture></a>
  <a href="https://modrinth.com/mod/konfig"><picture><source media="(prefers-color-scheme: dark)" srcset="https://shieldcn.dev/badge/Requires-Konfig-a78bfa.svg?logo=lu:Settings2&amp;variant=secondary&amp;mode=dark" /><img alt="Requires Konfig" src="https://shieldcn.dev/badge/Requires-Konfig-a78bfa.svg?logo=lu:Settings2&amp;variant=secondary&amp;mode=light" /></picture></a>
  <a href="LICENSE"><picture><source media="(prefers-color-scheme: dark)" srcset="https://shieldcn.dev/badge/License-PolyForm%20Shield-eeeeee.svg?logo=lu:Scale&amp;variant=secondary&amp;mode=dark" /><img alt="PolyForm Shield license" src="https://shieldcn.dev/badge/License-PolyForm%20Shield-eeeeee.svg?logo=lu:Scale&amp;variant=secondary&amp;mode=light" /></picture></a>
  <a href="https://discord.gg/HV5WgTksaB"><picture><source media="(prefers-color-scheme: dark)" srcset="https://shieldcn.dev/discord/1207469438719492176.svg?variant=secondary&amp;mode=dark" /><img alt="Discord" src="https://shieldcn.dev/discord/1207469438719492176.svg?variant=secondary&amp;mode=light" /></picture></a>
</p>

Minisort lets you sort storage, put matching items away, and collect supplies with three buttons: **Sort**, **Deposit**, and **Retrieve**. You can sort your inventory as well. Automatic hand refill replaces used-up stacks and broken tools with matching items you're carrying.

## The buttons

You'll find the controls along the right edge of an open chest.

- **Sort** combines stacks where there's room, then arranges the chest's contents. Your carried items aren't rearranged.
- **Deposit** checks what the chest contains and stores matching items from your inventory and hotbar. Put some cobblestone in a chest, and you can send the rest of your cobblestone there with one click.
- **Retrieve** uses your carried items as the filter. If you have a torch, clicking it collects the chest's torches, up to the space available in your inventory.

Holding **Shift** removes the matching-item filter. **Shift-Deposit** stores as much of your main inventory as the chest can hold, without moving hotbar items. **Shift-Retrieve** collects whatever fits, using the main inventory slots before the hotbar. Each button has a tooltip explaining its action.

<p align="center">
  <img src=".github/sort-before-after.png" alt="Chest contents before and after using Sort" width="100%" />
</p>

Supported storage includes regular and ender chests, barrels, shulker boxes, dispensers, droppers, and hoppers. Screens for crafting, smelting, repairing, trading, and similar tasks don't have these controls.

## Sorting your inventory

The inventory screen's **Sort** button arranges your 27 main inventory slots. Hotbar positions stay fixed.

Middle-click works as a shortcut: click a storage slot to sort that container, or an inventory slot to sort your carried items. Creative mode retains Minecraft's usual middle-click item copying.

## Sort order

The default order uses registry IDs. Items from the same mod appear together, ordered by their ID names.

Choose the experimental **Categories** option to group blocks, tools, combat equipment, armor, food, potions, materials, and spawn eggs. Related building blocks share a group: oak logs, planks, stairs, and doors, for example. Equipment is sorted by tier; colored items use the dye sequence.

## Hand refill

Hand refill works for either hand. It searches your inventory for a replacement after you:

- place the final block in a stack
- finish a stack of food or drinks
- use your remaining ender pearl, snowball, bone meal, or spawn egg
- break a tool

Names, enchantments, and other item data must match. Replacement tools may have different wear, but must otherwise match the broken tool. By default, the search starts in the hotbar and continues through your main inventory. Items inside bundles, shulker boxes, or open containers aren't available for refill.

Refill is disabled in Creative mode. Throwing items away and equipping armor don't activate it either.

## Configuration

<p align="center">
  <img src=".github/config.webp" alt="Minisort settings with illustrations of their effects" width="100%" />
</p>

Use the mod list to reach Minisort's configuration screen. Fabric users need [Mod Menu](https://modrinth.com/mod/modmenu) for this entry.

- Each player chooses their **Sort Mode** and button placement independently.
- The server controls **Hand Refill**, with separate switches for blocks, tool breakage, food and drinks, and other items used by right-clicking. Disable **Search Hotbar First** to look for replacements in your main inventory before checking other hotbar slots.

## Installation

Install Minisort alongside [Amber](https://modrinth.com/mod/amber) and [Konfig](https://modrinth.com/mod/konfig). Fabric installations require [Fabric API](https://modrinth.com/mod/fabric-api) too. Both the client and server need the mod, since the server handles item sorting, transfers, and refill.

Files are available for Fabric, Forge, and NeoForge, covering Minecraft 1.21.11, 26.1.2, 26.2, and 26.3. Support for earlier Minecraft releases is planned.

## Artifacts

Published files are separate jars for each loader. To build a jar combining
loaders for each Minecraft version, run `just horizontal-jars`.

The combined builds retain common class names on 26.1.2, 26.2, and 26.3.
On 1.21.11, this format is experimental and may relocate common classes or
loader metadata. Addons and mixins referencing Minisort's internals can
break as a result; choose a separate loader jar for compatibility on that
version.
