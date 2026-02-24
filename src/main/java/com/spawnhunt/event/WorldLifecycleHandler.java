package com.spawnhunt.event;

import com.spawnhunt.SpawnHuntMod;
import com.spawnhunt.data.HuntState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class WorldLifecycleHandler {

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (HuntState.isActive()) {
                SpawnHuntMod.LOGGER.info("SpawnHunt: hunt ended (disconnected from world)");
                HuntState.reset();
            }
        });
    }
}
