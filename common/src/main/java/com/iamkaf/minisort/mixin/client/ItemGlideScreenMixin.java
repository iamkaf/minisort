package com.iamkaf.minisort.mixin.client;

import com.iamkaf.minisort.client.ItemGlide;
//? if >=1.21.11 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
//?}
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the slots Minisort moved on their way to their new positions; {@link ItemGlide} decides what moves. A moving slot is
 * drawn whole, translated, so whatever other mods draw on that slot moves with it. Nothing is cancelled.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ItemGlideScreenMixin {
    @Shadow
    protected AbstractContainerMenu menu;

    //? if >=1.21.11 {
    // Moving slots draw after the resting ones, so a glide passes over the items it crosses.
    @Unique
    private final List<Slot> miniSort$moving = new ArrayList<>();
    @Unique
    private boolean miniSort$drawingMoving;
    // A screen that draws its own slots never reaches the replay, so its moving slots slide in place instead.
    @Unique
    private boolean miniSort$inPlace;
    //?}

    //? if >=26.1 {
    @Shadow
    protected abstract void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY);

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void miniSort$glideFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        miniSort$startFrame();
    }

    @WrapMethod(method = "extractSlot")
    private void miniSort$glideSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, Operation<Void> original) {
        ItemGlide.Offset offset = ItemGlide.offset(slot);
        if (offset == null) {
            original.call(graphics, slot, mouseX, mouseY);
        } else if (!miniSort$drawingMoving && !miniSort$inPlace) {
            miniSort$moving.add(slot);
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(offset.x(), offset.y());
            try {
                original.call(graphics, slot, mouseX, mouseY);
            } finally {
                graphics.pose().popMatrix();
            }
        }
    }

    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void miniSort$glideMovingSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo callback) {
        miniSort$drawingMoving = true;
        try {
            for (Slot slot : miniSort$moving) {
                extractSlot(graphics, slot, mouseX, mouseY);
            }
        } finally {
            miniSort$drawingMoving = false;
            miniSort$moving.clear();
        }
        for (ItemGlide.Ghost ghost : ItemGlide.ghosts()) {
            ItemGlide.Offset offset = ItemGlide.ghostOffset(ghost);
            graphics.pose().pushMatrix();
            graphics.pose().translate(offset.x(), offset.y());
            graphics.item(ghost.item(), ghost.to().x, ghost.to().y);
            graphics.pose().popMatrix();
        }
    }
    //?} else if >=1.21.11 {
    /*@Shadow
    protected abstract void renderSlot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY);

    @Inject(method = "renderContents", at = @At("HEAD"))
    private void miniSort$glideFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        miniSort$startFrame();
    }

    @WrapMethod(method = "renderSlot")
    private void miniSort$glideSlot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, Operation<Void> original) {
        ItemGlide.Offset offset = ItemGlide.offset(slot);
        if (offset == null) {
            original.call(graphics, slot, mouseX, mouseY);
        } else if (!miniSort$drawingMoving && !miniSort$inPlace) {
            miniSort$moving.add(slot);
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(offset.x(), offset.y());
            try {
                original.call(graphics, slot, mouseX, mouseY);
            } finally {
                graphics.pose().popMatrix();
            }
        }
    }

    @Inject(method = "renderSlots", at = @At("TAIL"))
    private void miniSort$glideMovingSlots(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback) {
        miniSort$drawingMoving = true;
        try {
            for (Slot slot : miniSort$moving) {
                renderSlot(graphics, slot, mouseX, mouseY);
            }
        } finally {
            miniSort$drawingMoving = false;
            miniSort$moving.clear();
        }
        for (ItemGlide.Ghost ghost : ItemGlide.ghosts()) {
            ItemGlide.Offset offset = ItemGlide.ghostOffset(ghost);
            graphics.pose().pushMatrix();
            graphics.pose().translate(offset.x(), offset.y());
            graphics.renderItem(ghost.item(), ghost.to().x, ghost.to().y);
            graphics.pose().popMatrix();
        }
    }
    *///?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void miniSort$glideFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        ItemGlide.frame(menu);
    }

    // Inside renderSlot's own push and pop, right after it lifts the slot's items: raised further, a moving slot
    // draws over resting items and still under the carried item and tooltips.
    @Inject(method = "renderSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void miniSort$glideSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        ItemGlide.Offset offset = ItemGlide.offset(slot);
        if (offset != null) {
            graphics.pose().translate(offset.x(), offset.y(), 75F);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void miniSort$glideGhosts(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        for (ItemGlide.Ghost ghost : ItemGlide.ghosts()) {
            ItemGlide.Offset offset = ItemGlide.ghostOffset(ghost);
            graphics.pose().pushPose();
            graphics.pose().translate(offset.x(), offset.y(), 175F);
            graphics.renderItem(ghost.item(), ghost.to().x, ghost.to().y);
            graphics.pose().popPose();
        }
    }
    *///?}

    //? if >=1.21.11 {
    @Unique
    private void miniSort$startFrame() {
        if (!miniSort$moving.isEmpty()) {
            miniSort$inPlace = true;
            miniSort$moving.clear();
        }
        ItemGlide.frame(menu);
    }
    //?}
}
