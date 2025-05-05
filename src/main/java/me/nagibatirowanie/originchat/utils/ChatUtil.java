package me.nagibatirowanie.originchat.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with chat
 */
public class ChatUtil {

    // Pattern for HEX colors in &#RRGGBB or #RRGGBB format
    private static final Pattern HEX_PATTERN = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Format text with color code support
     * @param text text to be formatted
     * @return formatted text
     */
    public static String formatColors(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Format text with HEX color support
     * @param text text to be formatted
     * @return formatted text
     */
    public static String formatHexColors(String text) {
        if (text == null) return "";
        
        text = formatColors(text);
        
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(2);
            String replacement = ChatColor.valueOf("#" + hex).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Format text with MiniMessage support
     * @param text text to be formatted
     * @return formatted text
     */
    public static String formatMiniMessage(String text) {
        if (text == null) return "";
        
        Component component = MINI_MESSAGE.deserialize(text);
        
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Remove all color codes from text
     * @param text text to be processed
     * @return text without color codes
     */
    public static String stripColors(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text);
    }

    /**
     * Center text in chat
     * @param text text to be centered
     * @return centered text
     */
    public static String centerText(String text) {
        if (text == null || text.isEmpty()) return "";
        
        int chatWidth = 53;
        
        String cleanText = stripColors(text);
        int textLength = cleanText.length();
        
        if (textLength >= chatWidth) {
            return text;
        }
        
        int spaces = (chatWidth - textLength) / 2;
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < spaces; i++) {
            result.append(" ");
        }
        
        result.append(text);
        
        return result.toString();
    }
}