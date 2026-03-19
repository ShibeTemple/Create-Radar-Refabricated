package com.happysg.radar.item.detectionfilter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class RadarFilterScreen extends Screen {
    public RadarFilterScreen() {
        super(Text.translatable("screen.create_radar.radar_filter"));
    }
}
