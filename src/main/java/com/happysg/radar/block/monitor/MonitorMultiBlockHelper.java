package com.happysg.radar.block.monitor;

import com.happysg.radar.config.RadarConfig;
import com.happysg.radar.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import static com.happysg.radar.block.monitor.MonitorBlock.SHAPE;

public class MonitorMultiBlockHelper {

    public static void onPlace(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (pState.get(SHAPE) != MonitorBlock.Shape.SINGLE && !pIsMoving) return;

        Direction originFacing = pState.get(HorizontalFacingBlock.FACING);
        int maxSize = RadarConfig.server().monitorMaxSize.get();

        BlockPos.iterate(
                (int)(pPos.getX() - maxSize), (int)(pPos.getY() - maxSize), (int)(pPos.getZ() - maxSize),
                (int)(pPos.getX() + maxSize), (int)(pPos.getY() + maxSize), (int)(pPos.getZ() + maxSize)
        ).forEach(candidate -> {
            BlockState candState = pLevel.getBlockState(candidate);
            if (!candState.isOf(ModBlocks.MONITOR.get())) return;
            if (candState.get(HorizontalFacingBlock.FACING) != originFacing) return;

            if (pLevel.getBlockEntity(candidate) instanceof MonitorBlockEntity monitor) {
                int size = getSize(pLevel, candidate);
                if (size > 1) {
                    formMulti(pState, pLevel, monitor.getControllerPos(), size);
                }
            }
        });
    }

    public static void onRemove(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (ModBlocks.MONITOR.has(pNewState) && !pIsMoving) return;
        if (pLevel.getBlockEntity(pPos) instanceof MonitorBlockEntity monitor) {
            destroyMulti(pState, pLevel, pPos, monitor.getControllerPos(), monitor.getSize());
        }
    }

    public static void onNeighborChange(BlockState state, World level, BlockPos pos, BlockPos neighbor) {
        // Handle neighbor changes for multi-block
    }

    static void formMulti(BlockState pState, World pLevel, BlockPos pPos, int size) {
        Direction facing = pState.get(HorizontalFacingBlock.FACING);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                MonitorBlock.Shape shape;
                if (i == 0 && j == 0) shape = MonitorBlock.Shape.LOWER_RIGHT;
                else if (i == 0 && j == size - 1) shape = MonitorBlock.Shape.LOWER_LEFT;
                else if (i == size - 1 && j == 0) shape = MonitorBlock.Shape.UPPER_RIGHT;
                else if (i == size - 1 && j == size - 1) shape = MonitorBlock.Shape.UPPER_LEFT;
                else if (i == 0) shape = MonitorBlock.Shape.LOWER_CENTER;
                else if (i == size - 1) shape = MonitorBlock.Shape.UPPER_CENTER;
                else if (j == 0) shape = MonitorBlock.Shape.MIDDLE_RIGHT;
                else if (j == size - 1) shape = MonitorBlock.Shape.MIDDLE_LEFT;
                else if (size == 3 && i == 1 && j == 1) shape = MonitorBlock.Shape.CENTER;
                else shape = MonitorBlock.Shape.SINGLE;

                BlockPos target = getMonitorPos(pPos, facing, i, j);
                if (!(pLevel.getBlockState(target).isOf(ModBlocks.MONITOR.get()))) continue;

                pLevel.setBlockState(target, pLevel.getBlockState(target).with(SHAPE, shape));
                if (pLevel.getBlockEntity(target) instanceof MonitorBlockEntity monitorBe) {
                    monitorBe.setControllerPos(pPos, size);
                }
            }
        }
    }

    static void destroyMulti(BlockState pState, World pLevel, BlockPos pPos, BlockPos controllerPos, int size) {
        if (size <= 1) return;
        Direction facing = pState.get(HorizontalFacingBlock.FACING);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                BlockPos target = getMonitorPos(controllerPos, facing, i, j);
                if (target.equals(pPos)) continue;
                if (!pLevel.getBlockState(target).isOf(ModBlocks.MONITOR.get())) continue;
                pLevel.setBlockState(target, pLevel.getBlockState(target).with(SHAPE, MonitorBlock.Shape.SINGLE));
                if (pLevel.getBlockEntity(target) instanceof MonitorBlockEntity monitorBe) {
                    monitorBe.setControllerPos(target, 1);
                }
            }
        }
    }

    private static BlockPos getMonitorPos(BlockPos origin, Direction facing, int up, int right) {
        // Calculate position based on facing direction
        Direction rightDir = facing.rotateYClockwise();
        return origin.up(up).offset(rightDir, right);
    }

    public static int getSize(World level, BlockPos pos) {
        if (!level.getBlockState(pos).isOf(ModBlocks.MONITOR.get()))
            return 0;
        Direction facing = level.getBlockState(pos).get(HorizontalFacingBlock.FACING);
        int potentialsize = 0;
        for (int i = 0; i < RadarConfig.server().monitorMaxSize.get(); i++) {
            boolean valid = true;
            for (BlockPos p : BlockPos.iterate(pos, pos.up(i).offset(facing.rotateYClockwise(), i))) {
                if (!level.getBlockState(p).isOf(ModBlocks.MONITOR.get())) {
                    valid = false;
                    break;
                }
            }
            if (valid)
                potentialsize = i + 1;
            else
                break;
        }
        if (potentialsize == 1)
            return 1;

        for (int i = 0; i < potentialsize; i++) {
            for (int j = 0; j < potentialsize; j++) {
                BlockEntity be = level.getBlockEntity(pos.up(i).offset(facing.rotateYClockwise(), j));
                if (!(be instanceof MonitorBlockEntity monitor && monitor.getSize() < potentialsize))
                    return Math.max(1, Math.min(i, j));
            }
        }
        return potentialsize;
    }
}
