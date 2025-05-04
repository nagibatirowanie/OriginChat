package me.nagibatirowanie.originChat.Modules;

import me.nagibatirowanie.originChat.OriginChat;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.nagibatirowanie.originLib.Messages.applyPlaceholders;
import static me.nagibatirowanie.originLib.Messages.replaceHexColors;

public class PrivateMessage extends Module implements CommandExecutor, TabCompleter {

    private final Map<Player, Player> lastMessageMap = new HashMap<>();
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    private String senderFormat;
    private String receiverFormat;
    private boolean registered = false;

    public PrivateMessage(OriginChat plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!isEnabled()) {
            plugin.getLogger().info("The PrivateMessages module is disabled in the configuration. Skip activation.");
            return;
        }

        if (!registered) {
            registerCommands();
            registered = true;
        }

        loadConfig();
        plugin.getLogger().info("The PrivateMessages module has been successfully loaded.");
    }

    @Override
    public void onDisable() {
        if (registered) {
            unregisterCommands();
            registered = false;
        }
        plugin.getLogger().info("The PrivateMessages module is disabled.");
    }

    private void registerCommands() {
        registerCommand("msg");
        registerCommand("r");
    }

    private void registerCommand(String commandName) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    private void unregisterCommands() {
        unregisterCommand("msg");
        unregisterCommand("r");
    }

    private void unregisterCommand(String commandName) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setExecutor(null);
            command.setTabCompleter(null);
        }
    }

    protected void loadConfig() {
        try {
            ConfigurationSection privateMessageSection = plugin.getConfig().getConfigurationSection("private-message");
            if (privateMessageSection != null) {
                senderFormat = privateMessageSection.getString("sender-format", "&7Вы &8-> &7{receiver}: &f{message}");
                receiverFormat = privateMessageSection.getString("receiver-format", "&7{sender} &8-> &7Вам: &f{message}");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❗Failed to load PrivateMessage configuration: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessageFromConfig("not-a-player"));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("msg")) {
            // Проверка наличия аргументов
            if (args.length == 0) {
                player.sendMessage(getMessageFromConfig("player-not-specified"));
                return true;
            }

            if (args.length == 1) {
                player.sendMessage(getMessageFromConfig("message-not-specified"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);

            // Проверка существования игрока
            if (target == null || !target.isOnline()) {
                player.sendMessage(getMessageFromConfig("player-not-found")
                        .replace("{player}", args[0]));
                return true;
            }

            // Проверка на отправку сообщения самому себе
            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(getMessageFromConfig("cannot-message-yourself"));
                return true;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            sendPrivateMessage(player, target, message);
            return true;
        }

        if (command.getName().equalsIgnoreCase("r")) {
            // Проверка наличия сообщения
            if (args.length == 0) {
                player.sendMessage(getMessageFromConfig("message-not-specified"));
                return true;
            }

            Player lastTarget = lastMessageMap.get(player);
            if (lastTarget == null || !lastTarget.isOnline()) {
                player.sendMessage(getMessageFromConfig("no-reply-target"));
                return true;
            }

            String message = String.join(" ", args);
            sendPrivateMessage(player, lastTarget, message);
            return true;
        }

        return false;
    }

    private void sendPrivateMessage(Player sender, Player receiver, String message) {
        String formattedSenderMessage = formatMessage(senderFormat, sender, receiver, message);
        String formattedReceiverMessage = formatMessage(receiverFormat, sender, receiver, message);

        sender.sendMessage(formattedSenderMessage);
        receiver.sendMessage(formattedReceiverMessage);

        lastMessageMap.put(sender, receiver);
        lastMessageMap.put(receiver, sender);
    }

    private String formatMessage(String format, Player sender, Player receiver, String message) {
        String result = format
                .replace("{sender}", sender.getName())
                .replace("{receiver}", receiver.getName())
                .replace("{message}", message);

        result = applyPlaceholders(sender, result);

        return replaceHexColors(result);
    }

    private String getMessageFromConfig(String path) {
        String message = plugin.getMessagesConfig().getString(path, "❗null");
        message = replaceHexColors(message);
        return applyPlaceholders(null, message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("msg") && args.length == 1) {
            List<String> onlinePlayers = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Не показывать себя в списке автодополнения
                if (!player.equals(sender)) {
                    onlinePlayers.add(player.getName());
                }
            }
            return onlinePlayers;
        }
        return Collections.emptyList();
    }
}