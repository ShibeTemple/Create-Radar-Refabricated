package com.happysg.radar.compat.cbc;

import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.happysg.radar.math3.analysis.UnivariateFunction;
import com.happysg.radar.math3.analysis.solvers.BrentSolver;
import com.happysg.radar.math3.analysis.solvers.UnivariateSolver;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.NaN;
import static java.lang.Math.log;
import static java.lang.Math.toRadians;

public class CannonTargeting {
    private static final Logger LOGGER = LoggerFactory.getLogger(CannonTargeting.class);
    /** Reused across calls — BrentSolver resets internal state at the start of each solve(). */
    private static final UnivariateSolver SOLVER = new BrentSolver(1e-32);

    public static double calculateProjectileYatX(double speed, double dX, double thetaRad, double drag, double g) {
        double l = log(1 - (drag * dX) / (speed * Math.cos(thetaRad)));
        if (Double.isInfinite(l)) l = NaN;
        return dX * Math.tan(thetaRad)
                + (dX * g) / (drag * speed * Math.cos(thetaRad))
                + g * l / (drag * drag);
    }

    public static List<Double> calculatePitch(
            CannonMountBlockEntity mount,
            Vec3d originPos,
            Vec3d targetPos,
            ServerWorld level
    ) {
        if (mount == null || targetPos == null || originPos == null) return null;

        PitchOrientedContraptionEntity contraption = mount.getContraption();
        if (contraption == null || !(contraption.getContraption() instanceof AbstractMountedCannonContraption cannon)) return null;

        if (CannonUtil.isLaserCannon(cannon)) {
            double dX = Math.hypot(targetPos.x - originPos.x, targetPos.z - originPos.z);
            double dY = targetPos.y - originPos.y;
            double pitch = Math.toDegrees(Math.atan2(dY, dX));
            return List.of(pitch);
        }

        float speed = CannonUtil.getInitialVelocity(cannon, level);
        double drag = CannonUtil.getProjectileDrag(cannon, level);
        double gravity = CannonUtil.getProjectileGravity(cannon, level);
        if (speed == 0) return null;

        double dX = Math.hypot(targetPos.x - originPos.x, targetPos.z - originPos.z);
        double dY = targetPos.y - originPos.y;
        double g = Math.abs(gravity);

        UnivariateFunction diffFunction = theta -> {
            double thetaRad = toRadians(theta);
            double y = calculateProjectileYatX(speed, dX, thetaRad, drag, g);
            return y - dY;
        };


        double start = -90, end = 90, step = 1.0;
        List<Double> roots = new ArrayList<>();

        double prevValue = diffFunction.value(start);
        double prevTheta = start;

        for (double theta = start + step; theta <= end; theta += step) {
            double currValue = diffFunction.value(theta);

            if (prevValue * currValue < 0) {
                try {
                    double root = SOLVER.solve(1000, diffFunction, prevTheta, theta);
                    roots.add(root);
                } catch (Exception e) {
                    return null;
                }
            }

            prevTheta = Double.isNaN(currValue) ? prevTheta : theta;
            prevValue = Double.isNaN(currValue) ? prevValue : currValue;
        }

        return roots.isEmpty() ? null : roots;
    }

    public static List<Double> calculatePitch(CannonMountBlockEntity mount, Vec3d targetPos, ServerWorld level) {
        if (mount == null || targetPos == null) return null;
        Vec3d originPos = PhysicsHandler.getWorldVec(level, mount.getControllerBlockPos().up(2).toCenterPos());
        return calculatePitch(mount, originPos, targetPos, level);
    }
}
