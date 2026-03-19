package com.happysg.radar.block.radar.track;

import com.happysg.radar.config.RadarConfig;
import net.createmod.catnip.theme.Color;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

public class RadarTrack {
    private final String id;
    private Vec3d position;
    private Vec3d velocity;
    private long scannedTime;
    private final TrackCategory trackCategory;
    private final String entityType;
    private final float entityheight;

    public RadarTrack(String id, Vec3d position, Vec3d velocity, long scannedTime, TrackCategory trackCategory, String entityType, float entityheight) {
        this.id = id;
        this.position = position;
        this.velocity = velocity;
        this.scannedTime = scannedTime;
        this.trackCategory = trackCategory;
        this.entityType = entityType;
        this.entityheight = entityheight;
    }

    public RadarTrack(Entity entity) {
        this(entity.getUuidAsString(), entity.getPos(), entity.getVelocity(), entity.getWorld().getTime(),
                TrackCategory.get(entity), entity.getType().toString(), entity.getHeight());
    }

    public Color getColor() {
        return switch (trackCategory) {
            case VS2 -> new Color(RadarConfig.client().VS2Color.get());
            case CONTRAPTION -> new Color(RadarConfig.client().contraptionColor.get());
            case PLAYER -> new Color(RadarConfig.client().playerColor.get());
            case ANIMAL -> new Color(RadarConfig.client().friendlyColor.get());
            case HOSTILE -> new Color(RadarConfig.client().hostileColor.get());
            case PROJECTILE -> new Color(RadarConfig.client().projectileColor.get());
            case ITEM -> new Color(RadarConfig.client().itemcolor.get());
            default -> Color.WHITE;
        };
    }

    public static RadarTrack deserializeNBT(NbtCompound tag) {
        return new RadarTrack(
                tag.getString("id"),
                new Vec3d(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")),
                new Vec3d(tag.getDouble("vx"), tag.getDouble("vy"), tag.getDouble("vz")),
                tag.getLong("scannedTime"),
                TrackCategory.values()[tag.getInt("Category")],
                tag.getString("entityType"),
                tag.getFloat("eh")
        );
    }

    public NbtCompound serializeNBT() {
        NbtCompound tag = new NbtCompound();
        tag.putString("id", id);
        tag.putDouble("x", position.x);
        tag.putDouble("y", position.y);
        tag.putDouble("z", position.z);
        tag.putDouble("vx", velocity.x);
        tag.putDouble("vy", velocity.y);
        tag.putDouble("vz", velocity.z);
        tag.putLong("scannedTime", scannedTime);
        tag.putInt("Category", trackCategory.ordinal());
        tag.putString("entityType", entityType);
        tag.putFloat("eh", entityheight);
        return tag;
    }

    public void updateRadarTrack(Entity entity) {
        position = entity.getPos();
        velocity = entity.getVelocity();
        scannedTime = entity.getWorld().getTime();
    }

    public String getId() { return id; }
    public Vec3d getPosition() { return position; }
    public void setPosition(Vec3d position) { this.position = position; }
    public Vec3d getVelocity() { return velocity; }
    public void setVelocity(Vec3d velocity) { this.velocity = velocity; }
    public long getScannedTime() { return scannedTime; }
    public void setScannedTime(long scannedTime) { this.scannedTime = scannedTime; }
    public float getEnityHeight() { return entityheight; }
    public TrackCategory getTrackCategory() { return trackCategory; }
    public String getEntityType() { return entityType; }

    // Record-style accessors
    public String id() { return id; }
    public Vec3d position() { return position; }
    public Vec3d velocity() { return velocity; }
    public long scannedTime() { return scannedTime; }
    public TrackCategory trackCategory() { return trackCategory; }
    public String entityType() { return entityType; }
}
