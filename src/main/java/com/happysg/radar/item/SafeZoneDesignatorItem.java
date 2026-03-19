package com.happysg.radar.item;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SafeZoneDesignatorItem extends Item {

    public SafeZoneDesignatorItem(Settings properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (selected) {
            NbtCompound data = stack.getOrCreateNbt();
            if (data.contains("monitorPos")) {
                BlockPos monitorPos = NbtHelper.toBlockPos(data.getCompound("monitorPos"));
                if (world.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitorBlockEntity && world.isClient) {
                    monitorBlockEntity.showSafeZone();
                }
            }
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos pos = context.getBlockPos();
        World world = context.getWorld();
        ItemStack stack = context.getStack();
        NbtCompound data = stack.getOrCreateNbt();
        PlayerEntity player = context.getPlayer();

        if (player == null) return ActionResult.FAIL;

        if (world.getBlockEntity(pos) instanceof MonitorBlockEntity monitorBlockEntity) {
            data.put("monitorPos", NbtHelper.fromBlockPos(monitorBlockEntity.getControllerPos()));
            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.set", Formatting.GREEN);
            return ActionResult.SUCCESS;
        }

        if (!data.contains("monitorPos")) {
            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.no_monitor", Formatting.RED);
            return ActionResult.FAIL;
        }
        BlockPos monitorPos = NbtHelper.toBlockPos(data.getCompound("monitorPos"));

        if (!data.contains("startPos")) {
            if (world.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitorBlockEntity) {
                if (monitorBlockEntity.getController().tryRemoveAABB(pos)) {
                    displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.remove", Formatting.RED);
                    return ActionResult.SUCCESS;
                }
            }
            data.put("startPos", NbtHelper.fromBlockPos(pos));
            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.start", Formatting.GREEN);
        } else {
            if (player.isSneaking()) {
                data.remove("startPos");
                displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.reset", Formatting.RED);
                return ActionResult.SUCCESS;
            }
            BlockPos startPos = NbtHelper.toBlockPos(data.getCompound("startPos"));
            if (world.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitorBlockEntity) {
                monitorBlockEntity.addSafeZone(startPos, pos);
                displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.end", Formatting.GREEN);
                data.remove("startPos");
            } else {
                displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.no_monitor", Formatting.RED);
                return ActionResult.FAIL;
            }
        }
        return ActionResult.SUCCESS;
    }

    private void displayMessage(PlayerEntity player, String key, Formatting color) {
        player.sendMessage(Text.translatable(key).formatted(color), true);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("monitorPos")) {
            BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("monitorPos"));
            tooltip.add(Text.translatable(CreateRadar.MODID + ".guided_fuze.linked_monitor", pos));
        } else {
            tooltip.add(Text.translatable(CreateRadar.MODID + ".guided_fuze.no_monitor"));
        }
    }

    @Nullable
    public BlockPos getMonitorPos(ItemStack stack) {
        NbtCompound data = stack.getOrCreateNbt();
        if (data.contains("monitorPos")) return NbtHelper.toBlockPos(data.getCompound("monitorPos"));
        return null;
    }
}
