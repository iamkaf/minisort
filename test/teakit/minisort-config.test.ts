import { Capability, Readiness, describe, expect, test } from "@teakit/test";
import type { ClientScreen, TeaKitTestContext } from "@teakit/test";

describe.configure({
  timeout: "4m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [Capability.ClientScreen, Capability.ClientScreens],
});

const configScreen = "com.iamkaf.konfig.impl.v1.client.screen.KonfigConfigScreen";

describe("Minisort config", () => {
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
    const labels = screen.lists().entries().map((entry) => entry.label);
    expect(labels).toContain("Sort Mode");
    expect(labels).toContain("Sort Button X");
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
