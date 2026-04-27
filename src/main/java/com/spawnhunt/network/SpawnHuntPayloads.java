package com.spawnhunt.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class SpawnHuntPayloads {

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(HuntSyncS2CPayload.ID, HuntSyncS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HuntWinS2CPayload.ID, HuntWinS2CPayload.CODEC);
    }
}
