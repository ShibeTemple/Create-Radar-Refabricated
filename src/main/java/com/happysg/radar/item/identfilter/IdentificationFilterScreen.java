package com.happysg.radar.item.identfilter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class IdentificationFilterScreen extends Screen {
    public IdentificationFilterScreen() {
        super(Text.translatable("screen.create_radar.ident_filter"));
    }
}
