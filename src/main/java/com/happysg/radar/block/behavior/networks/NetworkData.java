package com.happysg.radar.block.behavior.networks;

import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class NetworkData extends PersistentState {

    public enum RadarKind { BEARING, STATIONARY }
    public enum MountKind { NORMAL, FIXED, COMPACT }

    private final Map<String, String> radarToFilterer = new HashMap<>();

    public record FilterKey(RegistryKey<World> dim, BlockPos filtererPos) {}

    public static class Group {
        public final FilterKey key;
        public @Nullable String selectedTargetId;
        public final Set<BlockPos> monitorEndpoints = new HashSet<>();
        public @Nullable BlockPos radarPos;
        public @Nullable RadarKind radarKind;
        public final Set<BlockPos> weaponEndpoints = new HashSet<>();
        public final Set<BlockPos> usedWeaponMounts = new HashSet<>();
        public final Set<BlockPos> dataLinks = new HashSet<>();
        public NbtCompound targetingTag = defaultTargetingTag();
        public NbtCompound identificationTag = defaultIdentificationTag();
        public NbtCompound detectionTag = defaultDetectionTag();

        public Group(FilterKey key) {
            this.key = key;
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, Group> groupsByFilterer = new HashMap<>();
    private final Map<String, String> endpointToFilterer = new HashMap<>();
    private final Map<String, String> weaponMountToFilterer = new HashMap<>();
    private final Map<String, String> dataLinkToFilterer = new HashMap<>();
    private final Map<String, String> dataLinkToEndpoint = new HashMap<>();
    private final Map<String, String> controllerToWeaponMount = new HashMap<>();

    public static NetworkData get(ServerWorld level) {
        return level.getPersistentStateManager().getOrCreate(
                nbt -> NetworkData.load(nbt),
                NetworkData::new,
                "network_data"
        );
    }

    public NetworkData() {}

    public void onEndpointRemoved(ServerWorld level, BlockPos pos) {
        RegistryKey<World> dim = level.getRegistryKey();
        String key = posKey(dim, pos);

        String filtererKey = endpointToFilterer.remove(key);
        if (filtererKey != null) {
            Group g = groupsByFilterer.get(filtererKey);
            if (g != null) {
                g.monitorEndpoints.remove(pos);
                g.weaponEndpoints.remove(pos);
                if (Objects.equals(g.radarPos, pos)) g.radarPos = null;
            }
        }
        markDirty();
    }

    @Nullable
    public BlockPos getFiltererForEndpoint(RegistryKey<World> dim, BlockPos pos) {
        String key = endpointToFilterer.get(posKey(dim, pos));
        if (key == null) return null;
        return parseBlockPos(key);
    }

    @Nullable
    public Group getGroup(RegistryKey<World> dim, BlockPos filtererPos) {
        return groupsByFilterer.get(filtererKey(new FilterKey(dim, filtererPos)));
    }

    public boolean updateRadarPosition(RegistryKey<World> dim, BlockPos oldPos, BlockPos newPos) {
        String oldKey = posKey(dim, oldPos);
        String newKey = posKey(dim, newPos);
        String filtererKey = endpointToFilterer.get(oldKey);
        if (filtererKey == null) return false;
        endpointToFilterer.remove(oldKey);
        endpointToFilterer.put(newKey, filtererKey);
        Group g = groupsByFilterer.get(filtererKey);
        if (g != null && Objects.equals(g.radarPos, oldPos)) {
            g.radarPos = newPos;
        }
        markDirty();
        return true;
    }

    public boolean updateMonitorPosition(RegistryKey<World> dim, BlockPos oldPos, BlockPos newPos) {
        String oldKey = posKey(dim, oldPos);
        String newKey = posKey(dim, newPos);
        String filtererKey = endpointToFilterer.get(oldKey);
        if (filtererKey == null) return false;
        endpointToFilterer.remove(oldKey);
        endpointToFilterer.put(newKey, filtererKey);
        Group g = groupsByFilterer.get(filtererKey);
        if (g != null) {
            g.monitorEndpoints.remove(oldPos);
            g.monitorEndpoints.add(newPos);
        }
        markDirty();
        return true;
    }

    public void retargetEndpoint(RegistryKey<World> dim, BlockPos oldPos, BlockPos newPos) {
        String oldKey = posKey(dim, oldPos);
        String newKey = posKey(dim, newPos);
        String filtererKey = endpointToFilterer.remove(oldKey);
        if (filtererKey != null) {
            endpointToFilterer.put(newKey, filtererKey);
            Group g = groupsByFilterer.get(filtererKey);
            if (g != null) {
                g.monitorEndpoints.remove(oldPos);
                g.monitorEndpoints.add(newPos);
            }
        }
        markDirty();
    }

    private void notifyNodeDisconnected(ServerWorld level, @Nullable BlockPos pos) {
        if (pos == null) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof INetworkNode node) {
            node.onNetworkDisconnected();
        }
    }

    private static String posKey(RegistryKey<World> dim, BlockPos pos) {
        return dim.getValue() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String filtererKey(FilterKey key) {
        return posKey(key.dim(), key.filtererPos());
    }

    @Nullable
    private static BlockPos parseBlockPos(String key) {
        try {
            String[] parts = key.split("@")[1].split(",");
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }

    private static NbtCompound defaultTargetingTag() {
        return new NbtCompound();
    }

    private static NbtCompound defaultIdentificationTag() {
        return new NbtCompound();
    }

    private static NbtCompound defaultDetectionTag() {
        return DetectionConfig.DEFAULT.toTag();
    }

    public static NetworkData load(NbtCompound tag) {
        NetworkData data = new NetworkData();
        // Simplified: data loading can be expanded
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        // Simplified: persistence can be expanded
        return tag;
    }
}
