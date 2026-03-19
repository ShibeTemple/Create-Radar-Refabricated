package com.happysg.radar.compat.cbc;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

public final class CBCBallistics {

    private CBCBallistics() {}

    public record Result(int tofTicks, double dropY, double horizontalTraveled) {}

    public static Result estimateTofAndDrop(BallisticPropertiesComponent props,
                                            Vec3d muzzlePos,
                                            Vec3d aimDir,
                                            double muzzleSpeedBlocksPerTick,
                                            Vec3d targetPos,
                                            int maxTicks) {
        double dx = targetPos.x - muzzlePos.x;
        double dz = targetPos.z - muzzlePos.z;
        double targetRangeXZ = Math.sqrt(dx * dx + dz * dz);
        if (targetRangeXZ < 1e-6) return new Result(0, 0.0, 0.0);

        Vec3d vel = aimDir.multiply(muzzleSpeedBlocksPerTick);
        double gravity = props.gravity();
        double drag = props.drag();
        boolean quad = props.isQuadraticDrag();

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        double traveledXZ = 0.0;

        int t = 0;
        for (; t < maxTicks; t++) {
            x += vel.x;
            y += vel.y;
            z += vel.z;

            double nowXZ = Math.sqrt(x * x + z * z);
            traveledXZ = nowXZ;

            if (nowXZ >= targetRangeXZ) break;

            vel = vel.add(0.0, gravity, 0.0);
            vel = applyDrag(vel, drag, quad);
        }

        return new Result(t, y, traveledXZ);
    }

    public static Vec3d applyDrag(Vec3d vel, double drag, boolean quadratic) {
        if (drag <= 0) return vel;

        if (!quadratic) {
            double f = MathHelper.clamp(1.0 - drag, 0.0, 1.0);
            return vel.multiply(f);
        } else {
            double speed = vel.length();
            if (speed < 1e-9) return vel;
            double f = 1.0 / (1.0 + drag * speed);
            return vel.multiply(f);
        }
    }
}
