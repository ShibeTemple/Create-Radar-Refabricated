package com.happysg.radar.block.monitor;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.registry.ModRenderTypes;
import net.minecraft.client.render.RenderLayer;
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

    private final Identifier texture;
    private RenderLayer renderLayer;

    MonitorSprite() {
        this.texture = CreateRadar.asResource("textures/monitor_sprite/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public Identifier getTexture() {
        return texture;
    }

    /** Returns a cached {@link RenderLayer} for this sprite, creating it on first use. */
    public RenderLayer getRenderLayer() {
        if (renderLayer == null) {
            renderLayer = ModRenderTypes.polygonOffset(texture);
        }
        return renderLayer;
    }
}
