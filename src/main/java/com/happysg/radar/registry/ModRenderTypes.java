package com.happysg.radar.registry;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ModRenderTypes {

    private static final Map<Identifier, RenderLayer> CACHE = new HashMap<>();

    /**
     * Returns a RenderLayer for rendering monitor sprites on block faces.
     * Uses polygon offset layering so sprites render in front of the block face,
     * and disables backface culling so all monitor orientations work correctly.
     */
    public static RenderLayer polygonOffset(Identifier texture) {
        return CACHE.computeIfAbsent(texture, tex -> RenderLayer.of(
                "create_radar:monitor_overlay",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256,
                false,
                true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.ENTITY_TRANSLUCENT_PROGRAM)
                        .texture(new RenderPhase.Texture(tex, false, false))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .layering(RenderPhase.POLYGON_OFFSET_LAYERING)
                        .build(true)
        ));
    }
}
