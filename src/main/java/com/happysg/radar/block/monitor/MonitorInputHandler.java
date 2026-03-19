package com.happysg.radar.block.monitor;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

public class MonitorInputHandler {

    public static ActionResult onUse(MonitorBlockEntity controller, PlayerEntity player, Hand hand,
                                     BlockHitResult hit, Direction facing) {
        // TODO: implement monitor interaction
        return ActionResult.PASS;
    }
}
