import { Capability, Readiness, describe, expect, pos, test } from "@teakit/test";
import type { BlockPos, ClientScreen, TeaKitTestContext } from "@teakit/test";

describe.configure({
  timeout: "6m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreen,
    Capability.ClientScreens,
    Capability.PlayerInteractions,
    Capability.PlayerInventory,
    Capability.PlayerPosition,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
    Capability.WorldBlock,
    Capability.WorldInspection,
  ],
});

const containerPos: BlockPos = pos(0, 71, 0);
const refillTarget: BlockPos = pos(0, 79, 0);
const configScreen = "com.iamkaf.konfig.impl.v1.client.screen.KonfigConfigScreen";
const chestScreen = "net.minecraft.client.gui.screens.inventory.ContainerScreen";
const storageBlocks = [
  { block: "minecraft:chest[facing=north]", screen: chestScreen },
  { block: "minecraft:barrel[facing=north,open=false]", screen: chestScreen },
  { block: "minecraft:shulker_box", screen: "net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen" },
  { block: "minecraft:dispenser[facing=north,triggered=false]", screen: "net.minecraft.client.gui.screens.inventory.DispenserScreen" },
  { block: "minecraft:dropper[facing=north,triggered=false]", screen: "net.minecraft.client.gui.screens.inventory.DispenserScreen" },
  { block: "minecraft:hopper[enabled=false,facing=down]", screen: "net.minecraft.client.gui.screens.inventory.HopperScreen" },
] as const;

type Item = { id: string; count: number; slot: number };

