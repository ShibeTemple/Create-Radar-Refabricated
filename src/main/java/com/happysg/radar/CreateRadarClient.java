package com.happysg.radar;

import com.happysg.radar.block.monitor.MonitorInputHandler;
import com.happysg.radar.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.render.RenderLayer;

public class CreateRadarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RADAR_DISH_BLOCK.get(), RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RADAR_PLATE_BLOCK.get(), RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MONITOR.get(), RenderLayer.getCutout());

        ClientTickEvents.END_CLIENT_TICK.register(MonitorInputHandler::monitorPlayerHovering);
    }
}
