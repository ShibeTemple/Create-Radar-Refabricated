package com.happysg.radar.block.monitor;

import com.happysg.radar.block.behavior.networks.INetworkNode;
import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.RadarTrackUtil;
import com.happysg.radar.compat.vs2.PhysicsHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MonitorBlockEntity extends SmartBlockEntity implements INetworkNode {

    protected BlockPos controller;
    protected int radius = 1;
    protected @Nullable BlockPos radarPos;
    protected @Nullable IRadar radar;
    protected String hoveredEntity;
    public String selectedEntity;
    public RadarTrack activetrack;
    boolean reset = false;
    protected Collection<RadarTrack> cachedTracks = new ArrayList<>();
    protected DetectionConfig filter = DetectionConfig.DEFAULT;
    private NbtCompound lastDetectionTag = null;
    private BlockPos lastKnownPos = BlockPos.ORIGIN;
    public final List<Box> safeZones = new ArrayList<>();

    // Dirty-flag tracking to avoid sending data every 5 ticks when nothing changed.
    private int lastSentTrackCount = -1;
    private @Nullable BlockPos lastSentRadarPos = null;
    private @Nullable String lastSentSelectedEntity = null;
    private long lastForcedSendTime = -20;

    // Cached network group reference — avoids posKey string allocation + parseBlockPos every 5 ticks.
    private @Nullable NetworkData.Group cachedNetworkGroup = null;

    public MonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateCacheServerOrClient();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (world == null) return;

        if (!world.isClient && world instanceof ServerWorld sl) {
            if (world.getTime() % 5 == 0) {
                syncFromNetwork(sl);
                updateCacheServerOrClient();
                MonitorBlockEntity controllerBe = getController();
                if (controllerBe != null) {
                    controllerBe.activetrack = controllerBe.resolveActiveTrackFromCache();
                }
                maybeSendData();
            }
        }

        if (!world.isClient && world.getTime() % 40 == 0) {
            if (world instanceof ServerWorld serverWorld) {
                if (lastKnownPos.equals(pos)) return;
                RegistryKey<World> dim = serverWorld.getRegistryKey();
                NetworkData data = NetworkData.get(serverWorld);
                boolean updated = data.updateMonitorPosition(dim, lastKnownPos, pos);
                if (updated) {
                    lastKnownPos = pos;
                    markDirty();
                }
            }
        }
    }

    public void onDataLinkRemoved() {
        this.activetrack = null;
        this.radarPos = null;
        this.radar = null;
        this.controller = null;
        this.cachedNetworkGroup = null;
        markDirty();
        sendData();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void onNetworkDisconnected() {
        onDataLinkRemoved();
    }

    private void syncFromNetwork(ServerWorld sl) {
        NetworkData.Group g = getNetworkGroup(sl);
        if (g == null) {
            if (radarPos != null) {
                radarPos = null;
                radar = null;
                sendData();
            }
            return;
        }

        BlockPos netRadar = g.radarPos;
        if (!Objects.equals(netRadar, radarPos)) {
            radarPos = netRadar;
            radar = null;
        }

        // Only re-parse DetectionConfig when the tag object is replaced (config changed).
        if (g.detectionTag != lastDetectionTag) {
            lastDetectionTag = g.detectionTag;
            filter = DetectionConfig.fromTag(g.detectionTag);
        }
        selectedEntity = g.selectedTargetId;
    }

    private void updateCacheServerOrClient() {
        if (world == null) return;

        if (world.isClient) {
            if (radarPos == null) {
                cachedTracks = List.of();
                activetrack = null;
                selectedEntity = null;
            }
            return;
        }

        Optional<IRadar> r = getRadar();
        if (r.isEmpty()) {
            cachedTracks = List.of();
            activetrack = null;
            selectedEntity = null;
            return;
        }

        IRadar radar = r.get();
        cachedTracks = radar.getTracks().stream().filter(filter::test).toList();
        activetrack = resolveActiveTrack();
    }

    public boolean isLinked() {
        return getRadarCenterPos() != null;
    }

    @Nullable
    private RadarTrack resolveActiveTrack() {
        if (selectedEntity == null) return null;
        for (RadarTrack track : cachedTracks) {
            if (selectedEntity.equals(track.getId()) || selectedEntity.equals(track.id()))
                return track;
        }
        return null;
    }

    /**
     * Sends data to clients only when track count, radarPos, or selectedEntity changed,
     * or at least every 20 ticks so track positions stay reasonably fresh.
     */
    private void maybeSendData() {
        if (world == null) return;
        long time = world.getTime();
        int trackCount = cachedTracks.size();
        boolean changed = trackCount != lastSentTrackCount
                || !Objects.equals(radarPos, lastSentRadarPos)
                || !Objects.equals(selectedEntity, lastSentSelectedEntity)
                || (time - lastForcedSendTime) >= 20;
        if (changed) {
            lastSentTrackCount = trackCount;
            lastSentRadarPos = radarPos;
            lastSentSelectedEntity = selectedEntity;
            lastForcedSendTime = time;
            sendData();
        }
    }

    /** Resets this block to a standalone single monitor with no network connection. */
    public void disconnectAndReset() {
        this.controller = null;
        this.radius = 1;
        this.radarPos = null;
        this.radar = null;
        this.activetrack = null;
        this.cachedNetworkGroup = null;
        markDirty();
        sendData();
    }

    public void setSelectedTargetServer(@Nullable String trackId) {
        if (world == null || world.isClient) return;
        if (!(world instanceof ServerWorld sl)) return;
        MonitorBlockEntity controllerBe = getController();
        if (controllerBe == null || !controllerBe.isLinked()) return;
        NetworkData.Group g = controllerBe.getNetworkGroup(sl);
        if (g == null) return;
        NetworkData.get(sl).setSelectedTargetId(g, trackId);
        controllerBe.selectedEntity = trackId;
        controllerBe.sendData();
    }

    @Nullable
    RadarTrack resolveActiveTrackFromCache() {
        if (selectedEntity == null) return null;
        for (RadarTrack t : cachedTracks) {
            if (t == null) continue;
            if (selectedEntity.equals(t.getId()) || selectedEntity.equals(t.id()))
                return t;
        }
        return null;
    }

    @Nullable
    private NetworkData.Group getNetworkGroup(ServerWorld sl) {
        BlockPos endpointPos = getControllerPos();
        // Fast path: skip string key allocation if cached group is still valid.
        if (cachedNetworkGroup != null && cachedNetworkGroup.monitorEndpoints.contains(endpointPos)) {
            return cachedNetworkGroup;
        }
        NetworkData data = NetworkData.get(sl);
        BlockPos filtererPos = data.getFiltererForEndpoint(sl.getRegistryKey(), endpointPos);
        if (filtererPos == null) { cachedNetworkGroup = null; return null; }
        NetworkData.Group g = data.getGroup(sl.getRegistryKey(), filtererPos);
        if (g == null || !g.monitorEndpoints.contains(endpointPos)) { cachedNetworkGroup = null; return null; }
        cachedNetworkGroup = g;
        return g;
    }

    public Optional<IRadar> getRadar() {
        if (world == null) return Optional.empty();
        if (!isLinked()) return Optional.empty();

        if (world.isClient) {
            if (radarPos == null) return Optional.empty();
            if (radar instanceof net.minecraft.block.entity.BlockEntity be && be.getPos().equals(radarPos))
                return Optional.of(radar);
            radar = null;
            if (world.getBlockEntity(radarPos) instanceof IRadar r) radar = r;
            return Optional.ofNullable(radar);
        }

        if (radarPos == null) {
            radar = null;
            return Optional.empty();
        }

        if (radar instanceof net.minecraft.block.entity.BlockEntity be && be.getPos().equals(radarPos))
            return Optional.of(radar);

        radar = null;
        if (world.getBlockEntity(radarPos) instanceof IRadar r) radar = r;
        return Optional.ofNullable(radar);
    }

    public BlockPos getControllerPos() {
        if (controller == null) return pos;
        return controller;
    }

    public int getSize() { return radius; }

    public void setControllerPos(BlockPos newController, int size) {
        if (world instanceof ServerWorld sl) {
            BlockPos oldController = this.controller == null ? pos : this.controller;
            NetworkData data = NetworkData.get(sl);
            data.retargetEndpoint(sl.getRegistryKey(), oldController, newController);
        }
        this.controller = newController;
        this.radius = size;
        markDirty();
        sendData();
    }

    public boolean isController() {
        return pos.equals(getControllerPos());
    }

    public MonitorBlockEntity getController() {
        if (isController()) return this;
        if (world != null && world.getBlockEntity(controller) instanceof MonitorBlockEntity controllerBe)
            return controllerBe;
        return this;
    }

    @Override
    protected Box createRenderBoundingBox() {
        return super.createRenderBoundingBox().expand(10);
    }

    public Collection<RadarTrack> getTracks() { return cachedTracks; }

    public float getRange() {
        return getRadar().map(IRadar::getRange).orElse(0f);
    }

    @Nullable
    public Vec3d getRadarCenterPos() {
        if (radarPos == null || world == null) return null;
        return PhysicsHandler.getWorldVec(world, radarPos);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("Controller", NbtElement.COMPOUND_TYPE))
            controller = NbtHelper.toBlockPos(tag.getCompound("Controller"));

        if (clientPacket && tag.contains("HasRadarPos", NbtElement.BYTE_TYPE) && !tag.getBoolean("HasRadarPos")) {
            radarPos = null;
            radar = null;
        } else if (tag.contains("radarPos", NbtElement.COMPOUND_TYPE)) {
            radarPos = NbtHelper.toBlockPos(tag.getCompound("radarPos"));
        }

        selectedEntity = tag.contains("SelectedEntity", NbtElement.STRING_TYPE) ? tag.getString("SelectedEntity") : null;
        hoveredEntity = tag.contains("HoveredEntity", NbtElement.STRING_TYPE) ? tag.getString("HoveredEntity") : null;

        if (tag.contains("Filter", NbtElement.COMPOUND_TYPE))
            filter = DetectionConfig.fromTag(tag.getCompound("Filter"));
        else
            filter = DetectionConfig.DEFAULT;

        radius = tag.contains("Size", NbtElement.INT_TYPE) ? tag.getInt("Size") : 1;

        if (clientPacket && tag.contains("tracks", NbtElement.COMPOUND_TYPE))
            cachedTracks = RadarTrackUtil.deserializeListNBT(tag.getCompound("tracks"));

        readSafeZones(tag);
    }

    private void readSafeZones(NbtCompound tag) {
        safeZones.clear();
        NbtList safeZonesTag = tag.getList("SafeZones", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < safeZonesTag.size(); i++) {
            NbtCompound safeZoneTag = safeZonesTag.getCompound(i);
            Box safeZone = new Box(
                    safeZoneTag.getDouble("minX"), safeZoneTag.getDouble("minY"), safeZoneTag.getDouble("minZ"),
                    safeZoneTag.getDouble("maxX"), safeZoneTag.getDouble("maxY"), safeZoneTag.getDouble("maxZ")
            );
            safeZones.add(safeZone);
        }
    }

    @Override
    public void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (controller != null)
            tag.put("Controller", NbtHelper.fromBlockPos(controller));

        if (selectedEntity != null) tag.putString("SelectedEntity", selectedEntity);
        if (hoveredEntity != null) tag.putString("HoveredEntity", hoveredEntity);
        tag.putInt("Size", radius);

        if (clientPacket) {
            tag.putBoolean("HasRadarPos", radarPos != null);
            if (radarPos != null) tag.put("radarPos", NbtHelper.fromBlockPos(radarPos));
            tag.put("Filter", filter.toTag());
            tag.put("tracks", RadarTrackUtil.serializeNBTList(cachedTracks));
        } else {
            if (world instanceof ServerWorld slevel) {
                if (getNetworkGroup(slevel) == null) {
                    if (radarPos != null) tag.put("radarPos", NbtHelper.fromBlockPos(radarPos));
                    tag.put("Filter", filter.toTag());
                }
            }
        }

        tag.put("SafeZones", saveSafeZones());
    }

    private @NotNull NbtList saveSafeZones() {
        NbtList safeZonesTag = new NbtList();
        for (Box safeZone : safeZones) {
            NbtCompound safeZoneTag = new NbtCompound();
            safeZoneTag.putDouble("minX", safeZone.minX);
            safeZoneTag.putDouble("minY", safeZone.minY);
            safeZoneTag.putDouble("minZ", safeZone.minZ);
            safeZoneTag.putDouble("maxX", safeZone.maxX);
            safeZoneTag.putDouble("maxY", safeZone.maxY);
            safeZoneTag.putDouble("maxZ", safeZone.maxZ);
            safeZonesTag.add(safeZoneTag);
        }
        return safeZonesTag;
    }

    public String getHoveredEntity() { return hoveredEntity; }
    public String getSelectedEntity() { return selectedEntity; }

    public void showSafeZone() {
        // Client-side: triggers safe zone rendering (handled by the monitor renderer)
    }

    public void addSafeZone(BlockPos start, BlockPos end) {
        Box box = new Box(start, end).expand(1, 1, 1);
        safeZones.add(box);
        markDirty();
        sendData();
    }

    public boolean tryRemoveAABB(BlockPos pos) {
        Vec3d center = pos.toCenterPos();
        for (int i = safeZones.size() - 1; i >= 0; i--) {
            if (safeZones.get(i).contains(center)) {
                safeZones.remove(i);
                markDirty();
                sendData();
                return true;
            }
        }
        return false;
    }
}
