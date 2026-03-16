package com.spawnhunt.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class SpawnHuntPayloads {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(HuntSyncS2CPayload.ID, HuntSyncS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HuntWinS2CPayload.ID, HuntWinS2CPayload.CODEC);
    }
}
