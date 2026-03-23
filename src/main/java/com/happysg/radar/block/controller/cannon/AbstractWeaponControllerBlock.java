package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.registry.ModItems;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.world.World;

/**
 * Common block behaviour for all weapon-controller blocks
 * (DataLink, AutoYaw, AutoPitch, FireController).
 *
 * Binoculars pairing (one step):
 *  – The player first right-clicks a NetworkFilterer with binoculars → "radarFiltererPos"
 *    is stored in the binoculars NBT (handled by NetworkFiltererBlock).
 *  – Then the player right-clicks this weapon-controller block → the block reads
 *    "radarFiltererPos" and links itself to that filterer.
 */
public abstract class AbstractWeaponControllerBlock extends WrenchableDirectionalBlock {

    public AbstractWeaponControllerBlock(Settings properties) {
        super(properties);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getSide());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.OFF_HAND) return ActionResult.PASS;
        ItemStack held = player.getStackInHand(hand);

        if (held.isOf(ModItems.BINOCULARS.get())) {
            if (!world.isClient && world instanceof ServerWorld sl) {
                NbtCompound nbt = held.getOrCreateNbt();
                if (nbt.contains("radarFiltererPos")) {
                    BlockPos filtererPos = NbtHelper.toBlockPos(nbt.getCompound("radarFiltererPos"));
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof AbstractWeaponControllerBlockEntity ctrl) {
                        ctrl.linkToFilterer(sl, filtererPos);
                        world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                SoundCategory.BLOCKS, 0.5f, 1.2f);
                        player.sendMessage(Text.literal(
                                "Cannon controller linked to radar network!").formatted(Formatting.GREEN), true);
                    }
                    nbt.remove("radarFiltererPos");
                    if (nbt.isEmpty()) held.setNbt(null);
                } else {
                    player.sendMessage(Text.literal(
                            "First right-click the Radar Network Controller with binoculars, then right-click here.")
                            .formatted(Formatting.YELLOW), true);
                }
            }
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient && world instanceof ServerWorld sl) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof AbstractWeaponControllerBlockEntity ctrl) {
                    ctrl.onRemoved(sl);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
