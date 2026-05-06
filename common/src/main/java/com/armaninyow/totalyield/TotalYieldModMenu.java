package com.armaninyow.totalyield;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public class TotalYieldModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            TotalYieldConfig cfg = TotalYieldConfig.get();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("totalyield.config.title"))
                    .setSavingRunnable(TotalYieldConfig::save);

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(
                    Component.translatable("totalyield.config.category.general"));

            // ── Default display ────────────────────────────────────────────
            general.addEntry(entries
                    .startEnumSelector(
                            Component.translatable("totalyield.config.default_display"),
                            TotalYieldConfig.DefaultDisplay.class,
                            cfg.defaultDisplay)
                    .setDefaultValue(TotalYieldConfig.DefaultDisplay.VANILLA)
                    .setTooltip(Component.translatable("totalyield.config.default_display.tooltip"))
                    .setEnumNameProvider(e -> Component.translatable(
                            "totalyield.config.default_display." + e.name().toLowerCase()))
                    .setSaveConsumer(v -> cfg.defaultDisplay = v)
                    .build());

            // ── Shift display ──────────────────────────────────────────────
            general.addEntry(entries
                    .startEnumSelector(
                            Component.translatable("totalyield.config.shift_display"),
                            TotalYieldConfig.ShiftDisplay.class,
                            cfg.shiftDisplay)
                    .setDefaultValue(TotalYieldConfig.ShiftDisplay.EXACT)
                    .setTooltip(Component.translatable("totalyield.config.shift_display.tooltip"))
                    .setEnumNameProvider(e -> Component.translatable(
                            "totalyield.config.shift_display." + e.name().toLowerCase()))
                    .setSaveConsumer(v -> cfg.shiftDisplay = v)
                    .build());

            // ── Stack format ───────────────────────────────────────────────
            general.addEntry(entries
                    .startEnumSelector(
                            Component.translatable("totalyield.config.stack_format"),
                            TotalYieldConfig.StackFormat.class,
                            cfg.stackFormat)
                    .setDefaultValue(TotalYieldConfig.StackFormat.MULTIPLIER)
                    .setTooltip(Component.translatable("totalyield.config.stack_format.tooltip"))
                    .setEnumNameProvider(e -> Component.translatable(
                            "totalyield.config.stack_format." + e.name().toLowerCase()))
                    .setSaveConsumer(v -> cfg.stackFormat = v)
                    .build());

            // ── Animation ──────────────────────────────────────────────────
            general.addEntry(entries
                    .startBooleanToggle(
                            Component.translatable("totalyield.config.animation_enabled"),
                            cfg.animationEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("totalyield.config.animation_enabled.tooltip"))
                    .setSaveConsumer(v -> cfg.animationEnabled = v)
                    .build());

            return builder.build();
        };
    }
}