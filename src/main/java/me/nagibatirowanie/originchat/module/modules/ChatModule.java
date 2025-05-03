package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Модуль для обработки чата
 */
public class ChatModule extends AbstractModule implements Listener {

    private String chatFormat;
    private boolean hexColors;
    private boolean miniMessage;
    private int maxMessageLength;

    public ChatModule(OriginChat plugin) {
        super(plugin, "chat", "Модуль чата", "Обрабатывает сообщения в чате и применяет форматирование", "1.0");
    }

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        config = plugin.getConfigManager().getMainConfig();
        loadConfig();
        
        // Регистрация слушателя событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        plugin.getPluginLogger().info("Модуль чата включен. Формат: " + chatFormat);
    }

    @Override
    public void onDisable() {
        // Отмена регистрации слушателя событий
        HandlerList.unregisterAll(this);
        
        plugin.getPluginLogger().info("Модуль чата выключен.");
    }

    /**
     * Загрузить настройки из конфига
     */
    private void loadConfig() {
        chatFormat = config.getString("chat.format", "<gray>[%player_name%]</gray> <white>%message%</white>");
        hexColors = config.getBoolean("chat.hex-colors", true);
        miniMessage = config.getBoolean("chat.mini-message", true);
        maxMessageLength = config.getInt("chat.max-message-length", 256);
    }

    /**
     * Обработчик сообщений в чате
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Проверка длины сообщения
        if (message.length() > maxMessageLength) {
            message = message.substring(0, maxMessageLength);
        }
        
        // Форматирование сообщения
        String formattedMessage = formatMessage(player, message);
        
        // Установка нового формата сообщения
        event.setFormat(formattedMessage);
    }

    /**
     * Форматировать сообщение игрока
     * @param player игрок
     * @param message сообщение
     * @return отформатированное сообщение
     */
    private String formatMessage(Player player, String message) {
        String format = chatFormat;
        
        // Замена плейсхолдеров
        format = format.replace("%player_name%", player.getName());
        format = format.replace("%message%", message);
        
        // Применение форматирования
        if (miniMessage) {
            format = ChatUtil.formatMiniMessage(format);
        }
        
        if (hexColors) {
            format = ChatUtil.formatHexColors(format);
        }
        
        return format;
    }


}