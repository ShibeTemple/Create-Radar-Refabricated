package com.happysg.radar.item.binos;

import com.happysg.radar.CreateRadar;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SpyglassItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Binoculars extends SpyglassItem {
    private static final String TAG_LAST_HIT = "LastHitPos";

    public Binoculars(Settings properties) {
        super(properties);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        if (net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
            tooltip.add(Text.translatable(CreateRadar.MODID + ".binoculars.base_text"));
        }
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("filtererPos")) {
            BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("filtererPos"));
            tooltip.add(Text.translatable(CreateRadar.MODID + ".binoculars.controller").append(": " + pos.toShortString()));
        } else {
            tooltip.add(Text.translatable(CreateRadar.MODID + ".binoculars.no_controller"));
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos clickedPos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        ItemStack held = context.getStack();
        if (player == null) return super.useOnBlock(context);

        // Right-clicking a NetworkFilterer: store its position in the binoculars
        if (world.getBlockEntity(clickedPos) instanceof com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity) {
            if (!world.isClient) {
                held.getOrCreateNbt().put("filtererPos", NbtHelper.fromBlockPos(clickedPos));
                player.sendMessage(Text.translatable("create_radar.binoculars.paired").formatted(Formatting.BLUE), true);
            }
            return ActionResult.success(world.isClient);
        }

        // Right-clicking a RadarBearing or Monitor with stored filtererPos: link it
        if (held.hasNbt() && held.getNbt().contains("filtererPos")) {
            BlockPos filtererPos = NbtHelper.toBlockPos(held.getNbt().getCompound("filtererPos"));
            if (!world.isClient && world instanceof ServerWorld sl) {
                if (world.getBlockEntity(filtererPos) instanceof com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity filterer) {
                    if (world.getBlockState(clickedPos).getBlock() instanceof com.happysg.radar.block.radar.bearing.RadarBearingBlock) {
                        filterer.linkRadar(sl, clickedPos);
                        player.sendMessage(Text.literal("Radar linked to filterer at " + filtererPos.toShortString()).formatted(Formatting.GREEN), true);
                        return ActionResult.success(true);
                    }
                    if (world.getBlockState(clickedPos).getBlock() instanceof com.happysg.radar.block.monitor.MonitorBlock) {
                        BlockPos monitorController = clickedPos;
                        if (world.getBlockEntity(clickedPos) instanceof com.happysg.radar.block.monitor.MonitorBlockEntity monitorBe) {
                            monitorController = monitorBe.getControllerPos();
                        }
                        filterer.linkMonitor(sl, monitorController);
                        player.sendMessage(Text.literal("Monitor linked to filterer at " + filtererPos.toShortString()).formatted(Formatting.GREEN), true);
                        return ActionResult.success(true);
                    }
                }
            } else if (world.isClient) {
                // On client, allow the action if we're targeting radar or monitor
                if (world.getBlockState(clickedPos).getBlock() instanceof com.happysg.radar.block.radar.bearing.RadarBearingBlock ||
                    world.getBlockState(clickedPos).getBlock() instanceof com.happysg.radar.block.monitor.MonitorBlock) {
                    return ActionResult.SUCCESS;
                }
            }
        }

        return super.useOnBlock(context);
    }

    public static void setLastHit(ItemStack stack, @Nullable BlockPos pos) {
        NbtCompound tag = stack.getOrCreateNbt();
        if (pos == null) {
            tag.remove(TAG_LAST_HIT);
            return;
        }
        NbtCompound hit = new NbtCompound();
        hit.putInt("x", pos.getX());
        hit.putInt("y", pos.getY());
        hit.putInt("z", pos.getZ());
        tag.put(TAG_LAST_HIT, hit);
    }

    @Nullable
    public static BlockPos getLastHit(ItemStack stack) {
        NbtCompound tag = stack.getNbt();
        if (tag == null || !tag.contains(TAG_LAST_HIT)) return null;
        NbtCompound hit = tag.getCompound(TAG_LAST_HIT);
        return new BlockPos(hit.getInt("x"), hit.getInt("y"), hit.getInt("z"));
    }

    public static boolean hasLastHit(ItemStack stack) {
        NbtCompound tag = stack.getNbt();
        return tag != null && tag.contains(TAG_LAST_HIT);
    }

    public static void clearLastHit(ItemStack stack) {
        NbtCompound tag = stack.getNbt();
        if (tag != null) tag.remove(TAG_LAST_HIT);
    }
}
