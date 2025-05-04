package me.nagibatirowanie.originchat.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилитный класс для форматирования текста с использованием Kyori Adventure API
 * Поддерживает стандартные цветовые коды Minecraft, HEX-цвета и MiniMessage
 */
public class ColorUtil {

    // Паттерн для HEX цветов в формате &#RRGGBB или #RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
    
    // MiniMessage парсер для обработки тегов
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    // Legacy сериализатор для конвертации компонентов в строки с поддержкой цветов
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Проверка наличия PlaceholderAPI
    private static Boolean placeholderAPIEnabled = null;
    
    /**
     * Проверить, установлен ли PlaceholderAPI
     * @return true, если PlaceholderAPI установлен
     */
    public static boolean isPlaceholderAPIEnabled() {
        if (placeholderAPIEnabled == null) {
            placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return placeholderAPIEnabled;
    }
    
    /**
     * Заменить плейсхолдеры PlaceholderAPI в тексте
     * @param player игрок, для которого заменяются плейсхолдеры
     * @param text текст с плейсхолдерами
     * @return текст с замененными плейсхолдерами или исходный текст, если замена невозможна
     */
    public static String setPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return "";
        if (player != null && isPlaceholderAPIEnabled()) {
            try {
                String prev;
                String result = text;
                int maxIterations = 10; // ограничение на количество итераций
                int count = 0;
                do {
                    prev = result;
                    result = PlaceholderAPI.setPlaceholders(player, prev);
                    count++;
                } while (!result.equals(prev) && count < maxIterations);
                return result;
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка при обработке плейсхолдеров: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return text;
    }
    
    /**
     * Форматировать текст с поддержкой всех типов форматирования
     * @param text текст для форматирования
     * @return отформатированный текст
     */
    public static String format(String text) {
        if (text == null || text.isEmpty()) return "";
        // Сначала обрабатываем MiniMessage теги
        if (text.contains("<") && text.contains(">")) {
            text = formatMiniMessage(text);
        }
        // Затем HEX цвета
        if (text.contains("#")) {
            text = formatHexColors(text);
        }
        // Затем стандартные цветовые коды
        text = formatLegacyColors(text);
        return text;
    }
    
    /**
     * Форматировать текст с поддержкой всех типов форматирования и заменой плейсхолдеров
     * @param player игрок, для которого заменяются плейсхолдеры
     * @param text текст для форматирования
     * @return отформатированный текст с замененными плейсхолдерами
     */
    public static String format(Player player, String text) {
        if (text == null || text.isEmpty()) return "";
        // Сначала заменяем плейсхолдеры
        text = setPlaceholders(player, text);
        // Затем MiniMessage
        if (text.contains("<") && text.contains(">")) {
            text = formatMiniMessage(text);
        }
        // Затем HEX и стандартные цветовые коды
        if (text.contains("&") || text.contains("#")) {
            String colorCodes = text;
            colorCodes = formatHexColors(colorCodes);
            colorCodes = formatLegacyColors(colorCodes);
            text = colorCodes;
        }
        return text;
    }

    /**
     * Форматировать текст с поддержкой стандартных цветовых кодов Minecraft (&a, &b, и т.д.)
     * @param text текст для форматирования
     * @return отформатированный текст
     */
    public static String formatLegacyColors(String text) {
        if (text == null || text.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Форматировать текст с поддержкой HEX цветов (#RRGGBB или &#RRGGBB)
     * @param text текст для форматирования
     * @return отформатированный текст
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
     * Форматировать текст с поддержкой MiniMessage тегов (<bold>, <color:#RRGGBB>, и т.д.)
     * @param text текст для форматирования
     * @return отформатированный текст
     */
    public static String formatMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";
        
        try {
            // Парсим текст с помощью MiniMessage
            Component component = MINI_MESSAGE.deserialize(text);
            
            // Конвертируем компонент обратно в строку с поддержкой цветов
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка при обработке MiniMessage: " + e.getMessage());
            return text; // В случае ошибки возвращаем исходный текст
        }
    }

    /**
     * Создать компонент из текста с поддержкой всех типов форматирования
     * @param text текст для форматирования
     * @return компонент Adventure API
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        // Сначала применяем все форматирование
        String formatted = format(text);
        
        // Затем конвертируем в компонент
        return LegacyComponentSerializer.legacySection().deserialize(formatted);
    }
    
    /**
     * Создать компонент из текста с поддержкой всех типов форматирования и плейсхолдеров
     * @param player игрок, для которого заменяются плейсхолдеры
     * @param text текст для форматирования
     * @return компонент Adventure API
     */
    public static Component toComponent(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        // Сначала форматируем текст с заменой плейсхолдеров
        String formatted = format(player, text);
        
        // Затем конвертируем в компонент
        return LegacyComponentSerializer.legacySection().deserialize(formatted);
    }

    /**
     * Удалить все цветовые коды из текста
     * @param text текст для обработки
     * @return текст без цветовых кодов
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) return "";
        return ChatColor.stripColor(text);
    }
}