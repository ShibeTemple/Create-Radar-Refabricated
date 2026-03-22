package com.happysg.radar.item.detectionfilter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class DetectionFilterItem extends Item {

    public DetectionFilterItem(Settings properties) {
        super(properties);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            openScreen(hand, stack);
        }
        return TypedActionResult.success(stack);
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen(Hand hand, ItemStack stack) {
        net.minecraft.client.MinecraftClient.getInstance()
                .setScreen(new RadarFilterScreen(hand, stack));
    }
}
