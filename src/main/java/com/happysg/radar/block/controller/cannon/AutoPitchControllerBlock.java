package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.entity.BlockEntityType;

public class AutoPitchControllerBlock extends AbstractWeaponControllerBlock implements IBE<AutoPitchControllerBlockEntity> {

    public AutoPitchControllerBlock(Settings properties) {
        super(properties);
    }

    @Override
    public Class<AutoPitchControllerBlockEntity> getBlockEntityClass() {
        return AutoPitchControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AutoPitchControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.AUTO_PITCH_CONTROLLER.get();
    }
}
