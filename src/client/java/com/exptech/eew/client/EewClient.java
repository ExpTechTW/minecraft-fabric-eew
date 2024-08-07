package com.exptech.eew.client;

import com.exptech.eew.client.core.EarthquakeDataFetcher;
import com.exptech.eew.client.utils.TranslationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EewClient implements ClientModInitializer {

    public static final String MOD_ID = "eew";
    public static String VERSION = "0.0.0";
    private boolean hasJoinedWorld = false;
    private static final String EARTHQUAKE_DATA_URL = "https://api-1.exptech.dev/api/v1/eq/eew";
    public static final Logger LOGGER = LoggerFactory.getLogger("EewClient");

    @Override
    public void onInitializeClient() {
        try {
            FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> VERSION = modContainer.getMetadata().getVersion().getFriendlyString());

            TranslationManager.initialize();

            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                hasJoinedWorld = false;
                EarthquakeDataFetcher.startFetching(EARTHQUAKE_DATA_URL);
                LOGGER.info("Started fetching earthquake data");
            });

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.world != null && !hasJoinedWorld) {
                    displayWelcomeMessage(client);
                    hasJoinedWorld = true;
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(this::onGameClosing));
        } catch (Exception e) {
            LOGGER.error("Error initializing EewClient", e);
        }
    }

    private void onGameClosing() {
        LOGGER.info("Game is closing, stopping earthquake data fetching...");
        EarthquakeDataFetcher.stopFetching();
    }

    private void displayWelcomeMessage(MinecraftClient client) {
        MutableText colorfulText = Text.literal("")
                .append(Text.literal("[Minecraft EEW Mod] ").formatted(Formatting.BLUE))
                .append(Text.literal(VERSION).formatted(Formatting.GREEN));

        assert client.player != null;
        client.player.sendMessage(colorfulText, false);
    }
}