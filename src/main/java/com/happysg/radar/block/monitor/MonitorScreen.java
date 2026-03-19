package com.happysg.radar.block.monitor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class MonitorScreen extends Screen {

    private final BlockPos controllerPos;

    public MonitorScreen(BlockPos controllerPos) {
        super(Text.literal("Monitor"));
        this.controllerPos = controllerPos;
    }

    // TODO: implement monitor screen rendering
}