describe("Minisort", () => {
  for (const storage of storageBlocks) {
    test(`sorts and compacts ${storage.block.split("[")[0]}`, async (ctx) => {
      await prepareStorage(ctx, storage.block);
      const screen = await openContainer(ctx, storage.screen);
      await screen.widgets().activate("Sort container");

      await expect(() => containerItems(ctx)).toEventuallyEqual([
        { id: "minecraft:stone", count: 64, slot: 0 },
        { id: "minecraft:stone", count: 6, slot: 1 },
        { id: "minecraft:dirt", count: 2, slot: 2 },
        { id: "minecraft:apple", count: 3, slot: 3 },
      ], { timeout: "5s" });
      await ctx.client.closeMenus();
    });
  }

  test("rejects sorting while the cursor carries a stack", async (ctx) => {
    await prepareStorage(ctx, "minecraft:chest[facing=north]");
    const screen = await openContainer(ctx, chestScreen);
    await screen.menu().slot(1).click({ button: 0, clickType: "PICKUP" });
    await expect(() => containerItems(ctx)).toEventuallyEqual([
      { id: "minecraft:stone", count: 20, slot: 0 },
      { id: "minecraft:stone", count: 50, slot: 2 },
      { id: "minecraft:dirt", count: 2, slot: 3 },
    ], { timeout: "5s" });

    await screen.widgets().activate("Sort container");
    // Nothing observable changes on a rejected sort, so give the server a moment to act on the request.
    await ctx.runtime.wait(500);
    expect(await containerItems(ctx)).toEqual([
      { id: "minecraft:stone", count: 20, slot: 0 },
      { id: "minecraft:stone", count: 50, slot: 2 },
      { id: "minecraft:dirt", count: 2, slot: 3 },
    ]);
    await ctx.client.closeMenus();
  });

  test("shows the container controls in a column beside the container's slots", async (ctx) => {
    await prepareStorage(ctx, "minecraft:chest[facing=north]");
    const widgets = (await openContainer(ctx, chestScreen)).widgets().all();
    const sort = widgets.find((widget) => widget.label === "Sort container");
    const deposit = widgets.find((widget) => widget.label === "Deposit matching items");
    const retrieve = widgets.find((widget) => widget.label === "Retrieve matching items");

    expect(sort).toBeDefined();
    expect(deposit).toBeDefined();
    expect(retrieve).toBeDefined();
    expect(deposit?.x).toBe(sort?.x);
    expect(retrieve?.x).toBe(sort?.x);
    expect((deposit?.y ?? 0) - (sort?.y ?? 0)).toBe(13);
    expect((retrieve?.y ?? 0) - (deposit?.y ?? 0)).toBe(13);
    await ctx.client.closeMenus();
  });

  test("deposits only item types already stored in the container, leaving the hotbar", async (ctx) => {
    await prepareArea(ctx);
    await ctx.commands.run("/setblock 0 71 0 minecraft:chest[facing=north]");
    await ctx.commands.batch([
      "/item replace block 0 71 0 container.0 with minecraft:stone 30",
      "/item replace block 0 71 0 container.1 with minecraft:dirt 2",
      "/item replace entity @s hotbar.1 with minecraft:stone 20",
      "/item replace entity @s inventory.4 with minecraft:stone 15",
      "/item replace entity @s inventory.1 with minecraft:apple 5",
    ], { requireSuccess: true });
    const screen = await openContainer(ctx, chestScreen);
    await screen.widgets().activate("Deposit matching items");

    await expect(async () => totalCount(await containerItems(ctx), "minecraft:stone")).toEventuallyEqual(45, { timeout: "5s" });
    const items = await containerItems(ctx);
    expect(totalCount(items, "minecraft:dirt")).toBe(2);
    expect(totalCount(items, "minecraft:apple")).toBe(0);
    await ctx.commands.assert("/execute unless items entity @s inventory.* minecraft:stone");
    await ctx.commands.assert("/execute if items entity @s hotbar.1 minecraft:stone[count=20]");
    await ctx.commands.assert("/execute if items entity @s inventory.* minecraft:apple[count=5]");
    await ctx.client.closeMenus();
  });

  test("retrieves only item types already carried by the player", async (ctx) => {
    await prepareArea(ctx);
    await ctx.commands.run("/setblock 0 71 0 minecraft:chest[facing=north]");
    await ctx.commands.batch([
      "/item replace block 0 71 0 container.0 with minecraft:stone 20",
      "/item replace block 0 71 0 container.1 with minecraft:apple 5",
      "/item replace block 0 71 0 container.2 with minecraft:dirt 2",
      "/item replace entity @s inventory.0 with minecraft:stone 1",
    ], { requireSuccess: true });
    const screen = await openContainer(ctx, chestScreen);
    await screen.widgets().activate("Retrieve matching items");

    await expect(async () => totalCount(await containerItems(ctx), "minecraft:stone")).toEventuallyEqual(0, { timeout: "5s" });
    const items = await containerItems(ctx);
    expect(totalCount(items, "minecraft:apple")).toBe(5);
    expect(totalCount(items, "minecraft:dirt")).toBe(2);
    await ctx.commands.assert("/execute if items entity @s inventory.* minecraft:stone[count=21]");
    await ctx.commands.assert("/execute unless items entity @s container.* minecraft:apple");
    await ctx.client.closeMenus();
  });

  test("does not expose container controls on a special-purpose menu", async (ctx) => {
    await prepareArea(ctx);
    await ctx.commands.run("/setblock 0 71 0 minecraft:anvil");
    const screen = await openContainer(ctx, "net.minecraft.client.gui.screens.inventory.AnvilScreen");
    const labels = screen.widgets().all().map((widget) => widget.label);
    expect(labels).not.toContain("Sort container");
    expect(labels).not.toContain("Deposit matching items");
    expect(labels).not.toContain("Retrieve matching items");
    await ctx.client.closeMenus();
  });

  test("sorts the main inventory and leaves the hotbar alone", async (ctx) => {
    await prepareArea(ctx);
    await ctx.commands.batch([
      "/item replace entity @s hotbar.0 with minecraft:stone 5",
      "/item replace entity @s hotbar.4 with minecraft:apple 2",
      "/item replace entity @s inventory.0 with minecraft:stone 20",
      "/item replace entity @s inventory.5 with minecraft:apple 3",
      "/item replace entity @s inventory.9 with minecraft:stone 50",
      "/item replace entity @s inventory.20 with minecraft:dirt 2",
    ], { requireSuccess: true });
    await ctx.client.openInventory();
    const screen = await ctx.client.waitForScreen("net.minecraft.client.gui.screens.inventory.InventoryScreen", { timeoutMs: 8_000 });
    await screen.widgets().activate("Sort inventory");

    await eventually(ctx, "/execute if items entity @s inventory.0 minecraft:stone[count=64]");
    await ctx.commands.assert("/execute if items entity @s inventory.1 minecraft:stone[count=6]");
    await ctx.commands.assert("/execute if items entity @s inventory.2 minecraft:dirt[count=2]");
    await ctx.commands.assert("/execute if items entity @s inventory.3 minecraft:apple[count=3]");
    await ctx.commands.assert("/execute unless items entity @s inventory.4 *");
    await ctx.commands.assert("/execute if items entity @s hotbar.0 minecraft:stone[count=5]");
    await ctx.commands.assert("/execute if items entity @s hotbar.4 minecraft:apple[count=2]");
    await ctx.client.closeMenus();
  });

  test("middle-click sorts the side of the screen under the cursor", async (ctx) => {
    await prepareStorage(ctx, "minecraft:chest[facing=north]");
    await ctx.commands.batch([
      "/item replace entity @s inventory.0 with minecraft:dirt 1",
      "/item replace entity @s inventory.7 with minecraft:apple 4",
      "/item replace entity @s inventory.8 with minecraft:dirt 5",
    ], { requireSuccess: true });
    const screen = await openContainer(ctx, chestScreen);

    await middleClickSlot(ctx, screen, (slot) => slot.containerSlot === 9 && slot.slot >= 27);
    await eventually(ctx, "/execute if items entity @s inventory.0 minecraft:dirt[count=6]");
    await ctx.commands.assert("/execute if items entity @s inventory.1 minecraft:apple[count=4]");
    expect(await containerItems(ctx)).toEqual([
      { id: "minecraft:stone", count: 20, slot: 0 },
      { id: "minecraft:apple", count: 3, slot: 1 },
      { id: "minecraft:stone", count: 50, slot: 2 },
      { id: "minecraft:dirt", count: 2, slot: 3 },
    ]);

    await middleClickSlot(ctx, screen, (slot) => slot.slot === 0);
    await expect(() => containerItems(ctx)).toEventuallyEqual([
      { id: "minecraft:stone", count: 64, slot: 0 },
      { id: "minecraft:stone", count: 6, slot: 1 },
      { id: "minecraft:dirt", count: 2, slot: 2 },
      { id: "minecraft:apple", count: 3, slot: 3 },
    ], { timeout: "5s" });
    await ctx.client.closeMenus();
  });

  test("leaves middle-click to vanilla in creative mode", async (ctx) => {
    await prepareStorage(ctx, "minecraft:chest[facing=north]");
    await ctx.commands.run("/gamemode creative @s");
    const screen = await openContainer(ctx, chestScreen);
    await middleClickSlot(ctx, screen, (slot) => slot.slot === 5);
    // Nothing observable changes when the click is ignored, so give the server a moment to act on a request.
    await ctx.runtime.wait(500);
    expect(await containerItems(ctx)).toEqual([
      { id: "minecraft:stone", count: 20, slot: 0 },
      { id: "minecraft:apple", count: 3, slot: 1 },
      { id: "minecraft:stone", count: 50, slot: 2 },
      { id: "minecraft:dirt", count: 2, slot: 3 },
    ]);
    await ctx.client.closeMenus();
  });

  test("the sort key sorts the container, or the side under the cursor", async (ctx) => {
    await prepareStorage(ctx, "minecraft:chest[facing=north]");
    await ctx.commands.batch([
      "/item replace entity @s inventory.0 with minecraft:dirt 1",
      "/item replace entity @s inventory.7 with minecraft:apple 4",
      "/item replace entity @s inventory.8 with minecraft:dirt 5",
    ], { requireSuccess: true });
    const screen = await openContainer(ctx, chestScreen);

    // Over no slot, the key sorts the container.
    await ctx.client.scroll({ x: 2, y: 2, verticalAmount: 0 });
    await ctx.client.key(sortKey, { release: true });
    await expect(() => containerItems(ctx)).toEventuallyEqual([
      { id: "minecraft:stone", count: 64, slot: 0 },
      { id: "minecraft:stone", count: 6, slot: 1 },
      { id: "minecraft:dirt", count: 2, slot: 2 },
      { id: "minecraft:apple", count: 3, slot: 3 },
    ], { timeout: "5s" });
    await ctx.commands.assert("/execute if items entity @s inventory.7 minecraft:apple[count=4]");

    // Over an inventory slot, it sorts the inventory instead.
    await pointAtSlot(ctx, screen, (slot) => slot.containerSlot === 9 && slot.slot >= 27);
    await ctx.client.key(sortKey, { release: true });
    await eventually(ctx, "/execute if items entity @s inventory.0 minecraft:dirt[count=6]");
    await ctx.commands.assert("/execute if items entity @s inventory.1 minecraft:apple[count=4]");
    await ctx.client.closeMenus();
  });

  test("deposits the whole main inventory but not the hotbar with Shift", { target: { minecraft: ">=1.21.9" } }, async (ctx) => {
    await prepareArea(ctx);
    await ctx.commands.run("/setblock 0 71 0 minecraft:chest[facing=north]");
    await ctx.commands.batch([
      "/item replace block 0 71 0 container.0 with minecraft:stone 30",
      "/item replace entity @s hotbar.0 with minecraft:torch 10",
      "/item replace entity @s inventory.0 with minecraft:apple 5",
      "/item replace entity @s inventory.13 with minecraft:dirt 7",
    ], { requireSuccess: true });
    const screen = await openContainer(ctx, chestScreen);
    await screen.widgets().find("Deposit matching items").click({ button: 0, modifiers: shiftModifier });
    await expect(async () => totalCount(await containerItems(ctx), "minecraft:dirt")).toEventuallyEqual(7, { timeout: "5s" });

    const items = await containerItems(ctx);
    expect(totalCount(items, "minecraft:apple")).toBe(5);
    expect(totalCount(items, "minecraft:stone")).toBe(30);
    expect(totalCount(items, "minecraft:torch")).toBe(0);
    await ctx.commands.assert("/execute if items entity @s hotbar.0 minecraft:torch[count=10]");
    await ctx.commands.assert("/execute unless items entity @s inventory.* *");
    await ctx.client.closeMenus();
  });

  test("retrieves everything into the main inventory first with Shift", { target: { minecraft: ">=1.21.9" } }, async (ctx) => {
    await prepareArea(ctx);
    await ctx.commands.run("/setblock 0 71 0 minecraft:chest[facing=north]");
    await ctx.commands.batch([
      "/item replace block 0 71 0 container.0 with minecraft:apple 5",
      "/item replace block 0 71 0 container.4 with minecraft:dirt 7",
      "/item replace entity @s hotbar.0 with minecraft:torch 10",
    ], { requireSuccess: true });
    const screen = await openContainer(ctx, chestScreen);
    await screen.widgets().find("Retrieve matching items").click({ button: 0, modifiers: shiftModifier });
    await expect(async () => (await containerItems(ctx)).length).toEventuallyEqual(0, { timeout: "5s" });

    await ctx.commands.assert("/execute if items entity @s inventory.0 minecraft:apple[count=5]");
    await ctx.commands.assert("/execute if items entity @s inventory.1 minecraft:dirt[count=7]");
    await ctx.commands.assert("/execute unless items entity @s hotbar.1 *");
    await ctx.client.closeMenus();
  });

  test("refills placed blocks in both hands without creating items", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:stone 1",
      "/item replace entity @s inventory.0 with minecraft:stone 12",
    ]);
    await eventuallyHolds(ctx, () => expect(ctx.player.inventory()).toContainItem("minecraft:stone", { slot: 0, count: 1 }));
    await ctx.player.useBlock(refillTarget, { face: "up", hand: "main_hand" });
    await eventually(ctx, "/execute if items entity @s weapon.mainhand minecraft:stone[count=12]");
    await ctx.commands.assert("/execute unless items entity @s inventory.0 *");

    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.offhand with minecraft:cobblestone 1",
      "/item replace entity @s inventory.1 with minecraft:cobblestone 7",
    ]);
    await eventuallyHolds(ctx, () => expect(ctx.player.inventory()).toContainItem("minecraft:cobblestone", { equipmentSlot: "offhand", count: 1 }));
    await ctx.player.useBlock(refillTarget, { face: "up", hand: "off_hand" });
    await eventually(ctx, "/execute if items entity @s weapon.offhand minecraft:cobblestone[count=7]");
    await ctx.commands.assert("/execute unless items entity @s inventory.1 *");
  });

  test("leaves a hand empty when no exact component match exists", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:stone[minecraft:custom_data={minisort_marker:1b}] 1",
      "/item replace entity @s inventory.0 with minecraft:stone[minecraft:custom_data={minisort_marker:2b}] 8",
    ]);
    await ctx.player.useBlock(refillTarget, { face: "up", hand: "main_hand" });
    await eventually(ctx, "/execute if block 0 80 0 minecraft:stone");
    await ctx.runtime.wait(250);

    await ctx.commands.assert("/execute unless items entity @s weapon.mainhand *");
    await ctx.commands.assert(
      "/execute if items entity @s inventory.0 minecraft:stone[count=8,minecraft:custom_data~{minisort_marker:2b}]",
    );
  });

  test("refills a completed consumable use", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:golden_apple 1",
      "/item replace entity @s inventory.0 with minecraft:golden_apple 4",
    ]);
    await ctx.player.holdUse(true);
    try {
      await eventually(ctx, "/execute if items entity @s weapon.mainhand minecraft:golden_apple[count=4]", "6s");
    } finally {
      await ctx.player.holdUse(false);
    }
    await ctx.commands.assert("/execute unless items entity @s inventory.0 *");
  });

  test("refills a thrown item", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:snowball 1",
      "/item replace entity @s inventory.0 with minecraft:snowball 6",
    ]);
    await tapUse(ctx);

    // The use input can repeat once the hand refills, so check where the spare stack went rather than its count.
    await eventually(ctx, "/execute unless items entity @s inventory.0 *");
    await ctx.commands.assert("/execute if items entity @s weapon.mainhand minecraft:snowball");
  });

  test("does not pull a second piece of armor into the hand after equipping one", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:iron_helmet",
      "/item replace entity @s inventory.0 with minecraft:iron_helmet",
    ]);
    await tapUse(ctx);

    await eventually(ctx, "/execute if items entity @s armor.head minecraft:iron_helmet");
    await ctx.runtime.wait(250);
    await ctx.commands.assert("/execute unless items entity @s weapon.mainhand *");
    await ctx.commands.assert("/execute if items entity @s inventory.0 minecraft:iron_helmet");
  });

  test("replaces a broken tool with a copy that differs only in durability", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.run("/setblock 0 80 0 minecraft:dirt");
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:golden_shovel[minecraft:damage=31]",
      "/item replace entity @s inventory.0 with minecraft:golden_shovel[minecraft:damage=5,minecraft:custom_data={spare:1b}]",
      "/item replace entity @s inventory.1 with minecraft:golden_shovel[minecraft:damage=9]",
    ]);
    await ctx.player.mine(pos(0, 80, 0), { timeout: "5s" });

    await eventually(ctx, "/execute if items entity @s weapon.mainhand minecraft:golden_shovel[minecraft:damage=9]");
    await ctx.commands.assert("/execute if items entity @s inventory.0 minecraft:golden_shovel[minecraft:damage=5]");
    await ctx.commands.assert("/execute unless items entity @s inventory.1 *");
  });

  test("does not react to an intentional drop", async (ctx) => {
    await prepareRefill(ctx);
    await ctx.commands.batch([
      "/item replace entity @s weapon.mainhand with minecraft:stone 1",
      "/item replace entity @s inventory.0 with minecraft:stone 9",
    ]);
    await ctx.player.dropMainHand({ count: 1 });
    await eventually(ctx, "/execute unless items entity @s weapon.mainhand *");
    await ctx.runtime.wait(250);

    await ctx.commands.assert("/execute unless items entity @s weapon.mainhand *");
    await ctx.commands.assert("/execute if items entity @s inventory.0 minecraft:stone[count=9]");
  });

  // Leaving the world ends every later test, so this stays last.
  // TeaKit's Forge control plane stops answering after leaveWorld, so this runs on Fabric and NeoForge.
  test("opens the config screen with its picture panels", { target: { loader: ["fabric", "neoforge"] } }, async (ctx) => {
    await ctx.client.leaveWorld();
    await ctx.client.waitForScreen("Title", { timeoutMs: 30_000 });
    let screen = await ctx.client.screen();
    await screen.widgets().activate({ label: "Mods", contains: true });
    screen = await ctx.client.waitForScreen("Mods", { timeoutMs: 5_000 });
    if (screen.widgets().all().some((widget) => widget.label === "A-Z")) {
      await screen.widgets().activate({ label: "A-Z" });
    }
    screen = await selectMod(ctx, "Minisort");
    if ((await ctx.runtime.health()).loader === "fabric") {
      await activateModMenuConfigure(screen);
    } else {
      await screen.widgets().activate({ label: "Config", nth: 0 });
    }
    screen = await ctx.client.waitForScreen(configScreen, { timeoutMs: 10_000 });
    await expect(async () => (await ctx.client.screen()).lists().entries().length > 0).toEventuallyEqual(true, { timeout: "5s" });
    screen = await ctx.client.screen();
    const labels = screen.lists().entries().map((entry) => entry.label);
    for (const setting of ["Sort Mode", "Item Animation", "Button Style", "Turned Off In"]) {
      expect(labels).toContain(setting);
    }
    const sortMode = screen.lists().entries().find((entry) => entry.label === "Sort Mode");
    if (sortMode != null) {
      // Pointing at a row fills the info panel with its picture.
      await ctx.client.click({ x: sortMode.x + 4, y: sortMode.y + sortMode.height / 2, button: 1 });
      await ctx.client.waitForFrames(5);
    }
    await ctx.client.screenshot("minisort-config-screen", { hideWindowDecoration: true });
  });
});

