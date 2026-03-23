package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.entity.BlockEntityType;

public class AutoYawControllerBlock extends AbstractWeaponControllerBlock implements IBE<AutoYawControllerBlockEntity> {

    public AutoYawControllerBlock(Settings properties) {
        super(properties);
    }

    @Override
    public Class<AutoYawControllerBlockEntity> getBlockEntityClass() {
        return AutoYawControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AutoYawControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.AUTO_YAW_CONTROLLER.get();
    }
}
