package me.nagibatirowanie.originchat.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
        
        // Добавляем исключения для секций, которые не должны восстанавливаться
        // Обратите внимание: исключаются только секции верхнего уровня, а не полные пути
        // Например, для пути "chats.admin" будет исключена вся секция "chats"
        addExcludedPath("modules/chat", "chats.admin");
        addExcludedPath("modules/chat", "chats.private");
        
        // Добавляем те же исключения для имени файла без пути (для обратной совместимости)
        addExcludedPath("config", "modules.enabled");

        // Можно добавить и другие исключения для разных конфигураций
        // addExcludedPath("config", "some.path");
        
        // Для отладки
        plugin.getLogger().info("Добавлены исключения для конфигурации: секции верхнего уровня");
    }
    
    /**
     * Добавляет путь к списку исключений, которые не будут восстанавливаться при обновлении
     * @param configName имя конфигурации без расширения .yml
     * @param path путь к полю в конфигурации (например, "chats.admin")
     */
    public void addExcludedPath(String configName, String path) {
        // Получаем только первую часть пути (секцию верхнего уровня)
        // Например, из "chats.admin" получаем "chats"
        String section = path.split("\\.")[0];
        excludedPaths.computeIfAbsent(configName, k -> new ArrayList<>()).add(section);
    }
    
    /**
     * Добавляет несколько путей к списку исключений
     * @param configName имя конфигурации без расширения .yml
     * @param paths список путей к полям
     */
    public void addExcludedPaths(String configName, List<String> paths) {
        // Для каждого пути получаем только секцию верхнего уровня
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
            // Получаем список исключений для конфигурации
            // Важно: библиотека ConfigUpdater ожидает только имена секций верхнего уровня, а не полные пути
            List<String> ignoredSections = excludedPaths.getOrDefault("config", new ArrayList<>());
            
            // Обновляем конфигурацию с помощью библиотеки
            ConfigUpdater.update(plugin, "config.yml", configFile, ignoredSections);
            
            // Перезагружаем конфигурацию после обновления
            plugin.reloadConfig();
            mainConfig = plugin.getConfig();
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Ошибка при обновлении конфигурации: " + e.getMessage());
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
        // Обрабатываем пути с директориями (например, "modules/server_brand")
        String resourcePath = name + ".yml";
        File configFile = new File(plugin.getDataFolder(), resourcePath);
        
        if (!configFile.exists()) {
            // Создаем все необходимые директории
            configFile.getParentFile().mkdirs();
            
            // Пытаемся сохранить ресурс из JAR-файла
            try {
                // Проверяем, существует ли ресурс в JAR-файле
                if (plugin.getResource(resourcePath) != null) {
                    plugin.saveResource(resourcePath, false);
                    plugin.getLogger().info("Создан файл конфигурации: " + resourcePath);
                } else {
                    // Если ресурс не существует в JAR, создаем пустой файл
                    configFile.createNewFile();
                    plugin.getLogger().info("Создан пустой файл конфигурации: " + resourcePath);
                }
            } catch (Exception e) {
                plugin.getPluginLogger().severe("Ошибка при создании файла конфигурации '" + name + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        try {
            // Проверяем, существует ли ресурс в JAR для обновления
            if (plugin.getResource(resourcePath) != null) {
                // Получаем список исключений для конфигурации
                // Важно: библиотека ConfigUpdater ожидает только имена секций верхнего уровня, а не полные пути
                List<String> ignoredSections = excludedPaths.getOrDefault(name, new ArrayList<>());
                
                // Обновляем конфигурацию с помощью библиотеки
                ConfigUpdater.update(plugin, resourcePath, configFile, ignoredSections);
                
                // Перезагружаем конфигурацию после обновления
                config = YamlConfiguration.loadConfiguration(configFile);
            }
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Ошибка при обновлении конфигурации '" + name + "': " + e.getMessage());
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
    

}