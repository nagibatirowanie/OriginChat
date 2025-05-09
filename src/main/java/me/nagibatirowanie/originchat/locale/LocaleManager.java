package me.nagibatirowanie.originchat.locale;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.ColorUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Plugin localization manager
 */
public class LocaleManager {

    private final OriginChat plugin;
    private final Map<String, FileConfiguration> locales;
    private final LocaleUpdater localeUpdater;
    private FileConfiguration defaultLocale;
    private String defaultLanguage;
    
    public LocaleManager(OriginChat plugin) {
        this.plugin = plugin;
        this.locales = new HashMap<>();
        this.localeUpdater = new LocaleUpdater(plugin);
        this.defaultLanguage = plugin.getConfigManager().getMainConfig().getString("locale.default", "ru");
        loadLocales();
    }
    
    /**
     * Load all available locales
     */
    public void loadLocales() {
        // Очищаем кэш локализаций перед загрузкой
        locales.clear();
        defaultLocale = null;
        // Create locale directory if it doesn't exist
        File localeDir = new File(plugin.getDataFolder(), "locales");
        if (!localeDir.exists()) {
            localeDir.mkdirs();
        }
        
        // Get all locale files from resources
        try {
            // Get all locale files from resources directory
            InputStream resourceDirStream = plugin.getResource("locales");
            if (resourceDirStream != null) {
                // If we can access the directory listing, use it
                // This is not always possible in JAR files
                resourceDirStream.close();
            }
            
            // Save default locales from plugin resources
            // Since we can't list directory contents in JAR, we'll check for known locales
            saveDefaultLocale("ru");
            saveDefaultLocale("en");
            
            // Try to save any other locale files that might exist
            // This is a fallback method since we can't list directory contents in JAR
            try {
                // Check if there are other locale files in resources
                // Расширенный список поддерживаемых языков
                String[] commonLocales = {
                    // Европейские языки
                    "de", "fr", "es", "it", "pt", "nl", "pl", "sv", "no", "fi", "da", "cs", "sk", "hu", "ro", "bg", "el", "tr",
                    // Азиатские языки
                    "zh", "ja", "ko", "th", "vi", "id", "ms",
                    // Другие языки
                    "ar", "he", "hi", "uk", "fa", "af", "sq", "hy", "az", "eu", "be", "bn", "bs", "ca", "hr", "et", "tl", "gl", "ka",
                    "is", "kk", "km", "lo", "lv", "lt", "mk", "mn", "ne", "sr", "si", "sl", "sw", "ta", "te", "ur", "uz", "cy"
                };
                
                plugin.getPluginLogger().info("[LocaleManager] Проверка доступных локализаций...");
                for (String locale : commonLocales) {
                    try {
                        if (plugin.getResource("locales/" + locale + ".yml") != null) {
                            saveDefaultLocale(locale);
                            plugin.getPluginLogger().info("[LocaleManager] Загружена локализация: " + locale);
                        }
                    } catch (Exception ignored) {
                        // Ignore if locale doesn't exist
                    }
                }
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Could not check for additional locales: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Error accessing resource directory: " + e.getMessage());
        }
        
        // Load all locale files from directory
        File[] localeFiles = localeDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (localeFiles != null) {
            for (File file : localeFiles) {
                String fileName = file.getName();
                String localeName = fileName.substring(0, fileName.length() - 4); // Remove .yml extension
                
                FileConfiguration localeConfig = YamlConfiguration.loadConfiguration(file);
                
                // Проверяем и обновляем локализационный файл
                localeUpdater.checkAndUpdateLocale(file, localeConfig, "locales/" + localeName + ".yml");
                
                locales.put(localeName, localeConfig);
                
                plugin.getPluginLogger().info("Loaded locale: " + localeName);
            }
        }
        
        // Set default locale from configuration
        defaultLanguage = plugin.getConfigManager().getMainConfig().getString("locale.default", "ru");
        defaultLocale = locales.getOrDefault(defaultLanguage, locales.get("ru"));
        
        if (defaultLocale == null && !locales.isEmpty()) {
            defaultLocale = locales.values().iterator().next();
            defaultLanguage = locales.keySet().iterator().next();
        }
    }
    
    /**
     * Save default locale from plugin resources
     * @param locale locale name
     * @return true if locale was saved or updated successfully
     */
    private boolean saveDefaultLocale(String locale) {
        File localeFile = new File(plugin.getDataFolder(), "locales/" + locale + ".yml");
        
        if (!localeFile.exists()) {
            try {
                plugin.saveResource("locales/" + locale + ".yml", false);
                plugin.getPluginLogger().info("Saved new locale file: " + locale);
                return true;
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Could not save locale file " + locale + ": " + e.getMessage());
                return false;
            }
        } else {
            // Проверяем и обновляем существующий файл локализации
            FileConfiguration localeConfig = YamlConfiguration.loadConfiguration(localeFile);
            boolean updated = localeUpdater.checkAndUpdateLocale(localeFile, localeConfig, "locales/" + locale + ".yml");
            if (updated) {
                plugin.getPluginLogger().info("Updated locale file: " + locale);
            }
            return true;
        }
    }
    
    /**
     * Проверить наличие локализационного файла
     * @param locale имя локализации
     * @return true если файл существует
     */
    public boolean hasLocale(String locale) {
        return locales.containsKey(locale);
    }
    
    /**
     * Get message from locale
     * @param key message key
     * @param locale locale name
     * @return message or key if message not found
     */
    public String getMessage(String key, String locale) {
        // Extract base language from locale if it contains country code (e.g. en_US -> en)
        String baseLocale = locale;
        if (locale.contains("_")) {
            baseLocale = locale.split("_")[0].toLowerCase();
        }
        
        // Try to get locale config
        FileConfiguration localeConfig = locales.getOrDefault(baseLocale, defaultLocale);
        
        if (localeConfig == null) {
            return "§cLocale not found: " + locale;
        }
        
        String message = localeConfig.getString(key);
        
        if (message == null) {
            // If message not found in specified locale, try to find in default locale
            if (localeConfig != defaultLocale && defaultLocale != null) {
                message = defaultLocale.getString(key);
            }
            
            // If message still not found, return key
            if (message == null) {
                return "§cMessage not found: " + key;
            }
        }
        
        return ColorUtil.format(message);
    }
    
    /**
     * Send message to player with their locale
     * @param sender message recipient
     * @param key message key
     * @param args arguments for replacement in message
     */
    public void sendMessage(CommandSender sender, String key, Object... args) {
        String locale = defaultLanguage;
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            locale = getPlayerLocale(player);
        }
        
        String message = getMessage(key, locale);
        
        // Replace arguments in message
        if (args.length > 0 && args.length % 2 == 0) {
            for (int i = 0; i < args.length; i += 2) {
                String placeholder = String.valueOf(args[i]);
                String value = String.valueOf(args[i + 1]);
                message = message.replace(placeholder, value);
            }
        }
        
        sender.sendMessage(message);
    }
    
