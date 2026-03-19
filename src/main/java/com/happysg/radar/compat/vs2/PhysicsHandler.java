package com.happysg.radar.compat.vs2;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Simplified PhysicsHandler for Fabric port.
 * Valkyrien Skies 2 support is optional and not yet implemented for Fabric.
 */
public class PhysicsHandler {

    public static BlockPos getWorldPos(World level, BlockPos pos) {
        return pos;
    }

    public static Vec3d getShipVec(Vec3d vec3, BlockEntity be) {
        return vec3;
    }

    public static Vec3d getWorldVecDirectionTransform(Vec3d vec3, BlockEntity be) {
        return vec3;
    }

    public static BlockPos getWorldPos(BlockEntity blockEntity) {
        return blockEntity.getPos();
    }

    public static Vec3d getWorldVec(World level, BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3d getWorldVec(World level, Vec3d vec3) {
        return vec3;
    }

    public static Vec3d getWorldVec(BlockEntity blockEntity) {
        return Vec3d.ofCenter(blockEntity.getPos());
    }

    public static boolean isBlockInShipyard(World level, BlockPos blockPos) {
        return false;
    }
}
