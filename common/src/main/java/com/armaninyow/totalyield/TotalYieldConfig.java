package com.armaninyow.totalyield;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TotalYieldConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("totalyield.json");

    private static TotalYieldConfig INSTANCE = new TotalYieldConfig();

    // ── enums ─────────────────────────────────────────────────────────────────

    /** What to display on the result slot under normal conditions (no Shift). */
    public enum DefaultDisplay {
        /** Don't interfere — vanilla renders the slot as normal. */
        VANILLA,
        /** Show the exact total number (e.g. 256). */
        EXACT,
        /** Show a stack expression (e.g. 4x64 or 3x64+32). */
        STACKS
    }

    /** What to display on the result slot while Shift is held. */
    public enum ShiftDisplay {
        /** Show the exact total number (e.g. 256). */
        EXACT,
        /** Show a stack expression (e.g. 4x64 or 3x64+32). */
        STACKS
    }

    /** The visual format used when displaying a stack expression. */
    public enum StackFormat {
        /** Uses x as the separator: 4x64 / 3x64+32 */
        MULTIPLIER,
        /** Uses s as the stack suffix: 4s / 3s+32 */
        SHORTHAND,
        /** Uses parentheses around the stack size: 4(64) / 3(64)+32 */
        GROUPED
    }

    // ── settings ──────────────────────────────────────────────────────────────

    /** What to show on the result slot by default. */
    public DefaultDisplay defaultDisplay = DefaultDisplay.VANILLA;

    /** What to show on the result slot while Shift is held. */
    public ShiftDisplay shiftDisplay = ShiftDisplay.EXACT;

    /** The format to use for stack expressions. */
    public StackFormat stackFormat = StackFormat.MULTIPLIER;

    /** Whether to animate the number when it changes. */
    public boolean animationEnabled = true;

    // ── singleton access ───────────────────────────────────────────────────────

    public static TotalYieldConfig get() {
        return INSTANCE;
    }

    // ── persistence ───────────────────────────────────────────────────────────

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            INSTANCE = GSON.fromJson(r, TotalYieldConfig.class);
            // Guard against null enum fields if config is missing new keys
            if (INSTANCE.defaultDisplay == null) INSTANCE.defaultDisplay = DefaultDisplay.VANILLA;
            if (INSTANCE.shiftDisplay   == null) INSTANCE.shiftDisplay   = ShiftDisplay.EXACT;
            if (INSTANCE.stackFormat    == null) INSTANCE.stackFormat    = StackFormat.MULTIPLIER;
            if (!INSTANCE.animationEnabled && INSTANCE.animationEnabled) INSTANCE.animationEnabled = true; // no-op guard
        } catch (IOException e) {
            TotalYield.LOGGER.error("[TotalYield] Failed to load config", e);
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, w);
        } catch (IOException e) {
            TotalYield.LOGGER.error("[TotalYield] Failed to save config", e);
        }
    }

    // ── label helper ──────────────────────────────────────────────────────────

    /**
     * Builds the display label for a given total yield and item max stack size.
     * Returns null if nothing should be shown (e.g. yield <= 1).
     *
     * @param total        the total number of items that can be crafted
     * @param maxStackSize the item's actual max stack size (e.g. 64, 16, 1)
     * @param useStacks    true = stack expression format, false = exact number
     */
    public String buildLabel(int total, int maxStackSize, boolean useStacks) {
        if (total <= 1) return null;

        // Fall back to exact if item isn't stackable or total fits in one stack
        if (!useStacks || maxStackSize <= 1 || total <= maxStackSize) {
            return String.valueOf(total);
        }

        int stacks    = total / maxStackSize;
        int remainder = total % maxStackSize;

        // Only use stack expression when there's more than one full stack
        if (stacks <= 1 && remainder == 0) {
            // Exactly one full stack — just show the number
            return String.valueOf(total);
        }

        String stackPart = formatStacks(stacks, maxStackSize);
        return remainder == 0 ? stackPart : stackPart + "+" + remainder;
    }

    private String formatStacks(int stacks, int maxStackSize) {
        switch (stackFormat) {
            case MULTIPLIER: return stacks + "x" + maxStackSize;
            case SHORTHAND:  return stacks + "s";
            case GROUPED:    return stacks + "(" + maxStackSize + ")";
            default:         return stacks + "x" + maxStackSize;
        }
    }
}