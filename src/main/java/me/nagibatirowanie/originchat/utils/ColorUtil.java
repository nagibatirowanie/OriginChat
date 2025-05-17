/*
 * This file is part of OriginChat, a Minecraft plugin.
 *
 * Copyright (c) 2025 nagibatirowanie
 *
 * OriginChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin. If not, see <https://www.gnu.org/licenses/>.
 *
 * Created with ❤️ for the Minecraft community.
 */

package me.nagibatirowanie.originchat.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.animation.AnimationManager;
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

    // Updated serializer for proper HEX color handling
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .build();

    private static Boolean placeholderAPIEnabled = null;

    /**
     * Checks if PlaceholderAPI is installed
     * 
     * @return true if PlaceholderAPI is available
     */
    public static boolean isPlaceholderAPIEnabled() {
        if (placeholderAPIEnabled == null) {
            placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return placeholderAPIEnabled;
    }

    /**
     * Replaces PlaceholderAPI placeholders and animations in text
     * 
     * @param player Player for processing placeholders
     * @param text Text to process
     * @param enableAnimations Whether to process animations
     * @return Text with processed placeholders and animations
     */
    public static String setPlaceholders(Player player, String text, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return "";
        
        // Process animations first if enabled
        String processed = text;
        if (enableAnimations) {
            AnimationManager animationManager = OriginChat.getInstance().getAnimationManager();
            if (animationManager != null && processed.contains("{animation_")) {
                processed = animationManager.processAnimations(processed, player);
            }
        }
        
        // Then process PlaceholderAPI placeholders
        if (player != null && isPlaceholderAPIEnabled()) {
            try {
                String prev;
                String result = processed;
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
        return processed;
    }
    
    /**
     * Replaces PlaceholderAPI placeholders and animations in text
     * Animations are enabled by default
     * 
     * @param player Player for processing placeholders
     * @param text Text to process
     * @return Text with processed placeholders and animations
     */
    public static String setPlaceholders(Player player, String text) {
        return setPlaceholders(player, text, true);
    }

    /**
     * Processes HEX colors in &#RRGGBB or #RRGGBB format directly to §x§R§R§G§G§B§B format
     * for proper display in tab and other places that require legacy colors
     * 
     * @param text Text to process
     * @return Text with processed HEX colors
     */
    public static String processHexColorsToLegacy(String text) {
        if (text == null || text.isEmpty()) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group(2);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexColor.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(result, replacement.toString());
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Formats string: &-codes, hex, MiniMessage → MiniMessage → Component → legacy string
     * 
     * @param text Text to format
     * @param enableColors Allow color codes and hex colors formatting
     * @param enableMiniMessage Allow MiniMessage formatting
     * @param enablePlaceholders Allow processing placeholders
     * @param enableAnimations Allow processing animations
     * @return Formatted text
     */
    public static String format(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return "";
        
        // Process placeholders if enabled
        String processed = text;
        if (enablePlaceholders && isPlaceholderAPIEnabled() && text.contains("%")) {
            // Placeholders detected but player not specified
            // Can use any online player for server placeholders
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                Player anyPlayer = Bukkit.getOnlinePlayers().iterator().next();
                processed = setPlaceholders(anyPlayer, text, enableAnimations);
            }
        }
        
        // If formatting is disabled, return text as is or with placeholders only
        if (!enableColors && !enableMiniMessage) {
            return processed;
        }
        
        // If HEX colors processing is allowed but MiniMessage is disabled, process HEX directly
        if (enableColors && !enableMiniMessage && (processed.contains("&#") || processed.contains("#"))) {
            String legacyColored = processed.replace('&', '§');
            return processHexColorsToLegacy(legacyColored);
        }
        
        // Create component considering formatting settings
        Component component = toComponent(processed, enableColors, enableMiniMessage);
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Formats string: &-codes, hex, MiniMessage → MiniMessage → Component → legacy string
     * 
     * @param text Text to format
     * @param enableColors Allow color codes and hex colors formatting
     * @param enableMiniMessage Allow MiniMessage formatting
     * @param enablePlaceholders Allow processing placeholders and animations
     * @return Formatted text
     */
    public static String format(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
        return format(text, enableColors, enableMiniMessage, enablePlaceholders, false); // Animations disabled by default
    }
    
    /**
     * Formats string: &-codes, hex, MiniMessage → MiniMessage → Component → legacy string
     * All formatting types enabled by default, except animations
     * 
     * @param text Text to format
     * @return Formatted text
     */
    public static String format(String text) {
        return format(text, true, true, true, false); // Animations disabled by default
    }

    /**
     * Formats string with placeholders for specific player
     * 
     * @param player Player for processing placeholders
     * @param text Text to format
     * @param enableColors Allow color codes and hex colors formatting
     * @param enableMiniMessage Allow MiniMessage formatting
     * @param enablePlaceholders Allow processing placeholders
     * @param enableAnimations Allow processing animations
     * @return Formatted text
     */
    public static String format(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return "";
        if (player == null) return format(text, enableColors, enableMiniMessage, enablePlaceholders, enableAnimations);
        
        // Process placeholders with player context if enabled
        String processed = enablePlaceholders ? setPlaceholders(player, text, enableAnimations) : text;
        
        // If formatting is disabled, return text as is or with placeholders only
        if (!enableColors && !enableMiniMessage) {
            return processed;
        }
        
        // If HEX colors processing is allowed but MiniMessage is disabled, process HEX directly
        if (enableColors && !enableMiniMessage && (processed.contains("&#") || processed.contains("#"))) {
            String legacyColored = processed.replace('&', '§');
            return processHexColorsToLegacy(legacyColored);
        }
        
        // Create component considering formatting settings
        Component component = toComponent(processed, enableColors, enableMiniMessage);
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Formats string with placeholders for specific player
     * 
     * @param player Player for processing placeholders
     * @param text Text to format
     * @param enableColors Allow color codes and hex colors formatting
     * @param enableMiniMessage Allow MiniMessage formatting
     * @param enablePlaceholders Allow processing placeholders and animations
     * @return Formatted text
     */
    public static String format(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
        return format(player, text, enableColors, enableMiniMessage, enablePlaceholders, false); // Animations disabled by default
    }
    
    /**
     * Formats string with placeholders for specific player
     * All formatting types enabled by default, except animations
     * 
     * @param player Player for processing placeholders
     * @param text Text to format
     * @return Formatted text
     */
    public static String format(Player player, String text) {
        return format(player, text, true, true, true, false); // Animations disabled by default
    }

    /**
     * Removes all color codes from text
     * 
     * @param text Text to strip colors from
     * @return Text without color codes
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) return "";
        return ChatColor.stripColor(text);
    }

    /**
     * Converts string with &-codes and hex colors to MiniMessage string
     * 
     * @param text Text to convert
     * @return Text in MiniMessage format
     */
    public static String convertToMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";

        // First replace all § symbols with &
        text = text.replace('§', '&');

        // HEX colors: &#RRGGBB or #RRGGBB -> <#RRGGBB>
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(2) + ">");
        }
        matcher.appendTail(sb);
        String result = sb.toString();

        // &-codes -> MiniMessage tags
        StringBuilder finalResult = new StringBuilder();
        for (int i = 0; i < result.length(); ) {
            boolean replaced = false;
            for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
                String code = entry.getKey();
                if (result.startsWith(code, i)) {
                    finalResult.append(entry.getValue());
                    i += code.length();
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                finalResult.append(result.charAt(i));
                i++;
            }
        }
        return finalResult.toString();
    }

    /**
     * Creates component from string with support for &-codes, hex and MiniMessage
     * 
     * @param text Text to convert
     * @param enableColors Allow color codes and hex colors formatting
     * @param enableMiniMessage Allow MiniMessage formatting
     * @return Component with applied formatting
     */
    public static Component toComponent(String text, boolean enableColors, boolean enableMiniMessage) {
        if (text == null || text.isEmpty()) return Component.empty();

        // If formatting is disabled, return text as is
        if (!enableColors && !enableMiniMessage) {
            return Component.text(text);
        }
        
        // Convert to MiniMessage if color formatting is allowed
        String processedText = enableColors ? convertToMiniMessage(text) : text;
        
        try {
            // Use MiniMessage only if it's allowed
            if (enableMiniMessage) {
                return MINI_MESSAGE.deserialize(processedText);
            } else {
                // If MiniMessage is disabled but colors are allowed, use only color codes
                return Component.text(processedText);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error creating component via MiniMessage: " + e.getMessage());
            return Component.text(stripColors(text));
        }
    }
    
    /**
     * Creates component from string with support for &-codes, hex and MiniMessage
     * All formatting types enabled by default
     * 
     * @param text Text to convert
     * @return Component with applied formatting
     */
    public static Component toComponent(String text) {
        return toComponent(text, true, true);
    }

    /**
     * Creates component from string with support for player placeholders
     * 
     * @param player Player for processing placeholders
     * @param text Text to convert
     * @param enableColors Allow color codes and hex colors formatting
     * @param enableMiniMessage Allow MiniMessage formatting
     * @param enablePlaceholders Allow processing placeholders
     * @param enableAnimations Allow processing animations
     * @return Component with applied formatting
     */
    public static Component toComponent(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return Component.empty();
        String processed = enablePlaceholders ? setPlaceholders(player, text, enableAnimations) : text;
        return toComponent(processed, enableColors, enableMiniMessage);
    }
    
    /**
     * Creates component from string with support for player placeholders
     * 
     * @param player Player for processing placeholders
     * @param text Text to convert
     * @param enableColors Allow color codes and hex colors formatting
     * @param enableMiniMessage Allow MiniMessage formatting
     * @param enablePlaceholders Allow processing placeholders and animations
     * @return Component with applied formatting
     */
    public static Component toComponent(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
        return toComponent(player, text, enableColors, enableMiniMessage, enablePlaceholders, false); // Animations disabled by default
    }
    
    /**
     * Creates component from string with support for player placeholders
     * All formatting types enabled by default, except animations
     * 
     * @param player Player for processing placeholders
     * @param text Text to convert
     * @return Component with applied formatting
     */
    public static Component toComponent(Player player, String text) {
        return toComponent(player, text, true, true, true, false); // Animations disabled by default
    }
}