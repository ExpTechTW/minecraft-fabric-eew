package com.exptech.eew.client.utils;

import com.exptech.eew.client.EewClient;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TranslationManager {
    private static final Map<String, String> translations = new HashMap<>();
    private static final Map<String, String> fallbackTranslations = new HashMap<>();
    private static final Yaml yaml = new Yaml();
    private static final Logger LOGGER = EewClient.LOGGER;

    private static void loadTranslationFile(String filePath, Map<String, String> targetMap) {
        try {
            InputStream inputStream = TranslationManager.class.getResourceAsStream(filePath);
            if (inputStream != null) {
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                Map<String, String> loadedTranslations = yaml.load(reader);
                targetMap.putAll(loadedTranslations);
                reader.close();
            } else {
                LOGGER.warn("Translation file not found: {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading translation file: {}", filePath, e);
        }
    }

    private static void loadUserLanguageTranslations() {
        String userLang = getUserLanguage();
        loadTranslationFile("/lang/" + userLang + ".yaml", translations);
    }

    private static String getUserLanguage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getLanguageManager() != null) {
            return client.getLanguageManager().getLanguage();
        }
        LOGGER.warn("Unable to get user language settings, using default 'zh_tw'");
        return "zh_tw";
    }

    public static String getTranslation(String key) {
        return translations.getOrDefault(key, fallbackTranslations.getOrDefault(key, key));
    }

    public static void initialize() {
        loadTranslationFile("/lang/zh_tw.yaml", fallbackTranslations);
        loadUserLanguageTranslations();
    }
}