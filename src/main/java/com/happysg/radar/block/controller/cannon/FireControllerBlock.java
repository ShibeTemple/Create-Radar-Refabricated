package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.happysg.radar.registry.ModItems;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Outputs a redstone signal when the adjacent cannon mount is aligned
 * with the radar target. Linked to a NetworkFilterer via binoculars.
 */
public class FireControllerBlock extends Block implements IBE<FireControllerBlockEntity> {

    public static final BooleanProperty POWERED = BooleanProperty.of("powered");

    public FireControllerBlock(Settings properties) {
        super(properties);
        setDefaultState(getDefaultState().with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
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
                    if (be instanceof FireControllerBlockEntity ctrl) {
                        ctrl.linkToFilterer(sl, filtererPos);
                        world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                SoundCategory.BLOCKS, 0.5f, 1.2f);
                        player.sendMessage(Text.literal(
                                "Fire controller linked to radar network!").formatted(Formatting.GREEN), true);
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
                if (be instanceof FireControllerBlockEntity ctrl) ctrl.onRemoved(sl);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // ── IBE ──────────────────────────────────────────────────────────────────

    @Override
    public Class<FireControllerBlockEntity> getBlockEntityClass() {
        return FireControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FireControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.FIRE_CONTROLLER.get();
    }
}
