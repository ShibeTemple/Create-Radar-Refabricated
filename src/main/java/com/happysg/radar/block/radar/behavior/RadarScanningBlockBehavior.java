package com.happysg.radar.block.radar.behavior;

import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.TrackCategory;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.happysg.radar.config.RadarConfig;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class RadarScanningBlockBehavior extends BlockEntityBehaviour {

    public static final BehaviourType<RadarScanningBlockBehavior> TYPE = new BehaviourType<>();

    private int trackExpiration = 100;
    private int fov = RadarConfig.server().radarFOV.get();
    private int yRange = 20;
    private double range = RadarConfig.server().radarBaseRange.get();
    private double angle;
    private boolean running = false;
    private SmartBlockEntity bearingEntity;
    Vec3d scanPos = Vec3d.ZERO;

    // Cached once per scan tick to avoid per-entity config reads.
    private int cachedYScanRange = 20;
    // Split-box cache: rebuilt only when range changes.
    private List<Box> cachedSplitBoxes = null;
    private double splitBoxCachedRange = Double.NaN;

    private boolean scanPlayers = true;
    private boolean scanContraptions = true;
    private boolean scanMobs = true;
    private boolean scanAnimals = true;
    private boolean scanProjectiles = true;
    private boolean scanItems = true;

    private final Set<Entity> scannedEntities = new HashSet<>();
    private final Set<ProjectileEntity> scannedProjectiles = new HashSet<>();
    private final HashMap<String, RadarTrack> radarTracks = new HashMap<>();

    public RadarScanningBlockBehavior(SmartBlockEntity be) {
        super(be);
        this.bearingEntity = be;
    }

    public void applyDetectionConfig(DetectionConfig cfg) {
        if (cfg == null) cfg = DetectionConfig.DEFAULT;
        setScanFlags(cfg.player(), cfg.vs2(), cfg.contraption(), cfg.mob(), cfg.animal(), cfg.projectile(), cfg.item());
    }

    private boolean allowCategory(TrackCategory c) {
        return switch (c) {
            case PLAYER -> scanPlayers;
            case CONTRAPTION -> scanContraptions;
            case PROJECTILE -> scanProjectiles;
            case ITEM -> scanItems;
            case ANIMAL -> scanAnimals;
            case HOSTILE, MOB -> scanMobs;
            default -> true;
        };
    }

    private void pruneDisabledTracksNow() {
        radarTracks.entrySet().removeIf(e -> !allowCategory(e.getValue().trackCategory()));
    }

    public void setScanFlags(boolean players, boolean vs2, boolean contraptions, boolean mobs,
                             boolean animals, boolean projectiles, boolean items) {
        boolean changed = players != scanPlayers || contraptions != scanContraptions || mobs != scanMobs
                || animals != scanAnimals || projectiles != scanProjectiles || items != scanItems;

        this.scanPlayers = players;
        this.scanContraptions = contraptions;
        this.scanMobs = mobs;
        this.scanAnimals = animals;
        this.scanProjectiles = projectiles;
        this.scanItems = items;

        if (changed) pruneDisabledTracksNow();
    }

    @Override
    public void tick() {
        super.tick();
        if (blockEntity.getWorld() == null || blockEntity.getWorld().isClient) return;
        if (blockEntity.getWorld().getTime() % 5 != 1) return;

        cachedYScanRange = RadarConfig.server().radarYScanRange.get();
        removeDeadTracks();
        if (running) updateRadarTracks();
        if (running) {
            scannedEntities.clear();
            scannedProjectiles.clear();
            scanForEntityTracks();
        }
    }

    private void updateRadarTracks() {
        scanPos = PhysicsHandler.getWorldVec(bearingEntity).add(0, 0, 0);
        World level = blockEntity.getWorld();
        if (level == null) return;

        for (Entity entity : scannedEntities) {
            if (entity.isAlive() && isInFovAndRange(entity.getPos())) {
                radarTracks.compute(entity.getUuidAsString(), (id, track) -> {
                    if (track == null) return new RadarTrack(entity);
                    track.updateRadarTrack(entity);
                    return track;
                });
                if (entity instanceof ProjectileEntity proj) scannedProjectiles.add(proj);
            }
        }
    }

    private boolean isInFovAndRange(Vec3d target) {
        double dx = target.x - scanPos.x;
        double dz = target.z - scanPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double verticalDistance = Math.abs(target.y - scanPos.y);

        if (horizontalDistance > range || verticalDistance > cachedYScanRange) return false;
        if (horizontalDistance < 2) return true;

        double angleToEntity = Math.toDegrees(Math.atan2(dx, dz));
        angleToEntity = (angleToEntity + 360) % 360;
        double angleDiff = Math.abs(angleToEntity - angle);
        if (angleDiff > 180) angleDiff = 360 - angleDiff;

        return angleDiff <= fov / 2.0;
    }

    private void removeDeadTracks() {
        for (Entity entity : scannedEntities) {
            if (!entity.isAlive()) radarTracks.remove(entity.getUuidAsString());
        }

        long currentTime = blockEntity.getWorld().getTime();
        radarTracks.values().removeIf(track -> currentTime - track.scannedTime() > trackExpiration);

        scannedProjectiles.removeIf(p -> {
            boolean dead = !p.isAlive();
            if (dead) radarTracks.remove(p.getUuidAsString());
            return dead;
        });
    }

    private void scanForEntityTracks() {
        World level = blockEntity.getWorld();
        if (level == null) return;

        boolean scanAll = scanPlayers && scanContraptions && scanMobs && scanAnimals && scanProjectiles && scanItems;

        if (cachedSplitBoxes == null || splitBoxCachedRange != range) {
            splitBoxCachedRange = range;
            cachedSplitBoxes = splitBox(getRadarBox(), 256);
        }
        for (Box aabb : cachedSplitBoxes) {
            if (scanAll) {
                scannedEntities.addAll(level.getOtherEntities(null, aabb));
                continue;
            }
            if (scanPlayers) scannedEntities.addAll(level.getEntitiesByClass(PlayerEntity.class, aabb, e -> true));
            if (scanProjectiles) scannedEntities.addAll(level.getEntitiesByClass(ProjectileEntity.class, aabb, e -> true));
            if (scanItems) scannedEntities.addAll(level.getEntitiesByClass(ItemEntity.class, aabb, e -> true));
            if (scanContraptions) scannedEntities.addAll(level.getEntitiesByClass(AbstractContraptionEntity.class, aabb, e -> true));
            if (scanAnimals) scannedEntities.addAll(level.getEntitiesByClass(AnimalEntity.class, aabb, e -> true));
            if (scanMobs) {
                scannedEntities.addAll(level.getEntitiesByClass(MobEntity.class, aabb,
                        e -> !(e instanceof AnimalEntity)));
            }
        }
    }

    private Box getRadarBox() {
        BlockPos radarPos = PhysicsHandler.getWorldPos(blockEntity);
        double x = radarPos.getX() + 0.5;
        double y = radarPos.getY() + 0.5;
        double z = radarPos.getZ() + 0.5;
        double yScan = RadarConfig.server().radarYScanRange.get();
        World level = blockEntity.getWorld();
        double minY = level != null ? Math.max(y - yScan, level.getBottomY()) : y - yScan;
        double maxY = level != null ? Math.min(y + yScan, level.getTopY()) : y + yScan;
        return new Box(x - range, minY, z - range, x + range, maxY, z + range);
    }

    public static List<Box> splitBox(Box aabb, double maxSize) {
        List<Box> result = new ArrayList<>();
        for (double x = aabb.minX; x < aabb.maxX; x += maxSize) {
            for (double y = aabb.minY; y < aabb.maxY; y += maxSize) {
                for (double z = aabb.minZ; z < aabb.maxZ; z += maxSize) {
                    result.add(new Box(x, y, z,
                            Math.min(x + maxSize, aabb.maxX),
                            Math.min(y + maxSize, aabb.maxY),
                            Math.min(z + maxSize, aabb.maxZ)));
                }
            }
        }
        return result;
    }

    @Override
    public void read(NbtCompound nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if (nbt.contains("fov")) fov = nbt.getInt("fov");
        if (nbt.contains("yRange")) yRange = nbt.getInt("yRange");
        if (nbt.contains("range")) range = nbt.getDouble("range");
        if (nbt.contains("angle")) angle = nbt.getDouble("angle");
        if (nbt.contains("scanPosX"))
            scanPos = new Vec3d(nbt.getDouble("scanPosX"), nbt.getDouble("scanPosY"), nbt.getDouble("scanPosZ"));
        if (nbt.contains("running")) running = nbt.getBoolean("running");
        if (nbt.contains("trackExpiration")) trackExpiration = nbt.getInt("trackExpiration");
    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        nbt.putInt("fov", fov);
        nbt.putInt("yRange", yRange);
        nbt.putDouble("range", range);
        nbt.putDouble("angle", angle);
        nbt.putDouble("scanPosX", scanPos.x);
        nbt.putDouble("scanPosY", scanPos.y);
        nbt.putDouble("scanPosZ", scanPos.z);
        nbt.putBoolean("running", running);
        nbt.putInt("trackExpiration", trackExpiration);
    }

    public void setFov(int fov) { this.fov = fov; }
    public void setYRange(int yRange) { this.yRange = yRange; }
    public void setRange(double range) { this.range = range; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setScanPos(Vec3d scanPos) { this.scanPos = scanPos; }
    public void setRunning(boolean running) { this.running = running; }
    public void setTrackExpiration(int trackExpiration) { this.trackExpiration = trackExpiration; }

    public Collection<RadarTrack> getRadarTracks() { return radarTracks.values(); }

    @Override
    public BehaviourType<?> getType() { return TYPE; }

    public float getAngle() { return (float) angle; }
}
