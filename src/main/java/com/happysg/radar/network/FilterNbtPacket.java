package com.happysg.radar.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class FilterNbtPacket {

    public static final Identifier CHANNEL = new Identifier("create_radar", "filter_nbt");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, handler, buf, responseSender) -> {
            Hand hand = buf.readEnumConstant(Hand.class);
            NbtCompound nbt = buf.readNbt();
            server.execute(() -> {
                if (nbt != null && !player.getStackInHand(hand).isEmpty()) {
                    player.getStackInHand(hand).setNbt(nbt);
                }
            });
        });
    }

    @Environment(EnvType.CLIENT)
    public static void send(Hand hand, NbtCompound nbt) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(hand);
        buf.writeNbt(nbt);
        ClientPlayNetworking.send(CHANNEL, buf);
    }
}
