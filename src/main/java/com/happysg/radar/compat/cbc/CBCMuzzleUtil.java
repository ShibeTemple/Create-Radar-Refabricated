package com.happysg.radar.compat.cbc;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.entity.BlockEntity;

import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.IAutocannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.big_cannons.IBigCannonBlockEntity;

public final class CBCMuzzleUtil {
    private CBCMuzzleUtil() {}

    public static BlockPos getMuzzleExitLocal(AbstractMountedCannonContraption cannon) {
        if (cannon == null) return null;

        Direction dir = cannon.initialOrientation();
        if (dir == null) return null;

        BlockPos start = cannon.getStartPos();
        if (start == null) start = BlockPos.ORIGIN;

        if (cannon instanceof MountedBigCannonContraption) {
            BlockPos cur = start.toImmutable();
            while (true) {
                BlockEntity be = cannon.presentBlockEntities.get(cur);
                if (!(be instanceof IBigCannonBlockEntity)) break;
                cur = cur.offset(dir);
            }
            return cur;
        }

        if (cannon instanceof MountedAutocannonContraption) {
            BlockPos cur = start.offset(dir).toImmutable();
            while (true) {
                BlockEntity be = cannon.presentBlockEntities.get(cur);
                if (!(be instanceof IAutocannonBlockEntity)) break;
                cur = cur.offset(dir);
            }
            return cur;
        }

        return null;
    }

    public static Vec3d getCBCSpawnAnchorWorld(PitchOrientedContraptionEntity poce) {
        if (poce == null) return Vec3d.ZERO;

        if (!(poce.getContraption() instanceof AbstractMountedCannonContraption cannon)) {
            return poce.toGlobalVector(Vec3d.ofCenter(BlockPos.ORIGIN), 0);
        }

        BlockPos outside = getMuzzleExitLocal(cannon);
        if (outside == null) {
            return poce.toGlobalVector(Vec3d.ofCenter(BlockPos.ORIGIN), 0);
        }

        Direction dir = cannon.initialOrientation();
        BlockPos spawnAnchorLocal = outside.offset(dir);
        return poce.toGlobalVector(Vec3d.ofCenter(spawnAnchorLocal), 0);
    }

    public static Vec3d getForwardWorld(PitchOrientedContraptionEntity poce) {
        if (poce == null) return Vec3d.ZERO;
        Vec3d center = poce.toGlobalVector(Vec3d.ofCenter(BlockPos.ORIGIN), 0);
        Vec3d ahead  = poce.toGlobalVector(Vec3d.ofCenter(BlockPos.ORIGIN.offset(poce.getInitialOrientation())), 0);
        Vec3d v = ahead.subtract(center);
        return v.lengthSquared() < 1e-8 ? Vec3d.ZERO : v.normalize();
    }
}
