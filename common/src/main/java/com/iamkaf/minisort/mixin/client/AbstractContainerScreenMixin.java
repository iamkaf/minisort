package com.iamkaf.minisort.mixin.client;

import com.iamkaf.minisort.client.ClientConfig;
import com.iamkaf.minisort.client.ClientSortOrder;
import com.iamkaf.minisort.client.MiniSortClient;
import com.iamkaf.minisort.client.MiniSortScreen;
import com.iamkaf.minisort.client.PlacedButton;
import com.iamkaf.minisort.client.ShiftClickButton;
import com.iamkaf.minisort.client.ItemGlide;
import com.iamkaf.minisort.network.MiniSortNetwork;
import com.iamkaf.minisort.network.TransferContainerPayload.Action;
import com.iamkaf.minisort.sort.SortMenuPolicy;
import com.iamkaf.minisort.sort.SortTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen implements MiniSortScreen {
    @Unique
    private static final int MIDDLE_MOUSE_BUTTON = 2;
    // The buttons stand in a column this far right of the panel, one above the next.
    @Unique
    private static final int COLUMN_GAP = 1;
    @Unique
    private static final int BUTTON_PITCH = ShiftClickButton.SIZE + 1;

    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageWidth;

    @Shadow
    protected @Nullable Slot hoveredSlot;

    //? if >=1.21.9 {
    @Shadow
    private @Nullable Slot getHoveredSlot(double x, double y) {
        throw new AssertionError();
    }
    //?} else {
    /*@Shadow
    private Slot findSlot(double x, double y) {
        throw new AssertionError();
    }
    *///?}

    @Unique
    private final List<PlacedButton> miniSort$buttons = new ArrayList<>();

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void miniSort$addButtons(CallbackInfo callbackInfo) {
        miniSort$buttons.clear();
        // Spectators can look inside containers, but the server rejects every action from them.
        // A server without Minisort cannot act on them either.
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || !MiniSortNetwork.serverSupported()) {
            return;
        }

        int x = imageWidth + COLUMN_GAP;
        if (miniSort$storage()) {
            int y = miniSort$sectionTop(false);
            ShiftClickButton.Action deposit = shift -> miniSort$transfer(shift ? Action.DEPOSIT_ALL : Action.DEPOSIT_MATCHING);
            ShiftClickButton.Action retrieve = shift -> miniSort$transfer(shift ? Action.RETRIEVE_ALL : Action.RETRIEVE_MATCHING);
            miniSort$add(x, y, "sort", shift -> miniSort$sort(SortTarget.CONTAINER),
                    "gui.minisort.sort_container", PlacedButton.Shown.ALWAYS);
            miniSort$add(x, y + BUTTON_PITCH, "deposit", deposit,
                    "gui.minisort.deposit_matching", PlacedButton.Shown.WITHOUT_SHIFT);
            miniSort$add(x, y + BUTTON_PITCH, "deposit_all", deposit,
                    "gui.minisort.deposit_all", PlacedButton.Shown.WITH_SHIFT);
            miniSort$add(x, y + 2 * BUTTON_PITCH, "retrieve", retrieve,
                    "gui.minisort.retrieve_matching", PlacedButton.Shown.WITHOUT_SHIFT);
            miniSort$add(x, y + 2 * BUTTON_PITCH, "retrieve_all", retrieve,
                    "gui.minisort.retrieve_all", PlacedButton.Shown.WITH_SHIFT);
        } else if ((Object) this instanceof InventoryScreen) {
            miniSort$add(x, miniSort$sectionTop(true), "sort", shift -> miniSort$sort(SortTarget.INVENTORY),
                    "gui.minisort.sort_inventory", PlacedButton.Shown.ALWAYS);
        }
        miniSort$updateButtons();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void miniSort$tickButtons(CallbackInfo callbackInfo) {
        miniSort$updateButtons();
    }

    /**
     * The top edge of the slots the buttons act on, from the panel's top: the container's slots, or the main
     * inventory's on the inventory screen. Lining up with the slots keeps the buttons beside them on any panel.
     */
    @Unique
    private int miniSort$sectionTop(boolean playerInventory) {
        LocalPlayer player = minecraft.player;
        int top = Integer.MAX_VALUE;
        for (Slot slot : menu.slots) {
            boolean inventory = player != null && slot.container == player.getInventory();
            if (inventory == playerInventory && (!inventory || miniSort$mainInventory(slot.getContainerSlot()))) {
                top = Math.min(top, slot.y);
            }
        }
        // A slot's frame sits one pixel outside its item.
        return top == Integer.MAX_VALUE ? 0 : top - 1;
    }

    /** Main inventory slots, leaving out the hotbar, armor, and offhand. */
    @Unique
    private static boolean miniSort$mainInventory(int containerSlot) {
        return containerSlot >= Inventory.getSelectionSize() && containerSlot < Inventory.INVENTORY_SIZE;
    }

    // Opening the recipe book slides the inventory screen sideways, so the buttons follow the panel.
    // Holding Shift shows the "everything" icons.
    @Unique
    private void miniSort$updateButtons() {
        boolean shiftDown = miniSort$shiftDown();
        for (PlacedButton placed : miniSort$buttons) {
            placed.button().setPosition(leftPos + placed.x(), topPos + placed.y());
            placed.button().visible = placed.shown().visible(shiftDown);
        }
    }

    //? if >=1.21.9 {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void miniSort$middleClickSort(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> callback) {
        if (event.button() == MIDDLE_MOUSE_BUTTON && miniSort$middleClick(getHoveredSlot(event.x(), event.y()))) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void miniSort$sortKey(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (MiniSortClient.SORT_KEY.matches(event) && miniSort$pressSortKey()) {
            callback.setReturnValue(true);
        }
    }
    //?} else {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void miniSort$middleClickSort(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button == MIDDLE_MOUSE_BUTTON && miniSort$middleClick(findSlot(mouseX, mouseY))) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void miniSort$sortKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> callback) {
        if (MiniSortClient.SORT_KEY.matches(keyCode, scanCode) && miniSort$pressSortKey()) {
            callback.setReturnValue(true);
        }
    }
    *///?}

    /** Sorts the inventory or the container the clicked slot belongs to. Creative keeps middle-click for cloning. */
    @Unique
    private boolean miniSort$middleClick(@Nullable Slot slot) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.hasInfiniteMaterials() || slot == null || !miniSort$canSort()) {
            return false;
        }
        if (slot.container == player.getInventory()) {
            miniSort$sort(SortTarget.INVENTORY);
            return true;
        }
        if (miniSort$storage()) {
            miniSort$sort(SortTarget.CONTAINER);
            return true;
        }
        return false;
    }

    /**
     * The sort key does what middle-click does, for players without a middle button. With no slot under the
     * pointer, it sorts the container, or the inventory on screens without one.
     */
    @Override
    public boolean miniSort$pressSortKey() {
        LocalPlayer player = minecraft.player;
        // The creative inventory types the key into its search box, and other screens' text fields need it too.
        if (player == null || (Object) this instanceof CreativeModeInventoryScreen || getFocused() instanceof EditBox
                || !miniSort$canSort()) {
            return false;
        }
        Slot slot = hoveredSlot;
        boolean storage = miniSort$storage();
        if (slot != null ? slot.container == player.getInventory() : !storage) {
            miniSort$sort(SortTarget.INVENTORY);
            return true;
        }
        if (storage) {
            miniSort$sort(SortTarget.CONTAINER);
            return true;
        }
        return false;
    }

    @Override
    public List<Rect2i> miniSort$buttonAreas() {
        List<Rect2i> areas = new ArrayList<>();
        for (PlacedButton placed : miniSort$buttons) {
            AbstractButton button = placed.button();
            if (button.visible) {
                areas.add(new Rect2i(button.getX(), button.getY(), button.getWidth(), button.getHeight()));
            }
        }
        return areas;
    }

    /** Whether this screen is storage Minisort acts on, unless the player turned it off for this menu. */
    @Unique
    private boolean miniSort$storage() {
        return SortMenuPolicy.supportsStorageActions(menu) && !ClientConfig.hidden(menu);
    }

    @Unique
    private boolean miniSort$canSort() {
        LocalPlayer player = minecraft.player;
        return player != null && !player.isSpectator() && menu.getCarried().isEmpty() && MiniSortNetwork.serverSupported();
    }

    @Unique
    private void miniSort$sort(SortTarget target) {
        LocalPlayer player = minecraft.player;
        List<Slot> sorted = new ArrayList<>();
        for (Slot slot : menu.slots) {
            // The same slots the server sorts: the container's, or the main inventory's.
            boolean inventory = player != null && slot.container == player.getInventory();
            if (target == SortTarget.CONTAINER ? !inventory : inventory && miniSort$mainInventory(slot.getContainerSlot())) {
                sorted.add(slot);
            }
        }
        ItemGlide.arm(menu, sorted);
        MiniSortNetwork.sort(menu.containerId, target, ClientSortOrder.of(menu, ClientConfig.sortMode()));
    }

    @Unique
    private void miniSort$transfer(Action action) {
        // A transfer moves items between the container and the inventory, so every slot of the menu takes part.
        ItemGlide.arm(menu, menu.slots);
        MiniSortNetwork.transfer(menu.containerId, action);
    }

    @Unique
    private boolean miniSort$shiftDown() {
        //? if >=1.21.9 {
        return minecraft.hasShiftDown();
        //?} else {
        /*return Screen.hasShiftDown();
        *///?}
    }

    @Unique
    private void miniSort$add(int x, int y, String glyph, ShiftClickButton.Action action, String translationKey,
                              PlacedButton.Shown shown) {
        Component title = Component.translatable(translationKey);
        MutableComponent tooltip = Component.empty()
                .append(title)
                .append("\n")
                .append(Component.translatable(translationKey + ".description").withStyle(ChatFormatting.GRAY));
        if (shown == PlacedButton.Shown.WITHOUT_SHIFT) {
            tooltip.append("\n").append(Component.translatable("gui.minisort.shift_for_all").withStyle(ChatFormatting.DARK_GRAY));
        } else if (shown == PlacedButton.Shown.ALWAYS) {
            // Only the sort buttons show always; name their shortcuts, including the key as the player bound it.
            Component shortcut = MiniSortClient.SORT_KEY.isUnbound()
                    ? Component.translatable("gui.minisort.sort_shortcut.mouse")
                    : Component.translatable("gui.minisort.sort_shortcut", MiniSortClient.SORT_KEY.getTranslatedKeyMessage());
            tooltip.append("\n").append(shortcut.copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        // The "everything" twin always does everything: a controller can fake a held Shift for the icons without
        // putting Shift on its click. The plain twin still upgrades when its own click carries Shift.
        ShiftClickButton.Action pressed = shown == PlacedButton.Shown.WITH_SHIFT ? shift -> action.press(true) : action;
        ShiftClickButton button = new ShiftClickButton(leftPos + x, topPos + y, glyph, pressed, title);
        button.setTooltip(Tooltip.create(tooltip));
        addRenderableWidget(button);
        miniSort$buttons.add(new PlacedButton(button, x, y, shown));
    }
}
