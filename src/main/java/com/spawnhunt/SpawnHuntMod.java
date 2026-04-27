package com.spawnhunt;

import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.HuntState;
import com.spawnhunt.event.InventoryListener;
import com.spawnhunt.event.WorldLifecycleHandler;
import com.spawnhunt.hud.HuntHudRenderer;
import com.spawnhunt.network.ClientHuntState;
import com.spawnhunt.network.HuntSyncS2CPayload;
import com.spawnhunt.network.HuntWinS2CPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnHuntMod implements ClientModInitializer {
    public static final String MOD_ID = "spawnhunt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("SpawnHunt initializing...");

        HuntState.reset();
        ItemPool.logPool();

        // Start the hunt timer when the player joins the world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (HuntState.isActive() && !HuntState.isWon()) {
                HuntState.beginTimer();
                LOGGER.info("SpawnHunt: timer started! Target: {}", HuntState.getTargetItem());
            }
        });

        // Tick the timer and check inventory each client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (HuntState.isActive() && !HuntState.isWon() && client.player != null) {
                boolean paused = client.isPaused() || client.screen instanceof DeathScreen;
                HuntState.tick(paused);
                InventoryListener.tick(client);
            }
        });

        // Reset hunt state when disconnecting from a world
        WorldLifecycleHandler.register();

        // Render the HUD overlay (target block + timer)
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("spawnhunt", "hud"), HuntHudRenderer::extractRenderState);

        // Register multiplayer packet receivers
        ClientPlayNetworking.registerGlobalReceiver(HuntSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> ClientHuntState.update(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(HuntWinS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientHuntState.handleWin(payload);
                if (context.client().player != null) {
                    context.client().player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            });
        });

        LOGGER.info("SpawnHunt initialized!");
    }
}
