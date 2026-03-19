package com.happysg.radar.block.radar.track;

import com.happysg.radar.compat.Mods;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

public enum TrackCategory {
    PLAYER,
    MOB,
    HOSTILE,
    ANIMAL,
    VS2,
    PROJECTILE,
    CONTRAPTION,
    ITEM,
    MISC;

    public static TrackCategory get(Entity entity) {
        if (entity instanceof PlayerEntity) return PLAYER;
        if (entity instanceof HostileEntity) return HOSTILE;
        if (entity instanceof AnimalEntity) return ANIMAL;
        if (entity instanceof MobEntity) return MOB;
        if (entity instanceof ProjectileEntity) return PROJECTILE;
        if (entity instanceof AbstractContraptionEntity) return CONTRAPTION;
        if (entity instanceof ItemEntity) return ITEM;
        return MISC;
    }
}
