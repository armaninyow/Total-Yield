package com.armaninyow.totalyield.mixin;

import com.armaninyow.totalyield.TotalYieldConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class CraftingScreenMixin {

    @Unique private int     totalyield$animFrom      = 0;
    @Unique private int     totalyield$animTo        = -1;
    @Unique private float   totalyield$animTick      = 10f;
    @Unique private boolean totalyield$wasVisible    = false;
    @Unique private int     totalyield$lastUseStacks = -1;
    @Unique private static final float ANIM_TICKS    = 10f;

    @Unique
    private int totalyield$displayedYield() {
        TotalYieldConfig cfg = TotalYieldConfig.get();
        if (!cfg.animationEnabled || totalyield$animTick >= ANIM_TICKS) return totalyield$animTo;
        float t = totalyield$animTick / ANIM_TICKS;
        return Math.round(totalyield$animFrom + (totalyield$animTo - totalyield$animFrom) * t);
    }

    @Unique
    private float totalyield$zoomScale() {
        TotalYieldConfig cfg = TotalYieldConfig.get();
        if (!cfg.animationEnabled || totalyield$animTick >= ANIM_TICKS) return 1.0f;
        float sine = (float) Math.sin(Math.PI * (totalyield$animTick / ANIM_TICKS));
        return 1.0f + 0.25f * sine;
    }

    @Unique
    private boolean totalyield$isShiftHeld() {
        com.mojang.blaze3d.platform.Window win = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(win, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(win, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Inject(at = @At("HEAD"), method = "extractSlot", cancellable = true)
    private void totalyield$renderSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        boolean isCrafterResult = (self.getMenu() instanceof CrafterMenu)
            && slot.getClass().getSimpleName().equals("NonInteractiveResultSlot");
        boolean isStonecutterResult = (self.getMenu() instanceof StonecutterMenu)
            && slot.index == 1;
        if (!(slot instanceof ResultSlot) && !isCrafterResult && !isStonecutterResult) return;

        TotalYieldConfig cfg = TotalYieldConfig.get();
        boolean shiftHeld = totalyield$isShiftHeld();

        boolean useStacks;
        if (shiftHeld) {
            useStacks = cfg.shiftDisplay == TotalYieldConfig.ShiftDisplay.STACKS;
        } else {
            switch (cfg.defaultDisplay) {
                case VANILLA: totalyield$wasVisible = false; return;
                case STACKS:  useStacks = true;  break;
                case EXACT:
                default:      useStacks = false; break;
            }
        }

        if (!totalyield$wasVisible) {
            totalyield$animTo        = -1;
            totalyield$animFrom      = 0;
            totalyield$animTick      = ANIM_TICKS;
            totalyield$lastUseStacks = -1;
        }
        totalyield$wasVisible = true;

        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            totalyield$animTo        = -1;
            totalyield$animFrom      = 0;
            totalyield$animTick      = ANIM_TICKS;
            totalyield$wasVisible    = false;
            totalyield$lastUseStacks = -1;
            return;
        }

        int ingredientStart, ingredientEnd;
        if (self.getMenu() instanceof CraftingMenu) {
            ingredientStart = 1; ingredientEnd = 9;
        } else if (self.getMenu() instanceof CrafterMenu) {
            ingredientStart = 0; ingredientEnd = 8;
        } else if (self.getMenu() instanceof InventoryMenu) {
            ingredientStart = 1; ingredientEnd = 4;
        } else if (self.getMenu() instanceof StonecutterMenu) {
            ingredientStart = 0; ingredientEnd = 0;
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

        String label = cfg.buildLabel(realYield, stack.getMaxStackSize(), useStacks);
        if (label == null) return;

        int     useStacksInt = useStacks ? 1 : 0;
        boolean modeChanged  = totalyield$lastUseStacks != -1 && totalyield$lastUseStacks != useStacksInt;

        if (realYield != totalyield$animTo || modeChanged) {
            totalyield$animFrom = (totalyield$animTo < 0) ? 0 : totalyield$displayedYield();
            totalyield$animTo   = realYield;
            totalyield$animTick = 0f;
        }
        totalyield$lastUseStacks = useStacksInt;

        if (cfg.animationEnabled && totalyield$animTick < ANIM_TICKS) {
            totalyield$animTick = Math.min(totalyield$animTick + 1f, ANIM_TICKS);
        } else if (!cfg.animationEnabled) {
            totalyield$animTick = ANIM_TICKS;
        }

        int   animatedYield = totalyield$displayedYield();
        float zoom          = totalyield$zoomScale();

        label = cfg.buildLabel(animatedYield, stack.getMaxStackSize(), useStacks);
        if (label == null) label = String.valueOf(animatedYield);

        int slotX = slot.x;
        int slotY = slot.y;

        graphics.item(stack, slotX, slotY);

        Minecraft mc  = Minecraft.getInstance();
        int textW     = mc.font.width(label);
        int textX     = slotX + 19 - 2 - textW;
        int textY     = slotY + 6 + 3;

        float px = textX + textW;
        float py = textY + mc.font.lineHeight;

        graphics.pose().pushMatrix();
        graphics.pose().translate(px, py);
        graphics.pose().scale(zoom, zoom);
        graphics.pose().translate(-px, -py);

        graphics.itemDecorations(mc.font, stack, slotX, slotY, label);

        graphics.pose().popMatrix();

        ci.cancel();
    }
}