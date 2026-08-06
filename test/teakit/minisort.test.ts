import { Capability, Readiness, describe, expect, pos, test } from "@teakit/test";
import type { BlockPos, TeaKitTestContext } from "@teakit/test";

describe.configure({
  timeout: "6m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientScreen,
    Capability.ClientScreens,
    Capability.PlayerInteractions,
    Capability.PlayerInventory,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
    Capability.WorldBlock,
    Capability.WorldInspection,
  ],
});

const containerPos: BlockPos = pos(0, 71, 0);
const refillTarget: BlockPos = pos(0, 79, 0);
const storageBlocks = [
  { block: "minecraft:chest[facing=north]", screen: "net.minecraft.client.gui.screens.inventory.ContainerScreen" },
  { block: "minecraft:barrel[facing=north,open=false]", screen: "net.minecraft.client.gui.screens.inventory.ContainerScreen" },
  { block: "minecraft:shulker_box", screen: "net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen" },
  { block: "minecraft:dispenser[facing=north,triggered=false]", screen: "net.minecraft.client.gui.screens.inventory.DispenserScreen" },
  { block: "minecraft:dropper[facing=north,triggered=false]", screen: "net.minecraft.client.gui.screens.inventory.DispenserScreen" },
  { block: "minecraft:hopper[enabled=false,facing=down]", screen: "net.minecraft.client.gui.screens.inventory.HopperScreen" },
] as const;

describe("minisort", () => {
  for (const storage of storageBlocks) {
    test(`sorts and compacts ${storage.block.split("[")[0]}`, async (ctx) => {
      await openAndActivateSort(ctx, storage.block, storage.screen);

      const items = (await ctx.world.container(containerPos).inspect()).items
        .map(projectItem)
        .sort((left, right) => left.slot - right.slot);

      expect(items).toEqual([
        { id: "minecraft:apple", count: 3, slot: 0 },
        { id: "minecraft:dirt", count: 2, slot: 1 },
        { id: "minecraft:stone", count: 64, slot: 2 },
        { id: "minecraft:stone", count: 6, slot: 3 },
      ]);
      await ctx.client.closeMenus();
    });
  }

  test("rejects sorting while the cursor carries a stack", async (ctx) => {
    await prepareStorage(ctx, "minecraft:chest[facing=north]");
    await ctx.player.openBlock(containerPos);
    const screen = await ctx.client.waitForScreen("net.minecraft.client.gui.screens.inventory.ContainerScreen", {
      timeoutMs: 8_000,
    });
    await screen.menu().slot(1).click({ button: 0, clickType: "PICKUP" });
    await ctx.runtime.wait(250);
    await screen.widgets().activate("↕");
    await ctx.runtime.wait(250);

    const items = (await ctx.world.container(containerPos).inspect()).items
      .map(projectItem)
      .sort((left, right) => left.slot - right.slot);
    expect(items).toEqual([
      { id: "minecraft:stone", count: 20, slot: 0 },
      { id: "minecraft:stone", count: 50, slot: 2 },
      { id: "minecraft:dirt", count: 2, slot: 3 },
    ]);
    await ctx.client.closeMenus();
  });

  test("does not expose sorting on a special-purpose menu", async (ctx) => {
    await prepareArea(ctx);
    await ctx.commands.run("/setblock 0 71 0 minecraft:anvil");
    await ctx.runtime.wait(500);
    await ctx.player.openBlock(containerPos);
    const screen = await ctx.client.waitForScreen("net.minecraft.client.gui.screens.inventory.AnvilScreen", {
      timeoutMs: 8_000,
    });
    await expect(screen.widgets().find("↕").activate()).rejects.toThrow();
    await ctx.client.closeMenus();
  });

  test("refills placed blocks in both hands without creating items", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.player.inventory().selectHotbar(0);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:stone 1",
      "/item replace entity @s inventory.0 with minecraft:stone 12",
    ]);
    await ctx.player.useBlock(refillTarget, { face: "up", hand: "main_hand" });
    await ctx.runtime.wait(500);
    await ctx.commands.assert("/execute if items entity @a[limit=1] weapon.mainhand minecraft:stone[count=12]");
    await ctx.commands.assert("/execute unless items entity @a[limit=1] inventory.0 *");

    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.offhand with minecraft:cobblestone 1",
      "/item replace entity @s inventory.1 with minecraft:cobblestone 7",
    ]);
    await ctx.player.useBlock(refillTarget, { face: "up", hand: "off_hand" });
    await ctx.runtime.wait(500);
    await ctx.commands.assert("/execute if items entity @a[limit=1] weapon.offhand minecraft:cobblestone[count=7]");
    await ctx.commands.assert("/execute unless items entity @a[limit=1] inventory.1 *");
  });

  test("leaves a hand empty when no exact component match exists", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.player.inventory().selectHotbar(0);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:stone[minecraft:custom_data={minisort_marker:1b}] 1",
      "/item replace entity @s inventory.0 with minecraft:stone[minecraft:custom_data={minisort_marker:2b}] 8",
    ]);
    await ctx.player.useBlock(refillTarget, { face: "up", hand: "main_hand" });
    await ctx.runtime.wait(500);

    await ctx.commands.assert("/execute unless items entity @a[limit=1] weapon.mainhand *");
    await ctx.commands.assert("/execute if items entity @a[limit=1] inventory.0 minecraft:stone[count=8]");
    await ctx.commands.assert(
      "/execute if items entity @a[limit=1] inventory.0 minecraft:stone[minecraft:custom_data~{minisort_marker:2b}]",
    );
  });

  test("refills a completed consumable use", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.player.inventory().selectHotbar(0);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:golden_apple 1",
      "/item replace entity @s inventory.0 with minecraft:golden_apple 4",
    ]);
    await ctx.player.holdUse(true);
    await ctx.runtime.wait(3000);
    await ctx.player.holdUse(false);
    await ctx.runtime.wait(250);

    await ctx.commands.assert("/execute if items entity @a[limit=1] weapon.mainhand minecraft:golden_apple[count=4]");
    await ctx.commands.assert("/execute unless items entity @a[limit=1] inventory.0 *");
  });

  test("matches replacement tools while ignoring only durability damage", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.run("/setblock 0 80 0 minecraft:dirt");
    await ctx.player.inventory().selectHotbar(0);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:golden_shovel[minecraft:damage=31]",
      "/item replace entity @s inventory.0 with minecraft:golden_shovel[minecraft:damage=5]",
    ]);
    await ctx.player.mine(pos(0, 80, 0), { timeout: "5s" });
    await ctx.runtime.wait(250);

    await ctx.commands.assert(
      "/execute if items entity @a[limit=1] weapon.mainhand minecraft:golden_shovel[minecraft:damage=5]",
    );
    await ctx.commands.assert("/execute unless items entity @a[limit=1] inventory.0 *");
  });

  test("does not react to an intentional drop", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.player.inventory().selectHotbar(0);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:stone 1",
      "/item replace entity @s inventory.0 with minecraft:stone 9",
    ]);
    await ctx.player.dropMainHand({ count: 1 });
    await ctx.runtime.wait(250);

    await ctx.commands.assert("/execute unless items entity @a[limit=1] weapon.mainhand *");
    await ctx.commands.assert("/execute if items entity @a[limit=1] inventory.0 minecraft:stone[count=9]");
  });
});

