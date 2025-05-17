package me.nagibatirowanie.originchat.locale;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.ColorUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plugin localization manager
 */
public class LocaleManager {

    private final OriginChat plugin;
    private final Map<String, FileConfiguration> locales;
    private final Map<String, List<String>> excludedPaths;
    private FileConfiguration defaultLocale;
    private String defaultLanguage;
    
    public LocaleManager(OriginChat plugin) {
        this.plugin = plugin;
        this.locales = new HashMap<>();
        this.excludedPaths = new HashMap<>();
        this.defaultLanguage = plugin.getConfigManager().getMainConfig().getString("locale.default", "ru");
        loadLocales();
    }
    
    /**
     * Adds a path to the exclusion list that won't be restored during updates
     * @param localeName localization name without .yml extension
     * @param path path to the field in localization (for example, "commands.help")
     * @deprecated Method is deprecated and not used, as all elements should be restored
     */
    @Deprecated
    public void addExcludedPath(String localeName, String path) {
        // Method kept for backward compatibility, but not used
        // All elements should be restored during updates
        plugin.getPluginLogger().info("[LocaleManager] Method addExcludedPath is deprecated and not used");
    }
    
    /**
     * Adds multiple paths to the exclusion list
     * @param localeName localization name without .yml extension
     * @param paths list of paths to fields
     * @deprecated Method is deprecated and not used, as all elements should be restored
     */
    @Deprecated
    public void addExcludedPaths(String localeName, List<String> paths) {
        // Method kept for backward compatibility, but not used
        // All elements should be restored during updates
        plugin.getPluginLogger().info("[LocaleManager] Method addExcludedPaths is deprecated and not used");
    }
    
