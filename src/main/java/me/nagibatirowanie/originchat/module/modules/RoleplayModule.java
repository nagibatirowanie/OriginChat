package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.config.ConfigManager;
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
 * Модуль для ролевых команд (me, do, try и т.д.)
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
        super(plugin, "roleplay", "Модуль ролевых команд", "Добавляет команды для ролевой игры (me, do, try и т.д.)", "1.0");
    }

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        loadModuleConfig("modules/roleplay");
        
        // Регистрация команд
        if (!registered) {
            registerCommands();
            registered = true;
        }
        
        // Загрузка настроек
        loadSettings();
        
        plugin.getPluginLogger().info("Модуль ролевых команд успешно загружен!");
    }

    @Override
    public void onDisable() {
        if (registered) {
            unregisterCommands();
            registered = false;
        }
        plugin.getPluginLogger().info("Модуль ролевых команд выключен.");
    }

    /**
     * Загрузить настройки из конфигурации
     */
    private void loadSettings() {
        try {
            // Очистка предыдущих настроек
            commandRanges.clear();
            commandFormats.clear();
            magicBallAnswers.clear();
            
            // Загрузка диапазонов команд
            ConfigurationSection ranges = config.getConfigurationSection("command_ranges");
            if (ranges != null) {
                for (String command : ranges.getKeys(false)) {
                    commandRanges.put(command, ranges.getInt(command, 100));
                }
            }

            // Загрузка форматов сообщений
            ConfigurationSection formats = config.getConfigurationSection("formats");
            if (formats != null) {
                for (String command : formats.getKeys(false)) {
                    commandFormats.put(command, formats.getString(command, ""));
                }
            }
            
            // Загрузка ответов магического шара
            magicBallAnswers = config.getStringList("magic_ball_answers");
            if (magicBallAnswers.isEmpty()) {
                // Добавление стандартных ответов, если список пуст
                magicBallAnswers = Arrays.asList(
                    "Да", "Нет", "Возможно", "Весьма вероятно", "Маловероятно", 
                    "Определённо да", "Определённо нет", "Спроси позже", "Не могу сказать сейчас"
                );
                // Сохраняем стандартные ответы в конфиг
                config.set("magic_ball_answers", magicBallAnswers);
                config.save(configName);
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("❗Ошибка при загрузке настроек модуля ролевых команд: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Регистрация всех команд
     */
    private void registerCommands() {
        for (String commandName : COMMANDS) {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                command.setExecutor(this);
            } else {
                plugin.getPluginLogger().warning("❗Команда '" + commandName + "' не найдена в plugin.yml!");
            }
        }
    }

    /**
     * Отмена регистрации всех команд
     */
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

        // Проверка на наличие аргументов для команд, которые требуют текста
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

    /**
     * Обработка команды try/gtry
     */
    private void handleTryCommand(Player player, int range, boolean isGlobal, String message) {
        boolean success = random.nextBoolean();
        String formatKey = success ? (isGlobal ? "gtry_success" : "try_success") : (isGlobal ? "gtry_failure" : "try_failure");
        sendRoleplayMessage(player, range, isGlobal, commandFormats.get(formatKey), message);
    }

    /**
     * Обработка команды roll/groll
     */
    private void handleRollCommand(Player player, int range, boolean isGlobal, String[] args) {
        if (args.length < 2) {
            player.sendMessage(getMessage("errors.invalid_roll_usage"));
            return;
        }

        try {
            int min = Integer.parseInt(args[0]);
            int max = Integer.parseInt(args[1]);
            if (min > max) {
                player.sendMessage(getMessage("errors.invalid_roll_usage"));
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
            player.sendMessage(getMessage("errors.invalid_roll_usage"));
        }
    }

    /**
     * Проверка, требует ли команда текстовый аргумент
     */
    private boolean requiresTextArgument(String commandName) {
        return commandName.equals("me") || commandName.equals("gme") ||
                commandName.equals("do") || commandName.equals("gdo") ||
                commandName.equals("try") || commandName.equals("gtry") ||
                commandName.equals("todo") || commandName.equals("gtodo") ||
                commandName.equals("ball") || commandName.equals("gball");
    }

    /**
     * Обработка команды ball/gball
     */
    private void handleBallCommand(Player player, int range, boolean isGlobal, String question) {
        if (magicBallAnswers.isEmpty()) {
            player.sendMessage(getMessage("errors.empty_magic_ball"));
            return;
        }

        String answer = magicBallAnswers.get(random.nextInt(magicBallAnswers.size()));
        String format = isGlobal ? commandFormats.get("gball") : commandFormats.get("ball");
        String formattedMessage = format
                .replace("{player}", player.getName())
                .replace("{question}", question)
                .replace("{answer}", answer);

        sendRoleplayMessage(player, range, isGlobal, formattedMessage, "");
    }

    /**
     * Отправка ролевого сообщения
     */
    private void sendRoleplayMessage(Player sender, int range, boolean isGlobal, String format, String message) {
        if (format == null || format.isEmpty()) {
            sender.sendMessage(getMessage("errors.no_format_found"));
            return;
        }

        String formattedMessage = ColorUtil.format(format.replace("{message}", message)
                .replace("{player}", sender.getName()));
        
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

    /**
     * Получение сообщения из конфигурации
     */
    private String getMessage(String path) {
        String fullPath = "errors." + path;
        if (config.contains(fullPath)) {
            return ColorUtil.format(config.getString(fullPath, "&cОшибка: Сообщение не найдено"));
        }
        return ColorUtil.format("&cОшибка: Сообщение не найдено (" + path + ")");
    }
}