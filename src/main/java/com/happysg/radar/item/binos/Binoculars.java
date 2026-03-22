package com.happysg.radar.item.binos;

import com.happysg.radar.CreateRadar;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpyglassItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
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
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("radarFiltererPos")) {
            BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("radarFiltererPos"));
            tooltip.add(Text.literal("Radar controller: " + pos.toShortString()));
        }
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
