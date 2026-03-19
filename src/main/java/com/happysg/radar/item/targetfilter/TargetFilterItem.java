package com.happysg.radar.item.targetfilter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class TargetFilterItem extends Item {

    public TargetFilterItem(Settings properties) {
        super(properties);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
        openScreen();
        return TypedActionResult.success(player.getStackInHand(hand));
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen() {
        MinecraftClient.getInstance().setScreen(new AutoTargetScreen());
    }
}
