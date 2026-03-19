package com.happysg.radar.compat.cbc;

import com.happysg.radar.compat.vs2.PhysicsHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

import java.util.List;

public class CannonLead {
    private static final double VEL_EPS = 0.01;
    private static final double VEL_EPS_SQR = VEL_EPS * VEL_EPS;
    private static final Logger LOGGER = LoggerFactory.getLogger(CannonLead.class);

    public static class LeadSolution {
        public final Vec3d aimPoint;
        public final double pitchDeg;
        public final double yawRad;
        public final int flightTicks;

        public LeadSolution(Vec3d aimPoint, double pitchDeg, double yawRad, int flightTicks) {
            this.aimPoint = aimPoint;
            this.pitchDeg = pitchDeg;
            this.yawRad = yawRad;
            this.flightTicks = flightTicks;
        }
    }

    public static class SimResult {
        public final int ticks;
        public final Vec3d pos;
        public final Vec3d vel;

        public SimResult(int ticks, Vec3d pos, Vec3d vel) {
            this.ticks = ticks;
            this.pos = pos;
            this.vel = vel;
        }
    }

    public static Vec3d predictPositionTicks(Vec3d pos0, Vec3d velPerTick, Vec3d accelPerTick2, double tTicks) {
        return pos0
                .add(velPerTick.multiply(tTicks))
                .add(accelPerTick2.multiply(0.5 * tTicks * tTicks));
    }

    public static Vec3d predictVelocityTicks(Vec3d vel0PerTick, Vec3d accelPerTick2, double tTicks) {
        return vel0PerTick.add(accelPerTick2.multiply(tTicks));
    }

    public static Vec3d directionFromYawPitch(double yawRad, double pitchRad) {
        return new Vec3d(
                Math.cos(pitchRad) * Math.cos(yawRad),
                Math.sin(pitchRad),
                Math.cos(pitchRad) * Math.sin(yawRad)
        ).normalize();
    }

    public static SimResult simulateFlightTicks(
            Vec3d muzzlePos,
            Vec3d shooterVelPerTickAtFire,
            Vec3d dirUnit,
            double muzzleSpeedPerTick,
            double gravityPerTick,
            double drag,
            Vec3d targetPoint,
            double targetHorizontalDist,
            int maxTicks,
            boolean applyDrag
    ) {
        Vec3d pos = muzzlePos;
        Vec3d vel = shooterVelPerTickAtFire.add(dirUnit.multiply(muzzleSpeedPerTick));

        double targetDistSqr = targetHorizontalDist * targetHorizontalDist;

        for (int tick = 0; tick <= maxTicks; tick++) {
            double dx = pos.x - muzzlePos.x;
            double dz = pos.z - muzzlePos.z;
            if (dx * dx + dz * dz >= targetDistSqr) {
                return new SimResult(tick, pos, vel);
            }

            if (vel.lengthSquared() <= 1.0e-4) {
                return new SimResult(tick, pos, vel);
            }

            vel = vel.add(0.0, gravityPerTick, 0.0);

            if (applyDrag && drag != 0.0) {
                vel = vel.multiply(1.0 - drag);
            }

            pos = pos.add(vel);
        }

        return new SimResult(maxTicks, pos, vel);
    }

