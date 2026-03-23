package com.happysg.radar.compat.cbc;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.happysg.radar.registry.ModBlocks;

public class CBCCompatRegister {

    public static void registerCBC() {
        CreateRadar.LOGGER.info("Registering CBC Compat: DataLink, AutoYaw, AutoPitch, FireController");

        // Touch the block/BE entries to force eager initialisation.
        // (Registrate is lazy — referencing them here ensures they are registered
        //  before any CBC-specific hook tries to look them up.)
        ModBlocks.DATA_LINK.getId();
        ModBlocks.AUTO_YAW_CONTROLLER.getId();
        ModBlocks.AUTO_PITCH_CONTROLLER.getId();
        ModBlocks.FIRE_CONTROLLER.getId();
        ModBlockEntityTypes.DATA_LINK.getId();
        ModBlockEntityTypes.AUTO_YAW_CONTROLLER.getId();
        ModBlockEntityTypes.AUTO_PITCH_CONTROLLER.getId();
        ModBlockEntityTypes.FIRE_CONTROLLER.getId();

        CreateRadar.LOGGER.info("CBC cannon targeting blocks active. Place adjacent to a CannonMount and pair with binoculars.");
    }
}
