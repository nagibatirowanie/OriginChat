package me.nagibatirowanie.originchat.utils;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.ChatColor;

import java.util.logging.Level;

/**
 * Утилитный класс для логирования
 */
public class LoggerUtil {

    private final OriginChat plugin;
    private boolean debug;

    public LoggerUtil(OriginChat plugin) {
        this.plugin = plugin;
        this.debug = false;
        
        // Загрузка настроек отладки из конфига
        if (plugin.getConfig().contains("settings.debug")) {
            this.debug = plugin.getConfig().getBoolean("settings.debug");
        }
    }

    /**
     * Отправить информационное сообщение в консоль
     * @param message сообщение
     */
    public void info(String message) {
        plugin.getLogger().info(formatMessage(message));
    }

    /**
     * Отправить предупреждение в консоль
     * @param message сообщение
     */
    public void warning(String message) {
        plugin.getLogger().warning(formatMessage(message));
    }

    /**
     * Отправить сообщение об ошибке в консоль
     * @param message сообщение
     */
    public void severe(String message) {
        plugin.getLogger().severe(formatMessage(message));
    }

    /**
     * Отправить отладочное сообщение в консоль
     * @param message сообщение
     */
    public void debug(String message) {
        if (debug) {
            plugin.getLogger().log(Level.INFO, ChatColor.YELLOW + "[DEBUG] " + ChatColor.RESET + formatMessage(message));
        }
    }

    /**
     * Форматировать сообщение для логирования
     * @param message сообщение
     * @return отформатированное сообщение
     */
    private String formatMessage(String message) {
        String prefix = "";
        
        // Добавление префикса из конфига, если он есть
        if (plugin.getConfig().contains("settings.prefix")) {
            prefix = ChatUtil.formatColors(plugin.getConfig().getString("settings.prefix")) + " ";
        }
        
        return prefix + message;
    }

    /**
     * Установить режим отладки
     * @param debug включить/выключить отладку
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Проверить, включен ли режим отладки
     * @return true, если режим отладки включен
     */
    public boolean isDebug() {
        return debug;
    }
}