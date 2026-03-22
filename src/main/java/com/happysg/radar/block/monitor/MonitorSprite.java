package com.happysg.radar.block.monitor;

import com.happysg.radar.CreateRadar;
import net.minecraft.util.Identifier;

import java.util.Locale;

public enum MonitorSprite {
    CONTRAPTION_HITBOX,
    ENTITY_HITBOX,
    PROJECTILE,
    PLAYER,
    GRID_SQUARE,
    RADAR_BG_CIRCLE,
    RADAR_BG_FILLER,
    RADAR_SWEEP,
    TARGET_SELECTED,
    TARGET_HOVERED;

    public Identifier getTexture() {
        return CreateRadar.asResource("textures/monitor_sprite/" + name().toLowerCase(Locale.ROOT) + ".png");
    }
}
