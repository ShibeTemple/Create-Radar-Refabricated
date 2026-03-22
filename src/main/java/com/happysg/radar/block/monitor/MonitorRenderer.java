package com.happysg.radar.block.monitor;

import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.TrackCategory;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.happysg.radar.config.RadarConfig;
import com.happysg.radar.registry.ModRenderTypes;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitorRenderer extends SmartBlockEntityRenderer<MonitorBlockEntity> {

    private static final float DEPTH_BACKGROUND = 0.94f;
    private static final float DEPTH_GRID = 0.945f;
    private static final float DEPTH_SWEEP = 0.947f;
    private static final float DEPTH_TRACK_BASE = 0.95f;
    private static final float DEPTH_TRACK_INCREMENT = 0.0001f;
    private static final float LABEL_SCALE = 0.003f;
    private static final float LABEL_Z_OFFSET = 0.03f;
    private static final float LABEL_DEPTH_NUDGE = 0.00025f;
    private static final float ALPHA_BACKGROUND = 0.6f;
    private static final float ALPHA_GRID = 0.5f;
    private static final float ALPHA_SWEEP = 0.8f;
    private static final float TRACK_POSITION_SCALE = 0.75f;

    public MonitorRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(MonitorBlockEntity blockEntity, float partialTicks, MatrixStack ms,
                               VertexConsumerProvider bufferSource, int light, int overlay) {
        if (RadarConfig.client().disableMonitorRendering.get()) return;
        if (!blockEntity.isLinked() || !blockEntity.isController()) return;

        super.renderSafe(blockEntity, partialTicks, ms, bufferSource, light, overlay);

        ms.push();
        setupMonitorTransform(ms, blockEntity.getCachedState().get(MonitorBlock.FACING));

        blockEntity.getRadar().ifPresent(radar -> {
            if (!radar.isRunning()) return;
            renderRadarDisplay(radar, blockEntity, ms, bufferSource, partialTicks);
        });
        ms.pop();
    }

    private void setupMonitorTransform(MatrixStack ms, Direction direction) {
        ms.translate(0.5, 0.5, 0.5);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-directionToYRot(direction)));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(0.5, 0.5, 0.5);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        ms.translate(-0.5, -0.5, -0.5);
    }

    private static float directionToYRot(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    private void renderRadarDisplay(IRadar radar, MonitorBlockEntity blockEntity, MatrixStack ms,
                                    VertexConsumerProvider bufferSource, float partialTicks) {
        renderGrid(radar, blockEntity, ms, bufferSource);
        renderSafeZones(blockEntity, ms, bufferSource);
        renderBG(blockEntity, ms, bufferSource, MonitorSprite.RADAR_BG_FILLER);
        renderBG(blockEntity, ms, bufferSource, MonitorSprite.RADAR_BG_CIRCLE);
        renderSweep(radar, blockEntity, ms, bufferSource);
        renderRadarTracks(radar, blockEntity, ms, bufferSource);
    }

    private void renderGrid(IRadar radar, MonitorBlockEntity blockEntity, MatrixStack ms,
                            VertexConsumerProvider bufferSource) {
        int size = blockEntity.getSize();
        float range = radar.getRange();
        float gridSpacing = range * 2 / RadarConfig.client().gridBoxScale.get();

        VertexConsumer buffer = bufferSource.getBuffer(
                RenderLayer.getEntityTranslucent(MonitorSprite.GRID_SQUARE.getTexture()));
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();

        Color color = new Color(RadarConfig.client().groundRadarColor.get());
        float xmin = 1 - size, zmin = 1 - size, xmax = 1, zmax = 1;

        float u0 = -0.5f * gridSpacing, v0 = -0.5f * gridSpacing;
        float u1 =  0.5f * gridSpacing, v1 = -0.5f * gridSpacing;
        float u2 =  0.5f * gridSpacing, v2 =  0.5f * gridSpacing;
        float u3 = -0.5f * gridSpacing, v3 =  0.5f * gridSpacing;

        float r = color.getRedAsFloat(), g = color.getGreenAsFloat(), b = color.getBlueAsFloat();
        buffer.vertex(m, xmin, DEPTH_GRID, zmin).color(r, g, b, ALPHA_GRID)
                .texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, xmax, DEPTH_GRID, zmin).color(r, g, b, ALPHA_GRID)
                .texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, xmax, DEPTH_GRID, zmax).color(r, g, b, ALPHA_GRID)
                .texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, xmin, DEPTH_GRID, zmax).color(r, g, b, ALPHA_GRID)
                .texture(u3, v3).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
    }

    private void renderSafeZones(MonitorBlockEntity blockEntity, MatrixStack ms,
                                  VertexConsumerProvider bufferSource) {
        List<Box> safeZones = blockEntity.safeZones;
        if (safeZones == null || safeZones.isEmpty()) return;

        int size = blockEntity.getSize();
        float range = blockEntity.getRange();
        Direction facing = blockEntity.getCachedState().get(MonitorBlock.FACING);

        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();
        Color color = new Color(0x383b42);
        float alpha = 0.4f;

        blockEntity.getRadar().ifPresent(radar -> {
            VertexConsumer buffer = bufferSource.getBuffer(RenderLayer.getLines());
            for (Box zone : safeZones) {
                Vec3d zoneMin = transformWorldToRadar(
                        zone.minX, zone.minY, zone.minZ, radar, blockEntity, facing, range, size);
                Vec3d zoneMax = transformWorldToRadar(
                        zone.maxX, zone.maxY, zone.maxZ, radar, blockEntity, facing, range, size);
                if (isOutsideDisplay(zoneMin) && isOutsideDisplay(zoneMax)) continue;
                renderZoneOutline(buffer, m, n, zoneMin, zoneMax, color, alpha);
            }
        });
    }

    private void renderZoneOutline(VertexConsumer buffer, Matrix4f m, Matrix3f n,
                                   Vec3d min, Vec3d max, Color color, float alpha) {
        float r = color.getRedAsFloat(), g = color.getGreenAsFloat(), b = color.getBlueAsFloat();
        renderLine(buffer, m, n, (float) min.x, DEPTH_GRID, (float) min.z,
                (float) max.x, DEPTH_GRID, (float) min.z, r, g, b, alpha);
        renderLine(buffer, m, n, (float) max.x, DEPTH_GRID, (float) min.z,
                (float) max.x, DEPTH_GRID, (float) max.z, r, g, b, alpha);
        renderLine(buffer, m, n, (float) max.x, DEPTH_GRID, (float) max.z,
                (float) min.x, DEPTH_GRID, (float) max.z, r, g, b, alpha);
        renderLine(buffer, m, n, (float) min.x, DEPTH_GRID, (float) max.z,
                (float) min.x, DEPTH_GRID, (float) min.z, r, g, b, alpha);
    }

    private void renderLine(VertexConsumer buffer, Matrix4f m, Matrix3f n,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float r, float g, float b, float alpha) {
        buffer.vertex(m, x1, y1, z1).color(r, g, b, alpha).normal(n, 0, 1, 0).next();
        buffer.vertex(m, x2, y2, z2).color(r, g, b, alpha).normal(n, 0, 1, 0).next();
    }

    private void renderRadarTracks(IRadar radar, MonitorBlockEntity monitor, MatrixStack ms,
                                   VertexConsumerProvider bufferSource) {
        AtomicInteger depthCounter = new AtomicInteger(0);
        for (RadarTrack track : monitor.getTracks()) {
            renderTrack(track, monitor, radar, ms, bufferSource, depthCounter.getAndIncrement());
        }
    }

    private void renderTrack(RadarTrack track, MonitorBlockEntity monitor, IRadar radar,
                             MatrixStack ms, VertexConsumerProvider bufferSource, int depthMultiplier) {
        if (monitor.getWorld() == null) return;

        Direction monitorFacing = monitor.getCachedState().get(MonitorBlock.FACING);
        float scale = radar.getRange();
        int size = monitor.getSize();

        Vec3d radarPos = PhysicsHandler.getWorldPos(monitor.getWorld(), radar.getWorldPos()).toCenterPos();
        Vec3d relativePos = track.position().subtract(radarPos);

        float xOff = calculateTrackOffset(relativePos, monitorFacing, scale, true);
        float zOff = calculateTrackOffset(relativePos, monitorFacing, scale, false);

        if (Math.abs(xOff) > 0.5f || Math.abs(zOff) > 0.5f) return;

        xOff *= TRACK_POSITION_SCALE;
        zOff *= TRACK_POSITION_SCALE;

        float xmin = 1 - size + (xOff * size);
        float zmin = 1 - size + (zOff * size);
        float xmax = xOff * size + 1;
        float zmax = zOff * size + 1;

        float depth = DEPTH_TRACK_BASE + (depthMultiplier * DEPTH_TRACK_INCREMENT);

        long currentTime = monitor.getWorld().getTime();
        float trackAge = currentTime - track.scannedTime();
        float alpha = Math.max(0f, 1.0f - Math.min(1.0f, trackAge / 100f));

        DetectionConfig filter = monitor.filter;
        Color color = filter.getColor(track);

        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();

        renderVertices(getBuffer(bufferSource, getSpriteForTrack(track)), m, n, color, alpha, depth, xmin, zmin, xmax, zmax);

        if (track.id().equals(monitor.hoveredEntity)) {
            renderVertices(getBuffer(bufferSource, MonitorSprite.TARGET_HOVERED),
                    m, n, new Color(255, 255, 0), alpha, depth - 0.0001f, xmin, zmin, xmax, zmax);
        }
        if (track.id().equals(monitor.selectedEntity)) {
            renderVertices(getBuffer(bufferSource, MonitorSprite.TARGET_SELECTED),
                    m, n, new Color(255, 0, 0), alpha, depth - 0.0002f, xmin, zmin, xmax, zmax);
        }

        if (track.trackCategory() == TrackCategory.PLAYER) {
            try {
                UUID uuid = UUID.fromString(track.getId());
                var player = monitor.getWorld().getPlayerByUuid(uuid);
                if (player != null) {
                    float xCenter = (xmin + xmax) * 0.5f;
                    float zCenter = (zmin + zmax) * 0.5f;
                    float zBelow = Math.max((1f - size) + 0.04f,
                            Math.min(1f - 0.04f, zCenter + LABEL_Z_OFFSET));
                    renderTrackLabel(ms, bufferSource, player.getName().getString(),
                            xCenter, zBelow, depth, alpha);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private MonitorSprite getSpriteForTrack(RadarTrack track) {
        return switch (track.trackCategory()) {
            case PLAYER -> MonitorSprite.PLAYER;
            case PROJECTILE -> MonitorSprite.PROJECTILE;
            case CONTRAPTION, VS2 -> MonitorSprite.CONTRAPTION_HITBOX;
            default -> MonitorSprite.ENTITY_HITBOX;
        };
    }

    private float calculateTrackOffset(Vec3d relativePos, Direction monitorFacing, float scale, boolean isXOffset) {
        float offset;
        if (isXOffset) {
            offset = monitorFacing.getAxis() == Direction.Axis.Z
                    ? getOffset(relativePos.x, scale) : getOffset(relativePos.z, scale);
            if (monitorFacing == Direction.NORTH || monitorFacing == Direction.EAST) offset = -offset;
        } else {
            offset = monitorFacing.getAxis() == Direction.Axis.Z
                    ? getOffset(relativePos.z, scale) : getOffset(relativePos.x, scale);
            if (monitorFacing == Direction.NORTH || monitorFacing == Direction.WEST) offset = -offset;
        }
        return offset;
    }

    private float getOffset(double coordinate, float scale) {
        return (float) (coordinate / scale) / 2f;
    }

    private boolean isOutsideDisplay(Vec3d point) {
        return Math.abs(point.x) > 0.5 || Math.abs(point.z) > 0.5;
    }

    private Vec3d transformWorldToRadar(double x, double y, double z, IRadar radar,
                                        MonitorBlockEntity monitor, Direction facing,
                                        float range, int size) {
        Vec3d radarPos = PhysicsHandler.getWorldPos(monitor.getWorld(), radar.getWorldPos()).toCenterPos();
        Vec3d relativePos = new Vec3d(x, y, z).subtract(radarPos);
        float xOff = calculateTrackOffset(relativePos, facing, range, true) * TRACK_POSITION_SCALE;
        float zOff = calculateTrackOffset(relativePos, facing, range, false) * TRACK_POSITION_SCALE;
        return new Vec3d(1 - size + (xOff * size), DEPTH_GRID, 1 - size + (zOff * size));
    }

    private VertexConsumer getBuffer(VertexConsumerProvider bufferSource, MonitorSprite sprite) {
        return bufferSource.getBuffer(ModRenderTypes.polygonOffset(sprite.getTexture()));
    }

    private void renderVertices(VertexConsumer buffer, Matrix4f m, Matrix3f n,
                                Color color, float alpha, float depth,
                                float xmin, float zmin, float xmax, float zmax) {
        float r = color.getRedAsFloat(), g = color.getGreenAsFloat(), b = color.getBlueAsFloat();
        buffer.vertex(m, xmin, depth, zmin).color(r, g, b, alpha)
                .texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, xmax, depth, zmin).color(r, g, b, alpha)
                .texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, xmax, depth, zmax).color(r, g, b, alpha)
                .texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, xmin, depth, zmax).color(r, g, b, alpha)
                .texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
    }

    private void renderBG(MonitorBlockEntity blockEntity, MatrixStack ms,
                          VertexConsumerProvider bufferSource, MonitorSprite sprite) {
        int size = blockEntity.getSize();
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();
        Color color = new Color(RadarConfig.client().groundRadarColor.get());
        renderVertices(getBuffer(bufferSource, sprite), m, n, color, ALPHA_BACKGROUND, DEPTH_BACKGROUND,
                1f - size, 1f - size, 1, 1);
    }

    private void renderSweep(IRadar radar, MonitorBlockEntity controller, MatrixStack ms,
                             VertexConsumerProvider bufferSource) {
        if (!radar.isRunning()) return;

        VertexConsumer buffer = bufferSource.getBuffer(
                ModRenderTypes.polygonOffset(MonitorSprite.RADAR_SWEEP.getTexture()));
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();
        Color color = new Color(RadarConfig.client().groundRadarColor.get());

        Direction monitorFacing = controller.getCachedState().get(MonitorBlock.FACING);
        ConeDir2D cone = getConeDirectionOnMonitor(monitorFacing, Direction.NORTH);
        float currentAngle = switch (cone) {
            case NORTH -> radar.getGlobalAngle();
            case DOWN  -> 180 + radar.getGlobalAngle();
            case LEFT  -> 90  + radar.getGlobalAngle();
            case RIGHT -> 270 + radar.getGlobalAngle();
            default    -> 30f;
        };
        currentAngle = (currentAngle + 360) % 360;

        float angleDiff = currentAngle;
        if (angleDiff > 180)  angleDiff -= 360;
        if (angleDiff < -180) angleDiff += 360;

        float angleRad = angleDiff * (float) Math.PI / 180.0f;
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);

        float cx = 0.5f, cy = 0.5f;
        int size = controller.getSize();

        // Rotate UV coordinates for the sweep animation
        float u0 = cx + (0 - cx) * cos - (0 - cy) * sin;
        float v0 = cy + (0 - cx) * sin + (0 - cy) * cos;
        float u1 = cx + (1 - cx) * cos - (0 - cy) * sin;
        float v1 = cy + (1 - cx) * sin + (0 - cy) * cos;
        float u2 = cx + (1 - cx) * cos - (1 - cy) * sin;
        float v2 = cy + (1 - cx) * sin + (1 - cy) * cos;
        float u3 = cx + (0 - cx) * cos - (1 - cy) * sin;
        float v3 = cy + (0 - cx) * sin + (1 - cy) * cos;

        float r = color.getRedAsFloat(), g = color.getGreenAsFloat(), b = color.getBlueAsFloat();
        buffer.vertex(m, 1f - size, DEPTH_SWEEP, 1f - size).color(r, g, b, ALPHA_SWEEP)
                .texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, 1, DEPTH_SWEEP, 1f - size).color(r, g, b, ALPHA_SWEEP)
                .texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, 1, DEPTH_SWEEP, 1f).color(r, g, b, ALPHA_SWEEP)
                .texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
        buffer.vertex(m, 1f - size, DEPTH_SWEEP, 1f).color(r, g, b, ALPHA_SWEEP)
                .texture(u3, v3).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(n, 0, 1, 0).next();
    }

    public enum ConeDir2D { UP, RIGHT, DOWN, LEFT, NORTH }

    private ConeDir2D getConeDirectionOnMonitor(Direction monitorFacing, Direction radarFacing) {
        int steps = cwStepsBetween(monitorFacing, radarFacing);
        return switch (steps) {
            case 0 -> ConeDir2D.NORTH;
            case 1 -> ConeDir2D.RIGHT;
            case 2 -> ConeDir2D.DOWN;
            case 3 -> ConeDir2D.LEFT;
            default -> ConeDir2D.UP;
        };
    }

    private int cwStepsBetween(Direction from, Direction to) {
        int steps = dirIndex(to) - dirIndex(from);
        steps %= 4;
        if (steps < 0) steps += 4;
        return steps;
    }

    private int dirIndex(Direction d) {
        return switch (d) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default    -> 0;
        };
    }

    private void renderTrackLabel(MatrixStack ms, VertexConsumerProvider bufferSource,
                                  String text, float xCenter, float zBelow, float depth, float alpha) {
        if (alpha <= 0.02f) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        ms.push();
        ms.translate(xCenter, depth + LABEL_DEPTH_NUDGE, zBelow);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        ms.scale(LABEL_SCALE, LABEL_SCALE, LABEL_SCALE);

        int width = textRenderer.getWidth(text);
        float x = -width / 2.0f;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        int argb = (a << 24) | 0xFFFFFF;

        textRenderer.draw(text, x, 0, argb, false, ms.peek().getPositionMatrix(),
                bufferSource, TextRenderer.TextLayerType.NORMAL, 0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);

        ms.pop();
    }
}
