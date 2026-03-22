package com.happysg.radar.block.monitor;

import com.happysg.radar.block.radar.track.RadarTrack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Objects;

public class MonitorInputHandler {

    /**
     * Maps the 2D hit position on the monitor face to an XZ world-plane direction
     * that can be used to find which radar track the player clicked on.
     */
    static Vec3d adjustRelativeVectorForFacing(Vec3d relative, Direction monitorFacing) {
        return switch (monitorFacing) {
            case NORTH -> new Vec3d( relative.x, 0,  relative.y);
            case SOUTH -> new Vec3d( relative.x, 0, -relative.y);
            case WEST  -> new Vec3d( relative.y, 0,  relative.z);
            case EAST  -> new Vec3d(-relative.y, 0,  relative.z);
            default    -> relative;
        };
    }

    /**
     * Given a hit position on the monitor face, find the closest radar track
     * within selection threshold. Returns null if no track is close enough.
     */
    public static RadarTrack findTrack(World world, Vec3d hit, MonitorBlockEntity controller) {
        if (controller.getRadarCenterPos() == null) return null;

        var controllerState = world.getBlockState(controller.getControllerPos());
        if (!controllerState.contains(MonitorBlock.FACING)) return null;

        Direction monitorFacing = controllerState.get(MonitorBlock.FACING);
        Direction clockwise = monitorFacing.rotateYClockwise();
        int size = controller.getSize();

        // Center of the multiblock display in world space
        Vec3d center = controller.getControllerPos().toCenterPos()
                .add(clockwise.getOffsetX() * (size - 1) / 2.0,
                     (size - 1) / 2.0,
                     clockwise.getOffsetZ() * (size - 1) / 2.0);

        Vec3d relative = hit.subtract(center);
        relative = adjustRelativeVectorForFacing(relative, monitorFacing);

        Vec3d radarPos = controller.getRadarCenterPos();
        float range = controller.getRange();
        float sizeAdj = size == 1 ? 0.5f : ((size - 1) / 2f);
        if (size == 2) sizeAdj = 0.75f;
        // Tracks are rendered at TRACK_POSITION_SCALE (0.75) of their actual position,
        // so we must account for that when projecting back to world coordinates.
        sizeAdj *= 0.75f;

        Vec3d selected = radarPos.add(relative.multiply(range / sizeAdj));

        double bestDistance = 0.1 * range;
        RadarTrack bestTrack = null;
        for (RadarTrack track : controller.cachedTracks) {
            double distance = track.position().distanceTo(selected);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTrack = track;
            }
        }
        return bestTrack;
    }

    /**
     * Server-side: handles right-clicking the monitor to select/deselect a track.
     */
    public static ActionResult onUse(MonitorBlockEntity be, PlayerEntity player, Hand hand,
                                     BlockHitResult hit, Direction facing) {
        MonitorBlockEntity controller = be.getController();
        if (controller == null || !controller.isLinked()) return ActionResult.FAIL;

        if (player.isSneaking()) {
            controller.setSelectedTargetServer(null);
        } else {
            RadarTrack track = findTrack(be.getWorld(), hit.getPos(), controller);
            if (track != null) {
                controller.setSelectedTargetServer(track.id());
            }
        }
        return ActionResult.success(false);
    }

    private static int hoverThrottleCounter = 0;

    /**
     * Client-side: called each tick to update the hovered entity based on where
     * the player is looking on the monitor face. Throttled to run every 3 ticks.
     */
    @Environment(EnvType.CLIENT)
    public static void monitorPlayerHovering(MinecraftClient mc) {
        if (++hoverThrottleCounter % 3 != 0) return;
        if (mc.player == null || mc.world == null) return;

        HitResult result = mc.player.raycast(5.0, 0.0f, false);
        if (!(result instanceof BlockHitResult blockHit)) {
            clearHoveredMonitor(mc);
            return;
        }

        BlockPos hitPos = blockHit.getBlockPos();
        if (!(mc.world.getBlockEntity(hitPos) instanceof MonitorBlockEntity be)) {
            clearHoveredMonitor(mc);
            return;
        }

        MonitorBlockEntity controller = be.getController();
        if (controller == null || !controller.isLinked()) {
            if (controller != null) controller.hoveredEntity = null;
            return;
        }

        RadarTrack track = findTrack(mc.world, blockHit.getPos(), controller);
        String newHovered = track != null ? track.id() : null;
        if (!Objects.equals(controller.hoveredEntity, newHovered)) {
            controller.hoveredEntity = newHovered;
        }
    }

    @Environment(EnvType.CLIENT)
    private static void clearHoveredMonitor(MinecraftClient mc) {
        // No-op — hover naturally expires when the track ages out or display refreshes.
        // We intentionally don't scan all loaded BEs to clear stale hover state
        // because that would be expensive and the visual effect is negligible.
    }
}
