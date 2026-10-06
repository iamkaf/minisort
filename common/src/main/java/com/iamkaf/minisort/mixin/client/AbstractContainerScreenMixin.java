package com.iamkaf.minisort.mixin.client;

import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.client.ClientConfig;
import com.iamkaf.minisort.client.PlacedButton;
import com.iamkaf.minisort.client.ShiftClickButton;
import com.iamkaf.minisort.network.MiniSortNetwork;
import com.iamkaf.minisort.network.TransferContainerPayload.Action;
import com.iamkaf.minisort.sort.SortMenuPolicy;
import com.iamkaf.minisort.sort.SortTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
public abstract class AbstractContainerScreenMixin extends Screen {
    @Unique
    private static final int MIDDLE_MOUSE_BUTTON = 2;
    @Unique
    private static final WidgetSprites SORT_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("sort_button"),
            MiniSort.resource("sort_button_highlighted")
    );
    @Unique
    private static final WidgetSprites DEPOSIT_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("deposit_button"),
            MiniSort.resource("deposit_button_highlighted")
    );
    @Unique
    private static final WidgetSprites RETRIEVE_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("retrieve_button"),
            MiniSort.resource("retrieve_button_highlighted")
    );
    @Unique
    private static final WidgetSprites DEPOSIT_ALL_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("deposit_all_button"),
            MiniSort.resource("deposit_all_button_highlighted")
    );
    @Unique
    private static final WidgetSprites RETRIEVE_ALL_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("retrieve_all_button"),
            MiniSort.resource("retrieve_all_button_highlighted")
    );

    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

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
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        if (SortMenuPolicy.supportsStorageActions(menu)) {
            int depositX = ClientConfig.DEPOSIT_X.get();
            int depositY = ClientConfig.DEPOSIT_Y.get();
            int retrieveX = ClientConfig.RETRIEVE_X.get();
            int retrieveY = ClientConfig.RETRIEVE_Y.get();
            ShiftClickButton.Action deposit = shift -> miniSort$transfer(shift ? Action.DEPOSIT_ALL : Action.DEPOSIT_MATCHING);
            ShiftClickButton.Action retrieve = shift -> miniSort$transfer(shift ? Action.RETRIEVE_ALL : Action.RETRIEVE_MATCHING);
            miniSort$add(ClientConfig.SORT_X.get(), ClientConfig.SORT_Y.get(), SORT_BUTTON_SPRITES,
                    shift -> miniSort$sort(SortTarget.CONTAINER), "gui.minisort.sort_container", PlacedButton.Shown.ALWAYS);
            miniSort$add(depositX, depositY, DEPOSIT_BUTTON_SPRITES, deposit,
                    "gui.minisort.deposit_matching", PlacedButton.Shown.WITHOUT_SHIFT);
            miniSort$add(depositX, depositY, DEPOSIT_ALL_BUTTON_SPRITES, deposit,
                    "gui.minisort.deposit_all", PlacedButton.Shown.WITH_SHIFT);
            miniSort$add(retrieveX, retrieveY, RETRIEVE_BUTTON_SPRITES, retrieve,
                    "gui.minisort.retrieve_matching", PlacedButton.Shown.WITHOUT_SHIFT);
            miniSort$add(retrieveX, retrieveY, RETRIEVE_ALL_BUTTON_SPRITES, retrieve,
                    "gui.minisort.retrieve_all", PlacedButton.Shown.WITH_SHIFT);
        } else if ((Object) this instanceof InventoryScreen) {
            miniSort$add(ClientConfig.SORT_X.get(), ClientConfig.SORT_Y.get(), SORT_BUTTON_SPRITES,
                    shift -> miniSort$sort(SortTarget.INVENTORY), "gui.minisort.sort_inventory", PlacedButton.Shown.ALWAYS);
        }
        miniSort$updateButtons();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void miniSort$tickButtons(CallbackInfo callbackInfo) {
        miniSort$updateButtons();
    }

    // Opening the recipe book slides the inventory screen sideways, so the buttons follow its corner.
    // Holding Shift shows the "everything" icons. Each press reads Shift from its own click.
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
        if (event.button() == MIDDLE_MOUSE_BUTTON && miniSort$sortSection(getHoveredSlot(event.x(), event.y()))) {
            callback.setReturnValue(true);
        }
    }
    //?} else {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void miniSort$middleClickSort(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button == MIDDLE_MOUSE_BUTTON && miniSort$sortSection(findSlot(mouseX, mouseY))) {
            callback.setReturnValue(true);
        }
    }
    *///?}

    /** Sorts the inventory or the container the clicked slot belongs to. Creative keeps middle-click for cloning. */
    @Unique
    private boolean miniSort$sortSection(@Nullable Slot slot) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || player.hasInfiniteMaterials()
                || slot == null || !menu.getCarried().isEmpty()) {
            return false;
        }
        if (slot.container == player.getInventory()) {
            miniSort$sort(SortTarget.INVENTORY);
            return true;
        }
        if (SortMenuPolicy.supportsStorageActions(menu)) {
            miniSort$sort(SortTarget.CONTAINER);
            return true;
        }
        return false;
    }

    @Unique
    private void miniSort$sort(SortTarget target) {
        MiniSortNetwork.sort(menu.containerId, target, ClientConfig.sortMode());
    }

    @Unique
    private void miniSort$transfer(Action action) {
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
    private void miniSort$add(int x, int y, WidgetSprites sprites, ShiftClickButton.Action action, String translationKey,
                              PlacedButton.Shown shown) {
        Component title = Component.translatable(translationKey);
        MutableComponent tooltip = Component.empty()
                .append(title)
                .append("\n")
                .append(Component.translatable(translationKey + ".description").withStyle(ChatFormatting.GRAY));
        if (shown == PlacedButton.Shown.WITHOUT_SHIFT) {
            tooltip.append("\n").append(Component.translatable("gui.minisort.shift_for_all").withStyle(ChatFormatting.DARK_GRAY));
        }
        ShiftClickButton button = new ShiftClickButton(leftPos + x, topPos + y, sprites, action, title);
        button.setTooltip(Tooltip.create(tooltip));
        addRenderableWidget(button);
        miniSort$buttons.add(new PlacedButton(button, x, y, shown));
    }
}
