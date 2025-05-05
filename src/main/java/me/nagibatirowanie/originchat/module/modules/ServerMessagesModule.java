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

    public ServerMessagesModule(OriginChat plugin) {
        super(plugin, "server_messages", "Server Messages", "Manage player join/leave messages", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/server_messages");
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        log("Server messages module loaded.");
    }

    @Override
    public void onDisable() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        log("Server messages module disabled.");
    }

    // Load settings from config
    protected void loadConfig() {
        try {
            joinMessageEnabled = config.getBoolean("join_message_enabled", true);
            leaveMessageEnabled = config.getBoolean("leave_message_enabled", true);
            personalWelcomeEnabled = config.getBoolean("personal_welcome_enabled", true);
            joinMessages = config.getStringList("join_messages");
            leaveMessages = config.getStringList("leave_messages");
            personalWelcomeMessages = config.getStringList("personal_welcome_messages");
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
            saveModuleConfig("modules/server_messages");
        } catch (Exception e) {
            plugin.getPluginLogger().severe("❗ Error loading ServerMessagesModule config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (personalWelcomeEnabled && !personalWelcomeMessages.isEmpty()) {
            String welcomeMessage = getRandomMessage(personalWelcomeMessages, player);
            player.sendMessage(ColorUtil.toComponent(player, welcomeMessage));
        }
        if (joinMessageEnabled && !joinMessages.isEmpty()) {
            String joinMessage = getRandomMessage(joinMessages, player);
            event.joinMessage(ColorUtil.toComponent(player, joinMessage));
        } else {
            event.joinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (leaveMessageEnabled && !leaveMessages.isEmpty()) {
            String leaveMessage = getRandomMessage(leaveMessages, player);
            event.quitMessage(ColorUtil.toComponent(player, leaveMessage));
        } else {
            event.quitMessage(null);
        }
    }

    // Get random message from list and replace placeholders
    private String getRandomMessage(List<String> messages, Player player) {
        String message = messages.get(new Random().nextInt(messages.size()));
        return message.replace("{player}", player.getName());
    }

    // Get random message from list
    private String getRandomMessage(List<String> messages) {
        return messages.get(new Random().nextInt(messages.size()));
    }
}