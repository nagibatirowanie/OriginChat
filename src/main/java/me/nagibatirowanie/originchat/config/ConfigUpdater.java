package me.nagibatirowanie.originchat.config;

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
 * Класс для автоматического обновления конфигураций
 */
public class ConfigUpdater {

    private final OriginChat plugin;

    public ConfigUpdater(OriginChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Проверить и обновить конфигурацию при необходимости
     * @param configFile файл конфигурации
     * @param config загруженная конфигурация
     * @param resourceName имя ресурса в jar
     * @return true, если конфигурация была обновлена
     */
    public boolean checkAndUpdateConfig(File configFile, FileConfiguration config, String resourceName) {
        // Проверка версии конфига
        if (!config.contains("version")) {
            plugin.getPluginLogger().warning("Конфиг '" + resourceName + "' не содержит поле 'version'!");
            return false;
        }

        int currentVersion = config.getInt("version");

        // Загрузка конфигурации по умолчанию из ресурсов
        InputStream defaultConfigStream = plugin.getResource(resourceName);
        if (defaultConfigStream == null) {
            plugin.getPluginLogger().warning("Ресурс '" + resourceName + "' не найден в jar!");
            return false;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));

        // Проверка версии конфига по умолчанию
        if (!defaultConfig.contains("version")) {
            plugin.getPluginLogger().warning("Ресурс '" + resourceName + "' не содержит поле 'version'!");
            return false;
        }

        int defaultVersion = defaultConfig.getInt("version");

        // Если версия текущего конфига меньше версии по умолчанию, обновляем
        if (currentVersion < defaultVersion) {
            plugin.getPluginLogger().info("Обновление конфига '" + resourceName + "' с версии " + currentVersion + " до " + defaultVersion);
            return updateConfig(configFile, config, defaultConfig);
        }

        return false;
    }

    /**
     * Обновить конфигурацию
     * @param configFile файл конфигурации
     * @param config текущая конфигурация
     * @param defaultConfig конфигурация по умолчанию
     * @return true, если конфигурация была обновлена
     */
    private boolean updateConfig(File configFile, FileConfiguration config, FileConfiguration defaultConfig) {
        try {
            // Добавление новых секций и значений из конфига по умолчанию
            mergeConfigs(config, defaultConfig);

            // Обновление версии
            config.set("version", defaultConfig.getInt("version"));

            // Сохранение обновленного конфига
            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Ошибка при обновлении конфига: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Объединить конфигурации, добавляя новые секции и значения
     * @param config текущая конфигурация
     * @param defaultConfig конфигурация по умолчанию
     */
    private void mergeConfigs(FileConfiguration config, FileConfiguration defaultConfig) {
        // Получение всех ключей из конфига по умолчанию
        Set<String> keys = defaultConfig.getKeys(true);

        // Добавление отсутствующих ключей и значений
        for (String key : keys) {
            if (!config.contains(key)) {
                // Если ключ - секция, добавляем всю секцию
                if (defaultConfig.isConfigurationSection(key)) {
                    ConfigurationSection section = defaultConfig.getConfigurationSection(key);
                    if (section != null) {
                        config.createSection(key, section.getValues(false));
                    }
                } else {
                    // Иначе добавляем значение
                    config.set(key, defaultConfig.get(key));
                }
            }
        }
    }
}