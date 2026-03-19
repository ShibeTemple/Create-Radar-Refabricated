package com.happysg.radar.compat.cbc;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityTracker {
    private static final Map<UUID, Vec3d> LAST_POS  = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> LAST_VEL  = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_TICK = new ConcurrentHashMap<>();

    private static final double MAX_VEL_SQR = 25.0;
    private static final double VEL_ALPHA = 0.35;

    public static Vec3d getEstimatedVelocityPerTick(Entity e) {
        if (e == null) return Vec3d.ZERO;

        int tick = e.age;
        UUID id = e.getUuid();

        Integer lastTick = LAST_TICK.get(id);
        if (lastTick != null && lastTick == tick) {
            return LAST_VEL.getOrDefault(id, Vec3d.ZERO);
        }

        Vec3d now = e.getPos();
        Vec3d lastPos = LAST_POS.put(id, now);
        LAST_TICK.put(id, tick);

        if (lastPos == null) {
            LAST_VEL.put(id, Vec3d.ZERO);
            return Vec3d.ZERO;
        }

        Vec3d rawVel = now.subtract(lastPos);

        if (rawVel.lengthSquared() > MAX_VEL_SQR) {
            rawVel = Vec3d.ZERO;
        }

        Vec3d prev = LAST_VEL.getOrDefault(id, Vec3d.ZERO);
        Vec3d vel = prev.multiply(1.0 - VEL_ALPHA).add(rawVel.multiply(VEL_ALPHA));

        LAST_VEL.put(id, vel);
        return vel;
    }

    public static Vec3d getLastVelocityPerTick(UUID id) {
        return LAST_VEL.getOrDefault(id, Vec3d.ZERO);
    }

    public static void clear(UUID id) {
        if (id == null) return;
        LAST_POS.remove(id);
        LAST_VEL.remove(id);
        LAST_TICK.remove(id);
    }
}
