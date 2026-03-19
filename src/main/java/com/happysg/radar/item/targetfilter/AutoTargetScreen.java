package com.happysg.radar.item.targetfilter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class AutoTargetScreen extends Screen {
    public AutoTargetScreen() {
        super(Text.translatable("screen.create_radar.auto_target"));
    }
}
