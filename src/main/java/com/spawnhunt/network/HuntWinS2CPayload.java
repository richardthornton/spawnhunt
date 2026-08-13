package com.spawnhunt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HuntWinS2CPayload(
        String winnerName,
        long finalTimeMs,
        Identifier targetItem
) implements CustomPacketPayload {

    public static final Type<HuntWinS2CPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath("spawnhunt", "hunt_win"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HuntWinS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(HuntSyncS2CPayload.MAX_NAME_LENGTH), HuntWinS2CPayload::winnerName,
                    ByteBufCodecs.VAR_LONG, HuntWinS2CPayload::finalTimeMs,
                    Identifier.STREAM_CODEC, HuntWinS2CPayload::targetItem,
                    HuntWinS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
