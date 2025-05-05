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
 * Class for automatic updating of configurations
 */
public class ConfigUpdater {

    private final OriginChat plugin;

    public ConfigUpdater(OriginChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Check and update the configuration if necessary
     * @param configFile configuration file
     * @param config downloaded configuration
     * @param resourceName resource name in jar
     * @return true if the configuration has been updated
     */
    public boolean checkAndUpdateConfig(File configFile, FileConfiguration config, String resourceName) {
        if (!config.contains("version")) {
            plugin.getPluginLogger().warning("Конфиг '" + resourceName + "' не содержит поле 'version'!");
            return false;
        }

        int currentVersion = config.getInt("version");

        InputStream defaultConfigStream = plugin.getResource(resourceName);
        if (defaultConfigStream == null) {
            plugin.getPluginLogger().warning("Ресурс '" + resourceName + "' не найден в jar!");
            return false;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));

        if (!defaultConfig.contains("version")) {
            plugin.getPluginLogger().warning("Ресурс '" + resourceName + "' не содержит поле 'version'!");
            return false;
        }

        int defaultVersion = defaultConfig.getInt("version");

        if (currentVersion < defaultVersion) {
            plugin.getPluginLogger().info("Обновление конфига '" + resourceName + "' с версии " + currentVersion + " до " + defaultVersion);
            return updateConfig(configFile, config, defaultConfig);
        }

        return false;
    }

    /**
     * Update the configuration
     * @param configFile configuration file
     * @param config current configuration
     * @param defaultConfig default configuration
     * @return true if the configuration has been updated
     */
    private boolean updateConfig(File configFile, FileConfiguration config, FileConfiguration defaultConfig) {
        try {
            mergeConfigs(config, defaultConfig);

            config.set("version", defaultConfig.getInt("version"));

            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Ошибка при обновлении конфига: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Merge configurations by adding new sections and values
     * @param config current configuration
     * @param defaultConfig default configuration
     */
    private void mergeConfigs(FileConfiguration config, FileConfiguration defaultConfig) {
        Set<String> keys = defaultConfig.getKeys(true);

        for (String key : keys) {
            if (!config.contains(key)) {
                if (defaultConfig.isConfigurationSection(key)) {
                    ConfigurationSection section = defaultConfig.getConfigurationSection(key);
                    if (section != null) {
                        config.createSection(key, section.getValues(false));
                    }
                } else {
                    config.set(key, defaultConfig.get(key));
                }
            }
        }
    }
}