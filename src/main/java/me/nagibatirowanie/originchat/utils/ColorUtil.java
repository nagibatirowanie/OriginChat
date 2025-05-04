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
 * Утилитный класс для форматирования текста с использованием Kyori Adventure API
 * Поддерживает стандартные цветовые коды Minecraft, HEX-цвета и MiniMessage
 */
public class ColorUtil {

    // Паттерн для HEX цветов в формате &#RRGGBB или #RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
    
    // MiniMessage парсер для обработки тегов
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
        
        // Создаем компонент с помощью toComponent и затем сериализуем его обратно в строку
        // Это гарантирует согласованность между format и toComponent
        Component component = toComponent(text);
        
        // Используем LEGACY_SERIALIZER для обратной сериализации, но без секционных символов
        // Это предотвратит предупреждения о устаревших форматирующих кодах
        return LEGACY_SERIALIZER.serialize(component);
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
        String processed = setPlaceholders(player, text);
        // Затем используем основной метод format
        return format(processed);
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
            
            // Используем LEGACY_SERIALIZER для обратной сериализации
            // Это предотвратит предупреждения о устаревших форматирующих кодах
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
        
        // Проверяем, содержит ли текст MiniMessage теги
        if (text.contains("<") && text.contains(">")) {
            try {
                // Если есть MiniMessage теги, используем MiniMessage напрямую
                return MINI_MESSAGE.deserialize(convertToMiniMessage(text));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка при создании компонента через MiniMessage: " + e.getMessage());
                // В случае ошибки продолжаем обработку стандартными методами
            }
        }
        
        // Если нет MiniMessage тегов или произошла ошибка, используем стандартный подход
        // Но сначала преобразуем все & коды в MiniMessage формат
        String miniMessageText = text;
        if (miniMessageText.contains("&") || miniMessageText.contains("#")) {
            miniMessageText = convertToMiniMessage(miniMessageText);
            return MINI_MESSAGE.deserialize(miniMessageText);
        }
        
        // Если нет ни & кодов, ни # цветов, просто создаем текстовый компонент
        return Component.text(text);
    }
    
    /**
     * Создать компонент из текста с поддержкой всех типов форматирования и плейсхолдеров
     * @param player игрок, для которого заменяются плейсхолдеры
     * @param text текст для форматирования
     * @return компонент Adventure API
     */
    public static Component toComponent(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        // Сначала заменяем плейсхолдеры
        String processed = setPlaceholders(player, text);
        
        // Затем создаем компонент с помощью обновленного метода
        return toComponent(processed);
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
    

    /**
 * Конвертировать стандартные цветовые коды в формат MiniMessage
 * @param text текст с цветовыми кодами
 * @return текст с тегами MiniMessage
 */
public static String convertToMiniMessage(String text) {
    if (text == null || text.isEmpty()) return "";
    
    String result = text;
    for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
        result = result.replace(entry.getKey(), entry.getValue());
    }
    
    // Конвертация HEX цветов в формат MiniMessage
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