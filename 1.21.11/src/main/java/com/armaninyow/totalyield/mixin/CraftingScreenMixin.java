package com.armaninyow.totalyield.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class CraftingScreenMixin {

    @Shadow protected int imageWidth;

    @Unique private int   totalyield$animFrom = 0;
    @Unique private int   totalyield$animTo   = 0;
    @Unique private float totalyield$animTick = 10f;
    @Unique private static final float ANIM_TICKS = 10f;

    @Unique
    private int totalyield$displayedYield() {
        if (totalyield$animTick >= ANIM_TICKS) return totalyield$animTo;
        float t = totalyield$animTick / ANIM_TICKS;
        return Math.round(totalyield$animFrom + (totalyield$animTo - totalyield$animFrom) * t);
    }

    @Unique
    private float totalyield$zoomScale() {
        if (totalyield$animTick >= ANIM_TICKS) return 1.0f;
        float sine = (float) Math.sin(Math.PI * (totalyield$animTick / ANIM_TICKS));
        return 1.0f + 0.25f * sine;
    }

    // Matrix3x2fStack, extra int params are mouse x/y — use slot.x/slot.y for rendering (1.21.11)
    @Inject(at = @At("HEAD"), method = "renderSlot", cancellable = true)
    private void totalyield$renderSlot(GuiGraphics graphics, Slot slot, int i, int j, CallbackInfo ci) {
        if (!(slot instanceof ResultSlot)) return;

        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            totalyield$animTo   = 0;
            totalyield$animFrom = 0;
            totalyield$animTick = ANIM_TICKS;
            return;
        }

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;

        int ingredientStart, ingredientEnd;
        if (self.getMenu() instanceof CraftingMenu) {
            ingredientStart = 1; ingredientEnd = 9;
        } else if (self.getMenu() instanceof InventoryMenu) {
            ingredientStart = 1; ingredientEnd = 4;
        } else {
            return;
        }

        int maxCrafts         = Integer.MAX_VALUE;
        boolean hasIngredient = false;
        for (int idx = ingredientStart; idx <= ingredientEnd; idx++) {
            ItemStack ing = self.getMenu().slots.get(idx).getItem();
            if (!ing.isEmpty()) {
                hasIngredient = true;
                maxCrafts = Math.min(maxCrafts, ing.getCount());
            }
        }
        if (!hasIngredient || maxCrafts == Integer.MAX_VALUE) return;

        int realYield = stack.getCount() * maxCrafts;
        if (realYield <= 1) return;

        if (realYield != totalyield$animTo) {
            totalyield$animFrom = totalyield$displayedYield();
            totalyield$animTo   = realYield;
            totalyield$animTick = 0f;
        }

        if (totalyield$animTick < ANIM_TICKS) {
            totalyield$animTick = Math.min(totalyield$animTick + 1f, ANIM_TICKS);
        }

        int    displayedYield = totalyield$displayedYield();
        float  zoom           = totalyield$zoomScale();
        String label          = String.valueOf(displayedYield);

        int slotX = slot.x;
        int slotY = slot.y;
        int seed  = slotX + slotY * imageWidth;

        graphics.renderItem(stack, slotX, slotY, seed);

        Minecraft mc  = Minecraft.getInstance();
        int textW     = mc.font.width(label);
        int textX     = slotX + 19 - 2 - textW;
        int textY     = slotY + 6 + 3;
        float cx      = textX + textW * 0.5f;
        float cy      = textY + mc.font.lineHeight * 0.5f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(zoom, zoom);
        graphics.pose().translate(-cx, -cy);

        graphics.renderItemDecorations(mc.font, stack, slotX, slotY, label);

        graphics.pose().popMatrix();

        ci.cancel();
    }
}