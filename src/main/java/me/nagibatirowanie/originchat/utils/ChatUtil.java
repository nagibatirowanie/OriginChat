package me.nagibatirowanie.originchat.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилитный класс для работы с чатом
 */
public class ChatUtil {

    // Паттерн для HEX цветов в формате &#RRGGBB или #RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
    
    // MiniMessage парсер
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    // Legacy сериализатор для конвертации компонентов в строки
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Форматировать текст с поддержкой цветовых кодов
     * @param text текст для форматирования
     * @return отформатированный текст
     */
    public static String formatColors(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Форматировать текст с поддержкой HEX цветов
     * @param text текст для форматирования
     * @return отформатированный текст
     */
    public static String formatHexColors(String text) {
        if (text == null) return "";
        
        // Сначала обрабатываем обычные цветовые коды
        text = formatColors(text);
        
        // Затем обрабатываем HEX цвета
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
     * Форматировать текст с поддержкой MiniMessage
     * @param text текст для форматирования
     * @return отформатированный текст
     */
    public static String formatMiniMessage(String text) {
        if (text == null) return "";
        
        // Парсим текст с помощью MiniMessage
        Component component = MINI_MESSAGE.deserialize(text);
        
        // Конвертируем компонент обратно в строку с поддержкой цветов
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Удалить все цветовые коды из текста
     * @param text текст для обработки
     * @return текст без цветовых кодов
     */
    public static String stripColors(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text);
    }

    /**
     * Центрировать текст в чате
     * @param text текст для центрирования
     * @return центрированный текст
     */
    public static String centerText(String text) {
        if (text == null || text.isEmpty()) return "";
        
        // Ширина чата в символах (по умолчанию 53)
        int chatWidth = 53;
        
        // Удаляем цветовые коды для подсчета длины
        String cleanText = stripColors(text);
        int textLength = cleanText.length();
        
        // Если текст уже шире чата, возвращаем его без изменений
        if (textLength >= chatWidth) {
            return text;
        }
        
        // Вычисляем количество пробелов для центрирования
        int spaces = (chatWidth - textLength) / 2;
        StringBuilder result = new StringBuilder();
        
        // Добавляем пробелы перед текстом
        for (int i = 0; i < spaces; i++) {
            result.append(" ");
        }
        
        // Добавляем сам текст
        result.append(text);
        
        return result.toString();
    }
}