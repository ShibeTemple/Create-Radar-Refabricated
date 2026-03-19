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
        if (level instanceof ServerWorld sl) {
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
        if (!player.getMainHandStack().isEmpty() || hand == Hand.OFF_HAND)
            return ActionResult.PASS;

        if (RadarConfig.client().useGuiByDefault.get()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonitorBlockEntity monitor) {
                if (level.isClient) {
                    openMonitorScreenClient(monitor);
                }
                return ActionResult.success(level.isClient);
            }
        }

        if (player.isSneaking()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MonitorBlockEntity monitor && isGuiHotspot(monitor, hit)) {
                if (level.isClient) {
                    openMonitorScreenClient(monitor);
                }
                return ActionResult.success(level.isClient);
            }
        }

        return onBlockEntityUse(level, pos, monitorBlockEntity ->
                MonitorInputHandler.onUse(monitorBlockEntity.getController(), player, hand, hit, state.get(FACING)));
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

    private static boolean isGuiHotspot(MonitorBlockEntity anyPiece, BlockHitResult hit) {
        if (anyPiece == null || anyPiece.getWorld() == null) return false;

        MonitorBlockEntity controller = anyPiece.isController() ? anyPiece : anyPiece.getController();
        if (controller == null) return false;

        Direction screenFace = controller.getCachedState().get(FACING);
        if (hit.getSide() != screenFace) return false;

        BlockPos controllerPos = controller.getControllerPos();
        if (controllerPos == null) controllerPos = controller.getPos();
        if (!hit.getBlockPos().equals(controllerPos)) return false;

        int size = controller.getSize();
        if (size <= 0) return false;

        int stripPx = (size == 1) ? 3 : 6;
        float epsY = stripPx / 16f;
        Vec3d local = hit.getPos().subtract(controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());
        float v = 1f - (float) local.y;
        return v >= 1f - epsY;
    }

    private void openMonitorScreenClient(MonitorBlockEntity anyPiece) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            BlockPos pos = anyPiece.getPos();
            ClientHelper.openMonitorScreen(pos);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHelper {
        static void openMonitorScreen(BlockPos clickedPos) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.world == null) return;
            BlockEntity be = mc.world.getBlockEntity(clickedPos);
            if (!(be instanceof MonitorBlockEntity anyPiece)) return;
            MonitorBlockEntity controller = anyPiece.isController() ? anyPiece : anyPiece.getController();
            if (controller == null) return;
            BlockPos controllerPos = controller.getControllerPos();
            if (controllerPos == null) controllerPos = controller.getPos();
            mc.setScreen(new MonitorScreen(controllerPos));
        }
    }
}