    /**
     * Get player locale
     * @param player player
     * @return player locale or default locale
     */
    public String getPlayerLocale(Player player) {
        // If player is null, return default locale
        if (player == null) {
            return defaultLanguage;
        }
        
        // Check if auto-detection is enabled
        boolean autoDetect = plugin.getConfigManager().getMainConfig().getBoolean("locale.auto_detect", true);
        if (!autoDetect) {
            return defaultLanguage;
        }
        
        // Get locale from player configuration or use client locale
        String playerLocale = null;
        
        // Try to get locale from player client (works in Paper)
        try {
            // Using locale() instead of deprecated getLocale()
            String clientLocale = player.locale().toString();
            if (clientLocale != null && !clientLocale.isEmpty()) {
                // Используем полную локаль для поддержки всех языков
                // Преобразуем формат локали для совместимости с Google Translate
                // Например: en_US -> en, zh_CN -> zh-CN
                if (clientLocale.contains("_")) {
                    String[] parts = clientLocale.split("_");
                    // Для китайского, японского и других языков с региональными вариантами
                    // используем формат с дефисом (zh-CN, ja-JP и т.д.)
                    if (parts[0].equals("zh") || parts[0].equals("ja") || 
                        parts[0].equals("ko") || parts[0].equals("pt")) {
                        playerLocale = parts[0].toLowerCase() + "-" + parts[1].toUpperCase();
                    } else {
                        // Для остальных языков используем только код языка
                        playerLocale = parts[0].toLowerCase();
                    }
                } else {
                    playerLocale = clientLocale.toLowerCase();
                }
                
                // Логируем определенную локаль для отладки
                //plugin.getPluginLogger().info("[LocaleManager] Определена локаль игрока " + 
                    //player.getName() + ": " + clientLocale + " -> " + playerLocale);
            }
        } catch (Exception e) {
            // Логируем ошибку для отладки
            //plugin.getPluginLogger().warning("[LocaleManager] Ошибка при определении локали игрока " + 
                //player.getName() + ": " + e.getMessage());
        }
        
        // If locale is not defined or not supported, use default locale
        if (playerLocale == null || !locales.containsKey(playerLocale)) {
            playerLocale = defaultLanguage;
            //plugin.getPluginLogger().info("[LocaleManager] Используем локаль по умолчанию для игрока " + 
                //player.getName() + ": " + playerLocale);
        }
        
        return playerLocale;
    }

