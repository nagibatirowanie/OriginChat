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
 * Обработчик команд для работы с базой данных
 */
public class DatabaseCommands implements CommandExecutor, TabCompleter {

    private final OriginChat plugin;
    private boolean registered = false;

    public DatabaseCommands(OriginChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Регистрирует команды для работы с базой данных
     */
    public void registerCommands() {
        if (!registered) {
            plugin.getCommand("dbinfo").setExecutor(this);
            plugin.getCommand("dbinfo").setTabCompleter(this);
            registered = true;
            plugin.getPluginLogger().info("Команды базы данных зарегистрированы. Используется провайдер: " + plugin.getDatabaseManager().getProviderType());
        }
    }

    /**
     * Отменяет регистрацию команд
     */
    public void unregisterCommands() {
        if (registered) {
            plugin.getCommand("dbinfo").setExecutor(null);
            plugin.getCommand("dbinfo").setTabCompleter(null);
            registered = false;
            plugin.getPluginLogger().info("Команды базы данных отключены.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("originchat.admin")) {
            sender.sendMessage("§cУ вас нет прав для выполнения этой команды.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("dbinfo")) {
            if (!plugin.getDatabaseManager().isEnabled()) {
                sender.sendMessage("§cБаза данных отключена в конфигурации.");
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
                            sender.sendMessage("§cУкажите имя игрока.");
                            return true;
                        }
                        showPlayerInfo(sender, args[1]);
                        return true;
                    case "stats":
                        showDatabaseStats(sender);
                        return true;
                    default:
                        sender.sendMessage("§cНеизвестная подкоманда. Используйте /dbinfo, /dbinfo player <имя> или /dbinfo stats");
                        return true;
                }
            }
        }

        return false;
    }

    private void showDatabaseInfo(CommandSender sender) {
        sender.sendMessage("§6=== Информация о базе данных ===");
        sender.sendMessage("§7Тип: §f" + plugin.getDatabaseManager().getProviderType());
        sender.sendMessage("§7Статус: §f" + (plugin.getDatabaseManager().isEnabled() ? "§aВключена" : "§cВыключена"));
        sender.sendMessage("§7Используйте §f/dbinfo stats §7для просмотра статистики.");
        sender.sendMessage("§7Используйте §f/dbinfo player <имя> §7для просмотра информации об игроке.");
    }

    private void showDatabaseStats(CommandSender sender) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            sender.sendMessage("§cБаза данных отключена.");
            return;
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) as count FROM oc_players")) {

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    sender.sendMessage("§6=== Статистика базы данных ===");
                    sender.sendMessage("§7Тип: §f" + plugin.getDatabaseManager().getProviderType());
                    sender.sendMessage("§7Количество игроков: §f" + count);
                }
            }
        } catch (SQLException e) {
            sender.sendMessage("§cОшибка при получении статистики: " + e.getMessage());
            plugin.getPluginLogger().warning("Ошибка при получении статистики базы данных: " + e.getMessage());
        }
    }

    private void showPlayerInfo(CommandSender sender, String playerName) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            sender.sendMessage("§cБаза данных отключена.");
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

                    sender.sendMessage("§6=== Информация об игроке " + playerName + " ===");
                    sender.sendMessage("§7UUID: §f" + uuid);
                    sender.sendMessage("§7Локаль: §f" + locale);
                    sender.sendMessage("§7Автоперевод: §f" + (translateEnabled ? "§aВключен" : "§cВыключен"));
                    sender.sendMessage("§7Первый вход: §f" + firstJoin);
                    sender.sendMessage("§7Последний вход: §f" + lastJoin);
                } else {
                    sender.sendMessage("§cИгрок " + playerName + " не найден в базе данных.");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage("§cОшибка при получении информации об игроке: " + e.getMessage());
            plugin.getPluginLogger().warning("Ошибка при получении информации об игроке: " + e.getMessage());
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
                return null; // Вернуть null для автодополнения имен игроков
            }
        }
        return Collections.emptyList();
    }

    /**
     * Сохранить данные игрока в базу данных
     * @param player игрок
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