async function selectMod(ctx: TeaKitTestContext, label: string): Promise<ClientScreen> {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10_000) {
    const screen = await ctx.client.screen();
    const entry = screen.lists().entries().find((candidate) => candidate.label.includes(label));
    if (entry?.selected) return screen;
    if (entry != null) {
      await ctx.client.click({ x: entry.x + entry.width / 2, y: entry.y + entry.height / 2, button: 0 });
    } else {
      await screen.scroll({ vertical: -2 });
    }
    await ctx.runtime.wait(200);
  }
  const labels = (await ctx.client.screen()).lists().entries().map((entry) => entry.label);
  throw new Error(`Timed out selecting mod list entry ${label}; visible entries: ${labels.join(", ")}`);
}

async function activateModMenuConfigure(screen: ClientScreen): Promise<void> {
  if (screen.widgets().all().some((widget) => widget.label === "Configure...")) {
    await screen.widgets().activate({ label: "Configure..." });
    return;
  }
  await screen.widgets().activate({ widgetClass: "com.terraformersmc.modmenu.gui.widget.LegacyTexturedButtonWidget", nth: 1 });
}

async function openContainer(ctx: TeaKitTestContext, screenClass: string): Promise<ClientScreen> {
  await ctx.player.openBlock(containerPos);
  return ctx.client.waitForScreen(screenClass, { timeoutMs: 8_000 });
}

