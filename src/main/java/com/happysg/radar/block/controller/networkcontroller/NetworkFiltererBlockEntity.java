package com.happysg.radar.block.controller.networkcontroller;

import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.registry.ModBlockEntityTypes;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NetworkFiltererBlockEntity extends SmartBlockEntity {

    // 3 filter slots: 0=detection, 1=identification, 2=targeting
    private final ItemStack[] inventory = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    @Nullable private BlockPos linkedRadarPos;
    private final Set<BlockPos> linkedMonitorEndpoints = new HashSet<>();

    public NetworkFiltererBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public static void tick(World world, BlockPos pos, BlockState state, NetworkFiltererBlockEntity be) {
        if (!(world instanceof ServerWorld sl)) return;
        if (world.getTime() % 20 != 0) return;
        be.applyFiltersToNetwork(sl);
    }

    public void applyFiltersToNetwork(ServerWorld sl) {
        RegistryKey<World> dim = sl.getRegistryKey();
        NetworkData data = NetworkData.get(sl);
        NetworkData.Group group = data.getOrCreateGroup(dim, pos);

        // Apply detection filter from slot 0
        ItemStack detStack = inventory[0];
        if (!detStack.isEmpty() && detStack.hasNbt()) {
            NbtCompound tag = detStack.getNbt();
            NbtCompound det = null;
            if (tag.contains("Filters", NbtElement.COMPOUND_TYPE)) {
                NbtCompound filters = tag.getCompound("Filters");
                if (filters.contains("detection", NbtElement.COMPOUND_TYPE))
                    det = filters.getCompound("detection");
            } else if (tag.contains("detection", NbtElement.COMPOUND_TYPE)) {
                det = tag.getCompound("detection");
            }
            if (det != null) {
                data.setDetectionFilter(dim, pos, det);
            }
        }

        // Sync radar and monitor links
        if (!Objects.equals(group.radarPos, linkedRadarPos)) {
            if (group.radarPos != null) data.onEndpointRemoved(sl, group.radarPos);
            group.radarPos = linkedRadarPos;
            if (linkedRadarPos != null)
                data.setRadarPos(dim, pos, linkedRadarPos);
        }
    }

    public void linkRadar(ServerWorld sl, BlockPos radarPos) {
        RegistryKey<World> dim = sl.getRegistryKey();
        NetworkData data = NetworkData.get(sl);

        if (linkedRadarPos != null) data.onEndpointRemoved(sl, linkedRadarPos);

        linkedRadarPos = radarPos;
        data.setRadarPos(dim, pos, radarPos);
        markDirty();
        sendData();
    }

    public void linkMonitor(ServerWorld sl, BlockPos monitorPos) {
        RegistryKey<World> dim = sl.getRegistryKey();
        NetworkData data = NetworkData.get(sl);
        linkedMonitorEndpoints.add(monitorPos);
        data.addMonitorEndpoint(dim, pos, monitorPos);
        markDirty();
        sendData();
    }

    public void unlinkMonitor(ServerWorld sl, BlockPos monitorPos) {
        RegistryKey<World> dim = sl.getRegistryKey();
        NetworkData data = NetworkData.get(sl);
        linkedMonitorEndpoints.remove(monitorPos);
        data.removeMonitorEndpoint(dim, pos, monitorPos);
        markDirty();
        sendData();
    }

    public void dissolveNetwork(ServerWorld sl) {
        NetworkData.get(sl).dissolveNetworkForBrokenController(sl, pos);
        linkedRadarPos = null;
        linkedMonitorEndpoints.clear();
    }

    @Nullable
    public BlockPos getLinkedRadarPos() { return linkedRadarPos; }

    public int getLinkedMonitorCount() { return linkedMonitorEndpoints.size(); }

    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= inventory.length) return ItemStack.EMPTY;
        return inventory[slot] == null ? ItemStack.EMPTY : inventory[slot];
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.length) return;
        inventory[slot] = stack == null ? ItemStack.EMPTY : stack;
        markDirty();
        sendData();
        if (world instanceof ServerWorld sl) applyFiltersToNetwork(sl);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        for (int i = 0; i < 3; i++) {
            String key = "Slot" + i;
            if (tag.contains(key, NbtElement.COMPOUND_TYPE))
                inventory[i] = ItemStack.fromNbt(tag.getCompound(key));
            else
                inventory[i] = ItemStack.EMPTY;
        }
        if (tag.contains("LinkedRadar", NbtElement.COMPOUND_TYPE))
            linkedRadarPos = NbtHelper.toBlockPos(tag.getCompound("LinkedRadar"));
        else
            linkedRadarPos = null;
        linkedMonitorEndpoints.clear();
        if (tag.contains("LinkedMonitors", NbtElement.LIST_TYPE)) {
            NbtList list = tag.getList("LinkedMonitors", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++)
                linkedMonitorEndpoints.add(NbtHelper.toBlockPos(list.getCompound(i)));
        }
    }

    @Override
    public void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        for (int i = 0; i < 3; i++) {
            ItemStack s = inventory[i];
            if (s != null && !s.isEmpty())
                tag.put("Slot" + i, s.writeNbt(new NbtCompound()));
        }
        if (linkedRadarPos != null)
            tag.put("LinkedRadar", NbtHelper.fromBlockPos(linkedRadarPos));
        NbtList list = new NbtList();
        for (BlockPos ep : linkedMonitorEndpoints)
            list.add(NbtHelper.fromBlockPos(ep));
        tag.put("LinkedMonitors", list);
    }
}
