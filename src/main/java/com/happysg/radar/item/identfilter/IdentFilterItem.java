package com.happysg.radar.item.identfilter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class IdentFilterItem extends Item {

    public IdentFilterItem(Settings properties) {
        super(properties);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        // IdentificationFilterScreen not yet implemented — do nothing until it's ready
        return TypedActionResult.pass(player.getStackInHand(hand));
    }
}