type SlotPick = (slot: { slot: number; containerSlot?: number }) => boolean;

async function middleClickSlot(ctx: TeaKitTestContext, screen: ClientScreen, pick: SlotPick) {
  const { x, y } = slotCenter(screen, pick);
  await ctx.client.click({ x, y, button: 2 });
}

async function pointAtSlot(ctx: TeaKitTestContext, screen: ClientScreen, pick: SlotPick) {
  const { x, y } = slotCenter(screen, pick);
  await ctx.client.scroll({ x, y, verticalAmount: 0 });
}

// On a chest screen, the Sort button stands one pixel right of the panel, level with the top of the chest's slots
// (one pixel above the first slot's item), so it locates the panel's corner.
function slotCenter(screen: ClientScreen, pick: SlotPick): { x: number; y: number } {
  const sort = screen.widgets().all().find((widget) => widget.label === "Sort container");
  const slot = screen.menu().slots().find(pick);
  if (sort == null || slot?.x == null || slot.y == null) {
    throw new Error("Could not locate the slot");
  }
  const left = sort.x - 177;
  const top = sort.y - 17;
  return { x: left + slot.x + 8, y: top + slot.y + 8 };
}

// GLFW's Shift modifier bit, as a shift-click sends it. 1.21.1 clicks carry no modifiers, so the Shift tests skip it.
const shiftModifier = 1;

