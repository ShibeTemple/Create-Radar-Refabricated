package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.CBCCannonDriver;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Shared state + logic for all weapon-controller block entities.
 *
 * Sub-classes override {@link #driveWeapon(ServerWorld, BlockPos, Vec3d, Vec3d, NbtCompound)}
 * to implement their specific behaviour (yaw-only, pitch-only, full, fire-signal).
 */
public abstract class AbstractWeaponControllerBlockEntity extends SmartBlockEntity {

    /** Ticking interval in game ticks (every 5 ticks = 4 Hz). */
    private static final int TICK_INTERVAL = 5;

    @Nullable protected BlockPos linkedFiltererPos;

    // Resolved target data, updated each tick cycle.
    @Nullable protected Vec3d lastTargetPos;
    @Nullable protected Vec3d lastTargetVelPerTick;
    protected boolean aligned = false;

    public AbstractWeaponControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void initialize() {
        super.initialize();
        if (world instanceof ServerWorld sl && linkedFiltererPos != null) {
            NetworkData data = NetworkData.get(sl);
            data.addWeaponEndpoint(sl.getRegistryKey(), linkedFiltererPos, pos);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (world == null || world.isClient) return;
        if (!(world instanceof ServerWorld sl)) return;
        if (world.getTime() % TICK_INTERVAL != 0) return;

        tickTargeting(sl);
    }

    // ── Linking ──────────────────────────────────────────────────────────────

    public void linkToFilterer(ServerWorld sl, BlockPos filtererPos) {
        // Remove from old group if previously linked
        if (linkedFiltererPos != null) {
            NetworkData.get(sl).removeWeaponEndpoint(sl.getRegistryKey(), linkedFiltererPos, pos);
        }
        linkedFiltererPos = filtererPos;
        NetworkData.get(sl).addWeaponEndpoint(sl.getRegistryKey(), filtererPos, pos);
        markDirty();
        sendData();
    }

    public void onRemoved(ServerWorld sl) {
        if (linkedFiltererPos != null) {
            NetworkData.get(sl).removeWeaponEndpoint(sl.getRegistryKey(), linkedFiltererPos, pos);
        }
    }

    // ── Targeting ────────────────────────────────────────────────────────────

    private void tickTargeting(ServerWorld sl) {
        if (linkedFiltererPos == null) return;

        NetworkData data = NetworkData.get(sl);
        RegistryKey<World> dim = sl.getRegistryKey();
        NetworkData.Group group = data.getGroup(dim, linkedFiltererPos);
        if (group == null) return;

        NbtCompound targetingTag = group.targetingTag;
        boolean autoTarget = targetingTag.getBoolean("autoTarget");
        String targetId = group.selectedTargetId;

        if (targetId == null && !autoTarget) {
            aligned = false;
            return;
        }

        // Resolve radar
        if (group.radarPos == null) { aligned = false; return; }
        BlockEntity radarBe = sl.getBlockEntity(group.radarPos);
        if (!(radarBe instanceof IRadar radar) || !radar.isRunning()) { aligned = false; return; }

        Collection<RadarTrack> tracks = radar.getTracks();

        // Auto-select closest target
        if (targetId == null) {
            RadarTrack closest = null;
            double bestDist = Double.MAX_VALUE;
            Vec3d origin = Vec3d.ofCenter(pos);
            for (RadarTrack t : tracks) {
                double d = t.getPosition().squaredDistanceTo(origin);
                if (d < bestDist) { bestDist = d; closest = t; }
            }
            if (closest == null) { aligned = false; return; }
            targetId = closest.getId();
            data.setSelectedTargetId(group, targetId);
        }

        // Locate the track
        RadarTrack target = null;
        for (RadarTrack t : tracks) {
            if (targetId.equals(t.getId())) { target = t; break; }
        }
        if (target == null) {
            if (autoTarget) data.setSelectedTargetId(group, null);
            aligned = false;
            return;
        }

        lastTargetPos       = target.getPosition();
        // Radar velocity is blocks/tick already in the track system
        lastTargetVelPerTick = target.getVelocity();

        if (Mods.CREATEBIGCANNONS.isLoaded()) {
            aligned = driveWeapon(sl, pos, lastTargetPos, lastTargetVelPerTick, targetingTag);
        }
    }

    /**
     * Apply control outputs to the weapon. Called only when CBC is loaded.
     *
     * @return {@code true} when the weapon is aligned within tolerance.
     */
    protected abstract boolean driveWeapon(
            ServerWorld sl,
            BlockPos controllerPos,
            Vec3d targetPos,
            Vec3d targetVelPerTick,
            NbtCompound targetingTag
    );

    // ── Serialisation ────────────────────────────────────────────────────────

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("LinkedFilterer"))
            linkedFiltererPos = NbtHelper.toBlockPos(tag.getCompound("LinkedFilterer"));
        else
            linkedFiltererPos = null;
        aligned = tag.getBoolean("Aligned");
    }

    @Override
    public void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (linkedFiltererPos != null)
            tag.put("LinkedFilterer", NbtHelper.fromBlockPos(linkedFiltererPos));
        tag.putBoolean("Aligned", aligned);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    @Nullable
    public BlockPos getLinkedFiltererPos() { return linkedFiltererPos; }
    public boolean isAligned() { return aligned; }
}
