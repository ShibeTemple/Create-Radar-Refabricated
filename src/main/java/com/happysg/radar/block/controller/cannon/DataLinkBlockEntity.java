package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.compat.cbc.CBCCannonDriver;
import com.happysg.radar.registry.ModBlockEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * DataLink: linked to a NetworkFilterer via binoculars.
 * Drives BOTH yaw and pitch of the adjacent cannon mount each tick.
 * Sets link_style=controller when fully linked.
 */
public class DataLinkBlockEntity extends AbstractWeaponControllerBlockEntity {

    public DataLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean driveWeapon(ServerWorld sl, BlockPos controllerPos,
                                   Vec3d targetPos, Vec3d targetVelPerTick, NbtCompound targetingTag) {
        return CBCCannonDriver.driveMount(sl, controllerPos, targetPos, targetVelPerTick, targetingTag);
    }

    @Override
    public void linkToFilterer(ServerWorld sl, BlockPos filtererPos) {
        super.linkToFilterer(sl, filtererPos);
        // Update visual state to controller style
        BlockState state = getCachedState();
        if (state.contains(DataLinkBlock.LINK_STYLE)) {
            sl.setBlockState(pos, state.with(DataLinkBlock.LINK_STYLE, DataLinkBlock.LinkStyle.CONTROLLER));
        }
    }
}
