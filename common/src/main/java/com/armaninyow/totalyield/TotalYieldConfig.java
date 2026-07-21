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

    public enum DefaultDisplay {
        VANILLA,
        EXACT,
        STACKS
    }

    public enum ShiftDisplay {
        EXACT,
        STACKS
    }

    public enum StackFormat {
        MULTIPLIER,
        SHORTHAND,
        GROUPED
    }

    public DefaultDisplay defaultDisplay = DefaultDisplay.VANILLA;

    public ShiftDisplay shiftDisplay = ShiftDisplay.EXACT;

    public StackFormat stackFormat = StackFormat.MULTIPLIER;

    public boolean animationEnabled = true;

    public static TotalYieldConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            INSTANCE = GSON.fromJson(r, TotalYieldConfig.class);
            if (INSTANCE.defaultDisplay == null) INSTANCE.defaultDisplay = DefaultDisplay.VANILLA;
            if (INSTANCE.shiftDisplay   == null) INSTANCE.shiftDisplay   = ShiftDisplay.EXACT;
            if (INSTANCE.stackFormat    == null) INSTANCE.stackFormat    = StackFormat.MULTIPLIER;
            if (!INSTANCE.animationEnabled && INSTANCE.animationEnabled) INSTANCE.animationEnabled = true;
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

    public String buildLabel(int total, int maxStackSize, boolean useStacks) {
        if (total <= 1) return null;

        if (!useStacks || maxStackSize <= 1 || total <= maxStackSize) {
            return String.valueOf(total);
        }

        int stacks    = total / maxStackSize;
        int remainder = total % maxStackSize;

        if (stacks <= 1 && remainder == 0) {
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