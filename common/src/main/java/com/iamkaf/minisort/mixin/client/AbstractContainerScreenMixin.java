package com.iamkaf.minisort.mixin.client;

import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.client.ButtonConfig;
import com.iamkaf.minisort.network.MiniSortNetwork;
import com.iamkaf.minisort.sort.SortMenuPolicy;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    private static final WidgetSprites SORT_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("sort_button"),
            MiniSort.resource("sort_button_highlighted")
    );
    private static final WidgetSprites DEPOSIT_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("deposit_button"),
            MiniSort.resource("deposit_button_highlighted")
    );
    private static final WidgetSprites RETRIEVE_BUTTON_SPRITES = new WidgetSprites(
            MiniSort.resource("retrieve_button"),
            MiniSort.resource("retrieve_button_highlighted")
    );

    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void miniSort$addContainerButtons(CallbackInfo callbackInfo) {
        if (!SortMenuPolicy.supportsStorageActions(menu)) {
            return;
        }

        addRenderableWidget(miniSort$button(
                ButtonConfig.SORT_X.get(),
                ButtonConfig.SORT_Y.get(),
                SORT_BUTTON_SPRITES,
                ignored -> MiniSortNetwork.sortContainer(menu.containerId),
                "gui.minisort.sort_container"
        ));
        addRenderableWidget(miniSort$button(
                ButtonConfig.DEPOSIT_X.get(),
                ButtonConfig.DEPOSIT_Y.get(),
                DEPOSIT_BUTTON_SPRITES,
                ignored -> MiniSortNetwork.depositMatching(menu.containerId),
                "gui.minisort.deposit_matching"
        ));
        addRenderableWidget(miniSort$button(
                ButtonConfig.RETRIEVE_X.get(),
                ButtonConfig.RETRIEVE_Y.get(),
                RETRIEVE_BUTTON_SPRITES,
                ignored -> MiniSortNetwork.retrieveMatching(menu.containerId),
                "gui.minisort.retrieve_matching"
        ));
    }

    private ImageButton miniSort$button(
            int x,
            int y,
            WidgetSprites sprites,
            Button.OnPress onPress,
            String translationKey
    ) {
        Component message = Component.translatable(translationKey);
        ImageButton button = new ImageButton(leftPos + x, topPos + y, 18, 18, sprites, onPress, message);
        button.setTooltip(Tooltip.create(message));
        return button;
    }
}
