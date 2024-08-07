package com.exptech.eew.client.core;

import com.exptech.eew.client.EewClient;
import com.exptech.eew.client.utils.IntensityConversion;
import com.exptech.eew.client.utils.TranslationManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EarthquakeDataFetcher {

    private static final Set<String> processedIds = new HashSet<>();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
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
            InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            List<EarthquakeData> dataList = gson.fromJson(reader, new TypeToken<List<EarthquakeData>>() {
            }.getType());

            if (!dataList.isEmpty()) {
                for (EarthquakeData data : dataList) {
                    String id = String.format("%s-%s", data.id, data.serial);
                    if (!processedIds.contains(id)) {
                        processedIds.add(id);
                        LOGGER.info("New earthquake data received. ID: {}", data.id);
                        displayMessage(data);
                    }
                }
            }

            reader.close();
        } else {
            LOGGER.warn("HTTP request failed. Response code: {}", responseCode);
        }
        connection.disconnect();
    }

    private static void displayMessage(EarthquakeData data) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.execute(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String date = sdf.format(new Date(data.eq.time));

                String status = (data.status == 0) ? TranslationManager.getTranslation("warn") : TranslationManager.getTranslation("alert");
                String contextTemplate = TranslationManager.getTranslation("eew")
                        .replace("%author%", TranslationManager.getTranslation(data.author))
                        .replace("%status%", status)
                        .replace("%time%", date)
                        .replace("%serial%", String.valueOf(data.serial))
                        .replace("%mag%", String.valueOf(data.eq.mag))
                        .replace("%depth%", String.valueOf(data.eq.depth))
                        .replace("%loc%", data.eq.loc)
                        .replace("%max%", IntensityConversion.intensityToNumberString(data.eq.max));

                MutableText colorfulText = Text.literal("")
                        .append(Text.literal("[Minecraft EEW Mod] ").formatted(Formatting.BLUE))
                        .append(Text.literal(contextTemplate).formatted((data.status == 1) ? Formatting.RED : Formatting.GOLD));

                assert client.player != null;
                client.player.sendMessage(colorfulText, false);
                LOGGER.info("Displayed message to player: {}", colorfulText.getString());
            });
        }
    }

    public static class EarthquakeData {
        private String author;
        private String id;
        private int serial;
        private int status;
        private int finalValue;
        private Earthquake eq;
        private long time;

        public static class Earthquake {
            private long time;
            private double lon;
            private double lat;
            private int depth;
            private double mag;
            private String loc;
            private int max;
        }
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