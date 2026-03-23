package com.happysg.radar.compat.cbc;

import com.happysg.radar.mixin.AbstractCannonAccessor;
import com.happysg.radar.mixin.AutoCannonAccessor;
import com.happysg.radar.mixin.AutocannonProjectileAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.IAutocannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.breech.AbstractAutocannonBreechBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterial;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBehavior;
import rbasamoyai.createbigcannons.cannons.big_cannons.IBigCannonBlockEntity;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonAmmoItem;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;
import rbasamoyai.createbigcannons.munitions.big_cannon.propellant.BigCannonPropellantBlock;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CannonUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CannonUtil.class);

    /**
     * Cache for {@code getBallisticProperties()} Method lookups per concrete projectile class.
     * Avoids repeated getDeclaredMethod + setAccessible on every targeting cycle.
     * Empty Optional = lookup was tried and failed (no such method on that class).
     */
    private static final ConcurrentHashMap<Class<?>, Optional<Method>> BALLISTIC_METHOD_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Invokes {@code getBallisticProperties()} on {@code projectile} via a cached reflective
     * Method, or returns {@code null} if the method does not exist or the call fails.
     */
    private static BallisticPropertiesComponent invokeBallisticProperties(AbstractBigCannonProjectile projectile) {
        Optional<Method> cached = BALLISTIC_METHOD_CACHE.computeIfAbsent(projectile.getClass(), cls -> {
            try {
                Method m = cls.getDeclaredMethod("getBallisticProperties");
                m.setAccessible(true);
                return Optional.of(m);
            } catch (Throwable t) {
                return Optional.empty();
            }
        });
        if (cached.isEmpty()) return null;
        try {
            return (BallisticPropertiesComponent) cached.get().invoke(projectile);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final BallisticPropertiesComponent AC_FALLBACK = new BallisticPropertiesComponent(-0.025, 0.01, false, 0, 0, 0, 0);

    public static boolean isAutocannonFamily(AbstractMountedCannonContraption cannon) {
        return isAutoCannon(cannon);
    }

    public static int getBarrelLength(AbstractMountedCannonContraption cannon) {
        if (cannon == null) return 0;
        if (cannon.initialOrientation() == Direction.WEST || cannon.initialOrientation() == Direction.NORTH) {
            return ((AbstractCannonAccessor) cannon).getBackExtensionLength();
        } else {
            return ((AbstractCannonAccessor) cannon).getFrontExtensionLength();
        }
    }

    public static Vec3d getCannonMountOffset(World world, BlockPos pos) {
        return getCannonMountOffset(world.getBlockEntity(pos));
    }

    public static Vec3d getCannonMountOffset(BlockEntity mount) {
        if (mount == null) return Vec3d.ZERO;
        return isUp(mount) ? new Vec3d(0, 2, 0) : new Vec3d(0, -2, 0);
    }

    public static BallisticPropertiesComponent getAutocannonBallistics(AbstractMountedCannonContraption cannon, World world) {
        if (cannon == null || world == null) return AC_FALLBACK;

        Function<ItemStack, BallisticPropertiesComponent> fromCBCAmmo = (stack) -> {
            if (stack == null || stack.isEmpty()) return AC_FALLBACK;
            if (!(stack.getItem() instanceof AutocannonAmmoItem ammo)) return BallisticPropertiesComponent.DEFAULT;

            AbstractAutocannonProjectile proj = ammo.getAutocannonProjectile(stack, world);
            if (proj == null) return BallisticPropertiesComponent.DEFAULT;

            return ((AutocannonProjectileAccessor) proj).getBallisticProperties();
        };

        for (BlockEntity be : cannon.presentBlockEntities.values()) {
            if (be instanceof AbstractAutocannonBreechBlockEntity b) {
                ItemStack round = b.getInputBuffer().peek();
                if (round == null) round = ItemStack.EMPTY;
                return fromCBCAmmo.apply(round);
            }
        }

        return AC_FALLBACK;
    }

    public static BallisticPropertiesComponent getBallistics(AbstractMountedCannonContraption cannon, ServerWorld level) {
        if (cannon == null || level == null) return BallisticPropertiesComponent.DEFAULT;

        if (isAutocannonFamily(cannon)) {
            return getAutocannonBallistics(cannon, level);
        }

        Map<BlockPos, BlockEntity> presentBlockEntities = cannon.presentBlockEntities;
        for (BlockEntity blockEntity : presentBlockEntities.values()) {
            if (!(blockEntity instanceof IBigCannonBlockEntity cannonBlockEntity)) continue;

            BigCannonBehavior behavior = cannonBlockEntity.cannonBehavior();
            StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();
            Block block = containedBlockInfo.state().getBlock();

            if (block instanceof ProjectileBlock<?> projectileBlock) {
                AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, Collections.singletonList(containedBlockInfo));
                BallisticPropertiesComponent bp = invokeBallisticProperties(projectile);
                return bp != null ? bp : BallisticPropertiesComponent.DEFAULT;
            }
        }

        return BallisticPropertiesComponent.DEFAULT;
    }

    public static int getBigCannonSpeed(ServerWorld level, AbstractMountedCannonContraption cannon, PitchOrientedContraptionEntity contraptionEntity) {
        if (contraptionEntity == null) return 0;

        Map<BlockPos, BlockEntity> presentBlockEntities = cannon.presentBlockEntities;
        int speed = 0;
        for (BlockEntity blockEntity : presentBlockEntities.values()) {
            if (!(blockEntity instanceof IBigCannonBlockEntity cannonBlockEntity)) continue;
            BigCannonBehavior behavior = cannonBlockEntity.cannonBehavior();
            StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();

            Block block = containedBlockInfo.state().getBlock();
            if (block instanceof BigCannonPropellantBlock propellantBlock) {
                speed += (int) propellantBlock.getChargePower(containedBlockInfo);
            } else if (block instanceof ProjectileBlock<?> projectileBlock) {
                AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, Collections.singletonList(containedBlockInfo));
                speed += (int) projectile.addedChargePower();
            }
        }
        return speed;
    }

    public static float getInitialVelocity(AbstractMountedCannonContraption cannon, ServerWorld level) {
        if (cannon == null) return 0f;

        if (isBigCannon(cannon)) {
            return getBigCannonSpeed(level, cannon, (PitchOrientedContraptionEntity) cannon.entity);
        } else if (isAutoCannon(cannon)) {
            return getAutoCannonSpeed(cannon);
        }
        return 0;
    }

    public static int getAutocannonLifetimeTicks(AbstractMountedCannonContraption cannon) {
        if (cannon == null) return 100;
        if (!isAutoCannon(cannon)) return 100;

        try {
            AutocannonMaterial mat = ((AutoCannonAccessor) cannon).getMaterial();
            if (mat != null) {
                int t = mat.properties().projectileLifetime();
                if (t > 0) return t;
            }
        } catch (Throwable ignored) {
            LOGGER.debug("Mixin maybe didn't apply?");
        }

        return 100;
    }

    public static double getMaxProjectileRangeBlocks(AbstractMountedCannonContraption cannon, ServerWorld level) {
        if (cannon == null || level == null) return 0;

        double speed = getInitialVelocity(cannon, level);
        if (speed <= 0) return 0;

        int lifeTicks = getAutocannonLifetimeTicks(cannon);
        if (lifeTicks <= 0) return 0;

        if (isAutocannonFamily(cannon)) {
            BallisticPropertiesComponent bp = getAutocannonBallistics(cannon, level);

            if (bp.isQuadraticDrag()) {
                return speed * lifeTicks;
            }

            double drag = Math.max(0.0, Math.min(0.25, bp.drag()));
            double retained = Math.pow(1.0 - drag, lifeTicks);
            double avg = (1.0 + retained) * 0.5;
            return speed * lifeTicks * avg;
        }

        double drag = getProjectileDrag(cannon, level);
        drag = Math.max(0.0, Math.min(0.25, drag));

        double retained = Math.pow(1.0 - drag, lifeTicks);
        double avg = (1.0 + retained) * 0.5;

        return speed * lifeTicks * avg;
    }

    public static double getProjectileGravity(AbstractMountedCannonContraption cannon, ServerWorld level) {
        if (isAutocannonFamily(cannon)) {
            return getAutocannonBallistics(cannon, level).gravity();
        }
        Map<BlockPos, BlockEntity> presentBlockEntities = cannon.presentBlockEntities;
        for (BlockEntity blockEntity : presentBlockEntities.values()) {
            if (!(blockEntity instanceof IBigCannonBlockEntity cannonBlockEntity)) continue;
            BigCannonBehavior behavior = cannonBlockEntity.cannonBehavior();
            StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();

            Block block = containedBlockInfo.state().getBlock();
            if (block instanceof ProjectileBlock<?> projectileBlock) {
                AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, Collections.singletonList(containedBlockInfo));
                BallisticPropertiesComponent ballisticProperties = invokeBallisticProperties(projectile);
                if (ballisticProperties == null) return 0.05;
                return ballisticProperties.gravity();
            }
        }
        return 0.05;
    }

    public static double getProjectileDrag(AbstractMountedCannonContraption cannon, ServerWorld level) {
        Map<BlockPos, BlockEntity> presentBlockEntities = cannon.presentBlockEntities;
        double drag = 0.01;

        if (isAutocannonFamily(cannon)) {
            return getAutocannonBallistics(cannon, level).drag();
        }

        for (BlockEntity blockEntity : presentBlockEntities.values()) {
            if (!(blockEntity instanceof IBigCannonBlockEntity cannonBlockEntity)) continue;

            BigCannonBehavior behavior = cannonBlockEntity.cannonBehavior();
            StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();

            Block block = containedBlockInfo.state().getBlock();
            if (block instanceof ProjectileBlock<?> projectileBlock) {
                AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, Collections.singletonList(containedBlockInfo));
                BallisticPropertiesComponent bp = invokeBallisticProperties(projectile);
                if (bp != null) drag = bp.drag();
            }
        }
        return drag;
    }

    public static boolean isBigCannon(AbstractMountedCannonContraption cannon) {
        return cannon instanceof MountedBigCannonContraption;
    }

    public static boolean isAutoCannon(AbstractMountedCannonContraption cannon) {
        return cannon instanceof MountedAutocannonContraption;
    }

    public static boolean isLaserCannon(AbstractMountedCannonContraption cannonContraption) {
        return false; // Energy cannons not ported to Fabric
    }

    public static boolean isCannonReadyToFire(CannonMountBlockEntity mount) {
        return mount != null;
    }

    private static float getAutoCannonSpeed(AbstractMountedCannonContraption cannon) {
        AutocannonMaterial cann = ((AutoCannonAccessor) cannon).getMaterial();
        if (cann == null) return 0f;
        var props = cann.properties();

        float speed = props.baseSpeed();
        BlockPos pos = cannon.getStartPos().offset(cannon.initialOrientation());
        int count = 0;

        while (true) {
            BlockEntity be = cannon.presentBlockEntities.get(pos);
            if (be == null || !(be instanceof IAutocannonBlockEntity)) break;

            count++;
            if (count <= props.maxSpeedIncreases()) speed += props.speedIncreasePerBarrel();
            if (count > props.maxBarrelLength()) break;

            pos = pos.offset(cannon.initialOrientation());
        }

        return speed;
    }

    public static boolean isUp(World world, Vec3d mountPos) {
        BlockEntity blockEntity = world.getBlockEntity(new BlockPos((int) mountPos.x, (int) mountPos.y, (int) mountPos.z));
        return isUp(blockEntity);
    }

    public static boolean isUp(BlockEntity blockEntity) {
        if (!(blockEntity instanceof CannonMountBlockEntity cannonMountBlockEntity)) return true;
        if (cannonMountBlockEntity.getContraption() == null) return true;
        return !(cannonMountBlockEntity.getContraption().getPos().y < blockEntity.getPos().getY());
    }
}
