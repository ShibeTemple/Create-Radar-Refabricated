package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.cannon.AutoPitchControllerBlock;
import com.happysg.radar.block.controller.cannon.AutoYawControllerBlock;
import com.happysg.radar.block.controller.cannon.DataLinkBlock;
import com.happysg.radar.block.controller.cannon.FireControllerBlock;
import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlock;
import com.happysg.radar.block.monitor.MonitorBlock;
import com.happysg.radar.block.radar.bearing.RadarBearingBlock;
import com.happysg.radar.block.radar.receiver.AbstractRadarFrame;
import com.happysg.radar.block.radar.receiver.RadarReceiverBlock;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import static com.happysg.radar.CreateRadar.REGISTRATE;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

@SuppressWarnings("removal")
public class ModBlocks {

    public static final BlockEntry<MonitorBlock> MONITOR =
            REGISTRATE.block("monitor", MonitorBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .item()
                    .build()
                    .register();

    public static final BlockEntry<RadarBearingBlock> RADAR_BEARING_BLOCK =
            REGISTRATE.block("radar_bearing", RadarBearingBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .transform(BuilderTransformers.bearing("windmill", "gearbox"))
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                    .transform(axeOrPickaxe())
                    .item()
                    .model(AssetLookup.customBlockItemModel("_", "item"))
                    .build()
                    .register();

    @SuppressWarnings("unused")
    public static final BlockEntry<RadarReceiverBlock> RADAR_RECEIVER_BLOCK =
            REGISTRATE.block("radar_receiver_block", RadarReceiverBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> prov.directionalBlock(ctx.getEntry(), prov.models()
                            .getExistingFile(ctx.getId()), 180))
                    .simpleItem()
                    .register();

    @SuppressWarnings("unused")
    public static final BlockEntry<AbstractRadarFrame> RADAR_DISH_BLOCK =
            REGISTRATE.block("radar_dish_block", properties -> new AbstractRadarFrame(properties, ModShapes.RADAR_DISH))
                    .lang("Radar Dish")
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> prov.directionalBlock(ctx.getEntry(), prov.models()
                            .getExistingFile(ctx.getId()), 0))
                    .simpleItem()
                    .register();

    @SuppressWarnings("unused")
    public static final BlockEntry<AbstractRadarFrame> RADAR_PLATE_BLOCK =
            REGISTRATE.block("radar_plate_block", properties -> new AbstractRadarFrame(properties, ModShapes.RADAR_PLATE))
                    .lang("Radar Plate")
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> prov.directionalBlock(ctx.getEntry(), prov.models()
                            .getExistingFile(ctx.getId()), 0))
                    .simpleItem()
                    .register();

    @SuppressWarnings("unused")
    public static final BlockEntry<AbstractRadarFrame> CREATIVE_RADAR_PLATE_BLOCK =
            REGISTRATE.block("creative_radar_plate", properties -> new AbstractRadarFrame(properties, ModShapes.RADAR_PLATE))
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .blockstate((ctx, prov) -> prov.directionalBlock(ctx.getEntry(), prov.models()
                            .getExistingFile(ctx.getId()), 0))
                    .transform(axeOrPickaxe())
                    .simpleItem()
                    .register();

    public static final BlockEntry<NetworkFiltererBlock> NETWORK_FILTERER_BLOCK =
            REGISTRATE.block("network_filterer", NetworkFiltererBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> prov.directionalBlock(ctx.getEntry(), prov.models()
                            .getExistingFile(ctx.getId()), 0))
                    .simpleItem()
                    .register();

    // ── CBC Weapon Controller Blocks ─────────────────────────────────────────
    // Blockstate files already exist in src/main/resources; datagen lambdas are stubs.

    public static final BlockEntry<DataLinkBlock> DATA_LINK =
            REGISTRATE.block("data_link", DataLinkBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> {}) // existing blockstate file handles variants
                    .simpleItem()
                    .register();

    public static final BlockEntry<AutoYawControllerBlock> AUTO_YAW_CONTROLLER =
            REGISTRATE.block("auto_yaw_controller", AutoYawControllerBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> {})
                    .simpleItem()
                    .register();

    public static final BlockEntry<AutoPitchControllerBlock> AUTO_PITCH_CONTROLLER =
            REGISTRATE.block("auto_pitch_controller", AutoPitchControllerBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> {})
                    .simpleItem()
                    .register();

    public static final BlockEntry<FireControllerBlock> FIRE_CONTROLLER =
            REGISTRATE.block("fire_controller", FireControllerBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.nonOpaque())
                    .properties(p -> p.strength(0.8f))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> {})
                    .simpleItem()
                    .register();

    public static void register() {
        CreateRadar.LOGGER.info("Registering blocks!");
    }
}
