package com.happysg.radar.compat.cbc;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.List;

/**
 * All CBC API calls are isolated here so this class is only loaded when CBC is present.
 * Call only after confirming Mods.CREATEBIGCANNONS.isLoaded().
 */
public final class CBCCannonDriver {

    private CBCCannonDriver() {}

    /** Tolerance in degrees before the cannon is considered aligned. */
    private static final float ALIGN_YAW_TOLERANCE_DEG   = 2.0f;
    private static final float ALIGN_PITCH_TOLERANCE_DEG = 1.5f;

    /**
     * Scan the 6 adjacent positions from {@code controllerPos} for a CannonMountBlockEntity.
     */
    @Nullable
    public static CannonMountBlockEntity findAdjacentMount(ServerWorld sl, BlockPos controllerPos) {
        for (Direction dir : Direction.values()) {
            BlockEntity be = sl.getBlockEntity(controllerPos.offset(dir));
            if (be instanceof CannonMountBlockEntity mount) return mount;
        }
        return null;
    }

    /**
     * Drive both yaw and pitch of the adjacent cannon mount toward the target.
     * Returns {@code true} if the cannon is now aligned within tolerance.
     */
    public static boolean driveMount(
            ServerWorld sl,
            BlockPos controllerPos,
            Vec3d targetPos,
            Vec3d targetVelPerTick,
            NbtCompound targetingTag
    ) {
        CannonMountBlockEntity mount = findAdjacentMount(sl, controllerPos);
        if (mount == null || !mount.isRunning()) return false;

        LeadResult lead = computeLead(sl, mount, targetPos, targetVelPerTick, targetingTag);
        if (lead == null) return false;

        mount.setYaw(lead.yawDeg);
        mount.setPitch(lead.pitchDeg);

        float curYaw   = mount.getYawOffset(1.0f);
        float curPitch = mount.getPitchOffset(1.0f);
        float yawErr   = Math.abs(angleDiff(curYaw, lead.yawDeg));
        float pitchErr = Math.abs(angleDiff(curPitch, lead.pitchDeg));
        return yawErr <= ALIGN_YAW_TOLERANCE_DEG && pitchErr <= ALIGN_PITCH_TOLERANCE_DEG;
    }

    /** Drive only yaw; returns true when aligned. */
    public static boolean driveYaw(
            ServerWorld sl,
            BlockPos controllerPos,
            Vec3d targetPos,
            Vec3d targetVelPerTick,
            NbtCompound targetingTag
    ) {
        CannonMountBlockEntity mount = findAdjacentMount(sl, controllerPos);
        if (mount == null || !mount.isRunning()) return false;

        LeadResult lead = computeLead(sl, mount, targetPos, targetVelPerTick, targetingTag);
        if (lead == null) return false;

        mount.setYaw(lead.yawDeg);
        float yawErr = Math.abs(angleDiff(mount.getYawOffset(1.0f), lead.yawDeg));
        return yawErr <= ALIGN_YAW_TOLERANCE_DEG;
    }

    /** Drive only pitch; returns true when aligned. */
    public static boolean drivePitch(
            ServerWorld sl,
            BlockPos controllerPos,
            Vec3d targetPos,
            Vec3d targetVelPerTick,
            NbtCompound targetingTag
    ) {
        CannonMountBlockEntity mount = findAdjacentMount(sl, controllerPos);
        if (mount == null || !mount.isRunning()) return false;

        LeadResult lead = computeLead(sl, mount, targetPos, targetVelPerTick, targetingTag);
        if (lead == null) return false;

        mount.setPitch(lead.pitchDeg);
        float pitchErr = Math.abs(angleDiff(mount.getPitchOffset(1.0f), lead.pitchDeg));
        return pitchErr <= ALIGN_PITCH_TOLERANCE_DEG;
    }