// The Sort key's default binding, R.
const sortKey = 82;

async function containerItems(ctx: TeaKitTestContext): Promise<Item[]> {
  return (await ctx.world.container(containerPos).inspect()).items
    .map(projectItem)
    .sort((left, right) => left.slot - right.slot);
}

// Retries an assertion about client state that trails the server by a tick or more.
async function eventuallyHolds(ctx: TeaKitTestContext, check: () => unknown, timeoutMs = 5_000) {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    try {
      await check();
      return;
    } catch (error) {
      if (Date.now() > deadline) throw error;
      await ctx.runtime.wait(100);
    }
  }
}

// Polls a server command until it succeeds, for state the server settles at the end of a tick.
async function eventually(ctx: TeaKitTestContext, command: string, timeout = "5s") {
  await expect(async () => (await ctx.commands.run(command)).success).toEventuallyEqual(true, { timeout });
}

// Sends a single right-click through the client, like a player tapping the use key.
async function tapUse(ctx: TeaKitTestContext) {
  await ctx.player.holdUse(true);
  await ctx.runtime.wait(100);
  await ctx.player.holdUse(false);
}

async function prepareArea(ctx: TeaKitTestContext) {
  await ctx.client.closeMenus();
  await ctx.commands.batch([
    "/gamemode survival @s",
    "/clear @s",
    "/fill -3 70 -3 3 70 3 minecraft:stone",
    "/fill -3 71 -3 3 75 3 minecraft:air",
    "/tp @s 0.5 72 -1.5",
  ]);
  // Landing requires the client to acknowledge the teleport before block use.
  await expect(() => ctx.player.position()).toEventuallyEqual({ x: 0.5, y: 71, z: -1.5 }, { timeout: "5s" });
}

