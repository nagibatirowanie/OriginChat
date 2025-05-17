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


package me.nagibatirowanie.originchat.module.modules.servermessages;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Random;

/**
 * Module for managing player join/leave messages
 */
public class ServerMessagesModule extends AbstractModule implements Listener {

    private boolean joinMessageEnabled;
    private boolean leaveMessageEnabled;
    private boolean personalWelcomeEnabled;
    private List<String> joinMessages;
    private List<String> leaveMessages;
    private List<String> personalWelcomeMessages;
    
    // Подмодуль для обработки сообщений о выдаче прав оператора
    private OpMessagesSubmodule opMessagesSubmodule;

    public ServerMessagesModule(OriginChat plugin) {
        super(plugin, "server_messages", "Server Messages", "Manage player join/leave messages", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/server_messages");
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Инициализация подмодуля сообщений о выдаче прав оператора
        opMessagesSubmodule = new OpMessagesSubmodule(plugin, this);
        opMessagesSubmodule.loadConfig();
        opMessagesSubmodule.initialize();
        
        log("Server messages module loaded.");
    }

    @Override
    public void onDisable() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        
        // Отключение подмодуля сообщений о выдаче прав оператора
        if (opMessagesSubmodule != null) {
            opMessagesSubmodule.shutdown();
        }
        
        log("Server messages module disabled.");
    }

    // Load settings from config
    protected void loadConfig() {
        try {
            joinMessageEnabled = config.getBoolean("join_message_enabled", true);
            leaveMessageEnabled = config.getBoolean("leave_message_enabled", true);
            personalWelcomeEnabled = config.getBoolean("personal_welcome_enabled", true);
            
            // Загружаем сообщения из конфигурации только как резервные варианты
            // Основные сообщения будут загружаться из файлов локализации
            joinMessages = config.getStringList("join_messages");
            leaveMessages = config.getStringList("leave_messages");
            personalWelcomeMessages = config.getStringList("personal_welcome_messages");
            
            // Устанавливаем значения по умолчанию, если списки пусты
            if (joinMessages.isEmpty()) {
                joinMessages.add("&a+ &f{player} &7joined the server");
                config.set("join_messages", joinMessages);
            }
            if (leaveMessages.isEmpty()) {
                leaveMessages.add("&c- &f{player} &7left the server");
                config.set("leave_messages", leaveMessages);
            }
            if (personalWelcomeMessages.isEmpty()) {
                personalWelcomeMessages.add("&6Welcome to the server, &f{player}&6!");
                config.set("personal_welcome_messages", personalWelcomeMessages);
            }
            
            // Проверяем наличие настроек для подмодуля сообщений о выдаче прав оператора
            if (!config.contains("op_message_enabled")) {
                config.set("op_message_enabled", true);
            }
            
            // Настройка для отключения ванильных сообщений о выдаче прав оператора
            if (!config.contains("disable_vanilla_op_messages")) {
                config.set("disable_vanilla_op_messages", true);
            }
            
            saveModuleConfig("modules/server_messages");
        } catch (Exception e) {
            plugin.getPluginLogger().severe("❗ Error loading ServerMessagesModule config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Send personal welcome message to the player in their language
        if (personalWelcomeEnabled) {
            List<String> localizedWelcomeMessages = plugin.getConfigManager().getLocalizedMessageList("server_messages", "personal_welcome_messages", player);
            if (!localizedWelcomeMessages.isEmpty()) {
                String welcomeMessage = getRandomMessage(localizedWelcomeMessages);
                welcomeMessage = welcomeMessage.replace("{player}", player.getName());
                player.sendMessage(ColorUtil.toComponent(player, welcomeMessage));
            }
        }
        
        // Set join message that will be shown to all players
        if (joinMessageEnabled) {
            // We need to create a component that will be shown differently to each player based on their locale
            String baseJoinMessage = getRandomMessage(joinMessages);
            baseJoinMessage = baseJoinMessage.replace("{player}", player.getName());
            
            // Set the join message to null first to prevent the default message
            event.joinMessage(null);
            
            // Then broadcast a custom message to each player in their own language
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                List<String> localizedJoinMessages = plugin.getConfigManager().getLocalizedMessageList("server_messages", "join_messages", onlinePlayer);
                if (!localizedJoinMessages.isEmpty()) {
                    String localizedMessage = getRandomMessage(localizedJoinMessages);
                    localizedMessage = localizedMessage.replace("{player}", player.getName());
                    onlinePlayer.sendMessage(ColorUtil.toComponent(onlinePlayer, localizedMessage));
                } else {
                    // Fallback to config message if localization is not available
                    onlinePlayer.sendMessage(ColorUtil.toComponent(onlinePlayer, baseJoinMessage));
                }
            }
        } else {
            event.joinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (leaveMessageEnabled) {
            // We need to create a component that will be shown differently to each player based on their locale
            String baseLeaveMessage = getRandomMessage(leaveMessages);
            baseLeaveMessage = baseLeaveMessage.replace("{player}", player.getName());
            
            // Set the quit message to null first to prevent the default message
            event.quitMessage(null);
            
            // Then broadcast a custom message to each player in their own language
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                // Skip the player who is leaving
                if (onlinePlayer.equals(player)) continue;
                
                List<String> localizedLeaveMessages = plugin.getConfigManager().getLocalizedMessageList("server_messages", "leave_messages", onlinePlayer);
                if (!localizedLeaveMessages.isEmpty()) {
                    String localizedMessage = getRandomMessage(localizedLeaveMessages);
                    localizedMessage = localizedMessage.replace("{player}", player.getName());
                    onlinePlayer.sendMessage(ColorUtil.toComponent(onlinePlayer, localizedMessage));
                } else {
                    // Fallback to config message if localization is not available
                    onlinePlayer.sendMessage(ColorUtil.toComponent(onlinePlayer, baseLeaveMessage));
                }
            }
        } else {
            event.quitMessage(null);
        }
    }

    // Get random message from list
    private String getRandomMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        // Если в списке только одно сообщение, возвращаем его без рандома
        if (messages.size() == 1) {
            return messages.get(0);
        }
        // Иначе выбираем случайное сообщение из списка
        return messages.get(new Random().nextInt(messages.size()));
    }

}