async function openAndActivateSort(ctx: TeaKitTestContext, block: string, screenClass: string) {
  await prepareStorage(ctx, block);
  await ctx.player.openBlock(containerPos);
  const screen = await ctx.client.waitForScreen(screenClass, { timeoutMs: 8_000 });
  await screen.widgets().activate("↕");
  await ctx.runtime.wait(250);
}

async function prepareArea(ctx: TeaKitTestContext) {
  await ctx.commands.batch([
    "/gamemode survival @a[limit=1]",
    "/clear @a[limit=1]",
    "/fill -3 70 -3 3 70 3 minecraft:stone",
    "/fill -3 71 -3 3 75 3 minecraft:air",
    "/tp @a[limit=1] 0 72 -2",
  ]);
}

async function prepareStorage(ctx: TeaKitTestContext, block: string) {
  await prepareArea(ctx);
  await ctx.commands.run(`/setblock 0 71 0 ${block}`);
  await ctx.runtime.wait(500);
  await ctx.commands.batch([
    "/item replace block 0 71 0 container.0 with minecraft:stone 20",
    "/item replace block 0 71 0 container.1 with minecraft:apple 3",
    "/item replace block 0 71 0 container.2 with minecraft:stone 50",
    "/item replace block 0 71 0 container.3 with minecraft:dirt 2",
  ], { requireSuccess: true });
}

async function prepareRefill(ctx: TeaKitTestContext) {
  await ctx.commands.batch([
    "/gamemode survival @s",
    "/clear @s",
    "/fill -2 79 -2 2 79 2 minecraft:stone",
    "/fill -2 80 -2 2 84 2 minecraft:air",
    "/tp @s 0.5 80 -0.5 0 45",
  ]);
  await ctx.runtime.wait(250);
}

function projectItem(item: Record<string, unknown>): { id: string; count: number; slot: number } {
  const id = item.id ?? item.item ?? item.itemId ?? item.type;
  return { id: String(id), count: Number(item.count), slot: Number(item.slot) };
}