    /**
     * Load all available locales
     */
    public void loadLocales() {
        // Clear localization cache before loading
        locales.clear();
        defaultLocale = null;
        // Create locale directory if it doesn't exist
        File localeDir = new File(plugin.getDataFolder(), "locales");
        if (!localeDir.exists()) {
            localeDir.mkdirs();
        }
        
        plugin.getPluginLogger().info("[LocaleManager] Starting loading and updating localizations...");
        
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
                // Extended list of supported languages
                String[] commonLocales = {
                    // European languages
                    "de", "fr", "es", "it", "pt", "nl", "pl", "sv", "no", "fi", "da", "cs", "sk", "hu", "ro", "bg", "el", "tr",
                    // Asian languages
                    "zh", "ja", "ko", "th", "vi", "id", "ms",
                    // Other languages
                    "ar", "he", "hi", "uk", "fa", "af", "sq", "hy", "az", "eu", "be", "bn", "bs", "ca", "hr", "et", "tl", "gl", "ka",
                    "is", "kk", "km", "lo", "lv", "lt", "mk", "mn", "ne", "sr", "si", "sl", "sw", "ta", "te", "ur", "uz", "cy"
                };
                
                plugin.getPluginLogger().info("[LocaleManager] Checking available localizations...");
                for (String locale : commonLocales) {
                    try {
                        if (plugin.getResource("locales/" + locale + ".yml") != null) {
                            saveDefaultLocale(locale);
                            plugin.getPluginLogger().info("[LocaleManager] Loaded localization: " + locale);
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
                
                // Check and update localization file
                checkAndUpdateLocale(file, "locales/" + localeName + ".yml");
                
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
            // Check and update existing localization file
            try {
                // Use empty exclusion list so all deleted elements are restored
                List<String> ignoredSections = new ArrayList<>();
                
                // Update localization using the library
                ConfigUpdater.update(plugin, "locales/" + locale + ".yml", localeFile, ignoredSections);
                
                // Reload file after update
                FileConfiguration updatedConfig = YamlConfiguration.loadConfiguration(localeFile);
                locales.put(locale, updatedConfig);
                
                // If this is the default localization, update it too
                if (locale.equals(defaultLanguage)) {
                    defaultLocale = updatedConfig;
                }
                
                plugin.getPluginLogger().info("Updated locale file: " + locale);
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Error when updating locale file " + locale + ": " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
    }
    
    /**
     * Check if localization file exists
     * @param locale localization name
     * @return true if file exists
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
        
        try {
            String clientLocale = player.locale().toString();
            if (clientLocale != null && !clientLocale.isEmpty()) {
                if (clientLocale.contains("_")) {
                    String[] parts = clientLocale.split("_");
                    if (parts[0].equals("zh") || parts[0].equals("ja") || 
                        parts[0].equals("ko") || parts[0].equals("pt")) {
                        playerLocale = parts[0].toLowerCase() + "-" + parts[1].toUpperCase();
                    } else {
                        playerLocale = parts[0].toLowerCase();
                    }
                } else {
                    playerLocale = clientLocale.toLowerCase();
                }
                

            }
        } catch (Exception e) {
            plugin.getPluginLogger().warning("[LocaleManager] Error when determining player's locale! " + 
                player.getName() + ": " + e.getMessage());
        }
        
        if (playerLocale == null || !locales.containsKey(playerLocale)) {
            playerLocale = defaultLanguage;
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
            plugin.getPluginLogger().warning("[LocaleManager] Error when determining player's locale! " + 
                player.getName() + ": " + e.getMessage());
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
     * Checks and updates localization file if necessary
     * @param localeFile localization file
     * @param resourceName resource name in jar
     * @return true if localization was updated
     */
    private boolean checkAndUpdateLocale(File localeFile, String resourceName) {
        try {
            // Get localization name without .yml extension
            String localeName = localeFile.getName();
            if (localeName.endsWith(".yml")) {
                localeName = localeName.substring(0, localeName.length() - 4);
            }
            
            // Use empty exclusion list so all deleted elements are restored
            // This will allow to restore all deleted elements from original files
            List<String> ignoredSections = new ArrayList<>();
            
            // Update localization using the library
            ConfigUpdater.update(plugin, resourceName, localeFile, ignoredSections);
            
            // Reload file after update
            FileConfiguration updatedConfig = YamlConfiguration.loadConfiguration(localeFile);
            locales.put(localeName, updatedConfig);
            
            // If this is the default localization, update it too
            if (localeName.equals(defaultLanguage)) {
                defaultLocale = updatedConfig;
            }
            
            plugin.getPluginLogger().info("Successfully updated locale file: " + localeName);
            return true;
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Error when updating locale file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update localization
     * @param locale localization name without .yml extension
     * @return updated configuration or null in case of error
     */
    public FileConfiguration updateLocale(String locale) {
        if (!locales.containsKey(locale)) {
            return null;
        }
        
        File localeFile = new File(plugin.getDataFolder(), "locales/" + locale + ".yml");
        if (!localeFile.exists()) {
            return null;
        }
        
        FileConfiguration localeConfig = YamlConfiguration.loadConfiguration(localeFile);
        
        // Check and update localization file
        boolean updated = checkAndUpdateLocale(localeFile, "locales/" + locale + ".yml");
        if (updated) {
            // Reload configuration after update
            localeConfig = YamlConfiguration.loadConfiguration(localeFile);
            
            // Update cache
            locales.put(locale, localeConfig);
            
            // If this is the default localization, update it too
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
        
        // Check if list is empty but there's a single value
        if (messages.isEmpty() && localeConfig.contains(key)) {
            // Check if value is a string
            if (localeConfig.isString(key)) {
                String singleMessage = localeConfig.getString(key);
                if (singleMessage != null) {
                    return java.util.Collections.singletonList(ColorUtil.format(singleMessage));
                }
            }
        }
        
        if (messages.isEmpty()) {
            // If messages not found in specified locale, try to find in default locale
            if (localeConfig != defaultLocale && defaultLocale != null) {
                messages = defaultLocale.getStringList(key);
                
                // Check if list is empty but there's a single value in default localization
                if (messages.isEmpty() && defaultLocale.contains(key)) {
                    // Check if value is a string
                    if (defaultLocale.isString(key)) {
                        String singleMessage = defaultLocale.getString(key);
                        if (singleMessage != null) {
                            return java.util.Collections.singletonList(ColorUtil.format(singleMessage));
                        }
                    }
                }
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