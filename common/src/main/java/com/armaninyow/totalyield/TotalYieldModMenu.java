package com.armaninyow.totalyield;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TotalYieldModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            TotalYieldConfig cfg = TotalYieldConfig.get();

            return YetAnotherConfigLib.createBuilder()
                    .title(Component.translatable("totalyield.config.title"))
                    .category(ConfigCategory.createBuilder()
                            .name(Component.translatable("totalyield.config.category.general"))

                            .option(Option.<TotalYieldConfig.DefaultDisplay>createBuilder()
                                    .name(Component.translatable("totalyield.config.default_display"))
                                    .description(OptionDescription.of(
                                            Component.translatable("totalyield.config.default_display.tooltip")))
                                    .binding(TotalYieldConfig.DefaultDisplay.VANILLA,
                                            () -> cfg.defaultDisplay,
                                            v -> cfg.defaultDisplay = v)
                                    .controller(opt -> CyclingListControllerBuilder.create(opt)
                                            .values(List.of(TotalYieldConfig.DefaultDisplay.values()))
                                            .formatValue(v -> Component.translatable(
                                                    "totalyield.config.default_display." + v.name().toLowerCase())))
                                    .build())

                            .option(Option.<TotalYieldConfig.ShiftDisplay>createBuilder()
                                    .name(Component.translatable("totalyield.config.shift_display"))
                                    .description(OptionDescription.of(
                                            Component.translatable("totalyield.config.shift_display.tooltip")))
                                    .binding(TotalYieldConfig.ShiftDisplay.EXACT,
                                            () -> cfg.shiftDisplay,
                                            v -> cfg.shiftDisplay = v)
                                    .controller(opt -> CyclingListControllerBuilder.create(opt)
                                            .values(List.of(TotalYieldConfig.ShiftDisplay.values()))
                                            .formatValue(v -> Component.translatable(
                                                    "totalyield.config.shift_display." + v.name().toLowerCase())))
                                    .build())

                            .option(Option.<TotalYieldConfig.StackFormat>createBuilder()
                                    .name(Component.translatable("totalyield.config.stack_format"))
                                    .description(OptionDescription.of(
                                            Component.translatable("totalyield.config.stack_format.tooltip")))
                                    .binding(TotalYieldConfig.StackFormat.MULTIPLIER,
                                            () -> cfg.stackFormat,
                                            v -> cfg.stackFormat = v)
                                    .controller(opt -> CyclingListControllerBuilder.create(opt)
                                            .values(List.of(TotalYieldConfig.StackFormat.values()))
                                            .formatValue(v -> Component.translatable(
                                                    "totalyield.config.stack_format." + v.name().toLowerCase())))
                                    .build())

                            .option(Option.<Boolean>createBuilder()
                                    .name(Component.translatable("totalyield.config.animation_enabled"))
                                    .description(OptionDescription.of(
                                            Component.translatable("totalyield.config.animation_enabled.tooltip")))
                                    .binding(true,
                                            () -> cfg.animationEnabled,
                                            v -> cfg.animationEnabled = v)
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())

                            .build())
                    .save(TotalYieldConfig::save)
                    .build()
                    .generateScreen(parent);
        };
    }
}