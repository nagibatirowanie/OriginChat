package me.nagibatirowanie.originchat.commands;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.locale.LocaleManager;
import me.nagibatirowanie.originchat.utils.FormatUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Команда для отправки объявления всем игрокам
 */
public class BroadcastCommand implements CommandExecutor {

    private final OriginChat plugin;
    private final LocaleManager localeManager;

    public BroadcastCommand(OriginChat plugin) {
        this.plugin = plugin;
        this.localeManager = plugin.getLocaleManager();
        
        plugin.getCommand("broadcast").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем права на выполнение команды
        if (!sender.hasPermission("originchat.command.broadcast") && !sender.hasPermission("originchat.admin")) {
            localeManager.sendMessage(sender, "commands.no_permission");
            return true;
        }
        
        // Проверяем, что есть сообщение для отправки
        if (args.length == 0) {
            localeManager.sendMessage(sender, "commands.broadcast.usage");
            return true;
        }
        
        // Собираем сообщение из аргументов
        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }
        
        // Получаем имя отправителя
        String senderName = sender instanceof Player ? ((Player) sender).getDisplayName() : "Console";
        
        // Отправляем сообщение всем игрокам с учетом их локализации
        for (Player player : Bukkit.getOnlinePlayers()) {
            String locale = localeManager.getPlayerLocale(player);
            String format = localeManager.getMessage("commands.broadcast.format", locale);
            
            // Заменяем плейсхолдеры
            String formattedMessage = format
                    .replace("{sender}", senderName)
                    .replace("{message}", message.toString().trim());
            
            // Применяем форматирование и отправляем
            // Используем FormatUtil для лучшей поддержки HEX-цветов
            player.sendMessage(FormatUtil.format(player, formattedMessage, true, true, true));
        }
        
        // Отправляем сообщение в консоль
        if (!(sender instanceof Player)) {
            String format = localeManager.getMessage("commands.broadcast.format", localeManager.getDefaultLanguage());
            String formattedMessage = format
                    .replace("{sender}", senderName)
                    .replace("{message}", message.toString().trim());
            // Используем FormatUtil для консоли
            Bukkit.getConsoleSender().sendMessage(FormatUtil.format(formattedMessage));
        }
        
        // Логируем действие
        plugin.getLogger().info("[BroadcastCommand] " + senderName + " отправил объявление: " + message.toString().trim());
        
        return true;
    }
}