package me.nagibatirowanie.originchat.locale;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Класс для автоматического обновления локализационных файлов
 */
public class LocaleUpdater {

    private final OriginChat plugin;

    public LocaleUpdater(OriginChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Проверить и обновить локализационный файл при необходимости
     * @param localeFile файл локализации
     * @param localeConfig загруженная конфигурация
     * @param resourceName имя ресурса в jar
     * @return true если локализация была обновлена
     */
    public boolean checkAndUpdateLocale(File localeFile, FileConfiguration localeConfig, String resourceName) {
        // Если в файле нет поля version, добавляем его со значением 1
        if (!localeConfig.contains("version")) {
            plugin.getPluginLogger().warning("Локализационный файл '" + resourceName + "' не содержит поле 'version'! Устанавливаем версию 1.");
            localeConfig.set("version", 1);
            try {
                localeConfig.save(localeFile);
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Ошибка при сохранении версии локализационного файла: " + e.getMessage());
                return false;
            }
            return true;
        }

        int currentVersion = localeConfig.getInt("version");

        InputStream defaultLocaleStream = plugin.getResource(resourceName);
        if (defaultLocaleStream == null) {
            plugin.getPluginLogger().warning("Ресурс '" + resourceName + "' не найден в jar!");
            return false;
        }

        YamlConfiguration defaultLocale = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultLocaleStream, StandardCharsets.UTF_8));

        // Если в ресурсе нет поля version, добавляем его со значением 1
        if (!defaultLocale.contains("version")) {
            plugin.getPluginLogger().warning("Ресурс '" + resourceName + "' не содержит поле 'version'! Предполагаем версию 1.");
            return false;
        }

        int defaultVersion = defaultLocale.getInt("version");

        if (currentVersion < defaultVersion) {
            plugin.getPluginLogger().info("Обновление локализации '" + resourceName + "' с версии " + currentVersion + " до " + defaultVersion);
            return updateLocale(localeFile, localeConfig, defaultLocale);
        }

        return false;
    }

    /**
     * Обновить локализационный файл
     * @param localeFile файл локализации
     * @param localeConfig текущая конфигурация
     * @param defaultLocale конфигурация по умолчанию
     * @return true если локализация была обновлена
     */
    private boolean updateLocale(File localeFile, FileConfiguration localeConfig, FileConfiguration defaultLocale) {
        try {
            mergeLocales(localeConfig, defaultLocale);

            localeConfig.set("version", defaultLocale.getInt("version"));

            localeConfig.save(localeFile);
            return true;
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Ошибка при обновлении локализации: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Объединить локализации, добавляя новые секции и значения
     * @param localeConfig текущая конфигурация
     * @param defaultLocale конфигурация по умолчанию
     */
    private void mergeLocales(FileConfiguration localeConfig, FileConfiguration defaultLocale) {
        Set<String> keys = defaultLocale.getKeys(true);

        for (String key : keys) {
            // Пропускаем поле version, оно будет установлено отдельно
            if (key.equals("version")) {
                continue;
            }
            
            if (!localeConfig.contains(key)) {
                if (defaultLocale.isConfigurationSection(key)) {
                    ConfigurationSection section = defaultLocale.getConfigurationSection(key);
                    if (section != null) {
                        localeConfig.createSection(key, section.getValues(false));
                    }
                } else {
                    localeConfig.set(key, defaultLocale.get(key));
                }
            }
        }
    }
}