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
            String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", "errors.not-a-player", (Player)null);
            sender.sendMessage(ColorUtil.format(localizedMessage));
            return true;
        }
        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();
        int range = commandRanges.getOrDefault(commandName, 100);
        boolean isGlobal = commandName.startsWith("g");
        if (args.length == 0 && requiresTextArgument(commandName)) {
            String errorKey = "errors.not-enough-arguments";
            String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", errorKey, player);
            player.sendMessage(ColorUtil.format(localizedMessage));
            return true;
        }
        String message = String.join(" ", args);
        switch (commandName) {
            case "me":
            case "gme":
                sendRoleplayMessage(player, range, isGlobal, commandName, message);
                break;
            case "do":
            case "gdo":
                sendRoleplayMessage(player, range, isGlobal, commandName, message);
                break;
            case "try":
            case "gtry":
                handleTryCommand(player, range, isGlobal, message);
                break;
            case "todo":
            case "gtodo":
                sendRoleplayMessage(player, range, isGlobal, commandName, message);
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
        // Получаем список результатов из локализации
        List<String> resultsList = plugin.getConfigManager().getLocalizedMessageList("roleplay", "results", player);
        // Используем первые два элемента списка для успеха/неудачи
        // Если список пуст или недостаточно элементов, используем значения по умолчанию
        String resultText = success ? 
                (resultsList.size() > 0 ? resultsList.get(0) : "success") : 
                (resultsList.size() > 1 ? resultsList.get(1) : "failure");
        
        String formatKey = success ? (isGlobal ? "gtry_success" : "try_success") : (isGlobal ? "gtry_failure" : "try_failure");
        sendRoleplayMessage(player, range, isGlobal, formatKey, message);
    }

    // Handle roll/groll command
    private void handleRollCommand(Player player, int range, boolean isGlobal, String[] args) {
        if (args.length < 2) {
            String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", "errors.not-enough-arguments", player);
            player.sendMessage(ColorUtil.format(localizedMessage));
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
            
            // Используем локализованный формат для команды roll/groll
            String commandKey = isGlobal ? "groll" : "roll";
            String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, player);
            String formatted = localizedFormat.replace("{player}", player.getName())
                    .replace("{min}", String.valueOf(min))
                    .replace("{max}", String.valueOf(max))
                    .replace("{result}", String.valueOf(result));
            
            // Отправляем сообщение всем игрокам в зависимости от типа команды (локальная/глобальная)
            if (isGlobal) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    String targetFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                    String targetFormatted = targetFormat.replace("{player}", player.getName())
                            .replace("{min}", String.valueOf(min))
                            .replace("{max}", String.valueOf(max))
                            .replace("{result}", String.valueOf(result));
                    target.sendMessage(ColorUtil.format(target, targetFormatted));
                }
            } else {
                for (Player target : player.getWorld().getPlayers()) {
                    if (target.getLocation().distance(player.getLocation()) <= range) {
                        String targetFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                        String targetFormatted = targetFormat.replace("{player}", player.getName())
                                .replace("{min}", String.valueOf(min))
                                .replace("{max}", String.valueOf(max))
                                .replace("{result}", String.valueOf(result));
                        target.sendMessage(ColorUtil.format(target, targetFormatted));
                    }
                }
            }
        } catch (NumberFormatException e) {
            String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", "errors.not-enough-arguments", player);
            player.sendMessage(ColorUtil.format(localizedMessage));
        }
    }

    // Handle ball/gball command
    private void handleBallCommand(Player player, int range, boolean isGlobal, String message) {
        String commandKey = isGlobal ? "gball" : "ball";
        
        // Отправляем сообщение всем игрокам в зависимости от типа команды (локальная/глобальная)
        if (isGlobal) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                // Получаем список результатов для каждого целевого игрока индивидуально
                List<String> targetResultsList = plugin.getConfigManager().getLocalizedMessageList("roleplay", "results", target);
                
                // Выбираем ответ на языке целевого игрока
                String answer;
                if (targetResultsList.size() > 2) {
                    // Выбираем случайный ответ из списка результатов, начиная с индекса 2
                    int randomIndex = random.nextInt(targetResultsList.size() - 2) + 2;
                    answer = targetResultsList.get(randomIndex);
                } else {
                    // Используем резервный список ответов
                    answer = magicBallAnswers.get(random.nextInt(magicBallAnswers.size()));
                }
                
                String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                String formatted = localizedFormat.replace("{player}", player.getName())
                        .replace("{message}", message)
                        .replace("{result}", answer);
                target.sendMessage(ColorUtil.format(target, formatted));
            }
        } else {
            for (Player target : player.getWorld().getPlayers()) {
                if (target.getLocation().distance(player.getLocation()) <= range) {
                    // Получаем список результатов для каждого целевого игрока индивидуально
                    List<String> targetResultsList = plugin.getConfigManager().getLocalizedMessageList("roleplay", "results", target);
                    
                    // Выбираем ответ на языке целевого игрока
                    String answer;
                    if (targetResultsList.size() > 2) {
                        // Выбираем случайный ответ из списка результатов, начиная с индекса 2
                        int randomIndex = random.nextInt(targetResultsList.size() - 2) + 2;
                        answer = targetResultsList.get(randomIndex);
                    } else {
                        // Используем резервный список ответов
                        answer = magicBallAnswers.get(random.nextInt(magicBallAnswers.size()));
                    }
                    
                    String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                    String formatted = localizedFormat.replace("{player}", player.getName())
                            .replace("{message}", message)
                            .replace("{result}", answer);
                    target.sendMessage(ColorUtil.format(target, formatted));
                }
            }
        }
    }

    private boolean requiresTextArgument(String commandName) {
        return Arrays.asList("me", "gme", "do", "gdo", "try", "gtry", "todo", "gtodo", "ball", "gball").contains(commandName);
    }

    private void sendRoleplayMessage(Player player, int range, boolean isGlobal, String format, String message) {
        if (format == null || format.isEmpty()) {
            format = "{player}: {message}";
        }
        
        // Получаем правильный ключ формата из локализации
        String formatKey = "format." + format;
        
        if (isGlobal) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", formatKey, target);
                String formatted = localizedFormat.replace("{player}", player.getName()).replace("{message}", message);
                formatted = ColorUtil.format(target, formatted);
                target.sendMessage(formatted);
            }
        } else {
            for (Player target : player.getWorld().getPlayers()) {
                if (target.getLocation().distance(player.getLocation()) <= range) {
                    String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", formatKey, target);
                    String formatted = localizedFormat.replace("{player}", player.getName()).replace("{message}", message);
                    formatted = ColorUtil.format(target, formatted);
                    target.sendMessage(formatted);
                }
            }
        }
    }
}