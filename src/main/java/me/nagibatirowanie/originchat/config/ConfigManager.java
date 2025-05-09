package me.nagibatirowanie.originchat.config;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plug-in configuration manager
 */
public class ConfigManager {

    private final OriginChat plugin;
    private final Map<String, FileConfiguration> configs;
    private final ConfigUpdater configUpdater;
    
    private FileConfiguration mainConfig;
    
    public ConfigManager(OriginChat plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configUpdater = new ConfigUpdater(plugin);
    }
    
    /**
     * Load all configs
     */
    public void loadConfigs() {
        loadMainConfig();
        
    }
    
    /**
     * Load main config
     */
    private void loadMainConfig() {        
        plugin.saveDefaultConfig();
                
        plugin.reloadConfig();
                
        mainConfig = plugin.getConfig();
               
        File configFile = new File(plugin.getDataFolder(), "config.yml");
                
        boolean updated = configUpdater.checkAndUpdateConfig(configFile, mainConfig, "config.yml");
                
        if (updated) {
                        plugin.reloadConfig();
            mainConfig = plugin.getConfig();
        }
        
        configs.put("config", mainConfig);
            }
    
    /**
     * Load custom config
     * @param name config name without.yml extension
     * @return loaded configuration or null in case of an error
     */
    public FileConfiguration loadConfig(String name) {
        File configFile = new File(plugin.getDataFolder(), name + ".yml");
        
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(name + ".yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        InputStream defaultConfigStream = plugin.getResource(name + ".yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            
            configUpdater.checkAndUpdateConfig(configFile, config, name + ".yml");
        }
        
        configs.put(name, config);
        
        return config;
    }
    
    /**
     * Save config
     * @param name config name without.yml extension
     * @return save success
     */
    public boolean saveConfig(String name) {
        if (!configs.containsKey(name)) {
            return false;
        }
        
        File configFile = new File(plugin.getDataFolder(), name + ".yml");
        try {
            configs.get(name).save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Error when saving a config '" + name + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reload config
     * @param name config name without.yml extension
     * @return reloaded configuration or null in case of an error
     */
    public FileConfiguration reloadConfig(String name) {
        if (name.equals("config")) {
            loadMainConfig();
            return mainConfig;
        }
        
        return loadConfig(name);
    }
    
    /**
     * Get config by name
     * @param name config name without .yml extension
     * @return config or null if not found
     */
    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, null);
    }
    
    /**
     * Get localized message from module config
     * @param moduleName module name without .yml extension
     * @param path path to message in config
     * @param locale locale code (e.g. "en", "ru")
     * @return localized message or message from default locale if not found
     */
    public String getLocalizedMessage(String moduleName, String path, String locale) {
        // Try to get message from locale file first
        String localeKey = "modules." + moduleName + "." + path;
        String message = plugin.getLocaleManager().getMessage(localeKey, locale);
        
        // If message is not found in locale file (returns key with error prefix), try to get from module config
        if (message.startsWith("§cMessage not found") || message.equals(localeKey)) {
            FileConfiguration moduleConfig = getConfig("modules/" + moduleName);
            if (moduleConfig != null && moduleConfig.contains(path)) {
                return moduleConfig.getString(path);
            }
        }
        
        return message;
    }
    
    /**
     * Get localized message from module config
     * @param moduleName module name without .yml extension
     * @param path path to message in config
     * @param player player to get locale from, can be null (will use default locale)
     * @return localized message
     */
    public String getLocalizedMessage(String moduleName, String path, Player player) {
        String locale;
        if (player != null) {
            locale = plugin.getLocaleManager().getPlayerLocale(player);
        } else {
            locale = plugin.getLocaleManager().getDefaultLanguage();
        }
        
        return getLocalizedMessage(moduleName, path, locale);
    }
    
    /**
     * Get localized string list
     * @param module module name
     * @param key list key
     * @param player player for locale detection
     * @return localized string list or empty list if not found
     */
    public List<String> getLocalizedStringList(String module, String key, Player player) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Try to get list from player's locale
        List<String> list = getLocalizedMessageList(module, key, locale);
        
        // If not found, try to get from default locale
        if (list == null || list.isEmpty()) {
            list = getLocalizedMessageList(module, key, plugin.getLocaleManager().getDefaultLanguage());
        }
        
        return list != null ? list : List.of();
    }
    
    /**
     * Get main config
     * @return main config
     */
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    /**
     * Get localized message list from module config
     * @param moduleName module name without .yml extension
     * @param path path to message list in config
     * @param locale locale code (e.g. "en", "ru")
     * @return localized message list or message list from default locale if not found
     */
    public List<String> getLocalizedMessageList(String moduleName, String path, String locale) {
        // Try to get message list from locale file first
        String localeKey = "modules." + moduleName + "." + path;
        List<String> messages = plugin.getLocaleManager().getMessageList(localeKey, locale);
        
        // If message list is empty or contains error message, try to get from module config
        if (messages.isEmpty() || 
            (messages.size() == 1 && (messages.get(0).startsWith("§cMessage list not found") || 
                                     messages.get(0).equals(localeKey)))) {
            FileConfiguration moduleConfig = getConfig("modules/" + moduleName);
            if (moduleConfig != null && moduleConfig.contains(path)) {
                return moduleConfig.getStringList(path);
            }
        }
        
        return messages;
    }
    
    /**
     * Get localized message list from module config
     * @param moduleName module name without .yml extension
     * @param path path to message list in config
     * @param player player to get locale from, can be null (will use default locale)
     * @return localized message list
     */
    public List<String> getLocalizedMessageList(String moduleName, String path, Player player) {
        String locale;
        if (player != null) {
            locale = plugin.getLocaleManager().getPlayerLocale(player);
        } else {
            locale = plugin.getLocaleManager().getDefaultLanguage();
        }
        
        return getLocalizedMessageList(moduleName, path, locale);
    }
    
    /**
     * Get the config updater
     * @return config updater
     */
    public ConfigUpdater getConfigUpdater() {
        return configUpdater;
    }
}