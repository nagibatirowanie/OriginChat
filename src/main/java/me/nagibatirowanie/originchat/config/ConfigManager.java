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
 * Менеджер конфигураций плагина
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
     * Загрузить все конфигурации
     */
    public void loadConfigs() {
        // Загрузка основного конфига
        loadMainConfig();
        
        // Загрузка других конфигов при необходимости
    }
    
    /**
     * Загрузить основной конфиг
     */
    private void loadMainConfig() {
        // Сохранение конфига по умолчанию, если он не существует
        plugin.saveDefaultConfig();
        
        // Перезагрузка конфига из файла
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        
        // Проверка и обновление конфига при необходимости
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        configUpdater.checkAndUpdateConfig(configFile, mainConfig, "config.yml");
        
        // Сохранение конфига в кэше
        configs.put("config", mainConfig);
    }
    
    /**
     * Загрузить кастомный конфиг
     * @param name имя конфига (без расширения)
     * @return загруженная конфигурация или null в случае ошибки
     */
    public FileConfiguration loadConfig(String name) {
        File configFile = new File(plugin.getDataFolder(), name + ".yml");
        
        // Если файл не существует, создаем его из ресурсов
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(name + ".yml", false);
        }
        
        // Загрузка конфигурации из файла
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // Загрузка конфигурации по умолчанию из ресурсов для сравнения
        InputStream defaultConfigStream = plugin.getResource(name + ".yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            
            // Проверка и обновление конфига при необходимости
            configUpdater.checkAndUpdateConfig(configFile, config, name + ".yml");
        }
        
        // Сохранение конфига в кэше
        configs.put(name, config);
        
        return config;
    }
    
    /**
     * Сохранить конфиг
     * @param name имя конфига (без расширения)
     * @return успешность сохранения
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
            plugin.getPluginLogger().severe("Ошибка при сохранении конфига '" + name + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Перезагрузить конфиг
     * @param name имя конфига (без расширения)
     * @return перезагруженная конфигурация или null в случае ошибки
     */
    public FileConfiguration reloadConfig(String name) {
        if (name.equals("config")) {
            loadMainConfig();
            return mainConfig;
        }
        
        return loadConfig(name);
    }
    
    /**
     * Получить конфиг по имени
     * @param name имя конфига (без расширения)
     * @return конфигурация или null, если не найдена
     */
    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, null);
    }
    
    /**
     * Получить основной конфиг
     * @return основной конфиг
     */
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    /**
     * Получить обновитель конфигов
     * @return обновитель конфигов
     */
    public ConfigUpdater getConfigUpdater() {
        return configUpdater;
    }
}