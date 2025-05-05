package me.nagibatirowanie.originchat.config;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        configUpdater.checkAndUpdateConfig(configFile, mainConfig, "config.yml");
        
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
     * Get main config
     * @return main config
     */
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    /**
     * Get the config updater
     * @return config updater
     */
    public ConfigUpdater getConfigUpdater() {
        return configUpdater;
    }
}