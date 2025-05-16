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
 * along with this plugin. If not, see <https://www.gnu.org/licenses/>.n *
 * Created with ❤️ for the Minecraft community.
 */

package me.nagibatirowanie.originchat.commands;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.modules.AfkModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для управления режимом AFK
 */
public class AfkCommand implements CommandExecutor, TabCompleter {

    private final OriginChat plugin;
    private final AfkModule afkModule;

    public AfkCommand(OriginChat plugin, AfkModule afkModule) {
        this.plugin = plugin;
        this.afkModule = afkModule;
        
        // Регистрируем команду
        PluginCommand command = plugin.getCommand("afk");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
            plugin.getPluginLogger().info("[AfkCommand] AFK command registered successfully");
        } else {
            plugin.getPluginLogger().warning("[AfkCommand] Failed to register AFK command - command not found in plugin.yml");
        }
    }

    /**
     * Отменяет регистрацию команды
     */
    public void unregister() {
        PluginCommand command = plugin.getCommand("afk");
        if (command != null) {
            command.setExecutor(null);
            command.setTabCompleter(null);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLocaleManager().sendMessage(sender, "commands.player_only_command");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Проверяем права на использование команды
        if (!player.hasPermission("originchat.afk")) {
            plugin.getLocaleManager().sendMessage(player, "commands.no_permission");
            return true;
        }
        
        // Переключаем режим AFK
        boolean currentAfk = afkModule.isAfk(player);
        afkModule.setAfk(player, !currentAfk);
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Для команды /afk не нужны автодополнения
        return new ArrayList<>();
    }
}