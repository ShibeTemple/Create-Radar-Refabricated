package com.happysg.radar.block.radar.bearing;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.behavior.RadarScanningBlockBehavior;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.config.RadarConfig;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class RadarBearingBlockEntity extends MechanicalBearingBlockEntity implements IRadar {

    private BlockPos lastKnownPos = BlockPos.ORIGIN;
    private int dishCount;
    private boolean creative;
    private Direction receiverFacing = Direction.NORTH;
    private RadarScanningBlockBehavior scanningBehavior;
    private Collection<RadarTrack> networkFilteredTracks = List.of();
    private long lastFilterTick = -1;

    public RadarBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        movementMode.setValue(MovementMode.MOVE_NEVER_PLACE.ordinal());
        scanningBehavior = new RadarScanningBlockBehavior(this);
        behaviours.add(scanningBehavior);
    }

    @Override
    public BlockPos getWorldPos() {
        return getPos();
    }

    @Override
    public void tick() {
        super.tick();

        if (running) {
            scanningBehavior.setRange(getRange());
            scanningBehavior.setAngle(getGlobalAngle());
        }

        if (!world.isClient) {
            long gt = world.getTime();
            if (gt % 5 == 0 && gt != lastFilterTick) {
                lastFilterTick = gt;
                recomputeNetworkFilteredTracks();
            }
        }

        if (!world.isClient && world.getTime() % 40 == 0) {
            if (world instanceof ServerWorld serverWorld) {
                if (lastKnownPos.equals(pos)) return;

                RegistryKey<World> dim = serverWorld.getRegistryKey();
                NetworkData data = NetworkData.get(serverWorld);

                boolean updated = data.updateRadarPosition(dim, lastKnownPos, pos);
                if (updated) {
                    lastKnownPos = pos;
                    markDirty();
                }
            }
        }
    }

    public float getGlobalAngle() {
        Vec3d receiverVector = new Vec3d(receiverFacing.getOffsetX(), receiverFacing.getOffsetY(), receiverFacing.getOffsetZ());
        float receiverAngle = (float) Math.toDegrees(Math.atan2(receiverVector.x, receiverVector.z));
        return ((receiverAngle + angle + 360) + 180) % 360;
    }

    public float getAngularSpeed() {
        if (!RadarConfig.server().gearRadarBearingSpeed.get())
            return super.getAngularSpeed();

        float speed = convertToAngular(getSpeed());
        if (getSpeed() == 0) speed = 0;
        if (world.isClient) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }
        return speed / (4f + getDishCount() / 10);
    }

    @Override
    public void assemble() {
        if (!(world.getBlockState(getPos()).getBlock() instanceof RadarBearingBlock)) return;

        RadarContraption contraption = createContraption();
        if (contraption == null) return;

        updateGeneratedRotation();
        updateContraptionData();
        sendData();
    }

    @Override
    public void disassemble() {
        super.disassemble();
        updateContraptionData();
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) BlockStressValues.getImpact(getStressConfigKey()) + getDishCount();
        this.lastStressApplied = impact;
        return impact;
    }

    private RadarContraption createContraption() {
        RadarContraption contraption = new RadarContraption();
        try {
            if (!contraption.assemble(world, getPos())) return null;
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return null;
        }

        contraption.removeBlocksFromWorld(world, BlockPos.ORIGIN);
        movedContraption = ControlledContraptionEntity.create(world, this, contraption);
        BlockPos anchor = getPos().up();
        movedContraption.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(Direction.Axis.Y);
        world.spawnEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(world, getPos());

        running = true;
        angle = 0;
        return contraption;
    }

    private void updateContraptionData() {
        dishCount = getContraption().map(RadarContraption::getDishCount).orElse(0);
        receiverFacing = getContraption().map(RadarContraption::getReceiverFacing).orElse(Direction.NORTH);
        creative = getContraption().map(RadarContraption::isCreative).orElse(false);
        scanningBehavior.setRange(getRange());
        scanningBehavior.setScanPos(PhysicsHandler.getWorldVec(this));
        scanningBehavior.setRunning(running);
        scanningBehavior.setAngle(getGlobalAngle());
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Text.translatable(CreateRadar.MODID + ".radar.dish_count", dishCount));
        tooltip.add(Text.translatable(CreateRadar.MODID + ".radar.range", getRange()));
        return true;
    }

    @Override
    protected void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        dishCount = compound.getInt("dishCount");
        creative = compound.getBoolean("creative");
        if (compound.contains("receiverFacing"))
            receiverFacing = Direction.byId(compound.getInt("receiverFacing"));
    }

    @Override
    public void write(NbtCompound compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("dishCount", dishCount);
        compound.putBoolean("creative", creative);
        if (receiverFacing != null)
            compound.putInt("receiverFacing", receiverFacing.getId());
    }

    public int getDishCount() { return dishCount; }

    public Optional<RadarContraption> getContraption() {
        return Optional.ofNullable(movedContraption)
                .map(ControlledContraptionEntity::getContraption)
                .filter(c -> c instanceof RadarContraption)
                .map(c -> (RadarContraption) c);
    }

    public float getAngle() { return angle; }
    public Direction getReceiverFacing() { return receiverFacing; }

    public float getRange() {
        if (creative) return RadarConfig.server().maxRadarRange.get();
        return Math.min(RadarConfig.server().radarBaseRange.get() + dishCount * RadarConfig.server().dishRangeIncrease.get(),
                RadarConfig.server().maxRadarRange.get());
    }

    public Collection<RadarTrack> getTracks() {
        return scanningBehavior.getRadarTracks();
    }

    @Nullable
    private NetworkData.Group getNetworkGroup() {
        if (world == null || world.isClient) return null;
        if (!(world instanceof ServerWorld sl)) return null;
        NetworkData data = NetworkData.get(sl);
        BlockPos filtererPos = data.getFiltererForEndpoint(sl.getRegistryKey(), pos);
        if (filtererPos == null) return null;
        return data.getGroup(sl.getRegistryKey(), filtererPos);
    }

    private DetectionConfig getDetectionFilterFromNetworkOrDefault() {
        NetworkData.Group g = getNetworkGroup();
        if (g == null) return DetectionConfig.DEFAULT;
        return DetectionConfig.fromTag(g.detectionTag);
    }

    private void recomputeNetworkFilteredTracks() {
        if (world == null || world.isClient) return;
        if (getNetworkGroup() == null) {
            networkFilteredTracks = scanningBehavior.getRadarTracks();
            return;
        }
        DetectionConfig det = getDetectionFilterFromNetworkOrDefault();
        networkFilteredTracks = scanningBehavior.getRadarTracks().stream()
                .filter(det::test)
                .toList();
    }

    @Override
    public String getRadarType() { return "spinning"; }

    @Override
    public boolean renderRelativeToMonitor() { return false; }

    @Override
    public Direction getradarDirection() { return this.receiverFacing; }
}