    public String getPlayerLocaleRaw(Player player) {
        if (player == null) {
            return null;
        }
        try {
            String clientLocale = player.locale().toString();
            if (clientLocale != null && !clientLocale.isEmpty()) {
                if (clientLocale.contains("_")) {
                    String[] parts = clientLocale.split("_");
                    if (parts[0].equals("zh") || parts[0].equals("ja") ||
                        parts[0].equals("ko") || parts[0].equals("pt")) {
                        return parts[0].toLowerCase() + "-" + parts[1].toUpperCase();
                    } else {
                        return parts[0].toLowerCase();
                    }
                } else {
                    return clientLocale.toLowerCase();
                }
            }
        } catch (Exception e) {
            //plugin.getPluginLogger().warning("[LocaleManager] Ошибка при определении локали игрока " +
                //player.getName() + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get default locale
     * @return default locale
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * Get message list from locale
     * @param key message key
     * @param locale locale name
     * @return message list or empty list if not found
     */
    // public java.util.List<String> getMessageList(String key, String locale) {
    //     // Extract base language from locale if it contains country code (e.g. en_US -> en)
    //     String baseLocale = locale;
    //     if (locale.contains("_")) {
    //         baseLocale = locale.split("_")[0].toLowerCase();
    //     }
        
    //     // Try to get locale config
    //     FileConfiguration localeConfig = locales.getOrDefault(baseLocale, defaultLocale);
        
    //     if (localeConfig == null) {
    //         return java.util.Collections.emptyList();
    //     }
        
    //     java.util.List<String> messages = localeConfig.getStringList(key);
        
    //     // If message list is empty in specified locale, try to find in default locale
    //     if (messages.isEmpty() && localeConfig != defaultLocale && defaultLocale != null) {
    //         messages = defaultLocale.getStringList(key);
    //     }
        
    //     return messages;
    // }
    
    /**
     * Get message list for player
     * @param key message key
     * @param player player
     * @return message list
     */
    public java.util.List<String> getMessageList(String key, Player player) {
        String locale = getPlayerLocale(player);
        return getMessageList(key, locale);
    }
    
    /**
     * Set default locale
     * @param defaultLanguage default locale
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        this.defaultLocale = locales.getOrDefault(defaultLanguage, defaultLocale);
    }
    
    /**
     * Get the locale updater
     * @return locale updater
     */
    public LocaleUpdater getLocaleUpdater() {
        return localeUpdater;
    }
    
    /**
     * Обновить локализацию
     * @param locale имя локализации без расширения .yml
     * @return обновленная конфигурация или null в случае ошибки
     */
    public FileConfiguration updateLocale(String locale) {
        // Если локализация не загружена, пытаемся загрузить её
        if (!locales.containsKey(locale)) {
            return null;
        }
        
        File localeFile = new File(plugin.getDataFolder(), "locales/" + locale + ".yml");
        if (!localeFile.exists()) {
            return null;
        }
        
        FileConfiguration localeConfig = YamlConfiguration.loadConfiguration(localeFile);
        
        // Проверяем и обновляем локализационный файл
        boolean updated = localeUpdater.checkAndUpdateLocale(localeFile, localeConfig, "locales/" + locale + ".yml");
        if (updated) {
            // Обновляем кэш
            locales.put(locale, localeConfig);
            
            // Если это дефолтная локализация, обновляем её тоже
            if (locale.equals(defaultLanguage)) {
                defaultLocale = localeConfig;
            }
        }
        
        return localeConfig;
    }
    
    /**
     * Get list of messages from locale
     * @param key message key
     * @param locale locale name
     * @return list of messages or empty list if not found
     */
    public java.util.List<String> getMessageList(String key, String locale) {
        // Extract base language from locale if it contains country code (e.g. en_US -> en)
        String baseLocale = locale;
        if (locale.contains("_")) {
            baseLocale = locale.split("_")[0].toLowerCase();
        }
        
        // Try to get locale config
        FileConfiguration localeConfig = locales.getOrDefault(baseLocale, defaultLocale);
        
        if (localeConfig == null) {
            return java.util.Collections.singletonList("§cLocale not found: " + locale);
        }
        
        java.util.List<String> messages = localeConfig.getStringList(key);
        
        if (messages.isEmpty()) {
            // If messages not found in specified locale, try to find in default locale
            if (localeConfig != defaultLocale && defaultLocale != null) {
                messages = defaultLocale.getStringList(key);
            }
            
            // If messages still not found, return key as error message
            if (messages.isEmpty()) {
                return java.util.Collections.singletonList("§cMessage list not found: " + key);
            }
        }
        
        // Format colors for each message
        return messages.stream()
                .map(ColorUtil::format)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Send list of messages to player with their locale
     * @param sender message recipient
     * @param key message key
     * @param args arguments for replacement in messages
     */
    public void sendMessageList(CommandSender sender, String key, Object... args) {
        String locale = defaultLanguage;
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            locale = getPlayerLocale(player);
        }
        
        java.util.List<String> messages = getMessageList(key, locale);
        
        // Replace arguments in each message
        if (args.length > 0 && args.length % 2 == 0) {
            for (int i = 0; i < messages.size(); i++) {
                String message = messages.get(i);
                for (int j = 0; j < args.length; j += 2) {
                    String placeholder = String.valueOf(args[j]);
                    String value = String.valueOf(args[j + 1]);
                    message = message.replace(placeholder, value);
                }
                messages.set(i, message);
            }
        }
        
        // Send each message
        messages.forEach(sender::sendMessage);
    }
}