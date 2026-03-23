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
import java.util.Map;
import java.util.Set;

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

    public Group getOrCreateGroup(RegistryKey<World> dim, BlockPos filtererPos) {
        String key = filtererKey(new FilterKey(dim, filtererPos));
        return groupsByFilterer.computeIfAbsent(key, k -> new Group(new FilterKey(dim, filtererPos)));
    }

    public void setRadarPos(RegistryKey<World> dim, BlockPos filtererPos, @Nullable BlockPos radarPos) {
        Group g = getOrCreateGroup(dim, filtererPos);
        g.radarPos = radarPos;
        if (radarPos != null) {
            endpointToFilterer.put(posKey(dim, radarPos), filtererKey(new FilterKey(dim, filtererPos)));
        }
        markDirty();
    }

    public void addMonitorEndpoint(RegistryKey<World> dim, BlockPos filtererPos, BlockPos monitorPos) {
        Group g = getOrCreateGroup(dim, filtererPos);
        g.monitorEndpoints.add(monitorPos);
        endpointToFilterer.put(posKey(dim, monitorPos), filtererKey(new FilterKey(dim, filtererPos)));
        markDirty();
    }

    public void removeMonitorEndpoint(RegistryKey<World> dim, BlockPos filtererPos, BlockPos monitorPos) {
        Group g = getGroup(dim, filtererPos);
        if (g != null) g.monitorEndpoints.remove(monitorPos);
        endpointToFilterer.remove(posKey(dim, monitorPos));
        markDirty();
    }

    public void addWeaponEndpoint(RegistryKey<World> dim, BlockPos filtererPos, BlockPos weaponPos) {
        Group g = getOrCreateGroup(dim, filtererPos);
        g.weaponEndpoints.add(weaponPos);
        weaponMountToFilterer.put(posKey(dim, weaponPos), filtererKey(new FilterKey(dim, filtererPos)));
        markDirty();
    }

    public void removeWeaponEndpoint(RegistryKey<World> dim, BlockPos filtererPos, BlockPos weaponPos) {
        Group g = getGroup(dim, filtererPos);
        if (g != null) g.weaponEndpoints.remove(weaponPos);
        weaponMountToFilterer.remove(posKey(dim, weaponPos));
        markDirty();
    }

    @Nullable
    public BlockPos getFiltererForWeapon(RegistryKey<World> dim, BlockPos weaponPos) {
        String key = weaponMountToFilterer.get(posKey(dim, weaponPos));
        return key == null ? null : parseBlockPos(key);
    }

    public void dissolveNetworkForBrokenController(ServerWorld level, BlockPos filtererPos) {
        RegistryKey<World> dim = level.getRegistryKey();
        String key = filtererKey(new FilterKey(dim, filtererPos));
        Group g = groupsByFilterer.remove(key);
        if (g == null) return;
        for (BlockPos ep : g.monitorEndpoints) {
            endpointToFilterer.remove(posKey(dim, ep));
            notifyNodeDisconnected(level, ep);
        }
        if (g.radarPos != null) {
            endpointToFilterer.remove(posKey(dim, g.radarPos));
        }
        markDirty();
    }

    public void setDetectionFilter(RegistryKey<World> dim, BlockPos filtererPos, NbtCompound detectionTag) {
        Group g = getOrCreateGroup(dim, filtererPos);
        g.detectionTag = detectionTag;
        markDirty();
    }

    public String getSelectedTargetId(Group group) {
        return group.selectedTargetId;
    }

    public void setSelectedTargetId(Group group, @Nullable String id) {
        group.selectedTargetId = id;
        markDirty();
    }

    public Set<BlockPos> getWeaponEndpoints(Group group) {
        return group.weaponEndpoints;
    }

    public static NetworkData load(NbtCompound tag) {
        NetworkData data = new NetworkData();
        if (tag.contains("groups", NbtElement.LIST_TYPE)) {
            NbtList groups = tag.getList("groups", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < groups.size(); i++) {
                NbtCompound g = groups.getCompound(i);
                String keyStr = g.getString("key");
                try {
                    String[] parts = keyStr.split("@");
                    String dimStr = parts[0];
                    String[] coords = parts[1].split(",");
                    BlockPos filtererPos = new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
                    RegistryKey<World> dim = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimStr));
                    FilterKey fk = new FilterKey(dim, filtererPos);
                    Group group = new Group(fk);
                    if (g.contains("radarPos", NbtElement.COMPOUND_TYPE))
                        group.radarPos = NbtHelper.toBlockPos(g.getCompound("radarPos"));
                    if (g.contains("selectedTargetId", NbtElement.STRING_TYPE))
                        group.selectedTargetId = g.getString("selectedTargetId");
                    if (g.contains("detectionTag", NbtElement.COMPOUND_TYPE))
                        group.detectionTag = g.getCompound("detectionTag");
                    if (g.contains("monitorEndpoints", NbtElement.LIST_TYPE)) {
                        NbtList eps = g.getList("monitorEndpoints", NbtElement.COMPOUND_TYPE);
                        for (int j = 0; j < eps.size(); j++) {
                            BlockPos ep = NbtHelper.toBlockPos(eps.getCompound(j));
                            group.monitorEndpoints.add(ep);
                            data.endpointToFilterer.put(posKey(dim, ep), keyStr);
                        }
                    }
                    if (g.contains("weaponEndpoints", NbtElement.LIST_TYPE)) {
                        NbtList weps = g.getList("weaponEndpoints", NbtElement.COMPOUND_TYPE);
                        for (int j = 0; j < weps.size(); j++) {
                            BlockPos wp = NbtHelper.toBlockPos(weps.getCompound(j));
                            group.weaponEndpoints.add(wp);
                            data.weaponMountToFilterer.put(posKey(dim, wp), keyStr);
                        }
                    }
                    if (group.radarPos != null)
                        data.endpointToFilterer.put(posKey(dim, group.radarPos), keyStr);
                    data.groupsByFilterer.put(keyStr, group);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load network group: {}", keyStr, e);
                }
            }
        }
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        NbtList groups = new NbtList();
        for (Map.Entry<String, Group> entry : groupsByFilterer.entrySet()) {
            Group g = entry.getValue();
            NbtCompound gc = new NbtCompound();
            gc.putString("key", entry.getKey());
            if (g.radarPos != null) gc.put("radarPos", NbtHelper.fromBlockPos(g.radarPos));
            if (g.selectedTargetId != null) gc.putString("selectedTargetId", g.selectedTargetId);
            gc.put("detectionTag", g.detectionTag);
            NbtList eps = new NbtList();
            for (BlockPos ep : g.monitorEndpoints) eps.add(NbtHelper.fromBlockPos(ep));
            gc.put("monitorEndpoints", eps);
            NbtList weps = new NbtList();
            for (BlockPos wp : g.weaponEndpoints) weps.add(NbtHelper.fromBlockPos(wp));
            gc.put("weaponEndpoints", weps);
            groups.add(gc);
        }
        tag.put("groups", groups);
        return tag;
    }
}
