package com.happysg.radar.block.radar.receiver;

import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class AbstractRadarFrame extends WrenchableDirectionalBlock {

    private final VoxelShaper shape;

    public AbstractRadarFrame(Settings properties, VoxelShaper shape) {
        super(properties);
        this.shape = shape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape.get(state.get(FACING));
    }
}
