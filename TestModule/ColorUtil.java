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

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static Boolean placeholderAPIEnabled = null;

    /**
     * Проверяет, установлен ли PlaceholderAPI
     */
    public static boolean isPlaceholderAPIEnabled() {
        if (placeholderAPIEnabled == null) {
            placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return placeholderAPIEnabled;
    }

    /**
     * Заменяет PlaceholderAPI плейсхолдеры и анимации в тексте
     * 
     * @param player Игрок для обработки плейсхолдеров
     * @param text Текст для обработки
     * @param enableAnimations Разрешить обработку анимаций
     * @return Текст с обработанными плейсхолдерами и анимациями
     */
    public static String setPlaceholders(Player player, String text, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return "";
        
        // Сначала обрабатываем анимации, если они разрешены
        String processed = text;
        if (enableAnimations) {
            AnimationManager animationManager = OriginChat.getInstance().getAnimationManager();
            if (animationManager != null && processed.contains("{animation_")) {
                processed = animationManager.processAnimations(processed, player);
            }
        }
        
        // Затем обрабатываем PlaceholderAPI плейсхолдеры
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
     * Заменяет PlaceholderAPI плейсхолдеры и анимации в тексте
     * Анимации включены по умолчанию
     */
    public static String setPlaceholders(Player player, String text) {
        return setPlaceholders(player, text, true);
    }

    /**
     * Форматирует строку: &-коды, hex, MiniMessage → MiniMessage → Component → legacy string
     * 
     * @param text Текст для форматирования
     * @param enableColors Разрешить форматирование цветовых кодов и hex-цветов
     * @param enableMiniMessage Разрешить форматирование MiniMessage
     * @param enablePlaceholders Разрешить обработку плейсхолдеров
     * @param enableAnimations Разрешить обработку анимаций
     * @return Отформатированный текст
     */
    public static String format(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return "";
        
        // Обработка плейсхолдеров, если они разрешены
        String processed = text;
        if (enablePlaceholders && isPlaceholderAPIEnabled() && text.contains("%")) {
            // Плейсхолдеры обнаружены, но игрок не указан
            // Можно использовать любого онлайн-игрока для серверных плейсхолдеров
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                Player anyPlayer = Bukkit.getOnlinePlayers().iterator().next();
                processed = setPlaceholders(anyPlayer, text, enableAnimations);
            }
        }
        
        // Если форматирование отключено, возвращаем текст как есть или только с плейсхолдерами
        if (!enableColors && !enableMiniMessage) {
            return processed;
        }
        
        // Создаем компонент с учетом настроек форматирования
        Component component = toComponent(processed, enableColors, enableMiniMessage);
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Форматирует строку: &-коды, hex, MiniMessage → MiniMessage → Component → legacy string
     * 
     * @param text Текст для форматирования
     * @param enableColors Разрешить форматирование цветовых кодов и hex-цветов
     * @param enableMiniMessage Разрешить форматирование MiniMessage
     * @param enablePlaceholders Разрешить обработку плейсхолдеров и анимаций
     * @return Отформатированный текст
     */
    public static String format(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
        return format(text, enableColors, enableMiniMessage, enablePlaceholders, false); // Анимации отключены по умолчанию
    }
    
    /**
     * Форматирует строку: &-коды, hex, MiniMessage → MiniMessage → Component → legacy string
     * Все типы форматирования включены по умолчанию, кроме анимаций
     */
    public static String format(String text) {
        return format(text, true, true, true, false); // Анимации отключены по умолчанию
    }

    /**
     * Форматирует строку с плейсхолдерами для конкретного игрока
     * 
     * @param player Игрок для обработки плейсхолдеров
     * @param text Текст для форматирования
     * @param enableColors Разрешить форматирование цветовых кодов и hex-цветов
     * @param enableMiniMessage Разрешить форматирование MiniMessage
     * @param enablePlaceholders Разрешить обработку плейсхолдеров
     * @param enableAnimations Разрешить обработку анимаций
     * @return Отформатированный текст
     */
    public static String format(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return "";
        if (player == null) return format(text, enableColors, enableMiniMessage, enablePlaceholders, enableAnimations);
        
        // Обработка плейсхолдеров с привязкой к игроку, если они разрешены
        String processed = enablePlaceholders ? setPlaceholders(player, text, enableAnimations) : text;
        
        // Если форматирование отключено, возвращаем текст как есть или только с плейсхолдерами
        if (!enableColors && !enableMiniMessage) {
            return processed;
        }
        
        // Создаем компонент с учетом настроек форматирования
        Component component = toComponent(processed, enableColors, enableMiniMessage);
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Форматирует строку с плейсхолдерами для конкретного игрока
     * 
     * @param player Игрок для обработки плейсхолдеров
     * @param text Текст для форматирования
     * @param enableColors Разрешить форматирование цветовых кодов и hex-цветов
     * @param enableMiniMessage Разрешить форматирование MiniMessage
     * @param enablePlaceholders Разрешить обработку плейсхолдеров и анимаций
     * @return Отформатированный текст
     */
    public static String format(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
        return format(player, text, enableColors, enableMiniMessage, enablePlaceholders, false); // Анимации отключены по умолчанию
    }
    
    /**
     * Форматирует строку с плейсхолдерами для конкретного игрока
     * Все типы форматирования включены по умолчанию, кроме анимаций
     */
    public static String format(Player player, String text) {
        return format(player, text, true, true, true, false); // Анимации отключены по умолчанию
    }

    /**
     * Удаляет все цветовые коды из текста
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) return "";
        return ChatColor.stripColor(text);
    }

    /**
     * Преобразует строку с &-кодами и hex-цветами в MiniMessage-строку
     */
    public static String convertToMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";

        // Сначала заменяем все символы § на &
        text = text.replace('§', '&');

        // HEX цвета: &#RRGGBB или #RRGGBB -> <#RRGGBB>
        String result = HEX_PATTERN.matcher(text).replaceAll(match -> "<color:#" + match.group(2) + ">");

        // &-коды -> MiniMessage теги
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.length(); ) {
            boolean replaced = false;
            for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
                String code = entry.getKey();
                if (result.startsWith(code, i)) {
                    sb.append(entry.getValue());
                    i += code.length();
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                sb.append(result.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Создаёт компонент из строки с поддержкой &-кодов, hex и MiniMessage
     * 
     * @param text Текст для преобразования
     * @param enableColors Разрешить форматирование цветовых кодов и hex-цветов
     * @param enableMiniMessage Разрешить форматирование MiniMessage
     * @return Компонент с примененным форматированием
     */
    public static Component toComponent(String text, boolean enableColors, boolean enableMiniMessage) {
        if (text == null || text.isEmpty()) return Component.empty();

        // Если форматирование отключено, возвращаем текст как есть
        if (!enableColors && !enableMiniMessage) {
            return Component.text(text);
        }
        
        // Конвертируем в MiniMessage, если разрешено форматирование цветов
        String processedText = enableColors ? convertToMiniMessage(text) : text;
        
        try {
            // Используем MiniMessage только если он разрешен
            if (enableMiniMessage) {
                return MINI_MESSAGE.deserialize(processedText);
            } else {
                // Если MiniMessage отключен, но цвета разрешены, используем только цветовые коды
                return Component.text(processedText);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка при создании компонента через MiniMessage: " + e.getMessage());
            return Component.text(stripColors(text));
        }
    }
    
    /**
     * Создаёт компонент из строки с поддержкой &-кодов, hex и MiniMessage
     * Все типы форматирования включены по умолчанию
     */
    public static Component toComponent(String text) {
        return toComponent(text, true, true);
    }

    /**
     * Создаёт компонент из строки с поддержкой плейсхолдеров для игрока
     * 
     * @param player Игрок для обработки плейсхолдеров
     * @param text Текст для преобразования
     * @param enableColors Разрешить форматирование цветовых кодов и hex-цветов
     * @param enableMiniMessage Разрешить форматирование MiniMessage
     * @param enablePlaceholders Разрешить обработку плейсхолдеров
     * @param enableAnimations Разрешить обработку анимаций
     * @return Компонент с примененным форматированием
     */
    public static Component toComponent(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
        if (text == null || text.isEmpty()) return Component.empty();
        String processed = enablePlaceholders ? setPlaceholders(player, text, enableAnimations) : text;
        return toComponent(processed, enableColors, enableMiniMessage);
    }
    
    /**
     * Создаёт компонент из строки с поддержкой плейсхолдеров для игрока
     * 
     * @param player Игрок для обработки плейсхолдеров
     * @param text Текст для преобразования
     * @param enableColors Разрешить форматирование цветовых кодов и hex-цветов
     * @param enableMiniMessage Разрешить форматирование MiniMessage
     * @param enablePlaceholders Разрешить обработку плейсхолдеров и анимаций
     * @return Компонент с примененным форматированием
     */
    public static Component toComponent(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
        return toComponent(player, text, enableColors, enableMiniMessage, enablePlaceholders, false); // Анимации отключены по умолчанию
    }
    
    /**
     * Создаёт компонент из строки с поддержкой плейсхолдеров для игрока
     * Все типы форматирования включены по умолчанию, кроме анимаций
     */
    public static Component toComponent(Player player, String text) {
        return toComponent(player, text, true, true, true, false); // Анимации отключены по умолчанию
    }
}