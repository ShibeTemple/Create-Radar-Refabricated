package com.happysg.radar.block.radar.bearing;

import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class RadarBearingBlock extends BearingBlock implements IBE<RadarBearingBlockEntity> {

    public RadarBearingBlock(Settings properties) {
        super(properties);
        setDefaultState(getDefaultState().with(FACING, Direction.UP));
    }

    @Override
    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context) {
        return getDefaultState().with(FACING, Direction.UP);
    }

    @Override
    public Class<RadarBearingBlockEntity> getBlockEntityClass() {
        return RadarBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RadarBearingBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.RADAR_BEARING.get();
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        if (!context.getWorld().isClient) {
            BlockEntity be = context.getWorld().getBlockEntity(context.getBlockPos());
            if (be instanceof RadarBearingBlockEntity radar) {
                radar.disassemble();
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return super.getRenderType(state);
    }

    @Override
    public ActionResult onUse(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.getMainHandStack().isEmpty())
            return ActionResult.PASS;

        // Link mode: player has an active link session
        if (!level.isClient && level instanceof net.minecraft.server.world.ServerWorld sl) {
            if (com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity.hasLinkSession(player.getUuid())) {
                net.minecraft.util.math.BlockPos filtererPos = com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity.getLinkSession(player.getUuid());
                net.minecraft.block.entity.BlockEntity fbe = level.getBlockEntity(filtererPos);
                if (fbe instanceof com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity filterer) {
                    filterer.linkRadar(sl, pos);
                    player.sendMessage(net.minecraft.text.Text.literal("Radar Bearing linked to Network Controller!").formatted(net.minecraft.util.Formatting.GREEN), true);
                    return ActionResult.SUCCESS;
                }
            }
        }

        if (!level.isClient) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RadarBearingBlockEntity radar) {
                if (radar.isRunning()) {
                    radar.disassemble();
                } else {
                    radar.assemble();
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.isOf(newState.getBlock())) {
            super.onStateReplaced(state, level, pos, newState, isMoving);
            return;
        }

        if (!level.isClient && level instanceof ServerWorld sl) {
            NetworkData.get(sl).onEndpointRemoved(sl, pos);
        }

        super.onStateReplaced(state, level, pos, newState, isMoving);
    }
}
