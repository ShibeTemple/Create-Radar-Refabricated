package com.happysg.radar.block.monitor;

import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.config.RadarConfig;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.lang.Lang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MonitorBlock extends HorizontalFacingBlock implements IBE<MonitorBlockEntity> {

    public static final EnumProperty<Shape> SHAPE = EnumProperty.of("shape", Shape.class);

    public MonitorBlock(Settings properties) {
        super(properties);
        setDefaultState(getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(SHAPE, Shape.SINGLE));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState()
                .with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onBlockAdded(BlockState state, World level, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, level, pos, oldState, notify);
        MonitorMultiBlockHelper.onPlace(state, level, pos, oldState, notify);
    }

    @Override
    public void onStateReplaced(BlockState state, World level, BlockPos pos, BlockState newState, boolean moved) {
        MonitorMultiBlockHelper.onRemove(state, level, pos, newState, moved);
        if (!state.isOf(newState.getBlock()) && level instanceof ServerWorld sl) {
            NetworkData.get(sl).onEndpointRemoved(sl, pos);
        }
        super.onStateReplaced(state, level, pos, newState, moved);
    }

    @Override
    public void neighborUpdate(BlockState state, World level, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        MonitorMultiBlockHelper.onNeighborChange(state, level, pos, sourcePos);
        super.neighborUpdate(state, level, pos, sourceBlock, sourcePos, notify);
    }

    @Override
    public ActionResult onUse(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.OFF_HAND) return ActionResult.PASS;
        if (level.isClient) return ActionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MonitorBlockEntity be) {
            return MonitorInputHandler.onUse(be, player, hand, hit, state.get(FACING));
        }
        return ActionResult.PASS;
    }

    public enum Shape implements StringIdentifiable {
        SINGLE, CENTER, LOWER_CENTER, LOWER_LEFT, LOWER_RIGHT,
        UPPER_CENTER, UPPER_LEFT, UPPER_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT;

        @Override
        public @NotNull String asString() {
            return Lang.asId(name());
        }
    }

    @Override
    public Class<MonitorBlockEntity> getBlockEntityClass() {
        return MonitorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MonitorBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.MONITOR.get();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(SHAPE);
        super.appendProperties(builder);
    }

}
