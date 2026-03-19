package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.radar.bearing.RadarContraption;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModContraptionTypes {

    public static ContraptionType RADAR_BEARING;

    public static void register() {
        RADAR_BEARING = new ContraptionType(RadarContraption::new);
        Identifier id = CreateRadar.asResource("radar_bearing");
        Registry.register(CreateBuiltInRegistries.CONTRAPTION_TYPE, id, RADAR_BEARING);
        CreateRadar.LOGGER.info("Registered contraption type '{}'", id);
    }
}
