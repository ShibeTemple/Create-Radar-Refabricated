package com.happysg.radar.block.controller.networkcontroller;

import com.happysg.radar.block.monitor.MonitorBlock;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarBearingBlock;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.happysg.radar.registry.ModItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.block.ShapeContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class NetworkFiltererBlock extends WrenchableDirectionalBlock implements IBE<NetworkFiltererBlockEntity> {

    public NetworkFiltererBlock(Settings properties) {
        super(properties);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getSide());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return AllShapes.DATA_GATHERER.get(state.get(FACING));
    }

    @Override
    public Class<NetworkFiltererBlockEntity> getBlockEntityClass() {
        return NetworkFiltererBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NetworkFiltererBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.NETWORK_FILTERER.get();
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (!world.isClient && type == ModBlockEntityTypes.NETWORK_FILTERER.get()) {
            return (w, p, s, be) -> NetworkFiltererBlockEntity.tick(w, p, s, (NetworkFiltererBlockEntity) be);
        }
        return null;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.OFF_HAND) return ActionResult.PASS;
        ItemStack held = player.getStackInHand(hand);

        // ── Binoculars: two-step pairing ────────────────────────────────────────
        // Step 1: click radar-side controller → auto-detect adjacent radar bearing,
        //         store this filterer pos in binoculars NBT as "radarFiltererPos".
        // Step 2: click monitor-side controller → auto-detect adjacent monitor,
        //         copy radar pos from the stored filterer, complete the pairing.
        if (held.isOf(ModItems.BINOCULARS.get())) {
            if (!world.isClient && world instanceof ServerWorld sl) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof NetworkFiltererBlockEntity filterer) {
                    NbtCompound nbt = held.getOrCreateNbt();

                    if (nbt.contains("radarFiltererPos")) {
                        // ── Step 2: monitor-side controller ──────────────────────
                        BlockPos radarFiltPos = NbtHelper.toBlockPos(nbt.getCompound("radarFiltererPos"));
                        BlockEntity radarFiltBe = world.getBlockEntity(radarFiltPos);

                        if (radarFiltBe instanceof NetworkFiltererBlockEntity radarFilt) {
                            autoLinkAdjacentMonitor(sl, filterer, pos, world);

                            BlockPos radarPos = radarFilt.getLinkedRadarPos();
                            if (radarPos != null) {
                                filterer.linkRadar(sl, radarPos);
                                player.sendMessage(Text.literal(
                                        "Paired! Monitor will now display radar data.").formatted(Formatting.GREEN), true);
                            } else {
                                player.sendMessage(Text.literal(
                                        "Radar controller has no radar bearing nearby — place it adjacent to the bearing first.").formatted(Formatting.YELLOW), true);
                            }
                        }

                        nbt.remove("radarFiltererPos");
                        if (nbt.isEmpty()) held.setNbt(null);

                    } else {
                        // ── Step 1: radar-side controller ─────────────────────────
                        boolean found = autoLinkAdjacentRadar(sl, filterer, pos, world);
                        held.getOrCreateNbt().put("radarFiltererPos", NbtHelper.fromBlockPos(pos));

                        if (found) {
                            player.sendMessage(Text.literal(
                                    "Radar controller selected. Right-click the monitor's network controller to pair.").formatted(Formatting.AQUA), true);
                        } else {
                            player.sendMessage(Text.literal(
                                    "No radar bearing adjacent — place this controller next to the bearing, then retry.").formatted(Formatting.YELLOW), true);
                        }
                    }
                }
            }
            return ActionResult.success(world.isClient);
        }

        // ── Filter items: insert into appropriate slot ───────────────────────────
        if (!held.isEmpty()) {
            int slot = -1;
            if (held.isOf(ModItems.RADAR_FILTER_ITEM.get())) slot = 0;
            else if (held.isOf(ModItems.IDENT_FILTER_ITEM.get())) slot = 1;
            else if (held.isOf(ModItems.TARGET_FILTER_ITEM.get())) slot = 2;

            if (slot >= 0) {
                if (!world.isClient) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof NetworkFiltererBlockEntity filterer) {
                        if (filterer.getStack(slot).isEmpty()) {
                            ItemStack toInsert = held.copy();
                            toInsert.setCount(1);
                            filterer.setStack(slot, toInsert);
                            held.decrement(1);
                            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.6f, 1.0f);
                            player.sendMessage(Text.translatable("create_radar.network_filter.success").formatted(Formatting.GREEN), true);
                        } else {
                            player.sendMessage(Text.translatable("create_radar.network_filter.invalid").formatted(Formatting.RED), true);
                        }
                    }
                }
                return ActionResult.success(world.isClient);
            }
        }

        // ── Sneak + empty hand: extract first filled filter slot ─────────────────
        if (held.isEmpty() && player.isSneaking()) {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof NetworkFiltererBlockEntity filterer) {
                    for (int i = 0; i < 3; i++) {
                        ItemStack stack = filterer.getStack(i);
                        if (!stack.isEmpty()) {
                            filterer.setStack(i, ItemStack.EMPTY);
                            player.getInventory().offerOrDrop(stack.copy());
                            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.6f, 1.0f);
                            break;
                        }
                    }
                }
            }
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

    /** Searches the 6 adjacent blocks for a radar bearing; links it if found. */
    private static boolean autoLinkAdjacentRadar(ServerWorld sl, NetworkFiltererBlockEntity filterer,
                                                  BlockPos pos, World world) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (world.getBlockState(neighbor).getBlock() instanceof RadarBearingBlock) {
                filterer.linkRadar(sl, neighbor);
                return true;
            }
        }
        return false;
    }

    /** Searches the 6 adjacent blocks for a monitor; links its controller if found. */
    private static void autoLinkAdjacentMonitor(ServerWorld sl, NetworkFiltererBlockEntity filterer,
                                                 BlockPos pos, World world) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (world.getBlockState(neighbor).getBlock() instanceof MonitorBlock) {
                BlockPos controllerPos = neighbor;
                if (world.getBlockEntity(neighbor) instanceof MonitorBlockEntity monBe)
                    controllerPos = monBe.getControllerPos();
                filterer.linkMonitor(sl, controllerPos);
                return;
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient && world instanceof ServerWorld sl) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof NetworkFiltererBlockEntity filterer) {
                    filterer.dissolveNetwork(sl);
                    for (int i = 0; i < 3; i++) {
                        ItemStack stack = filterer.getStack(i);
                        if (!stack.isEmpty()) Block.dropStack(world, pos, stack);
                    }
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
