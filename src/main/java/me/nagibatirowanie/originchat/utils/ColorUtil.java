package me.nagibatirowanie.originchat.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for text formatting using Kyori Adventure API
 * Supports standard Minecraft color codes, HEX-colors and MiniMessage
 */
public class ColorUtil {

    // Pattern for HEX colors in &#RRGGBB or #RRGGBB format
    private static final Pattern HEX_PATTERN = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private static final Map<String, String> COLOR_MAP = new HashMap<String, String>() {{
        put("&0", "<black>");
        put("&1", "<dark_blue>");
        put("&2", "<dark_green>");
        put("&3", "<dark_aqua>");
        put("&4", "<dark_red>");
        put("&5", "<dark_purple>");
        put("&6", "<gold>");
        put("&7", "<gray>");
        put("&8", "<dark_gray>");
        put("&9", "<blue>");
        put("&a", "<green>");
        put("&b", "<aqua>");
        put("&c", "<red>");
        put("&d", "<light_purple>");
        put("&e", "<yellow>");
        put("&f", "<white>");
        put("&l", "<bold>");
        put("&m", "<strikethrough>");
        put("&n", "<underlined>");
        put("&o", "<italic>");
        put("&k", "<obfuscated>");
        put("&r", "<reset>");
    }};

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static Boolean placeholderAPIEnabled = null;
    
    /**
     * Check if PlaceholderAPI is installed
     * @return true if PlaceholderAPI is installed
     */
    public static boolean isPlaceholderAPIEnabled() {
        if (placeholderAPIEnabled == null) {
            placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return placeholderAPIEnabled;
    }
    
    /**
     * Replace PlaceholderAPI placeholders in the text
     * @param player player for which placeholders are replaced
     * @param text text with placeholders
     * @return text with replaced placeholders or original text, if replacement is impossible
     */
    public static String setPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return "";
        if (player != null && isPlaceholderAPIEnabled()) {
            try {
                String prev;
                String result = text;
                int maxIterations = 10;
                int count = 0;
                do {
                    prev = result;
                    result = PlaceholderAPI.setPlaceholders(player, prev);
                    count++;
                } while (!result.equals(prev) && count < maxIterations);
                return result;
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error in placeholder processing: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return text;
    }
    
    /**
     * Format text with support for all formatting types
     * @param text text to be formatted
     * @return formatted text
     */

    public static String format(String text) {
        if (text == null || text.isEmpty()) return "";
        
        Component component = toComponent(text);
        
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Format text with support for all formatting types and placeholders replacement
     * @param player player for which placeholders are replaced
     * @param text text to be formatted
     * @return formatted text with replaced placeholders
     */
    public static String format(Player player, String text) {
        if (text == null || text.isEmpty()) return "";
        String processed = setPlaceholders(player, text);
        return format(processed);
    }

    /**
     * Format text with support for standard Minecraft color codes (&a, &b, etc.)
     * @param text text to format
     * @return formatted text
     */
    public static String formatLegacyColors(String text) {
        if (text == null || text.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Format text with HEX color support (#RRRGGBB or &#RRRGGBB)
     * @param text text to be formatted
     * @return formatted text
     */
    public static String formatHexColors(String text) {
        if (text == null || text.isEmpty()) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(2);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hex).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Format text with MiniMessage tag support (<bold>, <color:#RRGGBB>, etc.)
     * @param text text to format
     * @return formatted text
     */
    public static String formatMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";
        
        try {
            Component component = MINI_MESSAGE.deserialize(text);
            
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка при обработке MiniMessage: " + e.getMessage());
            return text;
        }
    }

    /**
     * Create a component from text with support for all formatting types
     * @param text text for formatting
     * @return Adventure API component
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(convertToMiniMessage(text));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка при создании компонента через MiniMessage: " + e.getMessage());
            }
        }
        
        String miniMessageText = text;
        if (miniMessageText.contains("&") || miniMessageText.contains("#")) {
            miniMessageText = convertToMiniMessage(miniMessageText);
            return MINI_MESSAGE.deserialize(miniMessageText);
        }
        
        return Component.text(text);
    }
    
    /**
     * Create a component from text with support for all types of formatting and placeholders
     * @param player player for which placeholders are replaced
     * @param text text for formatting
     * @return Adventure API component
     */
    public static Component toComponent(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        String processed = setPlaceholders(player, text);
        
        return toComponent(processed);
    }

    /**
     * Remove all color codes from text
     * @param text text to be processed
     * @return text without color codes
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) return "";
        return ChatColor.stripColor(text);
    }
    

    /**
     * Convert standard color codes to MiniMessage format
     * @param text text with color codes
     * @return text with MiniMessage tags
     */
    public static String convertToMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";
        
        String result = text;
        for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        Matcher matcher = HEX_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(2);
            matcher.appendReplacement(buffer, "<color:#" + hex + ">");
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
}