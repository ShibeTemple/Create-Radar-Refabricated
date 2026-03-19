package com.happysg.radar.block.radar.track;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RadarTrackUtil {

    public static NbtCompound serializeNBTList(Collection<RadarTrack> tracks) {
        NbtList list = new NbtList();
        for (RadarTrack track : tracks) {
            list.add(track.serializeNBT());
        }
        NbtCompound tag = new NbtCompound();
        tag.put("tracks", list);
        return tag;
    }

    public static List<RadarTrack> deserializeListNBT(NbtCompound tag) {
        List<RadarTrack> tracks = new ArrayList<>();
        NbtList list = tag.getList("tracks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            tracks.add(RadarTrack.deserializeNBT(list.getCompound(i)));
        }
        return tracks;
    }
}
