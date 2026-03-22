package com.happysg.radar.item.targetfilter;

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
        // AutoTargetScreen not yet implemented — do nothing until it's ready
        return TypedActionResult.pass(player.getStackInHand(hand));
    }
}
