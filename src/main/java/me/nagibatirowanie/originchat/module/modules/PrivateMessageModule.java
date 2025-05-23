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
import me.nagibatirowanie.originchat.utils.FormatUtil;
 import org.bukkit.Bukkit;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.command.TabCompleter;
 import org.bukkit.entity.Player;
 
 import java.util.*;
 
 /**
  * Module for sending private messages between players.
  */
 public class PrivateMessageModule extends AbstractModule implements CommandExecutor, TabCompleter {
 
     private final Map<Player, Player> lastMessageMap = new HashMap<>();
 
     private String senderFormat;
     private String receiverFormat;
     private boolean enabled;
     private boolean registered = false;
 
     /**
      * Initializes the private message module.
      *
      * @param plugin OriginChat main instance
      */
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
 
     /**
      * Registers /msg and /r commands.
      */
     private void registerCommands() {
         plugin.getCommand("msg").setExecutor(this);
         plugin.getCommand("msg").setTabCompleter(this);
         plugin.getCommand("r").setExecutor(this);
         plugin.getCommand("r").setTabCompleter(this);
     }
 
     /**
      * Unregisters /msg and /r commands.
      */
     private void unregisterCommands() {
         plugin.getCommand("msg").setExecutor(null);
         plugin.getCommand("msg").setTabCompleter(null);
         plugin.getCommand("r").setExecutor(null);
         plugin.getCommand("r").setTabCompleter(null);
     }
 
     /**
      * Loads module settings from configuration.
      */
     protected void loadConfig() {
         enabled = config.getBoolean("enabled", true);
         senderFormat = config.getString("format.sender", "&7You &8-> &7{receiver}: &f{message}");
         receiverFormat = config.getString("format.receiver", "&7{sender} &8-> &7You: &f{message}");
     }
 
     @Override
     public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
         if (!(sender instanceof Player)) {
             String localizedMessage = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.not-a-player", (Player)null);
             sender.sendMessage(FormatUtil.format(localizedMessage));
             return true;
         }
 
         Player player = (Player) sender;
 
         if (command.getName().equalsIgnoreCase("msg")) {
             if (args.length == 0) {
                 String msg = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.player-not-specified", player);
                 player.sendMessage(FormatUtil.format(player, msg));
                 return true;
             }
 
             if (args.length == 1) {
                 String msg = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.message-not-specified", player);
                 player.sendMessage(FormatUtil.format(player, msg));
                 return true;
             }
 
             Player target = Bukkit.getPlayerExact(args[0]);
 
             if (target == null || !target.isOnline()) {
                 String msg = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.player-not-found", player);
                 msg = msg.replace("{player}", args[0]);
                 player.sendMessage(FormatUtil.format(player, msg));
                 return true;
             }
 
             if (target.getUniqueId().equals(player.getUniqueId())) {
                 String msg = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.cannot-message-yourself", player);
                 player.sendMessage(FormatUtil.format(player, msg));
                 return true;
             }
 
             String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
             sendPrivateMessage(player, target, message);
             return true;
         }
 
         if (command.getName().equalsIgnoreCase("r")) {
             if (args.length == 0) {
                 String msg = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.message-not-specified", player);
                 player.sendMessage(FormatUtil.format(player, msg));
                 return true;
             }
 
             Player lastTarget = lastMessageMap.get(player);
             if (lastTarget == null || !lastTarget.isOnline()) {
                 String msg = plugin.getConfigManager().getLocalizedMessage("private_messages", "messages.no-reply-target", player);
                 player.sendMessage(FormatUtil.format(player, msg));
                 return true;
             }
 
             String message = String.join(" ", args);
             sendPrivateMessage(player, lastTarget, message);
             return true;
         }
 
         return false;
     }
 
     /**
      * Sends a private message and updates lastMessageMap.
      *
      * @param sender   the message sender
      * @param receiver the message receiver
      * @param message  the message content
      */
     private void sendPrivateMessage(Player sender, Player receiver, String message) {
         String senderLocalizedFormat = plugin.getConfigManager().getLocalizedMessage("private_messages", "format.sender", sender);
         if (senderLocalizedFormat.isEmpty() || senderLocalizedFormat.startsWith("§cMessage not found")) {
             senderLocalizedFormat = senderFormat;
         }
 
         String receiverLocalizedFormat = plugin.getConfigManager().getLocalizedMessage("private_messages", "format.receiver", receiver);
         if (receiverLocalizedFormat.isEmpty() || receiverLocalizedFormat.startsWith("§cMessage not found")) {
             receiverLocalizedFormat = receiverFormat;
         }
 
         String formattedSenderMessage = formatMessage(senderLocalizedFormat, sender, receiver, message);
         String formattedReceiverMessage = formatMessage(receiverLocalizedFormat, sender, receiver, message);
 
         sender.sendMessage(FormatUtil.toComponent(sender, formattedSenderMessage));
         receiver.sendMessage(FormatUtil.toComponent(receiver, formattedReceiverMessage));
 
         lastMessageMap.put(sender, receiver);
         lastMessageMap.put(receiver, sender);
     }
 
     /**
      * Replaces placeholders in a format string.
      *
      * @param format   the format template
      * @param sender   the message sender
      * @param receiver the message receiver
      * @param message  the message content
      * @return the populated message string
      */
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
 