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

 package me.nagibatirowanie.originchat.database;

 import me.nagibatirowanie.originchat.OriginChat;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.command.TabCompleter;
 import org.bukkit.entity.Player;
 
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 import java.util.UUID;
 import java.util.stream.Collectors;
 
 /**
  * Command handler for database operations
  */
 public class DatabaseCommands implements CommandExecutor, TabCompleter {
 
     private final OriginChat plugin;
     private boolean registered = false;
 
     public DatabaseCommands(OriginChat plugin) {
         this.plugin = plugin;
     }
 
     /**
      * Registers commands for database operations
      */
     public void registerCommands() {
         if (!registered) {
             plugin.getCommand("dbinfo").setExecutor(this);
             plugin.getCommand("dbinfo").setTabCompleter(this);
             registered = true;
             plugin.getPluginLogger().info("Database commands registered. Provider used: " + plugin.getDatabaseManager().getProviderType());
         }
     }
 
     /**
      * Unregisters commands
      */
     public void unregisterCommands() {
         if (registered) {
             plugin.getCommand("dbinfo").setExecutor(null);
             plugin.getCommand("dbinfo").setTabCompleter(null);
             registered = false;
             plugin.getPluginLogger().info("Database commands disabled.");
         }
     }
 
     @Override
     public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
         if (!sender.hasPermission("originchat.admin")) {
             sender.sendMessage("§cYou don't have permission to execute this command.");
             return true;
         }
 
         if (command.getName().equalsIgnoreCase("dbinfo")) {
             if (!plugin.getDatabaseManager().isEnabled()) {
                 sender.sendMessage("§cDatabase is disabled in configuration.");
                 return true;
             }
 
             if (args.length == 0) {
                 showDatabaseInfo(sender);
                 return true;
             }
 
             if (args.length >= 1) {
                 String subCommand = args[0].toLowerCase();
                 switch (subCommand) {
                     case "player":
                         if (args.length < 2) {
                             sender.sendMessage("§cPlease specify a player name.");
                             return true;
                         }
                         showPlayerInfo(sender, args[1]);
                         return true;
                     case "stats":
                         showDatabaseStats(sender);
                         return true;
                     default:
                         sender.sendMessage("§cUnknown subcommand. Use /dbinfo, /dbinfo player <name> or /dbinfo stats");
                         return true;
                 }
             }
         }
 
         return false;
     }
 
     private void showDatabaseInfo(CommandSender sender) {
         sender.sendMessage("§6=== Database Information ===");
         sender.sendMessage("§7Type: §f" + plugin.getDatabaseManager().getProviderType());
         sender.sendMessage("§7Status: §f" + (plugin.getDatabaseManager().isEnabled() ? "§aEnabled" : "§cDisabled"));
         sender.sendMessage("§7Use §f/dbinfo stats §7to view statistics.");
         sender.sendMessage("§7Use §f/dbinfo player <name> §7to view player information.");
     }
 
     private void showDatabaseStats(CommandSender sender) {
         if (!plugin.getDatabaseManager().isEnabled()) {
             sender.sendMessage("§cDatabase is disabled.");
             return;
         }
 
         try (Connection conn = plugin.getDatabaseManager().getConnection();
              PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) as count FROM oc_players")) {
 
             try (ResultSet rs = ps.executeQuery()) {
                 if (rs.next()) {
                     int count = rs.getInt("count");
                     sender.sendMessage("§6=== Database Statistics ===");
                     sender.sendMessage("§7Type: §f" + plugin.getDatabaseManager().getProviderType());
                     sender.sendMessage("§7Number of players: §f" + count);
                 }
             }
         } catch (SQLException e) {
             sender.sendMessage("§cError while retrieving statistics: " + e.getMessage());
             plugin.getPluginLogger().warning("Error while retrieving database statistics: " + e.getMessage());
         }
     }
 
     private void showPlayerInfo(CommandSender sender, String playerName) {
         if (!plugin.getDatabaseManager().isEnabled()) {
             sender.sendMessage("§cDatabase is disabled.");
             return;
         }
 
         try (Connection conn = plugin.getDatabaseManager().getConnection();
              PreparedStatement ps = conn.prepareStatement("SELECT * FROM oc_players WHERE name = ?")) {
 
             ps.setString(1, playerName);
 
             try (ResultSet rs = ps.executeQuery()) {
                 if (rs.next()) {
                     String uuid = rs.getString("uuid");
                     String locale = rs.getString("locale");
                     boolean translateEnabled = rs.getBoolean("translate_enabled");
                     String firstJoin = rs.getString("first_join");
                     String lastJoin = rs.getString("last_join");
 
                     sender.sendMessage("§6=== Player Information: " + playerName + " ===");
                     sender.sendMessage("§7UUID: §f" + uuid);
                     sender.sendMessage("§7Locale: §f" + locale);
                     sender.sendMessage("§7Auto-translation: §f" + (translateEnabled ? "§aEnabled" : "§cDisabled"));
                     sender.sendMessage("§7First join: §f" + firstJoin);
                     sender.sendMessage("§7Last join: §f" + lastJoin);
                 } else {
                     sender.sendMessage("§cPlayer " + playerName + " not found in the database.");
                 }
             }
         } catch (SQLException e) {
             sender.sendMessage("§cError while retrieving player information: " + e.getMessage());
             plugin.getPluginLogger().warning("Error while retrieving player information: " + e.getMessage());
         }
     }
 
     @Override
     public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
         if (command.getName().equalsIgnoreCase("dbinfo")) {
             if (args.length == 1) {
                 return Arrays.asList("player", "stats").stream()
                         .filter(s -> s.startsWith(args[0].toLowerCase()))
                         .collect(Collectors.toList());
             } else if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
                 return null; // Return null for auto-completion of player names
             }
         }
         return Collections.emptyList();
     }
 
     /**
      * Save player data to the database
      * @param player player
      */
     public void savePlayerData(Player player) {
         if (!plugin.getDatabaseManager().isEnabled()) return;
         
         UUID uuid = player.getUniqueId();
         String name = player.getName();
         String locale = plugin.getLocaleManager().getPlayerLocale(player);
         boolean translateEnabled = plugin.getTranslateManager().isTranslateEnabled(player);
         
         plugin.getDatabaseManager().savePlayerData(uuid, name, locale, translateEnabled);
     }
 }