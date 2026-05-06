package com.armaninyow.totalyield.mixin;

import com.armaninyow.totalyield.TotalYieldConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.CrafterMenu;
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

// 1.21.6_1.21.10
@Mixin(AbstractContainerScreen.class)
public class CraftingScreenMixin {

    @Shadow protected int imageWidth;

    @Unique private int     totalyield$animFrom      = 0;
    @Unique private int     totalyield$animTo        = -1;  // -1 = never seen a yield yet
    @Unique private float   totalyield$animTick      = 10f;
    @Unique private boolean totalyield$wasVisible    = false;
    @Unique private int     totalyield$lastUseStacks = -1;  // -1 = never set; 0 = false; 1 = true
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
        try {
            com.mojang.blaze3d.platform.Window win = Minecraft.getInstance().getWindow();
            // Get the raw GLFW handle via reflection — avoids InputConstants API differences
            // across 1.21.6–1.21.10 where the method signature changed mid-range
            java.lang.reflect.Field[] fields = win.getClass().getDeclaredFields();
            long handle = 0L;
            for (java.lang.reflect.Field f : fields) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    long val = f.getLong(win);
                    if (val != 0L) { handle = val; break; }
                }
            }
            if (handle == 0L) return false;
            return org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)  == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return false;
        }
    }

    @Inject(at = @At("HEAD"), method = "renderSlot", cancellable = true)
    private void totalyield$renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        boolean isCrafterResult = (self.getMenu() instanceof CrafterMenu)
            && slot.getClass().getSimpleName().equals("NonInteractiveResultSlot");
        if (!(slot instanceof ResultSlot) && !isCrafterResult) return;

        TotalYieldConfig cfg = TotalYieldConfig.get();
        boolean shiftHeld = totalyield$isShiftHeld();

        // ── Resolve display mode ───────────────────────────────────────────────
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

        // ── Visibility transition — re-trigger animation when first becoming visible
        if (!totalyield$wasVisible) {
            totalyield$animTo        = -1;
            totalyield$animFrom      = 0;
            totalyield$animTick      = ANIM_TICKS;
            totalyield$lastUseStacks = -1;
        }
        totalyield$wasVisible = true;

        // ── Empty slot — reset state ───────────────────────────────────────────
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            totalyield$animTo        = -1;
            totalyield$animFrom      = 0;
            totalyield$animTick      = ANIM_TICKS;
            totalyield$wasVisible    = false;
            totalyield$lastUseStacks = -1;
            return;
        }

        // ── Ingredient scan ───────────────────────────────────────────────────
        int ingredientStart, ingredientEnd;
        if (self.getMenu() instanceof CraftingMenu) {
            ingredientStart = 1; ingredientEnd = 9;
        } else if (self.getMenu() instanceof CrafterMenu) {
            ingredientStart = 0; ingredientEnd = 8;
        } else if (self.getMenu() instanceof InventoryMenu) {
            ingredientStart = 1; ingredientEnd = 4;
        } else {
            return;
        }

        int maxCrafts         = Integer.MAX_VALUE;
        boolean hasIngredient = false;
        for (int i = ingredientStart; i <= ingredientEnd; i++) {
            ItemStack ing = self.getMenu().slots.get(i).getItem();
            if (!ing.isEmpty()) {
                hasIngredient = true;
                maxCrafts = Math.min(maxCrafts, ing.getCount());
            }
        }
        if (!hasIngredient || maxCrafts == Integer.MAX_VALUE) return;

        int realYield = stack.getCount() * maxCrafts;

        String label = cfg.buildLabel(realYield, stack.getMaxStackSize(), useStacks);
        if (label == null) return;

        // ── Animate — trigger on yield change OR display mode change ──────────
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

        // ── Render ────────────────────────────────────────────────────────────
        int slotX = slot.x;
        int slotY = slot.y;
        int seed  = slotX + slotY * imageWidth;

        graphics.renderItem(stack, slotX, slotY, seed);

        Minecraft mc  = Minecraft.getInstance();
        int textW     = mc.font.width(label);
        int textX     = slotX + 19 - 2 - textW;
        int textY     = slotY + 6 + 3;

        // Anchor at bottom-right — zooms toward top-left
        float px = textX + textW;
        float py = textY + mc.font.lineHeight;

        graphics.pose().pushMatrix();
        graphics.pose().translate(px, py);
        graphics.pose().scale(zoom, zoom);
        graphics.pose().translate(-px, -py);

        graphics.renderItemDecorations(mc.font, stack, slotX, slotY, label);

        graphics.pose().popMatrix();

        ci.cancel();
    }
}