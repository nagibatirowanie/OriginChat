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
        if (!config.contains("config-version")) {
            plugin.getPluginLogger().warning("Config '" + resourceName + "' does not contain 'config-version' field!");
            return false;
        }

        int currentVersion = config.getInt("config-version");

        InputStream defaultConfigStream = plugin.getResource(resourceName);
        if (defaultConfigStream == null) {
            plugin.getPluginLogger().warning("Resource '" + resourceName + "' not found in jar!");
            return false;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));

        if (!defaultConfig.contains("config-version")) {
            plugin.getPluginLogger().warning("Resource '" + resourceName + "' does not contain 'config-version' field!");
            return false;
        }

        int defaultVersion = defaultConfig.getInt("config-version");

        boolean updated = false;
        if (currentVersion < defaultVersion) {
            plugin.getPluginLogger().info("Updating config '" + resourceName + "' from version " + currentVersion + " to " + defaultVersion);
            updated = updateConfig(configFile, config, defaultConfig);
        } else {
            try {
                boolean hasChanges = mergeConfigs(config, defaultConfig);
                if (hasChanges) {
                    config.save(configFile);
                    updated = true;
                }
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Error updating config: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return updated;
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
            boolean hasChanges = mergeConfigs(config, defaultConfig);
            int oldVersion = config.getInt("config-version");
            int newVersion = defaultConfig.getInt("config-version");
            config.set("config-version", newVersion);

            hasChanges = hasChanges || (oldVersion != newVersion);

            if (hasChanges) {
                config.save(configFile);
                return true;
            }
            return false;
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Error updating config: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Merge configurations by adding new sections and values
     * @param config current configuration
     * @param defaultConfig default configuration
     * @return true if any changes were made
     */
    private boolean mergeConfigs(FileConfiguration config, FileConfiguration defaultConfig) {
        boolean hasChanges = false;

        for (String rootKey : defaultConfig.getKeys(false)) {
            if (rootKey.equals("config-version")) {
                continue;
            }

            if (!config.getKeys(false).contains(rootKey)) {
                if (defaultConfig.isConfigurationSection(rootKey)) {
                    ConfigurationSection section = defaultConfig.getConfigurationSection(rootKey);
                    if (section != null) {
                        config.createSection(rootKey, section.getValues(true));
                        plugin.getPluginLogger().info("Added missing root section: '" + rootKey + "'");
                        hasChanges = true;
                    }
                } else {
                    Object value = defaultConfig.get(rootKey);
                    config.set(rootKey, value);
                    plugin.getPluginLogger().info("Added missing root key: '" + rootKey + "' with value: '" + value + "'");
                    hasChanges = true;
                }
            } else if (defaultConfig.isConfigurationSection(rootKey)) {
                ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(rootKey);
                ConfigurationSection currentSection = config.getConfigurationSection(rootKey);

                if (defaultSection != null && currentSection != null) {
                    hasChanges |= mergeConfigSections(currentSection, defaultSection, rootKey);
                }
            }
        }

        return hasChanges;
    }

    /**
     * Recursively merge configuration sections
     * @param currentSection current section
     * @param defaultSection default section
     * @param parentPath parent section path
     * @return true if any changes were made
     */
    private boolean mergeConfigSections(ConfigurationSection currentSection, ConfigurationSection defaultSection, String parentPath) {
        boolean hasChanges = false;

        for (String key : defaultSection.getKeys(false)) {
            String fullPath = parentPath + "." + key;

            if (!currentSection.getKeys(false).contains(key)) {
                if (defaultSection.isConfigurationSection(key)) {
                    ConfigurationSection nestedSection = defaultSection.getConfigurationSection(key);
                    if (nestedSection != null) {
                        currentSection.createSection(key, nestedSection.getValues(true));
                        plugin.getPluginLogger().info("Added missing nested section: '" + fullPath + "'");
                        hasChanges = true;
                    }
                } else {
                    Object value = defaultSection.get(key);
                    currentSection.set(key, value);
                    plugin.getPluginLogger().info("Added missing nested key: '" + fullPath + "' with value: '" + value + "'");
                    hasChanges = true;
                }
            } else if (defaultSection.isConfigurationSection(key)) {
                ConfigurationSection defaultNestedSection = defaultSection.getConfigurationSection(key);
                ConfigurationSection currentNestedSection = currentSection.getConfigurationSection(key);

                if (defaultNestedSection != null && currentNestedSection != null) {
                    hasChanges |= mergeConfigSections(currentNestedSection, defaultNestedSection, fullPath);
                }
            }
        }

        return hasChanges;
    }
}