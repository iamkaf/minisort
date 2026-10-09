import { Capability, Readiness, describe, expect, pos, test } from "@teakit/test";
import type { TeaKitTestContext } from "@teakit/test";

// Runs only in a production client and dedicated-server pair: `just pair <node>`.

const chest = pos(0, 101, 0);
// Matched by title: production 1.21.11 class names are obfuscated.
const CHEST_SCREEN = "Chest";
// GLFW's Shift modifier bit, as a shift-click sends it.
const SHIFT = 1;

describe.configure({
  timeout: "5m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientScreens,
    Capability.PlayerInteractions,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
  ],
});

describe("Minisort on a dedicated server", () => {
  test("a joined player sorts, deposits, retrieves, and refills", async (ctx) => {
    if (!(await ctx.session.info()).paired) throw new Error("This test needs a client and dedicated-server pair");

    // A sky platform away from terrain. Creative until the platform exists, so the player cannot fall while its chunks
    // load. A fill into a chunk that is still loading fails quietly, so retry until the stone is really there.
    await ctx.commands.batch(["/gamemode creative @s", "/clear @s", "/tp @s 0.5 101 -1.5 0 30"]);
    await ctx.commands.batch(["/gamerule spawn_mobs false", "/weather clear"]);
    await eventually(ctx, "execute if block 0 100 0 minecraft:stone", 30_000, async () => {
      await ctx.commands.batch(["/fill -3 100 -3 3 100 3 minecraft:stone", "/fill -3 101 -3 3 104 3 minecraft:air"]);
    });
    await ctx.commands.batch([
      "/tp @s 0.5 101 -1.5 0 30",
      "/gamemode survival @s",
      "/setblock 0 101 0 minecraft:chest[facing=north]",
      "/item replace block 0 101 0 container.0 with minecraft:stone 20",
      "/item replace block 0 101 0 container.1 with minecraft:apple 3",
      "/item replace block 0 101 0 container.2 with minecraft:stone 50",
      "/item replace entity @s inventory.0 with minecraft:stone 10",
      "/item replace entity @s inventory.1 with minecraft:dirt 4",
    ]);

    // The client's buttons send packets the server acts on.
    let screen = await openChest(ctx);
    await screen.widgets().activate("Deposit matching items");
    await eventually(ctx, "execute unless items entity @s inventory.* minecraft:stone");
    await screen.widgets().activate("Sort container");
    await eventually(ctx, "execute if items block 0 101 0 container.1 minecraft:stone[count=64]");
    await ctx.commands.assert("/execute if items block 0 101 0 container.0 minecraft:apple[count=3]");
    await ctx.commands.assert("/execute if items block 0 101 0 container.2 minecraft:stone[count=16]");
    await screen.widgets().find("Retrieve matching items").click({ button: 0, modifiers: SHIFT });
    await eventually(ctx, "execute unless items block 0 101 0 container.* *");
    await ctx.commands.assert("/execute if items entity @s inventory.* minecraft:dirt[count=4]");
    await ctx.client.closeMenus();

    // The server refills the hand after the client places its last block.
    await ctx.commands.batch([
      "/clear @s",
      "/item replace entity @s weapon.mainhand with minecraft:cobblestone 1",
      "/item replace entity @s inventory.5 with minecraft:cobblestone 12",
    ]);
    await ctx.player.useBlock(pos(2, 100, 0), { face: "up", hand: "main_hand" });
    await eventually(ctx, "execute if items entity @s weapon.mainhand minecraft:cobblestone[count=12]");
    await ctx.commands.assert("/execute if block 2 101 0 minecraft:cobblestone");
    expect(true).toBe(true);
  });
});

// The client may not have the teleport or the chest yet, so keep trying until the screen opens.
async function openChest(ctx: TeaKitTestContext) {
  const deadline = Date.now() + 20_000;
  for (;;) {
    await ctx.player.openBlock(chest);
    try {
      return await ctx.client.waitForScreen(CHEST_SCREEN, { timeoutMs: 2_000 });
    } catch (error) {
      if (Date.now() > deadline) throw error;
    }
  }
}

/** Poll a server command until it passes; client actions reach the server a tick or more later. */
async function eventually(ctx: TeaKitTestContext, command: string, timeoutMs = 5_000, retry?: () => Promise<void>): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    await retry?.();
    if (((await ctx.commands.run(`/${command}`)) as { result?: number }).result) return;
    await ctx.runtime.wait(250);
  }
  throw new Error(`Timed out waiting for: ${command}`);
}
