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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Module for sending private messages between players
 */
public class PrivateMessageModule extends AbstractModule implements CommandExecutor, TabCompleter {

    private final Map<Player, Player> lastMessageMap = new HashMap<>();

    private String senderFormat;
    private String receiverFormat;
    private boolean enabled;
    private boolean registered = false;

    private String msgNotAPlayer;
    private String msgPlayerNotSpecified;
    private String msgMessageNotSpecified;
    private String msgPlayerNotFound;
    private String msgCannotMessageYourself;
    private String msgNoReplyTarget;

    public PrivateMessageModule(OriginChat plugin) {
        super(plugin, "private_messages", "Private Messages", "Module for sending private messages between players", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/private_messages");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        
        loadConfig();
        
        if (!enabled) {
            return;
        }

        if (!registered) {
            registerCommands();
            registered = true;
        }

    }

    @Override
    public void onDisable() {
        if (registered) {
            unregisterCommands();
            registered = false;
        }
    }

    private void registerCommands() {
        plugin.getCommand("msg").setExecutor(this);
        plugin.getCommand("msg").setTabCompleter(this);
        plugin.getCommand("r").setExecutor(this);
        plugin.getCommand("r").setTabCompleter(this);
    }

    private void unregisterCommands() {
        plugin.getCommand("msg").setExecutor(null);
        plugin.getCommand("msg").setTabCompleter(null);
        plugin.getCommand("r").setExecutor(null);
        plugin.getCommand("r").setTabCompleter(null);
    }

    protected void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            
            // Загружаем форматы сообщений из конфигурации как резервные варианты
            // Основные форматы будут загружаться из файлов локализации
            senderFormat = config.getString("format.sender", "&7You &8-> &7{receiver}: &f{message}");
            receiverFormat = config.getString("format.receiver", "&7{sender} &8-> &7You: &f{message}");
            
            // Загружаем сообщения из конфигурации как резервные варианты
            // Основные сообщения будут загружаться из файлов локализации при вызове
            ConfigurationSection messagesSection = config.getConfigurationSection("messages");
            if (messagesSection != null) {
                msgNotAPlayer = messagesSection.getString("not-a-player", "&cThis command is only available to players!");
                msgPlayerNotSpecified = messagesSection.getString("player-not-specified", "&cPlease specify a player name!");
                msgMessageNotSpecified = messagesSection.getString("message-not-specified", "&cPlease enter a message!");
                msgPlayerNotFound = messagesSection.getString("player-not-found", "&cPlayer {player} not found or not online!");
                msgCannotMessageYourself = messagesSection.getString("cannot-message-yourself", "&cYou cannot message yourself!");
                msgNoReplyTarget = messagesSection.getString("no-reply-target", "&cNo one to reply to! Send a message to someone first.");
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("❗ Error loading PrivateMessages settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.not-a-player", (Player)null);
            sender.sendMessage(ColorUtil.format(localizedMessage));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("msg")) {
            if (args.length == 0) {
                String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.player-not-specified", player);
                player.sendMessage(ColorUtil.format(player, localizedMessage));
                return true;
            }

            if (args.length == 1) {
                String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.message-not-specified", player);
                player.sendMessage(ColorUtil.format(player, localizedMessage));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);

            if (target == null || !target.isOnline()) {
                String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.player-not-found", player);
                localizedMessage = localizedMessage.replace("{player}", args[0]);
                player.sendMessage(ColorUtil.format(player, localizedMessage));
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.cannot-message-yourself", player);
                player.sendMessage(ColorUtil.format(player, localizedMessage));
                return true;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            sendPrivateMessage(player, target, message);
            return true;
        }

        if (command.getName().equalsIgnoreCase("r")) {
            if (args.length == 0) {
                String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.message-not-specified", player);
                player.sendMessage(ColorUtil.format(player, localizedMessage));
                return true;
            }

            Player lastTarget = lastMessageMap.get(player);
            if (lastTarget == null || !lastTarget.isOnline()) {
                String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.no-reply-target", player);
                player.sendMessage(ColorUtil.format(player, localizedMessage));
                return true;
            }

            String message = String.join(" ", args);
            sendPrivateMessage(player, lastTarget, message);
            return true;
        }

        return false;
    }

    private void sendPrivateMessage(Player sender, Player receiver, String message) {
        // Get localized format for sender
        String senderLocalizedFormat = plugin.getConfigManager().getLocalizedMessage("private_messages", "format.sender", sender);
        if (senderLocalizedFormat.isEmpty() || senderLocalizedFormat.startsWith("§cMessage not found")) {
            senderLocalizedFormat = senderFormat;
        }
        
        // Get localized format for receiver
        String receiverLocalizedFormat = plugin.getConfigManager().getLocalizedMessage("private_messages", "format.receiver", receiver);
        if (receiverLocalizedFormat.isEmpty() || receiverLocalizedFormat.startsWith("§cMessage not found")) {
            receiverLocalizedFormat = receiverFormat;
        }
        
        String formattedSenderMessage = formatMessage(senderLocalizedFormat, sender, receiver, message);
        String formattedReceiverMessage = formatMessage(receiverLocalizedFormat, sender, receiver, message);

        sender.sendMessage(ColorUtil.toComponent(sender, formattedSenderMessage));
        receiver.sendMessage(ColorUtil.toComponent(receiver, formattedReceiverMessage));

        lastMessageMap.put(sender, receiver);
        lastMessageMap.put(receiver, sender);
    }

    private String formatMessage(String format, Player sender, Player receiver, String message) {
        return format
                .replace("{sender}", sender.getName())
                .replace("{receiver}", receiver.getName())
                .replace("{message}", message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("msg") && args.length == 1) {
            List<String> onlinePlayers = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender)) {
                    onlinePlayers.add(player.getName());
                }
            }
            return onlinePlayers;
        }
        return Collections.emptyList();
    }
}