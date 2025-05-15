package me.nagibatirowanie.originchat.commands;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.locale.LocaleManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

/**
 * Команда для очистки чата
 */
public class ClearChatCommand implements CommandExecutor {

    private final OriginChat plugin;
    private final LocaleManager localeManager;

    public ClearChatCommand(OriginChat plugin) {
        this.plugin = plugin;
        this.localeManager = plugin.getLocaleManager();
        
        plugin.getCommand("clearchat").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем права на выполнение команды
        if (!sender.hasPermission("originchat.command.clearchat") && !sender.hasPermission("originchat.admin")) {
            localeManager.sendMessage(sender, "commands.no_permission");
            return true;
        }
        
        // Отправляем пустые строки только игрокам, а не в консоль
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 128; i++) {
                player.sendMessage(Component.empty());
            }
        }
        
        // Отправляем сообщение о том, что чат был очищен
        if (sender instanceof Player) {
            Player player = (Player) sender;
            localeManager.sendMessage(player, "commands.chat_cleared_by", "{player}", player.getName());
            
            // Логируем действие
            plugin.getLogger().info("[ClearChatCommand] Игрок " + player.getName() + " очистил чат");
        } else {
            // Если команду выполнил не игрок (консоль)
            localeManager.sendMessage(sender, "commands.chat_cleared_by_console");
            
            // Логируем действие
            plugin.getLogger().info("[ClearChatCommand] Консоль очистила чат");
        }
        
        return true;
    }
}