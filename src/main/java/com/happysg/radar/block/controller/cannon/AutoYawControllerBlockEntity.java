package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.compat.cbc.CBCCannonDriver;
import com.happysg.radar.registry.ModBlockEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Drives only the YAW of the adjacent cannon mount toward the radar target. */
public class AutoYawControllerBlockEntity extends AbstractWeaponControllerBlockEntity {

    public AutoYawControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean driveWeapon(ServerWorld sl, BlockPos controllerPos,
                                   Vec3d targetPos, Vec3d targetVelPerTick, NbtCompound targetingTag) {
        return CBCCannonDriver.driveYaw(sl, controllerPos, targetPos, targetVelPerTick, targetingTag);
    }
}