    public static LeadSolution solveLeadPerTickWithAcceleration(
            CannonMountBlockEntity mount,
            AbstractMountedCannonContraption cannon,
            ServerWorld level,

            Vec3d shooterVelPerTick,
            Vec3d shooterAccelPerTick2,

            Vec3d targetPosNow,
            Vec3d targetVelPerTick,
            Vec3d targetAccelPerTick2,

            int fireDelayTicks,
            double maxSimDistanceBlocks
    ) {
        if (mount == null || cannon == null || level == null) return null;
        if (targetPosNow == null || targetVelPerTick == null || targetAccelPerTick2 == null) return null;
        if (shooterVelPerTick == null || shooterAccelPerTick2 == null) return null;

        boolean targetMoving = targetVelPerTick.lengthSquared() >= VEL_EPS_SQR;
        boolean shooterMoving = shooterVelPerTick.lengthSquared() >= VEL_EPS_SQR;

        if (!shooterMoving) {
            shooterVelPerTick = Vec3d.ZERO;
            shooterAccelPerTick2 = Vec3d.ZERO;
        }

        double muzzleSpeedPerTick = CannonUtil.getInitialVelocity(cannon, level);
        if (muzzleSpeedPerTick <= 0.0) return null;

        Vec3d originNow = PhysicsHandler.getWorldVec(level, mount.getControllerBlockPos().up(2).toCenterPos());
        final double latencyTicks = 2.0;
        int barrelLength = CannonUtil.getBarrelLength(cannon);

        BallisticPropertiesComponent bp = CannonUtil.getBallistics(cannon, level);
        double gravityPerTick = bp.gravity();
        double formDrag = bp.drag();

        Vec3d shooterPosAtFire = predictPositionTicks(originNow, shooterVelPerTick, shooterAccelPerTick2, fireDelayTicks);
        Vec3d shooterVelAtFire = predictVelocityTicks(shooterVelPerTick, shooterAccelPerTick2, fireDelayTicks);

        Vec3d targetPosRel0 = targetPosNow.subtract(shooterPosAtFire);
        Vec3d targetVelRel = targetVelPerTick.subtract(shooterVelAtFire);
        Vec3d targetAccelRel = targetAccelPerTick2.subtract(shooterAccelPerTick2);

        if (!targetMoving) {
            Vec3d to = targetPosNow.subtract(shooterPosAtFire);
            double yaw = Math.atan2(to.z, to.x);
            double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
            double pitch = Math.atan2(to.y, Math.max(1.0e-6, horiz));
            return new LeadSolution(targetPosNow, Math.toDegrees(pitch), yaw, 0);
        }

        double dx0 = targetPosNow.x - shooterPosAtFire.x;
        double dz0 = targetPosNow.z - shooterPosAtFire.z;
        double horiz0 = Math.sqrt(dx0 * dx0 + dz0 * dz0);
        double tGuessTicks = horiz0 / Math.max(1.0e-6, muzzleSpeedPerTick);

        Vec3d aimPoint = targetPosNow;
        double chosenPitchDeg = 0.0;
        double chosenYawRad = 0.0;
        int flightTicks = (int) Math.round(tGuessTicks);

        for (int iter = 0; iter < 8; iter++) {
            double tFlightTicks = tGuessTicks;
            double tLeadTicks = tFlightTicks + latencyTicks;
            Vec3d aimRel = predictPositionTicks(targetPosRel0, targetVelRel, targetAccelRel, tLeadTicks);
            aimPoint = shooterPosAtFire.add(aimRel);

            Vec3d toPred = aimPoint.subtract(shooterPosAtFire);
            chosenYawRad = Math.atan2(toPred.z, toPred.x);

            double horizToPred = Math.sqrt(toPred.x * toPred.x + toPred.z * toPred.z);
            double pitchRad = Math.atan2(toPred.y, Math.max(1.0e-6, horizToPred));

            List<Double> pitchRoots = CannonTargeting.calculatePitch(mount, shooterPosAtFire, aimPoint, level);
            if (pitchRoots != null && !pitchRoots.isEmpty()) {
                pitchRad = Math.toRadians(pitchRoots.get(0));
            }

            Vec3d dir = directionFromYawPitch(chosenYawRad, pitchRad);
            chosenPitchDeg = Math.toDegrees(pitchRad);

            Vec3d muzzlePosAtFire = shooterPosAtFire.add(dir.multiply(barrelLength));

            double dx = aimPoint.x - muzzlePosAtFire.x;
            double dz = aimPoint.z - muzzlePosAtFire.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);

            SimResult sim = simulateFlightTicks(
                    muzzlePosAtFire,
                    shooterVelAtFire,
                    dir,
                    muzzleSpeedPerTick,
                    gravityPerTick,
                    formDrag,
                    aimPoint,
                    horiz,
                    computeMaxSimTicks(horiz, muzzleSpeedPerTick, maxSimDistanceBlocks),
                    true
            );

            int newFlightTicks = sim.ticks;

            if (Math.abs(newFlightTicks - tGuessTicks) < 0.5) {
                flightTicks = newFlightTicks;
                tGuessTicks = newFlightTicks;
                break;
            }

            flightTicks = newFlightTicks;
            tGuessTicks = newFlightTicks;
        }

