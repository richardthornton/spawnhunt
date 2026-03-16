package com.spawnhunt.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HuntWinS2CPayload(
        String winnerName,
        long finalTimeMs,
        String targetItemId
) implements CustomPayload {

    public static final Id<HuntWinS2CPayload> ID =
            new Id<>(Identifier.of("spawnhunt", "hunt_win"));

    public static final PacketCodec<RegistryByteBuf, HuntWinS2CPayload> CODEC = new PacketCodec<>() {
        @Override
        public HuntWinS2CPayload decode(RegistryByteBuf buf) {
            return new HuntWinS2CPayload(
                    buf.readString(64),
                    buf.readLong(),
                    buf.readString(256)
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, HuntWinS2CPayload payload) {
            buf.writeString(payload.winnerName);
            buf.writeLong(payload.finalTimeMs);
            buf.writeString(payload.targetItemId);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
