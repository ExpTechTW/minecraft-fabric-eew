package com.exptech.eew.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EewClient implements ClientModInitializer {

    private boolean hasJoinedWorld = false;
    private static final Map<String, String> translations = new HashMap<>();
    private static final Map<String, String> fallbackTranslations = new HashMap<>();

    @Override
    public void onInitializeClient() {
        try {
            loadTranslationFile("/lang/zh_tw.json", fallbackTranslations);

            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                hasJoinedWorld = false;
                loadUserLanguageTranslations();
            });

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.world != null && !hasJoinedWorld) {
                    displayWelcomeMessage(client);
                    hasJoinedWorld = true;
                }
            });
        } catch (Exception e) {
            System.err.println("EewClient 初始化時發生錯誤：");
            e.printStackTrace();
        }
    }

    private void loadUserLanguageTranslations() {
        String userLang = getUserLanguage();
        loadTranslationFile("/lang/" + userLang + ".json", translations);
    }

    private void loadTranslationFile(String filePath, Map<String, String> targetMap) {
        try {
            InputStream inputStream = EewClient.class.getResourceAsStream(filePath);
            if (inputStream != null) {
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> loadedTranslations = new Gson().fromJson(reader, type);
                targetMap.putAll(loadedTranslations);
                reader.close();
            } else {
                System.err.println("翻譯文件未找到: " + filePath);
            }
        } catch (Exception e) {
            System.err.println("加載翻譯文件時發生錯誤: " + filePath);
            e.printStackTrace();
        }
    }

    private String getUserLanguage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getLanguageManager() != null) {
            return client.getLanguageManager().getLanguage();
        }
        System.err.println("無法獲取用戶語言設置，使用默認值 'zh_tw'");
        return "zh_tw";
    }

    private void displayWelcomeMessage(MinecraftClient client) {
        String translatedMessage = getTranslation("welcome");
        Text text = Text.literal(translatedMessage).formatted(Formatting.GREEN);
        client.player.sendMessage(text, false);
    }

    public static String getTranslation(String key) {
        return translations.getOrDefault(key, fallbackTranslations.getOrDefault(key, key));
    }
}