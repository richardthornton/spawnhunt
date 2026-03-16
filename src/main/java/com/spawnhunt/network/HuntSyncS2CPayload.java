package com.spawnhunt.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HuntSyncS2CPayload(
        boolean active,
        String targetItemId,
        long elapsedMs,
        boolean won,
        String winnerName,
        long finalTimeMs
) implements CustomPayload {

    public static final Id<HuntSyncS2CPayload> ID =
            new Id<>(Identifier.of("spawnhunt", "hunt_sync"));

    public static final PacketCodec<RegistryByteBuf, HuntSyncS2CPayload> CODEC = new PacketCodec<>() {
        @Override
        public HuntSyncS2CPayload decode(RegistryByteBuf buf) {
            return new HuntSyncS2CPayload(
                    buf.readBoolean(),
                    buf.readString(256),
                    buf.readLong(),
                    buf.readBoolean(),
                    buf.readString(64),
                    buf.readLong()
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, HuntSyncS2CPayload payload) {
            buf.writeBoolean(payload.active);
            buf.writeString(payload.targetItemId);
            buf.writeLong(payload.elapsedMs);
            buf.writeBoolean(payload.won);
            buf.writeString(payload.winnerName);
            buf.writeLong(payload.finalTimeMs);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
