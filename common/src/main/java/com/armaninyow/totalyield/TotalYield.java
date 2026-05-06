package com.armaninyow.totalyield;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotalYield implements ModInitializer {
    public static final String MOD_ID = "totalyield";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TotalYieldConfig.load();
        LOGGER.info("Total Yield loaded! Crafting output will now show total stack yield.");
    }
}