        return new LeadSolution(aimPoint, chosenPitchDeg, chosenYawRad, flightTicks);
    }

    public static LeadSolution solveLeadPerTickConstantVelocity(
            CannonMountBlockEntity mount,
            AbstractMountedCannonContraption cannon,
            ServerWorld level,

            Vec3d shooterVelPerTick,
            Vec3d targetPosNow,
            Vec3d targetVelPerTick,

            int fireDelayTicks,
            double maxSimDistanceBlocks
    ) {
        if (shooterVelPerTick.lengthSquared() < VEL_EPS_SQR) shooterVelPerTick = Vec3d.ZERO;
        boolean targetMoving = targetVelPerTick.lengthSquared() >= VEL_EPS_SQR;

        double muzzleSpeedPerTick = CannonUtil.getInitialVelocity(cannon, level);
        if (muzzleSpeedPerTick <= 0.0) {
            LOGGER.warn("[LEAD] muzzleSpeedPerTick={} cannon={} mountPos={}",
                    muzzleSpeedPerTick, cannon.getClass().getSimpleName(), mount.getPos());
            return null;
        }

        Vec3d originNow = PhysicsHandler.getWorldVec(level, mount.getControllerBlockPos().up(2).toCenterPos());
        int barrelLength = CannonUtil.getBarrelLength(cannon);

        BallisticPropertiesComponent bp = CannonUtil.getBallistics(cannon, level);
        double gravityPerTick = bp.gravity();
        double drag = bp.drag();

        Vec3d shooterPosAtFire = originNow.add(shooterVelPerTick.multiply(fireDelayTicks));
        Vec3d shooterVelAtFire = shooterVelPerTick;

        Vec3d targetPosAtFire = targetPosNow.add(targetVelPerTick.multiply(fireDelayTicks));
        Vec3d targetVelAtFire = targetVelPerTick;

        Vec3d relPos0 = targetPosAtFire.subtract(shooterPosAtFire);
        Vec3d relVel = targetVelAtFire.subtract(shooterVelAtFire);

        if (!targetMoving) {
            Vec3d to = targetPosAtFire.subtract(shooterPosAtFire);
            double yaw = Math.atan2(to.z, to.x);
            double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
            double pitch = Math.atan2(to.y, Math.max(1.0e-6, horiz));
            return new LeadSolution(targetPosAtFire, Math.toDegrees(pitch), yaw, 0);
        }

        double horiz0 = Math.sqrt(relPos0.x * relPos0.x + relPos0.z * relPos0.z);
        double tGuessTicks = horiz0 / Math.max(1.0e-6, muzzleSpeedPerTick);

        Vec3d aimPoint = targetPosAtFire;
        double chosenPitchDeg = 0.0;
        double chosenYawRad = 0.0;
        int flightTicks = (int) Math.round(tGuessTicks);

        for (int iter = 0; iter < 8; iter++) {
            Vec3d aimRel = relPos0.add(relVel.multiply(tGuessTicks));
            aimPoint = shooterPosAtFire.add(aimRel);

            Vec3d toPred = aimPoint.subtract(shooterPosAtFire);
            chosenYawRad = Math.atan2(toPred.z, toPred.x);

            double horizToPred = Math.sqrt(toPred.x * toPred.x + toPred.z * toPred.z);
            double pitchRad = Math.atan2(toPred.y, Math.max(1.0e-6, horizToPred));

            List<Double> pitchRoots = CannonTargeting.calculatePitch(mount, shooterPosAtFire, aimPoint, level);
            if (pitchRoots != null && !pitchRoots.isEmpty()) {
                pitchRad = Math.toRadians(pitchRoots.get(0));
            }

            Vec3d dir = directionFromYawPitch(chosenYawRad, pitchRad);
            chosenPitchDeg = Math.toDegrees(pitchRad);

            Vec3d muzzlePosAtFire = shooterPosAtFire.add(dir.multiply(barrelLength));

            double dx = aimPoint.x - muzzlePosAtFire.x;
            double dz = aimPoint.z - muzzlePosAtFire.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);

            SimResult sim = simulateFlightTicks(
                    muzzlePosAtFire,
                    shooterVelAtFire,
                    dir,
                    muzzleSpeedPerTick,
                    gravityPerTick,
                    drag,
                    aimPoint,
                    horiz,
                    computeMaxSimTicks(horiz, muzzleSpeedPerTick, maxSimDistanceBlocks),
                    true
            );

            int newFlightTicks = sim.ticks;

            if (Math.abs(newFlightTicks - tGuessTicks) < 0.5) {
                flightTicks = newFlightTicks;
                tGuessTicks = newFlightTicks;
                break;
            }

            flightTicks = newFlightTicks;
            tGuessTicks = newFlightTicks;
        }

        return new LeadSolution(aimPoint, chosenPitchDeg, chosenYawRad, flightTicks);
    }

    private static int computeMaxSimTicks(double targetHorizontalDist, double muzzleSpeedPerTick, double maxSimDistanceBlocks) {
        final int HARD_MAX_TICKS = 8000;

        double speed = Math.max(1.0e-6, muzzleSpeedPerTick);
        double cappedDist = Math.min(targetHorizontalDist, Math.max(0.0, maxSimDistanceBlocks));

        int ticksToTarget = (int) Math.ceil(cappedDist / speed);
        int ticks = ticksToTarget + 40;

        if (ticks < 60) ticks = 60;
        if (ticks > HARD_MAX_TICKS) ticks = HARD_MAX_TICKS;
        return ticks;
    }

    public static void logLeadByBlocks(Vec3d targetPosNow, Vec3d aimPoint, Vec3d targetVelPerTick) {
        if (targetPosNow == null || aimPoint == null) return;

        Vec3d leadVec = aimPoint.subtract(targetPosNow);
        double totalLead = leadVec.length();

        double directionalLead = 0.0;
        if (targetVelPerTick != null && targetVelPerTick.lengthSquared() > 1.0e-9) {
            directionalLead = leadVec.dotProduct(targetVelPerTick.normalize());
        }

        LOGGER.warn("Lead debug → totalLead={} directionalLead={} leadVec={} targetVelPerTick={}",
                totalLead, directionalLead, leadVec, targetVelPerTick);
    }
}
