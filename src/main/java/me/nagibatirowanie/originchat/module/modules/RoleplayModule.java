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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Module for roleplay commands (me, do, try, etc.)
 */
public class RoleplayModule extends AbstractModule implements CommandExecutor {

    private static final List<String> COMMANDS = Arrays.asList(
            "me", "gme", "do", "gdo", "try", "gtry", "todo", "gtodo", "roll", "groll", "ball", "gball"
    );

    private final Random random = new Random();
    private boolean registered = false;

    private final Map<String, Integer> commandRanges = new HashMap<>();
    private final Map<String, String> commandFormats = new HashMap<>();
    private List<String> magicBallAnswers = new ArrayList<>();

    public RoleplayModule(OriginChat plugin) {
        super(plugin, "roleplay", "Roleplay Module", "Adds commands for roleplay (me, do, try, etc.)", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/roleplay");
        if (!registered) {
            registerCommands();
            registered = true;
        }
        loadConfig();
        plugin.getPluginLogger().info("Roleplay module loaded!");
    }

    @Override
    public void onDisable() {
        if (registered) {
            unregisterCommands();
            registered = false;
        }
        plugin.getPluginLogger().info("Roleplay module disabled.");
    }

    // Load settings from config
    private void loadConfig() {
        try {
            commandRanges.clear();
            commandFormats.clear();
            magicBallAnswers.clear();
            ConfigurationSection ranges = config.getConfigurationSection("command_ranges");
            if (ranges != null) {
                for (String command : ranges.getKeys(false)) {
                    commandRanges.put(command, ranges.getInt(command, 100));
                }
            }
            ConfigurationSection formats = config.getConfigurationSection("formats");
            if (formats != null) {
                for (String command : formats.getKeys(false)) {
                    commandFormats.put(command, formats.getString(command, ""));
                }
            }
            magicBallAnswers = config.getStringList("magic_ball_answers");
            if (magicBallAnswers.isEmpty()) {
                magicBallAnswers = Arrays.asList(
                    "Yes", "No", "Maybe", "Very likely", "Unlikely",
                    "Definitely yes", "Definitely no", "Ask later", "Cannot say now"
                );
                config.set("magic_ball_answers", magicBallAnswers);
                config.save(configName);
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("❗ Error loading RoleplayModule settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Register all commands
    private void registerCommands() {
        for (String commandName : COMMANDS) {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                command.setExecutor(this);
            } else {
                plugin.getPluginLogger().warning("Command '" + commandName + "' not found in plugin.yml!");
            }
        }
    }

    // Unregister all commands
    private void unregisterCommands() {
        for (String commandName : COMMANDS) {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                command.setExecutor(null);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("errors.only_players"));
            return true;
        }
        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();
        int range = commandRanges.getOrDefault(commandName, 100);
        boolean isGlobal = commandName.startsWith("g");
        if (args.length == 0 && requiresTextArgument(commandName)) {
            String errorKey = "errors.missing_argument." + commandName.replaceFirst("^g", "");
            player.sendMessage(getMessage(errorKey));
            return true;
        }
        String message = String.join(" ", args);
        switch (commandName) {
            case "me":
            case "gme":
                sendRoleplayMessage(player, range, isGlobal, commandFormats.get(commandName), message);
                break;
            case "do":
            case "gdo":
                sendRoleplayMessage(player, range, isGlobal, commandFormats.get(commandName), message);
                break;
            case "try":
            case "gtry":
                handleTryCommand(player, range, isGlobal, message);
                break;
            case "todo":
            case "gtodo":
                sendRoleplayMessage(player, range, isGlobal, commandFormats.get(commandName), message);
                break;
            case "roll":
            case "groll":
                handleRollCommand(player, range, isGlobal, args);
                break;
            case "ball":
            case "gball":
                handleBallCommand(player, range, isGlobal, message);
                break;
            default:
                return false;
        }
        return true;
    }

    // Handle try/gtry command
    private void handleTryCommand(Player player, int range, boolean isGlobal, String message) {
        boolean success = random.nextBoolean();
        String formatKey = success ? (isGlobal ? "gtry_success" : "try_success") : (isGlobal ? "gtry_failure" : "try_failure");
        sendRoleplayMessage(player, range, isGlobal, commandFormats.get(formatKey), message);
    }

    // Handle roll/groll command
    private void handleRollCommand(Player player, int range, boolean isGlobal, String[] args) {
        if (args.length < 2) {
            player.sendMessage(getMessage("errors.invalid_roll_usage"));
            return;
        }
        try {
            int min = Integer.parseInt(args[0]);
            int max = Integer.parseInt(args[1]);
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }
            int result = random.nextInt(max - min + 1) + min;
            String format = commandFormats.get(isGlobal ? "groll" : "roll");
            String formatted = format.replace("{player}", player.getName())
                    .replace("{min}", String.valueOf(min))
                    .replace("{max}", String.valueOf(max))
                    .replace("{result}", String.valueOf(result));
            sendRoleplayMessage(player, range, isGlobal, formatted, "");
        } catch (NumberFormatException e) {
            player.sendMessage(getMessage("errors.invalid_roll_usage"));
        }
    }

    // Handle ball/gball command
    private void handleBallCommand(Player player, int range, boolean isGlobal, String message) {
        String answer = magicBallAnswers.get(random.nextInt(magicBallAnswers.size()));
        String format = commandFormats.get(isGlobal ? "gball" : "ball");
        String formatted = format.replace("{player}", player.getName())
                .replace("{question}", message)
                .replace("{answer}", answer);
        sendRoleplayMessage(player, range, isGlobal, formatted, "");
    }

    private boolean requiresTextArgument(String commandName) {
        return Arrays.asList("me", "gme", "do", "gdo", "try", "gtry", "todo", "gtodo", "ball", "gball").contains(commandName);
    }

    private void sendRoleplayMessage(Player player, int range, boolean isGlobal, String format, String message) {
        if (format == null || format.isEmpty()) {
            format = "{player}: {message}";
        }
        String formatted = format.replace("{player}", player.getName()).replace("{message}", message);
        if (isGlobal) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                target.sendMessage(formatted);
            }
        } else {
            for (Player target : player.getWorld().getPlayers()) {
                if (target.getLocation().distance(player.getLocation()) <= range) {
                    target.sendMessage(formatted);
                }
            }
        }
    }

    private String getMessage(String key) {
        return config.getString(key, "");
    }
}