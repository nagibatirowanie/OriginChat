package me.nagibatirowanie.originchat.utils;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.ChatColor;

import java.util.logging.Level;

/**
 * Utility class for logging
 */
public class LoggerUtil {

    private final OriginChat plugin;
    private boolean debug;

    public LoggerUtil(OriginChat plugin) {
        this.plugin = plugin;
        this.debug = false;
        
        if (plugin.getConfig().contains("settings.debug")) {
            this.debug = plugin.getConfig().getBoolean("settings.debug");
        }
    }

    /**
     * Send an informational message to the console
     * @param message message
     */
    public void info(String message) {
        plugin.getLogger().info(formatMessage(message));
    }

    /**
     * Send a warning message to the console
     * @param message message
     */
    public void warning(String message) {
        plugin.getLogger().warning(formatMessage(message));
    }

    /**
     * Send an error message to the console
     * @param message message
     */
    public void severe(String message) {
        plugin.getLogger().severe(formatMessage(message));
    }

    /**
     * Send debug message to console
     * @param message message
     */
    public void debug(String message) {
        if (debug) {
            plugin.getLogger().log(Level.INFO, ChatColor.YELLOW + "[DEBUG] " + ChatColor.RESET + formatMessage(message));
        }
    }

    /**
     * Format message for logging
     * @param message message
     * @return formatted message
     */
    private String formatMessage(String message) {
        String prefix = "";
        
        if (plugin.getConfig().contains("settings.prefix")) {
            prefix = ColorUtil.format(plugin.getConfig().getString("settings.prefix")) + " ";
        }
        
        return prefix + message;
    }

    /**
     * Set debug mode
     * @param debug enable/disable debugging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Check if debug mode is enabled
     * @return true if debug mode is enabled
     */
    public boolean isDebug() {
        return debug;
    }
}