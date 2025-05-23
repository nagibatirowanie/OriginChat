package me.nagibatirowanie.originchat.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plug-in configuration manager
 */
public class ConfigManager {

    private final OriginChat plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, List<String>> excludedPaths;
    
    private FileConfiguration mainConfig;
    
    public ConfigManager(OriginChat plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.excludedPaths = new HashMap<>();
        
        // Add exclusions for sections that should not be restored
        // Note: only top-level sections are excluded, not full paths
        // For example, for path "chats.admin", the entire "chats" section will be excluded
        addExcludedPath("modules/chat", "chats.admin");
        addExcludedPath("modules/chat", "chats.private");
        
        // Add the same exclusions for file name without path (for backward compatibility)
        addExcludedPath("config", "modules.enabled");

        // You can add other exclusions for different configurations
        // addExcludedPath("config", "some.path");
        
        // For debugging
        plugin.getPluginLogger().debug("Added exclusions for configuration: top-level sections");
    }
    
    /**
     * Adds a path to the list of exclusions that will not be restored during update
     * @param configName configuration name without .yml extension
     * @param path path to the field in configuration (e.g., "chats.admin")
     */
    public void addExcludedPath(String configName, String path) {
        // Get only the first part of the path (top-level section)
        // For example, from "chats.admin" we get "chats"
        String section = path.split("\\.")[0];
        excludedPaths.computeIfAbsent(configName, k -> new ArrayList<>()).add(section);
    }
    
    /**
     * Adds multiple paths to the exclusion list
     * @param configName configuration name without .yml extension
     * @param paths list of paths to fields
     */
    public void addExcludedPaths(String configName, List<String> paths) {
        // For each path, get only the top-level section
        List<String> sections = new ArrayList<>();
        for (String path : paths) {
            String section = path.split("\\.")[0];
            sections.add(section);
        }
        excludedPaths.computeIfAbsent(configName, k -> new ArrayList<>()).addAll(sections);
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
        
        try {
            // Get the list of exclusions for configuration
            // Important: ConfigUpdater library expects only top-level section names, not full paths
            List<String> ignoredSections = excludedPaths.getOrDefault("config", new ArrayList<>());
            
            // Update configuration using the library
            ConfigUpdater.update(plugin, "config.yml", configFile, ignoredSections);
            
            // Reload configuration after update
            plugin.reloadConfig();
            mainConfig = plugin.getConfig();
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Error updating configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        configs.put("config", mainConfig);
    }
    
    /**
     * Load custom config
     * @param name config name without.yml extension
     * @return loaded configuration or null in case of an error
     */
    public FileConfiguration loadConfig(String name) {
        // Handle paths with directories (e.g., "modules/server_brand")
        String resourcePath = name + ".yml";
        File configFile = new File(plugin.getDataFolder(), resourcePath);
        
        if (!configFile.exists()) {
            // Create all necessary directories
            configFile.getParentFile().mkdirs();
            
            // Try to save resource from JAR file
            try {
                // Check if resource exists in JAR file
                if (plugin.getResource(resourcePath) != null) {
                    plugin.saveResource(resourcePath, false);
                    plugin.getPluginLogger().debug("Configuration file created: " + resourcePath);
                } else {
                    // If resource doesn't exist in JAR, create empty file
                    configFile.createNewFile();
                    plugin.getPluginLogger().debug("Empty configuration file created: " + resourcePath);
                }
            } catch (Exception e) {
                plugin.getPluginLogger().severe("Error creating configuration file '" + name + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        try {
            // Check if resource exists in JAR for updating
            if (plugin.getResource(resourcePath) != null) {
                // Get list of exclusions for configuration
                // Important: ConfigUpdater library expects only top-level section names, not full paths
                List<String> ignoredSections = excludedPaths.getOrDefault(name, new ArrayList<>());
                
                // Update configuration using the library
                ConfigUpdater.update(plugin, resourcePath, configFile, ignoredSections);
                
                // Reload configuration after update
                config = YamlConfiguration.loadConfiguration(configFile);
            }
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Error updating configuration '" + name + "': " + e.getMessage());
            e.printStackTrace();
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
     * Get localized message as Adventure Component from module config
     * @param moduleName module name without .yml extension
     * @param path path to message in config
     * @param locale locale code (e.g. "en", "ru")
     * @return localized message as Component or message from default locale if not found
     */
    public Component getLocalizedComponent(String moduleName, String path, String locale) {
        String message = getLocalizedMessage(moduleName, path, locale);
        return FormatUtil.format(message);
    }
    
    /**
     * Get localized message as Adventure Component from module config
     * @param moduleName module name without .yml extension
     * @param path path to message in config
     * @param player player to get locale from, can be null (will use default locale)
     * @return localized message as Component
     */
    public Component getLocalizedComponent(String moduleName, String path, Player player) {
        String locale;
        if (player != null) {
            locale = plugin.getLocaleManager().getPlayerLocale(player);
        } else {
            locale = plugin.getLocaleManager().getDefaultLanguage();
        }
        
        return getLocalizedComponent(moduleName, path, locale);
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
     * Get localized message list as Adventure Components from module config
     * @param moduleName module name without .yml extension
     * @param path path to message list in config
     * @param locale locale code (e.g. "en", "ru")
     * @return list of localized messages as Components
     */
    public List<Component> getLocalizedComponentList(String moduleName, String path, String locale) {
        List<String> messages = getLocalizedMessageList(moduleName, path, locale);
        return messages.stream()
                .map(FormatUtil::format)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get localized message list as Adventure Components from module config
     * @param moduleName module name without .yml extension
     * @param path path to message list in config
     * @param player player to get locale from, can be null (will use default locale)
     * @return list of localized messages as Components
     */
    public List<Component> getLocalizedComponentList(String moduleName, String path, Player player) {
        String locale;
        if (player != null) {
            locale = plugin.getLocaleManager().getPlayerLocale(player);
        } else {
            locale = plugin.getLocaleManager().getDefaultLanguage();
        }
        
        return getLocalizedComponentList(moduleName, path, locale);
    }
}