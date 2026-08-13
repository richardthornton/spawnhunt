package com.spawnhunt.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record HuntSyncS2CPayload(
        boolean active,
        Optional<Identifier> targetItem,
        long elapsedMs,
        boolean won,
        String winnerName,
        long finalTimeMs
) implements CustomPacketPayload {

    /** Vanilla caps player names at 16; 64 leaves room without inviting abuse. */
    static final int MAX_NAME_LENGTH = 64;

    public static final Type<HuntSyncS2CPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath("spawnhunt", "hunt_sync"));

    static final StreamCodec<ByteBuf, Optional<Identifier>> OPTIONAL_ITEM_ID =
            ByteBufCodecs.optional(Identifier.STREAM_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, HuntSyncS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, HuntSyncS2CPayload::active,
                    OPTIONAL_ITEM_ID, HuntSyncS2CPayload::targetItem,
                    ByteBufCodecs.VAR_LONG, HuntSyncS2CPayload::elapsedMs,
                    ByteBufCodecs.BOOL, HuntSyncS2CPayload::won,
                    ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), HuntSyncS2CPayload::winnerName,
                    ByteBufCodecs.VAR_LONG, HuntSyncS2CPayload::finalTimeMs,
                    HuntSyncS2CPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
