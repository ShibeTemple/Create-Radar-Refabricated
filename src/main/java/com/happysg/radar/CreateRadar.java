package com.happysg.radar;

import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.CBCCompatRegister;
import com.happysg.radar.config.RadarConfig;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.happysg.radar.registry.ModBlocks;
import com.happysg.radar.registry.ModContraptionTypes;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRadar implements ModInitializer {

    public static final String MODID = "create_radar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Create Radar (Fabric)!");

        ModBlocks.register();
        ModBlockEntityTypes.register();
        RadarConfig.register(MODID);

        ModContraptionTypes.register();

        Mods.CREATEBIGCANNONS.executeIfInstalled(() -> CBCCompatRegister::registerCBC);
    }

    public static Identifier asResource(String path) {
        return new Identifier(MODID, path);
    }

    static {
        REGISTRATE.setTooltipModifierFactory((item) ->
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
    }
}