async function prepareStorage(ctx: TeaKitTestContext, block: string) {
  await prepareArea(ctx);
  await ctx.commands.run(`/setblock 0 71 0 ${block}`);
  await ctx.commands.batch([
    "/item replace block 0 71 0 container.0 with minecraft:stone 20",
    "/item replace block 0 71 0 container.1 with minecraft:apple 3",
    "/item replace block 0 71 0 container.2 with minecraft:stone 50",
    "/item replace block 0 71 0 container.3 with minecraft:dirt 2",
  ], { requireSuccess: true });
}

async function prepareRefill(ctx: TeaKitTestContext) {
  await ctx.client.closeMenus();
  await ctx.player.inventory().selectHotbar(0);
  await ctx.commands.batch([
    "/gamemode survival @s",
    "/clear @s",
    "/fill -2 79 -2 2 79 2 minecraft:stone",
    "/fill -2 80 -2 2 84 2 minecraft:air",
    "/tp @s 0.5 81 -0.5 0 45",
  ]);
  await expect(() => ctx.player.position()).toEventuallyEqual({ x: 0.5, y: 80, z: -0.5 }, { timeout: "5s" });
}

function projectItem(item: Record<string, unknown>): Item {
  const id = item.id ?? item.item ?? item.itemId ?? item.type;
  return { id: String(id), count: Number(item.count), slot: Number(item.slot) };
}

function totalCount(items: Array<{ id: string; count: number }>, itemId: string): number {
  return items
    .filter((item) => item.id === itemId)
    .reduce((total, item) => total + item.count, 0);
}
