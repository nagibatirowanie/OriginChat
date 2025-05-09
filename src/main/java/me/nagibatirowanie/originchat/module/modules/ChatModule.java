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

package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A module for chat processing with multiple chat support and kulldowns
 */
public class ChatModule extends AbstractModule implements Listener {

    private final Map<String, ChatConfig> chatConfigs = new HashMap<>();
    private boolean hexColors;
    private boolean miniMessage;
    private int maxMessageLength;
    private boolean enabled;

    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private int defaultCooldown = 3;
    private boolean cooldownEnabled = true;

    private String msgNoPermission;
    private String msgNobodyHeard;
    private String msgChatNotFound;
    private String msgCooldown;

    public ChatModule(OriginChat plugin) {
        super(plugin, "chat", "Chat Module", "Adds chat distribution and formatting", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/chat");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        loadConfig();
        if (!enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    /**
     * Load settings from the config
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            hexColors = config.getBoolean("hex-colors", true);
            miniMessage = config.getBoolean("mini-message", true);
            maxMessageLength = config.getInt("max-message-length", 256);

            // Loading messages for players
            ConfigurationSection msgSection = config.getConfigurationSection("messages");
            if (msgSection != null) {
                msgNoPermission = msgSection.getString("no-permission", msgNoPermission);
                msgNobodyHeard = msgSection.getString("nobody-heard", msgNobodyHeard);
                msgChatNotFound = msgSection.getString("chat-not-found", msgChatNotFound);
                msgCooldown = msgSection.getString("cooldown", msgCooldown);
            }

            // Load Kuldown
            ConfigurationSection cooldownSection = config.getConfigurationSection("cooldown");
            cooldowns.clear();
            if (cooldownSection != null) {
                for (String key : cooldownSection.getKeys(false)) {
                    if (key.equalsIgnoreCase("enabled")) {
                        cooldownEnabled = cooldownSection.getBoolean("enabled", true);
                    } else if (key.equalsIgnoreCase("default")) {
                        defaultCooldown = cooldownSection.getInt("default", 3);
                    } else {
                        cooldowns.put(key, cooldownSection.getInt(key, defaultCooldown));
                    }
                }
            } else {
                defaultCooldown = config.getInt("cooldown.default", 3);
                cooldowns.put("originchat.moder", config.getInt("cooldown.originchat.moder", 2));
                cooldowns.put("originchat.admin", config.getInt("cooldown.originchat.admin", 0));
            }
            loadChatConfigs();
        } catch (Exception e) {
            log("❗ Error when loading chat configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void loadChatConfigs() {
        chatConfigs.clear();
        ConfigurationSection chatsSection = config.getConfigurationSection("chats");
        if (chatsSection != null) {
            for (String chatName : chatsSection.getKeys(false)) {
                ConfigurationSection chatSection = chatsSection.getConfigurationSection(chatName);
                if (chatSection != null) {
                    ChatConfig chatConfig = new ChatConfig(
                            chatSection.getString("prefix", ""),
                            chatSection.getInt("radius", -1),
                            chatSection.getString("format", "#f0f0f0[{chat}] {player}: {message}"),
                            chatSection.getString("permission-write", ""),
                            chatSection.getString("permission-view", "")
                    );
                    chatConfigs.put(chatName, chatConfig);
                    debug("Загружен чат: " + chatName + ", префикс: " + chatConfig.getPrefix());
                }
            }
        }

        if (chatConfigs.isEmpty()) {
            ChatConfig defaultChat = new ChatConfig(
                    "", -1, "<gray>[{player}]</gray> <white>{message}</white>", "", ""
            );
            chatConfigs.put("global", defaultChat);
            debug("Добавлен чат по умолчанию, так как не найдено настроенных чатов.");
        }
    }


    private int getPlayerCooldown(Player player) {
        if (player.hasPermission("originchat.admin")) {
            return 0;
        }
        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return defaultCooldown;
    }

    /**
     * Chat message handler
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.length() > maxMessageLength) {
            message = message.substring(0, maxMessageLength);
        }

        if (cooldownEnabled) {
            int cooldown = getPlayerCooldown(player);
            if (cooldown > 0) {
                long now = System.currentTimeMillis();
                long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
                long diff = (now - last) / 1000;
                if (diff < cooldown) {
                    String msg = msgCooldown.replace("{cooldown}", String.valueOf(cooldown - diff));
                    player.sendMessage(formatMessage(msg));
                    event.setCancelled(true);
                    return;
                }
                lastMessageTime.put(player.getUniqueId(), now);
            }
        }

        event.setCancelled(true);

        for (Map.Entry<String, ChatConfig> entry : chatConfigs.entrySet()) {
            String chatName = entry.getKey();
            ChatConfig chatConfig = entry.getValue();

            if (message.startsWith(chatConfig.getPrefix())) {
                if (!chatConfig.getPrefix().isEmpty()) {
                    message = message.substring(chatConfig.getPrefix().length());
                }

                if (!chatConfig.getPermissionWrite().isEmpty() && !player.hasPermission(chatConfig.getPermissionWrite())) {
                    player.sendMessage(formatMessage(msgNoPermission));
                    return;
                }
                String formattedMessage = formatChatMessage(player, message, chatConfig, chatName);

                if (chatConfig.getRadius() > 0) {
                    boolean heard = false;
                    for (Player target : player.getWorld().getPlayers()) {
                        if (target.equals(player)) continue;

                        if (target.getLocation().distance(player.getLocation()) <= chatConfig.getRadius() &&
                                (chatConfig.getPermissionView().isEmpty() || target.hasPermission(chatConfig.getPermissionView()))) {
                            target.sendMessage(formattedMessage);
                            heard = true;
                        }
                    }
                    if (!heard) {
                        player.sendMessage(formatMessage(msgNobodyHeard));
                    }
                    player.sendMessage(formattedMessage);
                } else {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> chatConfig.getPermissionView().isEmpty() || p.hasPermission(chatConfig.getPermissionView()))
                            .forEach(p -> p.sendMessage(formattedMessage));
                }
                return;
            }
        }
        player.sendMessage(formatMessage(msgChatNotFound));
    }


    private String formatChatMessage(Player player, String message, ChatConfig config, String chatName) {
        String format = config.getFormat();
        
        // Заменяем основные плейсхолдеры
        format = format.replace("{player}", player.getName());
        format = format.replace("{message}", message);
        format = format.replace("{chat}", chatName);
        format = format.replace("{world}", player.getWorld().getName());
        
        // Используем метод format с передачей игрока для обработки PlaceholderAPI
        return ColorUtil.format(player, format);
    }
    
    /**
     * Форматируем сообщение с учетом плейсхолдеров
     */
    private String formatMessage(String message) {
        return ColorUtil.format(message);
    }
    
    /**
     * Форматируем сообщение игроку с учетом плейсхолдеров
     */
    private String formatMessage(Player player, String message) {
        return ColorUtil.format(player, message);
    }

    private static class ChatConfig {
        private final String prefix;
        private final int radius;
        private final String format;
        private final String permissionWrite;
        private final String permissionView;

        public ChatConfig(String prefix, int radius, String format, String permissionWrite, String permissionView) {
            this.prefix = prefix;
            this.radius = radius;
            this.format = format;
            this.permissionWrite = permissionWrite;
            this.permissionView = permissionView;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getRadius() {
            return radius;
        }

        public String getFormat() {
            return format;
        }

        public String getPermissionWrite() {
            return permissionWrite;
        }

        public String getPermissionView() {
            return permissionView;
        }
    }
}