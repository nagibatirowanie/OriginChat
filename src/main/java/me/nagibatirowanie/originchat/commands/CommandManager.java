package me.nagibatirowanie.originchat.commands;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Менеджер команд плагина
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final OriginChat plugin;

    public CommandManager(OriginChat plugin) {
        this.plugin = plugin;
        
        // Регистрация команд
        plugin.getCommand("originchat").setExecutor(this);
        plugin.getCommand("originchat").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("originchat.admin")) {
                    sender.sendMessage(ChatUtil.formatColors("&cУ вас нет прав для выполнения этой команды!"));
                    return true;
                }
                
                // Перезагрузка плагина
                plugin.getConfigManager().loadConfigs();
                plugin.getModuleManager().unloadModules();
                plugin.getModuleManager().loadModules();
                
                sender.sendMessage(ChatUtil.formatColors("&aПлагин OriginChat успешно перезагружен!"));
                break;
                
            case "module":
                if (!sender.hasPermission("originchat.admin")) {
                    sender.sendMessage(ChatUtil.formatColors("&cУ вас нет прав для выполнения этой команды!"));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(ChatUtil.formatColors("&cИспользование: /originchat module <list|enable|disable> [имя_модуля]"));
                    return true;
                }
                
                handleModuleCommand(sender, args);
                break;
                
            case "help":
                sendHelp(sender);
                break;
                
            default:
                sender.sendMessage(ChatUtil.formatColors("&cНеизвестная команда. Используйте /originchat help для справки."));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Первый аргумент - подкоманды
            if (sender.hasPermission("originchat.admin")) {
                completions.addAll(Arrays.asList("reload", "module", "help"));
            } else {
                completions.add("help");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            // Второй аргумент для команды module
            if (sender.hasPermission("originchat.admin")) {
                completions.addAll(Arrays.asList("list", "enable", "disable", "reload"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("module") 
                && (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("reload"))) {
            // Третий аргумент - имя модуля
            if (sender.hasPermission("originchat.admin")) {
                plugin.getModuleManager().getModules().keySet().forEach(completions::add);
            }
        }
        
        return completions;
    }

    /**
     * Отправить справку по командам
     * @param sender отправитель
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.formatColors("&7===== &bOriginChat &7====="));
        sender.sendMessage(ChatUtil.formatColors("&b/originchat help &7- Показать справку"));
        
        if (sender.hasPermission("originchat.admin")) {
            sender.sendMessage(ChatUtil.formatColors("&b/originchat reload &7- Перезагрузить плагин"));
            sender.sendMessage(ChatUtil.formatColors("&b/originchat module list &7- Список модулей"));
            sender.sendMessage(ChatUtil.formatColors("&b/originchat module enable <имя> &7- Включить модуль"));
            sender.sendMessage(ChatUtil.formatColors("&b/originchat module disable <имя> &7- Выключить модуль"));
            sender.sendMessage(ChatUtil.formatColors("&b/originchat module reload <имя> &7- Перезагрузить модуль"));
        }
        
        sender.sendMessage(ChatUtil.formatColors("&7========================="));
    }

    /**
     * Обработать команды для управления модулями
     * @param sender отправитель
     * @param args аргументы
     */
    private void handleModuleCommand(CommandSender sender, String[] args) {
        switch (args[1].toLowerCase()) {
            case "list":
                // Список всех модулей
                sender.sendMessage(ChatUtil.formatColors("&7===== &bМодули OriginChat &7====="));
                
                plugin.getModuleManager().getModules().forEach((id, module) -> {
                    boolean enabled = plugin.getModuleManager().isModuleEnabled(id);
                    String status = enabled ? "&aВключен" : "&cВыключен";
                    
                    sender.sendMessage(ChatUtil.formatColors("&b" + module.getName() + " &7(" + id + ") - " + status));
                    sender.sendMessage(ChatUtil.formatColors("  &7" + module.getDescription()));
                });
                
                sender.sendMessage(ChatUtil.formatColors("&7========================="));
                break;
                
            case "enable":
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.formatColors("&cУкажите имя модуля для включения!"));
                    return;
                }
                
                String moduleToEnable = args[2].toLowerCase();
                if (plugin.getModuleManager().enableModule(moduleToEnable)) {
                    sender.sendMessage(ChatUtil.formatColors("&aМодуль '" + moduleToEnable + "' успешно включен!"));
                } else {
                    sender.sendMessage(ChatUtil.formatColors("&cНе удалось включить модуль '" + moduleToEnable + "'!"));
                }
                break;
                
            case "disable":
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.formatColors("&cУкажите имя модуля для выключения!"));
                    return;
                }
                
                String moduleToDisable = args[2].toLowerCase();
                if (plugin.getModuleManager().disableModule(moduleToDisable)) {
                    sender.sendMessage(ChatUtil.formatColors("&aМодуль '" + moduleToDisable + "' успешно выключен!"));
                } else {
                    sender.sendMessage(ChatUtil.formatColors("&cНе удалось выключить модуль '" + moduleToDisable + "'!"));
                }
                break;
                
            case "reload":
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.formatColors("&cУкажите имя модуля для перезагрузки!"));
                    return;
                }
                
                String moduleToReload = args[2].toLowerCase();
                if (plugin.getModuleManager().reloadModule(moduleToReload)) {
                    sender.sendMessage(ChatUtil.formatColors("&aМодуль '" + moduleToReload + "' успешно перезагружен!"));
                } else {
                    sender.sendMessage(ChatUtil.formatColors("&cНе удалось перезагрузить модуль '" + moduleToReload + "'!"));
                }
                break;
                
            default:
                sender.sendMessage(ChatUtil.formatColors("&cИспользование: /originchat module <list|enable|disable|reload> [имя_модуля]"));
                break;
        }
    }
}