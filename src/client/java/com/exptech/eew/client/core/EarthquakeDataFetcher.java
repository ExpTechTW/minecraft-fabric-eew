package com.exptech.eew.client.core;

import com.exptech.eew.client.EewClient;
import com.exptech.eew.client.utils.TranslationManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EarthquakeDataFetcher {

    private static final Set<String> processedIds = new HashSet<>();
    private static final Gson gson = new Gson();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger LOGGER = EewClient.LOGGER;
    private static final AtomicInteger fetchCount = new AtomicInteger(0);

    public static void startFetching(String url) {
        LOGGER.info("Starting earthquake data fetching from URL: {}", url);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchData(url);
                int count = fetchCount.incrementAndGet();
                if (count % 60 == 0) {  // Log every 60 fetches (approximately every minute)
                    LOGGER.info("Fetch count: {}", count);
                }
            } catch (Exception e) {
                LOGGER.error("Error fetching data:", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static void fetchData(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            List<EarthquakeData> dataList = gson.fromJson(reader, new TypeToken<List<EarthquakeData>>() {
            }.getType());

            if (!dataList.isEmpty()) {
                for (EarthquakeData data : dataList) {
                    if (!processedIds.contains(data.id)) {
                        processedIds.add(data.id);
                        LOGGER.info("New earthquake data received. ID: {}", data.id);
                        displayMessage(TranslationManager.getTranslation("earthquake_alert"));
                    }
                }
            }

            reader.close();
        } else {
            LOGGER.warn("HTTP request failed. Response code: {}", responseCode);
        }
        connection.disconnect();
    }

    private static void displayMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.execute(() -> {
                Text text = Text.literal(message).formatted(Formatting.YELLOW);
                client.player.sendMessage(text, false);
                LOGGER.info("Displayed message to player: {}", message);
            });
        }
    }

    private static class EarthquakeData {
        String id;
        String body;
    }

    public static void stopFetching() {
        LOGGER.info("Stopping earthquake data fetching. Total fetches: {}", fetchCount.get());
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}