    /** Check alignment without driving anything. */
    public static boolean isAligned(
            ServerWorld sl,
            BlockPos controllerPos,
            Vec3d targetPos,
            Vec3d targetVelPerTick,
            NbtCompound targetingTag
    ) {
        CannonMountBlockEntity mount = findAdjacentMount(sl, controllerPos);
        if (mount == null || !mount.isRunning()) return false;

        LeadResult lead = computeLead(sl, mount, targetPos, targetVelPerTick, targetingTag);
        if (lead == null) return false;

        float yawErr   = Math.abs(angleDiff(mount.getYawOffset(1.0f), lead.yawDeg));
        float pitchErr = Math.abs(angleDiff(mount.getPitchOffset(1.0f), lead.pitchDeg));
        return yawErr <= ALIGN_YAW_TOLERANCE_DEG && pitchErr <= ALIGN_PITCH_TOLERANCE_DEG;
    }

    // ── Internal lead computation ─────────────────────────────────────────────

    private record LeadResult(float yawDeg, float pitchDeg) {}

    @Nullable
    private static LeadResult computeLead(
            ServerWorld sl,
            CannonMountBlockEntity mount,
            Vec3d targetPos,
            Vec3d targetVelPerTick,
            NbtCompound targetingTag
    ) {
        PitchOrientedContraptionEntity contraptionEntity = mount.getContraption();
        AbstractMountedCannonContraption cannon = null;
        if (contraptionEntity != null &&
                contraptionEntity.getContraption() instanceof AbstractMountedCannonContraption c) {
            cannon = c;
        }

        boolean artilleryMode = targetingTag != null && targetingTag.getBoolean("artilleryMode");

        if (cannon == null || artilleryMode) {
            // Fallback: direct line-of-sight geometry
            Vec3d origin = Vec3d.ofCenter(mount.getPos());
            Vec3d to = targetPos.subtract(origin);
            float yawDeg = (float) Math.toDegrees(Math.atan2(to.z, to.x));
            double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
            float pitchDeg = (float) Math.toDegrees(Math.atan2(to.y, Math.max(1e-6, horiz)));
            return new LeadResult(yawDeg, pitchDeg);
        }

        CannonLead.LeadSolution lead = CannonLead.solveLeadPerTickConstantVelocity(
                mount, cannon, sl,
                Vec3d.ZERO,          // shooter stationary
                targetPos,
                targetVelPerTick,
                2,                   // fire delay ticks
                CannonUtil.getMaxProjectileRangeBlocks(cannon, sl)
        );

        if (lead == null) {
            // Fallback to direct geometry
            Vec3d origin = Vec3d.ofCenter(mount.getPos());
            Vec3d to = targetPos.subtract(origin);
            float yawDeg = (float) Math.toDegrees(Math.atan2(to.z, to.x));
            double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
            float pitchDeg = (float) Math.toDegrees(Math.atan2(to.y, Math.max(1e-6, horiz)));
            return new LeadResult(yawDeg, pitchDeg);
        }

        // Check line-of-sight if required
        if (targetingTag != null && targetingTag.getBoolean("lineOfSight")) {
            // Basic LoS check - if there are no blocks in the way (simplified)
            // A full raytrace is expensive; we'll leave this as a no-op for now
        }

        // Artillery: prefer the higher angle solution
        if (artilleryMode) {
            List<Double> pitchRoots = CannonTargeting.calculatePitch(mount, targetPos, sl);
            if (pitchRoots != null && pitchRoots.size() > 1) {
                // Pick the larger (higher) angle
                double highPitch = pitchRoots.stream().mapToDouble(Double::doubleValue).max().orElse(lead.pitchDeg);
                return new LeadResult(
                        (float) Math.toDegrees(lead.yawRad),
                        (float) highPitch
                );
            }
        }

        return new LeadResult(
                (float) Math.toDegrees(lead.yawRad),
                (float) lead.pitchDeg
        );
    }

    /** Signed angular difference in [-180, 180]. */
    private static float angleDiff(float a, float b) {
        float d = ((b - a) % 360 + 540) % 360 - 180;
        return d;
    }
}
