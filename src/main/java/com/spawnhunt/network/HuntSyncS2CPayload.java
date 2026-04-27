package com.spawnhunt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HuntSyncS2CPayload(
        boolean active,
        String targetItemId,
        long elapsedMs,
        boolean won,
        String winnerName,
        long finalTimeMs
) implements CustomPacketPayload {

    public static final Type<HuntSyncS2CPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath("spawnhunt", "hunt_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HuntSyncS2CPayload> CODEC = new StreamCodec<>() {
        @Override
        public HuntSyncS2CPayload decode(RegistryFriendlyByteBuf buf) {
            return new HuntSyncS2CPayload(
                    buf.readBoolean(),
                    buf.readUtf(256),
                    buf.readLong(),
                    buf.readBoolean(),
                    buf.readUtf(64),
                    buf.readLong()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, HuntSyncS2CPayload payload) {
            buf.writeBoolean(payload.active);
            buf.writeUtf(payload.targetItemId);
            buf.writeLong(payload.elapsedMs);
            buf.writeBoolean(payload.won);
            buf.writeUtf(payload.winnerName);
            buf.writeLong(payload.finalTimeMs);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
