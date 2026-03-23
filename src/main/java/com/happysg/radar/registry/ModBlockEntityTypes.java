package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.cannon.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.cannon.AutoYawControllerBlockEntity;
import com.happysg.radar.block.controller.cannon.DataLinkBlockEntity;
import com.happysg.radar.block.controller.cannon.FireControllerBlockEntity;
import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.monitor.MonitorRenderer;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import com.simibubi.create.content.contraptions.bearing.BearingVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.happysg.radar.CreateRadar.REGISTRATE;

public class ModBlockEntityTypes {

    public static final BlockEntityEntry<MonitorBlockEntity> MONITOR = REGISTRATE
            .blockEntity("monitor", MonitorBlockEntity::new)
            .validBlocks(ModBlocks.MONITOR)
            .renderer(() -> MonitorRenderer::new)
            .register();

    public static final BlockEntityEntry<RadarBearingBlockEntity> RADAR_BEARING = REGISTRATE
            .blockEntity("radar_bearing", RadarBearingBlockEntity::new)
            .visual(() -> BearingVisual::new, true)
            .validBlocks(ModBlocks.RADAR_BEARING_BLOCK)
            .renderer(() -> BearingRenderer::new)
            .register();

    public static final BlockEntityEntry<NetworkFiltererBlockEntity> NETWORK_FILTERER = REGISTRATE
            .blockEntity("network_filterer", NetworkFiltererBlockEntity::new)
            .validBlocks(ModBlocks.NETWORK_FILTERER_BLOCK)
            .register();

    public static final BlockEntityEntry<DataLinkBlockEntity> DATA_LINK = REGISTRATE
            .blockEntity("data_link", DataLinkBlockEntity::new)
            .validBlocks(ModBlocks.DATA_LINK)
            .register();

    public static final BlockEntityEntry<AutoYawControllerBlockEntity> AUTO_YAW_CONTROLLER = REGISTRATE
            .blockEntity("auto_yaw_controller", AutoYawControllerBlockEntity::new)
            .validBlocks(ModBlocks.AUTO_YAW_CONTROLLER)
            .register();

    public static final BlockEntityEntry<AutoPitchControllerBlockEntity> AUTO_PITCH_CONTROLLER = REGISTRATE
            .blockEntity("auto_pitch_controller", AutoPitchControllerBlockEntity::new)
            .validBlocks(ModBlocks.AUTO_PITCH_CONTROLLER)
            .register();

    public static final BlockEntityEntry<FireControllerBlockEntity> FIRE_CONTROLLER = REGISTRATE
            .blockEntity("fire_controller", FireControllerBlockEntity::new)
            .validBlocks(ModBlocks.FIRE_CONTROLLER)
            .register();

    public static void register() {
        CreateRadar.LOGGER.info("Registering block entity types!");
    }
}
