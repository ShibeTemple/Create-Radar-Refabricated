package com.happysg.radar.block.radar.bearing;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.radar.receiver.AbstractRadarFrame;
import com.happysg.radar.block.radar.receiver.RadarReceiverBlock;
import com.happysg.radar.registry.ModBlocks;
import com.happysg.radar.registry.ModContraptionTypes;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.structure.StructureTemplate;
import org.apache.commons.lang3.tuple.Pair;

public class RadarContraption extends BearingContraption {

    private int dishCount;
    private boolean hasReceiver;
    private boolean creative;
    private Direction receiverFacing;

    public RadarContraption() {
        facing = Direction.UP;
    }

    @Override
    public boolean assemble(World world, BlockPos pos) throws AssemblyException {
        boolean assembled = super.assemble(world, pos);
        if (!hasReceiver()) {
            throw new AssemblyException(Text.translatable(CreateRadar.MODID + ".radar.no_receiver"));
        }
        return assembled;
    }

    @Override
    public void addBlock(World level, BlockPos pos, Pair<StructureTemplate.StructureBlockInfo, BlockEntity> capture) {
        super.addBlock(level, pos, capture);

        if (ModBlocks.CREATIVE_RADAR_PLATE_BLOCK.has(capture.getKey().state()))
            creative = true;

        if (capture.getKey().state().getBlock() instanceof AbstractRadarFrame)
            dishCount++;

        if (capture.getKey().state().getBlock() instanceof RadarReceiverBlock) {
            hasReceiver = true;
            receiverFacing = capture.getKey().state().get(RadarReceiverBlock.FACING);
        }
    }

    public int getDishCount() { return dishCount; }
    public boolean hasReceiver() { return hasReceiver; }
    public Direction getReceiverFacing() { return receiverFacing; }
    public boolean isCreative() { return creative; }

    @Override
    public ContraptionType getType() {
        return ModContraptionTypes.RADAR_BEARING;
    }
}
