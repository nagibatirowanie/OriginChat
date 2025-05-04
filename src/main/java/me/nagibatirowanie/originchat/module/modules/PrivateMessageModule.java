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
 * Модуль для отправки приватных сообщений между игроками
 */
public class PrivateMessageModule extends AbstractModule implements CommandExecutor, TabCompleter {

    private final Map<Player, Player> lastMessageMap = new HashMap<>();

    private String senderFormat;
    private String receiverFormat;
    private boolean enabled;
    private boolean registered = false;

    // Сообщения для игроков
    private String msgNotAPlayer;
    private String msgPlayerNotSpecified;
    private String msgMessageNotSpecified;
    private String msgPlayerNotFound;
    private String msgCannotMessageYourself;
    private String msgNoReplyTarget;

    public PrivateMessageModule(OriginChat plugin) {
        super(plugin, "pm", "Приватные сообщения", "Модуль для отправки личных сообщений между игроками", "1.0");
    }

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        loadModuleConfig("modules/pm");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        
        loadConfig();
        
        if (!enabled) {
            log("Модуль приватных сообщений отключен в конфигурации.");
            return;
        }

        if (!registered) {
            registerCommands();
            registered = true;
        }

        log("Модуль приватных сообщений успешно загружен.");
    }

    @Override
    public void onDisable() {
        if (registered) {
            unregisterCommands();
            registered = false;
        }
        log("Модуль приватных сообщений выключен.");
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
            
            // Загрузка форматов сообщений
            senderFormat = config.getString("format.sender", "&7Вы &8-> &7{receiver}: &f{message}");
            receiverFormat = config.getString("format.receiver", "&7{sender} &8-> &7Вам: &f{message}");
            
            // Загрузка сообщений для игроков
            ConfigurationSection messagesSection = config.getConfigurationSection("messages");
            if (messagesSection != null) {
                msgNotAPlayer = messagesSection.getString("not-a-player", "&cЭта команда доступна только для игроков!");
                msgPlayerNotSpecified = messagesSection.getString("player-not-specified", "&cУкажите имя игрока!");
                msgMessageNotSpecified = messagesSection.getString("message-not-specified", "&cВведите сообщение!");
                msgPlayerNotFound = messagesSection.getString("player-not-found", "&cИгрок {player} не найден или не в сети!");
                msgCannotMessageYourself = messagesSection.getString("cannot-message-yourself", "&cВы не можете отправить сообщение самому себе!");
                msgNoReplyTarget = messagesSection.getString("no-reply-target", "&cНекому ответить! Сначала отправьте кому-нибудь сообщение.");
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при загрузке конфигурации модуля приватных сообщений: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.format(msgNotAPlayer));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("msg")) {
            // Проверка наличия аргументов
            if (args.length == 0) {
                player.sendMessage(ColorUtil.format(msgPlayerNotSpecified));
                return true;
            }

            if (args.length == 1) {
                player.sendMessage(ColorUtil.format(msgMessageNotSpecified));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);

            // Проверка существования игрока
            if (target == null || !target.isOnline()) {
                player.sendMessage(ColorUtil.format(msgPlayerNotFound.replace("{player}", args[0])));
                return true;
            }

            // Проверка на отправку сообщения самому себе
            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(ColorUtil.format(msgCannotMessageYourself));
                return true;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            sendPrivateMessage(player, target, message);
            return true;
        }

        if (command.getName().equalsIgnoreCase("r")) {
            // Проверка наличия сообщения
            if (args.length == 0) {
                player.sendMessage(ColorUtil.format(msgMessageNotSpecified));
                return true;
            }

            Player lastTarget = lastMessageMap.get(player);
            if (lastTarget == null || !lastTarget.isOnline()) {
                player.sendMessage(ColorUtil.format(msgNoReplyTarget));
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

        return ColorUtil.format(sender, result);
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