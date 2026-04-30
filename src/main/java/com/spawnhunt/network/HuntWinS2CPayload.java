package com.spawnhunt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HuntWinS2CPayload(
        String winnerName,
        long finalTimeMs,
        String targetItemId
) implements CustomPacketPayload {

    public static final Type<HuntWinS2CPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath("spawnhunt", "hunt_win"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HuntWinS2CPayload> CODEC = new StreamCodec<>() {
        @Override
        public HuntWinS2CPayload decode(RegistryFriendlyByteBuf buf) {
            return new HuntWinS2CPayload(
                    buf.readUtf(64),
                    buf.readLong(),
                    buf.readUtf(256)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, HuntWinS2CPayload payload) {
            buf.writeUtf(payload.winnerName);
            buf.writeLong(payload.finalTimeMs);
            buf.writeUtf(payload.targetItemId);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
