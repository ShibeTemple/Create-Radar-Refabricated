package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.cbc.CBCCannonDriver;
import com.happysg.radar.registry.ModBlockEntityTypes;
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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Checks alignment every 5 ticks and updates the POWERED blockstate accordingly.
 * Outputs redstone level 15 when the adjacent cannon is on target.
 */
public class FireControllerBlockEntity extends SmartBlockEntity {

    private static final int TICK_INTERVAL = 5;

    @Nullable private BlockPos linkedFiltererPos;

    public FireControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (world instanceof ServerWorld sl && linkedFiltererPos != null) {
            NetworkData.get(sl).addWeaponEndpoint(sl.getRegistryKey(), linkedFiltererPos, pos);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (world == null || world.isClient) return;
        if (!(world instanceof ServerWorld sl)) return;
        if (world.getTime() % TICK_INTERVAL != 0) return;

        boolean shouldPower = computeAligned(sl);
        BlockState current = getCachedState();
        if (current.get(FireControllerBlock.POWERED) != shouldPower) {
            world.setBlockState(pos, current.with(FireControllerBlock.POWERED, shouldPower));
            world.updateNeighbors(pos, current.getBlock());
        }
    }

    private boolean computeAligned(ServerWorld sl) {
        if (linkedFiltererPos == null) return false;
        if (!Mods.CREATEBIGCANNONS.isLoaded()) return false;

        NetworkData data = NetworkData.get(sl);
        RegistryKey<World> dim = sl.getRegistryKey();
        NetworkData.Group group = data.getGroup(dim, linkedFiltererPos);
        if (group == null) return false;

        String targetId = group.selectedTargetId;
        if (targetId == null) return false;

        if (group.radarPos == null) return false;
        BlockEntity radarBe = sl.getBlockEntity(group.radarPos);
        if (!(radarBe instanceof IRadar radar) || !radar.isRunning()) return false;

        Collection<RadarTrack> tracks = radar.getTracks();
        RadarTrack target = null;
        for (RadarTrack t : tracks) {
            if (targetId.equals(t.getId())) { target = t; break; }
        }
        if (target == null) return false;

        return CBCCannonDriver.isAligned(sl, pos, target.getPosition(),
                target.getVelocity(), group.targetingTag);
    }

    public void linkToFilterer(ServerWorld sl, BlockPos filtererPos) {
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

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("LinkedFilterer"))
            linkedFiltererPos = NbtHelper.toBlockPos(tag.getCompound("LinkedFilterer"));
        else
            linkedFiltererPos = null;
    }

    @Override
    public void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (linkedFiltererPos != null)
            tag.put("LinkedFilterer", NbtHelper.fromBlockPos(linkedFiltererPos));
    }
}
