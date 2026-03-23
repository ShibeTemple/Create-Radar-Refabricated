package com.happysg.radar.block.radar.bearing;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.behavior.networks.NetworkData;
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
    // Cached receiverAngle — recomputed only when receiverFacing changes.
    private Direction cachedReceiverFacingForAngle = null;
    private float cachedReceiverAngle = 0f;

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

        // setRange is only needed when the contraption changes (handled in updateContraptionData).
        // Only the angle changes every tick — avoid 3 config reads per tick from getRange().
        if (running) {
            scanningBehavior.setAngle(getGlobalAngle());
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
        if (receiverFacing != cachedReceiverFacingForAngle) {
            cachedReceiverFacingForAngle = receiverFacing;
            cachedReceiverAngle = (float) Math.toDegrees(
                    Math.atan2(receiverFacing.getOffsetX(), receiverFacing.getOffsetZ()));
        }
        return ((cachedReceiverAngle + angle + 360) + 180) % 360;
    }

    public float getAngularSpeed() {
        if (!RadarConfig.server().gearRadarBearingSpeed.get())
            return super.getAngularSpeed();

        // Divide only the rotation speed by the dish-count factor — not the
        // clientAngleDiff correction term. clientAngleDiff is a client-side
        // catch-up value that must converge at the same rate as Create's base
        // bearing; dividing it would make the bearing perpetually lag behind
        // server angle and cause visible jitter/reverse-direction artifacts.
        float divisor = 4f + getDishCount() / 10f;
        float speed = convertToAngular(getSpeed()) / divisor;
        if (getSpeed() == 0) speed = 0;
        if (world.isClient) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }
        return speed;
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
        // Range depends on dishCount/config — update here (contraption change) and not in tick().
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

    @Override
    public String getRadarType() { return "spinning"; }

    @Override
    public boolean renderRelativeToMonitor() { return false; }

    @Override
    public Direction getradarDirection() { return this.receiverFacing; }
}
