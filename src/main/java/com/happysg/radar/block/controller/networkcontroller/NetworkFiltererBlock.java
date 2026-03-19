package com.happysg.radar.block.controller.networkcontroller;

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
import net.minecraft.item.ItemStack;
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
        ItemStack held = player.getStackInHand(hand);

        // Binoculars: store filterer position
        if (held.isOf(ModItems.BINOCULARS.get())) {
            if (!world.isClient) {
                held.getOrCreateNbt().put("filtererPos", NbtHelper.fromBlockPos(pos));
                player.sendMessage(Text.translatable("create_radar.binoculars.paired").formatted(Formatting.BLUE), true);
            }
            return ActionResult.success(world.isClient);
        }

        // Filter items: insert into appropriate slot
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

        // Empty hand + sneaking: extract first filled slot
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
