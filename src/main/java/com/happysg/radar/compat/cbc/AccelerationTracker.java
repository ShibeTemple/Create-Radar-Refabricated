package com.happysg.radar.compat.cbc;

import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AccelerationTracker {

    private static final double VELOCITY_EPSILON = 1.0;
    private static final double VELOCITY_EPSILON_SQR = VELOCITY_EPSILON * VELOCITY_EPSILON;

    private static final Map<UUID, Vec3d> LAST_VEL_PER_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> LAST_ACCEL_PER_TICK2 = new ConcurrentHashMap<>();

    private static final Map<Long, Vec3d> LAST_VEL_PER_TICK_SHIP = new ConcurrentHashMap<>();
    private static final Map<Long, Vec3d> LAST_ACCEL_PER_TICK2_SHIP = new ConcurrentHashMap<>();

    public static Vec3d getAccelerationPerTick2(UUID id, Vec3d velPerTickNow) {
        if (id == null || velPerTickNow == null) return Vec3d.ZERO;

        if (velPerTickNow.lengthSquared() < VELOCITY_EPSILON_SQR) {
            return Vec3d.ZERO;
        }

        Vec3d lastVel = LAST_VEL_PER_TICK.put(id, velPerTickNow);
        if (lastVel == null) {
            LAST_ACCEL_PER_TICK2.put(id, Vec3d.ZERO);
            return Vec3d.ZERO;
        }

        Vec3d accel = velPerTickNow.subtract(lastVel);
        LAST_ACCEL_PER_TICK2.put(id, accel);
        return accel;
    }

    public static Vec3d getLastAccelerationPerTick2(UUID id) {
        if (id == null) return Vec3d.ZERO;
        return LAST_ACCEL_PER_TICK2.getOrDefault(id, Vec3d.ZERO);
    }

    public static void clear(UUID id) {
        if (id == null) return;
        LAST_VEL_PER_TICK.remove(id);
        LAST_ACCEL_PER_TICK2.remove(id);
    }

    public static Vec3d getAccelerationPerTick2(long shipId, Vec3d velPerTickNow) {
        if (velPerTickNow == null) return Vec3d.ZERO;

        if (velPerTickNow.lengthSquared() < VELOCITY_EPSILON_SQR) {
            return Vec3d.ZERO;
        }

        Vec3d lastVel = LAST_VEL_PER_TICK_SHIP.put(shipId, velPerTickNow);
        if (lastVel == null) {
            LAST_ACCEL_PER_TICK2_SHIP.put(shipId, Vec3d.ZERO);
            return Vec3d.ZERO;
        }

        Vec3d accel = velPerTickNow.subtract(lastVel);
        LAST_ACCEL_PER_TICK2_SHIP.put(shipId, accel);
        return accel;
    }

    public static Vec3d getLastAccelerationPerTick2(long shipId) {
        return LAST_ACCEL_PER_TICK2_SHIP.getOrDefault(shipId, Vec3d.ZERO);
    }

    public static void clearShip(long shipId) {
        LAST_VEL_PER_TICK_SHIP.remove(shipId);
        LAST_ACCEL_PER_TICK2_SHIP.remove(shipId);
    }
}
