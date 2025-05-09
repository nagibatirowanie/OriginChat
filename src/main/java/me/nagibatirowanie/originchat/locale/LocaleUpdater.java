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

/**
 * Class for automatic updating of localization files
 */
public class LocaleUpdater {

    private final OriginChat plugin;

    public LocaleUpdater(OriginChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Check and update the localization file if necessary
     * @param localeFile localization file
     * @param localeConfig loaded configuration
     * @param resourceName resource name in jar
     * @return true if the localization was updated
     */
    public boolean checkAndUpdateLocale(File localeFile, FileConfiguration localeConfig, String resourceName) {
        if (!localeConfig.contains("config-version")) {
            localeConfig.set("config-version", 3);
            try {
                localeConfig.save(localeFile);
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Error when saving the version of the localization file: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            return true;
        }

        int currentVersion = localeConfig.getInt("config-version");

        InputStream defaultLocaleStream = plugin.getResource(resourceName);
        if (defaultLocaleStream == null) {
            plugin.getPluginLogger().warning("Resource '" + resourceName + "' not found in jar!");
            return false;
        }

        YamlConfiguration defaultLocale = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultLocaleStream, StandardCharsets.UTF_8));

        if (!defaultLocale.contains("config-version")) {
            plugin.getPluginLogger().warning("Resource '" + resourceName + "' does not contain 'config-version' field! Assuming version 3.");
            return false;
        }

        int defaultVersion = defaultLocale.getInt("config-version");

        boolean updated = false;
        if (currentVersion < defaultVersion) {
            plugin.getPluginLogger().info("Updating localization '" + resourceName + "' from version " + currentVersion + " to " + defaultVersion);
            updated = updateLocale(localeFile, localeConfig, defaultLocale);
        } else {
            try {
                boolean hasChanges = mergeLocales(localeConfig, defaultLocale);
                if (hasChanges) {
                    localeConfig.save(localeFile);
                    updated = true;
                }
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Error updating localization: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return updated;
    }

    /**
     * Update the localization file
     * @param localeFile localization file
     * @param localeConfig current configuration
     * @param defaultLocale default configuration
     * @return true if the localization was updated
     */
    private boolean updateLocale(File localeFile, FileConfiguration localeConfig, FileConfiguration defaultLocale) {
        try {
            boolean hasChanges = mergeLocales(localeConfig, defaultLocale);
            int oldVersion = localeConfig.getInt("config-version");
            int newVersion = defaultLocale.getInt("config-version");
            localeConfig.set("config-version", newVersion);

            hasChanges = hasChanges || (oldVersion != newVersion);

            if (hasChanges) {
                localeConfig.save(localeFile);
                return true;
            }
            return false;
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Error when updating localization: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Merge localizations by adding new sections and values
     * @param localeConfig current configuration
     * @param defaultLocale default configuration
     * @return true if any changes were made
     */
    private boolean mergeLocales(FileConfiguration localeConfig, FileConfiguration defaultLocale) {
        boolean hasChanges = false;

        for (String key : defaultLocale.getKeys(true)) {
            if (key.equals("config-version")) {
                continue;
            }

            if (!localeConfig.contains(key)) {
                if (defaultLocale.isConfigurationSection(key)) {
                    ConfigurationSection section = defaultLocale.getConfigurationSection(key);
                    if (section != null) {
                        localeConfig.createSection(key, section.getValues(false));
                        plugin.getPluginLogger().info("Added missing localization section: '" + key + "'");
                    }
                } else {
                    Object value = defaultLocale.get(key);
                    localeConfig.set(key, value);
                    plugin.getPluginLogger().info("Added missing localization key: '" + key + "' with value: '" + value + "'");
                }
                hasChanges = true;
            }
        }

        return hasChanges;
    }
}