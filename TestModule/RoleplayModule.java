package me.nagibatirowanie.originChat.Modules;

import me.nagibatirowanie.originChat.OriginChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

import static me.nagibatirowanie.originLib.Messages.applyPlaceholders;
import static me.nagibatirowanie.originLib.Messages.replaceHexColors;

public class RoleplayModule extends Module implements CommandExecutor {

    private static final List<String> COMMANDS = Arrays.asList(
            "me", "gme", "do", "gdo", "try", "gtry", "todo", "gtodo", "roll", "groll", "ball", "gball"
    );

    private final Random random = new Random();
    private boolean registered = false;

    private final Map<String, Integer> commandRanges = new HashMap<>();
    private final Map<String, String> commandFormats = new HashMap<>();

    public RoleplayModule(OriginChat plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!isEnabled()) {
            plugin.getLogger().info("The Roleplay module is disabled in the configuration. Skip activation.");
            return;
        }

        if (!registered) {
            registerCommands();
            registered = true;
        }

        loadConfig();
        plugin.getLogger().info("The Roleplay module has been successfully loaded.");
    }

    @Override
    public void onDisable() {
        if (registered) {
            unregisterCommands();
            registered = false;
        }
        plugin.getLogger().info("The Roleplay module is disabled.");
    }

    protected void loadConfig() {
        try {
            ConfigurationSection roleplaySection = plugin.getConfig().getConfigurationSection("roleplay");
            if (roleplaySection != null) {
                // Загрузка диапазонов команд
                ConfigurationSection ranges = roleplaySection.getConfigurationSection("command_ranges");
                if (ranges != null) {
                    for (String command : ranges.getKeys(false)) {
                        commandRanges.put(command, ranges.getInt(command, 100));
                    }
                }

                // Загрузка форматов сообщений
                ConfigurationSection commands = plugin.getMessagesConfig().getConfigurationSection("roleplay.commands");
                if (commands != null) {
                    for (String command : commands.getKeys(false)) {
                        commandFormats.put(command, commands.getString(command, ""));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❗Failed to load RoleplayModule configuration: " + e.getMessage());
        }
    }

    private void registerCommands() {
        COMMANDS.forEach(this::registerCommand);
    }

    private void registerCommand(String commandName) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setExecutor(this);
        }
    }

    private void unregisterCommands() {
        COMMANDS.forEach(this::unregisterCommand);
    }

    private void unregisterCommand(String commandName) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setExecutor(null);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessageFromConfig("roleplay.errors.only_players"));
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();
        int range = commandRanges.getOrDefault(commandName, 100);
        boolean isGlobal = commandName.startsWith("g");

        // Проверка на наличие аргументов для команд, которые требуют текста
        if (args.length == 0 && requiresTextArgument(commandName)) {
            String errorKey = getErrorKeyForCommand(commandName);
            player.sendMessage(getMessageFromConfig(errorKey));
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
                handleTryCommand(player, range, isGlobal, message, commandName);
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

    private void handleTryCommand(Player player, int range, boolean isGlobal, String message, String commandName) {
        boolean success = random.nextBoolean();
        String formatKey = success ? (isGlobal ? "gtry_success" : "try_success") : (isGlobal ? "gtry_failure" : "try_failure");
        sendRoleplayMessage(player, range, isGlobal, commandFormats.get(formatKey), message);
    }

    private void handleRollCommand(Player player, int range, boolean isGlobal, String[] args) {
        if (args.length < 2) {
            player.sendMessage(getMessageFromConfig("roleplay.errors.invalid_roll_usage"));
            return;
        }

        try {
            int min = Integer.parseInt(args[0]);
            int max = Integer.parseInt(args[1]);
            if (min > max) {
                player.sendMessage(getMessageFromConfig("roleplay.errors.invalid_roll_usage"));
                return;
            }

            int result = random.nextInt((max - min) + 1) + min;
            String format = isGlobal ? commandFormats.get("groll") : commandFormats.get("roll");
            String formattedMessage = format
                    .replace("{player}", player.getName())
                    .replace("{min}", String.valueOf(min))
                    .replace("{max}", String.valueOf(max))
                    .replace("{result}", String.valueOf(result));

            sendRoleplayMessage(player, range, isGlobal, formattedMessage, "");
        } catch (NumberFormatException e) {
            player.sendMessage(getMessageFromConfig("roleplay.errors.invalid_roll_usage"));
        }
    }

    private String getErrorKeyForCommand(String commandName) {
        // Убираем префикс "g" для глобальных команд
        String baseCommand = commandName.replaceFirst("^g", "");
        return "roleplay.errors.missing_argument." + baseCommand;
    }

    private boolean requiresTextArgument(String commandName) {
        return commandName.equals("me") || commandName.equals("gme") ||
                commandName.equals("do") || commandName.equals("gdo") ||
                commandName.equals("try") || commandName.equals("gtry") ||
                commandName.equals("todo") || commandName.equals("gtodo") ||
                commandName.equals("ball") || commandName.equals("gball");
    }

    private void handleBallCommand(Player player, int range, boolean isGlobal, String question) {
        List<String> answers = plugin.getConfig().getStringList("roleplay.magic_ball_answers");
        if (answers.isEmpty()) {
            player.sendMessage(getMessageFromConfig("roleplay.errors.empty_magic_ball"));
            return;
        }

        String answer = answers.get(random.nextInt(answers.size()));
        String format = isGlobal ? commandFormats.get("gball") : commandFormats.get("ball");
        String formattedMessage = format
                .replace("{player}", player.getName())
                .replace("{question}", question)
                .replace("{answer}", answer);

        sendRoleplayMessage(player, range, isGlobal, formattedMessage, "");
    }

    private void sendRoleplayMessage(Player sender, int range, boolean isGlobal, String format, String message) {
        if (format == null || format.isEmpty()) {
            sender.sendMessage(getMessageFromConfig("roleplay.errors.no_format_found"));
            return;
        }

        String formattedMessage = applyPlaceholders(sender, format.replace("{message}", message));
        if (isGlobal) {
            Bukkit.broadcastMessage(formattedMessage);
        } else {
            sender.getNearbyEntities(range, range, range).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> p.sendMessage(formattedMessage));
            sender.sendMessage(formattedMessage);
        }
    }

    private String getMessageFromConfig(String path) {
        // Проверяем, загружен ли конфиг сообщений
        if (plugin.getMessagesConfig() == null) {
            plugin.getLogger().severe("❗Messages configuration file is not loaded!");
            return ChatColor.translateAlternateColorCodes('&', "&cОшибка: Конфигурация сообщений не загружена!");
        }

        // Получаем сообщение из конфига
        String message = plugin.getMessagesConfig().getString(path);

        // Если сообщение не найдено
        if (message == null) {
            plugin.getLogger().warning("❗Message not found in configuration: " + path);
            return ChatColor.translateAlternateColorCodes('&', "&cСообщение отсутствует: " + path);
        }

        // Если сообщение найдено, но пустое
        if (message.isEmpty()) {
            plugin.getLogger().warning("❗Message is empty in configuration: " + path);
            return ChatColor.translateAlternateColorCodes('&', "&cСообщение пустое: " + path);
        }

        // Применяем HEX-цвета и возвращаем сообщение
        try {
            return ChatColor.translateAlternateColorCodes('&', replaceHexColors(message));
        } catch (Exception e) {
            plugin.getLogger().severe("❗Failed to process message (invalid format?): " + path);
            plugin.getLogger().severe("❗Error: " + e.getMessage());
            return ChatColor.translateAlternateColorCodes('&', "&cОшибка обработки сообщения: " + path);
        }
    }
}