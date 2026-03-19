package com.happysg.radar.compat.cbc;

import com.happysg.radar.CreateRadar;

public class CBCCompatRegister {

    public static void registerCBC() {
        CreateRadar.LOGGER.info("Registering CBC Compat!");
        // GuidedFuzeItem registration deferred until NetworkFiltererBlockEntity is ported
    }
}
