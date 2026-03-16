package com.spawnhunt;

import com.spawnhunt.command.SpawnHuntCommand;
import com.spawnhunt.event.ServerHuntManager;
import com.spawnhunt.network.SpawnHuntPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnHuntCommon implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("spawnhunt");

    @Override
    public void onInitialize() {
        LOGGER.info("SpawnHunt common initializing...");

        // Register custom packet types (must happen before networking is used)
        SpawnHuntPayloads.register();

        // Register /spawnhunt commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SpawnHuntCommand.register(dispatcher);
        });

        // Register server tick handler for multiplayer hunt management
        ServerHuntManager.register();

        LOGGER.info("SpawnHunt common initialized!");
    }
}
