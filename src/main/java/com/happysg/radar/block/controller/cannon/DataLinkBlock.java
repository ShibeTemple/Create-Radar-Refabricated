package com.happysg.radar.block.controller.cannon;

import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;

public class DataLinkBlock extends AbstractWeaponControllerBlock implements IBE<DataLinkBlockEntity> {

    public enum LinkStyle implements StringIdentifiable {
        RADAR("radar"), CONTROLLER("controller");
        private final String name;
        LinkStyle(String name) { this.name = name; }
        @Override public String asString() { return name; }
    }

    public static final EnumProperty<LinkStyle> LINK_STYLE =
            EnumProperty.of("link_style", LinkStyle.class);

    public DataLinkBlock(Settings properties) {
        super(properties);
        setDefaultState(getDefaultState().with(LINK_STYLE, LinkStyle.RADAR));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LINK_STYLE);
    }

    @Override
    public Class<DataLinkBlockEntity> getBlockEntityClass() {
        return DataLinkBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DataLinkBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.DATA_LINK.get();
    }
}
